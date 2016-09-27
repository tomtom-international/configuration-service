/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
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
        Node x = new Node("x");
        assertEquals("x", x.getName());
        assertNull(x.getModified());
        assertNull(x.getNodes());
        assertNull(x.getParameters());
    }

    @Test
    public void testNodeEmpty2() {
        LOG.info("testNodeEmpty2");
        Node x = new Node("x", Immutables.emptyList(), Immutables.emptyList(), null, null, null);
        assertEquals("x", x.getName());
        assertNull(x.getModified());
        assertNull(x.getNodes());
        assertNull(x.getParameters());
    }

    @Test
    public void testNode() {
        LOG.info("testNode");
        Node x = new Node("x", Immutables.listOf(new Node("y")), Immutables.listOf(new Parameter("1", "2")), null, null, null);
        assertEquals("x", x.getName());
        assertNull(x.getModified());
        assertNotNull(x.getNodes());
        assertNotNull(x.getParameters());
        assertEquals("y", x.getNodes().iterator().next().getName());
        assertEquals("1", x.getParameters().iterator().next().getKey());
        assertEquals("{\"name\":\"x\",\"nodes\":[{\"name\":\"y\"}],\"parameters\":[{\"key\":\"1\",\"value\":\"2\"}]}", x.toString());
    }
}
