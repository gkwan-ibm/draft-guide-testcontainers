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

public class LibertyContainer extends GenericContainer<LibertyContainer> {

    static final Logger LOGGER = LoggerFactory.getLogger(LibertyContainer.class);

    public LibertyContainer(final String dockerImageName, boolean testHttps) {
        super(dockerImageName);
        // wait for smarter planet message by default
        if (testHttps) {
            this.addExposedPorts(9443, 9080);
        } else {
            this.addExposedPorts(9080);
        }
    }

    // tag::getBaseURL[]
    public String getBaseURL(String protocol) throws IllegalStateException {
    	return protocol + "://" + this.getContainerIpAddress()
            + ":" + this.getFirstMappedPort();
    }
    // end::getBaseURL[]

}
