/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.google.gson.Gson;
import com.tomtom.services.configuration.dto.NodeDTO;
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
public class ApiGetNodeTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiGetNodeTest.class);

    private final LocalTestServer server = new LocalTestServer("classpath:example.json");

    static final String HASH1 = "\"15bba634096a454170b51c072aefbab07cffd817\"";
    static final String HASH2 = "\"8b26f819393571120aac51ab767b3b666ccfd7b4\"";

    @Before
    public void startServer() throws IncorrectConfigurationException {
        server.startServer();
    }

    @After
    public void stopServer() {
        server.stopServer();
    }

    @Test
    public void checkTreeJson() {
        LOG.info("checkSimpleTreeJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String s = response.readEntity(String.class);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"TPEG\",\"nodes\":[{\"match\":\"P107\",\"nodes\":[{\"match\":\"Device[0-9]*\",\"parameters\":[{\"key\":\"radius\",\"value\":\"10\"},{\"key\":\"interval\",\"value\":\"120\"}]},{\"match\":\"Device123\",\"parameters\":[{\"key\":\"radius\",\"value\":\"80\"},{\"key\":\"interval\",\"value\":\"60\"}]}]},{\"match\":\"P508\",\"nodes\":[{\"match\":\"Device1.*\",\"parameters\":[{\"key\":\"radius\",\"value\":\"100\"}]},{\"match\":\"Device999\",\"parameters\":[{\"key\":\"radius\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"radius\",\"value\":\"40\"},{\"key\":\"interval\",\"value\":\"120\"}]}],\"parameters\":[{\"key\":\"radius\",\"value\":\"25\"},{\"key\":\"interval\",\"value\":\"120\"}],\"modified\":\"2016-01-02T12:34:56Z\"},{\"match\":\"SYS\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}],\"modified\":\"2016-01-02T12:34:00Z\",\"levels\":[\"service\",\"model\",\"deviceID\"]}",
                s);
        final NodeDTO x = new Gson().fromJson(s, NodeDTO.class);
        Assert.assertEquals(null, x.getMatch());
        Assert.assertNotNull(x.getNodes());

        final NodeDTO n = x.getNodes().get(0);
        Assert.assertNotNull(n.getNodes());
        Assert.assertNotNull(n.getParameters());
        Assert.assertEquals("TPEG", n.getMatch());
        Assert.assertEquals(2, n.getNodes().size());
        Assert.assertEquals(2, n.getParameters().size());
    }

    @Test
    public void checkUnknownNode() {
        LOG.info("checkUnknownNode");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/UNKNOWN").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void checkSubTree1() {
        LOG.info("checkSubTree1");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/SYS").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"match\":\"SYS\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSubTree2() {
        LOG.info("checkSubTree2");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/TPEG/P508").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"match\":\"P508\",\"nodes\":[{\"match\":\"Device1.*\",\"parameters\":[{\"key\":\"radius\",\"value\":\"100\"}]},{\"match\":\"Device999\",\"parameters\":[{\"key\":\"radius\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"radius\",\"value\":\"40\"},{\"key\":\"interval\",\"value\":\"120\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkNodeNotModified() {
        LOG.info("checkNodeNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/TPEG/P508/Device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:57 GMT").    // 1 sec later than config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(304, response.getStatus());
        Assert.assertEquals(HASH2, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
    }

    @Test
    public void checkNodeModified() {
        LOG.info("checkNodeModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/TPEG/P508/Device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:56 GMT").    // Same time as in config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"match\":\"Device999\",\"parameters\":[{\"key\":\"radius\",\"value\":\"200\"}]}",
                response.readEntity(String.class));
        Assert.assertEquals(HASH2, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
    }

    @Test
    public void checkModifiedFormatWrong() {
        LOG.info("checkModifiedFormatWrong");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/TPEG").
                request().
                header("If-Modified-Since", "wrong").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void checkETagEmpty() {
        LOG.info("checkETagEmpty");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/TPEG").
                request().
                header("If-None-Match", "").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
    }
}
