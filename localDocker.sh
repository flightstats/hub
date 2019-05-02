#!/usr/bin/env bash

[[ ! -z "$1" ]] && PREFIX=$1 || PREFIX="default"
[[ ! -z "$2" ]] && IMAGE_NAME=$2 || IMAGE_NAME="hub"

set -euo pipefail errfail

echo "Cleaning up any old files."
rm -rf docker/hub
rm -rf docker/configs

echo "Building the code into a tarball."
./gradlew clean compileJava distTar

echo "Uncompressing the tarball."
tar -xvf build/distributions/hub-*.tgz -C docker/

echo "Putting the hub code where the Dockerfile wants it."
mv docker/hub-*/ docker/hub

echo "Adding the config files matching: ${PREFIX}"
mkdir docker/configs
cp configs/${PREFIX}* docker/configs
for file in docker/configs/${PREFIX}*; do
  mv "$file" "${file/${PREFIX}-/}"
done

echo "Building the docker image."
docker build --tag ${IMAGE_NAME}:local docker

echo "----------"
echo ""
echo "Usage:"
echo "  docker run --name ${IMAGE_NAME} --publish 80:80 --publish 3333:3333 hub:local"
echo ""
