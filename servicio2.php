<?php
// ============================================================
// SERVICIO 2 — Recomendador de Formación y Carrera
// ============================================================
// Acciones que maneja:
//   ?action=recomendar_formacion   → POST  Recomienda qué estudiar
//                                          basado en análisis del mercado
//   ?action=panorama_mercado       → GET   Datos agregados públicos
// ============================================================

// ═══════════════════════════════════════════════════════════
// CONFIGURACIÓN DE BASE DE DATOS — cambia estos valores
// ═══════════════════════════════════════════════════════════
$host = 'sql303.infinityfree.com';
$user = 'if0_42097646';
$pass = 'vBl5vfa4CsPNjUD';
$db   = 'if0_42097646_bolsadetrabajo';

header('Content-Type: application/json; charset=utf-8');

// ═══════════════════════════════════════════════════════════
// FUNCIONES AUXILIARES
// ═══════════════════════════════════════════════════════════

function conectar() {
    global $host, $user, $pass, $db;
    $conn = new mysqli($host, $user, $pass, $db);
    if ($conn->connect_error) {
        http_response_code(500);
        die(json_encode(["exito" => false, "error" => "Error de conexión a la BD"]));
    }
    $conn->set_charset("utf8mb4");
    return $conn;
}

function responder($data) {
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function error($mensaje, $codigo = 400) {
    http_response_code($codigo);
    responder(["exito" => false, "error" => $mensaje]);
}

// ═══════════════════════════════════════════════════════════
// ENRUTADOR INTERNO
// ═══════════════════════════════════════════════════════════

if (!isset($action) || empty($action)) {
    error("No se especificó acción");
}

switch ($action) {
    case 'recomendar_formacion':
        recomendarFormacion();
        break;
    case 'panorama_mercado':
        panoramaMercado();
        break;
    default:
        error("Acción desconocida: $action");
}

// ═══════════════════════════════════════════════════════════
// 1. RECOMENDADOR DE FORMACIÓN (para un postulante)
// ═══════════════════════════════════════════════════════════
function recomendarFormacion() {
    $input = json_decode(file_get_contents('php://input'), true);
    $idPostulante = $input['id_postulante'] ?? null;
    
    if (empty($idPostulante)) {
        error("Se requiere 'id_postulante' en el body JSON");
    }
    
    $conn = conectar();
    
    // Verificar que el postulante exista
    $stmt = $conn->prepare("SELECT ID_POSTULANTE, NOMBRE, APELLIDO, ID_GRADO_ACADEMICO FROM POSTULANTE WHERE ID_POSTULANTE = ?");
    $stmt->bind_param("s", $idPostulante);
    $stmt->execute();
    $res = $stmt->get_result();
    
    if ($res->num_rows === 0) {
        $conn->close();
        error("Postulante '$idPostulante' no encontrado", 404);
    }
    
    $postulante = $res->fetch_assoc();
    $gradoPostulante = $postulante['ID_GRADO_ACADEMICO'];
    $stmt->close();
    
    $resultado = [
        "postulante" => [
            "id"       => $postulante['ID_POSTULANTE'],
            "nombre"   => $postulante['NOMBRE'] . ' ' . $postulante['APELLIDO'],
            "id_grado" => $gradoPostulante
        ]
    ];
    
    // ─── Sección 1: Grados más demandados en ofertas activas ───
    $sqlGrados = "
        SELECT 
            ga.ID_GRADO_ACADEMICO,
            ga.NOMBRE_GRADO,
            COUNT(*) as total_ofertas
        FROM OFERTA_TRABAJO ot
        JOIN GRADO_ACADEMICO ga ON ot.ID_GRADO_ACADEMICO = ga.ID_GRADO_ACADEMICO
        WHERE ot.FECHA_CADUCIDAD >= CURDATE() OR ot.FECHA_CADUCIDAD IS NULL
        GROUP BY ga.ID_GRADO_ACADEMICO, ga.NOMBRE_GRADO
        ORDER BY total_ofertas DESC
        LIMIT 10
    ";
    
    $resGrados = $conn->query($sqlGrados);
    $gradosDemandados = [];
    while ($row = $resGrados->fetch_assoc()) {
        $row['es_tu_grado'] = ($row['ID_GRADO_ACADEMICO'] == $gradoPostulante);
        $gradosDemandados[] = $row;
    }
    $resGrados->free();
    $resultado['grados_mas_demandados'] = $gradosDemandados;
    
    // ─── Sección 2: Recomendaciones de carrera ───
    //  Muestra los grados que NO tenés pero que SÍ están siendo pedidos
    //  por ofertas activas, ordenados por demanda.
    $sqlRecomendaciones = "
        SELECT 
            ga.ID_GRADO_ACADEMICO,
            ga.NOMBRE_GRADO,
            COUNT(*) as total_ofertas,
            CASE 
                WHEN COUNT(*) >= 10 THEN 'alta'
                WHEN COUNT(*) >= 3 THEN 'media'
                ELSE 'baja'
            END as impacto
        FROM GRADO_ACADEMICO ga
        LEFT JOIN OFERTA_TRABAJO ot 
            ON ga.ID_GRADO_ACADEMICO = ot.ID_GRADO_ACADEMICO
            AND (ot.FECHA_CADUCIDAD >= CURDATE() OR ot.FECHA_CADUCIDAD IS NULL)
        WHERE ga.ID_GRADO_ACADEMICO NOT IN (
            SELECT ID_GRADO_ACADEMICO FROM GRADO_ACADEMICO 
            WHERE LOWER(NOMBRE_GRADO) LIKE '%bachiller%'
        )
          AND ga.ID_GRADO_ACADEMICO != ?
        GROUP BY ga.ID_GRADO_ACADEMICO, ga.NOMBRE_GRADO
        HAVING total_ofertas > 0
        ORDER BY total_ofertas DESC
        LIMIT 10
    ";
    
    $stmtRec = $conn->prepare($sqlRecomendaciones);
    $stmtRec->bind_param("i", $gradoPostulante);
    $stmtRec->execute();
    $resRec = $stmtRec->get_result();
    $recomendaciones = [];
    while ($row = $resRec->fetch_assoc()) {
        $recomendaciones[] = $row;
    }
    $stmtRec->close();
    $resultado['recomendaciones_carrera'] = $recomendaciones;
    
    // ─── Sección 3: Instituciones con más egresados ───
    $sqlInstituciones = "
        SELECT 
            i.ID_INSTITUCION,
            i.NOMBRE_INSTITUCION,
            COUNT(DISTINCT fa.ID_POSTULANTE) as total_egresados
        FROM FORMACION_ACADEMICA fa
        JOIN OFERTA_ACADEMICA oa ON fa.ID_OFERTA_ACADEMICA = oa.ID_OFERTA_ACADEMICA
        JOIN INSTITUCION i ON oa.ID_INSTITUCION = i.ID_INSTITUCION
        GROUP BY i.ID_INSTITUCION, i.NOMBRE_INSTITUCION
        ORDER BY total_egresados DESC
        LIMIT 10
    ";
    
    $resInst = $conn->query($sqlInstituciones);
    $instituciones = [];
    while ($row = $resInst->fetch_assoc()) {
        $instituciones[] = $row;
    }
    $resInst->free();
    $resultado['instituciones_top'] = $instituciones;
    
    // ─── Sección 4: Skills más comunes entre postulantes ───
    $sqlSkills = "
        SELECT 
            h.ID_CATEGORIA_HABILIDAD,
            h.ID_HABILIDAD,
            h.NOMBRE_HABILIDAD,
            ch.NOMBRE_CATEGORIA,
            COUNT(hp.ID_POSTULANTE) as total_postulantes
        FROM HABILIDAD_POSTULANTE hp
        JOIN HABILIDAD h ON hp.ID_CATEGORIA_HABILIDAD = h.ID_CATEGORIA_HABILIDAD 
            AND hp.ID_HABILIDAD = h.ID_HABILIDAD
        JOIN CATEGORIA_HABILIDAD ch ON h.ID_CATEGORIA_HABILIDAD = ch.ID_CATEGORIA_HABILIDAD
        GROUP BY h.ID_CATEGORIA_HABILIDAD, h.ID_HABILIDAD, h.NOMBRE_HABILIDAD, ch.NOMBRE_CATEGORIA
        ORDER BY total_postulantes DESC
        LIMIT 15
    ";
    
    $resSkills = $conn->query($sqlSkills);
    $skillsPopulares = [];
    while ($row = $resSkills->fetch_assoc()) {
        $skillsPopulares[] = $row;
    }
    $resSkills->free();
    $resultado['skills_mas_comunes'] = $skillsPopulares;
    
    // ─── Sección 5: Skills que te FALTAN ───
    $sqlSkillsFaltan = "
        SELECT 
            h.ID_CATEGORIA_HABILIDAD,
            h.ID_HABILIDAD,
            h.NOMBRE_HABILIDAD,
            ch.NOMBRE_CATEGORIA
        FROM HABILIDAD h
        JOIN CATEGORIA_HABILIDAD ch ON h.ID_CATEGORIA_HABILIDAD = ch.ID_CATEGORIA_HABILIDAD
        WHERE (h.ID_CATEGORIA_HABILIDAD, h.ID_HABILIDAD) NOT IN (
            SELECT hp.ID_CATEGORIA_HABILIDAD, hp.ID_HABILIDAD
            FROM HABILIDAD_POSTULANTE hp
            WHERE hp.ID_POSTULANTE = ?
        )
        ORDER BY ch.NOMBRE_CATEGORIA, h.NOMBRE_HABILIDAD
    ";
    
    $stmtSkillsFalt = $conn->prepare($sqlSkillsFaltan);
    $stmtSkillsFalt->bind_param("s", $idPostulante);
    $stmtSkillsFalt->execute();
    $resSkillsFalt = $stmtSkillsFalt->get_result();
    $skillsFaltantes = [];
    while ($row = $resSkillsFalt->fetch_assoc()) {
        $skillsFaltantes[] = $row;
    }
    $stmtSkillsFalt->close();
    $resultado['skills_que_te_faltan'] = $skillsFaltantes;
    
    // ─── Sección 6: Estadísticas generales del mercado ───
    $totalOfertas = $conn->query("SELECT COUNT(*) as total FROM OFERTA_TRABAJO WHERE FECHA_CADUCIDAD >= CURDATE() OR FECHA_CADUCIDAD IS NULL")->fetch_assoc()['total'];
    $totalPostulantes = $conn->query("SELECT COUNT(*) as total FROM POSTULANTE")->fetch_assoc()['total'];
    $totalEmpresas = $conn->query("SELECT COUNT(*) as total FROM EMPRESA")->fetch_assoc()['total'];
    
    $resultado['estadisticas_mercado'] = [
        "ofertas_activas"    => intval($totalOfertas),
        "total_postulantes"  => intval($totalPostulantes),
        "total_empresas"     => intval($totalEmpresas),
        "competencia_promedio" => $totalOfertas > 0 
            ? round($totalPostulantes / $totalOfertas, 1) . " postulantes por oferta"
            : "Sin datos (no hay ofertas activas)"
    ];
    
    $conn->close();
    responder(["exito" => true, "data" => $resultado]);
}

// ═══════════════════════════════════════════════════════════
// 2. PANORAMA DEL MERCADO (público, sin necesidad de ID)
// ═══════════════════════════════════════════════════════════
function panoramaMercado() {
    $conn = conectar();
    
    // Grados demandados
    $sqlGrados = "
        SELECT ga.NOMBRE_GRADO, COUNT(*) as total
        FROM OFERTA_TRABAJO ot
        JOIN GRADO_ACADEMICO ga ON ot.ID_GRADO_ACADEMICO = ga.ID_GRADO_ACADEMICO
        WHERE ot.FECHA_CADUCIDAD >= CURDATE() OR ot.FECHA_CADUCIDAD IS NULL
        GROUP BY ga.NOMBRE_GRADO ORDER BY total DESC
    ";
    $grados = [];
    $res = $conn->query($sqlGrados);
    while ($r = $res->fetch_assoc()) $grados[] = $r;
    $res->free();
    
    // Departamentos con más ofertas
    $sqlDeptos = "
        SELECT dep.NOMBRE_DEPARTAMENTO, COUNT(*) as total_ofertas
        FROM OFERTA_TRABAJO ot
        JOIN EMPRESA e ON ot.NIT = e.NIT
        JOIN DISTRITO d ON e.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO 
            AND e.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO 
            AND e.ID_DISTRITO_ID = d.ID_DISTRITO
        JOIN MUNICIPIO m ON d.ID_DEPARTAMENTO = m.ID_DEPARTAMENTO 
            AND d.ID_MUNICIPIO = m.ID_MUNICIPIO
        JOIN DEPARTAMENTO dep ON m.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO
        WHERE ot.FECHA_CADUCIDAD >= CURDATE() OR ot.FECHA_CADUCIDAD IS NULL
        GROUP BY dep.NOMBRE_DEPARTAMENTO
        ORDER BY total_ofertas DESC
    ";
    $deptos = [];
    $res = $conn->query($sqlDeptos);
    while ($r = $res->fetch_assoc()) $deptos[] = $r;
    $res->free();
    
    // Totales
    $totalOfertas = $conn->query("SELECT COUNT(*) as t FROM OFERTA_TRABAJO WHERE FECHA_CADUCIDAD >= CURDATE() OR FECHA_CADUCIDAD IS NULL")->fetch_assoc()['t'];
    $totalPostulantes = $conn->query("SELECT COUNT(*) as t FROM POSTULANTE")->fetch_assoc()['t'];
    $totalEmpresas = $conn->query("SELECT COUNT(*) as t FROM EMPRESA")->fetch_assoc()['t'];
    
    $conn->close();
    
    responder([
        "exito" => true,
        "data" => [
            "totales" => [
                "ofertas_activas"   => intval($totalOfertas),
                "postulantes"       => intval($totalPostulantes),
                "empresas"          => intval($totalEmpresas)
            ],
            "grados_demandados"     => $grados,
            "ofertas_por_departamento" => $deptos
        ]
    ]);
}
