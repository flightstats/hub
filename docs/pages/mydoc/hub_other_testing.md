---
title: Load Testing and Verification
keywords: testing
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_other_testing.html
folder: hub
---

We use a combination of tools to ensure that Hub-V2:
* Can support at least 150% of the current peak production load
* Guarantee ordering for [Sequential Write Use Case](Sequential-Write-Use-Case)
* Ensure that all items are written from Spoke into S3

## Hourly Rolling Restart
Every hour a [jenkins job](http://ops-jenkins01.cloud-east.dev/view/hub-v2/job/hub-v2-dev-rolling-restart/) performs a rolling restart of the Hub-V2 instances to expose any issues with a deploy or rolling restart.

## Locust Testing
We use [Locust.io](http://locust.io/) to generate load and perform some verifications
* [Locust Dev](http://hub-node-tester.cloud-east.dev:8089/)
* [Locust Int](http://hub-node-tester.cloud-east.staging:8089/)

The [locust file](https://github.com/flightstats/hubv2/blob/master/src/test/load/read-write.py) has tasks which:
* creates N channels
* each channel have payload byte size 300 * N^2
* write and immediately read an item 
* performs minute, hour and day queries
* reads items 3-4 times for every write
* verifies sequential ordering by:
  * writing 10 items sequentially with no delay
  * querying those items from the last two minutes
  * verifying that the read order is the same write order

A [jenkins job](http://ops-jenkins01.cloud-east.dev/view/hub-v2/job/hub-v2-locust-verifier/) runs every hour which executes a [jasmine spec](https://github.com/flightstats/hubv2/blob/master/src/test/continuous/verify_locust_spec.js) to make sure that no failures have occurred, then resets the statistics in locust.

## S3 Write Behind Verification
A [jenkins job](http://ops-jenkins01.cloud-east.dev/view/hub-v2/job/hub-v2-s3-write-verifier/) runs every half hour to make sure that all items in the Hub's Spoke are also in S3.
It executes a [jasmine spec](https://github.com/flightstats/hubv2/blob/master/src/test/continuous/verify_s3_writer_spec.js)

{% include links.html %}