/*
 * Copyright (C) 2012-2021, TomTom (http://tomtom.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
