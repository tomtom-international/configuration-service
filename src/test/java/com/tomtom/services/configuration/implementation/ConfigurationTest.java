/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.services.configuration.domain.Node;
import com.tomtom.services.configuration.dto.NodeDTO;
import com.tomtom.services.configuration.dto.ParameterDTO;
import com.tomtom.services.configuration.dto.ParameterListDTO;
import com.tomtom.services.configuration.dto.SearchResultDTO;
import com.tomtom.services.configuration.dto.SearchResultsDTO;
import com.tomtom.speedtools.objects.Immutables;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"OverlyBroadThrowsClause", "ConstantConditions", "ProhibitedExceptionDeclared"})
public class ConfigurationTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTest.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testJson1() throws Exception {
        LOG.info("testJson1");
        final NodeDTO root = new NodeDTO(null, null, null, null, null, null);
        String json = mapper.writeValueAsString(root);
        LOG.info("root empty={}", json);
        Assert.assertEquals("{}", json);

        final String rootJson = mapper.writeValueAsString(root);
        NodeDTO testRoot = mapper.readValue(rootJson, NodeDTO.class);
        Assert.assertEquals(null, testRoot.getName());
        testRoot.setName("x");
        testRoot.validate();

        json = mapper.writeValueAsString(testRoot);
        LOG.info("root re-read as json={}", json);
        Assert.assertEquals("{\"name\":\"x\"}", json);

        testRoot = mapper.readValue("{ \"modified\": \"2016-01-02T12:34:56Z\", \"nodes\": [ { \"name\": \"child-1\", \"parameters\": [ { \"key\": \"key-1a\", \"value\": \"value-1a\" }, { \"key\": \"key-1b\", \"value\": \"value-1b\" } ] }, { \"name\": \"child-2\", \"parameters\": [ { \"key\": \"key-2\", \"value\": \"value-2\" } ] } ], \"parameters\": [ { \"key\": \"key-0\", \"value\": \"value-0\" } ] }",
                NodeDTO.class);
        Assert.assertEquals(null, testRoot.getName());
        testRoot.setName("x");
        testRoot.validate();

        json = mapper.writeValueAsString(testRoot);
        LOG.info("tree read as json={}", json);
        Assert.assertNotNull(testRoot.getNodes());
        Assert.assertEquals(2, testRoot.getNodes().size());
        Assert.assertEquals("{\"name\":\"x\",\"nodes\":[{\"name\":\"child-1\",\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}]},{\"name\":\"child-2\",\"parameters\":[{\"key\":\"key-2\",\"value\":\"value-2\"}]}],\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"modified\":\"2016-01-02T12:34:56Z\"}",
                json);
    }

    @Test
    public void testJson2() throws Exception {
        LOG.info("testJson2");
        final NodeDTO root = new NodeDTO(null, null, null, null, null, null);
        Assert.assertNotNull(root);
        final List<NodeDTO> children = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            final NodeDTO child = new NodeDTO("child-" + i, null, null, null, null, null);
            final List<NodeDTO> subchildren = new ArrayList<>();
            for (int i2 = 0; i2 < 3; ++i2) {
                final NodeDTO subchild = new NodeDTO("subchild-" + i + '/' + i2,
                        null,
                        new ParameterListDTO(Immutables.listOf(
                                new ParameterDTO("x", String.valueOf(i)),
                                new ParameterDTO("y", String.valueOf(i2)))),
                        null, null, null);
                subchildren.add(subchild);
            }
            child.setNodes(subchildren);
            children.add(child);
        }
        root.setNodes(children);
        root.setModified("2016-01-02T12:34:56Z");
        root.validate();

        final String rootJson = mapper.writeValueAsString(root);
        LOG.info("rootJson={}", rootJson);
        Assert.assertNotNull(root.getNodes());
        Assert.assertEquals(3, root.getNodes().size());
        Assert.assertEquals("{\"nodes\":[{\"name\":\"child-0\",\"nodes\":[{\"name\":\"subchild-0/0\",\"parameters\":[{\"key\":\"x\",\"value\":\"0\"},{\"key\":\"y\",\"value\":\"0\"}]},{\"name\":\"subchild-0/1\",\"parameters\":[{\"key\":\"x\",\"value\":\"0\"},{\"key\":\"y\",\"value\":\"1\"}]},{\"name\":\"subchild-0/2\",\"parameters\":[{\"key\":\"x\",\"value\":\"0\"},{\"key\":\"y\",\"value\":\"2\"}]}]},{\"name\":\"child-1\",\"nodes\":[{\"name\":\"subchild-1/0\",\"parameters\":[{\"key\":\"x\",\"value\":\"1\"},{\"key\":\"y\",\"value\":\"0\"}]},{\"name\":\"subchild-1/1\",\"parameters\":[{\"key\":\"x\",\"value\":\"1\"},{\"key\":\"y\",\"value\":\"1\"}]},{\"name\":\"subchild-1/2\",\"parameters\":[{\"key\":\"x\",\"value\":\"1\"},{\"key\":\"y\",\"value\":\"2\"}]}]},{\"name\":\"child-2\",\"nodes\":[{\"name\":\"subchild-2/0\",\"parameters\":[{\"key\":\"x\",\"value\":\"2\"},{\"key\":\"y\",\"value\":\"0\"}]},{\"name\":\"subchild-2/1\",\"parameters\":[{\"key\":\"x\",\"value\":\"2\"},{\"key\":\"y\",\"value\":\"1\"}]},{\"name\":\"subchild-2/2\",\"parameters\":[{\"key\":\"x\",\"value\":\"2\"},{\"key\":\"y\",\"value\":\"2\"}]}]}],\"modified\":\"2016-01-02T12:34:56Z\"}",
                rootJson);
    }

    @Test
    public void testFindBestMatchingParameters() throws Exception {
        LOG.info("testFindBestMatchingParameters");
        Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:example.json"));

        final String rootJson = mapper.writeValueAsString(configuration.getRoot());
        Assert.assertEquals("{\"nodes\":[{\"name\":\"TPEG\",\"nodes\":[{\"name\":\"P107\",\"nodes\":[{\"name\":\"Device[0-9]*\",\"parameters\":[{\"key\":\"radius\",\"value\":\"10\"},{\"key\":\"interval\",\"value\":\"120\"}]},{\"name\":\"Device123\",\"parameters\":[{\"key\":\"radius\",\"value\":\"80\"},{\"key\":\"interval\",\"value\":\"60\"}]}]},{\"name\":\"P508\",\"nodes\":[{\"name\":\"Device1.*\",\"parameters\":[{\"key\":\"radius\",\"value\":\"100\"}]},{\"name\":\"Device999\",\"parameters\":[{\"key\":\"radius\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"radius\",\"value\":\"40\"},{\"key\":\"interval\",\"value\":\"120\"}]}],\"parameters\":[{\"key\":\"radius\",\"value\":\"25\"},{\"key\":\"interval\",\"value\":\"120\"}],\"modified\":\"2016-01-02T12:34:56Z\"},{\"name\":\"SYS\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}],\"modified\":\"2016-01-02T12:34:00Z\",\"levels\":[\"service\",\"model\",\"deviceID\"]}",
                rootJson);

        SearchResultsDTO y = configuration.findBestMatchingNodes("service/model/deviceID", "");
        Assert.assertTrue(y.isEmpty());

        y = configuration.findBestMatchingNodes("service/model/deviceID", "Unknown");
        Assert.assertTrue(y.isEmpty());

        SearchResultDTO x = configuration.findBestMatchingNodes("service/model/deviceID", "/SYS").get(0);
        Assert.assertEquals("demo", x.getParameters().get(0).getKey());
        Assert.assertEquals("false", x.getParameters().get(0).getValue());
        Assert.assertEquals("sound", x.getParameters().get(1).getKey());
        Assert.assertEquals("off", x.getParameters().get(1).getValue());
        Assert.assertEquals("/SYS", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model/deviceID", "SYS").get(0);
        Assert.assertEquals("demo", x.getParameters().get(0).getKey());
        Assert.assertEquals("false", x.getParameters().get(0).getValue());
        Assert.assertEquals("sound", x.getParameters().get(1).getKey());
        Assert.assertEquals("off", x.getParameters().get(1).getValue());
        Assert.assertEquals("/SYS", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model/deviceID", "SYS/Unknown/Device").get(0);
        Assert.assertEquals("demo", x.getParameters().get(0).getKey());
        Assert.assertEquals("false", x.getParameters().get(0).getValue());
        Assert.assertEquals("sound", x.getParameters().get(1).getKey());
        Assert.assertEquals("off", x.getParameters().get(1).getValue());
        Assert.assertEquals("/SYS", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model/deviceID", "TPEG").get(0);
        Assert.assertEquals("radius", x.getParameters().get(0).getKey());
        Assert.assertEquals("25", x.getParameters().get(0).getValue());
        Assert.assertEquals("/TPEG", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model/deviceID", "TPEG/Unknown").get(0);
        Assert.assertEquals("radius", x.getParameters().get(0).getKey());
        Assert.assertEquals("25", x.getParameters().get(0).getValue());
        Assert.assertEquals("/TPEG", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model/deviceID", "TPEG/P508").get(0);
        Assert.assertEquals("radius", x.getParameters().get(0).getKey());
        Assert.assertEquals("40", x.getParameters().get(0).getValue());
        Assert.assertEquals("/TPEG/P508", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model/deviceID", "TPEG/P508/Device1.*").get(0);
        Assert.assertEquals("radius", x.getParameters().get(0).getKey());
        Assert.assertEquals("100", x.getParameters().get(0).getValue());
        Assert.assertEquals("/TPEG/P508/Device1.*", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model/deviceID", "TPEG/P508/Device999").get(0);
        Assert.assertEquals("radius", x.getParameters().get(0).getKey());
        Assert.assertEquals("200", x.getParameters().get(0).getValue());
        Assert.assertEquals("/TPEG/P508/Device999", x.getMatched());

        configuration = new Configuration(new ConfigurationServiceProperties("classpath:onlyparams.json"));

        x = configuration.findBestMatchingNodes("service/model/deviceID", "/").get(0);
        Assert.assertEquals("x", x.getParameters().get(0).getKey());
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("/", x.getMatched());

        x = configuration.findBestMatchingNodes("service", "TPEG").get(0);
        Assert.assertEquals("x", x.getParameters().get(0).getKey());
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("/", x.getMatched());

        x = configuration.findBestMatchingNodes("service", "/TPEG").get(0);
        Assert.assertEquals("x", x.getParameters().get(0).getKey());
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("/", x.getMatched());

        x = configuration.findBestMatchingNodes("service/model", "TPEG/P508").get(0);
        Assert.assertEquals("x", x.getParameters().get(0).getKey());
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("/", x.getMatched());
    }

    @Test
    public void testFindBestMatchingParametersRegex() throws Exception {
        LOG.info("testFindBestMatchingParametersRegex");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:regex.json"));

        final SearchResultsDTO list = configuration.findBestMatchingNodes("criterium", "String");
        SearchResultDTO x = list.get(0);
        Assert.assertEquals("2", x.getParameters().get(0).getValue());
        Assert.assertEquals("/String", x.getMatched());

        x = configuration.findBestMatchingNodes("criterium", "String123").get(0);
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("/String[0-9]*", x.getMatched());

        x = configuration.findBestMatchingNodes("criterium", "String999").get(0);
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("/String[0-9]*", x.getMatched());

        x = configuration.findBestMatchingNodes("criterium", "other").get(0);
        Assert.assertEquals("4", x.getParameters().get(0).getValue());
        Assert.assertEquals("/.*", x.getMatched());

        x = configuration.findBestMatchingNodes("criterium", "String9[0-9]*").get(0);
        Assert.assertEquals("3", x.getParameters().get(0).getValue());
        Assert.assertEquals("/String9[0-9]*", x.getMatched());

        x = configuration.findBestMatchingNodes("criterium", ".*").get(0);
        Assert.assertEquals("4", x.getParameters().get(0).getValue());
        Assert.assertEquals("/.*", x.getMatched());

        x = configuration.findBestMatchingNodes("criterium", "/.*").get(0);
        Assert.assertEquals("4", x.getParameters().get(0).getValue());
        Assert.assertEquals("/.*", x.getMatched());

        x = configuration.findBestMatchingNodes("criterium", "").get(0);
        Assert.assertEquals("4", x.getParameters().get(0).getValue());
        Assert.assertEquals("/.*", x.getMatched());
    }

    @Test
    public void testFindNodeAndParent() throws Exception {
        LOG.info("testFindNodeAndParent");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:example.json"));

        Node x = configuration.findNode("");
        Assert.assertSame(configuration.getRoot(), x);

        x = configuration.findNode("/");
        Assert.assertSame(configuration.getRoot(), x);

        x = configuration.findNode("Unknown");
        Assert.assertNull(x);

        x = configuration.findNode("TPEG/Unknown");
        Assert.assertNull(x);

        x = configuration.findNode("TPEG");
        Assert.assertEquals("TPEG", x.getName());

        x = configuration.findNode("/TPEG");
        Assert.assertEquals("TPEG", x.getName());

        x = configuration.findNode("/TPEG/P508");
        Assert.assertEquals("P508", x.getName());
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncorrect1() throws IncorrectConfigurationException {
        LOG.info("testIncorrect1");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:incorrect1.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncorrect2() throws IncorrectConfigurationException {
        LOG.info("testIncorrect2");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:incorrect2.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncorrect3() throws IncorrectConfigurationException {
        LOG.info("testIncorrect3");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:incorrect3.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncorrect4() throws IncorrectConfigurationException {
        LOG.info("testIncorrect4");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:incorrect4.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncorrect5() throws IncorrectConfigurationException {
        LOG.info("testIncorrect5");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:incorrect5.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncorrect6() throws IncorrectConfigurationException {
        LOG.info("testIncorrect6");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:incorrect6.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncludeMultiError1() throws IncorrectConfigurationException {
        LOG.info("testIncludeMultiError1");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:includemultierror1.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncludeMultiError2() throws IncorrectConfigurationException {
        LOG.info("testIncludeMultiError2");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:includemultierror2.json"));
        Assert.assertNull(configuration);
    }

    @Test
    public void testIncludeMultiOK1() throws IncorrectConfigurationException {
        LOG.info("testIncludeMultiOK1");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:includemultiok1.json"));
        Assert.assertNotNull(configuration);
    }

    @Test
    public void testIncludeMultiOK2() throws IncorrectConfigurationException {
        LOG.info("testIncludeMultiOK2");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:includemultiok2.json"));
        Assert.assertNotNull(configuration);
    }
}
