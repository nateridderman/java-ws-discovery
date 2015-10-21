A cross platform Java implementation of WS-Discovery as described in the specification draft from 04/2005 (http://schemas.xmlsoap.org/ws/2005/04/discovery/). For compliance with the WS-Discovery standard 1.1, please use the latest beta (available in Downloads).

**NEW 04. nov. 2011: Beta of new release available in downloads.**

WS-Discovery is a Web Service discovery protocol that uses UDP multicast and SOAP/XML to discover new services. It is one of several technologies used in Microsofts WSDAPI and Windows Rally Technologies.

The source code is released under LGPL.

### Code examples ###
  * [WsDiscoveryFinderExample](WsDiscoveryFinderExample.md) - How to discover services using WS-Discovery
  * [WsDiscoveryUsingServerExample](WsDiscoveryUsingServerExample.md) - How to discover services using the WS-Discovery server class
  * [WsDiscoveryPublishJaxWs](WsDiscoveryPublishJaxWs.md) - How to publish a JAX-WS generated service
  * [WsDiscoveryInvokeJaxWs](WsDiscoveryInvokeJaxWs.md) - How to discovery and invoke a JAX-WS generated service with WS-Discovery

### About the modules ###

  * **wsdiscovery-lib** contains the WS-Discovery core library. The other directories are examples.
  * **wsdiscovery-gui** is a simple GUI for publishing and discovering services. This GUI will be replaced by wsdiscovery-gui2 in the next release.
  * **wsdiscovery-gui2** is a rewrite the original GUI with more functionality.
  * **wsdiscovery-examples** contains some examples of how the WS-discovery implementation can be used to publish and find services.
  * **wsdiscovery-example-ws** contains an example of how to publish a Web Service description.

### Adding matching algorithms ###
Matching algorithms are used in WS-Discovery when probing for new services. The sender of the probe specifies which matching algorithm should be used when the receiver is looking for matching services. If the matching algorithm is unsupported the receiver will simply discard the probe.

The WS-Discovery specification defaults to the matching algorithm specified in RFC2396, which matches the elements in the URI one element at a time (`http://a.b.c/Service/` will match
`http://a.b.c/Service/MyService`, but `http://a.b.c/Serv` will not).

New matching algorithms can be added to the implementation by writing new classes that implement the IMatchType interface and adding them to the `com.ms.wsdiscovery.servicedirectory.MatchBy` enumerator.

### Adding transport protocols ###
Currently only SOAP-over-UDP and a variant of SOAP-over-UDP using gzip-compression is implemented.

Other transport protocols can be added by creating a class that implements the ITransportType interface. The new class must be added to the `com.ms.wsdiscovery.transport.TransportType` enumerator. To activate the new transport protocol, change `WsDiscoveryConstants.transportType`.

`com.ms.wsdiscovery.network.transport.soapudp.zlib.SOAPOverUDPzlib` is an example of how SOAP-over-UDP can be extended to support compression. It should be relatively easy to add other compression-methods, like Efx. When compression is enabled the implementation is not compatible with the WS-Discovery specification draft.

### Problems with IPv6 and multicast ###
Sun Java will by default use IPv6 multicast if the network interface has an IPv6 address. In a mixed network with IPv6 and IPv4 nodes, this leads to problems, as the IPv4-nodes will not be able to see WS-Discovery multicast messages sent by the IPv6-enabled nodes.

This problem typically occurs when combining Linux or OS X clients (IPv6 enabled by default) with Windows clients (often IPv4-only).

To force Java to only use IPv4 on all hosts, the system parameter `java.net.preferIPv4Stack` can be set to true. I.e.:
```
java -Djava.net.preferIPv4Stack=true [class name]
```



### Requirements ###
As of version 0.2, WS-Discovery must be built using Maven. This release has been tested on Maven 2.0.9, but earlier versions may work as well. The built-in Maven included in Netbeans 6.7 on Windows does not always work. We recommend that Netbeans-users configure their IDE to use an external Maven installation.

The Web Services and the examples interacting with them, need a Java 1.6 environment to compile properly.

More verbose logging in the WS-Discovery library can be enabled by adjusting `com.ms.wsdiscovery.WsDiscoveryConstants.loggerLevel`, e.g. by setting it to FINEST.

### Known limitations ###
  * The matching algorithm for LDAP is not implemented. It can be added by completing the class `com.ms.wsdiscovery.servicedirectory.matcher.MatchScopeLDAP`.
  * WS-Security signatures are not supported.

### Changes ###
In 0.2.0:
  * Added new GUI (wsdiscovery-gui2) with more functionality and logging.
  * Implemented proxy/suppression mode
  * Added support for user defined service directories, allowing persistent storage of services in proxy nodes (e.g. registries)
  * Build environment changed from ant to maven. Project separated into modules
  * Package names changed from `ws_discovery.*` to `com.ms.wsdiscovery.*`
  * Added unit tests for several classes (but far from all)
  * Added support for specifying multicast port and multicast interface
  * Added support for specifying the announced proxy address
  * Changed how the service directory works. Local service directory now contains local services, while the global service directory contains all known services - including local ones.
  * Hello is now always accepted by clients, even when using a proxy server (as described in the spec.)
  * Bye is now always accepted by clients, even when using a proxy server (as described in the spec)
  * Log messages are now more verbose, and hopefully more consistent. Several redundant messages have been removed.
  * Unicast send port is now randomly assigned
  * Listening port in proxy mode is now randomly assigned
  * Unicast and multicast now send on the same socket
  * Added variable `WsDiscoveryConstants.loggerHandler` to let the user specify where log messages are sent.
  * Fixed a bug that resulted in duplicate messages not always being discarded properly
  * Added matcher for UUIDs. Fixed several bugs in the other matchers.
  * Several other minor bug fixes

In 0.1.1:
  * The included LICENSE.txt contained GPL, not LGPL. This has been fixed. All the code included in this library is released under the LGPL license.

In 0.1:
  * This was the initial release.