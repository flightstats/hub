#!/bin/bash

# Reset cassandra data on all VPC instances

BINDIR=`dirname "$0"`

${BINDIR}/reset_cassandra_data.sh \
    cassandra-01.cloud-east.dev \
    cassandra-02.cloud-east.dev \
    cassandra-03.cloud-east.dev \
