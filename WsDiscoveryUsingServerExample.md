Starts a WS-Discovery server, probes for services with a Probe-message
and then displays the result. If some services are missing the XAddrs field
(invocation address) a Resolve-message is sent.

See the [WsDiscoveryFinderExample](WsDiscoveryFinderExample.md) for an alternative, and perhaps
easier way to do this.

```
public class discovery_using_server {

    public static void main(String[] args) 
            throws WsDiscoveryException, InterruptedException, UnknownHostException, SocketException {
        
        System.out.println("Starting WS-Discovery server...");

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
```