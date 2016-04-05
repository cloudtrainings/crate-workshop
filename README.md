#2.Installation and Configuration
##Amazon AWS:

###2.1 AWS available crate AMIs:
```bash
aws ec2 describe-images --filters "Name=name,Values=crate-0.54.*"
aws ec2 describe-images --filters "Name=name,Values=crate-0.53.*"
aws ec2 describe-images --filters "Name=name,Values=crate-0.52.*"
aws ec2 describe-images --filters "Name=name,Values=crate-0.51.*"
```
Install a new AWS machine based on create AMI.  
Choose one of the AMI returned by one of the commands from above. 
 
>  Example:  
>  "VirtualizationType": "hvm", 
>  "Name": "crate-0.51.1-1-amzn-ami-hvm-2015.03.0.x86_64",  
>  "Hypervisor": "xen",  
>  "SriovNetSupport": "simple",  
>  "ImageId": "ami-23592546",  
            
```bash       
aws ec2 run-instances --image-id ami-23592546 --instance-type m3.medium --key-name dev
```
Notes:  
should be used an aws instance type which supports hvm virtualization.  
please check : [AWS instance-type-matrix](https://aws.amazon.com/amazon-linux-ami/instance-type-matrix/).  
> key name should be created preliminary - is not part of this workshop  
          
Crate URL: http://[AWS Instance:public IP]:4200/admin  

Let's have a short look into the instance:
1.  Enter ssh to instance:  
```bash       
sudo ssh -i ~/.aws/keys/dev.pem ubuntu@[AWS Public IP]
```
2.  See the crate process running with:  

```bash       
ps -ax | grep crate
```  
Output:  

> 998 ?        Ssl   75:24 java -Xms256m -Xss256k -Djava.awt.headless=true -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Dcrate -Des.path.home=/usr/share/crate -Des.config=/usr/share/crate/config/crate.yml -cp :/usr/share/crate/lib/crate-*.jar:/usr/share/crate/lib/crate-app.jar:/usr/share/crate/lib/*:/usr/share/crate/lib/sigar/* io.crate.bootstrap.CrateF
 7028 pts/0    S+     0:00 grep --color=auto crate

3.  Stop Crate service:  

```bash       
sudo service crate stop
```  
4.  Start Crate service:  

```bash       
sudo service crate start
```  


###2.2 Self-installation on an empty fresh EC2 machine  

Start an EC2 Instance:
```bash       
aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev 
```  
Connect via ssh to new AWS instance:

```bash       
sudo ssh -i ~/.aws/keys/dev.pem ubuntu@[AWS Public IP]
```  
Manual CRATE installation:  
```bash 
sudo add-apt-repository ppa:openjdk-r/ppa -y
sudo apt-get update -y
sudo apt-get install openjdk-8-jdk -y
sudo apt-get install curl -y
bash -c "$(curl -L install.crate.io)" -y
``` 
Crate URL: http://[AWS Instance:public IP]:4200/admin   

###2.3 Cluster setup based on EC2 discovery  

```bash 
 aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev --iam-instance-profile Name=workshop-crate --user-data file://scripts/install_workshop-crate_cluster.sh --count 3            
``` 

Configuration file: /etc/crate/crate.yml it looks as:


Cluster name identifies your cluster for auto-discovery. If you're running  
multiple clusters on the same network, make sure you're using unique names. 
  
> cluster.name: workshop-crate

To enable EC2 discovery you need to set the discovery type to 'ec2'.
> discovery.type: ec2  

Set to ensure a node sees M other master eligible nodes to be considered
operational within the cluster. Its recommended to set it to a higher value
than 1 when running more than 2 nodes in the cluster.

We highly recommend to set the minimum master nodes as follows:
minimum_master_nodes: (N / 2) + 1 where N is the cluster size
That will ensure a full recovery of the cluster state.


> discovery.zen.minimum_master_nodes: 2

The gateway persists cluster meta data on disk every time the meta data      
changes. This data is stored persistently across full cluster restarts          
and recovered after nodes are started again.  

recover_after_nodes:  
Defines the number of nodes that need to be started before any cluster  
state recovery will start.  

> gateway.recover_after_nodes: 3  

recover_after_time:   
Defines the time to wait before starting the >recovery once the number    
of nodes defined in gateway.recover_after_nodes are >started.    

> gateway.recover_after_time: 5m  


gateway.expected_nodes:  
Defines how many nodes should be waited for until >the cluster state is  
recovered immediately. The value should be equal to >the number of nodes  
in the cluster.  

> gateway.expected_nodes: 3    

Let's create a dummy entity and see that it is available on any instance of the cluster:

Copy weblogstreaming/webApacheSchema.sql to one of the cluster machine:

```bash 
 scp -i ~/.aws/keys/dev.pem weblogstreaming/webApacheSchema.sql ubuntu@[AWS public IP]:/home/ubuntu/
``` 
Copy dummy data:

```bash 
scp -i ~/.aws/keys/dev.pem weblogstreaming/logstashdata.tar.gz  ubuntu@[AWS public IP]:/home/ubuntu 
``` 
Connect ssh the one of the cluster machine: 
```bash 
ssh -i ~/.aws/keys/dev.pem ubuntu@[AWS Public IP]
``` 
Create web_apache schema: 
```bash 
sudo /usr/share/crate/bin/crash  < webApacheSchema.sql
``` 
Check created entity:

```bash 
sudo /usr/share/crate/bin/crash -c "select schema_name, table_name,number_of_shards,number_of_replicas from information_schema.tables where table_name like 'web%'"
``` 
Output:  
>  
>+-------------+------------+------------------+--------------------+  
>| schema_name | table_name | number_of_shards | number_of_replicas |  
>+-------------+------------+------------------+--------------------+  
>| doc         | web_apache |                5 | 0-all              |  
>+-------------+------------+------------------+--------------------+  
>SELECT 1 row in set (0.001 sec)   
>  

Connect ssh the one of the others cluster machines and run command from above and you will see the same results  


Now let's make some dummy inserts:

Extract data from logstashdata.tar.gz:  
```bash  
tar -xvf logstashdata.tar.gz
```  
Import dummy Data :  
```bash  
sudo /usr/share/crate/bin/crash -c "copy web_apache from '/home/ubuntu/logstashdata/apache_2016-01-21 23:44:21,000.json'"  
```  
Output:   
>COPY OK, 125 rows affected (4.089 sec)   

Visualize data:  
```bash  
 sudo /usr/share/crate/bin/crash -c "select distinct  clientip,count,timestamp from web_apache limit 10"
```  
Output:
>
> +---------------+-------+---------------------------+  
> | clientip      | count | timestamp                 |  
> +---------------+-------+---------------------------+  
> | 71.94.126.153 |     1 | 21/Jan/2016:3:44:21 -2000 |  
> +---------------+-------+---------------------------+  
> SELECT 1 row in set (0.016 sec)  
>  

```bash  
 sudo /usr/share/crate/bin/crash -c "select count(1) from web_apache"
``` 
Output:
>
> +----------+  
> | count(1) |  
> +----------+  
> |      125 |  
> +----------+  
> SELECT 1 row in set (0.003 sec)  
>  
 
Connect ssh the one of the others cluster machines and run command from above and you will see the same results 

Now let's delete one row:
```bash 
sudo /usr/share/crate/bin/crash -c "delete from web_apache where bytes='79581'"
``` 
Output:

> DELETE OK, -1 rows affected (0.109 sec)

```bash  
 sudo /usr/share/crate/bin/crash -c "select count(1) from web_apache"
``` 
 Output:
>
> +----------+  
> | count(1) |  
> +----------+  
> |      124 |  
> +----------+  
> SELECT 1 row in set (0.003 sec)  
>  

All cluster machines contains 124 records.    

Node names are generated dynamically on startup, so you're relieved  
from configuring them manually. You can tie this node to a specific name:  

> node.name: "Franz Kafka"  

Every node can be configured to allow or deny being eligible as the master,  
and to allow or deny to store the data.  

Allow this node to be eligible as a master node (enabled by default):  

> node.master: true  

Allow this node to store data (enabled by default):  

> node.data: true

You can exploit these settings to design advanced cluster topologies.  

1. You want this node to never become a master node, only to hold data.  
    This will be the "workhorse" of your cluster.

> node.master: false  
> node.data: true

2. You want this node to only serve as a master: to not store any data and
    to have free resources. This will be the "coordinator" of your cluster.

> node.master: true    
> node.data: false  

3. You want this node to be neither master nor data node, but
    to act as a "search load balancer" (fetching data from nodes,
    aggregating results, etc.)

> node.master: false  
> node.data: false  

Inspect the cluster state via GUI tools  
such as Crate Admin [http://[AWS public IP]:4200/admin/].  
.
Notes:  
EC2 discovery is only available on Crate version 0.51.0 or higher. 
For older versions of CRATE can be used unicast discovery:  
```bash
 sudo vi /etc/crate/crate.yml
```   
 >  1. discovery.zen.ping.multicast.enabled: false  
 >  2. discovery.zen.ping.unicast.hosts:  
 >  *  - [IP address]:4300  
 >  *  - [IP address]:4300 

#3. Exercise 

##3.1 Start an EC2 crate machine using a related AMI

Choose AMI from output of the:  

```bash
aws ec2 describe-images --filters "Name=name,Values=crate-0.51.*"
```  

>  Example:  
>  "VirtualizationType": "hvm",  
>  "Name": "crate-0.51.4-3-amzn-ami-hvm-2015.03.0.x86_64",  
>  "Hypervisor": "xen",  
>  "SriovNetSupport": "simple",  
>  "ImageId": "ami-99631cfc",  
>  "State": "available",  

```bash       
aws ec2 run-instances --image-id ami-99631cfc --instance-type m3.medium --key-name dev
```  
Check version crate-0.51.4 on web application:  

> Crate URL: http://[AWS Instance:public IP]:4200/admin

Repeat procedure for others CRATE version:  

1.  crate-0.51.*    
2.  crate-0.52.*  
3.  crate-0.53.*  
4.  crate-0.54.*     

##3.2 Start a fresh EC2 ubuntu 14.04 machine and manually install latest crate version
Start an EC2 Instance:
```bash       
aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev 
```  
Connect via ssh to new AWS instance:

```bash       
sudo ssh -i ~/.aws/keys/dev.pem ubuntu@[AWS Public IP]
```  
Manual CRATE installation:  
```bash 
sudo add-apt-repository ppa:openjdk-r/ppa -y
sudo apt-get update -y
sudo apt-get install openjdk-8-jdk -y
sudo apt-get install curl -y
bash -c "$(curl -L install.crate.io)" -y
``` 
Crate URL: http://[AWS Instance:public IP]:4200/admin    


##3.3 Crate version 0.54.x - restore from EBS device

```bash       
aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev --iam-instance-profile Name=workshop-crate --user-data file://scripts/installCrate.sh --count 1
``` 
Copy weblogstreaming/webApacheSchema.sql to the AWS instance:    

```bash  
scp -i ~/.aws/keys/dev.pem weblogstreaming/webApacheSchema.sql ubuntu@[AWS Public IP]:/home/ubuntu
``` 
Copy dummy data to the AWS instance:    

```bash  
scp -i ~/.aws/keys/dev.pem weblogstreaming/logstashdata.tar.gz  ubuntu@[AWS public IP]:/home/ubuntu
``` 
Connect ssh the AWS instance:  
```bash  
ssh -i ~/.aws/keys/dev.pem ubuntu@[AWS Public IP]
``` 
Create web_apache schema:    
```bash  
sudo /usr/share/crate/bin/crash  < webApacheSchema.sql
```  

Extract data from logstashdata.tar.gz:  
```bash  
tar -xvf logstashdata.tar.gz
```  

Import Data :  
```bash  
sudo /usr/share/crate/bin/crash -c "copy web_apache from '/home/ubuntu/logstashdata/apache_2016-01-21 23:44:21,000.json'"  
```  
Output:   
>COPY OK, 125 rows affected (4.089 sec)   

Visualize data:  
```bash  
 sudo /usr/share/crate/bin/crash -c "select distinct  clientip,count,timestamp from web_apache limit 10"
```  
Output:
>
> +---------------+-------+---------------------------+  
> | clientip      | count | timestamp                 |  
> +---------------+-------+---------------------------+  
> | 71.94.126.153 |     1 | 21/Jan/2016:3:44:21 -2000 |  
> +---------------+-------+---------------------------+  
> SELECT 1 row in set (0.016 sec)  
>  

```bash  
 sudo /usr/share/crate/bin/crash -c "select count(1) from web_apache"
``` 
Output:
>
> +----------+  
> | count(1) |  
> +----------+  
> |      125 |  
> +----------+  
> SELECT 1 row in set (0.003 sec)  
>  


Backup data(take the volume id which is attached to the instance):  

```bash  
 create-snapshot --volume-id vol-0b69fed4 --description "workshop-crate-backupDummmyData"
```  
Output:
>
> {  
>    "Description": "workshop-crate-backupDummmyData",  
>    "Encrypted": false,  
>    "VolumeId": "vol-0b69fed4",  
>    "State": "pending",  
>    "VolumeSize": 8,  
>    "Progress": "",  
>    "StartTime": "2016-02-15T11:42:19.000Z",  
>    "SnapshotId": "snap-2580d73e",  
>    "OwnerId": "026880652069"  
> }   

Create new Volume:  

```bash  
 create-snapshot --volume-id vol-0b69fed4 --description "workshop-crate-backupDummmyData"
```  
Output:  
>
> {  
>     "AvailabilityZone": "us-east-1e",  
>    "Encrypted": false,  
>    "VolumeType": "gp2",  
>    "VolumeId": "vol-306d0cef",  
>    "State": "creating",  
>    "Iops": 24,  
>    "SnapshotId": "snap-2580d73e",  
>    "CreateTime": "2016-02-15T11:52:35.116Z",  
>    "Size": 8  
> }    

Stop the Instance:  

```bash  
 aws ec2 stop-instances --instance-ids i-91387e11
```  
Output:  

>
> {  
>    "StoppingInstances": [  
>        {  
>            "InstanceId": "i-91387e11",  
>            "CurrentState": {  
>                "Code": 64,  
>                "Name": "stopping"  
>            },  
>            "PreviousState": {  
>                "Code": 16,  
>                "Name": "running"  
>            }  
>        }  
>    ]  
> }    
>    

Create new instance:    

```bash       
aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev --iam-instance-profile Name=workshop-crate
```  
Stop new instance and detach the EBS:  
 
```bash 
 aws ec2 stop-instances --instance-ids i-7e99c9fe
 aws ec2 detach-volume --volume-id vol-087213d7
```  
Output:  
>   
> {  
>    "AttachTime":   
> "2016-02-15T11:59:34.000Z",  
>    "InstanceId": "i-7e99c9fe",  
>    "VolumeId": "vol-087213d7",  
>    "State": "detaching",  
>    "Device": "/dev/sda1"  
> }  
>  

Attach backup volume:  

```bash 
aws ec2 attach-volume --volume-id vol-306d0cef --instance-id i-7e99c9fe --device /dev/sda1
```  
Output:    

> {  
>    "AttachTime":   
> "2016-02-15T12:08:16.536Z",  
>    "InstanceId": "i-7e99c9fe",  
>    "VolumeId": "vol-306d0cef",  
>    "State": "attaching",  
>    "Device": "/dev/sda1"  
> }  
>  

Start instance:  

```bash 
 aws ec2 start-instances --instance-ids i-7e99c9fe
```  
Output:  
>
>{  
>    "StartingInstances": [  
>        {  
>            "InstanceId": "i-7e99c9fe",  
>            "CurrentState": {  
>                "Code": 0,  
>                "Name": "pending"  
>            },  
>            "PreviousState": {  
>                "Code": 80,  
>                "Name": "stopped"  
>            }  
>        }  
>    ]  
> }  
>   

Connect ssh to the instance:  

```bash 
 sudo ssh -i ~/.aws/keys/dev.pem ubuntu@54.146.179.53
```  
Run the same query , you will have the same results:  

```bash  
 sudo /usr/share/crate/bin/crash -c "select distinct  clientip,count,timestamp from web_apache limit 10"
```  
Output:  
>   
> +---------------+-------+---------------------------+  
> | clientip      | count | timestamp                 |  
> +---------------+-------+---------------------------+  
> | 71.94.126.153 |     1 | 21/Jan/2016:3:44:21 -2000 |  
> +---------------+-------+---------------------------+  
> SELECT 1 row in set (0.010 sec)  
>  

```bash  
 sudo /usr/share/crate/bin/crash -c "select count(1) from web_apache"
``` 
Output:  
>  
> +----------+  
> | count(1) |  
> +----------+  
> |      125 |  
> +----------+  
> SELECT 1 row in set (0.003 sec)  
>    

You can check also the crate Web application:

> Crate URL: http://[AWS Instance:public IP]:4200/admin 

##3.4 Crate - EC2 discovery

Start one master EC2 create machine having ec2 discovery enabled:  

```bash 
 aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev --iam-instance-profile Name=workshop-crate --user-data file://scripts/install_workshop-crate_cluster.sh --count 1             
```  

Start a second crate EC2 instance having ec2 discovery enabled:  

```bash 
 aws ec2 run-instances --image-id ami-84562dec --instance-type m1.medium --key-name dev --iam-instance-profile Name=workshop-crate --user-data file://scripts/install_workshop-crate_cluster.sh --count 1             
```  

The cluster workshop-crate is automatically created.  

The script which install crate and create workshop-crate cluster is scripts/install_workshop-crate_cluster.sh"  
Cluster name identifies your cluster for auto-discovery. If you're running
multiple clusters on the same network, make sure you're using unique names.

> cluster.name: workshop-crate



To enable EC2 discovery you need to set the discovery type to 'ec2'.

> discovery.type: ec2



Set to ensure a node sees M other master eligible nodes to be considered
operational within the cluster. Its recommended to set it to a higher value
than 1 when running more than 2 nodes in the cluster.

We highly recommend to set the minimum master nodes as follows:
minimum_master_nodes: (N / 2) + 1 where N is the cluster size
That will ensure a full recovery of the cluster state.

>discovery.zen.minimum_master_nodes: 2


The gateway persists cluster meta data on disk every time the meta data
changes. This data is stored persistently across full cluster restarts
and recovered after nodes are started again.

recover_after_nodes:
 	Defines the number of nodes that need to be started before any cluster
state recovery will start.


>gateway.recover_after_nodes: 3

recover_after_time: 
Defines the time to wait before starting the recovery once the number
of nodes defined in gateway.recover_after_nodes are started.

>gateway.recover_after_time: 5m


gateway.expected_nodes:  
Defines how many nodes should be waited for until the cluster state is
recovered immediately. The value should be equal to the number of nodes
in the cluster.

> gateway.expected_nodes: 3  


#4.	Crate DDL and DML 

##4.1 DDL create table, drop table, sharding (define on table level) 


Copy workshop/createtables_workshop_crate.sql to the Crate instance

```bash 
scp -i ~/.aws/keys/dev.pem workspace/project_crate_demos/workshop/createtables_workshop_crate.sql ubuntu@54.146.179.53:/home/ubuntu
``` 

Connect ssh to the ec2 machine created above:  

```bash 
 sudo ssh -i ~/.aws/keys/dev.pem ubuntu@54.146.179.53
```  

 Create workshop tables :  
 
```bash 
      sudo /usr/share/crate/bin/crash < createtables_workshop_crate.sql
```  
Output:
>  
> CREATE OK (0.091 sec)  
> CREATE OK (0.253 sec)  
>

 Check what entities were created:
 
```bash 
 sudo /usr/share/crate/bin/crash -c "select schema_name, table_name,number_of_shards,number_of_replicas from information_schema.tables where table_name like 'workshop%'"
```  
 
 Output:
 
> 
> +-------------+------------------+------------------+--------------------+  
> | schema_name | table_name       | number_of_shards | number_of_replicas |  
> +-------------+------------------+------------------+--------------------+  
> | doc         | workshop_cities  |                6 | 0-all              |  
> | doc         | workshop_weather |                6 | 0-all              |  
> +-------------+------------------+------------------+--------------------+  
> SELECT 2 rows in set (0.007 sec)  
>  
 
Were created 2 tables : workshop_cities and workshop_weather with 6 shards and expand the number of replicas to the available number of nodes.  

Let's see it with more details:

```bash 
  sudo /usr/share/crate/bin/crash -c "show create table workshop_weather"
```  
Output:  
> 
>+-------------------------------------------------------+  
>| SHOW CREATE TABLE doc.workshop_weather                |  
>+-------------------------------------------------------+  
>| CREATE TABLE IF NOT EXISTS "doc"."workshop_weather" ( |  
>|    "city" STRING,                                     |  
>|    "datetime" TIMESTAMP,                              |  
>|    "prcp" DOUBLE,                                     |  
>|    "temp_hi" INTEGER,                                 |  
>|    "temp_low" INTEGER,                                |  
>|    PRIMARY KEY ("city", "datetime")                   |  
>| )                                                     |  
>| CLUSTERED BY ("city") INTO 6 SHARDS                   |  
>| WITH (                                                |  
>|    "blocks.metadata" = false,                         |  
>|    "blocks.read" = false,                             |  
>|    "blocks.read_only" = false,                        |  
>|    "blocks.write" = false,                            |  
>|    column_policy = 'dynamic',                         |  
>|    "gateway.local.sync" = 5000,                       |  
>|    number_of_replicas = '0-all',                      |  
>|    "recovery.initial_shards" = 'quorum',              |  
>|    refresh_interval = 1000,                           |  
>|    "routing.allocation.enable" = 'all',               |  
>|    "routing.allocation.total_shards_per_node" = -1,   |  
>|    "translog.disable_flush" = false,                  |  
>|    "translog.flush_threshold_ops" = 2147483647,       |  
>|    "translog.flush_threshold_period" = 1800000,       |  
>|    "translog.flush_threshold_size" = 209715200,       |  
>|    "translog.interval" = 5000,                        |  
>|    "unassigned.node_left.delayed_timeout" = 60000,    |  
>|    "warmer.enabled" = true                            |  
>| )                                                     |  
>+-------------------------------------------------------+  
>  

Sharding ? Replication ? What does it means ? Why do we need it?

A database shard is a horizontal partition of data in a database or search engine. Each individual partition is referred to as a shard or database shard. Each shard is held on a separate database server instance, to spread load.
Sharding is increasingly popular method in any data storage system and is a favorite choice for scaling the read and write throughput. For any data-centric application, the data grows much more rapidly and thus, the traditional vertical scaling approach which involves more and more computing power on a single machine soon starts to become a costly and there is always a practical limit to what we can scale vertically. With sharding, the entire data set is distributed over number of nodes as shards thus reducing the number of operations each shard has to handle and making it possible to store massive amount of data while still scaling in terms of data related operations.
One of the finest things about Crate is it eliminates need to glue number of technologies together to achieve your big data dream and thus is a breeze for developers. Crate further simplifies database sharding and replication since it performs automatic sharding and replication thus reducing overhead for datastore administrators.
With the introduction of sharding, there comes a problem of identifying shard to which a particular document belongs to.
 
table sys.shards stores all the real-time statistics for all shards of all non-system tables.

Now let's have a look:
```bash 
 sudo /usr/share/crate/bin/crash -c "select schema_name,table_name,id,num_docs,primary,size,state from sys.shards where table_name like 'workshop%' order by id"
```  
 Output:
 
> 
>+-------------+------------------+----+----------+---------+------+---------+  
>| schema_name | table_name       | id | num_docs | primary | size | state   |  
>+-------------+------------------+----+----------+---------+------+---------+  
>| doc         | workshop_cities  |  0 |        1 | FALSE   | 3266 | STARTED |  
>| doc         | workshop_cities  |  0 |        1 | FALSE   | 3266 | STARTED |  
>| doc         | workshop_weather |  0 |       10 | FALSE   | 5824 | STARTED |  
>| doc         | workshop_weather |  0 |       10 | FALSE   | 5824 | STARTED |  
>| doc         | workshop_cities  |  0 |        1 | TRUE    | 3266 | STARTED |  
>| doc         | workshop_weather |  0 |       10 | TRUE    | 5824 | STARTED |  
>| doc         | workshop_cities  |  1 |        1 | FALSE   | 3282 | STARTED |  
>| doc         | workshop_cities  |  1 |        1 | FALSE   | 3282 | STARTED |  
>| doc         | workshop_weather |  1 |       10 | FALSE   | 5898 | STARTED |  
>| doc         | workshop_weather |  1 |       10 | FALSE   | 5898 | STARTED |  
>| doc         | workshop_cities  |  1 |        1 | TRUE    | 3282 | STARTED |  
>| doc         | workshop_weather |  1 |       10 | TRUE    | 5898 | STARTED |  
>| doc         | workshop_cities  |  2 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  2 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  2 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  2 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  2 |        0 | TRUE    |  144 | STARTED |  
>| doc         | workshop_weather |  2 |        0 | TRUE    |  144 | STARTED |  
>| doc         | workshop_cities  |  3 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  3 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  3 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  3 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  3 |        0 | TRUE    |  144 | STARTED |  
>| doc         | workshop_weather |  3 |        0 | TRUE    |  144 | STARTED |  
>| doc         | workshop_cities  |  4 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  4 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  4 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  4 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  4 |        0 | TRUE    |  144 | STARTED |  
>| doc         | workshop_weather |  4 |        0 | TRUE    |  144 | STARTED |  
>| doc         | workshop_cities  |  5 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  5 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  5 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_weather |  5 |        0 | FALSE   |  144 | STARTED |  
>| doc         | workshop_cities  |  5 |        0 | TRUE    |  144 | STARTED |  
>| doc         | workshop_weather |  5 |        0 | TRUE    |  144 | STARTED |  
>+-------------+------------------+----+----------+---------+------+---------+  
>SELECT 36 rows in set (0.017 sec)  
>  

Replication is an essential element for highly available data stores and since Crate aims to be one, it offers support for replication on table-basis. Replication is primarily helpful for real good read performance and thus is a good candidate for business intelligence tools where data are mostly read. Replication of a table in Crate means that each primary shard of a table is stored additionally on so called secondary shards. Crate by default creates one replica thus the content of table is stored twice across the cluster nodes.

number_of_replicas:

	Range	Explanation  
	0-1	Will create 0 or 1 replicas depending on the number of available nodes  
	2-4	Table requires at least 2 replicas to be fully replicated. Will create up to 4 if nodes are added.  
	0-all	Will expand the number of replicas to the available number of nodes.  

Now a few words about constrains in Crate:  
 Table constraints are constraints that are applied to more than one column or to the table as a whole. The only table constraints currently supported are:  
 
	PRIMARY KEY Constraint
	     in our case:
	     ........................
	         primary key (city, datetime)
	     ............................
	INDEX Constraint
	     in our case :
	     .............................................
	         clustered by (city) into 6 shards
				WITH (
				    number_of_replicas = '0-all',
				    refresh_interval = 1000
				);
		  ..............................................

Drop workshop tables:  

 
```bash 
sudo /usr/share/crate/bin/crash -c "drop table workshop_weather"
sudo /usr/share/crate/bin/crash -c "drop table workshop_cities"
``` 
 
##4.2 DML  insert, update, delete 

Let's insert some dummy data into table created at previous step:  

Copy the workshop/insert_workshop_data.sql to the Crate instance:  

```bash 
scp -i ~/.aws/keys/dev.pem workspace/project_crate_demos/workshop/insert_workshop_data.sql   ubuntu@54.146.179.53:/home/ubuntu
``` 
Connect ssh to the ec2 machine:  

```bash 
 sudo ssh -i ~/.aws/keys/dev.pem ubuntu@54.146.179.53
```  
Insert dummy data:  

```bash 
 sudo /usr/share/crate/bin/crash < insert_workshop_data.sql
```  
Output:
>INSERT OK, 1 row affected (0.008 sec)  
>INSERT OK, 1 row affected (0.002 sec)  
>INSERT OK, 1 row affected (0.002 sec)  
>INSERT OK, 1 row affected (0.005 sec)  
>INSERT OK, 1 row affected (0.002 sec)  
>INSERT OK, 1 row affected (0.002 sec)  
>INSERT OK, 1 row affected (0.005 sec)  
>INSERT OK, 1 row affected (0.002 sec)  
>INSERT OK, 1 row affected (0.002 sec)  
>INSERT OK, 1 row affected (0.002 sec)  
>INSERT OK, 1 row affected (0.004 sec)  
>INSERT OK, 1 row affected (0.003 sec)  

Now let's count the records:

```bash 
 sudo /usr/share/crate/bin/crash -c "select count(1) from workshop_weather"
``` 
 Output:
>  
>+----------+    
>| count(1) |  
>+----------+  
>|       10 |  
>+----------+  
>SELECT 1 row in set (0.006 sec)         
>  

```bash 
 sudo /usr/share/crate/bin/crash -c "select count(1) from workshop_cities"
``` 
 Output:  
>  
>+----------+    
>| count(1) |  
>+----------+  
>|       2 |  
>+----------+  
>SELECT 1 row in set (0.008 sec)         
>  

Data from workshop cities:
```bash 
 sudo /usr/share/crate/bin/crash -c "select * from workshop_cities"
``` 
Output:  
> +---------+--------------------------+---------------+  
> | country | location                 | name          |  
> +---------+--------------------------+---------------+  
> | US      | [-118.237367, 34.060009] | Los Angeles   |  
> | US      | [-122.409107, 37.775049] | San Francisco |  
> +---------+--------------------------+---------------+  
> SELECT 2 rows in set (0.017 sec)  



Now let's delete the data inserted and make the same inserts but using json file:

Delete data:
```bash 
sudo /usr/share/crate/bin/crash -c "delete from workshop_weather"
``` 
Copy the workshop/insert_workshop_data.sql to the Crate instance:  

```bash 
scp -i ~/.aws/keys/dev.pem workspace/project_crate_demos/workshop/weather_data.json   ubuntu@54.146.179.53:/home/ubuntu
``` 

Import Data:   
```bash 
 sudo /usr/share/crate/bin/crash -c "copy workshop_weather from '/home/ubuntu/weather_data.json'"
``` 
Output:  
> COPY OK, 10 rows affected (0.039 sec)


Join tables:

```bash 
sudo /usr/share/crate/bin/crash -c "SELECT * FROM workshop_weather, workshop_cities WHERE city = name;"
``` 

Export data:

```bash 
 sudo /usr/share/crate/bin/crash -c "copy workshop_weather to '/tmp/weather_data_export.json'"
``` 

#5 Amount of data  


Copy the workshop/weblogstreaming/webApacheAmountDataSchemas.sql to the Crate instance:  

```bash 
scp -i ~/.aws/keys/dev.pem wworkshop/weblogstreaming/webApacheAmountDataSchemas.sql   ubuntu@54.146.179.53:/home/ubuntu
``` 

Create Tables:

```bash 
 sudo /usr/share/crate/bin/crash  < /home/ubuntu/webApacheAmountDataSchemas.sql
``` 


Import data:

```bash 
  unzip weblogstreaming/crate-client-1.1.zip  
  tar -xvf weblogstreaming/logstashdatabig.tar.gz  
 crate-client-1.1/bin/crate-client -c 54.163.98.22:4300 -p /home/user/workspace/project_crate_demos/workshop/logstashdata/ -t workshopcrate
``` 
The data import will take a few minutes are ~1GB dates

Let's play a little bit with the queries on this structure of the data:

Inner join - nner Joins require each record of one table to have matching records on the other table:

```bash 
sudo /usr/share/crate/bin/crash -c "SELECT date_format(datetime),bytes,response,log.clientip,geolocation from web_apache_log log,web_apache_client_info info where log.clientip=info.clientip  and ipinfo_city='Beijing' limit 10"
``` 

Limitations:  

  - AGGREGATIONS on CROSS JOIN is not supported  
   
```bash 
sudo /usr/share/crate/bin/crash -c "SELECt count(1) from web_apache_log log,web_apache_client_info info where log.clientip=info.clientip  and ipinfo_city='San Francisco'"
``` 
  - GROUP BY on CROSS JOIN is not supported   
  
```bash 
sudo /usr/share/crate/bin/crash -c "SELECT info.clientip,sum(cast(bytes as integer)) from web_apache_log log,web_apache_client_info info where log.clientip=info.clientip and ipinfo_city='San Francisco' group by info.clientip"
```  
#6 Application development- GuestBook
[app_guestbook!](app_guestbook/README.md)

 
   
  

