/*
WsDiscoveryBuilder.java

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

package ws_discovery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import ws_discovery.network.exception.WsDiscoveryNetworkException;
import ws_discovery.servicedirectory.WsDiscoveryService;
import ws_discovery.servicedirectory.WsDiscoveryServiceDirectory;
import ws_discovery.servicedirectory.matcher.MatchBy;
import ws_discovery.xml.jaxb_generated.ScopesType;

/** 
 * Methods for creating common WS-Discovery objects.
 *
 * @author Magnus Skjegstad
 */
public class WsDiscoveryBuilder {
    /**
     * Creates a WS-Discovery server object. The returned instance must be started 
     * before it can be used. See {@link WsDiscoveryServer}.
     * 
     * @return New WS-Discovery object.
     * @throws WsDiscoveryNetworkException
     */
    public static WsDiscoveryServer createServer() throws WsDiscoveryNetworkException {
        return new WsDiscoveryServer();
    }
    /**
     * Creates a named service directory.
     * @param name Name of service directory.
     * @return New, empty service directory.
     */
    public static WsDiscoveryServiceDirectory createServiceDirectory(String name) {
        return new WsDiscoveryServiceDirectory(name);
    }
    /**
     * Creates a service directory. 
     * @return A new, empty service directory.
     */
    public static WsDiscoveryServiceDirectory createServiceDirectory() {
        return createServiceDirectory();
    }
    /**
     * Creates a new WS-Discovery service description. If you need to create a service 
     * with more than one portType or scope, create a {@link WsDiscoveryService} instance
     * manually.
     * @param portType Service portType
     * @param scope Service scope
     * @param XAddr Invocation URI
     * @return A new instance of {@link WsDiscoveryService}
     */
    public static WsDiscoveryService createService(QName portType, String scope, String XAddr) {
        ScopesType scopes = new ScopesType();
        scopes.getValue().add(scope);
        return new WsDiscoveryService(portType, scopes, XAddr);
    }
    /**
     * Creates a WS-Discovery service description from a JAX-WS service description.
     * <p>
     * <li>The JAX-WS service name is used as WS-Discovery portType.</li>
     * <li>The JAX-WS WSDL-location is used as WS-Discovery XAddrs.</li>
     * <li>The JAX-WS port types are used as WS-Discovery scopes.</li>
     * @param jaxwsservice JAX-WS service description.
     * @return WS-Discovery service description.
     */
    public static WsDiscoveryService createService(Service jaxwsservice) {
        ScopesType scopes = new ScopesType();
        List<String> xaddrs = new ArrayList<String>();
        List<QName> ports = new ArrayList<QName>();       
        
        // Add scopes
        for (Iterator<QName> i = jaxwsservice.getPorts(); i.hasNext(); ) {
            QName q = i.next();
            // Use namespaces from the ports to assume scopes
            if (!scopes.getValue().contains(q.getNamespaceURI()))
                scopes.getValue().add(q.getNamespaceURI());
        }
        
        // Add port
        ports.add(jaxwsservice.getServiceName());
        
        // Add xaddrs. Just use WSDL-address
        xaddrs.add(jaxwsservice.getWSDLDocumentLocation().toString());
        
        // .. no scopes...
        
        return new WsDiscoveryService(ports, scopes, xaddrs);
    }
    
    /**
     * Creates a new WsDiscoveryFinder instance. WsDiscoverFinder can be used
     * by clients to discover and search for services.
     * @return New instance of WsDiscoveryFinder.
     * @throws WsDiscoveryNetworkException 
     */
    public static WsDiscoveryFinder createFinder() throws WsDiscoveryNetworkException {
        return new WsDiscoveryFinder();
    }
    
    /**
     * Gets the correct MatchBy object from a JAXB ScopesType. If no matcher is specified in 
     * <code>scopes</code> or if <code>scopes.getMatchBy()</code> is null, 
     * {@link WsDiscoveryConstants#defaultMatchBy} is returned.
     * @param scopes ScopesType
     * @return Matcher specified in <code>scopes</code>.
     */
    public static MatchBy getMatcher(ScopesType scopes) {
        MatchBy res = WsDiscoveryConstants.defaultMatchBy;
        
        if ((scopes != null) && (scopes.getMatchBy() != null))
            for (MatchBy m : MatchBy.values())
                if (m.toString().equals(scopes.getMatchBy())) {
                    res = m;
                    break;
                }
        
        return res;
    }
}
