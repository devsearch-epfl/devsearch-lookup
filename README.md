# devsearch-lookup
Module for performing the online search query

## Setup the database
* Install MongoDB (3 or higher)
* Run the database daemon (if it is not already started):
`mongod --dbpath some/path`
* Get some features in MongoDB JSON format from hdfs (/projects/devsearch/JsonBuckets/features/bucket*)
* Download and import the features into MongoDB:

`cat features/* | mongoimport --db devsearch --collection features --drop`
* Create the index on the feature value:

`mongo --eval "db.features.createIndex( { feature: 1 } )" devsearch`

Note: you can check the last step with

`mongo --eval "printjson(db.features.getIndexes())" devsearch`

or check if the last step is done with

`mongo --eval "printjson(db.currentOp())" devsearch`

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

The first system you need to start is without flags or optionnaly the -n.
Then you can add as many slaves, with the -s flag. You will also need to override
the default port with -p (2555). They will connect to the local Mongo database though,
so there will not contain diverse search results.
sbt "run -n 1"
sbt "run -s -p 21454"


### How to run tests

For now mongod has to be running in the background with the right data set. We should automate this.
* Import test features: `mongoimport --db devsearch --collection features --file testData/testFeatures.json --drop`
* Import test rankings: `mongoimport --db devsearch --collection rankings --file testData/testRanking.json --drop`
* Run tests: `sbt test`
