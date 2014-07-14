#!/bin/sh
FEATURE_BRANCH=`git rev-parse --abbrev-ref HEAD`
git checkout develop
git pull origin develop
echo "about to merge"
git merge --no-edit ${FEATURE_BRANCH}
git push origin develop
git checkout ${FEATURE_BRANCH}
#adding line
