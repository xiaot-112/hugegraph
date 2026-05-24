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

package org.apache.hugegraph.ct.base;

import java.util.ArrayList;
import java.util.List;

import org.apache.hugegraph.ct.client.ClusterRestClient;
import org.apache.hugegraph.ct.env.BaseEnv;
import org.apache.hugegraph.serializer.direct.util.HugeException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import jakarta.ws.rs.core.Response;

public abstract class BaseClusterTest {

    protected static final String BASE_URL = "http://";
    protected static final String DEFAULT_GRAPH = "hugegraphapi";
    protected static final String USERNAME = "admin";

    protected static BaseEnv env;
    protected static ClusterRestClient client;
    protected static List<ClusterRestClient> serverClients = new ArrayList<>();
    protected static String graphName = DEFAULT_GRAPH;
    protected static String urlPrefix;

    protected static void setupCluster() {
        if (env == null) {
            env = createEnv();
        }
        env.startCluster();
        initClients();
        initGraph(graphName);
    }

    @AfterClass
    public static void destroyClusterTest() {
        if (client != null) {
            client.close();
        }
        for (ClusterRestClient c : serverClients) {
            c.close();
        }
        serverClients.clear();
        if (env != null) {
            env.stopCluster();
        }
    }

    protected static BaseEnv createEnv() {
        return null;
    }

    protected static void initClients() {
        List<String> addrs = env.getServerRestAddrs();
        serverClients.clear();
        for (String addr : addrs) {
            serverClients.add(new ClusterRestClient(BASE_URL + addr));
        }
        client = serverClients.get(0);
        urlPrefix = "graphspaces/DEFAULT/graphs/" + graphName;
    }

    protected static void initGraph(String graph) {
        String prefix = "graphspaces/DEFAULT/graphs/" + graph;
        Response r = client.get(prefix);
        if (r.getStatus() != 200) {
            String body = "{\n" +
                          "  \"backend\": \"hstore\",\n" +
                          "  \"serializer\": \"binary\",\n" +
                          "  \"store\": \"" + graph + "\",\n" +
                          "  \"search.text_analyzer\": \"jieba\",\n" +
                          "  \"search.text_analyzer_mode\": \"INDEX\"\n" +
                          "}";
            r = client.post(prefix, body);
            if (r.getStatus() != 201) {
                throw new HugeException("Failed to create graph: " + graph +
                                        r.readEntity(String.class));
            }
        }
    }

    protected static void dropGraph(String graph) {
        String prefix = "graphspaces/DEFAULT/graphs/" + graph;
        Response r = client.get(prefix);
        if (r.getStatus() == 200) {
            client.delete(prefix);
        }
    }

    protected static String assertResponseStatus(int expected, Response response) {
        String content = response.readEntity(String.class);
        String message = String.format("Response with status %s and content %s",
                                       response.getStatus(), content);
        Assert.assertEquals(message, expected, response.getStatus());
        return content;
    }

    public static Response createAndAssert(String path, String body, int status) {
        Response r = client.post(path, body);
        assertResponseStatus(status, r);
        return r;
    }
}
