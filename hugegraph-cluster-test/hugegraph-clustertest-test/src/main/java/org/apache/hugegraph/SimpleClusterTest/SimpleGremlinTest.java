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

import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleGremlinTest extends BaseSimpleTest {

    @Test
    public void testGremlinVertexCount() {
        createBasicSchema();
        String vertices = URL_PREFIX + "/graph/vertices";

        client.post(vertices, "{\"label\":\"person\",\"properties\":{\"name\":\"g1\",\"age\":20}}");
        client.post(vertices, "{\"label\":\"person\",\"properties\":{\"name\":\"g2\",\"age\":21}}");

        String gremlinUrl = URL_PREFIX + "/gremlin";
        String body = "{\"gremlin\":\"g.V().hasLabel('person').count()\"}";
        Response r = client.post(gremlinUrl, body);
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("2"));
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
}
