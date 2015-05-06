# Devsearch-lookup Tests

## How to run test

* Import test features: `mongoimport --db devsearch --collection features --file testData/testFeatures.json --drop`
* Import test rankings: `mongoimport --db devsearch --collection rankings --file testData/testRanking.json --drop`
* Run tests: `sbt test`