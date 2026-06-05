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

        $stmt = $conn->prepare("
            SELECT o.NIT, o.ID_OFERTA, o.TITULO_PUESTO,
                   o.EXPERIENCIA_ANIOS, o.DESCRIPCION_OFERTA_TRABAJO,
                   o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD,
                   o.EDAD_MINIMA, o.EDAD_MAXIMA,
                   e.NOMBRE_EMPRESA,
                   g.NOMBRE_GRADO AS GRADO_REQUERIDO
            FROM OFERTA_TRABAJO o
            LEFT JOIN EMPRESA e ON o.NIT = e.NIT
            LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
            WHERE o.EDAD_MINIMA <= ?
              AND o.EDAD_MAXIMA >= ?
              AND (o.FECHA_CADUCIDAD IS NULL OR o.FECHA_CADUCIDAD >= CURDATE())
            ORDER BY o.FECHA_PUBLICACION DESC
        ");
        $stmt->bind_param("ii", $edad, $edad);
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
}

$conn->close();
