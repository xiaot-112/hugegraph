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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiDynamicScaleTest extends BaseMultiClusterTest {

    @Test
    public void testAddAndRemoveServerNode() {
        int originalCount = env.getAliveServerNodeCount();
        assertTrue("Need at least 1 server to test", originalCount >= 1);

        int newIndex = env.addServerNode();
        assertEquals(originalCount + 1, env.getAliveServerNodeCount());

        env.removeServerNode(newIndex);
        assertEquals(originalCount, env.getAliveServerNodeCount());
    }

    @Test
    public void testAddAndRemovePDNode() {
        int originalCount = env.getAlivePDNodeCount();
        assertTrue("Need at least 1 PD to test", originalCount >= 1);

        int newIndex = env.addPDNode();
        assertEquals(originalCount + 1, env.getAlivePDNodeCount());

        env.removePDNode(newIndex);
        assertEquals(originalCount, env.getAlivePDNodeCount());
    }

    @Test
    public void testAddAndRemoveStoreNode() {
        int originalCount = env.getAliveStoreNodeCount();
        assertTrue("Need at least 1 store to test", originalCount >= 1);

        int newIndex = env.addStoreNode();
        assertEquals(originalCount + 1, env.getAliveStoreNodeCount());

        env.removeStoreNode(newIndex);
        assertEquals(originalCount, env.getAliveStoreNodeCount());
    }
}
