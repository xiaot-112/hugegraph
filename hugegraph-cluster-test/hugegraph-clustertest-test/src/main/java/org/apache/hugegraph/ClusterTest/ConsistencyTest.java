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

package org.apache.hugegraph.ClusterTest;

import org.apache.hugegraph.ct.client.ClusterRestClient;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsistencyTest extends BaseClusterTest {

    @Test
    public void testWriteOnOneServerReadOnAnother() throws InterruptedException {
        if (clients.size() < 2) {
            return;
        }

        ClusterRestClient writer = clients.get(0);
        createBasicSchema(writer);

        String body = "{\"label\":\"person\",\"properties\":{\"name\":\"consistency_test\",\"age\":30}}";
        Response r = writer.createVertex(body);
        assertEquals(201, r.getStatus());
        String vertexContent = r.readEntity(String.class);
        assertTrue(vertexContent.contains("consistency_test"));
        String vertexId = extractId(vertexContent);

        Thread.sleep(3000);

        for (int i = 1; i < clients.size(); i++) {
            ClusterRestClient reader = clients.get(i);
            Response readR = reader.get("/graph/vertices/" + formatIdForUrl(vertexId));
            assertEquals(200, readR.getStatus());
            String content = readR.readEntity(String.class);
            assertTrue("Server " + i + " should see the vertex",
                       content.contains("consistency_test"));
        }
    }
}
