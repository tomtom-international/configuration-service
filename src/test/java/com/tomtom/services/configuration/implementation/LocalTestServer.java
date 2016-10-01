/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.speedtools.maven.MavenProperties;
import com.tomtom.speedtools.rest.Reactor;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.testutils.akka.SimpleExecutionContext;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nonnull;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class LocalTestServer {
    private static final int PORT = 8081;
    private static final String HOST = "http://localhost:";

    final private TJWSEmbeddedJaxrsServer server;
    private final String config;
    private final int port;

    public LocalTestServer(@Nonnull final String config) {
        this.config = config;
        this.port = PORT;
        server = new TJWSEmbeddedJaxrsServer();
        server.setPort(port);
    }

    @Before
    public void startServer() throws IncorrectConfigurationException {
        final ConfigurationServiceProperties configurationServiceProperties =
                new ConfigurationServiceProperties(config);
        final MavenProperties mavenProperties = new MavenProperties("1.0.0-TEST");
        final Configuration configuration = new Configuration(configurationServiceProperties);

        // Create a simple ResourceProcessor, required for implementation of REST service using the SpeedTools framework.
        final Reactor reactor = new Reactor() {
            @Nonnull
            @Override
            public ExecutionContext getExecutionContext() {
                return SimpleExecutionContext.getInstance();
            }

            // This method is stubbed and never used.
            @Nonnull
            @Override
            public DateTime getSystemStartupTime() {
                return new DateTime();
            }

            // This method is stubbed and never used.
            @Nonnull
            @Override
            public <T> T createTopLevelActor(@Nonnull final Class<T> interfaceClass, @Nonnull final Class<? extends T> implementationClass, @Nonnull Object... explicitParameters) {
                assert false;
                //noinspection ConstantConditions
                return null;
            }
        };
        final ResourceProcessor resourceProcessor = new ResourceProcessor(reactor);

        // Add root resource.
        server.getDeployment().getResources().add(new HelperResourceImpl(
                configuration,
                configurationServiceProperties,
                mavenProperties
        ));

        // Add tree resource.
        server.getDeployment().getResources().add(new TreeResourceImpl(
                configuration,
                resourceProcessor
        ));
        server.start();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Nonnull
    public String getHost() {
        return HOST + port;
    }
}
