#!/bin/bash
## Run this from the root dir of repo, e.g. $ bash docker/attackHubCodeWithGradle.sh
set -e

# Clean up any old code
rm -rf docker/hub/

# Attack hub code with gradle. It's semi-effective.
/usr/bin/gradle clean compileJava distTar

# Thanks for tarring that, gradle, we'll take it from here
tar -xvf build/distributions/hub-*.tgz -C .

# Put the hub code where the Dockerfile wants it
mv hub-*/ docker/hub/
