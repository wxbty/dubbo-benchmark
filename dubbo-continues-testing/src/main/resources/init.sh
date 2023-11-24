#!/bin/sh

cd /xxx
git pull
mvn clean package -DskipTests
cd /xxx/dubbo-continues-testing

