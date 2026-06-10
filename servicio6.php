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
    // DEPARTAMENTOS: lista de departamentos
    // ============================================================
    case 'departamentos':
        $rs = $conn->query("SELECT ID_DEPARTAMENTO, NOMBRE_DEPARTAMENTO FROM DEPARTAMENTO ORDER BY NOMBRE_DEPARTAMENTO");
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    // ============================================================
    // MUNICIPIOS POR DEPARTAMENTO
    // ============================================================
    case 'municipios_por_depto':
        $idDepto = $_GET['id_departamento'] ?? '';
        if (empty($idDepto)) {
            http_response_code(400);
            die(json_encode(["error" => "id_departamento es requerido"]));
        }
        $idSafe = $conn->real_escape_string($idDepto);
        $rs = $conn->query("SELECT ID_MUNICIPIO, NOMBRE_MUNICIPIO FROM MUNICIPIO WHERE ID_DEPARTAMENTO = '$idSafe' ORDER BY NOMBRE_MUNICIPIO");
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    // ============================================================
    // FILTRAR OFERTAS POR UBICACION (departamento / municipio)
    // ============================================================
    case 'filtrar_ofertas_ubicacion':
        $idDepto = $_GET['id_departamento'] ?? '';
        $idMuni  = $_GET['id_municipio'] ?? '';

        if (empty($idDepto)) {
            http_response_code(400);
            die(json_encode(["error" => "id_departamento es requerido"]));
        }

        $idDeptoSafe = $conn->real_escape_string($idDepto);
        $whereMuni = '';
        if (!empty($idMuni)) {
            $idMuniSafe = $conn->real_escape_string($idMuni);
            $whereMuni = " AND e.ID_DISTRITO_MUNICIPIO = '$idMuniSafe'";
        }

        $sql = "
            SELECT o.NIT, o.ID_OFERTA, o.ID_GRADO_ACADEMICO, o.TITULO_PUESTO,
                   o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, o.EXPERIENCIA_ANIOS,
                   o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO,
                   e.NOMBRE_EMPRESA, e.CONTACTO_DIRECTO,
                   d.NOMBRE_DISTRITO, m.NOMBRE_MUNICIPIO, dep.NOMBRE_DEPARTAMENTO,
                   g.NOMBRE_GRADO
            FROM OFERTA_TRABAJO o
            JOIN EMPRESA e ON o.NIT = e.NIT
            LEFT JOIN DISTRITO d ON e.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO
                AND e.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO
                AND e.ID_DISTRITO_ID = d.ID_DISTRITO
            LEFT JOIN MUNICIPIO m ON e.ID_DISTRITO_DEPTO = m.ID_DEPARTAMENTO
                AND e.ID_DISTRITO_MUNICIPIO = m.ID_MUNICIPIO
            LEFT JOIN DEPARTAMENTO dep ON e.ID_DISTRITO_DEPTO = dep.ID_DEPARTAMENTO
            LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
            WHERE e.ID_DISTRITO_DEPTO = '$idDeptoSafe'
            $whereMuni
            ORDER BY o.FECHA_PUBLICACION DESC
        ";

        $rs = $conn->query($sql);
        $ofertas = [];
        while ($row = $rs->fetch_assoc()) {
            $nitSafe = $conn->real_escape_string($row['NIT']);
            $idOfertaSafe = $conn->real_escape_string($row['ID_OFERTA']);

            $rsR = $conn->query("
                SELECT ID_DETALLE, DESCRIPCION_REQUISITO
                FROM DETALLE_REQUISITO
                WHERE NIT = '$nitSafe' AND ID_OFERTA = '$idOfertaSafe'
                ORDER BY ID_DETALLE
            ");
            $row['requisitos'] = $rsR->fetch_all(MYSQLI_ASSOC);
            $ofertas[] = $row;
        }

        echo json_encode(["exito" => true, "data" => $ofertas], JSON_UNESCAPED_UNICODE);
        break;

    // ============================================================
    // FILTRAR POSTULANTES POR EMPRESA (los que aplicaron a sus ofertas)
    // ============================================================
    case 'filtrar_postulantes_empresa':
        $nit = $_GET['nit'] ?? '';
        if (empty($nit)) {
            http_response_code(400);
            die(json_encode(["error" => "nit es requerido"]));
        }

        $nitSafe = $conn->real_escape_string($nit);

        $sql = "
            SELECT po.ID_POSTULANTE, po.NOMBRE, po.APELLIDO, po.EMAIL,
                   po.TELEFONO_CELULAR, po.ID_GRADO_ACADEMICO,
                   p.ID_POSTULACION, p.ID_OFERTA, o.TITULO_PUESTO,
                   p.FECHA_APLICACION, p.ESTADO_PROCESO,
                   g.NOMBRE_GRADO
            FROM POSTULACION p
            JOIN POSTULANTE po ON p.ID_POSTULANTE = po.ID_POSTULANTE
            LEFT JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA
            LEFT JOIN GRADO_ACADEMICO g ON po.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
            WHERE p.NIT = '$nitSafe'
            ORDER BY p.FECHA_APLICACION DESC
        ";

        $rs = $conn->query($sql);
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;

    // ============================================================
    // FILTRAR POSTULANTES POR EMPRESA Y ESTADO
    // ============================================================
    case 'filtrar_postulantes_empresa_estado':
        $nit = $_GET['nit'] ?? '';
        $estado = $_GET['estado'] ?? '';

        if (empty($nit)) {
            http_response_code(400);
            die(json_encode(["error" => "nit es requerido"]));
        }

        $nitSafe = $conn->real_escape_string($nit);
        $whereEstado = '';
        if (!empty($estado)) {
            $estadoSafe = $conn->real_escape_string($estado);
            $whereEstado = " AND p.ESTADO_PROCESO = '$estadoSafe'";
        }

        $sql = "
            SELECT po.ID_POSTULANTE, po.NOMBRE, po.APELLIDO, po.EMAIL,
                   po.TELEFONO_CELULAR, po.ID_GRADO_ACADEMICO,
                   p.ID_POSTULACION, p.ID_OFERTA, o.TITULO_PUESTO,
                   p.FECHA_APLICACION, p.ESTADO_PROCESO,
                   g.NOMBRE_GRADO
            FROM POSTULACION p
            JOIN POSTULANTE po ON p.ID_POSTULANTE = po.ID_POSTULANTE
            LEFT JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA
            LEFT JOIN GRADO_ACADEMICO g ON po.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
            WHERE p.NIT = '$nitSafe'
            $whereEstado
            ORDER BY p.FECHA_APLICACION DESC
        ";

        $rs = $conn->query($sql);
        $data = $rs->fetch_all(MYSQLI_ASSOC);
        echo json_encode(["exito" => true, "data" => $data], JSON_UNESCAPED_UNICODE);
        break;
}

$conn->close();
