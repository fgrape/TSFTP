<?php
session_start();

function checkLogin() {
	if (!isset($_SESSION['loggedin']) || $_SESSION['loggedin'] !== true) {
		header('Location: login.php');
		return false;
	}
	
	return true;
}

if (isset($_GET['logout'])) {
	unset($_SESSION['loggedin']);
	session_destroy();
}

checkLogin();

?>

<html>
<head>
<title>Welcome to Acme!</title>
</head>
<body bgcolor="blue" text="red">
<center><h1>Welcome to Acme!</h1></center>
<img src="katt.gif">
<img src="cat.gif">
<img src="301.gif">
<img src="cage.jpg">

<a href="index.php?logout">Log out</a>
</body>
</html>
