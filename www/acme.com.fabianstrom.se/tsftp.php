<?php
define('CERTS_DIR', 'certs');
define('FILES_DIR', 'files');
define('KEY_FILENAME', '__tsftp_encryption_key');

$actions = array('cert', 'file', 'key', 'upload', 'delete');

if (!isset($_GET['action'])) {
	// no action
	bye('no action', 400);
} else if (!in_array($_GET['action'], $actions)) {
	// invalid action
	bye('invalid action', 400);
} else {
	if ($_GET['action'] == 'cert') serveCert();
	if ($_GET['action'] == 'file') serveFile();
	if ($_GET['action'] == 'key') serveKey();
	if ($_GET['action'] == 'upload') handleUpload();
	if ($_GET['action'] == 'delete') handleDelete();
}

function serveCert() {
	if (!isset($_GET['user'])) {
		// no user specified
		bye('no user', 400);
	} else {
		$user = $_GET['user'];
		if (!validateUser($user)) {
			bye('invalid user', 404);
		} else {
			$path = CERTS_DIR . '/' . $user;
			download($path);
		}
	}
}

function serveFile() {
	if (!isset($_GET['hash']) || !isset($_GET['filename'])) {
		// no user specified
		bye('hash or filename missing', 400);
	} else {
		$hash = $_GET['hash'];
		$filename = $_GET['filename'];
		if (!validateHash($hash)) {
			bye('invalid hash', 404);
		} else if (!validateFilename($filename)) {
			bye('invalid filename', 404);
		} else {
			$path = FILES_DIR . '/' . $hash . '/' . $filename;
			download($path);
		}
	}
}

function serveKey() {
	if (!isset($_GET['hash'])) {
		// no user specified
		bye('hash missing', 400);
	} else {
		$hash = $_GET['hash'];
		if (!validateHash($hash)) {
			bye('invalid hash', 404);
		} else {
			$path = FILES_DIR . '/' . $hash . '/' . KEY_FILENAME;
			download($path);
		}
	}
}

function handleUpload() {
	$tmppath = $_FILES['file']['tmp_name'];

	if (!file_exists($tmppath) || !isset($_POST['encryption_key'])) {
		bye('no file or no encryption key', 400);
	} else {
		$hash = hash_file('sha256', $tmppath);
		$key = $_POST['encryption_key'];
		$dir = FILES_DIR . '/' . $hash;
		$filepath = $dir . '/' . basename($_FILES['file']['name']);

		mkdir($dir, 0755);

		file_put_contents($dir.'/'.KEY_FILENAME, $key);
		move_uploaded_file($tmppath, $filepath);

		echo 'tsftp://acme.com.fabianstrom.se/' . $hash . '/' . $_FILES['file']['name'];
	}
}

function handleDelete() {
	if (!isset($_GET['hash'])) {
		// no user specified
		bye('hash missing', 400);
	} else {
		$hash = $_GET['hash'];
		if (!validateHash($hash)) {
			bye('invalid hash', 404);
		} else {
			$dir = FILES_DIR . '/' . $hash;
			if (!is_dir($dir)) return;
			foreach (scandir($dir) as $file) {
				if ($file == '.' || $file == '..') continue;
				echo $dir . '/' . $file . "\n";
				unlink($dir . '/' . $file);
			}
			echo $dir;
			rmdir($dir);
		}
	}
}

function validateUser($user) {
	return preg_match('/^[A-Za-z0-9\._-]+$/', $user);
}

function validateHash($hash) {
	if (strlen($hash) !== 64) return false;
	return preg_match('/^[A-Fa-f0-9]+$/', $hash);
}

function validateFilename($filename) {
	return preg_match('/^[A-Za-z0-9\._-]+$/', $filename);
}

function download($file) {
	if (!file_exists($file)) bye('file not found', 404);
	header('Content-Description: File Transfer');
	header('Content-Type: application/octet-stream');
	header('Content-Disposition: attachment; filename="'.basename($file).'"');
	header('Expires: 0');
	header('Cache-Control: must-revalidate');
	header('Pragma: public');
	header('Content-Length: ' . filesize($file));
	readfile($file);
}

function bye($msg, $code) {
	die('<h1>'.$msg.'</h1>');
}
