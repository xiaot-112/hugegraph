#!/bin/bash
set -euo pipefail

PD_COUNT=${1:-3}
STORE_COUNT=${2:-3}
SERVER_COUNT=${3:-3}

VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
PD_DIR="hugegraph-pd/apache-hugegraph-pd-${VERSION}"
STORE_DIR="hugegraph-store/apache-hugegraph-store-${VERSION}"
SERVER_DIR="hugegraph-server/apache-hugegraph-server-${VERSION}"

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

echo "=== Starting ${PD_COUNT} PD nodes ==="
for i in $(seq 0 $((PD_COUNT - 1))); do
    GRPC_PORT=$((PD_BASE_GRPC + i))
    REST_PORT=$((PD_BASE_REST + i))
    RAFT_PORT=$((PD_BASE_RAFT + i))
    DATA_PATH="./pd_data_${i}"

    INSTANCE_DIR="${PD_DIR}_${i}"
    cp -r "$PD_DIR" "$INSTANCE_DIR"

    export SPRING_APPLICATION_JSON="{
      \"grpc\":   { \"host\": \"127.0.0.1\", \"port\": \"${GRPC_PORT}\" },
      \"server\": { \"port\": \"${REST_PORT}\" },
      \"raft\":   { \"address\": \"127.0.0.1:${RAFT_PORT}\", \"peers-list\": \"${PD_RAFT_PEERS}\" },
      \"pd\":     { \"data-path\": \"${DATA_PATH}\", \"initial-store-list\": \"${STORE_GRPC_LIST}\", \"initial-store-count\": ${STORE_COUNT} }
    }"

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

    export SPRING_APPLICATION_JSON="{
      \"pdserver\": { \"address\": \"${PD_GRPC_LIST}\" },
      \"grpc\":     { \"host\": \"127.0.0.1\", \"port\": \"${GRPC_PORT}\" },
      \"raft\":     { \"address\": \"127.0.0.1:${RAFT_PORT}\" },
      \"server\":   { \"port\": \"${REST_PORT}\" },
      \"app\":      { \"data-path\": \"${DATA_PATH}\" }
    }"

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

    sed -i "s|backend=.*|backend=hstore|g" "$CONF"
    sed -i "s|serializer=.*|serializer=binary|g" "$CONF"
    sed -i "s|pd.peers=.*|pd.peers=${PD_GRPC_LIST}|g" "$CONF"
    if ! grep -q "pd.peers" "$CONF"; then
        echo "pd.peers=${PD_GRPC_LIST}" >> "$CONF"
    fi

    sed -i "s|restserver.url=.*|restserver.url=http://127.0.0.1:${REST_PORT}|g" "$REST_CONF"
    sed -i "s|rpc.server_port=.*|rpc.server_port=${RPC_PORT}|g" "$REST_CONF"
    if ! grep -q "usePD" "$REST_CONF"; then
        echo "usePD=true" >> "$REST_CONF"
    else
        sed -i "s|usePD=.*|usePD=true|g" "$REST_CONF"
    fi
    if ! grep -q "server.id" "$REST_CONF"; then
        echo "server.id=server${i}" >> "$REST_CONF"
    else
        sed -i "s|server.id=.*|server.id=server${i}|g" "$REST_CONF"
    fi
    if [ "$i" -eq 0 ]; then
        ROLE="master"
    else
        ROLE="worker"
    fi
    if ! grep -q "server.role" "$REST_CONF"; then
        echo "server.role=${ROLE}" >> "$REST_CONF"
    else
        sed -i "s|server.role=.*|server.role=${ROLE}|g" "$REST_CONF"
    fi

    echo "Starting Server node ${i}: rest=${REST_PORT}, rpc=${RPC_PORT}, role=${ROLE}"
    pushd "$INSTANCE_DIR"
    echo -e "pa" | bash bin/init-store.sh
    bash bin/start-hugegraph.sh -j "-Xms256m -Xmx512m" -t 120 || (cat logs/hugegraph-server.log 2>/dev/null; exit 1)
    popd
    sleep 5
done

echo "=== Cluster started: ${PD_COUNT} PD + ${STORE_COUNT} Store + ${SERVER_COUNT} Server ==="
