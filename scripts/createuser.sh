#!/bin/bash
sudo apt-get install whois
	
sudo sed -i "s;^PasswordAuthentication.*;PasswordAuthentication  yes;" "/etc/ssh/sshd_config"

UADD=/usr/sbin/useradd
OPENSSL=/usr/bin/openssl
SHELL=/bin/bash

# Generate 12 characters long password and print it.
PASS=`$OPENSSL rand -base64 4`
echo $1 $PASS

# Make the password usable for the useradd utility
PASS=`mkpasswd $PASS`

# Create the user
sudo $UADD -s $SHELL -m $1 -p $PASS
