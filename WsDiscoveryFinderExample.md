An example demonstrating how to use the `WsDiscoveryFinder()` to find a specific
service or to retrieve a list of all available services through WS-Discovery.

The `WsDiscoveryFinder()` will start a new `WsDiscoveryServer` in the background, that
runs until the search is completed. A short example
of how `WsDiscoveryServer()` can be invoked directly is available [here](WsDiscoveryUsingServerExample.md).

```
public class discovery_using_finder {
    public static void main(String[] args) 
            throws InterruptedException, WsDiscoveryNetworkException, WsDiscoveryException {
        
        // Create new finder instance. 
        System.out.println("Creating new finder-instance...");

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
```