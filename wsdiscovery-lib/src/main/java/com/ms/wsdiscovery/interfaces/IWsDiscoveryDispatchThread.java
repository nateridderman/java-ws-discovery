/*
IWsDiscoveryDispatchThread.java

Copyright (C) 2009 Magnus Skjegstad

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
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDP;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author Magnus Skjegstad
 */
public interface IWsDiscoveryDispatchThread {

    /**
     * Disable proxy announcements.
     */
    void disableProxyAnnouncements() throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

    /**
     * End main loop and stop thread.
     */
    void done() throws WsDiscoveryException;

    /**
     * Enables proxy announcements. All multicast Hello-messages will be
     * responded to with a suppression message asking the recipient to
     * use unicast against us instead.
     * @throws WsDiscoveryServiceDirectoryException
     */
    void enableProxyAnnouncements() throws WsDiscoveryServiceDirectoryException, WsDiscoveryNetworkException, WsDiscoveryXMLException;

    /**
     * Get local services.
     * @return Service directory containing local services.
     */
    IWsDiscoveryServiceDirectory getLocalServices();

    /**
     * Returns the address of an active proxy server or null if no proxy server
     * is in use. {@see isUsingProxy}
     *
     * @return address of proxy server or null.
     */
    InetSocketAddress getProxyServer();

    /**
     * Get remote services. Services can be discovered with <code>sendProbe</code>
     * @return Service directory containing remote services.
     */
    IWsDiscoveryServiceDirectory getServiceDirectory();

    /**
     * Are we a proxy server?
     * @return True if this instance is acting as a proxy server.
     */
    boolean isProxy();

    boolean isRunning();

    /**
     * Returns true when a suppression message has been received from a remote
     * proxy server.
     *
     * @return true when using a proxy server, otherwise false.
     */
    boolean isUsingProxy();

    /**
     * Main loop. Starts transport layer and continues to loop over dispatch().
     */
    void run();

    /**
     * Send Bye.
     *
     * @param service Service that says Bye.
     */
    void sendBye(WsDiscoveryService service) throws WsDiscoveryXMLException, WsDiscoveryNetworkException;

    /**
     * Send Hello.
     *
     * @param service Service that says Hello.
     */
    void sendHello(WsDiscoveryService service)  throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

    /**
     * <p>
     * Send Probe-packet. Probe packets are sent multicast to all listening clients. The clients
     * should respond with a unicast ProbeMatch-packet containing descriptions of services matching
     * <code>types</code> and <code>scopes</code> using the algorithm specified by <code>matchBy</code>.
     * </p><p>
     * If <code>types</code> and <code>scopes</code> are <code>null</code>, all services will be returned in the ProbeMatch.
     * </p>
     * <p>
     * For a detailed description of Probe-messages, see the WS-Discovery specification.
     * </p>
     * @param types A list of portTypes that is to be probed for. <code>null</code> matches all portTypes.
     * @param scopes A list of scopes to search within. Scopes are matched using the algorithm specified in matchBy.
     * @param matchBy Match algorithm clients should use when matching scopes. When set to <code>null</code> WsDiscoveryConstants.defaultMatchBy will be assumed. Some clients may not support all matching methods.
     */
    void sendProbe(List<QName> types, List<URI> scopes, MatchBy matchBy)  throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

    /**
     * Sends a blank probe. Matches all services.
     */
    void sendProbe()  throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

    /**
     * Send Resolve. Only sent every 10 seconds for each service to avoid network floods.
     *
     * @param service Service to resolve
     */
    void sendResolve(WsDiscoveryService service) throws WsDiscoveryXMLException, WsDiscoveryNetworkException;;

    void start();

    void useServiceStore(IWsDiscoveryServiceCollection newServiceStore);

    /**
     * Returns the SOAPOverUDP instance used by this thread.
     *
     * @return Instance of SOAPOverUDP.
     */
    ISOAPOverUDP getSOAPOverUDPInstance();

    MatchBy getDefaultMatchBy();

}
