<?php
$json_str = '{"from":"CES","text":"' + $_POST["text"] + '","to":"306972566409","api_key":"' + $api_key + '","api_secret":"' + $api_secret + '"}';
$url = "https://rest.nexmo.com/sms/json";
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