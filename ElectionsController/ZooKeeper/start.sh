#!/bin/sh
cd zookeeper-3.4.14
cp ../zoo.cfg .
sed -i "s/ZOOKEEPER1/$ZOOKEEPER1/g" zoo.cfg
sed -i "s/ZOOKEEPER2/$ZOOKEEPER2/g" zoo.cfg
sed -i "s/ZOOKEEPER3/$ZOOKEEPER3/g" zoo.cfg
cp zoo.cfg ./conf/
echo $MYID > /data/zookeeper/myid
./bin/zkServer.sh start-foreground