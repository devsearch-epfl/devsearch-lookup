set -e

# install dependencies
sudo apt-get update
sudo apt-get -y install git openjdk-7-jdk mongodb

# install SBT
wget https://dl.bintray.com/sbt/debian/sbt-0.13.7.deb
sudo dpkg -i sbt-0.13.7.deb


# clone our Play app
git clone https://github.com/devsearch-epfl/devsearch-lookup.git
cd devsearch-lookup
git checkout deploy
echo '*** Setup is complete ***'
