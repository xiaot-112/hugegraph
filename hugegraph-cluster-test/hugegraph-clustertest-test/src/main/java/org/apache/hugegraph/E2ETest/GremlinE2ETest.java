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

import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GremlinE2ETest extends BaseE2ETest {

    @Test
    public void testGremlinVertexCount() {
        createBasicSchema(testGraphName);
        String vertices = testUrlPrefix + "/graph/vertices";

        client.post(vertices, "{\"label\":\"person\",\"properties\":{\"name\":\"g1\",\"age\":20}}");
        client.post(vertices, "{\"label\":\"person\",\"properties\":{\"name\":\"g2\",\"age\":21}}");

        String gremlinUrl = testUrlPrefix + "/gremlin";
        String body = "{\"gremlin\":\"g.V().hasLabel('person').count()\"}";
        Response r = client.post(gremlinUrl, body);
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("2"));
    }

    @Test
    public void testGremlinPathTraversal() {
        createBasicSchema(testGraphName);
        String vertices = testUrlPrefix + "/graph/vertices";
        String edges = testUrlPrefix + "/graph/edges";

        Response r1 = client.post(vertices,
            "{\"label\":\"person\",\"properties\":{\"name\":\"a1\",\"age\":20}}");
        String id1 = extractId(r1.readEntity(String.class));
        Response r2 = client.post(vertices,
            "{\"label\":\"person\",\"properties\":{\"name\":\"a2\",\"age\":21}}");
        String id2 = extractId(r2.readEntity(String.class));
        Response r3 = client.post(vertices,
            "{\"label\":\"person\",\"properties\":{\"name\":\"a3\",\"age\":22}}");
        String id3 = extractId(r3.readEntity(String.class));

        client.post(edges, "{\"label\":\"knows\",\"source\":\"" + id1 +
                           "\",\"target\":\"" + id2 +
                           "\",\"properties\":{\"weight\":0.5}}");
        client.post(edges, "{\"label\":\"knows\",\"source\":\"" + id2 +
                           "\",\"target\":\"" + id3 +
                           "\",\"properties\":{\"weight\":0.6}}");

        String gremlinUrl = testUrlPrefix + "/gremlin";
        String body = "{\"gremlin\":\"g.V().has('name','a1').out('knows').out('knows').values('name')\"}";
        Response r = client.post(gremlinUrl, body);
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("a3"));
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
        return content.substring(start, end);
    }
}
