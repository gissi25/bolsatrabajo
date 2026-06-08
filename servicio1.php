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
