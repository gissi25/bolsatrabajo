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

    case 'ofertas_vigentes':
        $nit = $_GET['nit'] ?? '';
        if ($nit) {
            $stmt = $conn->prepare("SELECT o.NIT, o.ID_OFERTA, o.TITULO_PUESTO, e.NOMBRE_EMPRESA
                FROM OFERTA_TRABAJO o
                JOIN EMPRESA e ON o.NIT = e.NIT
                WHERE o.FECHA_CADUCIDAD >= CURDATE() AND o.NIT = ?
                ORDER BY o.FECHA_PUBLICACION DESC");
            $stmt->bind_param("s", $nit);
        } else {
            $stmt = $conn->prepare("SELECT o.NIT, o.ID_OFERTA, o.TITULO_PUESTO, e.NOMBRE_EMPRESA
                FROM OFERTA_TRABAJO o
                JOIN EMPRESA e ON o.NIT = e.NIT
                WHERE o.FECHA_CADUCIDAD >= CURDATE()
                ORDER BY o.FECHA_PUBLICACION DESC");
        }
        $stmt->execute();
        $res = $stmt->get_result();
        $data = [];
        while ($row = $res->fetch_assoc()) {
            $data[] = $row;
        }
        $stmt->close();
        echo json_encode(["exito" => true, "data" => $data]);
        break;

    case 'matching_postulante':
        $input = json_decode(file_get_contents("php://input"), true);
        $idPost = $input['id_postulante'] ?? '';
        if (!$idPost) {
            echo json_encode(["error" => "id_postulante es requerido"]);
            break;
        }

        // Datos del postulante
        $stmt = $conn->prepare("SELECT p.*, TIMESTAMPDIFF(YEAR, p.FECHA_NACIMIENTO, CURDATE()) as edad
            FROM POSTULANTE p WHERE p.ID_POSTULANTE = ?");
        $stmt->bind_param("s", $idPost);
        $stmt->execute();
        $postulante = $stmt->get_result()->fetch_assoc();
        $stmt->close();
        if (!$postulante) {
            echo json_encode(["error" => "Postulante no encontrado"]);
            break;
        }

        $idGradoPost = (int)$postulante['ID_GRADO_ACADEMICO'];
        $edad = (int)$postulante['edad'];

        // Habilidades del postulante
        $stmt = $conn->prepare("SELECT hp.NIVEL_DESTREZA, h.NOMBRE_HABILIDAD
            FROM HABILIDAD_POSTULANTE hp
            JOIN HABILIDAD h ON hp.ID_CATEGORIA_HABILIDAD = h.ID_CATEGORIA_HABILIDAD AND hp.ID_HABILIDAD = h.ID_HABILIDAD
            WHERE hp.ID_POSTULANTE = ?");
        $stmt->bind_param("s", $idPost);
        $stmt->execute();
        $skillsRes = $stmt->get_result();
        $postSkills = [];
        while ($s = $skillsRes->fetch_assoc()) {
            $postSkills[] = ['name' => mb_strtolower(trim($s['NOMBRE_HABILIDAD'])), 'nivel' => $s['NIVEL_DESTREZA']];
        }
        $stmt->close();

        // Experiencia total del postulante
        $stmt = $conn->prepare("SELECT COALESCE(SUM(DATEDIFF(COALESCE(FECHA_FIN, CURDATE()), FECHA_INICIO)) / 365.0, 0) as total_exp
            FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = ?");
        $stmt->bind_param("s", $idPost);
        $stmt->execute();
        $totalExp = (float)$stmt->get_result()->fetch_assoc()['total_exp'];
        $stmt->close();

        // Todas las ofertas activas
        $ofertasRes = $conn->query("SELECT o.*, e.NOMBRE_EMPRESA
            FROM OFERTA_TRABAJO o
            JOIN EMPRESA e ON o.NIT = e.NIT
            WHERE o.FECHA_CADUCIDAD >= CURDATE()
            ORDER BY o.FECHA_PUBLICACION DESC");

        $results = [];
        while ($oferta = $ofertasRes->fetch_assoc()) {
            $nitO = $oferta['NIT'];
            $idOf = $oferta['ID_OFERTA'];

            // Requisitos de la oferta (descripciones)
            $reqStmt = $conn->prepare("SELECT DESCRIPCION_REQUISITO FROM DETALLE_REQUISITO WHERE NIT = ? AND ID_OFERTA = ?");
            $reqStmt->bind_param("ss", $nitO, $idOf);
            $reqStmt->execute();
            $reqRes = $reqStmt->get_result();
            $reqDescriptions = [];
            while ($r = $reqRes->fetch_assoc()) {
                $reqDescriptions[] = mb_strtolower(trim($r['DESCRIPCION_REQUISITO']));
            }
            $reqStmt->close();
            $totalRequeridas = count($reqDescriptions);

            // --- Scoring ---

            // 1. Grado Academico (30 pts)
            $idGradoOferta = (int)$oferta['ID_GRADO_ACADEMICO'];
            if ($idGradoPost >= $idGradoOferta) {
                $puntajeGrado = 30;
            } elseif ($idGradoPost == $idGradoOferta - 1) {
                $puntajeGrado = 15;
            } else {
                $puntajeGrado = 0;
            }

            // 2. Habilidades (30 pts)
            $coincidencias = 0;
            $tieneAvanzado = false;
            if ($totalRequeridas > 0) {
                foreach ($reqDescriptions as $req) {
                    foreach ($postSkills as $skill) {
                        if (mb_strpos($req, $skill['name']) !== false) {
                            $coincidencias++;
                            if (mb_strtolower(trim($skill['nivel'])) === 'avanzado') {
                                $tieneAvanzado = true;
                            }
                            break;
                        }
                    }
                }
                $puntajeHabilidades = ($coincidencias / $totalRequeridas) * 30;
                if ($tieneAvanzado) $puntajeHabilidades *= 1.05;
                $puntajeHabilidades = min(30, round($puntajeHabilidades, 1));
            } else {
                $puntajeHabilidades = 0;
            }

            // 3. Experiencia (25 pts)
            $expRequerida = (int)$oferta['EXPERIENCIA_ANIOS'];
            if ($expRequerida <= 0) {
                $puntajeExperiencia = 25;
            } elseif ($totalExp >= $expRequerida) {
                $puntajeExperiencia = 25;
            } elseif ($totalExp >= $expRequerida * 0.5) {
                $puntajeExperiencia = 12;
            } else {
                $puntajeExperiencia = 0;
            }

            // 4. Edad (15 pts)
            $edadMin = (int)$oferta['EDAD_MINIMA'];
            $edadMax = (int)$oferta['EDAD_MAXIMA'];
            if ($edadMin > 0 && $edadMax > 0) {
                if ($edad >= $edadMin && $edad <= $edadMax) {
                    $puntajeEdad = 15;
                } elseif ($edad > $edadMax && ($edad - $edadMax) < 5) {
                    $puntajeEdad = 8;
                } else {
                    $puntajeEdad = 0;
                }
            } else {
                $puntajeEdad = 15;
            }

            $puntajeTotal = round($puntajeGrado + $puntajeHabilidades + $puntajeExperiencia + $puntajeEdad, 1);

            if ($puntajeTotal >= 85) $clasificacion = 'Excelente';
            elseif ($puntajeTotal >= 60) $clasificacion = 'Bueno';
            elseif ($puntajeTotal >= 30) $clasificacion = 'Regular';
            else $clasificacion = 'Bajo';

            $results[] = [
                'nit' => $nitO,
                'id_oferta' => $idOf,
                'titulo_puesto' => $oferta['TITULO_PUESTO'],
                'nombre_empresa' => $oferta['NOMBRE_EMPRESA'],
                'puntaje_total' => $puntajeTotal,
                'puntaje_grado' => $puntajeGrado,
                'puntaje_habilidades' => $puntajeHabilidades,
                'puntaje_experiencia' => $puntajeExperiencia,
                'puntaje_edad' => $puntajeEdad,
                'clasificacion' => $clasificacion
            ];
        }

        usort($results, fn($a, $b) => $b['puntaje_total'] <=> $a['puntaje_total']);

        echo json_encode(["exito" => true, "data" => $results]);
        break;

    case 'matching_oferta':
        $input = json_decode(file_get_contents("php://input"), true);
        $nit = $input['nit'] ?? '';
        $idOferta = $input['id_oferta'] ?? '';
        if (!$nit || !$idOferta) {
            echo json_encode(["error" => "nit e id_oferta son requeridos"]);
            break;
        }

        // Datos de la oferta
        $stmt = $conn->prepare("SELECT o.*, e.NOMBRE_EMPRESA
            FROM OFERTA_TRABAJO o
            JOIN EMPRESA e ON o.NIT = e.NIT
            WHERE o.NIT = ? AND o.ID_OFERTA = ?");
        $stmt->bind_param("ss", $nit, $idOferta);
        $stmt->execute();
        $oferta = $stmt->get_result()->fetch_assoc();
        $stmt->close();
        if (!$oferta) {
            echo json_encode(["error" => "Oferta no encontrada"]);
            break;
        }

        $idGradoOferta = (int)$oferta['ID_GRADO_ACADEMICO'];
        $edadMin = (int)$oferta['EDAD_MINIMA'];
        $edadMax = (int)$oferta['EDAD_MAXIMA'];
        $expRequerida = (int)$oferta['EXPERIENCIA_ANIOS'];

        // Requisitos de la oferta
        $reqStmt = $conn->prepare("SELECT DESCRIPCION_REQUISITO FROM DETALLE_REQUISITO WHERE NIT = ? AND ID_OFERTA = ?");
        $reqStmt->bind_param("ss", $nit, $idOferta);
        $reqStmt->execute();
        $reqRes = $reqStmt->get_result();
        $reqDescriptions = [];
        while ($r = $reqRes->fetch_assoc()) {
            $reqDescriptions[] = mb_strtolower(trim($r['DESCRIPCION_REQUISITO']));
        }
        $reqStmt->close();
        $totalRequeridas = count($reqDescriptions);

        // Todos los postulantes
        $postulantesRes = $conn->query("SELECT p.*, TIMESTAMPDIFF(YEAR, p.FECHA_NACIMIENTO, CURDATE()) as edad
            FROM POSTULANTE p ORDER BY p.APELLIDO, p.NOMBRE");

        $results = [];
        while ($postulante = $postulantesRes->fetch_assoc()) {
            $idPost = $postulante['ID_POSTULANTE'];
            $idGradoPost = (int)$postulante['ID_GRADO_ACADEMICO'];
            $edad = (int)$postulante['edad'];

            // Habilidades del postulante
            $sStmt = $conn->prepare("SELECT hp.NIVEL_DESTREZA, h.NOMBRE_HABILIDAD
                FROM HABILIDAD_POSTULANTE hp
                JOIN HABILIDAD h ON hp.ID_CATEGORIA_HABILIDAD = h.ID_CATEGORIA_HABILIDAD AND hp.ID_HABILIDAD = h.ID_HABILIDAD
                WHERE hp.ID_POSTULANTE = ?");
            $sStmt->bind_param("s", $idPost);
            $sStmt->execute();
            $sRes = $sStmt->get_result();
            $postSkills = [];
            while ($s = $sRes->fetch_assoc()) {
                $postSkills[] = ['name' => mb_strtolower(trim($s['NOMBRE_HABILIDAD'])), 'nivel' => $s['NIVEL_DESTREZA']];
            }
            $sStmt->close();

            // Experiencia total
            $eStmt = $conn->prepare("SELECT COALESCE(SUM(DATEDIFF(COALESCE(FECHA_FIN, CURDATE()), FECHA_INICIO)) / 365.0, 0) as total_exp
                FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = ?");
            $eStmt->bind_param("s", $idPost);
            $eStmt->execute();
            $totalExp = (float)$eStmt->get_result()->fetch_assoc()['total_exp'];
            $eStmt->close();

            // --- Scoring (misma logica) ---
            if ($idGradoPost >= $idGradoOferta) $puntajeGrado = 30;
            elseif ($idGradoPost == $idGradoOferta - 1) $puntajeGrado = 15;
            else $puntajeGrado = 0;

            $coincidencias = 0;
            $tieneAvanzado = false;
            if ($totalRequeridas > 0) {
                foreach ($reqDescriptions as $req) {
                    foreach ($postSkills as $skill) {
                        if (mb_strpos($req, $skill['name']) !== false) {
                            $coincidencias++;
                            if (mb_strtolower(trim($skill['nivel'])) === 'avanzado') $tieneAvanzado = true;
                            break;
                        }
                    }
                }
                $puntajeHabilidades = ($coincidencias / $totalRequeridas) * 30;
                if ($tieneAvanzado) $puntajeHabilidades *= 1.05;
                $puntajeHabilidades = min(30, round($puntajeHabilidades, 1));
            } else {
                $puntajeHabilidades = 0;
            }

            if ($expRequerida <= 0) $puntajeExperiencia = 25;
            elseif ($totalExp >= $expRequerida) $puntajeExperiencia = 25;
            elseif ($totalExp >= $expRequerida * 0.5) $puntajeExperiencia = 12;
            else $puntajeExperiencia = 0;

            if ($edadMin > 0 && $edadMax > 0) {
                if ($edad >= $edadMin && $edad <= $edadMax) $puntajeEdad = 15;
                elseif ($edad > $edadMax && ($edad - $edadMax) < 5) $puntajeEdad = 8;
                else $puntajeEdad = 0;
            } else {
                $puntajeEdad = 15;
            }

            $puntajeTotal = round($puntajeGrado + $puntajeHabilidades + $puntajeExperiencia + $puntajeEdad, 1);

            if ($puntajeTotal >= 85) $clasificacion = 'Excelente';
            elseif ($puntajeTotal >= 60) $clasificacion = 'Bueno';
            elseif ($puntajeTotal >= 30) $clasificacion = 'Regular';
            else $clasificacion = 'Bajo';

            $results[] = [
                'id_postulante' => $idPost,
                'nombre' => $postulante['NOMBRE'],
                'apellido' => $postulante['APELLIDO'],
                'puntaje_total' => $puntajeTotal,
                'puntaje_grado' => $puntajeGrado,
                'puntaje_habilidades' => $puntajeHabilidades,
                'puntaje_experiencia' => $puntajeExperiencia,
                'puntaje_edad' => $puntajeEdad,
                'clasificacion' => $clasificacion
            ];
        }

        usort($results, fn($a, $b) => $b['puntaje_total'] <=> $a['puntaje_total']);

        echo json_encode(["exito" => true, "data" => $results]);
        break;

    default:
        echo json_encode(["error" => "Accion no valida para servicio 9"]);
}

$conn->close();
