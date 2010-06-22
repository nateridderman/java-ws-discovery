/*
IWsDiscoveryServer.java

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
package com.ms.wsdiscovery.interfaces;

import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * Interface implemented by all WsDiscoveryServer versions.
 *
 * @author Magnus Skjegstad
 */
public interface IWsDiscoveryServer {

    /**
     * Disable proxy server mode. See the WS-Discovery specification for details.
     * <p>
     * Removes the proxy service from the local service directory and resumes
     * normal client behaviour.
     */
    void disableProxyMode() throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

    /**
     * Unpublish all services and stop.
     * @throws WsDiscoveryException on error.
     */
    void done() throws WsDiscoveryException;

    /**
     * Start WS-Discovery listening thread and transport layer.
     */
    public void start();

    /**
     * Returns true when WS-Discovery is running, i.e. when start()
     * has been called successfully. Call done() to terminate the threads.
     * @return true when WS-Discovery is running.
     */
    public boolean isRunning();

    /**
     * This call has been deprecated. Use {@link #isRunning()} instead.
     * @return true when WS-Discovery is running.
     */
    @Deprecated
    public boolean isAlive();

    /**
     * Turns this WS-Discovery instance into a proxy server. See the WS-Discovery
     * specification for details.
     * <p>
     * A call to this method will result in a proxy service being added to the
     * local service directory. This service will send suppression messages to
     * clients when they send multicast messages, hopefully forcing them to
     * send unicast directly to the proxy server instead.
     */
    void enableProxyMode() throws WsDiscoveryNetworkException, WsDiscoveryXMLException;

    /**
     * Sends an empty probe. Matches all services.
     * <p>
     * Returns immediately. The remote service directory must be checked for
     * new services manually by the caller.
     */
    void probe() throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

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
    void probe(List<QName> types, List<URI> scopes, MatchBy matchBy) throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

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
    void probe(QName portType, URI scope, MatchBy matchBy) throws WsDiscoveryXMLException, WsDiscoveryNetworkException;

    /**
     * Publish the specified WS-Discovery service. Sends an initial Hello-packet
     * and adds the service to the local service directory.
     *
     * @param service Service to publish.
     * @throws WsDiscoveryServiceDirectoryException on failure to store service in the service directory.
     */
    void publish(WsDiscoveryService service) throws WsDiscoveryServiceDirectoryException, WsDiscoveryXMLException, WsDiscoveryNetworkException;

    /**
     * Publish the specified JAX-WS service. See {@link WsDiscoveryBuilder#createService}
     * for details on how the service is converted to a WS-Discovery service.
     * <p>
     * Sends an initial Hello-packet and adds the service to the local service directory.
     * @param JAXWSService
     * @throws WsDiscoveryServiceDirectoryException on failure to store service in the service directory.
     */
    void publish(Service JAXWSService) throws WsDiscoveryServiceDirectoryException, WsDiscoveryXMLException, WsDiscoveryNetworkException;

    /**
     * Resolve XAddrs/invocation URI for the specified service.
     *
     * @param service Service to resolve.
     */
    void resolve(WsDiscoveryService service) throws WsDiscoveryXMLException, WsDiscoveryNetworkException;

    /**
     * Unpublish a service with the specified endpoint address. Sends a Bye-packet
     * and removes the service from the local service directory.
     * @param address Endpoint address.
     */
    void unpublish(String address) throws WsDiscoveryNetworkException, WsDiscoveryXMLException;

    /**
     * Unpublish a service. Sends a Bye-packet and removes the service from
     * the local service directory.
     * @param service Service to unpublish.
     */
    void unpublish(WsDiscoveryService service) throws WsDiscoveryNetworkException, WsDiscoveryXMLException;

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
    void useServiceStore(IWsDiscoveryServiceCollection newServiceStore);

    /**
     * Get remote services. Services can be discovered with <code>sendProbe</code>
     * @return Service directory containing remote services.
     */
    public IWsDiscoveryServiceDirectory getServiceDirectory();

    /**
     * Get local services.
     * @return Service directory containing local services.
     */
    public IWsDiscoveryServiceDirectory getLocalServices();

    /**
     * Determine if this node is a WS-Discovery proxy.
     * @return Returns true when this node acts as a WS-Discovery proxy.
     */
    public boolean isProxy();

    /**
     * Determine if this node is using a proxy server.
     * @return Returns true when this node is using a proxy server.
     */
    public boolean isUsingProxy();

    /**
     * If this node is using a proxy, this call returns the ip address
     * of the proxy node. Returns null if a proxy server is not used.
     * @return IP address of proxy server.
     */
    public InetSocketAddress getProxyServer();

    /**
     * Returns the multicast address used by this WS-Discovery instance.
     * @return multicast address.
     */
    public InetAddress getMulticastAddress();

    /**
     * Returns the multicast port this WS-Discovery instance listens to.
     * @return multicast port.
     */
    public int getMulticastPort();

    /**
     * Returns the unicast port this WS-Discovery instance listens to.
     * @return
     */
    public int getUnicastPort();

    /**
     * Returns the matcher that is used to match services by default.
     */
    public MatchBy getDefaultMatchBy();
}
