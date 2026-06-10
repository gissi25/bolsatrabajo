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

    case 'subir_postulaciones':
        if ($method !== 'POST') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $input = json_decode(file_get_contents('php://input'), true);
        $postulaciones = $input['postulaciones'] ?? [];

        if (empty($postulaciones)) {
            http_response_code(400);
            die(json_encode(["error" => "No se enviaron postulaciones"]));
        }

        $conn->begin_transaction();
        try {
            $stmt = $conn->prepare("
                INSERT INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    FECHA_APLICACION = VALUES(FECHA_APLICACION),
                    ESTADO_PROCESO = VALUES(ESTADO_PROCESO)
            ");

            $insertadas = 0;
            $actualizadas = 0;
            foreach ($postulaciones as $p) {
                $idPostulacion = trim($p['id_postulacion'] ?? '');
                $nit = trim($p['nit'] ?? '');
                $idOferta = trim($p['id_oferta'] ?? '');
                $idPostulante = trim($p['id_postulante'] ?? '');
                $fecha = !empty($p['fecha_aplicacion']) ? $p['fecha_aplicacion'] : null;
                $estado = !empty($p['estado_proceso']) ? $p['estado_proceso'] : 'pendiente';

                if ($idPostulacion === '' || $nit === '' || $idOferta === '' || $idPostulante === '') {
                    continue;
                }

                $stmt->bind_param("ssssss", $idPostulacion, $nit, $idOferta, $idPostulante, $fecha, $estado);
                $stmt->execute();

                if ($stmt->affected_rows > 0) {
                    if ($stmt->affected_rows === 2) {
                        $actualizadas++;
                    } else {
                        $insertadas++;
                    }
                }
            }

            $conn->commit();
            echo json_encode([
                "exito" => true,
                "mensaje" => "$insertadas insertadas, $actualizadas actualizadas",
                "insertadas" => $insertadas,
                "actualizadas" => $actualizadas
            ], JSON_UNESCAPED_UNICODE);
        } catch (Exception $e) {
            $conn->rollback();
            http_response_code(400);
            echo json_encode([
                "exito" => false,
                "error" => "Error: " . $e->getMessage()
            ], JSON_UNESCAPED_UNICODE);
        }
        break;

    case 'resumen_reclutamiento':
        if ($method !== 'GET') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $nit = trim($_GET['nit'] ?? '');
        if ($nit === '') {
            http_response_code(400);
            die(json_encode(["error" => "NIT requerido"]));
        }

        $activas = 0;
        $vencidas = 0;
        $totalPostulaciones = 0;
        $promedio = 0;

        $rs = $conn->query("SELECT COUNT(*) FROM OFERTA_TRABAJO WHERE NIT = '$nit' AND (FECHA_CADUCIDAD IS NULL OR FECHA_CADUCIDAD >= CURDATE())");
        if ($rs && $row = $rs->fetch_row()) $activas = $row[0];

        $rs = $conn->query("SELECT COUNT(*) FROM OFERTA_TRABAJO WHERE NIT = '$nit' AND FECHA_CADUCIDAD IS NOT NULL AND FECHA_CADUCIDAD < CURDATE()");
        if ($rs && $row = $rs->fetch_row()) $vencidas = $row[0];

        $rs = $conn->query("
            SELECT COUNT(*), COUNT(DISTINCT CONCAT(p.NIT, p.ID_OFERTA))
            FROM POSTULACION p
            JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA
            WHERE o.NIT = '$nit'
        ");
        if ($rs && $row = $rs->fetch_row()) {
            $totalPostulaciones = $row[0];
            $totalOfertasConPostulaciones = $row[1];
            $promedio = $totalOfertasConPostulaciones > 0 ? round($totalPostulaciones / $totalOfertasConPostulaciones, 1) : 0;
        }

        echo json_encode([
            "exito" => true,
            "nit" => $nit,
            "ofertas_activas" => $activas,
            "ofertas_vencidas" => $vencidas,
            "total_postulaciones" => $totalPostulaciones,
            "promedio_por_oferta" => $promedio
        ], JSON_UNESCAPED_UNICODE);
        break;

    case 'ranking_ofertas':
        if ($method !== 'GET') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $nit = trim($_GET['nit'] ?? '');
        if ($nit === '') {
            http_response_code(400);
            die(json_encode(["error" => "NIT requerido"]));
        }

        $rs = $conn->query("
            SELECT o.ID_OFERTA, o.TITULO_PUESTO, o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD,
                   COUNT(p.ID_POSTULACION) AS total_postulaciones
            FROM OFERTA_TRABAJO o
            LEFT JOIN POSTULACION p ON o.NIT = p.NIT AND o.ID_OFERTA = p.ID_OFERTA
            WHERE o.NIT = '$nit'
            GROUP BY o.NIT, o.ID_OFERTA
            ORDER BY total_postulaciones DESC
        ");
        $data = $rs ? $rs->fetch_all(MYSQLI_ASSOC) : [];

        echo json_encode(["exito" => true, "nit" => $nit, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    case 'postulantes_por_estado':
        if ($method !== 'GET') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $nit = trim($_GET['nit'] ?? '');
        if ($nit === '') {
            http_response_code(400);
            die(json_encode(["error" => "NIT requerido"]));
        }

        $rs = $conn->query("
            SELECT p.ESTADO_PROCESO, COUNT(*) AS total
            FROM POSTULACION p
            JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA
            WHERE o.NIT = '$nit'
            GROUP BY p.ESTADO_PROCESO
            ORDER BY total DESC
        ");
        $data = $rs ? $rs->fetch_all(MYSQLI_ASSOC) : [];

        echo json_encode(["exito" => true, "nit" => $nit, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    case 'dashboard_empresa':
        if ($method !== 'GET') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $nit = trim($_GET['nit'] ?? '');
        if ($nit === '') {
            http_response_code(400);
            die(json_encode(["error" => "NIT requerido"]));
        }

        $activas = 0;
        $vencidas = 0;
        $totalPostulaciones = 0;
        $promedio = 0;

        $rs = $conn->query("SELECT COUNT(*) FROM OFERTA_TRABAJO WHERE NIT = '$nit' AND (FECHA_CADUCIDAD IS NULL OR FECHA_CADUCIDAD >= CURDATE())");
        if ($rs && $row = $rs->fetch_row()) $activas = (int)$row[0];

        $rs = $conn->query("SELECT COUNT(*) FROM OFERTA_TRABAJO WHERE NIT = '$nit' AND FECHA_CADUCIDAD IS NOT NULL AND FECHA_CADUCIDAD < CURDATE()");
        if ($rs && $row = $rs->fetch_row()) $vencidas = (int)$row[0];

        $rs = $conn->query("
            SELECT COUNT(*), COUNT(DISTINCT CONCAT(p.NIT, p.ID_OFERTA))
            FROM POSTULACION p
            JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA
            WHERE o.NIT = '$nit'
        ");
        if ($rs && $row = $rs->fetch_row()) {
            $totalPostulaciones = (int)$row[0];
            $totalOfertasConPostulaciones = (int)$row[1];
            $promedio = $totalOfertasConPostulaciones > 0 ? round($totalPostulaciones / $totalOfertasConPostulaciones, 1) : 0;
        }

        $rs = $conn->query("
            SELECT o.ID_OFERTA, o.TITULO_PUESTO, o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD,
                   COUNT(p.ID_POSTULACION) AS total_postulaciones
            FROM OFERTA_TRABAJO o
            LEFT JOIN POSTULACION p ON o.NIT = p.NIT AND o.ID_OFERTA = p.ID_OFERTA
            WHERE o.NIT = '$nit'
            GROUP BY o.NIT, o.ID_OFERTA
            ORDER BY total_postulaciones DESC
        ");
        $ranking = $rs ? $rs->fetch_all(MYSQLI_ASSOC) : [];

        $rs = $conn->query("
            SELECT p.ESTADO_PROCESO, COUNT(*) AS total
            FROM POSTULACION p
            JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA
            WHERE o.NIT = '$nit'
            GROUP BY p.ESTADO_PROCESO
            ORDER BY total DESC
        ");
        $estados = $rs ? $rs->fetch_all(MYSQLI_ASSOC) : [];

        echo json_encode([
            "exito" => true,
            "nit" => $nit,
            "ofertas_activas" => $activas,
            "ofertas_vencidas" => $vencidas,
            "total_postulaciones" => $totalPostulaciones,
            "promedio_por_oferta" => $promedio,
            "ranking" => $ranking,
            "estados" => $estados
        ], JSON_UNESCAPED_UNICODE);
        break;
}

$conn->close();
