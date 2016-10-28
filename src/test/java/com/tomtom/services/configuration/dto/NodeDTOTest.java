/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.dto;

import com.tomtom.speedtools.objects.Immutables;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDTOTest {
    private static final Logger LOG = LoggerFactory.getLogger(NodeDTOTest.class);

    @Test
    public void testEquals() {
        final NodeDTO x1 = new NodeDTO("x",
                null,
                new ParameterListDTO(Immutables.listOf(new ParameterDTO("k1", "v1"), new ParameterDTO("k2", "v2"))),
                "modified",
                Immutables.listOf("a", "b", "c"),
                "include",
                "includeArray");
        final NodeDTO x2 = new NodeDTO("x",
                null,
                new ParameterListDTO(Immutables.listOf(new ParameterDTO("k1", "v1"), new ParameterDTO("k2", "v2"))),
                "modified",
                Immutables.listOf("a", "b", "c"),
                "include",
                "includeArray");
        final NodeDTO x3 = new NodeDTO("x",
                null,
                new ParameterListDTO(Immutables.listOf(new ParameterDTO("k1", "v1"), new ParameterDTO("k2", "vX"))),
                "modified",
                Immutables.listOf("a", "b", "c"),
                "include",
                "includeArray");
        Assert.assertTrue(x1.equals(x2));
        Assert.assertTrue(x2.equals(x1));
        Assert.assertFalse(x1.equals(x3));
        Assert.assertFalse(x3.equals(x1));

        Assert.assertTrue(x1.hashCode() == x2.hashCode());
        Assert.assertTrue(x2.hashCode() == x1.hashCode());
        Assert.assertFalse(x1.hashCode() == x3.hashCode());
        Assert.assertFalse(x3.hashCode() == x1.hashCode());
    }
}
