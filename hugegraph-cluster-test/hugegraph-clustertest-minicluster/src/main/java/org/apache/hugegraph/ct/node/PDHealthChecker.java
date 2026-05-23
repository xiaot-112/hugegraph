/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.ct.node;

import org.apache.hugegraph.ct.base.ClusterConstant;
import org.apache.hugegraph.pd.client.PDClient;
import org.apache.hugegraph.pd.client.PDConfig;

public class PDHealthChecker implements HealthChecker {

    private final PDClient pdClient;
    private final long pollIntervalMs;

    public PDHealthChecker(String grpcAddr) {
        this(grpcAddr, ClusterConstant.HEALTH_POLL_INTERVAL_MS);
    }

    public PDHealthChecker(String grpcAddr, long pollIntervalMs) {
        PDConfig config = PDConfig.of(grpcAddr);
        this.pdClient = PDClient.create(config);
        this.pollIntervalMs = pollIntervalMs;
    }

    @Override
    public boolean isReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                this.pdClient.dbCompaction();
                return true;
            } catch (Exception ignored) {
                // PD not ready yet
            }
            try {
                Thread.sleep(this.pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
