#!/bin/sh

# $1: the path of dubbo-continues-testing
# $2: the version of dubbo-continues-testing
cd ~/work/dubbo
git pull
#sudo -u $(whoami) mvn clean install -Drevision=$2 -Dmaven.test.skip=true --settings ~/work/settings.xml
mvn clean install -Drevision=$1 -Dmaven.test.skip=true --settings ~/work/settings.xml

cd ~/work/dubbo-benchmark/dubbo-continues-testing-demo
mvn clean install -Drevision=$1  --settings ~/work/settings.xml
java -jar ~/work/dubbo-benchmark/dubbo-continues-testing-demo/target/testing-demo.jar

