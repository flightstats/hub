#!/bin/bash

DIR=`dirname $0`

echo Creating virtualenv
virtualenv ${DIR}/env

echo Activating the virtualenv
source ${DIR}/env/bin/activate

echo Installing dependencies
pip install ${DIR}/dhbackup.pybundle

echo All done