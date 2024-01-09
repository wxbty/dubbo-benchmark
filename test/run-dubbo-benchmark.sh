#!/bin/sh

DIR=../dubbo-continues-testing-demo/
cd $DIR
mvn clean package -DskipTests
java -jar target/testing-demo.jar &> continues-bh.log

