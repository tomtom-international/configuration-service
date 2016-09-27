/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.dto;

import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterDTOTest {
    private static final Logger LOG = LoggerFactory.getLogger(ParameterDTOTest.class);

    @Test
    public void testEquals() {
        final ParameterDTO x1 = new ParameterDTO("x", "1");
        final ParameterDTO x2 = new ParameterDTO("x", "1");
        final ParameterDTO x3 = new ParameterDTO("x", "2");
        final ParameterDTO x4 = new ParameterDTO("y", "1");
        Assert.assertTrue(x1.equals(x2));
        Assert.assertTrue(x2.equals(x1));
        Assert.assertFalse(x1.equals(x3));
        Assert.assertFalse(x3.equals(x1));
        Assert.assertFalse(x1.equals(x4));
        Assert.assertFalse(x4.equals(x1));

        Assert.assertTrue(x1.hashCode() == x2.hashCode());
        Assert.assertTrue(x2.hashCode() == x1.hashCode());
        Assert.assertFalse(x1.hashCode() == x3.hashCode());
        Assert.assertFalse(x3.hashCode() == x1.hashCode());
        Assert.assertFalse(x1.hashCode() == x4.hashCode());
        Assert.assertFalse(x4.hashCode() == x1.hashCode());
    }
}

