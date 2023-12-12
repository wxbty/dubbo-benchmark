#!/bin/sh

# $1: the path of dubbo-continues-testing
# $2: the version of dubbo-continues-testing

cd $1/../dubbo
git pull
mvn clean install -Dmaven.test.skip=true

cd $1/dubbo-continues-testing-demo
mvn clean install -Drevision=$2
java -jar $1/dubbo-continues-testing-demo/target/testing-demo.jar

