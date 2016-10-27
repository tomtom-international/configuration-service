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
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tomtom.speedtools.objects.Immutables.listOf;

@SuppressWarnings({"OverlyBroadThrowsClause", "ConstantConditions", "ProhibitedExceptionDeclared"})
public class ConfigurationTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTest.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Nonnull
    public Map<String, String> mapOf(final String... args) {
        Assert.assertTrue((args.length % 2) == 0);
        final Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i = i + 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    @Test
    public void testJson1() throws Exception {
        LOG.info("testJson1");
        final NodeDTO root = new NodeDTO(null, null, null, null, null, null, null);
        String json = mapper.writeValueAsString(root);
        LOG.info("root empty={}", json);
        Assert.assertEquals("{}", json);

        final String rootJson = mapper.writeValueAsString(root);
        NodeDTO testRoot = mapper.readValue(rootJson, NodeDTO.class);
        Assert.assertEquals(null, testRoot.getMatch());
        testRoot.setMatch("x");
        testRoot.validate();

        json = mapper.writeValueAsString(testRoot);
        LOG.info("root re-read as json={}", json);
        Assert.assertEquals("{\"match\":\"x\"}", json);

        testRoot = mapper.readValue("{ \"modified\": \"2016-01-02T12:34:56Z\", \"nodes\": [ { \"match\": \"child-1\", \"parameters\": [ { \"key\": \"key-1a\", \"value\": \"value-1a\" }, { \"key\": \"key-1b\", \"value\": \"value-1b\" } ] }, { \"match\": \"child-2\", \"parameters\": [ { \"key\": \"key-2\", \"value\": \"value-2\" } ] } ], \"parameters\": [ { \"key\": \"key-0\", \"value\": \"value-0\" } ] }",
                NodeDTO.class);
        Assert.assertEquals(null, testRoot.getMatch());
        testRoot.setMatch("x");
        testRoot.validate();

        json = mapper.writeValueAsString(testRoot);
        LOG.info("tree read as json={}", json);
        Assert.assertNotNull(testRoot.getNodes());
        Assert.assertEquals(2, testRoot.getNodes().size());
        Assert.assertEquals("{\"match\":\"x\",\"nodes\":[{\"match\":\"child-1\",\"parameters\":[{\"key\":\"key-1a\",\"value\":\"value-1a\"},{\"key\":\"key-1b\",\"value\":\"value-1b\"}]},{\"match\":\"child-2\",\"parameters\":[{\"key\":\"key-2\",\"value\":\"value-2\"}]}],\"parameters\":[{\"key\":\"key-0\",\"value\":\"value-0\"}],\"modified\":\"2016-01-02T12:34:56Z\"}",
                json);
    }

    @Test
    public void testJson2() throws Exception {
        LOG.info("testJson2");
        final NodeDTO root = new NodeDTO(null, null, null, null, null, null, null);
        Assert.assertNotNull(root);
        final List<NodeDTO> children = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            final NodeDTO child = new NodeDTO("child-" + i, null, null, null, null, null, null);
            final List<NodeDTO> subchildren = new ArrayList<>();
            for (int i2 = 0; i2 < 3; ++i2) {
                final NodeDTO subchild = new NodeDTO("subchild-" + i + '/' + i2,
                        null,
                        new ParameterListDTO(listOf(
                                new ParameterDTO("x", String.valueOf(i)),
                                new ParameterDTO("y", String.valueOf(i2)))),
                        null, null, null, null);
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
        Assert.assertEquals("{\"nodes\":[{\"match\":\"child-0\",\"nodes\":[{\"match\":\"subchild-0/0\",\"parameters\":[{\"key\":\"x\",\"value\":\"0\"},{\"key\":\"y\",\"value\":\"0\"}]},{\"match\":\"subchild-0/1\",\"parameters\":[{\"key\":\"x\",\"value\":\"0\"},{\"key\":\"y\",\"value\":\"1\"}]},{\"match\":\"subchild-0/2\",\"parameters\":[{\"key\":\"x\",\"value\":\"0\"},{\"key\":\"y\",\"value\":\"2\"}]}]},{\"match\":\"child-1\",\"nodes\":[{\"match\":\"subchild-1/0\",\"parameters\":[{\"key\":\"x\",\"value\":\"1\"},{\"key\":\"y\",\"value\":\"0\"}]},{\"match\":\"subchild-1/1\",\"parameters\":[{\"key\":\"x\",\"value\":\"1\"},{\"key\":\"y\",\"value\":\"1\"}]},{\"match\":\"subchild-1/2\",\"parameters\":[{\"key\":\"x\",\"value\":\"1\"},{\"key\":\"y\",\"value\":\"2\"}]}]},{\"match\":\"child-2\",\"nodes\":[{\"match\":\"subchild-2/0\",\"parameters\":[{\"key\":\"x\",\"value\":\"2\"},{\"key\":\"y\",\"value\":\"0\"}]},{\"match\":\"subchild-2/1\",\"parameters\":[{\"key\":\"x\",\"value\":\"2\"},{\"key\":\"y\",\"value\":\"1\"}]},{\"match\":\"subchild-2/2\",\"parameters\":[{\"key\":\"x\",\"value\":\"2\"},{\"key\":\"y\",\"value\":\"2\"}]}]}],\"modified\":\"2016-01-02T12:34:56Z\"}",
                rootJson);
    }

    @Test
    public void testFindBestMatchingParameters() throws Exception {
        LOG.info("testFindBestMatchingParameters");
        Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:example.json"));

        final String rootJson = mapper.writeValueAsString(configuration.getRoot());
        Assert.assertEquals("{\"nodes\":[{\"match\":\"traffic\",\"nodes\":[{\"match\":\"cheapo\",\"nodes\":[{\"match\":\"device[0-9]*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"10\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]},{\"match\":\"device123\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"80\"},{\"key\":\"interval_secs\",\"value\":\"60\"}]}]},{\"match\":\"luxuri\",\"nodes\":[{\"match\":\"device1.*\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"100\"}]},{\"match\":\"device999\",\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"200\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"40\"},{\"key\":\"interval_secs\",\"value\":\"120\"}]}],\"parameters\":[{\"key\":\"api_key\",\"value\":\"my_api_key\"},{\"key\":\"radius_km\",\"value\":\"25\"},{\"key\":\"interval_secs\",\"value\":\"120\"}],\"modified\":\"2016-01-02T12:34:56Z\"},{\"match\":\"settings\",\"parameters\":[{\"key\":\"demo\",\"value\":\"false\"},{\"key\":\"sound\",\"value\":\"off\"}],\"modified\":\"2016-01-02T12:34:50Z\"}],\"modified\":\"2016-01-02T12:34:00Z\",\"levels\":[\"service\",\"model\",\"device\"]}",
                rootJson);

        SearchResultsDTO y = configuration.matchNode(listOf(mapOf("service", "", "model", "", "device", "")));
        Assert.assertTrue(y.isEmpty());

        y = configuration.matchNode(listOf(mapOf("service", "unknown")));
        Assert.assertTrue(y.isEmpty());

        Assert.assertTrue(configuration.matchNode(listOf(mapOf("service", "/settings"))).isEmpty());

        SearchResultDTO x = configuration.matchNode(listOf(mapOf("service", "settings"))).get(0);
        Assert.assertEquals("demo", x.getParameters().get(0).getKey());
        Assert.assertEquals("false", x.getParameters().get(0).getValue());
        Assert.assertEquals("sound", x.getParameters().get(1).getKey());
        Assert.assertEquals("off", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=settings", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "settings"))).get(0);
        Assert.assertEquals("demo", x.getParameters().get(0).getKey());
        Assert.assertEquals("false", x.getParameters().get(0).getValue());
        Assert.assertEquals("sound", x.getParameters().get(1).getKey());
        Assert.assertEquals("off", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=settings", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "settings", "model", "unknown", "device", "device"))).get(0);
        Assert.assertEquals("demo", x.getParameters().get(0).getKey());
        Assert.assertEquals("false", x.getParameters().get(0).getValue());
        Assert.assertEquals("sound", x.getParameters().get(1).getKey());
        Assert.assertEquals("off", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=settings", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic"))).get(0);
        Assert.assertEquals("radius_km", x.getParameters().get(1).getKey());
        Assert.assertEquals("25", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=traffic", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic", "model", "unknown"))).get(0);
        Assert.assertEquals("radius_km", x.getParameters().get(1).getKey());
        Assert.assertEquals("25", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=traffic", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic", "model", "luxuri"))).get(0);
        Assert.assertEquals("radius_km", x.getParameters().get(1).getKey());
        Assert.assertEquals("40", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=traffic&model=luxuri", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic", "model", "luxuri", "device", "device123"))).get(0);
        Assert.assertEquals("radius_km", x.getParameters().get(1).getKey());
        Assert.assertEquals("100", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=traffic&model=luxuri&device=device1.*", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic", "model", "luxuri", "device", "device1.*"))).get(0);
        Assert.assertEquals("radius_km", x.getParameters().get(1).getKey());
        Assert.assertEquals("100", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=traffic&model=luxuri&device=device1.*", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic", "model", "luxuri", "device", "device.*"))).get(0);
        Assert.assertEquals("radius_km", x.getParameters().get(1).getKey());
        Assert.assertEquals("40", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=traffic&model=luxuri", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic", "model", "luxuri", "device", "device999"))).get(0);
        Assert.assertEquals("radius_km", x.getParameters().get(1).getKey());
        Assert.assertEquals("200", x.getParameters().get(1).getValue());
        Assert.assertEquals("service=traffic&model=luxuri&device=device999", x.getMatched());

        configuration = new Configuration(new ConfigurationServiceProperties("classpath:onlyparams.json"));

        x = configuration.matchNode(listOf(mapOf("service", ""))).get(0);
        Assert.assertEquals("x", x.getParameters().get(0).getKey());
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic"))).get(0);
        Assert.assertEquals("x", x.getParameters().get(0).getKey());
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("service", "traffic", "model", "luxuri"))).get(0);
        Assert.assertEquals("x", x.getParameters().get(0).getKey());
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("", x.getMatched());
    }

    @Test
    public void testFindBestMatchingParametersRegex() throws Exception {
        LOG.info("testFindBestMatchingParametersRegex");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:regex.json"));

        final SearchResultsDTO list = configuration.matchNode(listOf(mapOf("criterium", "String")));
        SearchResultDTO x = list.get(0);
        Assert.assertEquals("2", x.getParameters().get(0).getValue());
        Assert.assertEquals("criterium=String", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("criterium", "String123"))).get(0);
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("criterium=String[0-9]*", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("criterium", "String999"))).get(0);
        Assert.assertEquals("1", x.getParameters().get(0).getValue());
        Assert.assertEquals("criterium=String[0-9]*", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("criterium", "other"))).get(0);
        Assert.assertEquals("4", x.getParameters().get(0).getValue());
        Assert.assertEquals("criterium=.*", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("criterium", "String9[0-9]*"))).get(0);
        Assert.assertEquals("3", x.getParameters().get(0).getValue());
        Assert.assertEquals("criterium=String9[0-9]*", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("criterium", ".*"))).get(0);
        Assert.assertEquals("4", x.getParameters().get(0).getValue());
        Assert.assertEquals("criterium=.*", x.getMatched());

        x = configuration.matchNode(listOf(mapOf("criterium", ""))).get(0);
        Assert.assertEquals("4", x.getParameters().get(0).getValue());
        Assert.assertEquals("criterium=.*", x.getMatched());
    }

    @Test
    public void testFindNodeAndParent() throws Exception {
        LOG.info("testFindNodeAndParent");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:example.json"));

        Node x = configuration.findNode("");
        Assert.assertSame(configuration.getRoot(), x);

        x = configuration.findNode("Unknown");
        Assert.assertNull(x);

        x = configuration.findNode("traffic/Unknown");
        Assert.assertNull(x);

        x = configuration.findNode("traffic");
        Assert.assertEquals("traffic", x.getMatch());

        x = configuration.findNode("traffic/luxuri");
        Assert.assertEquals("luxuri", x.getMatch());

        x = configuration.findNode("/");            // Wrong use of '/' prefix!
        Assert.assertNull(x);

        x = configuration.findNode("/traffic");        // Wrong use of '/' prefix!
        Assert.assertNull(x);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testDuplicateName() throws IncorrectConfigurationException {
        LOG.info("testDuplicateName");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:duplicate-name.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testMissingKey() throws IncorrectConfigurationException {
        LOG.info("testMissingKey");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:missing-key.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testKeyNull() throws IncorrectConfigurationException {
        LOG.info("testKeyNull");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:key-null.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testRootNodeNotNull() throws IncorrectConfigurationException {
        LOG.info("testRootNodeNotNull");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:root-node-with-name.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testWrongDate() throws IncorrectConfigurationException {
        LOG.info("testWrongDate");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:wrong-date.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testEmptyName() throws IncorrectConfigurationException {
        LOG.info("testEmptyName");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:empty-name.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncludeMultiError1() throws IncorrectConfigurationException {
        LOG.info("testIncludeMultiError1");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:include-multi-error1.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncludeMultiError2() throws IncorrectConfigurationException {
        LOG.info("testIncludeMultiError2");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:include-multi-error2.json"));
        Assert.assertNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testIncludeRecursive() throws IncorrectConfigurationException {
        LOG.info("testIncludeRecursive");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:include-recursive.json"));
        Assert.assertNull(configuration);
    }

    @Test
    public void testIncludeMultiOK1() throws IncorrectConfigurationException {
        LOG.info("testIncludeMultiOK1");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:include-multi-ok1.json"));
        Assert.assertNotNull(configuration);
    }

    @Test
    public void testIncludeMultiOK2() throws IncorrectConfigurationException {
        LOG.info("testWrongName1");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:include-multi-ok2.json"));
        Assert.assertNotNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testWrongName1() throws IncorrectConfigurationException {
        LOG.info("testWrongName1");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:wrong-name1.json"));
        Assert.assertNotNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testWrongName2() throws IncorrectConfigurationException {
        LOG.info("testWrongName2");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:wrong-name2.json"));
        Assert.assertNotNull(configuration);
    }

    @Test(expected = IncorrectConfigurationException.class)
    public void testWrongName3() throws IncorrectConfigurationException {
        LOG.info("testWrongName3");
        final Configuration configuration = new Configuration(new ConfigurationServiceProperties("classpath:wrong-name3.json"));
        Assert.assertNotNull(configuration);
    }
}
