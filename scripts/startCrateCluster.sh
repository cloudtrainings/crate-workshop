#!/bin/bash

if [ "$#" -lt 1 ]; then
    echo "Usage: startCrateCluster.sh <securityGroupName>"
    exit 1
fi

sed -i -e 's/##group##/'$1'/g' ./scripts/install_workshop-crate_cluster.sh

aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev --iam-instance-profile Name=workshop-crate --user-data file://scripts/install_workshop-crate_cluster.sh --security-groups $1 --count 3

