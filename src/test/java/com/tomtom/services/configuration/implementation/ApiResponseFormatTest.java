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
public class ApiResponseFormatTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiResponseFormatTest.class);

    @Test
    public void checkEmptyTreeJson() throws Exception {
        LOG.info("checkEmptyTreeJson");
        final LocalTestServer server = new LocalTestServer("classpath:empty.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{}", response.readEntity(String.class));
        server.stopServer();
    }

    @Test
    public void checkEmptyTreeXml() throws Exception {
        LOG.info("checkSimpleTreeXml");
        final LocalTestServer server = new LocalTestServer("classpath:empty.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><node/>",
                response.readEntity(String.class));
        server.stopServer();

    }

    @Test
    public void checkSimpleTreeJson() throws Exception {
        LOG.info("checkSimpleTreeJson");
        final LocalTestServer server = new LocalTestServer("classpath:simple1.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"nodes\":[{\"name\":\"child-1\",\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}],\"modified\":\"2016-01-02T11:11:11Z\"},{\"name\":\"child-2\",\"parameters\":[{\"key\":\"key-2\",\"value\":\"value-2\"}]}],\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"modified\":\"2016-01-02T00:00:00Z\",\"levels\":[\"criterium\"]}",
                response.readEntity(String.class));
        server.stopServer();

    }

    @Test
    public void checkSimpleTreeXml() throws Exception {
        LOG.info("checkSimpleTreeXml");
        final LocalTestServer server = new LocalTestServer("classpath:simple1.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><node><nodes><node><name>child-1</name><parameters><parameter><key>key-1a</key><value>value-1a</value></parameter><parameter><key>key-1b</key><value>value-1b</value></parameter></parameters><modified>2016-01-02T11:11:11Z</modified></node><node><name>child-2</name><parameters><parameter><key>key-2</key><value>value-2</value></parameter></parameters></node></nodes><parameters><parameter><key>key-0</key><value>value-0</value></parameter></parameters><modified>2016-01-02T00:00:00Z</modified><levels><level>criterium</level></levels></node>",
                response.readEntity(String.class));
        server.stopServer();

    }

    @Test
    public void checkSearchInSImpleTreeJson() throws Exception {
        LOG.info("checkSearchInSImpleTreeJson");
        final LocalTestServer server = new LocalTestServer("classpath:simple1.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}],\"matched\":\"/child-1\"}",
                response.readEntity(String.class));
        server.stopServer();

    }


    @Test
    public void checkSearchInSimpleTreeXml() throws Exception {
        LOG.info("checkSearchInSimpleTreeXml");
        final LocalTestServer server = new LocalTestServer("classpath:simple1.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><searchResult><parameters><parameter><key>key-1a</key><value>value-1a</value></parameter><parameter><key>key-1b</key><value>value-1b</value></parameter></parameters><matched>/child-1</matched></searchResult>",
                response.readEntity(String.class));
        server.stopServer();

    }

    @Test
    public void checkMultiSearchJson() throws Exception {
        LOG.info("checkMultiSearchJson");
        final LocalTestServer server = new LocalTestServer("classpath:simple1.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1;unknown").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}],\"matched\":\"/child-1\"},{\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"matched\":\"/\"}]",
                response.readEntity(String.class));
        server.stopServer();
    }


    @Test
    public void checkMultiSearchXml() throws Exception {
        LOG.info("checkMultiSearchXml");
        final LocalTestServer server = new LocalTestServer("classpath:simple1.json");
        server.startServer();
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1;unknown").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><searchResults><searchResult><parameters><parameter><key>key-1a</key><value>value-1a</value></parameter><parameter><key>key-1b</key><value>value-1b</value></parameter></parameters><matched>/child-1</matched></searchResult><searchResult><parameters><parameter><key>key-0</key><value>value-0</value></parameter></parameters><matched>/</matched></searchResult></searchResults>",
                response.readEntity(String.class));
        server.stopServer();
    }
}
