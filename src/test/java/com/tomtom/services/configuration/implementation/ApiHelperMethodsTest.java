/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.google.gson.Gson;
import com.tomtom.services.configuration.dto.VersionDTO;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class ApiHelperMethodsTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiHelperMethodsTest.class);

    private final LocalTestServer server = new LocalTestServer("classpath:example.json");

    @Before
    public void startServer() throws IncorrectConfigurationException {
        server.startServer();
    }

    @After
    public void stopServer() {
        server.stopServer();
    }

    @Test
    public void checkHelp() {
        LOG.info("checkHelp");
        final Response r = new ResteasyClientBuilder().build().
                target(server.getHost() + '/').
                request().
                get();
        Assert.assertNotNull(r);
        final int status = r.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);
    }

    @Test
    public void checkStatusOK() {
        LOG.info("checkStatusOK");
        final Response r = new ResteasyClientBuilder().build().
                target(server.getHost() + "/status").
                request().
                get();
        Assert.assertNotNull(r);
        final int status = r.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);
    }

    @Test
    public void checkVersionWithParameters() {
        LOG.info("checkVersionWithParameters");
        final Response r = new ResteasyClientBuilder().build().
                target(server.getHost() + "/version?x=1&y=2").
                request().
                get();
        Assert.assertNotNull(r);
        final int status = r.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);
        final String s = r.readEntity(String.class);
        Assert.assertEquals("{\"version\":\"1.0.0-TEST\",\"startupConfigurationURI\":\"classpath:example.json\"}",
                s);

        final VersionDTO x = new Gson().fromJson(s, VersionDTO.class);
        Assert.assertEquals("1.0.0-TEST", x.getVersion());
        Assert.assertEquals("classpath:example.json", x.getStartupConfigurationURI());
    }

    @Test
    public void checkVersionJson() {
        LOG.info("checkVersionJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/version").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"version\":\"1.0.0-TEST\",\"startupConfigurationURI\":\"classpath:example.json\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkVersionXml() {
        LOG.info("checkVersionXml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/version").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><version><version>1.0.0-TEST</version><startupConfigurationURI>classpath:example.json</startupConfigurationURI></version>",
                response.readEntity(String.class));
    }
}
