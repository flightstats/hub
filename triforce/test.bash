#!/bin/bash

exec > temp.stdout
exec 2> temp.stderr

echo "stdout"
echo "stderr" 1>&2
