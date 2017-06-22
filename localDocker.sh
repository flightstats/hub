#!/usr/bin/env bash
gradle clean compileJava distTar

bash docker/attackHubCodeWithGradle.sh

docker build docker

echo 'Now run the following command using the image id above'
echo 'docker run -p 80:80 <image id>'
