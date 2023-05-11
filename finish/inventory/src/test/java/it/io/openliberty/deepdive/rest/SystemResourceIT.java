// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.deepdive.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Base64;
import java.util.List;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

//@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
public class SystemResourceIT {

    private static Logger logger = LoggerFactory.getLogger(SystemResourceIT.class);
    private static String appPath = "/inventory/api";
    private static String postgresHost = "postgres";
    private static String postgresImageName = "postgres-sample:latest";
    private static String appImageName = "liberty-deepdive-inventory:1.0-SNAPSHOT";

    private static String urlPath;
    
    public static SystemResourceClient client;
    // tag::network[]
    public static Network network = Network.newNetwork();
    // end::network[]
    private static String authHeader;

    // tag::postgresSetup[]
    public static GenericContainer<?> postgresContainer
        = new GenericContainer<>(postgresImageName)
              // tag::pNetwork[]
              .withNetwork(network)
              // end::pNetwork[]
              .withExposedPorts(5432)
              .withNetworkAliases(postgresHost)
              .withLogConsumer(new Slf4jLogConsumer(logger));
    // end::postgresSetup[]

    // tag::libertySetup[]
    public static LibertyContainer libertyContainer
        = new LibertyContainer(appImageName,testHttps())
              .withEnv("POSTGRES_HOSTNAME", postgresHost)
              // tag::lNetwork[]
              .withNetwork(network)
              // end::lNetwork[]
              // tag::health[]
              //.waitingFor(Wait.forHttps("/health/ready"))
              // end::health[]
              .withLogConsumer(new Slf4jLogConsumer(logger));
    // end::libertySetup[]

    private static boolean isServiceRunning(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            socket.close();
            return true;
        } catch (Exception e) {
        	return false;
        }
    }
    
    private static String getProtocol() {
        return System.getProperty("test.protocol", "https");
    }

    private static boolean testHttps() {
        return getProtocol().equalsIgnoreCase("https");
    }
    
    // tag::createRestClient[]
    private static SystemResourceClient createRestClient() throws KeyStoreException {
        ClientBuilder builder = ResteasyClientBuilder.newBuilder();
        if (testHttps()) {
        	builder.trustStore(KeyStore.getInstance("PKCS12"));
        }
        ResteasyClient client = (ResteasyClient) builder.build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(urlPath));
        return target.proxy(SystemResourceClient.class);
    }
    // end::createRestClient[]

    @BeforeAll
    public static void setup() throws Exception {
    	if (isServiceRunning("localhost", 9080)) {
    		logger.info("Testing by dev mode or local runtime...");
    		if (isServiceRunning("localhost", 5432)) {
    			logger.info("The application is ready to test.");
        		urlPath = getProtocol() + "://localhost:9443";
        	} else {
        		throw new Exception(
      	            "Postgres database is not running");
    		}
    	} else {
    		logger.info("Testing by using Testcontainers...");
    		if (isServiceRunning("localhost", 5432)) {
        		throw new Exception(
          	        "Postgres database is running locally. Stop it and retry.");    			
    		} else {
    			postgresContainer.start();
           	    libertyContainer.start();
           	    libertyContainer.waitingFor(Wait.forLogMessage("^.*CWWKF0011I.*$", 1));
          	    libertyContainer.waitingFor(Wait.forHttps("/health/ready"));
                urlPath = libertyContainer.getBaseURL(getProtocol());
    		}
    	}
        urlPath += appPath;
        System.out.println("TEST: " + urlPath);
        client = createRestClient();
        String userPassword = "bob" + ":" + "bobpwd";
        authHeader = "Basic "
            + Base64.getEncoder().encodeToString(userPassword.getBytes());
    }
    
    @AfterAll
    public static void tearDown() {
        postgresContainer.stop();
        libertyContainer.stop();
        network.close();
    }

    private void showSystemData(SystemData system) {
        System.out.println("TEST: SystemData > "
            + system.getId() + ", "
            + system.getHostname() + ", "
            + system.getOsName() + ", "
            + system.getJavaVersion() + ", "
            + system.getHeapSize());
    }

    // tag::testAddSystem[]
    @Test
    @Order(1)
    public void testAddSystem() {
        System.out.println("TEST: Testing add a system");
        // tag::addSystem[]
        client.addSystem("localhost", "linux", "11", Long.valueOf(2048));
        // end::addSystem[]
        // tag::listContents[]
        List<SystemData> systems = client.listContents();
        // end::listContents[]
        assertEquals(1, systems.size());
        showSystemData(systems.get(0));
        assertEquals("11", systems.get(0).getJavaVersion());
        assertEquals(Long.valueOf(2048), systems.get(0).getHeapSize());
    }
    // end::testAddSystem[]

    // tag::testUpdateSystem[]
    @Test
    @Order(2)
    public void testUpdateSystem() {
        System.out.println("TEST: Testing update a system");
        // tag::updateSystem[]
        client.updateSystem(authHeader, "localhost", "linux", "8", Long.valueOf(1024));
        // end::updateSystem[]
        // tag::getSystem[]
        SystemData system = client.getSystem("localhost");
        // end::getSystem[]
        showSystemData(system);
        assertEquals("8", system.getJavaVersion());
        assertEquals(Long.valueOf(1024), system.getHeapSize());
    }
    // end::testUpdateSystem[]

    // tag::testRemoveSystem[]
    @Test
    @Order(3)
    public void testRemoveSystem() {
        System.out.println("TEST: Testing remove a system");
        // tag::removeSystem[]
        client.removeSystem(authHeader, "localhost");
        // end::removeSystem[]
        List<SystemData> systems = client.listContents();
        assertEquals(0, systems.size());
    }
    // end::testRemoveSystem[]
}
