<?php
$peak_threshold = $_GET["peak_threshold"];
echo "peak_threshold=".$peak_threshold."\n";

$myfile = fopen("upload/ok", "w") or die("Unable to open file!");
fwrite($myfile, $peak_threshold);
fclose($myfile);

echo "fileok";

#$result = system("ls upload/", $out);
#print_r($out."<br/>");

# $result2 = system("cd upload; /home/bingo/Bundler-master/RunSFM2.sh $peak_threshold", $out);
# print_r($out."<br/>");
?>
