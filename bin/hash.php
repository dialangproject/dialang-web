<?php
$data = $argv[1];
$key = 'secret';
print hash_hmac('sha1', $data, $key) . "\n";
?>
