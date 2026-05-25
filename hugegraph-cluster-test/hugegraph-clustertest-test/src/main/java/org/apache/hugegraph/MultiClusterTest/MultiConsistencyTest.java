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

package org.apache.hugegraph.MultiClusterTest;

import org.apache.hugegraph.SimpleClusterTest.BaseSimpleTest.RestClient;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiConsistencyTest extends BaseMultiClusterTest {

    @Test
    public void testWriteOnOneServerReadOnAnother() throws InterruptedException {
        if (clients.size() < 2) {
            return;
        }

        RestClient writer = clients.get(0);
        createBasicSchema(writer);
        String vertices = URL_PREFIX + "/graph/vertices";

        String body = "{\"label\":\"person\",\"properties\":{\"name\":\"consistency_test\",\"age\":30}}";
        Response r = writer.post(vertices, body);
        assertEquals(201, r.getStatus());
        String vertexContent = r.readEntity(String.class);
        assertTrue(vertexContent.contains("consistency_test"));
        String vertexId = extractId(vertexContent);

        Thread.sleep(3000);

        for (int i = 1; i < clients.size(); i++) {
            RestClient reader = clients.get(i);
            Response readR = reader.get(vertices + "/" + formatIdForUrl(vertexId));
            assertEquals(200, readR.getStatus());
            String content = readR.readEntity(String.class);
            assertTrue("Server " + i + " should see the vertex",
                       content.contains("consistency_test"));
        }
    }

    protected void createBasicSchema(RestClient c) {
        String pkUrl = URL_PREFIX + "/schema/propertykeys";
        c.post(pkUrl, "{\"name\":\"name\",\"data_type\":\"TEXT\"," +
                      "\"cardinality\":\"SINGLE\",\"check_exist\":false}");
        c.post(pkUrl, "{\"name\":\"age\",\"data_type\":\"INT\"," +
                      "\"cardinality\":\"SINGLE\",\"check_exist\":false}");
        c.post(pkUrl, "{\"name\":\"weight\",\"data_type\":\"DOUBLE\"," +
                      "\"cardinality\":\"SINGLE\",\"check_exist\":false}");

        String vlUrl = URL_PREFIX + "/schema/vertexlabels";
        c.post(vlUrl, "{\"name\":\"person\",\"id_strategy\":\"PRIMARY_KEY\"," +
                      "\"primary_keys\":[\"name\"],\"properties\":[\"name\",\"age\"]," +
                      "\"check_exist\":false}");
    }

    protected static String extractId(String content) {
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
        return content.substring(start, end);
    }

    protected static String formatIdForUrl(String id) {
        return "\"" + id + "\"";
    }
}
