A simple example that publishes a JAX-WS Web Service implemented in CalculatorService (com.ms.wsdiscovery.examples.calculatorservice). This example will only publish the Web Service information through WS-Discovery. The actual service must also be deployed for this to work. See also [WsDiscoveryInvokeJaxWs](WsDiscoveryInvokeJaxWs.md).

```
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
```