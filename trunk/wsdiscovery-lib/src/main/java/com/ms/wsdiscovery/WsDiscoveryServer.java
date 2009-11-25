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

import com.ms.wsdiscovery.interfaces.IWsDiscoveryServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.interfaces.IWsDiscoveryDispatchThread;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import java.net.InetAddress;

/** 
 * WS-Discovery server thread. 
 * 
 * @author Magnus Skjegstad
 */

public class WsDiscoveryServer implements IWsDiscoveryServer {

    protected IWsDiscoveryDispatchThread dispatchThread;

    /**
     * Constructor
     * @throws WsDiscoveryException
     */
    public WsDiscoveryServer() throws WsDiscoveryException {
        try {
            dispatchThread = WsDiscoveryConstants.defaultNsDiscovery.getNewDispatchThreadInstance();
        } catch (Exception ex) {
            throw new WsDiscoveryException("Unable to create WS-Discovery dispatch thread instance.", ex);
        }
    }
    
    /**
     * Publish the specified WS-Discovery service. Sends an initial Hello-packet
     * and adds the service to the local service directory. 
     * 
     * @param service Service to publish.
     * @throws WsDiscoveryServiceDirectoryException on failure to store service in the service directory.
     */
    public void publish(WsDiscoveryService service) throws WsDiscoveryServiceDirectoryException, WsDiscoveryXMLException, WsDiscoveryNetworkException  {
        dispatchThread.getLocalServices().store(service);
        dispatchThread.getServiceDirectory().store(service);
        synchronized (this) {
            dispatchThread.sendHello(service);
        }
    }
    
    /**
     * Publish the specified JAX-WS service. See {@link WsDiscoveryFactory#createService}
     * for details on how the service is converted to a WS-Discovery service.
     * <p>
     * Sends an initial Hello-packet and adds the service to the local service directory.
     * @param JAXWSService
     * @throws WsDiscoveryServiceDirectoryException on failure to store service in the service directory.
     * @throws WsDiscoveryXMLException
     * @throws WsDiscoveryNetworkException
     */
    public void publish(Service JAXWSService) throws WsDiscoveryServiceDirectoryException, WsDiscoveryXMLException, WsDiscoveryNetworkException {
        publish(WsDiscoveryFactory.createService(JAXWSService));
    }
    
    /**
     * Unpublish a service with the specified endpoint address. Sends a Bye-packet
     * and removes the service from the local service directory.
     * @param address Endpoint address.
     * @throws WsDiscoveryNetworkException
     * @throws WsDiscoveryXMLException
     */
    public void unpublish(String address) throws WsDiscoveryNetworkException, WsDiscoveryXMLException {
        unpublish(dispatchThread.getLocalServices().findService(address));
    }
    
    /**
     * Unpublish a service. Sends a Bye-packet and removes the service from 
     * the local service directory.
     * @param service Service to unpublish.
     */
    public void unpublish(WsDiscoveryService service) throws WsDiscoveryNetworkException, WsDiscoveryXMLException {
        dispatchThread.sendBye(service);
        dispatchThread.getLocalServices().remove(service);
        dispatchThread.getServiceDirectory().remove(service);
    }
    
    /**
     * Resolve XAddrs/invocation URI for the specified service. 
     * 
     * @param service Service to resolve.
     */
    public void resolve(WsDiscoveryService service) throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        synchronized (this) {
            dispatchThread.sendResolve(service);
        }
    }
    
    /**
     * Sends an empty probe. Matches all services.
     * <p>
     * Returns immediately. The remote service directory must be checked for
     * new services manually by the caller.  
     * @throws WsDiscoveryXMLException
     * @throws WsDiscoveryNetworkException 
     */
    public void probe() throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        synchronized (this) {
            dispatchThread.sendProbe();
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
    public void probe(List<QName> types, List<URI> scopes, MatchBy matchBy) throws WsDiscoveryXMLException, WsDiscoveryNetworkException{
        synchronized (this) {
            dispatchThread.sendProbe(types, scopes, matchBy);
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
    public void probe(QName portType, URI scope, MatchBy matchBy) throws WsDiscoveryXMLException, WsDiscoveryNetworkException{
        List<QName> ports = null;
        if (portType != null) {
            ports = new ArrayList<QName>();
            ports.add(portType);
        }

        List<URI> scopes = null;
        if (scope != null) {
            scopes = new ArrayList<URI>();
            scopes.add(scope);
        }

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
     * @throws WsDiscoveryXMLException 
     * @throws WsDiscoveryNetworkException
     */   
    public void enableProxyMode() throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        synchronized (this) {
            try {
                dispatchThread.enableProxyAnnouncements();
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to initialize proxy service.", ex);
            }
        }
    }
    
    /**
     * Disable proxy server mode. See the WS-Discovery specification for details.
     * <p>
     * Removes the proxy service from the local service directory and resumes 
     * normal client behaviour. 
     * @throws WsDiscoveryXMLException 
     * @throws WsDiscoveryNetworkException
     */
    public void disableProxyMode() throws WsDiscoveryXMLException, WsDiscoveryNetworkException{
        synchronized (this) {
            dispatchThread.disableProxyAnnouncements();
        }
    }

    /**
     * Use the specified service collection as storage for the service directory.
     * The IWsDiscoveryServiceCollection implementation does not need to be thread safe, but after it has
     * been passed as a parameter to this method, the collection should not
     * be used by other threads.<p/>
     * This call has been implemented to allow for persistent storage of the service directory, e.g.
     * when running in proxy mode.
     *
     * @param newServiceStore Implementation of IWsDiscoveryServiceCollection to use as service directory.
     */
    public void useServiceStore(IWsDiscoveryServiceCollection newServiceStore) {
        dispatchThread.useServiceStore(newServiceStore);
    }
    
    /**
     * Unpublish all services and stop.
     * @throws WsDiscoveryException
     */
    @Override
    public void done() throws WsDiscoveryException {
        try {
            for (WsDiscoveryService service : dispatchThread.getLocalServices().matchAll())
                unpublish(service);
        } catch (WsDiscoveryServiceDirectoryException ex) {
            throw new WsDiscoveryException("Unable to unpublish all services.", ex);
        }
        dispatchThread.done();
    }

    /**
     * Get remote services. Services can be discovered with <code>sendProbe</code>
     * @return Service directory containing remote services.
     */
    public IWsDiscoveryServiceDirectory getServiceDirectory() {
        return dispatchThread.getServiceDirectory();
    }

    /**
     * Get local services.
     * @return Service directory containing local services.
     */
    public IWsDiscoveryServiceDirectory getLocalServices() {
        return dispatchThread.getLocalServices();
    }

    /**
     *
     */
    public void start() {
        dispatchThread.start();
    }

    /**
     *
     * @return
     */
    public boolean isRunning() {
        return dispatchThread.isRunning();
    }

    /**
     *
     * @return
     * @deprecated
     */
    @Deprecated
    public boolean isAlive() {
        return this.isRunning();
    }

    /**
     * Determine if this node is a WS-Discovery proxy.
     * @return Returns true when this node acts as a WS-Discovery proxy.
     */
    public boolean isProxy() {
        return dispatchThread.isProxy();
    }

    /**
     * Returns true when this WS-Discovery instance is using a proxy server.
     * @return true when a proxy server is used.
     */
    public boolean isUsingProxy() {
        return dispatchThread.isUsingProxy();
    }

    /**
     * Gets the address and port of the proxy server that is currently in use.
     * @return address of proxy server.
     */
    public InetSocketAddress getProxyServer() {
        return dispatchThread.getProxyServer();
    }

    /**
     * Returns the multicast address used by this WS-Discovery instance.
     * @return multicast address.
     */
    public InetAddress getMulticastAddress() {
        return dispatchThread.getSOAPOverUDPInstance().getTransport().getMulticastAddress();
    }

    /**
     * Returns the multicast port this WS-Discovery instance listens to.
     * @return multicast port.
     */
    public int getMulticastPort() {
        return dispatchThread.getSOAPOverUDPInstance().getTransport().getMulticastPort();
    }

    /**
     * Returns the unicast port this WS-Discovery instance listens to.
     * @return
     */
    public int getUnicastPort() {
        return dispatchThread.getSOAPOverUDPInstance().getTransport().getUnicastPort();
    }
}
