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

package org.apache.hugegraph.ct.client;

import java.util.Map;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.message.GZipEncoder;

import com.google.common.collect.Multimap;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class ClusterRestClient implements AutoCloseable {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "pa";

    private final Client client;
    private final WebTarget target;

    public ClusterRestClient(String baseUrl) {
        this(baseUrl, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    public ClusterRestClient(String baseUrl, String username, String password) {
        this.client = ClientBuilder.newClient();
        this.client.register(EncodingFilter.class);
        this.client.register(GZipEncoder.class);
        this.client.register(HttpAuthenticationFeature.basic(username, password));
        this.target = this.client.target(baseUrl);
    }

    public WebTarget target() {
        return this.target;
    }

    public WebTarget target(String url) {
        return this.client.target(url);
    }

    public Response get(String path) {
        return this.target.path(path).request().get();
    }

    public Response get(String path, String id) {
        return this.target.path(path).path(id).request().get();
    }

    public Response get(String path, MultivaluedMap<String, Object> headers) {
        return this.target.path(path).request().headers(headers).get();
    }

    public Response get(String path, Multimap<String, Object> params) {
        WebTarget t = this.target.path(path);
        for (Map.Entry<String, Object> entry : params.entries()) {
            t = t.queryParam(entry.getKey(), entry.getValue());
        }
        return t.request().get();
    }

    public Response get(String path, Map<String, Object> params) {
        WebTarget t = this.target.path(path);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            t = t.queryParam(entry.getKey(), entry.getValue());
        }
        return t.request().get();
    }

    public Response post(String path, String content) {
        return this.post(path, Entity.json(content));
    }

    public Response post(String path, Entity<?> entity) {
        return this.target.path(path).request().post(entity);
    }

    public Response put(String path, String content) {
        return this.target.path(path).request().put(Entity.json(content));
    }

    public Response put(String path, String content, Map<String, Object> queryParams) {
        WebTarget t = this.target.path(path);
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            t = t.queryParam(entry.getKey(), entry.getValue());
        }
        return t.request().put(Entity.json(content));
    }

    public Response delete(String path) {
        return this.target.path(path).request().delete();
    }

    @Override
    public void close() {
        this.client.close();
    }
}
