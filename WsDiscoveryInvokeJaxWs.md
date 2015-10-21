This example creates an instance of WSDiscoveryFinder and tries to locate the JAX-WS Web Service CalculatorService.

For this to work, a CalculatorService must be deployed and published through WS-Discovery (see also [WsDiscoveryPublishJaxWs](WsDiscoveryPublishJaxWs.md)).

```
public class invoke_jaxws {

    public static void main(String[] args) {
        try { // Call Web Service Operation            
            WsDiscoveryFinder finder = new WsDiscoveryFinder();

            
            QName myService = new QName("http://calculatorservice.examples.wsdiscovery.ms.com/", "CalculatorService");
            
            Thread.sleep(5000);
            System.out.println("Searching for " + myService);
            IWsDiscoveryServiceCollection serviceCollection = finder.find(myService, 5000);
                
           System.out.println(serviceCollection.size() + " services found.");
                
           // Invoke the service
           for (WsDiscoveryService wsdservice : serviceCollection) {
               URL serviceurl = new URL(wsdservice.getXAddrs().get(0));
               com.ms.wsdiscovery.examples.calculatorservice.CalculatorService service =
                           new com.ms.wsdiscovery.examples.calculatorservice.CalculatorService(
                                serviceurl,
                                wsdservice.getPortTypes().get(0));
               com.ms.wsdiscovery.examples.calculatorservice.Calculator port = service.getCalculatorPort();
               int a = (int)Math.round(Math.random() * 1000);
               int b = (int)Math.round(Math.random() * 1000);
               System.out.println("result of " + a + " + " + b + ": " + port.add(a, b));
           }
                                
        } catch (Exception ex) {
            System.out.println("Exception... ");
            ex.printStackTrace();
        }       
     }
}
```