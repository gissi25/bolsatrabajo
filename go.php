<?php
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$action = $_GET['action'] ?? '';

$allowed = ['empresas', 'grados', 'insertar_ofertas'];
if (!in_array($action, $allowed)) {
    die(json_encode(["error" => "Accion no valida"]));
}

include 'servicio1.php';
