#!/bin/sh

SERVICE=datahub-service-0.1.7

for i in `seq 1 3` ; do
    HOST=ubuntu@deihub-0${i}.cloud-east.dev
    #push everything
    scp ../../../build/distributions/${SERVICE}.tgz ${HOST}:.
    ssh ${HOST} "tar -xzf ${SERVICE}.tgz -C local/"
    ssh ${HOST} ln -svT ${SERVICE} local/datahub

    #just push code changes
    #scp ../../../build/libs/${SERVICE}.jar ${HOST}:local/datahub/lib/.

    scp run.sh ${HOST}:local/datahub/.
    scp dev/* ${HOST}:local/datahub/conf/.
    ssh ${HOST} "chmod u+x ./local/datahub/run.sh"
done
