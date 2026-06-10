<?php
ini_set('display_errors', '0');
error_reporting(0);
header('Content-Type: application/json; charset=utf-8');

$host = 'sql303.infinityfree.com';
$user = 'if0_42097646';
$pass = 'vBl5vfa4CsPNjUD';
$db   = 'if0_42097646_bolsadetrabajo';

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    http_response_code(500);
    die(json_encode(["error" => "Error de conexion: " . $conn->connect_error], JSON_UNESCAPED_UNICODE));
}
$conn->set_charset("utf8");

$method = $_SERVER['REQUEST_METHOD'];

switch ($action) {

    case 'ofertas_por_edad':
        if ($method !== 'GET') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"], JSON_UNESCAPED_UNICODE));
        }

        $edad = intval($_GET['edad'] ?? 0);
        if ($edad <= 0) {
            http_response_code(400);
            die(json_encode(["error" => "Edad invalida"], JSON_UNESCAPED_UNICODE));
        }

        $idPostulante = trim($_GET['id_postulante'] ?? '');

        $sql = "
            SELECT o.NIT, o.ID_OFERTA, o.TITULO_PUESTO,
                   o.EXPERIENCIA_ANIOS, o.DESCRIPCION_OFERTA_TRABAJO,
                   o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD,
                   o.EDAD_MINIMA, o.EDAD_MAXIMA,
                   e.NOMBRE_EMPRESA,
                   g.NOMBRE_GRADO AS GRADO_REQUERIDO
        ";

        if ($idPostulante !== '') {
            $idSafe = $conn->real_escape_string($idPostulante);
            $sql .= ",
                   p.ESTADO_PROCESO AS ESTADO_POSTULACION
                FROM OFERTA_TRABAJO o
                LEFT JOIN EMPRESA e ON o.NIT = e.NIT
                LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
                LEFT JOIN POSTULACION p
                    ON o.NIT = p.NIT
                   AND o.ID_OFERTA = p.ID_OFERTA
                   AND p.ID_POSTULANTE = '$idSafe'
                WHERE o.EDAD_MINIMA <= $edad
                  AND o.EDAD_MAXIMA >= $edad
                  AND (o.FECHA_CADUCIDAD IS NULL OR o.FECHA_CADUCIDAD >= CURDATE())
                ORDER BY o.FECHA_PUBLICACION DESC
            ";
        } else {
            $sql .= "
                FROM OFERTA_TRABAJO o
                LEFT JOIN EMPRESA e ON o.NIT = e.NIT
                LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
                WHERE o.EDAD_MINIMA <= $edad
                  AND o.EDAD_MAXIMA >= $edad
                  AND (o.FECHA_CADUCIDAD IS NULL OR o.FECHA_CADUCIDAD >= CURDATE())
                ORDER BY o.FECHA_PUBLICACION DESC
            ";
        }

        $rs = $conn->query($sql);
        if (!$rs) {
            http_response_code(500);
            die(json_encode(["error" => "Error en consulta: " . $conn->error], JSON_UNESCAPED_UNICODE));
        }

        $data = $rs->fetch_all(MYSQLI_ASSOC);

        echo json_encode([
            "exito" => true,
            "edad_ingresada" => $edad,
            "total_ofertas" => count($data),
            "data" => $data
        ], JSON_UNESCAPED_UNICODE);
        break;

    case 'postular':
        if ($method === 'GET') {
            $idPostulante = trim($_GET['id_postulante'] ?? '');
            $nit = trim($_GET['nit'] ?? '');
            $idOferta = trim($_GET['id_oferta'] ?? '');
        } elseif ($method === 'POST') {
            $input = json_decode(file_get_contents('php://input'), true);
            if (!is_array($input)) {
                http_response_code(400);
                die(json_encode(["error" => "JSON invalido en el cuerpo de la peticion"], JSON_UNESCAPED_UNICODE));
            }
            $idPostulante = trim($input['id_postulante'] ?? '');
            $nit = trim($input['nit'] ?? '');
            $idOferta = trim($input['id_oferta'] ?? '');
        } else {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"], JSON_UNESCAPED_UNICODE));
        }

        if ($idPostulante === '' || $nit === '' || $idOferta === '') {
            http_response_code(400);
            die(json_encode(["error" => "Faltan datos: id_postulante, nit, id_oferta"], JSON_UNESCAPED_UNICODE));
        }

        $nitSafe = $conn->real_escape_string($nit);
        $idOfertaSafe = $conn->real_escape_string($idOferta);
        $idPostulanteSafe = $conn->real_escape_string($idPostulante);

        $rs = $conn->query("
            SELECT 1 FROM OFERTA_TRABAJO
            WHERE NIT = '$nitSafe'
              AND ID_OFERTA = '$idOfertaSafe'
              AND (FECHA_CADUCIDAD IS NULL OR FECHA_CADUCIDAD >= CURDATE())
        ");
        if (!$rs || !$rs->fetch_row()) {
            http_response_code(400);
            die(json_encode(["error" => "La oferta no existe o ha caducado"], JSON_UNESCAPED_UNICODE));
        }

        $rs = $conn->query("SELECT 1 FROM POSTULANTE WHERE ID_POSTULANTE = '$idPostulanteSafe' LIMIT 1");
        if (!$rs || !$rs->fetch_row()) {
            http_response_code(400);
            die(json_encode([
                "error" => "El postulante '$idPostulante' no existe. Debe estar registrado en la tabla POSTULANTE."
            ], JSON_UNESCAPED_UNICODE));
        }

        $rs = $conn->query("
            SELECT 1 FROM POSTULACION
            WHERE ID_POSTULANTE = '$idPostulanteSafe'
              AND NIT = '$nitSafe'
              AND ID_OFERTA = '$idOfertaSafe'
            LIMIT 1
        ");
        if ($rs && $rs->fetch_row()) {
            http_response_code(400);
            die(json_encode(["error" => "Ya has postulado a esta oferta"], JSON_UNESCAPED_UNICODE));
        }

        $idPostulacion = null;
        $rs = $conn->query("SELECT ID_POSTULACION FROM POSTULACION ORDER BY ID_POSTULACION DESC LIMIT 1");
        if ($rs && $row = $rs->fetch_row()) {
            $ultimo = $row[0];
            if (preg_match('/(\d+)$/', $ultimo, $m)) {
                $idPostulacion = 'POS' . str_pad(((int)$m[1]) + 1, 3, '0', STR_PAD_LEFT);
            }
        }
        if ($idPostulacion === null) {
            $idPostulacion = 'POS001';
        }

        $idPostulacionSafe = $conn->real_escape_string($idPostulacion);
        $ok = $conn->query("
            INSERT INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO)
            VALUES ('$idPostulacionSafe', '$nitSafe', '$idOfertaSafe', '$idPostulanteSafe', CURDATE(), 'en proceso')
        ");

        if (!$ok) {
            http_response_code(400);
            die(json_encode(["error" => "Error al postular: " . $conn->error], JSON_UNESCAPED_UNICODE));
        }

        echo json_encode([
            "exito" => true,
            "mensaje" => "Postulacion exitosa",
            "id_postulacion" => $idPostulacion,
            "estado" => "en proceso"
        ], JSON_UNESCAPED_UNICODE);
        break;

    default:
        http_response_code(400);
        die(json_encode(["error" => "Accion no reconocida en servicio 7"], JSON_UNESCAPED_UNICODE));
}

$conn->close();
