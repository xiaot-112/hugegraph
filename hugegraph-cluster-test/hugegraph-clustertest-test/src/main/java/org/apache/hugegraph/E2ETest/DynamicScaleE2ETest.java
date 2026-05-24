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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamicScaleE2ETest extends BaseE2ETest {

    @Test
    public void testAddPDNodeAndVerify() {
        int originalCount = env.getAlivePDNodeCount();
        int newIndex = env.addPDNode();
        assertEquals(originalCount + 1, env.getAlivePDNodeCount());
        assertTrue(newIndex >= 0);
    }

    @Test
    public void testAddStoreNodeAndVerify() {
        int originalCount = env.getAliveStoreNodeCount();
        int newIndex = env.addStoreNode();
        assertEquals(originalCount + 1, env.getAliveStoreNodeCount());
        assertTrue(newIndex >= 0);
    }

    @Test
    public void testAddServerNodeAndVerify() {
        int originalCount = env.getAliveServerNodeCount();
        int newIndex = env.addServerNode();
        assertEquals(originalCount + 1, env.getAliveServerNodeCount());
        assertTrue(newIndex >= 0);
    }

    @Test
    public void testRemoveServerNodeAndVerify() {
        int originalCount = env.getAliveServerNodeCount();
        if (originalCount <= 1) {
            return;
        }
        env.removeServerNode(originalCount - 1);
        assertEquals(originalCount - 1, env.getAliveServerNodeCount());
    }
}
