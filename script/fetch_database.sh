set -e

#Install dbms

sudo apt-get -y install mongodb

# Fetch features files TODO

# Convert the database ???TODO???

# Import the file in the database

mongoimport --db devsearch --collection features --file features.json --drop