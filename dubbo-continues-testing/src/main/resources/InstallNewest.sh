#!/bin/sh

# $1: the path of dubbo-continues-testing
# $2: the version of dubbo-continues-testing

cd $1/../dubbo
git pull
sudo -u zcy mvn clean install -Drevision=$2 -Dmaven.test.skip=true --settings /Users/zcy/Documents/work/tool/apache-maven-3.8.5/settings.xml

cd $1/dubbo-continues-testing-demo
mvn clean install -Drevision=$2  --settings /Users/zcy/Documents/work/tool/apache-maven-3.8.5/settings.xml
java -jar $1/dubbo-continues-testing-demo/target/testing-demo.jar

