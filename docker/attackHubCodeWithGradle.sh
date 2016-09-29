#!/bin/bash
set -e

rm -rf docker/singlehub/
gradle clean compileJava distTar
tar -xvf build/distributions/hub-*.tgz -C .
mv hub-*/ docker/singlehub/
