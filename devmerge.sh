#!/bin/sh
FEATURE_BRANCH=`git rev-parse --abbrev-ref HEAD`
git branch -d develop
git checkout develop
git pull origin develop
git merge --no-edit -X theirs ${FEATURE_BRANCH}
git push origin develop --force
git checkout ${FEATURE_BRANCH}
git branch -d develop
