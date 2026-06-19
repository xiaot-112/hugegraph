#!/bin/bash
set -euo pipefail

VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
PD_DIR="hugegraph-pd/apache-hugegraph-pd-${VERSION}"
STORE_DIR="hugegraph-store/apache-hugegraph-store-${VERSION}"
SERVER_DIR="hugegraph-server/apache-hugegraph-server-${VERSION}"

echo "=== Stopping Server nodes ==="
for d in ${SERVER_DIR}_*; do
    if [ -d "$d" ]; then
        echo "Stopping $d"
        "$d/bin/stop-hugegraph.sh" 2>/dev/null || true
    fi
done

echo "=== Stopping Store nodes ==="
for d in ${STORE_DIR}_*; do
    if [ -d "$d" ]; then
        echo "Stopping $d"
        bash "$d/bin/stop-hugegraph-store.sh" 2>/dev/null || true
    fi
done

echo "=== Stopping PD nodes ==="
for d in ${PD_DIR}_*; do
    if [ -d "$d" ]; then
        echo "Stopping $d"
        bash "$d/bin/stop-hugegraph-pd.sh" 2>/dev/null || true
    fi
done

echo "=== Cluster stopped ==="
