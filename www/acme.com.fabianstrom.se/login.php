<?php
session_start();

require_once('GoogleAuthenticator.php');
require_once('users.php');

if (isset($_GET['logout'])) session_destroy();
if (isset($_GET['email'])) {
	makeUser();
	die();
}

function checkLogin() {
	if (isset($_SESSION['loggedin']) && $_SESSION['loggedin'] === true) {
		header('Location: index.php');
		return true;
	}
	
	return false;
}
checkLogin();

function loginCert() {
	if ($_SERVER['VERIFIED'] !== 'SUCCESS') {
		echo "<pre>";
		var_dump($_SERVER);
		echo "</pre>";

		$dn = $_SERVER['DN'];
		$dn = explode('/', $dn);

		$found = false;
		foreach ($dn as $line) {
			if (preg_match('/^CN=/', $line)) {
				$cn = explode('=', $line);
				$cn = $cn[1];
				if (isset($users[$cn])) {
					$_SESSION['loggedin'] = true;
					header('Location: index.php');
					return true;
				}
			}
		}

		return false;
	}
	return false;
}
if (isset($_GET['cert'])) {
	if (loginCert()) exit(0);
}

function makeUser() {
	$email = $_GET['email'];
	$password = $_GET['password'];
	$ga = new PHPGangsta_GoogleAuthenticator();
	$secret = $ga -> createSecret();
	$hmackey = hash('sha256', openssl_random_pseudo_bytes(27));
	echo "<img src='".$ga -> getQRCodeGoogleUrl($email, $secret)."'><br>";
	echo "<pre>";
	echo "'" . $email . "' => array(" . "\n";
	echo "        'password' => '" . hash_hmac("sha256", $password, $hmackey) . "', // " . $password . "\n";
	echo "        'hmac_key' => '" . $hmackey . "',\n";
	echo "        'totp_key' => '" . $secret . "'\n";
	echo ")," . "<br>";
	echo "</pre>";
}

if (isset($_POST['login_password'])) {
	$email = $_POST['email'];
	$password = $_POST['password'];
	$otk = $_POST['otk'];

	$user = $users[$email];
	$hash = hash_hmac('sha256', $password, $user['hmac_key']);
	$ga = new PHPGangsta_GoogleAuthenticator();
	if (!isset($user)) {
		echo "Invalid username, password or one time code.";
	} else if ($hash !== $user['password']) {
		echo "Invalid username, password or one time code.";
	} else if (!($ga -> verifyCode($user['totp_key'], $otk))) {
		echo "Invalid username, password or one time code.";
	} else {
		$_SESSION['loggedin'] = true;
		checkLogin();
	}
}

?>

<html>
<head>
<title>Acme login</title>
</head>
<body>

<a href="login.php?cert">Log in with cert</a>

<form action="login.php" method="post">
<table>
<tr>
	<td>Email</td>
	<td><input type="text" name="email"></td>
</tr>
<tr>
	<td>Password</td>
	<td><input type="password" name="password"></td>
</tr>
<tr>
	<td>One time key</td>
	<td><input type="password" name="otk"></td>
</tr>
<tr>
	<td colspan="2"><input type="submit" name="login_password" value="Login"></td>
</tr>
</table>
</body>
</html>
