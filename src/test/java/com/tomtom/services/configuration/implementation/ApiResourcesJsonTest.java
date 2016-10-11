/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.google.gson.Gson;
import com.tomtom.services.configuration.dto.NodeDTO;
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
public class ApiResourcesJsonTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiResourcesJsonTest.class);

    private final LocalTestServer server = new LocalTestServer("classpath:example.json");

    static final String HASH1 = "\"db6d848405d2184f5137757dfe38e41c22ef66af\"";
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
    public void checkSearch() {
        LOG.info("checkSearch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p107&deviceID=Device123").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"80\"},{\"key\":\"interval\",\"value\":\"60\"}],\"matched\":\"TPEG/P107/Device123\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearch2() {
        LOG.info("checkSearch2");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=TPEG&model=P107&deviceID=Device123").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"80\"},{\"key\":\"interval\",\"value\":\"60\"}],\"matched\":\"TPEG/P107/Device123\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearch3() {
        LOG.info("checkSearch3");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=TPEG&model=P107&deviceID=").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"25\"},{\"key\":\"interval\",\"value\":\"120\"}],\"matched\":\"TPEG\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkDifferentLevelsOrder() {
        LOG.info("checkDifferentLevelsOrder");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?deviceID=Device123&service=tpeg&model=p107").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"80\"},{\"key\":\"interval\",\"value\":\"60\"}],\"matched\":\"TPEG/P107/Device123\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkMultiSearch() {
        LOG.info("checkMultiSearch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg,sys&model=p107&deviceID=123").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"parameters\":[{\"key\":\"radius\",\"value\":\"80\"},{\"key\":\"interval\",\"value\":\"60\"}],\"matched\":\"TPEG/P107/Device123\"},{\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"matched\":\"SYS\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearchLevelDoesNotExist() {
        LOG.info("checkSearchLevelDoesNotExist");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p107&deviceID=Device123&unknown=456").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
        // TODO check if this is OK
    }

    @Test
    public void checkTooFewLevels() {
        LOG.info("checkTooFewLevels");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=P508").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"40\"},{\"key\":\"interval\",\"value\":\"120\"}],\"matched\":\"TPEG/P508\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearchSearchForEmptyLastLevel() {
        LOG.info("checkSearchSearchForEmptyLastLevel");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"40\"},{\"key\":\"interval\",\"value\":\"120\"}],\"matched\":\"TPEG/P508\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAllLevelsProvided() {
        LOG.info("checkAllLevelsProvided");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"200\"}],\"matched\":\"TPEG/P508/Device999\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkEmptySearch() {
        LOG.info("checkEmptySearch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void checkUsingCorrectSeparator() {
        LOG.info("checkUsingCorrectSeparator");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg,tpeg,tpeg&model=,p508&device=,,Device999").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("xxxx",
                response.readEntity(String.class));
    }

    @Test
    public void checkUsingWrongSeparator() {
        LOG.info("checkUsingWrongSeparator");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999;unknown").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void checkNotModified() {
        LOG.info("checkNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg,model=p508&deviceID=Device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:57 GMT").    // 1 sec later than config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(304, response.getStatus());
    }

    @Test
    public void checkModified() {
        LOG.info("checkModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:56 GMT").    // Same time as in config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"200\"}],\"matched\":\"TPEG/P508/Device999\"}",
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
        Assert.assertEquals(HASH2, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(304, response.getStatus());
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
    public void checkETagSameNotModified() {
        LOG.info("checkETagSameNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                header("If-None-Match", HASH1).
                header("If-Modified-Since", "Mon, 2 Jan 2016 22:22:22 GMT").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(304, response.getStatus());
    }

    @Test
    public void checkETagSameModified() {
        LOG.info("checkETagSameModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                header("If-None-Match", HASH1).
                header("If-Modified-Since", "Mon, 2 Jan 2016 00:00:00 GMT").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(304, response.getStatus());
    }

    @Test
    public void checkETagWrongModified() {
        LOG.info("checkETagWrongModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                header("If-None-Match", "51cba67887b54ccaefbba417dab6b9f64ba2d765").                            // Should be quoted! Must return body.
                header("If-Modified-Since", "Mon, 2 Jan 2016 00:00:00 GMT").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void checkETagNotSameNotModified() {
        LOG.info("checkETagNotSameNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                header("If-None-Match", "\"x\"").                               // Mismatch, so must return body.
                header("If-Modified-Since", "Mon, 2 Jan 2100 12:34:57 GMT").    // Later date.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void checkETagSame() {
        LOG.info("checkETagSame");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                header("If-None-Match", HASH1).
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(304, response.getStatus());
    }

    @Test
    public void checkETagNotSame() {
        LOG.info("checkETagNotSame");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=tpeg&model=p508&deviceID=Device999").
                request().
                header("If-None-Match", "\"x\"").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH1, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"radius\",\"value\":\"200\"}],\"matched\":\"TPEG/P508/Device999\"}",
                response.readEntity(String.class));
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

    @Test
    public void checkNoSearchResult() {
        LOG.info("checkNoSearchResult");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=XYZ").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void checkMultiSearchMatch() {
        LOG.info("checkMultiSearchMatch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=TPEG,SYS").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"parameters\":[{\"key\":\"radius\",\"value\":\"25\"},{\"key\":\"interval\",\"value\":\"120\"}],\"matched\":\"TPEG\"},{\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"matched\":\"SYS\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkMultiSearchNoMatch() {
        LOG.info("checkMultiSearchMatch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=TPEG,XYZ").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(404, response.getStatus());
    }
}
