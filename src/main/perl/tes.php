<?php
$user = $_GET['user'];
$theirhash = $_GET['hash'];

$ourhash = base64_encode(hash_hmac('sha1', $user . 'adrian_moodle', 'secret', true));

if ($ourhash == $theirhash) {

	$json = <<<EOT
	{
		"id": "safjkjlksajdlgkjl",
		"al": "spa_es",
		"tl": "eng_gb",
		"skill": "reading",
		"hideVSPT": false,
		"hideVSPTResult": true,
		"hideSA": true,
		"testDifficulty": "medium",
		"hideTest": true,
		"hideFeedbackMenu": true,
		"disallowInstantFeedback": true,
		"testCompleteUrl": "https://sakai-staging.lancs.ac.uk/dialangtes/testcompleted.php"
	}
EOT;

} else {
	$json = '{}';
}
echo $json;
?>
