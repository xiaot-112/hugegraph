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

public class SchemaE2ETest extends BaseE2ETest {

    @Test
    public void testPropertyKeyCRUD() {
        String pks = testUrlPrefix + "/schema/propertykeys";

        String body = "{\"name\":\"pk_test\",\"data_type\":\"TEXT\"," +
                      "\"cardinality\":\"SINGLE\",\"check_exist\":false}";
        Response r = client.post(pks, body);
        assertEquals(202, r.getStatus());

        r = client.get(pks + "/pk_test");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"pk_test\""));

        r = client.get(pks);
        assertEquals(200, r.getStatus());
        content = r.readEntity(String.class);
        assertTrue(content.contains("pk_test"));
    }

    @Test
    public void testVertexLabelCRUD() {
        String pks = testUrlPrefix + "/schema/propertykeys";
        client.post(pks, "{\"name\":\"vl_name\",\"data_type\":\"TEXT\"," +
                         "\"cardinality\":\"SINGLE\",\"check_exist\":false}");

        String vls = testUrlPrefix + "/schema/vertexlabels";
        String body = "{\"name\":\"vl_test\",\"id_strategy\":\"PRIMARY_KEY\"," +
                      "\"primary_keys\":[\"vl_name\"],\"properties\":[\"vl_name\"]," +
                      "\"check_exist\":false}";
        Response r = client.post(vls, body);
        assertEquals(202, r.getStatus());

        r = client.get(vls + "/vl_test");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"vl_test\""));
    }

    @Test
    public void testEdgeLabelCRUD() {
        createBasicSchema(testGraphName);

        String els = testUrlPrefix + "/schema/edgelabels";
        String body = "{\"name\":\"el_test\",\"source_label\":\"person\"," +
                      "\"target_label\":\"person\",\"properties\":[\"weight\"]," +
                      "\"check_exist\":false}";
        Response r = client.post(els, body);
        assertEquals(202, r.getStatus());

        r = client.get(els + "/el_test");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"el_test\""));
    }

    @Test
    public void testIndexLabelCRUD() {
        createBasicSchemaWithIndex(testGraphName);

        String ils = testUrlPrefix + "/schema/indexlabels";
        String body = "{\"name\":\"ageIdx\",\"base_type\":\"VERTEX_LABEL\"," +
                      "\"base_value\":\"person\",\"index_fields\":[\"age\"]," +
                      "\"check_exist\":false}";
        Response r = client.post(ils, body);
        assertEquals(202, r.getStatus());

        r = client.get(ils + "/ageIdx");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"ageIdx\""));
    }
}
