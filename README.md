# devsearch-lookup
Module for performing the online search query

## Setup the database
* Install MongoDB (3 or higher)
* Run the database daemon (if it is not already started): `mongod --dbpath some/path`
* Get some features from the offline Spark job
* Convert the features file to MongoDB JSON format (with Python 3): `python features2json.py < [your features file] > features.json`
* Import the features into MongoDB `mongoimport --db devsearch --collection features --file features.json --drop`
* Create the index on the feature value: `mongo --eval "db.records.createIndex( { feature: 1 } )" devsearch`
Note: you can check the last step with `mongo --eval "printjson(db.features.getIndexes())" devsearch`
