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

$vigenciaPorTipo = [1 => 5, 2 => 3, 3 => 3, 4 => 3, 5 => 1];

switch ($action) {

    case 'tipos_certificacion':
        $rs = $conn->query("SELECT ID_TIPO_CERTIFICACION, NOMBRE_TIPO FROM TIPO_CERTIFICACION ORDER BY NOMBRE_TIPO");
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        foreach ($data as &$tipo) {
            $tipo['DURACION_VIGENCIA_ANIOS'] = $vigenciaPorTipo[$tipo['ID_TIPO_CERTIFICACION']] ?? null;
        }
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    case 'sincronizar_certificaciones':
        if ($method !== 'POST') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }
        $input = json_decode(file_get_contents('php://input'), true);
        $postulantes = $input['postulantes'] ?? [];

        if (empty($postulantes)) {
            http_response_code(400);
            die(json_encode(["error" => "No se enviaron postulantes"]));
        }

        $conn->begin_transaction();
        try {
            $stmtPost = $conn->prepare("
                INSERT INTO POSTULANTE (ID_POSTULANTE, ID_GENERO, ID_TIPO_DOCUMENTO, NUM_DOCUMENTO,
                    ID_GRADO_ACADEMICO, NOMBRE, APELLIDO, FECHA_NACIMIENTO, NUP, EMAIL,
                    ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID,
                    DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    NOMBRE=VALUES(NOMBRE), APELLIDO=VALUES(APELLIDO), EMAIL=VALUES(EMAIL),
                    ID_GENERO=VALUES(ID_GENERO), ID_TIPO_DOCUMENTO=VALUES(ID_TIPO_DOCUMENTO),
                    ID_GRADO_ACADEMICO=VALUES(ID_GRADO_ACADEMICO), NUM_DOCUMENTO=VALUES(NUM_DOCUMENTO),
                    NUP=VALUES(NUP), FECHA_NACIMIENTO=VALUES(FECHA_NACIMIENTO),
                    ID_DISTRITO_DEPTO=VALUES(ID_DISTRITO_DEPTO),
                    ID_DISTRITO_MUNICIPIO=VALUES(ID_DISTRITO_MUNICIPIO),
                    ID_DISTRITO_ID=VALUES(ID_DISTRITO_ID),
                    DIRECCION_DETALLE=VALUES(DIRECCION_DETALLE),
                    TELEFONO_CASA=VALUES(TELEFONO_CASA),
                    TELEFONO_CELULAR=VALUES(TELEFONO_CELULAR)
            ");
            $stmtCert = $conn->prepare("
                INSERT INTO CERTIFICACION (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE,
                    ID_TIPO_CERTIFICACION, NOMBRE_CERTIFICACION, FECHA_CERTIFICACION, FECHA_INICIO, FECHA_FIN)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    NOMBRE_CERTIFICACION=VALUES(NOMBRE_CERTIFICACION),
                    ID_TIPO_CERTIFICACION=VALUES(ID_TIPO_CERTIFICACION),
                    FECHA_CERTIFICACION=VALUES(FECHA_CERTIFICACION),
                    FECHA_INICIO=VALUES(FECHA_INICIO),
                    FECHA_FIN=VALUES(FECHA_FIN)
            ");

            $nuevas = 0;
            $actualizadas = 0;
            foreach ($postulantes as $p) {
                $idPost = trim($p['id_postulante'] ?? '');
                $idGenero = isset($p['id_genero']) ? intval($p['id_genero']) : 1;
                $idTipoDoc = isset($p['id_tipo_documento']) ? intval($p['id_tipo_documento']) : 1;
                $numDoc = trim($p['num_documento'] ?? '');
                $idGrado = isset($p['id_grado_academico']) ? intval($p['id_grado_academico']) : 1;
                $nombre = trim($p['nombre'] ?? '');
                $apellido = trim($p['apellido'] ?? '');
                $fechaNac = !empty($p['fecha_nacimiento']) ? $p['fecha_nacimiento'] : null;
                $nup = trim($p['nup'] ?? '');
                $email = trim($p['email'] ?? '');
                $distDepto = isset($p['id_distrito_depto']) ? intval($p['id_distrito_depto']) : null;
                $distMuni = isset($p['id_distrito_municipio']) ? intval($p['id_distrito_municipio']) : null;
                $distId = isset($p['id_distrito_id']) ? intval($p['id_distrito_id']) : null;
                $direccion = trim($p['direccion'] ?? '');
                $telCasa = trim($p['telefono_casa'] ?? '');
                $telCel = trim($p['telefono_celular'] ?? '');
                if ($idPost === '' || $nombre === '' || $email === '') continue;

                $stmtPost->bind_param("siiissssssssssss",
                    $idPost, $idGenero, $idTipoDoc, $numDoc, $idGrado,
                    $nombre, $apellido, $fechaNac, $nup, $email,
                    $distDepto, $distMuni, $distId,
                    $direccion, $telCasa, $telCel
                );
                $stmtPost->execute();

                $certs = $p['certificaciones'] ?? [];
                foreach ($certs as $c) {
                    $idCert = trim($c['id_certificacion'] ?? '');
                    $idInst = trim($c['id_institucion'] ?? '');
                    $idTipoCert = intval($c['id_tipo_certificacion'] ?? 0);
                    $nomCert = trim($c['nombre'] ?? '');
                    $fechaCert = !empty($c['fecha_certificacion']) ? $c['fecha_certificacion'] : date('Y-m-d');
                    $fechaIni = !empty($c['fecha_inicio']) ? $c['fecha_inicio'] : date('Y-m-d', strtotime('-2 years'));
                    $fechaFin = !empty($c['fecha_fin']) ? $c['fecha_fin'] : date('Y-m-d', strtotime('-1 year'));

                    // Ajustar para cumplir trigger TR_CERTIFICACION_FECHAS
                    if ($fechaIni >= $fechaFin) {
                        $fechaFin = date('Y-m-d', strtotime($fechaIni . ' +1 day'));
                    }
                    if ($fechaCert < $fechaFin) {
                        $fechaCert = $fechaFin;
                    }
                    $maxCert = date('Y-m-d', strtotime($fechaFin . ' +1 year'));
                    if ($fechaCert > $maxCert) {
                        $fechaCert = $maxCert;
                    }

                    if ($idCert === '' || $idInst === '' || $idTipoCert <= 0 || $nomCert === '') continue;

                    $stmtCert->bind_param("sssissss",
                        $idCert, $idInst, $idPost, $idTipoCert, $nomCert,
                        $fechaCert, $fechaIni, $fechaFin
                    );
                    $stmtCert->execute();
                    $afectadas = $stmtCert->affected_rows;
                    if ($afectadas === 1) {
                        $nuevas++;
                    } elseif ($afectadas === 2) {
                        $actualizadas++;
                    }
                }
            }

            $conn->commit();
            echo json_encode([
                "exito" => true,
                "nuevas" => $nuevas,
                "actualizadas" => $actualizadas,
                "mensaje" => "$nuevas nueva(s), $actualizadas actualizada(s)"
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

    case 'buscar_certificaciones':
        $tipo = isset($_GET['tipo']) ? intval($_GET['tipo']) : 0;
        $nombre = isset($_GET['nombre']) ? trim($_GET['nombre']) : '';
        $vigente = isset($_GET['vigente']) ? trim($_GET['vigente']) : 'todos';

        if ($tipo <= 0) {
            http_response_code(400);
            die(json_encode(["error" => "El tipo de certificacion es obligatorio"]));
        }

        $sql = "
            SELECT p.ID_POSTULANTE, p.ID_GENERO, p.ID_TIPO_DOCUMENTO, p.NUM_DOCUMENTO,
                   p.ID_GRADO_ACADEMICO, p.NOMBRE, p.APELLIDO, p.EMAIL, p.FECHA_NACIMIENTO,
                   p.NUP, p.ID_DISTRITO_DEPTO, p.ID_DISTRITO_MUNICIPIO, p.ID_DISTRITO_ID,
                   p.DIRECCION_DETALLE, p.TELEFONO_CASA, p.TELEFONO_CELULAR,
                   ga.NOMBRE_GRADO,
                   c.ID_CERTIFICACION, c.ID_INSTITUCION, c.ID_TIPO_CERTIFICACION,
                   c.NOMBRE_CERTIFICACION, c.FECHA_CERTIFICACION, c.FECHA_INICIO, c.FECHA_FIN,
                   tc.NOMBRE_TIPO, i.NOMBRE_INSTITUCION
            FROM POSTULANTE p
            JOIN CERTIFICACION c ON p.ID_POSTULANTE = c.ID_POSTULANTE
            LEFT JOIN TIPO_CERTIFICACION tc ON c.ID_TIPO_CERTIFICACION = tc.ID_TIPO_CERTIFICACION
            LEFT JOIN INSTITUCION i ON c.ID_INSTITUCION = i.ID_INSTITUCION
            LEFT JOIN GRADO_ACADEMICO ga ON p.ID_GRADO_ACADEMICO = ga.ID_GRADO_ACADEMICO
            WHERE c.ID_TIPO_CERTIFICACION = ?
        ";
        $params = [$tipo];
        $types = "i";

        if (!empty($nombre)) {
            $sql .= " AND c.NOMBRE_CERTIFICACION LIKE ?";
            $params[] = "%$nombre%";
            $types .= "s";
        }

        $sql .= " ORDER BY p.ID_POSTULANTE, c.FECHA_CERTIFICACION DESC";

        $stmt = $conn->prepare($sql);
        if (!$stmt) {
            http_response_code(500);
            die(json_encode(["error" => "Error al preparar la consulta: " . $conn->error]));
        }

        $stmt->bind_param($types, ...$params);
        $stmt->execute();
        $rs = $stmt->get_result();

        $rows = $rs->fetch_all(MYSQLI_ASSOC);

        $hoy = new DateTime();
        $postulantes = [];
        foreach ($rows as $row) {
            $idTipo = intval($row['ID_TIPO_CERTIFICACION']);
            $aniosVigencia = $vigenciaPorTipo[$idTipo] ?? null;
            $fechaCert = $row['FECHA_CERTIFICACION'];

            $fechaVencimiento = null;
            $vigenteCert = null;
            if ($aniosVigencia !== null && $fechaCert) {
                $fv = new DateTime($fechaCert);
                $fv->modify("+$aniosVigencia years");
                $fechaVencimiento = $fv->format('Y-m-d');
                $vigenteCert = $fv >= $hoy;
            }

            if ($vigente === 'vigentes' && $vigenteCert !== true) continue;
            if ($vigente === 'vencidos' && $vigenteCert !== false) continue;

            $id = $row['ID_POSTULANTE'];
            if (!isset($postulantes[$id])) {
                $postulantes[$id] = [
                    "id_postulante" => $row['ID_POSTULANTE'],
                    "id_genero" => intval($row['ID_GENERO']),
                    "id_tipo_documento" => intval($row['ID_TIPO_DOCUMENTO']),
                    "id_grado_academico" => intval($row['ID_GRADO_ACADEMICO']),
                    "id_distrito_depto" => $row['ID_DISTRITO_DEPTO'] ? intval($row['ID_DISTRITO_DEPTO']) : null,
                    "id_distrito_municipio" => $row['ID_DISTRITO_MUNICIPIO'] ? intval($row['ID_DISTRITO_MUNICIPIO']) : null,
                    "id_distrito_id" => $row['ID_DISTRITO_ID'] ? intval($row['ID_DISTRITO_ID']) : null,
                    "nombre" => $row['NOMBRE'],
                    "apellido" => $row['APELLIDO'],
                    "email" => $row['EMAIL'],
                    "num_documento" => $row['NUM_DOCUMENTO'] ?? '',
                    "nup" => $row['NUP'] ?? '',
                    "fecha_nacimiento" => $row['FECHA_NACIMIENTO'] ?? '',
                    "direccion" => $row['DIRECCION_DETALLE'] ?? '',
                    "telefono_casa" => $row['TELEFONO_CASA'] ?? '',
                    "telefono_celular" => $row['TELEFONO_CELULAR'] ?? '',
                    "grado" => $row['NOMBRE_GRADO'] ?? '',
                    "certificaciones" => [],
                    "total_cert" => 0
                ];
            }
            $postulantes[$id]['certificaciones'][] = [
                "id_certificacion" => $row['ID_CERTIFICACION'],
                "id_institucion" => $row['ID_INSTITUCION'],
                "id_tipo_certificacion" => $idTipo,
                "nombre" => $row['NOMBRE_CERTIFICACION'],
                "tipo" => $row['NOMBRE_TIPO'],
                "fecha_certificacion" => $fechaCert,
                "fecha_inicio" => $row['FECHA_INICIO'],
                "fecha_fin" => $row['FECHA_FIN'],
                "anio" => $fechaCert ? substr($fechaCert, 0, 4) : '',
                "institucion" => $row['NOMBRE_INSTITUCION'] ?? '',
                "fecha_vencimiento" => $fechaVencimiento,
                "vigente" => $vigenteCert
            ];
            $postulantes[$id]['total_cert']++;
        }

        $postulantes = array_values($postulantes);

        usort($postulantes, function ($a, $b) {
            return $b['total_cert'] - $a['total_cert'];
        });

        echo json_encode([
            "exito" => true,
            "total" => count($postulantes),
            "data" => $postulantes
        ], JSON_UNESCAPED_UNICODE);
        break;
}

$conn->close();
