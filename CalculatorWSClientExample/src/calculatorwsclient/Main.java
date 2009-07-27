/*
CalculatorWSClient

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

package calculatorwsclient;

import java.net.URL;
import javax.xml.namespace.QName;
import ws_discovery.WsDiscoveryFinder;
import ws_discovery.servicedirectory.WsDiscoveryService;
import ws_discovery.servicedirectory.WsDiscoveryServiceDirectory;

/**
 * CalculatorWSClient
 *
 * This program will create an instance of WSDiscoveryFinder and try to
 * locate the Web Service CalculatorService.
 *
 * The CalculatorService is implemented in the example CalculatorWSServer.
 *
 * To make this work, CalculatorWSServer (the Web Service) must be deployed and
 * WSPublisherExample must be running.
 *
 * @author Magnus Skjegstad
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try { // Call Web Service Operation            
            WsDiscoveryFinder finder = new WsDiscoveryFinder();

            
            QName myService = new QName("http://calculator.me.org/", "CalculatorService");
            
            //while (true) {
                Thread.sleep(5000);
                System.out.println("Searching for " + myService);
                WsDiscoveryServiceDirectory sd = finder.find(myService, 5000);
                
                System.out.println(sd.size() + " services found.");
                
                for (int i = 0; i < sd.size(); i++) {                
                    WsDiscoveryService wsdservice = sd.get(i);
                                                            
                    URL serviceurl = new URL(wsdservice.getXAddrs().get(0));
                    org.me.calculator.CalculatorService service =                             
                            new org.me.calculator.CalculatorService(
                                serviceurl,
                                wsdservice.getTypes().get(0));
                    org.me.calculator.Calculator port = service.getCalculatorPort();
                    int a = (int)Math.round(Math.random() * 1000);
                    int b = (int)Math.round(Math.random() * 1000);
                    System.out.println("result of " + a + " + " + b + ": " + port.add(a, b));
                }
            //}
                                
        } catch (Exception ex) {
            System.out.println("Exception... ");
            ex.printStackTrace();
        // TODO handle custom exceptions here
        }       
    }
}
