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
    // Servicio 2: recomendador de formación y panorama de mercado
    'recomendar_formacion' => 2,
    'panorama_mercado' => 2,
    // Servicio 3: sincronizar postulantes
    'sincronizar_postulantes' => 3,
    // Servicios 4-10: (disponibles)
];

if (!array_key_exists($action, $route)) {
    die(json_encode(["error" => "Accion no valida"]));
}

$serviceFile = "servicio{$route[$action]}.php";
if (!file_exists($serviceFile)) {
    die(json_encode(["error" => "El archivo $serviceFile no existe. Crealo primero"]));
}

include $serviceFile;
