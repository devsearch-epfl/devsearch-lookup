#!/usr/bin/env bash
set -e


sudo service postgresql stop

# create directory where the db will lie
db_path="/var/postgresql/devsearch/"

# start postgresql as postgres user
sudo su -c "/usr/lib/postgresql/*/bin/pg_ctl -D ${db_path} -l ${db_path}/logfile start -w" postgres
