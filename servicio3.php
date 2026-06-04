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

    case 'sincronizar_postulantes':
        if ($method !== 'POST') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $p = json_decode(file_get_contents('php://input'), true);
        if (!$p || empty($p['id_postulante'])) {
            http_response_code(400);
            die(json_encode(["error" => "Datos de postulante incompletos"]));
        }

        $idPost = trim($p['id_postulante']);
        $idGenero = intval($p['id_genero'] ?? 0);
        $idTipoDoc = intval($p['id_tipo_documento'] ?? 0);
        $numDoc = trim($p['num_documento'] ?? '');
        $idGrado = intval($p['id_grado_academico'] ?? 0);
        $nombre = trim($p['nombre'] ?? '');
        $apellido = trim($p['apellido'] ?? '');
        $fechaNac = !empty($p['fecha_nacimiento']) ? $p['fecha_nacimiento'] : null;
        $nup = trim($p['nup'] ?? '');
        $email = trim($p['email'] ?? '');
        $direccion = trim($p['direccion'] ?? '');
        $telCasa = trim($p['telefono_casa'] ?? '');
        $telCel = trim($p['telefono_celular'] ?? '');
        $distDepto = !empty($p['id_distrito_depto']) ? intval($p['id_distrito_depto']) : 'NULL';
        $distMuni = !empty($p['id_distrito_municipio']) ? intval($p['id_distrito_municipio']) : 'NULL';
        $distId = !empty($p['id_distrito_id']) ? intval($p['id_distrito_id']) : 'NULL';

        // Si hay distrito, asegurarse de que existe en MySQL, si no, crearlo
        if ($distDepto !== 'NULL' && $distMuni !== 'NULL' && $distId !== 'NULL') {
            $rsD = $conn->query("SELECT 1 FROM DISTRITO WHERE ID_DEPARTAMENTO=$distDepto AND ID_MUNICIPIO=$distMuni AND ID_DISTRITO=$distId");
            if (!$rsD || !$rsD->fetch_row()) {
                $deptoNombre = $conn->real_escape_string($p['depto_nombre'] ?? 'Distrito '.$distDepto);
                $muniNombre = $conn->real_escape_string($p['municipio_nombre'] ?? 'Municipio '.$distMuni);
                $distNombre = $conn->real_escape_string($p['distrito_nombre'] ?? 'Distrito '.$distId);
                // Crear departamento si no existe
                $conn->query("INSERT IGNORE INTO DEPARTAMENTO (ID_DEPARTAMENTO, NOMBRE_DEPARTAMENTO) VALUES ($distDepto, '$deptoNombre')");
                // Crear municipio si no existe
                $conn->query("INSERT IGNORE INTO MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_MUNICIPIO) VALUES ($distDepto, $distMuni, '$muniNombre')");
                // Crear distrito
                $conn->query("INSERT IGNORE INTO DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO, NOMBRE_DISTRITO) VALUES ($distDepto, $distMuni, $distId, '$distNombre')");
            }
        }

        $errores = [];

        // Validaciones
        if ($fechaNac && $fechaNac > date('Y-m-d')) $errores[] = "fecha nacimiento futura";
        if ($fechaNac && date_diff(date_create($fechaNac), date_create('today'))->y < 18) $errores[] = "menor de edad";
        if ($idGrado > 0) {
            $rsG = $conn->query("SELECT LOWER(NOMBRE_GRADO) FROM GRADO_ACADEMICO WHERE ID_GRADO_ACADEMICO = $idGrado");
            if ($rsG && $row = $rsG->fetch_row()) {
                if ($row[0] === 'bachiller') $errores[] = "grado Bachiller no permitido";
            } else $errores[] = "grado $idGrado no existe";
        }
        if ($idGenero > 0) {
            $rs = $conn->query("SELECT 1 FROM GENERO WHERE ID_GENERO = $idGenero");
            if (!$rs || !$rs->fetch_row()) $errores[] = "genero $idGenero no existe";
        }
        if ($idTipoDoc > 0) {
            $rs = $conn->query("SELECT 1 FROM TIPO_DOCUMENTO WHERE ID_TIPO_DOCUMENTO = $idTipoDoc");
            if (!$rs || !$rs->fetch_row()) $errores[] = "tipo_documento $idTipoDoc no existe";
        }

        if (!empty($errores)) {
            http_response_code(400);
            die(json_encode(["exito" => false, "error" => "$idPost: " . implode(", ", $errores)]));
        }

        $conn->begin_transaction();
        try {
            $rs = $conn->query("SELECT 1 FROM POSTULANTE WHERE ID_POSTULANTE = '$idPost'");
            $existe = $rs && $rs->fetch_row();

            if ($existe) {
                $conn->query("UPDATE POSTULANTE SET 
                    ID_GENERO=$idGenero, ID_TIPO_DOCUMENTO=$idTipoDoc, NUM_DOCUMENTO='$numDoc',
                    ID_GRADO_ACADEMICO=$idGrado, NOMBRE='$nombre', APELLIDO='$apellido',
                    FECHA_NACIMIENTO=" . ($fechaNac ? "'$fechaNac'" : "NULL") . ",
                    NUP='$nup', EMAIL='$email', DIRECCION_DETALLE='$direccion',
                    TELEFONO_CASA='$telCasa', TELEFONO_CELULAR='$telCel',
                    ID_DISTRITO_DEPTO=$distDepto, ID_DISTRITO_MUNICIPIO=$distMuni, ID_DISTRITO_ID=$distId
                    WHERE ID_POSTULANTE='$idPost'");
                $accion = "actualizado";

                foreach (['FORMACION_ACADEMICA','EXPERIENCIA_LABORAL','CERTIFICACION','HABILIDAD_POSTULANTE','RED_SOCIAL_POSTULANTE'] as $t) {
                    $conn->query("DELETE FROM $t WHERE ID_POSTULANTE='$idPost'");
                }
            } else {
                $conn->query("INSERT INTO POSTULANTE 
                    (ID_POSTULANTE,ID_GENERO,ID_TIPO_DOCUMENTO,NUM_DOCUMENTO,ID_GRADO_ACADEMICO,NOMBRE,APELLIDO,FECHA_NACIMIENTO,NUP,EMAIL,DIRECCION_DETALLE,TELEFONO_CASA,TELEFONO_CELULAR,ID_DISTRITO_DEPTO,ID_DISTRITO_MUNICIPIO,ID_DISTRITO_ID) VALUES (
                    '$idPost',$idGenero,$idTipoDoc,'$numDoc',$idGrado,'$nombre','$apellido',"
                    . ($fechaNac ? "'$fechaNac'" : "NULL") . ",'$nup','$email','$direccion','$telCasa','$telCel',$distDepto,$distMuni,$distId)");
                $accion = "insertado";
            }

            // Insertar hijas
            $tablas = [
                'formaciones' => ['tabla' => 'FORMACION_ACADEMICA', 'cols' => ['ID_FORMACION','ID_OFERTA_ACADEMICA','TITULO_OBTENIDO','FECHA_INICIO','FECHA_FIN','FECHA_OBTENCION'], 'keys' => ['id_formacion','id_oferta_academica','titulo_obtenido','fecha_inicio','fecha_fin','fecha_obtencion']],
                'experiencias' => ['tabla' => 'EXPERIENCIA_LABORAL', 'cols' => ['NIT','ID_EXPERIENCIA','PUESTO_TRABAJO','FECHA_INICIO','FECHA_FIN','DESCP_EXPERIENCIA_LABORAL','CONTACTO_REFERENCIA'], 'keys' => ['nit','id_experiencia','puesto_trabajo','fecha_inicio','fecha_fin','descripcion','contacto']],
                'certificaciones' => ['tabla' => 'CERTIFICACION', 'cols' => ['ID_CERTIFICACION','ID_INSTITUCION','ID_TIPO_CERTIFICACION','NOMBRE_CERTIFICACION','FECHA_CERTIFICACION','FECHA_INICIO','FECHA_FIN'], 'keys' => ['id_certificacion','id_institucion','id_tipo_certificacion','nombre_certificacion','fecha_certificacion','fecha_inicio','fecha_fin']],
                'habilidades' => ['tabla' => 'HABILIDAD_POSTULANTE', 'cols' => ['ID_CATEGORIA_HABILIDAD','ID_HABILIDAD','NIVEL_DESTREZA'], 'keys' => ['id_categoria_habilidad','id_habilidad','nivel_destreza']],
                'redes' => ['tabla' => 'RED_SOCIAL_POSTULANTE', 'cols' => ['ID_RED_SOCIAL','URL_PERFIL'], 'keys' => ['id_red_social','url_perfil']]
            ];

            foreach ($tablas as $key => $cfg) {
                $items = $p[$key] ?? [];
                if (empty($items)) continue;
                $cols = $cfg['cols'];
                $keys = $cfg['keys'];
                $tabla = $cfg['tabla'];
                $tipoCols = "ID_POSTULANTE," . implode(",", $cols);
                $placeholders = "?," . implode(",", array_fill(0, count($cols), "?"));
                $sql = "INSERT INTO $tabla ($tipoCols) VALUES ($placeholders)";
                $stmt = $conn->prepare($sql);
                $tipos = str_repeat("s", count($cols) + 1);
                foreach ($items as $item) {
                    $vals = [$idPost];
                    foreach ($keys as $k) $vals[] = $item[$k] ?? '';
                    @$stmt->bind_param($tipos, ...$vals);
                    @$stmt->execute();
                }
            }

            $conn->commit();
            echo json_encode(["exito" => true, "id_postulante" => $idPost, "accion" => $accion], JSON_UNESCAPED_UNICODE);
        } catch (Exception $e) {
            $conn->rollback();
            http_response_code(400);
            echo json_encode(["exito" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        }
        break;
}

$conn->close();
