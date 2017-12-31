#!/bin/sh
sudo add-apt-repository ppa:deadsnakes/ppa
sudo apt-get update

sudo apt-get install build-essential
sudo apt-get install libreadline-gplv2-dev libncursesw5-dev libssl-dev libsqlite3-dev tk-dev libgdbm-dev libc6-dev libbz2-dev
sudo apt-get install python2.7
sudo apt-get install libevent-dev
sudo apt-get install python-dev
wget https://bootstrap.pypa.io/get-pip.py
sudo python get-pip.py
sudo pip install locustio --upgrade
sudo pip install websocket-client --upgrade
sudo pip install httplib2

