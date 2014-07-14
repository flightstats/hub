#!/bin/sh
FEATURE_BRANCH=`git rev-parse --abbrev-ref HEAD`
git checkout develop
git pull origin develop
echo "about to merge"
git merge ${FEATURE_BRANCH} --no-edit
git push origin develop
git checkout ${FEATURE_BRANCH}
