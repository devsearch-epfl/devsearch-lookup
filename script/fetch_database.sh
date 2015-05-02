set -e

#Install dbms

sudo apt-get -y install mongodb

# Fetch features files should be here from the start

# Convert the database
python features2json.py < feature.raw > features.json
# Import the file in the database

mongoimport --db devsearch --collection features --file features.json --drop
