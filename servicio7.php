<?php
$host = 'sql303.infinityfree.com';
$user = 'if0_42097646';
$pass = 'vBl5vfa4CsPNjUD';
$db   = 'if0_42097646_bolsadetrabajo';

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    die(json_encode(["error" => "Error de conexion: " . $conn->connect_error]));
}
$conn->set_charset("utf8");

$method = $_SERVER['REQUEST_METHOD'];

switch ($action) {

    case 'ofertas_por_edad':
        if ($method !== 'GET') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $edad = intval($_GET['edad'] ?? 0);
        if ($edad <= 0) {
            http_response_code(400);
            die(json_encode(["error" => "Edad invalida"]));
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
            $sql .= ", p.ESTADO_PROCESO AS ESTADO_POSTULACION
                FROM OFERTA_TRABAJO o
                LEFT JOIN EMPRESA e ON o.NIT = e.NIT
                LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
                LEFT JOIN POSTULACION p ON o.NIT = p.NIT AND o.ID_OFERTA = p.ID_OFERTA AND p.ID_POSTULANTE = ?
                WHERE o.EDAD_MINIMA <= ?
                  AND o.EDAD_MAXIMA >= ?
                  AND (o.FECHA_CADUCIDAD IS NULL OR o.FECHA_CADUCIDAD >= CURDATE())
                ORDER BY o.FECHA_PUBLICACION DESC
            ";
            $stmt = $conn->prepare($sql);
            $stmt->bind_param("sii", $idPostulante, $edad, $edad);
        } else {
            $sql .= "
                FROM OFERTA_TRABAJO o
                LEFT JOIN EMPRESA e ON o.NIT = e.NIT
                LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
                WHERE o.EDAD_MINIMA <= ?
                  AND o.EDAD_MAXIMA >= ?
                  AND (o.FECHA_CADUCIDAD IS NULL OR o.FECHA_CADUCIDAD >= CURDATE())
                ORDER BY o.FECHA_PUBLICACION DESC
            ";
            $stmt = $conn->prepare($sql);
            $stmt->bind_param("ii", $edad, $edad);
        }
        $stmt->execute();
        $rs = $stmt->get_result();
        $data = $rs->fetch_all(MYSQLI_ASSOC);

        echo json_encode([
            "exito" => true,
            "edad_ingresada" => $edad,
            "total_ofertas" => count($data),
            "data" => $data
        ], JSON_UNESCAPED_UNICODE);
        break;

    case 'postular':
        if ($method !== 'POST') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $input = json_decode(file_get_contents('php://input'), true);
        $idPostulante = trim($input['id_postulante'] ?? '');
        $nit = trim($input['nit'] ?? '');
        $idOferta = trim($input['id_oferta'] ?? '');

        if ($idPostulante === '' || $nit === '' || $idOferta === '') {
            http_response_code(400);
            die(json_encode(["error" => "Faltan datos: id_postulante, nit, id_oferta"]));
        }

        // Verificar que la oferta existe y no ha caducado
        $rs = $conn->query("SELECT 1 FROM OFERTA_TRABAJO WHERE NIT='$nit' AND ID_OFERTA='$idOferta' AND (FECHA_CADUCIDAD IS NULL OR FECHA_CADUCIDAD >= CURDATE())");
        if (!$rs || !$rs->fetch_row()) {
            http_response_code(400);
            die(json_encode(["error" => "La oferta no existe o ha caducado"]));
        }

        // Verificar que no exista ya una postulación duplicada
        $rs = $conn->query("SELECT 1 FROM POSTULACION WHERE ID_POSTULANTE='$idPostulante' AND NIT='$nit' AND ID_OFERTA='$idOferta'");
        if ($rs && $rs->fetch_row()) {
            http_response_code(400);
            die(json_encode(["error" => "Ya has postulado a esta oferta"]));
        }

        // Generar ID_POSTULACION formato POS###
        $rs = $conn->query("SELECT COALESCE(MAX(CAST(REPLACE(ID_POSTULACION, 'POS', '') AS UNSIGNED)), 0) FROM POSTULACION");
        $row = $rs->fetch_row();
        $idPostulacion = 'POS' . str_pad($row[0] + 1, 3, '0', STR_PAD_LEFT);

        $stmt = $conn->prepare("
            INSERT INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO)
            VALUES (?, ?, ?, ?, CURDATE(), 'En Proceso')
        ");
        $stmt->bind_param("ssss", $idPostulacion, $nit, $idOferta, $idPostulante);
        $stmt->execute();

        echo json_encode([
            "exito" => true,
            "mensaje" => "Postulación exitosa",
            "id_postulacion" => $idPostulacion,
            "estado" => "En Proceso"
        ], JSON_UNESCAPED_UNICODE);
        break;
}

$conn->close();
