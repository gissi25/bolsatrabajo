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

// ============================================================
// SERVICIO 6 - Pendiente de implementar
// Agrega tus casos aqui
// ============================================================
switch ($action) {
    // case 'mi_accion':
    //     ... logica ...
    //     echo json_encode(["exito" => true, ...]);
    //     break;
}

$conn->close();

