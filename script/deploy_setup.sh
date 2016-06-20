#!/usr/bin/env bash

# Install script for azure D11_v2 vms

set -e

# prepare folder to accept files from DATA servers
mkdir -p bucket-data

sudo apt-get update
sudo apt-get install -y git

echo "Installing Git, MongodDB and PostgreSQL"

# add mongodb APT repository
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927
echo "deb http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list

# add sbt APT repository
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list

# add postgresql APT repository
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" >> /etc/apt/sources.list.d/pgdg.list'
wget -q https://www.postgresql.org/media/keys/ACCC4CF8.asc -O - | sudo apt-key add -

sudo apt-get update


# install mongodb, sbt and postgresql as dependencies
sudo apt-get install -y mongodb-org

sudo apt-get install -y sbt

sudo apt-get install -y postgresql postgresql-contrib

# install our tool
git clone https://github.com/devsearch-epfl/devsearch-lookup.git
cd devsearch-lookup/

echo '*** Setup is complete ***'
