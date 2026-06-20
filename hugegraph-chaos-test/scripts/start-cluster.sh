#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
set -euo pipefail

PD_COUNT=${1:-3}
STORE_COUNT=${2:-3}
SERVER_COUNT=${3:-3}

HOME_DIR=$(pwd)

PROPERTIES_FILE="$HOME_DIR/hugegraph-commons/hugegraph-common/src/main/resources/version.properties"
if [ -f "$PROPERTIES_FILE" ]; then
    set -a
    source "$PROPERTIES_FILE"
    set +a
else
    echo "Error: properties file not found at $PROPERTIES_FILE"
    exit 1
fi

PD_DIR="$HOME_DIR/hugegraph-pd/apache-hugegraph-pd-$VersionInBash"
STORE_DIR="$HOME_DIR/hugegraph-store/apache-hugegraph-store-$VersionInBash"
SERVER_DIR="$HOME_DIR/hugegraph-server/apache-hugegraph-server-$VersionInBash"

PD_BASE_GRPC=8686
PD_BASE_REST=8620
PD_BASE_RAFT=8610

STORE_BASE_GRPC=8500
STORE_BASE_REST=8520
STORE_BASE_RAFT=8510

SERVER_BASE_REST=8080
SERVER_BASE_RPC=8091

PD_RAFT_PEERS=""
PD_GRPC_LIST=""
STORE_GRPC_LIST=""

for i in $(seq 0 $((PD_COUNT - 1))); do
    RAFT_PORT=$((PD_BASE_RAFT + i))
    if [ -n "$PD_RAFT_PEERS" ]; then
        PD_RAFT_PEERS="${PD_RAFT_PEERS},"
    fi
    PD_RAFT_PEERS="${PD_RAFT_PEERS}127.0.0.1:${RAFT_PORT}"
done

for i in $(seq 0 $((STORE_COUNT - 1))); do
    GRPC_PORT=$((STORE_BASE_GRPC + i))
    if [ -n "$STORE_GRPC_LIST" ]; then
        STORE_GRPC_LIST="${STORE_GRPC_LIST},"
    fi
    STORE_GRPC_LIST="${STORE_GRPC_LIST}127.0.0.1:${GRPC_PORT}"
done

for i in $(seq 0 $((PD_COUNT - 1))); do
    GRPC_PORT=$((PD_BASE_GRPC + i))
    if [ -n "$PD_GRPC_LIST" ]; then
        PD_GRPC_LIST="${PD_GRPC_LIST},"
    fi
    PD_GRPC_LIST="${PD_GRPC_LIST}127.0.0.1:${GRPC_PORT}"
done

function sed_in_place() {
    local expression=$1
    local file=$2
    case "$(uname)" in
        Darwin) sed -i '' "$expression" "$file" ;;
        *) sed -i "$expression" "$file" ;;
    esac
}

echo "=== Starting ${PD_COUNT} PD nodes ==="
for i in $(seq 0 $((PD_COUNT - 1))); do
    GRPC_PORT=$((PD_BASE_GRPC + i))
    REST_PORT=$((PD_BASE_REST + i))
    RAFT_PORT=$((PD_BASE_RAFT + i))
    DATA_PATH="./pd_data_${i}"

    INSTANCE_DIR="${PD_DIR}_${i}"
    cp -r "$PD_DIR" "$INSTANCE_DIR"

    export SPRING_APPLICATION_JSON="{\"grpc\":{\"host\":\"127.0.0.1\",\"port\":\"${GRPC_PORT}\"},\"server\":{\"port\":\"${REST_PORT}\"},\"raft\":{\"address\":\"127.0.0.1:${RAFT_PORT}\",\"peers-list\":\"${PD_RAFT_PEERS}\"},\"pd\":{\"data-path\":\"${DATA_PATH}\",\"initial-store-list\":\"${STORE_GRPC_LIST}\",\"initial-store-count\":${STORE_COUNT}}}"

    echo "Starting PD node ${i}: grpc=${GRPC_PORT}, rest=${REST_PORT}, raft=${RAFT_PORT}"
    pushd "$INSTANCE_DIR"
    bash bin/start-hugegraph-pd.sh -j "-Xms256m -Xmx512m" || (cat logs/hugegraph-pd.log 2>/dev/null; exit 1)
    popd
    unset SPRING_APPLICATION_JSON
    sleep 5
done

echo "=== Starting ${STORE_COUNT} Store nodes ==="
for i in $(seq 0 $((STORE_COUNT - 1))); do
    GRPC_PORT=$((STORE_BASE_GRPC + i))
    REST_PORT=$((STORE_BASE_REST + i))
    RAFT_PORT=$((STORE_BASE_RAFT + i))
    DATA_PATH="./storage_${i}"

    INSTANCE_DIR="${STORE_DIR}_${i}"
    cp -r "$STORE_DIR" "$INSTANCE_DIR"

    export SPRING_APPLICATION_JSON="{\"pdserver\":{\"address\":\"${PD_GRPC_LIST}\"},\"grpc\":{\"host\":\"127.0.0.1\",\"port\":\"${GRPC_PORT}\"},\"raft\":{\"address\":\"127.0.0.1:${RAFT_PORT}\"},\"server\":{\"port\":\"${REST_PORT}\"},\"app\":{\"data-path\":\"${DATA_PATH}\"}}"

    echo "Starting Store node ${i}: grpc=${GRPC_PORT}, rest=${REST_PORT}, raft=${RAFT_PORT}"
    pushd "$INSTANCE_DIR"
    bash bin/start-hugegraph-store.sh -j "-Xms256m -Xmx512m" || (cat logs/hugegraph-store.log 2>/dev/null; exit 1)
    popd
    unset SPRING_APPLICATION_JSON
    sleep 5
done

echo "=== Starting ${SERVER_COUNT} Server nodes ==="
for i in $(seq 0 $((SERVER_COUNT - 1))); do
    REST_PORT=$((SERVER_BASE_REST + i))
    RPC_PORT=$((SERVER_BASE_RPC + i))

    INSTANCE_DIR="${SERVER_DIR}_${i}"
    cp -r "$SERVER_DIR" "$INSTANCE_DIR"

    CONF="${INSTANCE_DIR}/conf/graphs/hugegraph.properties"
    REST_CONF="${INSTANCE_DIR}/conf/rest-server.properties"

    sed_in_place "s|backend=.*|backend=hstore|g" "$CONF"
    sed_in_place "s|serializer=.*|serializer=binary|g" "$CONF"

    if grep -q "pd.peers" "$CONF"; then
        sed_in_place "s|pd.peers=.*|pd.peers=${PD_GRPC_LIST}|g" "$CONF"
    else
        echo "pd.peers=${PD_GRPC_LIST}" >> "$CONF"
    fi

    sed_in_place "s|restserver.url=.*|restserver.url=http://127.0.0.1:${REST_PORT}|g" "$REST_CONF"
    sed_in_place "s|rpc.server_port=.*|rpc.server_port=${RPC_PORT}|g" "$REST_CONF"

    if grep -q "usePD" "$REST_CONF"; then
        sed_in_place "s|usePD=.*|usePD=true|g" "$REST_CONF"
    else
        echo "usePD=true" >> "$REST_CONF"
    fi

    if grep -q "server.id" "$REST_CONF"; then
        sed_in_place "s|server.id=.*|server.id=server${i}|g" "$REST_CONF"
    else
        echo "server.id=server${i}" >> "$REST_CONF"
    fi

    if [ "$i" -eq 0 ]; then
        ROLE="master"
    else
        ROLE="worker"
    fi

    if grep -q "server.role" "$REST_CONF"; then
        sed_in_place "s|server.role=.*|server.role=${ROLE}|g" "$REST_CONF"
    else
        echo "server.role=${ROLE}" >> "$REST_CONF"
    fi

    echo "Starting Server node ${i}: rest=${REST_PORT}, rpc=${RPC_PORT}, role=${ROLE}"
    pushd "$INSTANCE_DIR"
    echo -e "pa" | bash bin/init-store.sh
    bash bin/start-hugegraph.sh -j "-Xms256m -Xmx512m" -t 120 || (cat logs/hugegraph-server.log 2>/dev/null; exit 1)
    popd
    sleep 5
done

echo "=== Cluster started: ${PD_COUNT} PD + ${STORE_COUNT} Store + ${SERVER_COUNT} Server ==="
