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

package com.tomtom.services.configuration.implementation;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.*;

@SuppressWarnings({"JUnitTestMethodWithNoAssertions", "OverlyBroadThrowsClause", "ProhibitedExceptionDeclared"})
public class RegexConfigTest {
    private static final Logger LOG = LoggerFactory.getLogger(RegexConfigTest.class);

    @Test
    public void checkRegexConfigJson() throws Exception {
        LOG.info("checkRegexConfigJson");
        final LocalTestServer server = new LocalTestServer("classpath:regex-config.json");
        server.startServer();

        Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=XY12345678&country=nl&connection=wifi&version=17.0").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        String s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for specific device with MUID XY12345678"));

        // Search for specific MUID.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=XY12345678&country=uk&connection=bt&version=16.4").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for specific device with MUID XY12345678"));

        // Search with too few items specified; should return 400.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=nomuid").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("general fallback"));

        // Search with enough items, but nothing is found; provides fallback.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=nomuid&country=country&connection=connect&version=version").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("general fallback"));

        // Search for config in UK.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=nomuid&country=UK&connection=connect&version=version").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for UK"));

        // Search for config in UK, even though BT and 16.4 is specified.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=nomuid&country=UK&connection=BT&navkit16.4").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for UK"));

        // Same, but now with lowercase 'uk'.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=nomuid&country=uk&connection=BT&navkit16.4").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for UK"));

        // Search for config of BT.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=nomuid&country=NL&connection=BT&navkit16.4").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for bluetooth"));

        // Search for config of 16.4.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=nomuid&country=x&connection=x&version=16.4").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for 16.4"));

        // Search for config of variant of 16.4.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=device=nomuid&country=x&connection=x&version=16.4.100").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        assertTrue(s.contains("for 16.4"));

        server.stopServer();
    }
}
