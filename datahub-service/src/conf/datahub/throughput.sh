#!/bin/sh

#startup the throughput drivers, spread across the instances
curl http://deihub-01.cloud-east.dev:8080/throughput/1/1.0
curl http://deihub-01.cloud-east.dev:8080/throughput/2/1.0
curl http://deihub-01.cloud-east.dev:8080/throughput/5/1.0
curl http://deihub-01.cloud-east.dev:8080/throughput/10/1.0
curl http://deihub-01.cloud-east.dev:8080/throughput/20/1.0
curl http://deihub-01.cloud-east.dev:8080/throughput/50/1.0
curl http://deihub-02.cloud-east.dev:8080/throughput/100/1.0
curl http://deihub-03.cloud-east.dev:8080/throughput/200/1.0