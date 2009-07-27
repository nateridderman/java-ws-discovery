A Java implementation of the WS-Discovery specification draft (http://schemas.xmlsoap.org/ws/2005/04/discovery/).

This is version 0.1 and the first release of this library. Please check http://code.google.com/p/java-ws-discovery/ for future updates.

** License **
This program is free software: you can redistribute it and/or modify
it under the terms of the Lesser GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
Lesser GNU General Public License for more details.

You should have received a copy of the Lesser GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

** About the files **

WS-Discovery               Contains the actual WS-Discovery implementation. The other directories are examples.

WSDiscoveryGUI             A simple GUI for publishing and discovering services.

WSDiscoveryExamples        Some examples of how the WS-discovery implementation can be used to publish and find services.

WSPublisherExample         How to publish a Web Service description.

CalculatorWSClientExample  A WS-client that tries to find CalculatorWSServer by using WS-Discovery and invoke it. 
                           Requires that WSPublisherExample running and that CalculatorWSServer is deployed.

CalculatorWSServer         A simple Web Service. Deploy this before running WSPublisherExample and
                           CalculatorWSClientExample.

** Requirements **
All the examples requires the WsDiscovery.jar-file that can be found in WS-Discovery/dist after building WS-Discovery.

The Web Services and the examples interacting with them, need a Java 1.6 environment to compile properly. 

The project files were created in NetBeans 6.5.

More verbose logging in the WS-Discovery library can be enabled by adjusting ws_discovery.WsDiscoveryConstants.loggerLevel, i.e. by setting it to FINEST.

** Known limitations **
- Proxy client/server mode is currently untested and may not work properly. 
- The specification draft does not specify how a proxy server should be implemented, so proxy server mode is most likely incompatible
with other WS-Discovery implementations.
- Matching algorithms for LDAP and UUID are not implemented. They can easily be added by completing the classes
ws_discovery.servicedirectory.matcher.MatchScopeLDAP and ws_discovery.servicedirectory.matcher.MatchScopeUUID. 
- WS-Security signatures are not supported.  

** Adding matching algorithms **
Matching algorithms are used in WS-Discovery when probing for new services. The sender of the 
probe specifies which matching algorithm should be used when the receiver is looking for matching
services. If the matching algorithm is unsupported the receiver will simply discard the probe. 

The WS-Discovery specification defaults to the matching algorithm specified in RFC2396, which basically
matches the elements in the URI one element at a time (http://a.b.c/Service/ will match 
http://a.b.c/Service/MyService, but http://a.b.c/Serv will not). By creating custom matching algorithms the 
WS-Discoveryprotocol can be extended to support more advanced service discovery mechanisms.     

Matching algorithms can be added to the implementation by writing new classes that implement the IMatchType interface
and adding them to the ws_discovery.servicedirectory.MatchBy enumerator. 

** Adding transport protocols **
Currently only SOAP-over-UDP and a variant of SOAP-over-UDP using gzip-compression is implemented.  

Other transport protocols can be added by creating a class that implements the ITransportType interface. The new class
must be added to the ws_discovery.transport.TransportType enumerator. To activate the new transport protocol,
change WsDiscoveryConstants.transportType.

ws_discovery.network.transport.soapudp.zlib.SOAPOverUDPzlib is an example of how SOAP-over-UDP can be extended
to support compression. It should be relatively easy to add other compression-methods, like Efx. When compression is
enabled the implementation is not compatible with the WS-Discovery specification draft.

Magnus Skjegstad (magnus@skjegstad.com), 22.01.09