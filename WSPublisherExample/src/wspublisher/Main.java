/*
WSPublisherExample

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

package wspublisher;

import java.io.IOException;
import ws_discovery.WsDiscoveryBuilder;
import ws_discovery.WsDiscoveryServer;

/**
 * A simple example that publishes a Web Service implemented in CalculatorWSServer.
 * This example will only publish the Web Service information through WS-Discovery. The
 * actual service must also be deployed (CalculatorWSServer) for this to work.
 * 
 * A client is implemented in CalculatorWSClientExample.
 *
 * @author Magnus Skjegstad
 */
public class Main {

    public static void main(String[] args) throws IOException, Exception {

        // Create a WS-Discovery server thread
        WsDiscoveryServer wsd = WsDiscoveryBuilder.createServer();
        
        wsd.start();

        // Publish the Web Service
        wsd.publish(new org.me.calculator.CalculatorService());
        
        while (wsd.isAlive()) 
            Thread.sleep(1000);
    }

}
