#!/bin/bash
set -e

cd ..
gradle build distTar
tar -xvf build/distributions/hub-*.tgz -C .
mv hub-*/ docker/singlehub/
