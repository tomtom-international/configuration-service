/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JUnitTestMethodWithNoAssertions", "OverlyBroadThrowsClause", "ProhibitedExceptionDeclared"})
public class ApiResponseFormatTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiResponseFormatTest.class);

    private LocalTestServer server = null;

    public void startServer(@Nonnull final String config) throws IncorrectConfigurationException {
        server = new LocalTestServer("classpath:" + config);
        server.startServer();
    }

    @After
    public void stopServer() {
        server.stopServer();
    }

    @Test
    public void checkEmptyTreeJson() throws Exception {
        LOG.info("checkEmptyTreeJson");
        startServer("empty.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{}", response.readEntity(String.class));
    }

    @Test
    public void checkEmptyTreeXml() throws Exception {
        LOG.info("checkSimpleTreeXml");
        startServer("empty.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(APPLICATION_XML_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><node/>",
                response.readEntity(String.class));

    }

    @Test
    public void checkSimpleTreeJson() throws Exception {
        LOG.info("checkSimpleTreeJson");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"nodes\":[{\"name\":\"child-1\",\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}],\"modified\":\"2016-01-02T11:11:11Z\"},{\"name\":\"child-2\",\"parameters\":[{\"key\":\"key-2\",\"value\":\"value-2\"}]}],\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"modified\":\"2016-01-02T00:00:00Z\",\"levels\":[\"criterium\"]}",
                response.readEntity(String.class));

    }

    @Test
    public void checkSimpleTreeXml() throws Exception {
        LOG.info("checkSimpleTreeXml");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree").
                request().
                accept(APPLICATION_XML_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><node><nodes><node><name>child-1</name><parameters><parameter><key>key-1a</key><value>value-1a</value></parameter><parameter><key>key-1b</key><value>value-1b</value></parameter></parameters><modified>2016-01-02T11:11:11Z</modified></node><node><name>child-2</name><parameters><parameter><key>key-2</key><value>value-2</value></parameter></parameters></node></nodes><parameters><parameter><key>key-0</key><value>value-0</value></parameter></parameters><modified>2016-01-02T00:00:00Z</modified><levels><level>criterium</level></levels></node>",
                response.readEntity(String.class));

    }

    @Test
    public void checkSearchInSImpleTreeJson() throws Exception {
        LOG.info("checkSearchInSImpleTreeJson");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}],\"matched\":\"/child-1\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearchInSimpleTreeXml() throws Exception {
        LOG.info("checkSearchInSimpleTreeXml");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1").
                request().
                accept(APPLICATION_XML_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><searchResult><parameters><parameter><key>key-1a</key><value>value-1a</value></parameter><parameter><key>key-1b</key><value>value-1b</value></parameter></parameters><matched>/child-1</matched></searchResult>",
                response.readEntity(String.class));

    }

    @Test
    public void checkMultiSearchWrongSeparatorJson() throws Exception {
        LOG.info("checkMultiSearchWrongSeparatorJson");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1;unknown").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void checkMultiSearchJson() throws Exception {
        LOG.info("checkMultiSearchJson");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1,unknown").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("[{\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}],\"matched\":\"/child-1\"},{\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"matched\":\"/\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkMultiSearchWrongSeparatorXml() throws Exception {
        LOG.info("checkMultiSearchWrongSeparatorXml");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1;unknown").
                request().
                accept(APPLICATION_XML_TYPE).get();
        assertNotNull(response);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void checkMultiSearchXml() throws Exception {
        LOG.info("checkMultiSearchXml");
        startServer("simple1.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=child-1,unknown").
                request().
                accept(APPLICATION_XML_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><searchResults><searchResult><parameters><parameter><key>key-1a</key><value>value-1a</value></parameter><parameter><key>key-1b</key><value>value-1b</value></parameter></parameters><matched>/child-1</matched></searchResult><searchResult><parameters><parameter><key>key-0</key><value>value-0</value></parameter></parameters><matched>/</matched></searchResult></searchResults>",
                response.readEntity(String.class));
    }

    @Test
    public void checkRegexSearchWithEmptyTerm1() throws Exception {
        LOG.info("checkRegexSearchWithEmptyTerm1");
        startServer("regex.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=criterium&search=").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"parameters\":[{\"key\":\"value\",\"value\":\"4\"}],\"matched\":\"/.*\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkRegexSearchWithEmptyTerm2() throws Exception {
        LOG.info("checkRegexSearchWithEmptyTerm2");
        startServer("regex-config.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=///").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"general fallback\"},{\"key\":\"interval\",\"value\":\"general fallback\"}],\"matched\":\"/.*/.*/.*/.*\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkRegexSearchWithEmptyTerm3() throws Exception {
        LOG.info("checkRegexSearchWithEmptyTerm3");
        startServer("regex-config.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=x/y//").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"general fallback\"},{\"key\":\"interval\",\"value\":\"general fallback\"}],\"matched\":\"/.*/.*/.*/.*\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkRegexSearchWithEmptyTerm4() throws Exception {
        LOG.info("checkRegexSearchWithEmptyTerm4");
        startServer("regex-config.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=x/y//z").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"general fallback\"},{\"key\":\"interval\",\"value\":\"general fallback\"}],\"matched\":\"/.*/.*/.*/.*\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkRegexSearchWithEmptyTerm5() throws Exception {
        LOG.info("checkRegexSearchWithEmptyTerm5");
        startServer("regex-config.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=XY12345678///").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"radius for specific device with MUID XY12345678\"},{\"key\":\"interval\",\"value\":\"radius for specific device with XY12345678\"}],\"matched\":\"/XY12345678\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkRegexSearchWithTooManyTerms() throws Exception {
        LOG.info("checkRegexSearchWithTooManyTerms");
        startServer("regex-config.json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?levels=deviceID/country/connection/navkit&search=XY12345678////").
                request().
                accept(APPLICATION_JSON_TYPE).get();
        assertNotNull(response);
        assertEquals(400, response.getStatus());
    }
}
