<?php
// ============================================================
// SERVICIO 1 — Carga Masiva de Ofertas de Trabajo (MEJORADO)
// ============================================================
// Acciones que maneja:
//   ?action=empresas          → GET   Lista empresas con ubicación
//   ?action=grados            → GET   Lista grados académicos
//   ?action=insertar_ofertas  → POST  Inserta ofertas en lote con requisitos
// ============================================================

// ═══════════════════════════════════════════════════════════
// CONFIGURACIÓN DE BASE DE DATOS — cambia estos valores
// ═══════════════════════════════════════════════════════════
$host = 'sql303.infinityfree.com';
$user = 'if0_42097646';
$pass = 'vBl5vfa4CsPNjUD';
$db   = 'if0_42097646_bolsadetrabajo';

// ═══════════════════════════════════════════════════════════
// FUNCIONES AUXILIARES
// ═══════════════════════════════════════════════════════════

function conectar() {
    global $host, $user, $pass, $db;
    $conn = new mysqli($host, $user, $pass, $db);
    if ($conn->connect_error) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        die(json_encode(["exito" => false, "error" => "Error de conexión a la BD"]));
    }
    $conn->set_charset("utf8mb4");
    return $conn;
}

function responder($data) {
    header('Content-Type: application/json; charset=utf-8');
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

// La variable $action viene de go.php (pasada por include)
if (!isset($action) || empty($action)) {
    error("No se especificó acción");
}

switch ($action) {
    case 'empresas':
        listarEmpresas();
        break;
    case 'grados':
        listarGrados();
        break;
    case 'insertar_ofertas':
        insertarOfertas();
        break;
    case 'descargar_formato':
        descargarFormato();
        break;
    default:
        error("Acción desconocida: $action");
}

// ═══════════════════════════════════════════════════════════
// 1. LISTAR EMPRESAS (con datos geográficos)
// ═══════════════════════════════════════════════════════════
function listarEmpresas() {
    $conn = conectar();
    
    $sql = "
        SELECT 
            e.NIT, 
            e.NOMBRE_EMPRESA, 
            e.CONTACTO_DIRECTO,
            d.NOMBRE_DISTRITO,
            m.NOMBRE_MUNICIPIO,
            dep.NOMBRE_DEPARTAMENTO,
            dep.ID_DEPARTAMENTO,
            m.ID_MUNICIPIO,
            d.ID_DISTRITO
        FROM EMPRESA e
        JOIN DISTRITO d 
            ON e.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO 
            AND e.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO 
            AND e.ID_DISTRITO_ID = d.ID_DISTRITO
        JOIN MUNICIPIO m 
            ON d.ID_DEPARTAMENTO = m.ID_DEPARTAMENTO 
            AND d.ID_MUNICIPIO = m.ID_MUNICIPIO
        JOIN DEPARTAMENTO dep 
            ON m.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO
        ORDER BY dep.NOMBRE_DEPARTAMENTO, m.NOMBRE_MUNICIPIO, e.NOMBRE_EMPRESA
    ";
    
    $result = $conn->query($sql);
    if (!$result) {
        $conn->close();
        error("Error al consultar empresas: " . $conn->error, 500);
    }
    
    $empresas = [];
    while ($row = $result->fetch_assoc()) {
        $empresas[] = $row;
    }
    
    $conn->close();
    responder(["exito" => true, "data" => $empresas]);
}

// ═══════════════════════════════════════════════════════════
// 2. LISTAR GRADOS ACADÉMICOS
// ═══════════════════════════════════════════════════════════
function listarGrados() {
    $conn = conectar();
    
    $sql = "SELECT ID_GRADO_ACADEMICO, NOMBRE_GRADO FROM GRADO_ACADEMICO ORDER BY ID_GRADO_ACADEMICO";
    $result = $conn->query($sql);
    
    if (!$result) {
        $conn->close();
        error("Error al consultar grados: " . $conn->error, 500);
    }
    
    $grados = [];
    while ($row = $result->fetch_assoc()) {
        $grados[] = $row;
    }
    
    $conn->close();
    responder(["exito" => true, "data" => $grados]);
}

// ═══════════════════════════════════════════════════════════
// 3. INSERTAR OFERTAS EN LOTE (con validación completa)
// ═══════════════════════════════════════════════════════════
function insertarOfertas() {
    // Leer JSON del body
    $input = json_decode(file_get_contents('php://input'), true);
    if (!$input || !isset($input['ofertas']) || !is_array($input['ofertas'])) {
        error("Se esperaba un JSON con campo 'ofertas' (array)");
    }
    
    $ofertas = $input['ofertas'];
    if (count($ofertas) === 0) {
        error("El array 'ofertas' está vacío");
    }
    
    $conn = conectar();
    
    // Cachear empresas y grados para validación rápida
    $empresasCache = [];
    $resEmp = $conn->query("SELECT NIT, NOMBRE_EMPRESA FROM EMPRESA");
    while ($r = $resEmp->fetch_assoc()) $empresasCache[$r['NIT']] = $r['NOMBRE_EMPRESA'];
    $resEmp->free();
    
    $gradosCache = [];
    $resGrad = $conn->query("SELECT ID_GRADO_ACADEMICO, NOMBRE_GRADO FROM GRADO_ACADEMICO");
    while ($r = $resGrad->fetch_assoc()) $gradosCache[$r['ID_GRADO_ACADEMICO']] = $r['NOMBRE_GRADO'];
    $resGrad->free();
    
    $resultados = [];
    $insertadas = 0;
    $fallidas = 0;
    
    foreach ($ofertas as $index => $oferta) {
        $idOferta = $oferta['id_oferta'] ?? "índice $index";
        
        // --- VALIDACIONES ---
        $errores = validarOferta($oferta, $empresasCache, $gradosCache);
        
        if (!empty($errores)) {
            $resultados[] = [
                "oferta_id"      => $idOferta,
                "titulo"         => $oferta['titulo'] ?? '',
                "estado"         => "fallida",
                "errores"        => $errores
            ];
            $fallidas++;
            continue;
        }
        
        // --- INSERTAR EN TRANSACCIÓN ---
        $conn->begin_transaction();
        
        try {
            $nit        = $conn->real_escape_string($oferta['nit']);
            $idOfer     = $conn->real_escape_string($oferta['id_oferta']);
            $idGrado    = isset($oferta['id_grado']) ? intval($oferta['id_grado']) : 'NULL';
            $idGradoVal = isset($oferta['id_grado']) ? $idGrado : 'NULL';
            $titulo     = $conn->real_escape_string($oferta['titulo']);
            $fechaPub   = !empty($oferta['fecha_publicacion']) ? "'" . $conn->real_escape_string($oferta['fecha_publicacion']) . "'" : 'NULL';
            $fechaCad   = !empty($oferta['fecha_caducidad'])   ? "'" . $conn->real_escape_string($oferta['fecha_caducidad'])   . "'" : 'NULL';
            $expAnios   = isset($oferta['experiencia_anios'])  ? intval($oferta['experiencia_anios'])  : 'NULL';
            $edadMin    = isset($oferta['edad_minima'])        ? intval($oferta['edad_minima'])        : 'NULL';
            $edadMax    = isset($oferta['edad_maxima'])        ? intval($oferta['edad_maxima'])        : 'NULL';
            $desc       = !empty($oferta['descripcion'])       ? "'" . $conn->real_escape_string($oferta['descripcion']) . "'" : 'NULL';
            
            // Insertar la oferta
            $sql = "INSERT INTO OFERTA_TRABAJO 
                (NIT, ID_OFERTA, ID_GRADO_ACADEMICO, TITULO_PUESTO, FECHA_PUBLICACION, FECHA_CADUCIDAD, 
                 EXPERIENCIA_ANIOS, EDAD_MINIMA, EDAD_MAXIMA, DESCRIPCION_OFERTA_TRABAJO)
                VALUES 
                ('$nit', '$idOfer', $idGradoVal, '$titulo', $fechaPub, $fechaCad, 
                 $expAnios, $edadMin, $edadMax, $desc)";
            
            if (!$conn->query($sql)) {
                throw new Exception("Error al insertar oferta: " . $conn->error);
            }
            
            // Insertar requisitos
            $requisitos = $oferta['requisitos'] ?? [];
            if (is_array($requisitos)) {
                foreach ($requisitos as $req) {
                    $idDet   = $conn->real_escape_string($req['id_detalle'] ?? '');
                    $descReq = $conn->real_escape_string($req['descripcion'] ?? '');
                    
                    if (empty($idDet) || empty($descReq)) continue;
                    
                    $sqlReq = "INSERT INTO DETALLE_REQUISITO 
                        (NIT, ID_OFERTA, ID_DETALLE, DESCRIPCION_REQUISITO)
                        VALUES ('$nit', '$idOfer', '$idDet', '$descReq')";
                    
                    if (!$conn->query($sqlReq)) {
                        throw new Exception("Error al insertar requisito '$idDet': " . $conn->error);
                    }
                }
            }
            
            $conn->commit();
            $resultados[] = [
                "oferta_id"      => $idOferta,
                "titulo"         => $oferta['titulo'],
                "estado"         => "insertada",
                "requisitos"     => count($requisitos)
            ];
            $insertadas++;
            
        } catch (Exception $e) {
            $conn->rollback();
            $resultados[] = [
                "oferta_id"      => $idOferta,
                "titulo"         => $oferta['titulo'] ?? '',
                "estado"         => "fallida",
                "errores"        => [$e->getMessage()]
            ];
            $fallidas++;
        }
    }
    
    $conn->close();
    
    responder([
        "exito"       => true,
        "mensaje"     => "Procesadas: $insertadas insertadas, $fallidas fallidas de " . count($ofertas) . " total",
        "insertadas"  => $insertadas,
        "fallidas"    => $fallidas,
        "detalle"     => $resultados
    ]);
}

// ═══════════════════════════════════════════════════════════
// VALIDADOR DE OFERTA
// ═══════════════════════════════════════════════════════════
function validarOferta($oferta, $empresasCache, $gradosCache) {
    $errores = [];
    
    // Campos obligatorios
    if (empty($oferta['nit']))            $errores[] = "NIT es obligatorio";
    if (empty($oferta['id_oferta']))      $errores[] = "ID de oferta es obligatorio";
    if (empty($oferta['titulo']))         $errores[] = "Título del puesto es obligatorio";
    
    // Validar NIT existe en EMPRESA
    if (!empty($oferta['nit']) && !isset($empresasCache[$oferta['nit']])) {
        $errores[] = "El NIT '{$oferta['nit']}' no existe en EMPRESA";
    }
    
    // Validar ID_GRADO_ACADEMICO si se proporciona
    if (isset($oferta['id_grado']) && $oferta['id_grado'] !== null && $oferta['id_grado'] !== '') {
        $gradoId = intval($oferta['id_grado']);
        if (!isset($gradosCache[$gradoId])) {
            $errores[] = "El grado académico ID=$gradoId no existe";
        }
    }
    
    // Validar fechas
    $fechaPub = $oferta['fecha_publicacion'] ?? null;
    $fechaCad = $oferta['fecha_caducidad'] ?? null;
    
    if (!empty($fechaPub) && !preg_match('/^\d{4}-\d{2}-\d{2}$/', $fechaPub)) {
        $errores[] = "Fecha de publicación inválida (formato: YYYY-MM-DD)";
    }
    if (!empty($fechaCad) && !preg_match('/^\d{4}-\d{2}-\d{2}$/', $fechaCad)) {
        $errores[] = "Fecha de caducidad inválida (formato: YYYY-MM-DD)";
    }
    if (!empty($fechaPub) && !empty($fechaCad) && $fechaCad <= $fechaPub) {
        $errores[] = "Fecha de caducidad debe ser posterior a la de publicación";
    }
    
    // Validar rango de edad
    $edadMin = $oferta['edad_minima'] ?? null;
    $edadMax = $oferta['edad_maxima'] ?? null;
    
    if ($edadMin !== null && $edadMin !== '' && intval($edadMin) < 18) {
        $errores[] = "Edad mínima debe ser ≥ 18";
    }
    if ($edadMin !== null && $edadMax !== null && $edadMin !== '' && $edadMax !== '' 
        && intval($edadMin) > intval($edadMax)) {
        $errores[] = "Edad mínima no puede ser mayor a la máxima";
    }
    
    // Validar experiencia
    $expAnios = $oferta['experiencia_anios'] ?? null;
    if ($expAnios !== null && $expAnios !== '' && intval($expAnios) < 0) {
        $errores[] = "Experiencia no puede ser negativa";
    }
    
    return $errores;
}

// ═══════════════════════════════════════════════════════════
// 4. DESCARGAR FORMATO XLSX
// ═══════════════════════════════════════════════════════════
function descargarFormato() {
    // Template XLSX pre-generado, codificado en base64 — sin dependencias
    $base64 = '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';

    $data = base64_decode($base64);
    if ($data === false) {
        header('Content-Type: text/plain; charset=utf-8');
        die("Error al decodificar el archivo XLSX");
    }

    header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    header('Content-Disposition: attachment; filename="formato_carga_ofertas.xlsx"');
    header('Content-Length: ' . strlen($data));
    echo $data;
    exit;
}
