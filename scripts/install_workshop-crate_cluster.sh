#!/bin/bash 

echo "BEGIN Crate Installation...."
sudo add-apt-repository ppa:openjdk-r/ppa -y
sudo apt-get update -y
sudo apt-get install openjdk-8-jdk -y
sudo apt-get install curl -y
bash -c "$(curl -L install.crate.io)" -y
echo "END Crate Installation...."

echo "Configure Crate Cluster...."



# Cluster name identifies your cluster for auto-discovery. If you're running
# multiple clusters on the same network, make sure you're using unique names.
#
echo " cluster.name: workshop-crate" >> /etc/crate/crate.yml


# To enable EC2 discovery you need to set the discovery type to 'ec2'.
#
echo " discovery.type: ec2" >> /etc/crate/crate.yml


# Set to ensure a node sees M other master eligible nodes to be considered
# operational within the cluster. Its recommended to set it to a higher value
# than 1 when running more than 2 nodes in the cluster.
#
# We highly recommend to set the minimum master nodes as follows:
#   minimum_master_nodes: (N / 2) + 1 where N is the cluster size
# That will ensure a full recovery of the cluster state.
#

echo " discovery.zen.minimum_master_nodes: 2" >> /etc/crate/crate.yml


# The gateway persists cluster meta data on disk every time the meta data
# changes. This data is stored persistently across full cluster restarts
# and recovered after nodes are started again.

#recover_after_nodes:
# 	Defines the number of nodes that need to be started before any cluster
# 	state recovery will start.
#

# recover_after_time: 
# Defines the time to wait before starting the recovery once the number
# of nodes defined in gateway.recover_after_nodes are started.
#

# gateway.expected_nodes:  
# Defines how many nodes should be waited for until the cluster state is
# recovered immediately. The value should be equal to the number of nodes
# in the cluster.
#

echo " gateway.recover_after_nodes: 3" >> /etc/crate/crate.yml
echo " gateway.recover_after_time: 5m" >> /etc/crate/crate.yml
echo " gateway.expected_nodes: 3" >> /etc/crate/crate.yml

echo " discovery.ec2.groups: ##group##" >> /etc/crate/crate.yml
echo "Restart Crate Cluster...."
sudo service crate restart

echo "Install Maven...."
sudo apt-get install maven -y
	
echo "Configure Crash"
sudo ln -sf /usr/share/crate/bin/crash /usr/bin/
sudo ln -sf /usr/share/crate/bin/crash_standalone /usr/bin/
