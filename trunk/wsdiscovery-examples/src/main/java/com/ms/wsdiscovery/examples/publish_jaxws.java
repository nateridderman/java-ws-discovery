/*
publish_jaxws.java

Copyright (C) 2008-2009 Magnus Skjegstad

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.ms.wsdiscovery.examples;

import java.io.IOException;
import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.WsDiscoveryServer;

/**
 * A simple example that publishes a JAX-WS Web Service implemented in CalculatorService 
 * (com.ms.wsdiscovery.examples.calculatorservice). This example will only publish the
 * Web Service information through WS-Discovery. The actual service must also be deployed
 * for this to work.
 *
 * A client is implemented in invoke_jaxws.java.
 *
 * @author Magnus Skjegstad
 */
public class publish_jaxws {

    public static void main(String[] args) throws IOException, Exception {

        // Create a WS-Discovery server thread
        WsDiscoveryServer wsd = WsDiscoveryBuilder.createServer();

        wsd.start();

        // Publish the Web Service from a stub generated from a WSDL by JAX-WS' wsimport
        wsd.publish(new com.ms.wsdiscovery.examples.calculatorservice.CalculatorService());

        while (wsd.isAlive())
            Thread.sleep(1000);
    }

}
