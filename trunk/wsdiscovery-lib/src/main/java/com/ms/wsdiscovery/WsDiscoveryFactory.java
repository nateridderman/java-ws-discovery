/*
WsDiscoveryFactory.java

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

package com.ms.wsdiscovery;

import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;

/** 
 * Methods for creating common WS-Discovery objects.
 *
 * @author Magnus Skjegstad
 */
public class WsDiscoveryFactory {
    /**
     * Creates a WS-Discovery server object. The returned instance must be started 
     * before it can be used. See {@link WsDiscoveryServer} and {@link WsDiscoveryServer#start()}.
     * 
     * @return New WS-Discovery object.
     * @throws WsDiscoveryException on errors.
     */
    public static WsDiscoveryServer createServer() throws WsDiscoveryException {
        return new WsDiscoveryServer();
    }
    /**
     * Creates a named service directory object.
     * 
     * @param name Name of service directory.
     * @param defaultMatcher This matcher will be used to match service scope names when no matchers are specified.
     * 
     * @return New, empty service directory.
     */
    public static WsDiscoveryServiceDirectory createServiceDirectory(String name, MatchBy defaultMatcher) {
        return new WsDiscoveryServiceDirectory(name, defaultMatcher);
    }
    /**
     * Creates a new service directory object.
     *
     * @param defaultMatcher This matcher will be used to match service scope names when no matchers are specified.
     * 
     * @return A new, empty service directory.
     */
    public static WsDiscoveryServiceDirectory createServiceDirectory(MatchBy defaultMatcher) {
        return new WsDiscoveryServiceDirectory("", defaultMatcher);
    }
    /**
     * Creates a new WS-Discovery service description with one portType, scope and XAddr. 
     *
     * @param portType Service portType
     * @param scope Service scope
     * @param XAddr Invocation URI
     * @return A new instance of {@link WsDiscoveryService}
     */
    public static WsDiscoveryService createService(QName portType, String scope, String XAddr) {
        WsDiscoveryScopesType scopes = new WsDiscoveryScopesType(null);
        scopes.getValue().add(scope);
        return new WsDiscoveryService(portType, scopes, XAddr);
    }
    /**
     * Creates a WS-Discovery service description from a JAX-WS service description.
     * <p>
     * <li>WS-Discovery portType-field will be set to the JAX-WS service name</li>
     * <li>WS-Discovery XAddrs-field (invocation address) will be set to the JAX-WS WSDL-location.</li>
     * <li>WS-Discovery scopes will be set to the JAX-WS port types.</li>
     * 
     * @param jaxwsservice JAX-WS service description.
     * @return WS-Discovery service description.
     */
    public static WsDiscoveryService createService(Service jaxwsservice) {
        WsDiscoveryScopesType scopes = new WsDiscoveryScopesType(null);
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
     * Creates a new {@link WsDiscoveryFinder} instance. {@link WsDiscoveryFinder} can be used
     * by WS-Discovery clients to discover and search for services.
     * 
     * @return New instance of {@link WsDiscoveryFinder}.
     * @throws WsDiscoveryException if an error occured while creating the {@link WsDiscoveryFinder}-object.
     */
    public static WsDiscoveryFinder createFinder() throws WsDiscoveryException {
        return new WsDiscoveryFinder();
    }
    
    /**
     * Gets the correct MatchBy object from a string. If no matcher is found
     * the matcher specified in <code>defaultMatcher</code> is returned instead.
     * 
     * @param matcherToFind String containing the full name of the matcher.
     * @param defaultMatcher Matcher that should be returned if the matcher in <code>matcherToFind</code> is not found.
     * @return A matcher with the same name as <code>matcherToFind</code> or the matcher specified in <code>defaultMatcher</code>.
     */
    public static MatchBy getMatcher(String matcherToFind, MatchBy defaultMatcher) {
        MatchBy res = defaultMatcher;
        
        if (matcherToFind != null)
            for (MatchBy m : MatchBy.values())
                if ((m.toString()).equals(matcherToFind)) {
                    res = m;
                    break;
                }
        
        return res;
    }
}
