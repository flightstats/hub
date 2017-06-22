#!/usr/bin/env bash
set -euo pipefail errfail

echo "Cleaning up any old files."
rm -rf docker/hub

echo "Running gradle clean compileJava distTar"
gradle clean compileJava distTar

echo "Untarring the distTar. Thanks, Gradle."
tar -xvf build/distributions/hub-*.tgz -C docker/

echo "Putting the hub code where the Dockerfile wants it."
mv docker/hub-*/ docker/hub

echo "Building the docker image."
docker build docker

NEWESTIMAGE=$(docker images -q | head -1)

echo "Now run the following command using the image id above"
echo "E.g. docker run -p 80:80 ${NEWESTIMAGE}"
