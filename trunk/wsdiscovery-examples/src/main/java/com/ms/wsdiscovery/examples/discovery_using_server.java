/*
discovery_using_server.java

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

import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.WsDiscoveryServer;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Starts a WS-Discovery server, probes for services with a Probe-message 
 * and then displays the result. If some services are missing the XAddrs field
 * (invocation address) a Resolve-message is sent.
 *
 * See the discovery_using_finder-example for an alternative, and perhaps
 * easier way to do this.
 * 
 * @author Magnus Skjegstad
 */
public class discovery_using_server {

    public static void main(String[] args) 
            throws WsDiscoveryException, InterruptedException, UnknownHostException, SocketException {
        
        System.out.println("Starting WS-Discovery server...");

        // Uncomment the following to override the IP this server will announce itself at
        // if proxy mode is enabled. This IP is only used if server.enableProxyMode() is called.
        //   WsDiscoveryConstants.proxyAddress = InetAddress.getByName("10.0.1.3");
        // Uncomment the following to bind multicasts to a specific interface
        //   WsDiscoveryConstants.multicastInterface = NetworkInterface.getByInetAddress(InetAddress.getByName("10.0.1.4"));
        
        // Create a new server instance
        WsDiscoveryServer server = WsDiscoveryBuilder.createServer();
        
        // Start background threads
        server.start();

        System.out.println("Sending probe...");
        
        // Send Probe-message. 
        server.probe();
        
        // All listening WS-Discovery instances should respond to a blank probe. 
        // The background server will receive the replies and store the 
        // discovered services in a service directory.
        
        System.out.println("Waiting for replies. (2 sec)");
        Thread.sleep(2000);

        // Check if any of the discovered services are missing XAddrs (invocation address).
        // If they are, try to resolve it. 
        {
            // Get a copy of the remote service directory
            IWsDiscoveryServiceCollection result = server.getServiceDirectory().matchAll();
            boolean resolve_sent = false;

            for (WsDiscoveryService service : result)
                // Is XAddrs empty?
                if (service.getXAddrs().size() == 0) {
                    // Send Resolve-message 
                    System.out.println("Trying to resolve XAddr for service " +service.getEndpointReference());
                    server.resolve(service);
                    resolve_sent = true;
                }
                        
            if (resolve_sent) {
                System.out.println("Waiting for ResolveMatches. (2 sec)");
                Thread.sleep(2000);
            }
        }
                
        // Get a copy of the remote service directory and display the results.
        {
            System.out.println("** Discovered services: **");
            
            IWsDiscoveryServiceCollection result = server.getServiceDirectory().matchAll();

            for (WsDiscoveryService service : result) {
                // Print service info
                System.out.println(service);
                
                System.out.println("---");
            }
        }
    }

}
