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

    static final String HASH = "\"c5072e7f10f980f06ed99d6f64483bc439968afa\"";

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
        Assert.assertEquals("{\"nodes\":[{\"match\":\"traffic\",\"nodes\":[{\"match\":\"cheapo\",\"nodes\":[{\"match\":\"device[0-9]*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"10\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]},{\"match\":\"device123\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}]}]},{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device1.*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"modified\":\"2016-01-02T12:34:56Z\"},{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}],\"modified\":\"2016-01-02T12:34:00Z\",\"levels\":[\"service\",\"model\",\"device\"]}",
                s);
        final NodeDTO x = new Gson().fromJson(s, NodeDTO.class);
        Assert.assertEquals(null, x.getMatch());
        Assert.assertNotNull(x.getNodes());

        final NodeDTO n = x.getNodes().get(0);
        Assert.assertNotNull(n.getNodes());
        Assert.assertNotNull(n.getParameters());
        Assert.assertEquals("traffic", n.getMatch());
        Assert.assertEquals(2, n.getNodes().size());
        Assert.assertEquals(3, n.getParameters().size());
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
                target(server.getHost() + "/tree/settings").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSubTree2() {
        LOG.info("checkSubTree2");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/traffic/luxuri").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device1.*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkNodeNotModified() {
        LOG.info("checkNodeNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/traffic/luxuri/device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:57 GMT").    // 1 sec later than config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(304, response.getStatus());
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
    }

    @Test
    public void checkNodeModified() {
        LOG.info("checkNodeModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/traffic/luxuri/device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:56 GMT").    // Same time as in config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"match\":\"device999\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}]}",
                response.readEntity(String.class));
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
    }

    @Test
    public void checkModifiedFormatWrong() {
        LOG.info("checkModifiedFormatWrong");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree/traffic").
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
                target(server.getHost() + "/tree/traffic").
                request().
                header("If-None-Match", "").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
    }
}
