<?php
if (move_uploaded_file($_FILES["file"]["tmp_name"], "upload/" . $_FILES["file"]["name"])) {
	echo "Name: " . $_FILES["file"]["name"];
	echo "Type: " . $_FILES["file"]["type"];
	echo "Size: " . ($_FILES["file"]["size"] / 1024) . " KB";
	echo "okokok";
} else {
	echo "error";
}
?>
