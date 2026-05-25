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

import org.apache.hugegraph.ct.base.BaseClusterTest;
import org.apache.hugegraph.ct.base.ClusterScale;
import org.apache.hugegraph.ct.env.BaseEnv;
import org.apache.hugegraph.ct.env.DynamicEnv;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import jakarta.ws.rs.core.Response;

@ClusterScale(pd = 3, store = 3, server = 3)
public class BaseE2ETest extends BaseClusterTest {

    protected static String testGraphName;
    protected static String testUrlPrefix;

    @BeforeClass
    public static void initE2E() {
        testGraphName = DEFAULT_GRAPH;
        testUrlPrefix = "graphspaces/DEFAULT/graphs/" + testGraphName;
        env = createE2EEnv();
        setupCluster();
    }

    protected static BaseEnv createE2EEnv() {
        ClusterScale scale = BaseE2ETest.class.getAnnotation(ClusterScale.class);
        if (scale != null) {
            return new DynamicEnv(scale.pd(), scale.store(), scale.server());
        }
        return new DynamicEnv(3, 3, 3);
    }

    @Before
    public void setupTestGraph() {
    }

    @After
    public void cleanupTestGraph() {
    }

    protected void createBasicSchema(String graph) {
        String prefix = "graphspaces/DEFAULT/graphs/" + graph;

        String pkUrl = prefix + "/schema/propertykeys";
        client.post(pkUrl, "{\"name\":\"name\",\"data_type\":\"TEXT\"," +
                           "\"cardinality\":\"SINGLE\",\"check_exist\":false}");
        client.post(pkUrl, "{\"name\":\"age\",\"data_type\":\"INT\"," +
                           "\"cardinality\":\"SINGLE\",\"check_exist\":false}");
        client.post(pkUrl, "{\"name\":\"weight\",\"data_type\":\"DOUBLE\"," +
                           "\"cardinality\":\"SINGLE\",\"check_exist\":false}");

        String vlUrl = prefix + "/schema/vertexlabels";
        client.post(vlUrl, "{\"name\":\"person\",\"id_strategy\":\"PRIMARY_KEY\"," +
                           "\"primary_keys\":[\"name\"],\"properties\":[\"name\",\"age\"]," +
                           "\"check_exist\":false}");
        client.post(vlUrl, "{\"name\":\"software\",\"id_strategy\":\"PRIMARY_KEY\"," +
                           "\"primary_keys\":[\"name\"],\"properties\":[\"name\",\"age\"]," +
                           "\"check_exist\":false}");

        String elUrl = prefix + "/schema/edgelabels";
        client.post(elUrl, "{\"name\":\"knows\",\"source_label\":\"person\"," +
                           "\"target_label\":\"person\",\"properties\":[\"weight\"]," +
                           "\"check_exist\":false}");
        client.post(elUrl, "{\"name\":\"created\",\"source_label\":\"person\"," +
                           "\"target_label\":\"software\",\"properties\":[\"weight\"]," +
                           "\"check_exist\":false}");
    }

    protected void createBasicSchemaWithIndex(String graph) {
        createBasicSchema(graph);
        String prefix = "graphspaces/DEFAULT/graphs/" + graph;
        String ilUrl = prefix + "/schema/indexlabels";
        client.post(ilUrl, "{\"name\":\"personByName\",\"base_type\":\"VERTEX_LABEL\"," +
                           "\"base_value\":\"person\",\"index_type\":\"SECONDARY\"," +
                           "\"fields\":[\"name\"],\"check_exist\":false}");
    }

    protected Response ensureOk(Response r, int expectedStatus) {
        if (r.getStatus() != expectedStatus) {
            throw new AssertionError("Expected " + expectedStatus +
                                     " but got " + r.getStatus() +
                                     ": " + r.readEntity(String.class));
        }
        return r;
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
