# devsearch-lookup
Module for performing the online search query

## Setup the database
* Install MongoDB (3 or higher)
* Run the database daemon (if it is not already started)

`mongod --dbpath some/path`
* Get some features in MongoDB JSON format from hdfs (/projects/devsearch/JsonBuckets/features/bucket*)
* Download and import the features into MongoDB:

`for f in features/*; do mongoimport --db devsearch --collection features --file $f; done`

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
