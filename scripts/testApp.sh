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
mvn -ntp liberty:start
mvn -ntp verify
mvn -ntp verify -Dtest.protocol=http
mvn -ntp liberty:stop
docker stop postgres-container
docker rm postgres-container

