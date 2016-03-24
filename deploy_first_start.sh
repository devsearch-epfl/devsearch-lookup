#!/usr/bin/env bash

sudo mkdir /mnt/database
sudo mongod --fork --logpath /var/log/mongodb.log --dbpath /mnt/database

# fill the db with local files (previously copied over with scp for instance)
cat bucket-data/bucket*.json | mongoimport --db devsearch --collection features --drop
cat bucket-data/bucketx.globalcount.json | mongoimport --db devsearch --collection global_occ --drop
cat bucket-data/bucket*.partitioncounts.json | mongoimport --db devsearch --collection local_occ --drop
mongo --eval "db.features.createIndex( { feature: 1 } )" devsearch
mongo --eval "db.features.createIndex( { file: 1 } )" devsearch
mongo --eval "db.global_occ.createIndex( { feature: 1 } )" devsearch
mongo --eval "db.local_occ.createIndex( { feature: 1 } )" devsearch


git clone https://github.com/devsearch-epfl/devsearch-lookup.git
cd devsearch-lookup/


sbt run -c -5