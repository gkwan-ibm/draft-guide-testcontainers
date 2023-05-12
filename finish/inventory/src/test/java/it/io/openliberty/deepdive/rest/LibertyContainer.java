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

// logger imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// testcontainers imports
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class LibertyContainer extends GenericContainer<LibertyContainer> {

    static final Logger LOGGER = LoggerFactory.getLogger(LibertyContainer.class);

    public LibertyContainer(final String dockerImageName, boolean testHttps) {
        super(dockerImageName);
        if (testHttps) {
            addExposedPorts(9443, 9080);
        } else {
            addExposedPorts(9080);
        }
        // wait for smarter planet message by default
        waitingFor(Wait.forLogMessage("^.*CWWKF0011I.*$", 1));
    }

    // tag::getBaseURL[]
    public String getBaseURL(String protocol) throws IllegalStateException {
    	return protocol + "://" + getContainerIpAddress()
            + ":" + getFirstMappedPort();
    }
    // end::getBaseURL[]

}
