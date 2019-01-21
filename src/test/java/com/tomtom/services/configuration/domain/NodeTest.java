/**
 * Copyright (C) 2019. TomTom NV (http://www.tomtom.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.services.configuration.domain;

import com.tomtom.speedtools.objects.Immutables;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class NodeTest {
    private static final Logger LOG = LoggerFactory.getLogger(NodeTest.class);

    @Test
    public void testNodeEmpty1() {
        LOG.info("testNodeEmpty1");
        final Node x = new Node("x");
        assertEquals("x", x.getMatch());
        assertNull(x.getModified());
        assertNull(x.getNodes());
        assertNull(x.getParameters());
    }

    @Test
    public void testNodeEmpty2() {
        LOG.info("testNodeEmpty2");
        final Node x = new Node("x", Immutables.emptyList(), Immutables.emptyList(), null, null, null);
        assertEquals("x", x.getMatch());
        assertNull(x.getModified());
        assertNull(x.getNodes());
        assertNull(x.getParameters());
    }

    @Test
    public void testNode() {
        LOG.info("testNode");
        final Node x = new Node("x", Immutables.listOf(new Node("y")), Immutables.listOf(new Parameter("1", "2")), null, null, null);
        assertEquals("x", x.getMatch());
        assertNull(x.getModified());
        assertNotNull(x.getNodes());
        assertNotNull(x.getParameters());
        assertEquals("y", x.getNodes().iterator().next().getMatch());
        assertEquals("1", x.getParameters().iterator().next().getKey());
        assertEquals("{\"match\":\"x\",\"nodes\":[{\"match\":\"y\"}],\"parameters\":[{\"key\":\"1\",\"value\":\"2\"}]}", x.toString());
    }
}
