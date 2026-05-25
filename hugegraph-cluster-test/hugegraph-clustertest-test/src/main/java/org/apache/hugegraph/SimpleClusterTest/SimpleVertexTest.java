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

package org.apache.hugegraph.SimpleClusterTest;

import java.util.Map;

import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleVertexTest extends BaseSimpleTest {

    @Test
    public void testCreateAndQueryVertex() {
        createBasicSchema();
        String vertices = URL_PREFIX + "/graph/vertices";

        String body = "{\"label\":\"person\",\"properties\":{\"name\":\"tom\",\"age\":25}}";
        Response r = client.post(vertices, body);
        assertEquals(201, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"tom\""));

        r = client.get(vertices, Map.of("label", "person"));
        assertEquals(200, r.getStatus());
        content = r.readEntity(String.class);
        assertTrue(content.contains("tom"));
    }

    @Test
    public void testUpdateVertexProperty() {
        createBasicSchema();
        String vertices = URL_PREFIX + "/graph/vertices";

        String body = "{\"label\":\"person\",\"properties\":{\"name\":\"alice\",\"age\":30}}";
        Response r = client.post(vertices, body);
        assertEquals(201, r.getStatus());
        String vertexId = extractId(r.readEntity(String.class));

        String updateBody = "{\"properties\":{\"age\":31}}";
        r = client.put(vertices + "/" + formatIdForUrl(vertexId), updateBody,
                        Map.of("action", "append"));
        assertEquals(200, r.getStatus());
    }

    @Test
    public void testDeleteVertex() {
        createBasicSchema();
        String vertices = URL_PREFIX + "/graph/vertices";

        String body = "{\"label\":\"person\",\"properties\":{\"name\":\"bob\",\"age\":22}}";
        Response r = client.post(vertices, body);
        assertEquals(201, r.getStatus());
        String vertexId = extractId(r.readEntity(String.class));

        r = client.delete(vertices + "/" + formatIdForUrl(vertexId));
        assertEquals(204, r.getStatus());
    }

    @Test
    public void testBatchCreateVertices() {
        createBasicSchema();
        String batchUrl = URL_PREFIX + "/graph/vertices/batch";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 50; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"label\":\"person\",\"properties\":{\"name\":\"user")
              .append(i).append("\",\"age\":").append(i % 50 + 20).append("}}");
        }
        sb.append("]");
        Response r = client.post(batchUrl, sb.toString());
        assertEquals(201, r.getStatus());
    }

    protected void createBasicSchema() {
        String pkUrl = URL_PREFIX + "/schema/propertykeys";
        client.post(pkUrl, "{\"name\":\"name\",\"data_type\":\"TEXT\"," +
                           "\"cardinality\":\"SINGLE\",\"check_exist\":false}");
        client.post(pkUrl, "{\"name\":\"age\",\"data_type\":\"INT\"," +
                           "\"cardinality\":\"SINGLE\",\"check_exist\":false}");
        client.post(pkUrl, "{\"name\":\"weight\",\"data_type\":\"DOUBLE\"," +
                           "\"cardinality\":\"SINGLE\",\"check_exist\":false}");

        String vlUrl = URL_PREFIX + "/schema/vertexlabels";
        client.post(vlUrl, "{\"name\":\"person\",\"id_strategy\":\"PRIMARY_KEY\"," +
                           "\"primary_keys\":[\"name\"],\"properties\":[\"name\",\"age\"]," +
                           "\"check_exist\":false}");
        client.post(vlUrl, "{\"name\":\"software\",\"id_strategy\":\"PRIMARY_KEY\"," +
                           "\"primary_keys\":[\"name\"],\"properties\":[\"name\",\"age\"]," +
                           "\"check_exist\":false}");

        String elUrl = URL_PREFIX + "/schema/edgelabels";
        client.post(elUrl, "{\"name\":\"knows\",\"source_label\":\"person\"," +
                           "\"target_label\":\"person\",\"properties\":[\"weight\"]," +
                           "\"check_exist\":false}");
        client.post(elUrl, "{\"name\":\"created\",\"source_label\":\"person\"," +
                           "\"target_label\":\"software\",\"properties\":[\"weight\"]," +
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
