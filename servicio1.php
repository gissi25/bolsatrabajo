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

    case 'empresas':
        $rs = $conn->query("SELECT NIT, NOMBRE_EMPRESA FROM EMPRESA ORDER BY NOMBRE_EMPRESA");
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    case 'grados':
        $rs = $conn->query("SELECT ID_GRADO_ACADEMICO, NOMBRE_GRADO FROM GRADO_ACADEMICO ORDER BY NOMBRE_GRADO");
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    case 'insertar_ofertas':
        if ($method !== 'POST') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $input = json_decode(file_get_contents('php://input'), true);
        $ofertas = $input['ofertas'] ?? [];

        if (empty($ofertas)) {
            http_response_code(400);
            die(json_encode(["error" => "No se enviaron ofertas"]));
        }

        $conn->begin_transaction();
        try {
            $stmtOferta = $conn->prepare("
                INSERT INTO OFERTA_TRABAJO 
                (NIT, ID_OFERTA, ID_GRADO_ACADEMICO, TITULO_PUESTO, FECHA_PUBLICACION, 
                 FECHA_CADUCIDAD, EXPERIENCIA_ANIOS, EDAD_MINIMA, EDAD_MAXIMA, 
                 DESCRIPCION_OFERTA_TRABAJO)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ");
            $stmtReq = $conn->prepare("
                INSERT INTO DETALLE_REQUISITO 
                (NIT, ID_OFERTA, ID_DETALLE, DESCRIPCION_REQUISITO)
                VALUES (?, ?, ?, ?)
            ");

            $insertadas = 0;
            $reqInsertados = 0;
            foreach ($ofertas as $of) {
                $nit = trim($of['nit'] ?? '');
                $idOferta = trim($of['id_oferta'] ?? '');
                $idGrado = !empty($of['id_grado']) ? intval($of['id_grado']) : null;
                $titulo = trim($of['titulo'] ?? '');
                $fechaPub = !empty($of['fecha_publicacion']) ? $of['fecha_publicacion'] : null;
                $fechaCad = !empty($of['fecha_caducidad']) ? $of['fecha_caducidad'] : null;
                $expAnios = !empty($of['experiencia_anios']) ? intval($of['experiencia_anios']) : null;
                $edadMin = !empty($of['edad_minima']) ? intval($of['edad_minima']) : null;
                $edadMax = !empty($of['edad_maxima']) ? intval($of['edad_maxima']) : null;
                $descripcion = trim($of['descripcion'] ?? '');
                $requisitos = $of['requisitos'] ?? [];

                if ($nit === '' || $idOferta === '' || $titulo === '') {
                    throw new Exception("Campos obligatorios faltantes en oferta: $idOferta");
                }

                $stmtOferta->bind_param("ssisssiiis", 
                    $nit, $idOferta, $idGrado, $titulo, $fechaPub,
                    $fechaCad, $expAnios, $edadMin, $edadMax, $descripcion
                );
                $stmtOferta->execute();
                $insertadas++;

                foreach ($requisitos as $req) {
                    $idDetalle = trim($req['id_detalle'] ?? '');
                    $descReq = trim($req['descripcion'] ?? '');
                    if ($idDetalle === '' || $descReq === '') continue;
                    $stmtReq->bind_param("ssss", $nit, $idOferta, $idDetalle, $descReq);
                    $stmtReq->execute();
                    $reqInsertados++;
                }
            }

            $conn->commit();
            echo json_encode([
                "exito" => true,
                "mensaje" => "$insertadas ofertas y $reqInsertados requisitos insertados",
                "insertadas" => $insertadas,
                "requisitos" => $reqInsertados
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
}

$conn->close();
