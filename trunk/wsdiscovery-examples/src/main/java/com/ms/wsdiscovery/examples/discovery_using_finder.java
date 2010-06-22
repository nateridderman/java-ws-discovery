/*
discovery_using_finder.java

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

import com.ms.wsdiscovery.WsDiscoveryConstants;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryFinder;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import java.util.logging.Level;

/**
 * An example demonstrating how to use the WsDiscoveryFinder() to find a specific
 * service or to retrieve a list of all available services through WS-Discovery.
 *
 * The WsDiscoveryFinder() will start a new WsDiscoveryServer in the background, that
 * runs until the search is completed. See discovery_using_server.java for a short example
 * of how WsDiscoveryServer() can be invoked directly.
 *
 * @author Magnus Skjegstad
 */
public class discovery_using_finder {
    public static void main(String[] args) 
            throws InterruptedException, WsDiscoveryNetworkException, WsDiscoveryException {
        
        // Create new finder instance. 
        System.out.println("Creating new finder-instance...");

        // Set the logger level
        // WsDiscoveryConstants.loggerLevel = Level.FINEST;

        // Create finder instance
        WsDiscoveryFinder finder = new WsDiscoveryFinder();

        /**
         * Search for a specific service
         */
        {
            // Describe the port type of the service we are looking for. Namespace is optional.
            QName myPortType = new QName("http://calculatorservice.examples.wsdiscovery.ms.com/", "CalculatorService");

            // Search for with 5 second timeout...
            System.out.println("Searching for service with port type \"" + myPortType.toString() + "\"");
            IWsDiscoveryServiceCollection result = finder.find(myPortType, 5000);

            // Display the results.
            System.out.println("** Discovered services: **");

            for (WsDiscoveryService service : result) {
                // Print service info
                System.out.println(service.toString());

                System.out.println("---");
            }
        }
        
        /** 
         * Search for any service
         */

        {
            System.out.println("Searching for all services (2 sec).");
            IWsDiscoveryServiceCollection result = finder.findAll(2000);
        
            // Display the results.
            System.out.println("** Discovered services: **");

            for (WsDiscoveryService service : result) {
                // Print service info
                System.out.println(service.toString());

                System.out.println("---");
            }
        }
        
        // Stop finder 
        finder.done();
    }
}
