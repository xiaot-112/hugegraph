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

public class SimpleEdgeTest extends BaseSimpleTest {

    @Test
    public void testCreateAndQueryEdge() {
        createBasicSchema();
        String vertices = URL_PREFIX + "/graph/vertices";
        String edges = URL_PREFIX + "/graph/edges";

        String v1 = "{\"label\":\"person\",\"properties\":{\"name\":\"alice\",\"age\":30}}";
        Response r1 = client.post(vertices, v1);
        assertEquals(201, r1.getStatus());
        String id1 = extractId(r1.readEntity(String.class));

        String v2 = "{\"label\":\"person\",\"properties\":{\"name\":\"bob\",\"age\":25}}";
        Response r2 = client.post(vertices, v2);
        assertEquals(201, r2.getStatus());
        String id2 = extractId(r2.readEntity(String.class));

        String edgeBody = "{\"label\":\"knows\",\"outV\":\"" + id1 +
                          "\",\"inV\":\"" + id2 +
                          "\",\"properties\":{\"weight\":0.8}}";
        Response re = client.post(edges, edgeBody);
        assertEquals(201, re.getStatus());
        String content = re.readEntity(String.class);
        assertTrue(content.contains("\"label\":\"knows\""));

        re = client.get(edges, Map.of("label", "knows"));
        assertEquals(200, re.getStatus());
        content = re.readEntity(String.class);
        assertTrue(content.contains("knows"));
    }

    @Test
    public void testDeleteEdge() {
        createBasicSchema();
        String vertices = URL_PREFIX + "/graph/vertices";
        String edges = URL_PREFIX + "/graph/edges";

        String v1 = "{\"label\":\"person\",\"properties\":{\"name\":\"eve\",\"age\":28}}";
        Response r1 = client.post(vertices, v1);
        String id1 = extractId(r1.readEntity(String.class));

        String v2 = "{\"label\":\"person\",\"properties\":{\"name\":\"carol\",\"age\":35}}";
        Response r2 = client.post(vertices, v2);
        String id2 = extractId(r2.readEntity(String.class));

        String edgeBody = "{\"label\":\"knows\",\"outV\":\"" + id1 +
                          "\",\"inV\":\"" + id2 +
                          "\",\"properties\":{\"weight\":0.5}}";
        Response re = client.post(edges, edgeBody);
        assertEquals(201, re.getStatus());
        String edgeId = extractId(re.readEntity(String.class));

        re = client.delete(edges + "/" + formatIdForUrl(edgeId),
                           Map.of("label", "knows"));
        assertEquals(204, re.getStatus());
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

        String elUrl = URL_PREFIX + "/schema/edgelabels";
        client.post(elUrl, "{\"name\":\"knows\",\"source_label\":\"person\"," +
                           "\"target_label\":\"person\",\"properties\":[\"weight\"]," +
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
