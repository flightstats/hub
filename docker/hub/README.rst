Docker in OS X
==================

Download the docker toolbox https://docs.docker.com/mac/step_one/

You will want the latest version, including docker-compose 1.6+ in order to use the v2 docker-compose file.

If using OS X, the Docker engine will run inside of a virtual machine. Once installed, the command ``docker-machine start`` should start a virtual machine called "default" in Virtualbox. This is your "Docker Host" (because OS X darwin's kernel doesn't Docker).

To ssh to this Docker host, use ``docker-machine ssh``. This will log you into your docker host. 

DNS troubleshooting in OS X
=============================
I experienced difficulty pulling images from the Docker Hub until I modified the DNS settings for the Virtualbox VM Docker host. One can either ``echo "nameserver 8.8.8.8" > /etc/resolv.conf`` **on the Docker Host Virtual Machine** or run ``VBoxManage modifyvm "default" --natdnshostresolver1 on`` from the OS X terminal.

http://iftekhar.blogspot.com.au/2012/05/virtualbox-dhcp-nameserver.html

Hub in Docker
==================

**NOTE! This application requires AWS credentials!** 

To supply the credentials and a small amount of custom config you will need to:

#. Move the contents of conf/ (hub.properties, aws.credentials, and an *optional* logback.xml) into a **local docker volume named hub_conf**. This means having superuser access on your docker host, which may be a virtual machine if you are using OS X or Windows (use ``docker-machine ssh``). It is commonly in ``/var/lib/docker/volumes``, and you will want to move these files into ``/var/lib/docker/volumes/hub_conf/_data/``

#. Include your own AWS credentials in ``aws.credentials`` for an AWS IAM user described below, as well as in the file itself.

#. Modify ``hub.properties`` and provide some aws configuration options described there.

#. Type ``docker-compose up`` and the pieces should fall into place. Heads up: There are a lot of logs generated from these 7 containers! *requires docker-compose 1.6 or higher*

**NOTE!! EVEN IF SPECIFYING "LOCAL" OPERATION, CREDENTIALS AND CORRESPONDING BUCKETS ARE STILL REQUIRED.**

This means that if s3.environment is "local," you will want permission to a hub-docker-local s3 bucket and dynamo table.

This is the kind of IAM user you want::

  "Statement": [
      {
        "Sid": "Stmt1391722720000",
        "Effect": "Allow",
        "Action": [
          "s3:*"
        ],
        "Resource": [
          "arn:aws:s3:::<s3-bucket>/*"
        ]
      },
      {
        "Sid": "Stmt1391722763000",
        "Effect": "Allow",
        "Action": [
          "s3:*"
        ],
        "Resource": [
          "arn:aws:s3:::<s3-bucket>"
        ]
      },
      {
        "Sid": "Stmt1391722978000",
        "Effect": "Allow",
        "Action": [
          "dynamodb:*"
        ],
        "Resource": [
          "arn:aws:dynamodb:us-west-2:<account-id>:table/<dynamo-prefix>-*"
        ]
      }
    ]


For more info on Hub installations, see the wiki of the open source project at
https://github.com/flightstats/hub/wiki/Install-a-Hub-Cluster
