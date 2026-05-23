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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.hugegraph.ct.base.ClusterConstant;

public class PortHealthChecker implements HealthChecker {

    private final String host;
    private final int port;
    private final long pollIntervalMs;

    public PortHealthChecker(String host, int port) {
        this(host, port, ClusterConstant.HEALTH_POLL_INTERVAL_MS);
    }

    public PortHealthChecker(String host, int port, long pollIntervalMs) {
        this.host = host;
        this.port = port;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Override
    public boolean isReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(this.host, this.port), 1000);
                return true;
            } catch (IOException ignored) {
                // port not ready yet
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
