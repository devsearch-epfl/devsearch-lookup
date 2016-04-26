# devsearch-lookup
Module for performing the online search query

## Setup the database
* Install MongoDB (3 or higher)
* Run the database daemon (if it is not already started):
`mongod --dbpath some/path`
* Get some features in MongoDB JSON format from hdfs (/projects/devsearch/JsonBuckets/features/bucket*)
* Download and import the features into MongoDB:

```
cat features/bucketX/* | mongoimport --db devsearch --collection features --drop
cat globalCount/* | mongoimport --db devsearch --collection global_occ --drop
cat partitionCounts/bucketX/* | mongoimport --db devsearch --collection local_occ --drop
```
* Create the index on the feature value:

```
mongo --eval "db.features.createIndex( { feature: 1 } )" devsearch
mongo --eval "db.features.createIndex( { file: 1 } )" devsearch
mongo --eval "db.global_occ.createIndex( { feature: 1 } )" devsearch
mongo --eval "db.local_occ.createIndex( { feature: 1 } )" devsearch
```

Note: you can check the last step with

`mongo --eval "printjson(db.features.getIndexes())" devsearch`

or check if the last step is done with

`mongo --eval "printjson(db.currentOp())" devsearch`

## Run mongodb as daemon

One can also start mongodb as a daemon with:
```
mongod --fork --logpath /var/log/mongodb.log --dbpath some/path
```

## Running the application

### Usage
```shell
Usage: LookupCluster [options]

  -c | --cluster
        run the cluster on multiple machines
  -s | --slave
        slave node
  -p <value> | --port <value>
        port of the netty server
  -n <value> | --nbpart <value>
        number of partitions
```


### Running locally

The first system you need to start is without flags or optionnaly the `-n`.
Then you can add as many slaves, with the `-s` flag. You will also need to override
the default port with `-p (2555)`. They will connect to the local Mongo database though,
so there will not contain diverse search results:
* `sbt "run -n 1"`
* `sbt "run -s -p 21454"`


### Run on a cluster

* Machines should share a common network
* A machine should be named `master1`
* Run the lookUpProvider (actor that merge responses from other nodes) 
with `sbt "run -c -n 5"` 5 being the number of slave nodes (**Warning: -c must always be the first argument after run!**).
* Run the 5 slaves with `sbt "run -c -s"`.
* If a slave resides on the master node, run `sbt "run -c -s -p 21454"`
or whatever port you fancy.


### How to run tests

For now mongod has to be running in the background with the right data set. We should automate this.
* Import test features: `mongoimport --db devsearch --collection features --file testData/testFeatures.json --drop`
* Import test rankings: `mongoimport --db devsearch --collection rankings --file testData/testRanking.json --drop`
* Run tests: `sbt test`
