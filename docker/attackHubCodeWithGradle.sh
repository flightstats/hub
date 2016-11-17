#!/bin/bash

set -e

cd $(dirname $0)

# Clean up any old code
rm -rf hub/

# Attack hub code with gradle. It's semi-effective.
echo "using Gradle at: $(which gradle)"
gradle clean compileJava distTar

# Thanks for tarring that, gradle, we'll take it from here
tar -xvf ../build/distributions/hub-*.tgz -C .

# Put the hub code where the Dockerfile wants it
mv hub-*/ hub/
