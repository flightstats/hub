#!/bin/sh
git checkout develop
git pull origin develop
git merge $1
git push origin develop