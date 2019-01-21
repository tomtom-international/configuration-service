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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class ApiFindBestMatchTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiFindBestMatchTest.class);

    private final LocalTestServer server = new LocalTestServer("classpath:example.json");

    static final String HASH = "\"0e54b7591e3059b0ef7b1a4d9de263081997dd69\"";

    @Before
    public void startServer() throws IncorrectConfigurationException {
        server.startServer();
    }

    @After
    public void stopServer() {
        server.stopServer();
    }

    @Test
    public void checkSearch() {
        LOG.info("checkSearch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=cheapo&device=device123").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}],\"searched\":\"service=traffic&model=cheapo&device=device123\",\"matched\":\"service=traffic&model=cheapo&device=device123\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearch2() {
        LOG.info("checkSearch2");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=cheapo&device=device123").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}],\"searched\":\"service=traffic&model=cheapo&device=device123\",\"matched\":\"service=traffic&model=cheapo&device=device123\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearch3() {
        LOG.info("checkSearch3");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=cheapo&device=").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"searched\":\"service=traffic&model=cheapo&device=\",\"matched\":\"service=traffic\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkDifferentLevelsOrder() {
        LOG.info("checkDifferentLevelsOrder");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?device=device123&service=traffic&model=cheapo").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}],\"searched\":\"service=traffic&model=cheapo&device=device123\",\"matched\":\"service=traffic&model=cheapo&device=device123\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkMultiSearch() {
        LOG.info("checkMultiSearch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic,settings&model=cheapo&device=device123").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}],\"searched\":\"service=traffic&model=cheapo&device=device123\",\"matched\":\"service=traffic&model=cheapo&device=device123\"},{\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"searched\":\"service=settings&model=cheapo&device=device123\",\"matched\":\"service=settings\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearchLevelDoesNotExist() {
        LOG.info("checkSearchLevelDoesNotExist");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=cheapo&device=device123&unknown=456").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}],\"searched\":\"service=traffic&model=cheapo&device=device123\",\"matched\":\"service=traffic&model=cheapo&device=device123\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkTooFewLevels() {
        LOG.info("checkTooFewLevels");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"searched\":\"service=traffic&model=luxuri&device=\",\"matched\":\"service=traffic&model=luxuri\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkSearchSearchForEmptyLastLevel() {
        LOG.info("checkSearchSearchForEmptyLastLevel");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"searched\":\"service=traffic&model=luxuri&device=\",\"matched\":\"service=traffic&model=luxuri\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAllLevelsProvided() {
        LOG.info("checkAllLevelsProvided");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}],\"searched\":\"service=traffic&model=luxuri&device=device999\",\"matched\":\"service=traffic&model=luxuri&device=device999\"}",
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
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void checkUsingCorrectSeparator() {
        LOG.info("checkUsingCorrectSeparator");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic,traffic,traffic&model=,luxuri&device=,,device999").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"searched\":\"service=traffic&model=&device=\",\"matched\":\"service=traffic\"},{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"searched\":\"service=traffic&model=luxuri&device=\",\"matched\":\"service=traffic&model=luxuri\"},{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}],\"searched\":\"service=traffic&model=luxuri&device=device999\",\"matched\":\"service=traffic&model=luxuri&device=device999\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkUsingWrongSeparator() {
        LOG.info("checkUsingWrongSeparator");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999;unknown").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void checkNotModified() {
        LOG.info("checkNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:57 GMT").    // 1 sec later than config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(304, response.getStatus());
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
    }

    @Test
    public void checkModified() {
        LOG.info("checkModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-Modified-Since", "Mon, 2 Jan 2016 12:34:56 GMT").    // Same time as in config tree.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}],\"searched\":\"service=traffic&model=luxuri&device=device999\",\"matched\":\"service=traffic&model=luxuri&device=device999\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkETagSameNotModified() {
        LOG.info("checkETagSameNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-None-Match", HASH).
                header("If-Modified-Since", "Mon, 2 Jan 2016 22:22:22 GMT").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(304, response.getStatus());
    }

    @Test
    public void checkETagSameModified() {
        LOG.info("checkETagSameModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-None-Match", HASH).
                header("If-Modified-Since", "Mon, 2 Jan 2016 00:00:00 GMT").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(304, response.getStatus());
    }

    @Test
    public void checkETagWrongModified() {
        LOG.info("checkETagWrongModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-None-Match", "51cba67887b54ccaefbba417dab6b9f64ba2d765").                            // Should be quoted! Must return body.
                header("If-Modified-Since", "Mon, 2 Jan 2016 00:00:00 GMT").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void checkETagNotSameNotModified() {
        LOG.info("checkETagNotSameNotModified");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-None-Match", "\"x\"").                               // Mismatch, so must return body.
                header("If-Modified-Since", "Mon, 2 Jan 2100 12:34:57 GMT").    // Later date.
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
    }

    @Test
    public void checkETagSame() {
        LOG.info("checkETagSame");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-None-Match", HASH).
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(304, response.getStatus());
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
    }

    @Test
    public void checkETagNotSame() {
        LOG.info("checkETagNotSame");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic&model=luxuri&device=device999").
                request().
                header("If-None-Match", "\"x\"").
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(HASH, response.getHeaderString("ETag"));
        Assert.assertEquals("Sat, 02 Jan 2016 12:34:56 GMT", response.getHeaderString("Last-Modified"));
        Assert.assertEquals("{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}],\"searched\":\"service=traffic&model=luxuri&device=device999\",\"matched\":\"service=traffic&model=luxuri&device=device999\"}",
                response.readEntity(String.class));
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
                target(server.getHost() + "/tree?service=traffic,settings").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"searched\":\"service=traffic&model=&device=\",\"matched\":\"service=traffic\"},{\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"searched\":\"service=settings&model=&device=\",\"matched\":\"service=settings\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkMultiSearchNoMatch() {
        LOG.info("checkMultiSearchMatch");
        final Response response = new ResteasyClientBuilder().build().
                target(server.getHost() + "/tree?service=traffic,XYZ").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(404, response.getStatus());
    }
}
