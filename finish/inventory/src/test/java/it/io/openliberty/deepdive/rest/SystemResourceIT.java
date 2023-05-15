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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

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
    
    private static int HTTP_PORT = Integer.parseInt(System.getProperty("http.port"));
    private static int HTTPS_PORT = Integer.parseInt(System.getProperty("https.port"));
    private static String APP_PATH = System.getProperty("context.root") + "/api";
    private static String APP_IMAGE = "inventory:1.0-SNAPSHOT";
    private static String USER_PASSWORD = "bob" + ":" + "bobpwd";

    private static String POSTGRES_HOST = "postgres";
    private static int POSTGRES_PORT = 5432;
    private static String POSTGRES_IMAGE = "postgres-sample:latest";

    private static SystemResourceClient client;
    private static Network network = Network.newNetwork();
    private static String authHeader;

    //@Container
    private static GenericContainer<?> postgresContainer
        = new GenericContainer<>(POSTGRES_IMAGE)
              .withNetwork(network)
              .withExposedPorts(POSTGRES_PORT)
              .withNetworkAliases(POSTGRES_HOST)
              .withLogConsumer(new Slf4jLogConsumer(logger));

    //@Container
    private static LibertyContainer inventoryContainer
        = new LibertyContainer(APP_IMAGE, testHttps(), HTTPS_PORT, HTTP_PORT)
              .withEnv("POSTGRES_HOSTNAME", POSTGRES_HOST)
              .withNetwork(network)
              .waitingFor(Wait.forHttp("/health/ready").forPort(HTTP_PORT))
              .withLogConsumer(new Slf4jLogConsumer(logger));

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
    
    private static SystemResourceClient createRestClient(String urlPath) throws KeyStoreException {
        ClientBuilder builder = ResteasyClientBuilder.newBuilder();
        if (testHttps()) {
            builder.trustStore(KeyStore.getInstance("PKCS12"));
            HostnameVerifier v = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return hostname.equals("localhost") || hostname.equals("docker");
                } };
            builder.hostnameVerifier(v );
        }
        ResteasyClient client = (ResteasyClient) builder.build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(urlPath));
        return target.proxy(SystemResourceClient.class);
    }

    @BeforeAll
    public static void setup() throws Exception {
        String urlPath;
        if (isServiceRunning("localhost", HTTP_PORT)) {
            logger.info("Testing by dev mode or local runtime...");
            if (isServiceRunning("localhost", POSTGRES_PORT)) {
                logger.info("The application is ready to test.");
                urlPath = getProtocol() + "://localhost:"
                          + (testHttps() ? HTTPS_PORT : HTTP_PORT);
            } else {
                throw new Exception(
                      "Postgres database is not running");
            }
        } else {
            logger.info("Testing by using Testcontainers...");
            if (isServiceRunning("localhost", POSTGRES_PORT)) {
                throw new Exception(
                      "Postgres database is running locally. Stop it and retry.");                
            } else {
                postgresContainer.start();
                inventoryContainer.start();
                urlPath = inventoryContainer.getBaseURL(getProtocol());
            }
        }
        urlPath += APP_PATH;
        System.out.println("TEST: " + urlPath);
        client = createRestClient(urlPath);
        authHeader = "Basic "
            + Base64.getEncoder().encodeToString(USER_PASSWORD.getBytes());
    }

    @AfterAll
    public static void tearDown() {
        inventoryContainer.stop();
        postgresContainer.stop();
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
