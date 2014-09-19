<?php
$id = $_POST['id'];
$theirhash = $_POST['hash'];

$ourhash = base64_encode(hash_hmac('sha1', $id . 'lti_test', 'secret', true));

if ($ourhash == $theirhash) {
	error_log("OK !!!!!!!!");
} else {
	error_log("NOT OK !!!!!!!!");
}
?>
