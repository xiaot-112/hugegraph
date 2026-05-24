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

package org.apache.hugegraph.E2ETest;

import org.apache.hugegraph.ct.client.ClusterRestClient;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsistencyE2ETest extends BaseE2ETest {

    @Test
    public void testWriteOnOneServerReadOnAnother() throws InterruptedException {
        if (serverClients.size() < 2) {
            return;
        }

        createBasicSchema(testGraphName);
        String prefix = "graphspaces/DEFAULT/graphs/" + testGraphName;
        String vertices = prefix + "/graph/vertices";

        ClusterRestClient writer = serverClients.get(0);
        String body = "{\"label\":\"person\",\"properties\":{\"name\":\"consistency_test\",\"age\":30}}";
        Response r = writer.post(vertices, body);
        assertEquals(201, r.getStatus());
        String vertexContent = r.readEntity(String.class);
        assertTrue(vertexContent.contains("consistency_test"));
        String vertexId = extractId(vertexContent);

        Thread.sleep(3000);

        for (int i = 1; i < serverClients.size(); i++) {
            ClusterRestClient reader = serverClients.get(i);
            Response readR = reader.get(vertices + "/" + vertexId);
            assertEquals(200, readR.getStatus());
            String content = readR.readEntity(String.class);
            assertTrue("Server " + i + " should see the vertex",
                       content.contains("consistency_test"));
        }
    }

    private String extractId(String content) {
        int idx = content.indexOf("\"id\":");
        if (idx < 0) return "";
        int start = idx + 5;
        if (content.charAt(start) == '"') start++;
        int end = start;
        while (end < content.length() &&
               content.charAt(end) != ',' && content.charAt(end) != '"' &&
               content.charAt(end) != '}') {
            end++;
        }
        String rawId = content.substring(start, end);
        return "\"" + rawId + "\"";
    }
}
