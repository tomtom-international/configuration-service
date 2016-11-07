/**
 * Copyright (C) 2016, TomTom International BV (http://www.tomtom.com)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.services.configuration.domain.Node;
import com.tomtom.services.configuration.dto.NodeDTO;
import com.tomtom.speedtools.objects.Immutables;
import com.tomtom.speedtools.time.UTCTime;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"OverlyBroadThrowsClause", "ProhibitedExceptionDeclared"})
public class ConfigurationFormatTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationFormatTest.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testEmptyJson() throws Exception {
        LOG.info("testEmptyJson");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:empty.json"));
        final Node root = configuration.getRoot();
        Assert.assertNotNull(root);
        Assert.assertNull(root.getMatch());
        Assert.assertNull(root.getNodes());
        Assert.assertNull(root.getParameters());
        Assert.assertNull(root.getModified());
        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{}", rootJson);
    }

    @Test
    public void testEmptyXml() throws Exception {
        LOG.info("testEmptyXml");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:empty.xml"));
        final Node root = configuration.getRoot();
        Assert.assertNotNull(root);
        Assert.assertNull(root.getMatch());
        Assert.assertNull(root.getNodes());
        Assert.assertNull(root.getParameters());
        Assert.assertNull(root.getModified());
        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{}", rootJson);
    }

    @Test
    public void testSimple1Json() throws Exception {
        LOG.info("testSimple1Json");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:simple1.json"));
        final Node root = configuration.getRoot();
        Assert.assertNotNull(root);
        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"child-1\",\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}],\"modified\":\"2016-01-02T11:11:11Z\"},{\"match\":\"child-2\",\"parameters\":[{\"key\":\"key-2\",\"value\":\"value-2\"}]}],\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"modified\":\"2016-01-02T00:00:00Z\",\"levels\":[\"criterium\"]}",
                rootJson);
    }

    @Test
    public void testSimple2Json() throws Exception {
        LOG.info("testSimple2Json");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:simple2.json"));
        final Node root = configuration.getRoot();
        Assert.assertNotNull(root);
        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"traffic\",\"nodes\":[{\"match\":\"cheapo\",\"parameters\":[{\"key\":\"radius_km\",\"value\":\"25\"}]},{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device123\",\"parameters\":[{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"radius_km\",\"value\":\"200\"}]}]}],\"parameters\":[{\"key\":\"radius_km\",\"value\":\"50\"}]},{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}]}],\"modified\":\"2016-04-05T17:28:16Z\",\"levels\":[\"service\",\"model\",\"device\"]}",
                rootJson);
    }

    @Test
    public void testSimple1Xml() throws Exception {
        LOG.info("testSimple1Xml");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:simple1.xml"));
        final Node root = configuration.getRoot();
        Assert.assertNotNull(root);
        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"child-1\",\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}]},{\"match\":\"child-2\",\"parameters\":[{\"key\":\"key-2\",\"value\":\"value-2\"}]}],\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"modified\":\"2016-01-02T12:34:56Z\",\"levels\":[\"criterium\"]}",
                rootJson);
    }

    @Test
    public void testSimple2Xml() throws Exception {
        LOG.info("testSimple2Xml");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:simple2.xml"));
        final Node root = configuration.getRoot();
        Assert.assertNotNull(root);
        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"traffic\",\"nodes\":[{\"match\":\"cheapo\",\"parameters\":[{\"key\":\"radius_km\",\"value\":\"25\"}]},{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device123\",\"parameters\":[{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"radius_km\",\"value\":\"200\"}]}]}],\"parameters\":[{\"key\":\"radius_km\",\"value\":\"50\"}]},{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}]}],\"modified\":\"2016-04-05T17:28:16Z\",\"levels\":[\"service\",\"model\",\"device\"]}",
                rootJson);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testIncludesJson() throws Exception {
        LOG.info("testIncludesFile");
        final ConfigurationServiceProperties properties = new ConfigurationServiceProperties("classpath:example.json");
        Assert.assertEquals("classpath:example.json", properties.getStartupConfigurationURI());

        final Configuration configuration = new Configuration(properties);
        final Node root = configuration.getRoot();
        final NodeDTO rootDTO = new NodeDTO(root);
        Assert.assertNotNull(rootDTO);
        rootDTO.validate();

        Assert.assertNull(rootDTO.getMatch());
        Assert.assertNotNull(rootDTO.getNodes());
        Assert.assertEquals(2, rootDTO.getNodes().size());
        Immutables.listOf(rootDTO.getNodes()).stream().forEach(x -> {
            x.validate();
            if (x.getParameters() != null) {
                x.getParameters().validate();
            }
        });

        Assert.assertEquals("traffic", rootDTO.getNodes().get(0).getMatch());
        Assert.assertEquals("settings", rootDTO.getNodes().get(1).getMatch());
        Assert.assertNotNull(rootDTO.getNodes().get(1).getParameters());

        Assert.assertEquals(2, rootDTO.getNodes().get(1).getParameters().size());
        Assert.assertEquals("demo", rootDTO.getNodes().get(1).getParameters().get(0).getKey());
        Assert.assertEquals("false", rootDTO.getNodes().get(1).getParameters().get(0).getValue());
        Assert.assertEquals("sound", rootDTO.getNodes().get(1).getParameters().get(1).getKey());
        Assert.assertEquals("off", rootDTO.getNodes().get(1).getParameters().get(1).getValue());

        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"traffic\",\"nodes\":[{\"match\":\"cheapo\",\"nodes\":[{\"match\":\"device[0-9]*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"10\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]},{\"match\":\"device123\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}]}]},{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device1.*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"modified\":\"2016-01-02T12:34:56Z\"},{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}],\"modified\":\"2016-01-02T12:34:00Z\",\"levels\":[\"service\",\"model\",\"device\"]}",
                rootJson);

        final String rootDTOJson = mapper.writeValueAsString(rootDTO);
        LOG.info("rootDTOJson={}", rootDTOJson);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"traffic\",\"nodes\":[{\"match\":\"cheapo\",\"nodes\":[{\"match\":\"device[0-9]*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"10\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]},{\"match\":\"device123\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}]}]},{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device1.*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"modified\":\"2016-01-02T12:34:56Z\"},{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}],\"modified\":\"2016-01-02T12:34:00Z\",\"levels\":[\"service\",\"model\",\"device\"]}",
                rootDTOJson);
    }

    @Test
    public void testIncludesXml() throws Exception {
        LOG.info("testIncludesXml");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:example.xml"));
        Assert.assertNotNull(configuration);
        final Node root = configuration.getRoot();
        Assert.assertNotNull(root);
        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertEquals("{\"nodes\":[{\"match\":\"traffic\",\"nodes\":[{\"match\":\"cheapo\",\"nodes\":[{\"match\":\"device[0-9]*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"10\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]},{\"match\":\"device123\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}]}]},{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device1.*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"modified\":\"2016-01-02T12:34:56Z\"},{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}],\"modified\":\"2016-01-02T12:34:56Z\",\"levels\":[\"service\",\"model\",\"device\"]}",
                rootJson);
    }

    @Test
    public void testModified1Json() throws Exception {
        LOG.info("testModified1Json");
        final ConfigurationServiceProperties properties = new ConfigurationServiceProperties("classpath:modified1.json");
        Assert.assertEquals("classpath:modified1.json", properties.getStartupConfigurationURI());

        final Configuration configuration = new Configuration(properties);
        final Node root = configuration.getRoot();
        Assert.assertEquals(UTCTime.parse("2016-01-01T00:00:10Z"), root.getModified());

        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        final String expected = "{\"nodes\":[{\"match\":\"time 30\",\"nodes\":[{\"match\":\"time 20\",\"parameters\":[{\"key\":\"x\",\"value\":\"20\"}],\"modified\":\"2016-01-01T00:00:20Z\"}],\"parameters\":[{\"key\":\"x\",\"value\":\"30\"}],\"modified\":\"2016-01-01T00:00:30Z\"}],\"parameters\":[{\"key\":\"x\",\"value\":\"10\"}],\"modified\":\"2016-01-01T00:00:10Z\",\"levels\":[\"l1\",\"l2\"]}";
        Assert.assertEquals(expected, rootJson);
    }

    @Test
    public void testModified2Json() throws Exception {
        LOG.info("testModified2Json");
        final ConfigurationServiceProperties properties = new ConfigurationServiceProperties("classpath:example.json");
        Assert.assertEquals("classpath:example.json", properties.getStartupConfigurationURI());

        final Configuration configuration = new Configuration(properties);
        final Node root = configuration.getRoot();
        Assert.assertEquals(UTCTime.parse("2016-01-02T12:34:00Z"), root.getModified());
    }
}
