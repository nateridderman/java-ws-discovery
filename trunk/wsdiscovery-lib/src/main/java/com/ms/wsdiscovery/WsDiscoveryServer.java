/*
WsDiscoveryServer.java

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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.network.DispatchThread;
import com.ms.wsdiscovery.network.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;

/** 
 * WS-Discovery server thread. 
 * 
 * @author Magnus Skjegstad
 */

public class WsDiscoveryServer extends DispatchThread {

    /**
     * Constructor
     * @throws WsDiscoveryNetworkException 
     */
    public WsDiscoveryServer() throws WsDiscoveryNetworkException {
        super();
    }
    
    /**
     * Publish the specified WS-Discovery service. Sends an initial Hello-packet
     * and adds the service to the local service directory. 
     * 
     * @param service Service to publish.
     * @throws WsDiscoveryServiceDirectoryException on failure to store service in the service directory.
     */
    public void publish(WsDiscoveryService service) throws WsDiscoveryServiceDirectoryException {
        getLocalServices().store(service);
        synchronized (this) {
            sendHello(service);
        }
    }
    
    /**
     * Publish the specified JAX-WS service. See {@link WsDiscoveryBuilder#createService}
     * for details on how the service is converted to a WS-Discovery service.
     * <p>
     * Sends an initial Hello-packet and adds the service to the local service directory.
     * @param JAXWSService
     * @throws WsDiscoveryServiceDirectoryException on failure to store service in the service directory.
     */
    public void publish(Service JAXWSService) throws WsDiscoveryServiceDirectoryException {
        publish(WsDiscoveryBuilder.createService(JAXWSService));
    }
    
    /**
     * Unpublish a service with the specified endpoint address. Sends a Bye-packet
     * and removes the service from the local service directory.
     * @param address Endpoint address.
     */
    public void unpublish(String address) {
        unpublish(getLocalServices().findService(address));        
    }
    
    /**
     * Unpublish a service. Sends a Bye-packet and removes the service from 
     * the local service directory.
     * @param service Service to unpublish.
     */
    public void unpublish(WsDiscoveryService service) {
        synchronized (this) {
            sendBye(service);
        }
        getLocalServices().remove(service);
    }
    
    /**
     * Resolve XAddrs/invocation URI for the specified service. 
     * 
     * @param service Service to resolve.
     */
    public void resolve(WsDiscoveryService service) {
        synchronized (this) {
            sendResolve(service);
        }
    }
    
    /**
     * Sends an empty probe. Matches all services.
     * <p>
     * Returns immediately. The remote service directory must be checked for
     * new services manually by the caller.  
     */
    public void probe() {
        synchronized (this) {
            sendProbe();
        }
    }
    
    /**
     * Probes for a service with the specified portTypes and scopes. Scopes 
     * are matched by the matching algorithm specified in <code>matchBy</code>. 
     * <p>
     * Returns immediately. The remote service directory must be checked for
     * new services manually by the caller.  
     * @param types portTypes to match. <code>null</code> is all portTypes.
     * @param scopes scopes to match. <code>null</code> is all scopes.
     * @param matchBy Matching algorithm to use when matching scopes. 
     * <code>null</code> uses default from {@link WsDiscoveryConstants}
     */
    public void probe(List<QName> types, List<URI> scopes, MatchBy matchBy) {
        synchronized (this) {
            sendProbe(types, scopes, matchBy);
        }    
    }
    
    /**
     * Probes for a service with the specified portType and scope. Scopes 
     * are matched by the matching algorithm specified in <code>matchBy</code>. 
     * <p>
     * Returns immediately. The remote service directory must be checked for
     * new services manually by the caller.  
     * @param portType portType to match. <code>null</code> is all portTypes.
     * @param scope scope to match. <code>null</code> is all scopes.
     * @param matchBy Matching algorithm to use when matching scopes. 
     * <code>null</code> uses default from {@link WsDiscoveryConstants}
     */
    public void probe(QName portType, URI scope, MatchBy matchBy) {
        List<QName> ports = new ArrayList<QName>();        
        ports.add(portType);
        List<URI> scopes = new ArrayList<URI>(); 
        scopes.add(scope);        

        probe(ports, scopes, matchBy);
    }
    
    /**
     * Turns this WS-Discovery instance into a proxy server. See the WS-Discovery 
     * specification for details.
     * <p>
     * A call to this method will result in a proxy service being added to the
     * local service directory. This service will send suppression messages to
     * clients when they send multicast messages, hopefully forcing them to
     * send unicast directly to the proxy server instead. 
     * @throws WsDiscoveryException 
     */
    public void enableProxyMode() throws WsDiscoveryException {
        synchronized (this) {
            try {
                enableProxyAnnouncements();
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryException("Unable to initialize proxy service.");
            }
        }
    }
    
    /**
     * Disable proxy server mode. See the WS-Discovery specification for details.
     * <p>
     * Removes the proxy service from the local service directory and resumes 
     * normal client behaviour. 
     */
    public void disableProxyMode() {
        synchronized (this) {
            disableProxyAnnouncements();
        }
    }
    
    /**
     * Unpublish all services and stop.
     */
    @Override
    public void done() {
        for (int i = 0; i < getLocalServices().size(); i++)
            unpublish(getLocalServices().get(i));
        super.done(); 
    }
}
