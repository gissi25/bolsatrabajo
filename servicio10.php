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

    case 'mis_postulaciones':
        $input = json_decode(file_get_contents("php://input"), true);
        $idPostulante = $input['id_postulante'] ?? ($_GET['id_postulante'] ?? '');
        if (!$idPostulante) {
            echo json_encode(["error" => "id_postulante es requerido"]);
            break;
        }

        $stmt = $conn->prepare("
            SELECT po.ID_POSTULACION, po.FECHA_APLICACION, po.ESTADO_PROCESO,
                   of.TITULO_PUESTO, of.FECHA_PUBLICACION, of.FECHA_CADUCIDAD,
                   em.NOMBRE_EMPRESA, em.NIT, of.ID_OFERTA
            FROM POSTULACION po
            INNER JOIN OFERTA_TRABAJO of ON po.NIT = of.NIT AND po.ID_OFERTA = of.ID_OFERTA
            INNER JOIN EMPRESA em ON of.NIT = em.NIT
            WHERE po.ID_POSTULANTE = ?
            ORDER BY po.FECHA_APLICACION DESC
        ");
        $stmt->bind_param("s", $idPostulante);
        $stmt->execute();
        $res = $stmt->get_result();
        $data = [];
        while ($row = $res->fetch_assoc()) {
            $data[] = $row;
        }
        $stmt->close();

        echo json_encode(["exito" => true, "data" => $data]);
        break;

    default:
        echo json_encode(["error" => "Accion no valida para servicio 10"]);
}

$conn->close();
