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

public class SimpleSchemaTest extends BaseSimpleTest {

    @Test
    public void testPropertyKeyCRUD() {
        String pks = URL_PREFIX + "/schema/propertykeys";

        String body = "{\"name\":\"pk_test\",\"data_type\":\"TEXT\"," +
                      "\"cardinality\":\"SINGLE\",\"check_exist\":false}";
        Response r = client.post(pks, body);
        assertTrue("Expected 2xx, got " + r.getStatus(),
                   r.getStatus() >= 200 && r.getStatus() < 300);

        r = client.get(pks + "/pk_test");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"pk_test\""));
    }

    @Test
    public void testVertexLabelCRUD() {
        String pks = URL_PREFIX + "/schema/propertykeys";
        client.post(pks, "{\"name\":\"vl_name\",\"data_type\":\"TEXT\"," +
                         "\"cardinality\":\"SINGLE\",\"check_exist\":false}");

        String vls = URL_PREFIX + "/schema/vertexlabels";
        String body = "{\"name\":\"vl_test\",\"id_strategy\":\"PRIMARY_KEY\"," +
                      "\"primary_keys\":[\"vl_name\"],\"properties\":[\"vl_name\"]," +
                      "\"check_exist\":false}";
        Response r = client.post(vls, body);
        assertTrue("Expected 2xx, got " + r.getStatus(),
                   r.getStatus() >= 200 && r.getStatus() < 300);

        r = client.get(vls + "/vl_test");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"vl_test\""));
    }

    @Test
    public void testEdgeLabelCRUD() {
        createBasicSchema();

        String els = URL_PREFIX + "/schema/edgelabels";
        String body = "{\"name\":\"el_test\",\"source_label\":\"person\"," +
                      "\"target_label\":\"person\",\"properties\":[\"weight\"]," +
                      "\"check_exist\":false}";
        Response r = client.post(els, body);
        assertTrue("Expected 2xx, got " + r.getStatus(),
                   r.getStatus() >= 200 && r.getStatus() < 300);

        r = client.get(els + "/el_test");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"el_test\""));
    }

    @Test
    public void testIndexLabelCRUD() {
        createBasicSchema();

        String ils = URL_PREFIX + "/schema/indexlabels";
        String body = "{\"name\":\"ageIdx\",\"base_type\":\"VERTEX_LABEL\"," +
                      "\"base_value\":\"person\",\"index_type\":\"RANGE\"," +
                      "\"fields\":[\"age\"],\"check_exist\":false}";
        Response r = client.post(ils, body);
        assertTrue("Expected 2xx, got " + r.getStatus() + ": " +
                   (r.getStatus() >= 300 ? r.readEntity(String.class) : ""),
                   r.getStatus() >= 200 && r.getStatus() < 300);

        r = client.get(ils + "/ageIdx");
        assertEquals(200, r.getStatus());
        String content = r.readEntity(String.class);
        assertTrue(content.contains("\"name\":\"ageIdx\""));
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
}
