#!/bin/bash

# Reset cassandra data on all VPC instances

BINDIR=`dirname "$0"`

${BINDIR}/reset_cassandra_data.sh \
    datahub-01.cloud-east.dev \
    datahub-02.cloud-east.dev \
    datahub-03.cloud-east.dev \
