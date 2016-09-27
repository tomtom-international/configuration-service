/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings({"JUnitTestMethodWithNoAssertions", "OverlyBroadThrowsClause", "ProhibitedExceptionDeclared"})
public class RegexConfigTest {
    private static final Logger LOG = LoggerFactory.getLogger(RegexConfigTest.class);

    @Test
    public void checkRegexConfigJson() throws Exception {
        LOG.info("checkRegexConfigJson");
        final LocalTestServer server = new LocalTestServer("classpath:regex-config.json");
        server.startServer();

        Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=XY12345678/nl/wifi/17.0").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        String s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for specific device with MUID XY12345678"));

        // Search for specific MUID.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=XY12345678/uk/bt/16.4").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for specific device with MUID XY12345678"));

        // Search with too few items specified; should return 404.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(404, response.getStatus());

        // Search with enough items, but nothing is found; provides fallback.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid/country/connect/navkit").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("general fallback"));

        // Search for config in UK.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid/UK/connect/navkit").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for UK"));

        // Search for config in UK, even though BT and 16.4 is specified.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid/UK/BT/16.4").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for UK"));

        // Same, but now with lowercase 'uk'.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid/uk/BT/16.4").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for UK"));

        // Search for config of BT.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid/NL/BT/16.4").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for bluetooth"));

        // Search for config of 16.4.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid/x/x/16.4").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for 16.4"));

        // Search for config of variant of 16.4.
        response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=nomuid/x/x/16.4.100").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        LOG.info("response={}", s);
        Assert.assertTrue(s.contains("for 16.4"));

        server.stopServer();
    }
}
