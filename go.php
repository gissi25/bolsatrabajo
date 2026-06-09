<?php
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$action = $_GET['action'] ?? '';

// ============================================================
// MAPA DE ACCIONES -> SERVICIO
// ============================================================
$route = [
    // Servicio 1: carga masiva de ofertas
    'empresas' => 1, 'grados' => 1, 'insertar_ofertas' => 1,
    // Servicio 2: (disponible)
    // Servicio 3: sincronizar postulantes
    'sincronizar_postulantes' => 3,
    // Servicio 4: buscar certificaciones
    'tipos_certificacion' => 4, 'buscar_certificaciones' => 4, 'sincronizar_certificaciones' => 4,
    // Servicio 5: descargar datos del servidor
    'catalogos' => 5, 'empresas_full' => 5, 'ofertas_full' => 5, 'postulantes_full' => 5, 'postulaciones_full' => 5, 'insertar_postulacion' => 5,
    // Servicio 6: filtrar por ubicacion y postulantes por empresa
    'departamentos' => 6, 'municipios_por_depto' => 6,
    'filtrar_ofertas_ubicacion' => 6, 'filtrar_postulantes_empresa' => 6,
    'filtrar_postulantes_empresa_estado' => 6,
    // Servicios 7-10: (disponibles)
];

if (!array_key_exists($action, $route)) {
    die(json_encode(["error" => "Accion no valida"]));
}

$serviceFile = "servicio{$route[$action]}.php";
if (!file_exists($serviceFile)) {
    die(json_encode(["error" => "El archivo $serviceFile no existe. Crealo primero"]));
}

include $serviceFile;
