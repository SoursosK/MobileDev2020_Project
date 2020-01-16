<?php
$json_str = file_get_contents('php://input');
$url = "https://kostas109.pythonanywhere.com/login";
$options = array(
    'http' => array(
        'header' => "Content-type: application/json\r\n",
        'method' => "POST",
        'content' => $json_str
    )
);
$context = stream_context_create($options);
echo file_get_contents($url, false, $context);
?>