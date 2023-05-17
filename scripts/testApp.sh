#!/bin/bash
set -euxo pipefail

cd postgres
docker build -t postgres-sample .

cd ../inventory
mvn -ntp -Dhttp.keepAlive=false \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -q clean package liberty:create liberty:install-feature liberty:deploy
docker build -t inventory:1.0-SNAPSHOT .

# Test by Testcontainers
export TESTCONTAINERS_RYUK_DISABLED=true
mvn -ntp verify
mvn -ntp verify -Dtest.protocol=http

# Test by Liberty runtime
docker run --name postgres-container -p 5432:5432 -d postgres-sample
if [ -d "/home/project" ]; then
    mvn -ntp liberty:start
else
    POSTGRES_IP=`docker inspect -f "{{.NetworkSettings.IPAddress }}" postgres-container`
    mvn -ntp liberty:start -Ddb.hostname=$POSTGRES_IP
fi
mvn -ntp verify
mvn -ntp verify -Dtest.protocol=http
mvn -ntp liberty:stop
docker stop postgres-container
docker rm postgres-container

