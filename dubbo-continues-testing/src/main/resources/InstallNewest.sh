#!/bin/sh

# $1: the path of dubbo-continues-testing
# $2: the version of dubbo-continues-testing

cd $1/../jmh_demo
git pull
mvn clean install

cd $1/dubbo-continues-testing-demo
mvn clean install -DsiteVersion=$2
java -jar $1/dubbo-continues-testing-demo/target/testing-demo.jar

