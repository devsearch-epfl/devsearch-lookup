#!/usr/bin/env bash

# MongoDb startup script for azure D11_v2 vms

set -e


sudo service postgresql stop


# create directory where the db will lie
db_path="/mnt/var/postgresql/devsearch/"
sudo rm -rf ${db_path}
sudo mkdir -p ${db_path}
sudo chown postgres:postgres ${db_path}

# the user running postgresql must run this command for correct right managements
sudo su -c "/usr/lib/postgresql/*/bin/initdb ${db_path}" postgres

# overriding default postgres configuration for specific memory management
# there must be a nicer way to do so
postgres_conf=${db_path}/postgresql.conf
sudo bash -c "echo shared_buffers=4GB >> $postgres_conf"
sudo bash -c "echo work_mem=400MB >> $postgres_conf"

# start postgresql as postgres user
sudo su -c "/usr/lib/postgresql/*/bin/pg_ctl -D ${db_path} -l ${db_path}/logfile start -w" postgres


# create database and user for current unix user (later used to access the db)
sudo -u postgres createdb $USER
sudo -u postgres createuser --superuser $USER

echo '*** PostgreSQL is started, preparing data for import ***'

# prepare data for import
sed s/\\\\/\\\\\\\\/g ~/bucket-data/bucket*.json > ~/bucket-data/postgres-json-format.json
sed -i s/\\\\\\\\u0000//g ~/bucket-data/postgres-json-format.json


echo '*** importing data ***'

psql -af $(dirname $0)/postgresql_import.sql


echo '*** PostgreSQL is started, filled and indexed ***'