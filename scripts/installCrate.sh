#!/bin/bash 

echo "BEGIN Crate Installation...."
sudo add-apt-repository ppa:openjdk-r/ppa -y
sudo apt-get update -y
sudo apt-get install openjdk-8-jdk -y
sudo apt-get install curl -y
bash -c "$(curl -L install.crate.io)" -y
echo "END Crate Installation...."