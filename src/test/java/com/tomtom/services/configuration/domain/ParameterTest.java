/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.domain;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ParameterTest {
    private static final Logger LOG = LoggerFactory.getLogger(ParameterTest.class);

    @Test
    public void testParameter() {
        LOG.info("testParameter");
        final Parameter x = new Parameter("x", "y");
        assertEquals("x", x.getKey());
        assertEquals("y", x.getValue());
        assertEquals("{\"key\":\"x\",\"value\":\"y\"}", x.toString());
    }
}
