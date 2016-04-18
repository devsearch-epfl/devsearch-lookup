#!/usr/bin/env bash

# Install script for azure D11_v2 vms

set -e

mkdir bucket-data

sudo apt-get update

echo "starting installation with mongodb and sbt"

sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927
echo "deb http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-get update


# install mongodb, sbt and git as dependencies
sudo apt-get install -y mongodb-org

sudo apt-get install -y sbt

sudo apt-get install -y git

# install our tool
git clone https://github.com/devsearch-epfl/devsearch-lookup.git
cd devsearch-lookup/

echo '*** Setup is complete ***'
