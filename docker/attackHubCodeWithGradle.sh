#!/bin/bash
set -e

gradle clean compileJava distTar
tar -xvf build/distributions/hub-*.tgz -C .
mv hub-*/ docker/singlehub/
