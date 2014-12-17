#!/bin/sh
sudo add-apt-repository ppa:fkrull/deadsnakes
sudo apt-get update
#skipping
#sudo apt-get install gcc
sudo apt-get install build-essential
sudo apt-get install libreadline-gplv2-dev libncursesw5-dev libssl-dev libsqlite3-dev tk-dev libgdbm-dev libc6-dev libbz2-dev
sudo apt-get install python2.7
sudo apt-get install python-pip
sudo apt-get install libevent-dev
sudo apt-get install python-dev
sudo pip install locustio --upgrade
#sudo pip install https://github.com/surfly/gevent/releases/download/1.0.1/gevent-1.0.1.tar.gz
sudo pip install websocket-client
