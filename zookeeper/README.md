The Hub's ZooKeeper
=======

* [overview](#overview)
* [setup](#setup)

## overview

The Hub uses Zookeepr for distributed state throughout a cluster.
The primary use is tracking the sequence ids of items in a channel.
A secondary use is leader election.
Zookeeper is run on dedicated hosts. 

## setup

Run all these steps as the ubuntu user.

1. [Read this first](#http://zookeeper.apache.org/doc/trunk/zookeeperAdmin.html#sc_zkMulitServerSetup).
2. Create an EC2 instance 
3. Attach a 15GB SSD EBS volume
4. Create a mount point for the EBS volume at /ebs/zookeeper
5. Put a file named 'myid' in /ebs/zookeeper with the value equal to the server's number in zoo.cfg 
6. Extract zookeeper-3.4.5.tar.gz in the ubuntu user's home dir
7. Create a symlink zookeeper -> zookeeper-3.4.5
8. Copy zoo.cfg for the environment to ~/zookeeper/conf
9. Start zookeeper with ~/zookeeper/bin/zkServer.sh
10. Create cron job to run every 5 minutes calling purge.sh

