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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class LibertyContainer extends GenericContainer<LibertyContainer> {

    public LibertyContainer(
    	String imageName, boolean testHttps, int httpsPort, int httpPort) {
        
    	super(imageName);
        if (testHttps) {
            addExposedPorts(httpsPort, httpPort);
        } else {
            addExposedPorts(httpPort);
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
