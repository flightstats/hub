#!/bin/sh

SERVICE=hub-0.2.0

for i in `seq 1 3` ; do
    HOST=ubuntu@deihub-0${i}.cloud-east.dev
    #push everything
    scp ../../../build/distributions/${SERVICE}.tgz ${HOST}:.
    ssh ${HOST} "tar -xzf ${SERVICE}.tgz -C local/"
    ssh ${HOST} ln -svT ${SERVICE} local/hub

    #just push code changes
    #scp ../../../build/libs/${SERVICE}.jar ${HOST}:local/hub/lib/.

    scp run.sh ${HOST}:local/hub/.
    scp dev/* ${HOST}:local/hub/conf/.
    ssh ${HOST} "chmod u+x ./local/hub/run.sh"
done
