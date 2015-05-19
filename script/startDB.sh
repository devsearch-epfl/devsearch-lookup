

cat db/features/* | mongoimport --db devsearch --collection features --drop
cat db/globalCount/* | mongoimport --db devsearch --collection global_occ --drop
cat db/partitionCount/* | mongoimport --db devsearch --collection local_occ --drop

mongo --eval "db.features.createIndex( { feature: 1 } )" devsearch
mongo --eval "db.features.createIndex( { file: 1, feature : 1})" devsearch 
mongo --eval "db.global_occ.createIndex( { feature: 1 } )" devsearch
mongo --eval "db.local_occ.createIndex( { feature: 1 } )" devsearch
