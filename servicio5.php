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

        // ============================================================
        // CATALOGOS: todas las tablas catalogo en un solo JSON
        // ============================================================
        case 'catalogos':
            $res = [
                "exito" => true,
                "categoria_habilidad" => [],
                "genero" => [],
                "tipo_documento" => [],
                "departamento" => [],
                "municipio" => [],
                "distrito" => [],
                "institucion" => [],
                "grado_academico" => [],
                "red_social" => [],
                "tipo_certificacion" => [],
                "habilidad" => [],
                "oferta_academica" => []
            ];

            $rs = $conn->query("SELECT ID_CATEGORIA_HABILIDAD, NOMBRE_CATEGORIA FROM CATEGORIA_HABILIDAD ORDER BY NOMBRE_CATEGORIA");
            $res["categoria_habilidad"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_GENERO, NOMBRE_GENERO FROM GENERO ORDER BY NOMBRE_GENERO");
            $res["genero"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_TIPO_DOCUMENTO, NOMBRE_TIPO FROM TIPO_DOCUMENTO ORDER BY NOMBRE_TIPO");
            $res["tipo_documento"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_DEPARTAMENTO, NOMBRE_DEPARTAMENTO FROM DEPARTAMENTO ORDER BY NOMBRE_DEPARTAMENTO");
            $res["departamento"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_MUNICIPIO FROM MUNICIPIO ORDER BY NOMBRE_MUNICIPIO");
            $res["municipio"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_DISTRITO, NOMBRE_DISTRITO, ID_DEPARTAMENTO, ID_MUNICIPIO FROM DISTRITO ORDER BY NOMBRE_DISTRITO");
            $res["distrito"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_INSTITUCION, NOMBRE_INSTITUCION FROM INSTITUCION ORDER BY NOMBRE_INSTITUCION");
            $res["institucion"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_GRADO_ACADEMICO, NOMBRE_GRADO FROM GRADO_ACADEMICO ORDER BY NOMBRE_GRADO");
            $res["grado_academico"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_RED_SOCIAL, NOMBRE_RED FROM RED_SOCIAL ORDER BY NOMBRE_RED");
            $res["red_social"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT ID_TIPO_CERTIFICACION, NOMBRE_TIPO FROM TIPO_CERTIFICACION ORDER BY NOMBRE_TIPO");
            $res["tipo_certificacion"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT h.ID_CATEGORIA_HABILIDAD, h.ID_HABILIDAD, h.NOMBRE_HABILIDAD FROM HABILIDAD h ORDER BY h.NOMBRE_HABILIDAD");
            $res["habilidad"] = $rs->fetch_all(MYSQLI_ASSOC);

            $rs = $conn->query("SELECT oa.ID_OFERTA_ACADEMICA, oa.ID_GRADO_ACADEMICO, oa.ID_INSTITUCION FROM OFERTA_ACADEMICA oa ORDER BY oa.ID_OFERTA_ACADEMICA");
            $res["oferta_academica"] = $rs->fetch_all(MYSQLI_ASSOC);

            echo json_encode($res, JSON_UNESCAPED_UNICODE);
            break;

        // ============================================================
        // EMPRESAS: todas las empresas con info de distrito
        // ============================================================
        case 'empresas_full':
            $rs = $conn->query("
                SELECT e.NIT, e.ID_DISTRITO_DEPTO, e.ID_DISTRITO_MUNICIPIO, e.ID_DISTRITO_ID,
                       e.NOMBRE_EMPRESA, e.CONTACTO_DIRECTO
                FROM EMPRESA e
                ORDER BY e.NOMBRE_EMPRESA
            ");
            $data = $rs->fetch_all(MYSQLI_ASSOC);
            echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
            break;

        // ============================================================
        // OFERTAS: todas las ofertas con sus requisitos anidados
        // ============================================================
        case 'ofertas_full':
            $ofertas = [];
            $rsO = $conn->query("
                SELECT o.NIT, o.ID_OFERTA, o.ID_GRADO_ACADEMICO, o.TITULO_PUESTO,
                       o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, o.EXPERIENCIA_ANIOS,
                       o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO
                FROM OFERTA_TRABAJO o
                ORDER BY o.FECHA_PUBLICACION DESC
            ");
            while ($row = $rsO->fetch_assoc()) {
                $nit = $conn->real_escape_string($row['NIT']);
                $idOferta = $conn->real_escape_string($row['ID_OFERTA']);

                $rsR = $conn->query("
                    SELECT ID_DETALLE, DESCRIPCION_REQUISITO
                    FROM DETALLE_REQUISITO
                    WHERE NIT = '$nit' AND ID_OFERTA = '$idOferta'
                    ORDER BY ID_DETALLE
                ");
                $requisitos = $rsR->fetch_all(MYSQLI_ASSOC);
                $row['requisitos'] = $requisitos;
                $ofertas[] = $row;
            }
            echo json_encode(["exito" => true, "data" => $ofertas], JSON_UNESCAPED_UNICODE);
            break;

        // ============================================================
        // POSTULANTES: todos los postulantes con sus 5 tablas hijas
        // ============================================================
        case 'postulantes_full':
            $postulantes = [];
            $rsP = $conn->query("
                SELECT ID_POSTULANTE, ID_GENERO, ID_TIPO_DOCUMENTO, NUM_DOCUMENTO,
                       ID_GRADO_ACADEMICO, NOMBRE, APELLIDO, FECHA_NACIMIENTO,
                       NUP, EMAIL, DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR,
                       ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID
                FROM POSTULANTE
                ORDER BY APELLIDO, NOMBRE
            ");
            while ($row = $rsP->fetch_assoc()) {
                $idPost = $conn->real_escape_string($row['ID_POSTULANTE']);

                $rsF = $conn->query("
                    SELECT ID_FORMACION, ID_OFERTA_ACADEMICA, TITULO_OBTENIDO,
                           FECHA_INICIO, FECHA_FIN, FECHA_OBTENCION
                    FROM FORMACION_ACADEMICA
                    WHERE ID_POSTULANTE = '$idPost'
                    ORDER BY FECHA_FIN DESC
                ");
                $row['formaciones'] = $rsF->fetch_all(MYSQLI_ASSOC);

                $rsE = $conn->query("
                    SELECT NIT, ID_EXPERIENCIA, PUESTO_TRABAJO, FECHA_INICIO,
                           FECHA_FIN, DESCP_EXPERIENCIA_LABORAL, CONTACTO_REFERENCIA
                    FROM EXPERIENCIA_LABORAL
                    WHERE ID_POSTULANTE = '$idPost'
                    ORDER BY FECHA_FIN DESC
                ");
                $row['experiencias'] = $rsE->fetch_all(MYSQLI_ASSOC);

                $rsC = $conn->query("
                    SELECT ID_CERTIFICACION, ID_INSTITUCION, ID_TIPO_CERTIFICACION,
                           NOMBRE_CERTIFICACION, FECHA_CERTIFICACION, FECHA_INICIO, FECHA_FIN
                    FROM CERTIFICACION
                    WHERE ID_POSTULANTE = '$idPost'
                    ORDER BY FECHA_CERTIFICACION DESC
                ");
                $row['certificaciones'] = $rsC->fetch_all(MYSQLI_ASSOC);

                $rsH = $conn->query("
                    SELECT ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, NIVEL_DESTREZA
                    FROM HABILIDAD_POSTULANTE
                    WHERE ID_POSTULANTE = '$idPost'
                ");
                $row['habilidades'] = $rsH->fetch_all(MYSQLI_ASSOC);

                $rsR2 = $conn->query("
                    SELECT ID_RED_SOCIAL, URL_PERFIL
                    FROM RED_SOCIAL_POSTULANTE
                    WHERE ID_POSTULANTE = '$idPost'
                ");
                $row['redes'] = $rsR2->fetch_all(MYSQLI_ASSOC);

                $postulantes[] = $row;
            }
            echo json_encode(["exito" => true, "data" => $postulantes], JSON_UNESCAPED_UNICODE);
            break;

    // ============================================================
    // POSTULACIONES: todas o filtradas por postulante
    // ============================================================
    case 'postulaciones_full':
        $idPostulante = $_GET['id_postulante'] ?? '';
        $whereId = '';
        if (!empty($idPostulante)) {
            $idSafe = $conn->real_escape_string($idPostulante);
            $whereId = " AND p.ID_POSTULANTE = '$idSafe'";
        }

        $rs = $conn->query("
            SELECT p.ID_POSTULACION, p.NIT, p.ID_OFERTA, p.ID_POSTULANTE,
                   p.FECHA_APLICACION, p.ESTADO_PROCESO
            FROM POSTULACION p
            WHERE 1=1 $whereId
            ORDER BY p.FECHA_APLICACION DESC
        ");
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    // ============================================================
    // INSERTAR POSTULACION: un postulante se postula a una oferta
    // ============================================================
    case 'insertar_postulacion':
        if ($method !== 'POST') {
            http_response_code(405);
            die(json_encode(["error" => "Metodo no permitido"]));
        }

        $p = json_decode(file_get_contents('php://input'), true);
        if (!$p || empty($p['id_postulacion'])) {
            http_response_code(400);
            die(json_encode(["error" => "Datos de postulacion incompletos"]));
        }

        $idPostulacion = $conn->real_escape_string($p['id_postulacion']);
        $nit = $conn->real_escape_string($p['nit'] ?? '');
        $idOferta = $conn->real_escape_string($p['id_oferta'] ?? '');
        $idPostulante = $conn->real_escape_string($p['id_postulante'] ?? '');
        $fecha = $conn->real_escape_string($p['fecha_aplicacion'] ?? '');
        $estado = $conn->real_escape_string($p['estado_proceso'] ?? 'activo');

        $conn->query("
            INSERT INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO)
            VALUES ('$idPostulacion', '$nit', '$idOferta', '$idPostulante', '$fecha', '$estado')
            ON DUPLICATE KEY UPDATE ESTADO_PROCESO = '$estado', FECHA_APLICACION = '$fecha'
        ");

        if ($conn->error) {
            http_response_code(400);
            die(json_encode(["exito" => false, "error" => $conn->error]));
        }

        echo json_encode(["exito" => true, "id_postulacion" => $idPostulacion], JSON_UNESCAPED_UNICODE);
        break;
}

    $conn->close();
