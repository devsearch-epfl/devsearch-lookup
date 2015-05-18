set -e

# install dependencies
sudo apt-get update
sudo apt-get -y install git openjdk-7-jdk

# install SBT
wget https://dl.bintray.com/sbt/debian/sbt-0.13.7.deb
sudo dpkg -i sbt-0.13.7.deb

# install mongo
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
echo "deb http://repo.mongodb.org/apt/ubuntu "$(lsb_release -sc)"/mongodb-org/3.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org

#Configure mongo
#Change working path to ssd
sudo sed -i 's/^dbpath=.*/dbpath=\/mnt/g' /etc/mongod.conf
#Give permission
sudo chown mongodb:mongodb /mnt/


# clone akka cluster process
git clone https://github.com/devsearch-epfl/devsearch-lookup.git

#Set up start up script
sudo cp devsearch-lookup/script/startDB.sh /etc/init.d
sudo update-rc.d startDB.sh defaults
sudo chmod +x /etc/init.d/startDB.sh

echo '*** Setup is complete ***'
