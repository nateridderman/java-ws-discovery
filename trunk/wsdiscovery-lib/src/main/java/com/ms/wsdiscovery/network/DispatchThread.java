/*
DispatchThread.java

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
package com.ms.wsdiscovery.network;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.logger.WsdLogger;
import com.ms.wsdiscovery.network.transport.interfaces.ITransportType;
import com.ms.wsdiscovery.xml.WsdXMLBuilder;
import com.ms.wsdiscovery.xml.jaxb_generated.ByeType;
import com.ms.wsdiscovery.xml.jaxb_generated.HelloType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeMatchType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeType;
import com.ms.wsdiscovery.xml.jaxb_generated.ResolveMatchType;
import com.ms.wsdiscovery.xml.jaxb_generated.ResolveMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ResolveType;
import com.ms.wsdiscovery.xml.jaxb_generated.ScopesType;
import com.ms.wsdiscovery.network.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.ms.wsdiscovery.xml.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.xml.soap.WsdSOAPMessage;
import com.ms.wsdiscovery.xml.soap.WsdSOAPMessageBuilder;
import com.ms.wsdiscovery.xml.jaxb_generated.AttributedURI;
import com.ms.wsdiscovery.xml.jaxb_generated.Relationship;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Map.Entry;

/** 
 * Worker thread for WS-Discovery. Handles WS-Discovery messages received from 
 * the transport layer and maintains the service directories.
 * <p>
 * The local service directory contains services published by the current 
 * WS-Discovery instance. These services are matched against Probe-messages and 
 * when appropriate, returned in ProbeMatch-messages. 
 * <p>
 * The remote service directory contains discovered remotes services. When a
 * ProbeMatch-message is received the services are automatically added.
 * @author Magnus Skjegstad
 */
public class DispatchThread extends Thread {    
    private ITransportType transport; 
    protected WsDiscoveryServiceDirectory localServices = new WsDiscoveryServiceDirectory(); // Service directory containing published local services
    protected WsDiscoveryServiceDirectory serviceDirectory = new WsDiscoveryServiceDirectory(); // Service directory containing discovered services (including local)
    private WsdSOAPMessageBuilder soapBuilder = WsDiscoveryConstants.SOAPBUILDER; // Helper functions for building SOAP-messages
    private WsdXMLBuilder jaxbBuilder = WsDiscoveryConstants.XMLBUILDER; // Helper functions for building XML with JAXB
    private LinkedList<AttributedURI> messagesReceived = new LinkedList<AttributedURI>(); // list of received message IDs
    private WsdLogger logger = new WsdLogger(DispatchThread.class.getName());
    private boolean threadDone = false; // Thread aborts when set to true
    
    private boolean useProxy = false; // Address to proxy server. Null when disabled.
    private int useProxyPort = WsDiscoveryConstants.multicastPort; // Default the same as the multicast port
    private InetAddress useProxyAddress = null; // Address of proxy server
    private WsDiscoveryService remoteProxyService = null;
    
    private boolean isProxy = false; // TRUE when functioning as a proxy server
    private WsDiscoveryService localProxyService = null; // Must be a service description registered in localServices when isProxy is set
    private boolean isRunning = false;

    /**
     * Creates a new {@link DispatchThread} instance.
     *
     * A proxy service description will also be created, but it will not be used until
     * enableProxyAnnouncements() is called. The IP of the proxy service will be
     * enumerated from the constants given in WsDiscoveryConstants, according to
     * the following rules:<br>
     * <li>Try to use proxyAddress</li>
     * <li>If proxyAddress is null, try to use the first IP mapped to multicastInterface</li>
     * <li>If multicastInterface is null, use the first non-loopback IP-address returned from NetworkInterface.getNetworkInterfaces().</li>
     * <br>
     * 
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException 
     * Thrown when unable to instantiate the transport layer.
     */
    public DispatchThread() throws WsDiscoveryNetworkException {        
        try {
            this.transport = WsDiscoveryConstants.transportType.newInstance();
        } catch (IllegalAccessException ex) {
            throw new WsDiscoveryNetworkException("Unable to instantiate transport type: " + ex.toString());
        } catch (InstantiationException ex) {
            throw new WsDiscoveryNetworkException("Unable to instantiate transport type: " + ex.toString());
        } 

        // Create a proxy service description that can be added to the directory later if a proxy is enabled on this host
        InetAddress proxyIp = null;
        Enumeration<InetAddress> ips = null;

        if (WsDiscoveryConstants.proxyAddress != null) {
            proxyIp = WsDiscoveryConstants.proxyAddress;
        } else
        if (WsDiscoveryConstants.multicastInterface != null) {
            ips = WsDiscoveryConstants.multicastInterface.getInetAddresses();

            while ((ips != null) && (ips.hasMoreElements())) {
               proxyIp = ips.nextElement();
               logger.info("IP detected on multicastinterface: " + proxyIp.getHostAddress());
               if (proxyIp instanceof Inet4Address) // Prefer IPv4
                   break;
            }
        } else
            try {
                proxyIp = getFirstNonLoopbackAddress(true, false);
                logger.warning("Proxy address guessed as " + proxyIp.toString() + ". Set proxyAddress to override.");
            } catch (SocketException ex) {
                logger.warning("Unable to enumerate IP address for proxy service.");
            }
       
        if (proxyIp != null) {
            logger.info("Proxy-service bound to " + proxyIp.getHostAddress() + " on port " + this.transport.getUnicastPort() + " (not enabled)");

            localProxyService = WsDiscoveryBuilder.createService(WsDiscoveryConstants.proxyPortType,
                                                                    WsDiscoveryConstants.proxyScope,
                                                                    "http://" +
                                                                    proxyIp.getHostAddress() +
                                                                    ":" +
                                                                    this.transport.getUnicastPort() +
                                                                    "/" +
                                                                    WsDiscoveryConstants.proxyPortType.getLocalPart());
        }  else {
            logger.warning("Unable to assign IP-address to proxy-service. This thread may not act as a proxy server.");
            localProxyService = null;
        }

        this.setDaemon(true);

    }
    
    /**
     * Get local services.
     * @return Service directory containing local services.
     */
    public IWsDiscoveryServiceDirectory getLocalServices() {
        return localServices;
    }

    /**
     * Get remote services. Services can be discovered with <code>sendProbe</code>
     * @return Service directory containing remote services.
     */
    public IWsDiscoveryServiceDirectory getServiceDirectory() {
        return serviceDirectory;
    }
    
    /**
     * Check if the AppSequence in the soap-messages is already received (used to avoid duplicates)
     * @param soap A SOAP-message
     * @return Whether a message with the same MessageID as the message in <code>soap</code> has been received earlier.
     * @throws WsDiscoveryNetworkException if getWsaMessageId() returns null.
     */ 
    private boolean isAlreadyReceived(WsdSOAPMessage soap) throws WsDiscoveryNetworkException {
        if (soap.getWsaMessageId() == null)
            throw new WsDiscoveryNetworkException("Message ID was null.");
        
        for (AttributedURI a : messagesReceived)
            try {
                if (a.getValue().equals(soap.getWsaMessageId().getValue()))
                    return true;
            } catch (NullPointerException ex) {
                logger.finer("isAlreadyReceived() got null pointer exception");
            }
        return false;
    }
    
    /**
     * Reads AppSequenceType in <code>soap</code> and registers this messages as received. Used to avoid duplicates.
     * @param soap SOAP-message
     * @throws WsDiscoveryNetworkException if getWsaMessageId() returns null.
     */
    private void registerReceived(WsdSOAPMessage soap) throws WsDiscoveryNetworkException {
        if (soap.getWsaMessageId() == null)
            throw new WsDiscoveryNetworkException("MessageID was null");
        // TODO Use ringbuffer instead?
        // trim at 1000 entries
        while (messagesReceived.size() > 1000)
            messagesReceived.removeFirst();
        // add to end
        messagesReceived.add(soap.getWsaMessageId());
    }
    
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
    protected void sendProbe(List<QName> types, List<URI> scopes, MatchBy matchBy) {
        
        WsdSOAPMessage<ProbeType> probe = soapBuilder.createWsdSOAPMessageProbe();
                       
        logger.finer("sendProbe() Sending probe with @MatchBy="+matchBy);
        
        // Create new scope
        ScopesType scopesType = new ScopesType();
                
        // Set @MatchBy. If null it defaults to rfc2396 - see WsdConstants.matchBy* for valid options
        if (matchBy != null)
            scopesType.setMatchBy(matchBy.toString());
        
        // Place all URIs we want to search for in scopeType
        if (scopes != null) {
            for (URI u : scopes)
                scopesType.getValue().add(u.toString());        
            probe.getJAXBBody().setScopes(scopesType);
        }
        
        // Add types 
        if (types != null)
            probe.getJAXBBody().getTypes().addAll(types);                
        
        // Send packet multicast or to proxy
        if (useProxy) {
            logger.fine("Sending probe unicast to proxy at " + useProxyAddress + ":" + useProxyPort);
            transport.send(new NetworkMessage(probe.toString(), null, 0, useProxyAddress, useProxyPort)); // Unicast to proxy
        } else {
            logger.fine("Multicasting probe (not using proxy).");
            transport.send(new NetworkMessage(probe));  // Multicast
        }
    }
    
    /**
     * Enables proxy announcements. All multicast Hello-messages will be 
     * responded to with a suppression message asking the recipient to 
     * use unicast against us instead.
     * @throws WsDiscoveryServiceDirectoryException 
     */
    protected void enableProxyAnnouncements() throws WsDiscoveryServiceDirectoryException {
        if (isProxy)
            return; // Already enabled
        isProxy = true;
        if (localProxyService == null)
            throw new WsDiscoveryServiceDirectoryException("Local proxy service not available.");
        localServices.store(localProxyService);
        serviceDirectory.store(localProxyService);
        // All Hello's should now be answered by multicast suppression messages 
    }
    
    /**
     * Disable proxy announcements.
     */
    protected void disableProxyAnnouncements() {
        if (!isProxy) // Not enabled
            return;
        
        isProxy = false;
        sendBye(localProxyService);
        localServices.remove(localProxyService);
        serviceDirectory.remove(localProxyService);
    }
    
    /**
     * Sends a blank probe. Matches all services.
     */
    protected void sendProbe() {
        this.sendProbe(null, null, null);
    }
    
    /**
     * Receive Hello-message.
     * 
     * @param m SOAP message
     * @throws WsDiscoveryNetworkException if <code>m</code> is not an instance of HelloType.
     */
    private void recvHello(WsdSOAPMessage m) 
            throws WsDiscoveryNetworkException {
        logger.finer("recvHello()");

        if (m.getJAXBBody() instanceof HelloType) {            
            HelloType hello = (HelloType) m.getJAXBBody();
            if ((hello.getEndpointReference() != null) && (hello.getEndpointReference().getAddress() != null))
                logger.fine("Hello received for service " + hello.getEndpointReference().getAddress().getValue() + ", metadata version " + hello.getMetadataVersion());
            else
                logger.warning("Hello received without endpoint reference.");

            // if RelatesTo is set and @Relationship="Suppression", use sender as proxy.
            if (m.getWsaRelatesTo() != null) {
                logger.finer("wsaRelatesTo: " + m.getWsaRelatesTo().getValue());
                if (m.getWsaRelatesTo().getOtherAttributes() != null)
                    for (Entry<QName, String> e : m.getWsaRelatesTo().getOtherAttributes().entrySet())
                        logger.finer("wsaRelatesTo.otherAttributes: " + e.getKey().toString() + "=" + e.getValue());
                if (m.getWsaRelatesTo().getRelationshipType() != null) {
                    logger.finer("wsaRelatesTo.RelationshipType: " + m.getWsaRelatesTo().getRelationshipType().toString());
                    if (m.getWsaRelatesTo().getRelationshipType().equals(WsDiscoveryConstants.defaultProxyRelatesToRelationship)) {
                        logger.fine("Received proxy suppression.");
                        try {
                            // Find proxy address from hello body                            
                            URI addr = URI.create(hello.getXAddrs().get(0));
                            useProxyAddress = InetAddress.getByName((addr.getHost()));
                            if (addr.getPort() == -1) {
                                useProxyPort = WsDiscoveryConstants.multicastPort;
                            } else {
                                useProxyPort = addr.getPort();
                            }

                            remoteProxyService = new WsDiscoveryService(hello);

                            useProxy = true;
                            logger.fine("Using proxy server at " + useProxyAddress.toString() + ", port " + useProxyPort);
                        } catch (Exception ex) {
                            logger.warning("Proxy suppression received, but contained invalid data. Aborted (not using proxy).");
                            ex.printStackTrace();
                            useProxy = false;
                        }
                    }
                }
            }

            // Store service information
            try {
                serviceDirectory.store(hello);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store service received in Hello-message.");
            }
        } else {
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvHello()");
        }
    }
    
    /**
     * Recieve ProbeMatch message.
     * 
     * @param m SOAP message with a ProbeMatch.
     * @param originalMessage The original NetworkMessage.
     * @throws WsDiscoveryNetworkException if m is not an instance of ProbeMatchesType.
     */
    private void recvProbeMatches(WsdSOAPMessage m, NetworkMessage originalMessage)
            throws WsDiscoveryNetworkException {
        logger.finer("recvProbeMatches()");
        if (m.getJAXBBody() instanceof ProbeMatchesType) {
            ProbeMatchesType pmt = (ProbeMatchesType) m.getJAXBBody();
            if (pmt.getProbeMatch() != null)
                logger.fine("ProbeMatches received with " + pmt.getProbeMatch().size() + " matches from " + originalMessage.getSrcAddress() + ":" + originalMessage.getSrcPort());
            else
                logger.fine("ProbeMatches received from "+ originalMessage.getSrcAddress() + ":" + originalMessage.getSrcPort()+", but it contained no results (was null).");

            try {
                serviceDirectory.store(pmt);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store remote service.");
            }
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvProbeMatches()");
    }
    
    /**
     * Receive ResolveMatches.
     * @param m SOAP message.
     * @param originalMessage The original NetworkMessage.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ResolveMatchesType.
     */
    private void recvResolveMatches(WsdSOAPMessage m, NetworkMessage originalMessage)
            throws WsDiscoveryNetworkException {
        logger.finer("recvResolveMatches()");
        if (m.getJAXBBody() instanceof ResolveMatchesType) {
            ResolveMatchesType rmt = (ResolveMatchesType) m.getJAXBBody();
            logger.fine("ResolveMatches received for " + rmt.getResolveMatch().getEndpointReference() + " from "  + originalMessage.getSrcAddress() + ":" + originalMessage.getSrcPort());
            try {
                serviceDirectory.store(rmt);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store results from ResolveMatches-message.");
            }
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvResolveMatches()");
    }
    
    /**
     * Receive Bye.
     * 
     * @param m SOAP message.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ByeType.
     */
    private void recvBye(WsdSOAPMessage m) 
            throws WsDiscoveryNetworkException {
        logger.finer("recvBye()");
        if (m.getJAXBBody() instanceof ByeType) {
            ByeType bt = (ByeType) m.getJAXBBody();
            if ((bt.getEndpointReference() != null) && (bt.getEndpointReference().getAddress() != null))
                logger.fine("Bye received for " + bt.getEndpointReference().getAddress().getValue());
            else
                logger.warning("Bye received without endpoint reference.");
            
            if (useProxy) // Check if the proxy server sent bye
                if (bt.getEndpointReference().getAddress().getValue().equals(remoteProxyService.getEndpointReference()))  {
                    logger.fine("Proxy service left the network. Disabling proxy.");
                    useProxy = false; // Stop using the proxy server
                }
            serviceDirectory.remove((ByeType)m.getJAXBBody());
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvBye()");
    }
    
    /**
     * Receive Resolve.
     * 
     * @param m SOAP message.
     * @param originalMessage Original message as received from the transport layer.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ResolveType.
     */
    private void recvResolve(WsdSOAPMessage m, NetworkMessage originalMessage) 
            throws WsDiscoveryNetworkException {
        logger.finer("recvResolve()");
        if (m.getJAXBBody() instanceof ResolveType) {
            logger.fine("Received Resolve for service " + 
                    ((ResolveType)m.getJAXBBody()).getEndpointReference().getAddress().getValue());
           
            // See if we have the service
            // If we are running in proxy mode, search local services first, then remote
            WsDiscoveryService match = localServices.findService(((ResolveType)m.getJAXBBody()).getEndpointReference());
            if (match != null) {
                logger.fine("Service found locally. Sending resolve match.");

                // Service found, send resolve match
                sendResolveMatch(match, m,
                        originalMessage.getSrcAddress(), originalMessage.getSrcPort());
            } else {
                if (isProxy) { // We are running in proxy mode. Check full service directory
                    match = serviceDirectory.findService(((ResolveType)m.getJAXBBody()).getEndpointReference());
                    if (match != null)
                        logger.fine("Service found in service directory. Sending resolve match in proxy mode.");
                    else
                        logger.fine("Service not found. Sending empty resolve match in proxy mode.");
                    sendResolveMatch(match, m,
                                originalMessage.getSrcAddress(), originalMessage.getSrcPort());
                } else // If in normal mode, just log failure.
                    logger.fine("Service not found locally. No reply sent.");
            }
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvResolve()");
    }
    
    /**
     * Receive Probe.
     * 
     * @param m SOAP msesage.
     * @param originalMessage Original message as received from the transport layer.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ProbeType.
     */
    private void recvProbe(WsdSOAPMessage m, NetworkMessage originalMessage) 
            throws WsDiscoveryNetworkException {
        logger.finer("recvProbe()");
        
        if (m.getJAXBBody() instanceof ProbeType) {
                        
            ProbeType probe = (ProbeType)m.getJAXBBody();
            logger.fine("Probe received from " + originalMessage.getSrcAddress() + ", port " + originalMessage.getSrcPort());
            
            IWsDiscoveryServiceCollection totalMatches;

            if (!isProxy) { // Not in proxy mode; match local services only                
                try {
                    totalMatches = localServices.matchBy(probe.getTypes(), probe.getScopes());
                } catch (WsDiscoveryServiceDirectoryException ex) {
                    throw new WsDiscoveryNetworkException("Unable to get MatchBy-results for received Probe-message.");
                }
            } else { // If we are in proxy mode, search full service directory
                try {
                    totalMatches = serviceDirectory.matchBy(probe.getTypes(), probe.getScopes());
                } catch (WsDiscoveryServiceDirectoryException ex) {
                    throw new WsDiscoveryNetworkException("Unable to search remote services for match.");
                }
            }
            
            if ((totalMatches.size() > 0) || isProxy) { // Proxy MUST reply with match, even if empty
                logger.fine("ProbeMatches sent with " + totalMatches.size() + " matches to " + originalMessage.getSrcAddress() + ":" + originalMessage.getSrcPort());
                sendProbeMatch(totalMatches, m, originalMessage.getSrcAddress(), originalMessage.getSrcPort());
            } else
                logger.fine("ProbeMatches NOT found. No reply sent.");
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvProbe()");
    }
    
    /**
     * Send Resolve. Only sent every 10 seconds for each service to avoid network floods.
     * 
     * @param service Service to resolve
     */
    protected void sendResolve(WsDiscoveryService service) {
        logger.finer("sendResolve()");
        // Return if less than 10 seconds from last time we tried to resolve this service
        if (service.getTriedToResolve() != null)
            if (service.getTriedToResolve().getTime()+10000 > (new Date()).getTime()) {
                logger.finer("sendResolve() called too often for service " + service.getEndpointReference());
                return;
            }
        
        logger.finer("sendResolve() Sent Resolve for service " + service.getEndpointReference());
        // Send resolve package
        WsdSOAPMessage<ResolveType> resolve = soapBuilder.createWsdSOAPMessageResolve();
        resolve.getJAXBBody().setEndpointReference(service.createEndpointReferenceObject());
        // Send multicast in normal mode or unicast in proxy mode
        if (useProxy) // Unicast
            transport.send(new NetworkMessage(resolve.toString(), null, 0, useProxyAddress, useProxyPort));
        else // Multicast
            transport.send(new NetworkMessage(resolve));        
        
        service.setTriedToResolve(new Date());
    }
    
    /**
     * Send proxy announce / suppression message.
     * 
     * @param relatesToMessage The message ID of the message we are suppressing.
     * @param originalMessage Original message as received from the transport layer.
     */
    protected void sendProxyAnnounce(WsdSOAPMessage relatesToMessage, 
            NetworkMessage originalMessage) {
        logger.finer("sendProxyAnnounce()");

        logger.fine("Sending proxy announce to " + originalMessage.getSrcAddress() + ":" + originalMessage.getSrcPort());
        WsdSOAPMessage<HelloType> m = soapBuilder.createWsdSOAPMessageHello(localProxyService);
        
        Relationship r = new Relationship();
        r.setValue(relatesToMessage.getWsaMessageId().getValue());
        // Set relationShipType to suppression (client should suppress multicast and use unicast to proxy instead)
        r.setRelationshipType(WsDiscoveryConstants.defaultProxyRelatesToRelationship);
        
        m.setWsaRelatesTo(r);
        
        transport .send(new NetworkMessage(m.toString(), null, 0, originalMessage.getSrcAddress(), originalMessage.getSrcPort()));
    }
    
    
    /**
     * Send Hello.
     * 
     * @param service Service that says Hello.
     */
    protected void sendHello(WsDiscoveryService service) {
        WsdSOAPMessage<HelloType> m = soapBuilder.createWsdSOAPMessageHello(service);
        transport.send (new NetworkMessage(m)); // Send multicast if no address is given
        logger.finer("sendHello() called for service " + m.getJAXBBody().getEndpointReference());
    }
    
    /**
     * Send Bye.
     * 
     * @param service Service that says Bye.
     */
    protected void sendBye(WsDiscoveryService service) {
        WsdSOAPMessage<ByeType> m = soapBuilder.createWsdSOAPMessageBye(service);
        transport.send(new NetworkMessage(m));
        logger.finer("sendBye() called for service " + m.getJAXBBody().getEndpointReference());
    }
    
    /**
     * Send ResolveMatch. Will only be sent once every 10 seconds for each 
     * service to avoid network floods.
     * 
     * @param matchedService The service that matched the Resolve - may be null.
     * @param originalMessage Original message as received from transport layer.
     * @param dstAddress Destination address.
     * @param dstPort Destination port.
     */
    private void sendResolveMatch(WsDiscoveryService matchedService, 
            WsdSOAPMessage originalMessage, InetAddress dstAddress, int dstPort) {
        // Return if less than 10 seconds since we sent a resolve match for this service to the requesting host
        if ((matchedService != null) && (matchedService.getTriedToResolve() != null))
            if (matchedService.getTriedToResolve().getTime() + 10000 > (new Date()).getTime()) {
                logger.finer("sendResolveMatch() called too often for service " + matchedService.getEndpointReference());
                return;
            }
              
        // Send resolve match
        WsdSOAPMessage<ResolveMatchesType> m = soapBuilder.createWsdSOAPMessageResolveMatches();
        
        // RelatesTo must contain the original MessageID
        m.setWsaRelatesTo(jaxbBuilder.createRelationship(originalMessage.getWsaMessageId().getValue()));
        
        // Set To to ReplyTo, or the anonymous value
        if ((originalMessage.getWsaReplyTo() != null) && (originalMessage.getWsaReplyTo().getAddress() != null))
            m.setWsaTo(jaxbBuilder.createAttributedURI(originalMessage.getWsaReplyTo().getAddress().getValue()));
        else
            m.setWsaTo(WsDiscoveryConstants.anonymousTo);
        
        ResolveMatchType match = null;
        if (matchedService != null) {
            match = new ResolveMatchType();

            match.setEndpointReference(matchedService.createEndpointReferenceObject());
            match.setMetadataVersion(matchedService.getMetadataVersion());
            match.setScopes(matchedService.createScopesObject());
            match.getTypes().addAll(matchedService.getPortTypes());
            match.getXAddrs().addAll(matchedService.getXAddrs());

            m.getJAXBBody().setResolveMatch(match);
        }
                        
        // Send match to dstaddress and dstport (this is the source address and port of the host that sent the resolve-packet)
        transport.send(new NetworkMessage(m.toString(), null, 0, dstAddress, dstPort));
        
        // Store time 
        matchedService.setSentResolveMatch(dstAddress);
    }
    
    /**
     * Send unicast ProbeMatch for all services in "matches". 
     * 
     * @param matches Services to include in ProbeMatch.
     * @param originalMessage Original message as received from transport layer.
     * @param dstAddress Destination address.
     * @param dstPort Destination port.
     */
    private void sendProbeMatch(IWsDiscoveryServiceCollection matches,
            WsdSOAPMessage originalMessage, InetAddress dstAddress, int dstPort)  {        
        
        // Create probe match
        WsdSOAPMessage<ProbeMatchesType> m = soapBuilder.createWsdSOAPMessageProbeMatches();
        
        // RelatesTo must contain the original MessageID
        m.setWsaRelatesTo(jaxbBuilder.createRelationship(originalMessage.getWsaMessageId().getValue()));
        
        // Set To to ReplyTo, or the anonymous value
        if ((originalMessage.getWsaReplyTo() != null) && (originalMessage.getWsaReplyTo().getAddress() != null))
            m.setWsaTo(jaxbBuilder.createAttributedURI(originalMessage.getWsaReplyTo().getAddress().getValue()));
        else
            m.setWsaTo(WsDiscoveryConstants.anonymousTo);

        for (WsDiscoveryService service : matches) {
            ProbeMatchType match = new ProbeMatchType();
            
            match.setEndpointReference(service.createEndpointReferenceObject());
            match.setMetadataVersion(service.getMetadataVersion());
            match.setScopes(service.createScopesObject());
            match.getTypes().addAll(service.getPortTypes());
            match.getXAddrs().addAll(service.getXAddrs());
            
            m.getJAXBBody().getProbeMatch().add(match);
        }
                        
        // Send match to dstaddress and dstport (this is the source address and port of the host that sent the resolve-packet)
        transport.send(new NetworkMessage(m.toString(), null, 0, dstAddress, dstPort)); 
    }
    
    /**
     * Dispatcher. Should be called from the while-loop in run().
     * 
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException on errors.
     */
    private void dispatch() throws WsDiscoveryNetworkException {
        
        NetworkMessage message = null;
        
        try {
            message = transport.recv(1000);
        } catch (InterruptedException ex) {}
        
        if (message == null)
            return;
        
        // Was message sent multicast or unicast?
        boolean isMulticast = (message.getDstAddress().equals(WsDiscoveryConstants.multicastAddress) ||
                              (message.getDstPort() == transport.getMulticastPort()));
        
        // Parse message
        WsdSOAPMessage m;
        try {
            m = soapBuilder.createWsdSOAPMessage(message.getMessage());
        } catch (WsDiscoveryXMLException ex) {
            throw new WsDiscoveryNetworkException("Unable to create WS-Discovery SOAP message.", ex);
        }             
        
        // Return if the message was from us
        if ((m.getWsdInstanceId() == WsDiscoveryConstants.instanceId) &&
                ((m.getWsdSequenceId() == null) ||
                (m.getWsdSequenceId().equals("urn:uuid:" + WsDiscoveryConstants.sequenceId)))) {
            // TODO Shouldn't this be handled by the transport class?
            logger.finest("** Discarded message sent from us: " + m.getWsaMessageId().getValue());
            return;
        }
        
        // Return if the message has already been handled
        if (isAlreadyReceived(m)) {
            // TODO Shouldn't this be handled by the transport class?
            logger.finest("** Discarded duplicate MessageID: " + m.getWsaMessageId().getValue());
            return;
        }

        try {
            // HELLO
            if (m.getJAXBBody() instanceof HelloType) {                
                recvHello(m); // Add new service, even when using proxy
            } else
            // PROBE
            if (m.getJAXBBody() instanceof ProbeType) {
                if (isMulticast && isProxy) { // Respond to multicast probes with unicast proxy announcement
                    logger.fine("Sending proxy announce in response to multicast Probe with MessageID: " + m.getWsaMessageId().getValue());
                    sendProxyAnnounce(m, message);
                }
                recvProbe(m, message);
            } else
            // PROBE MATCHES
            if (m.getJAXBBody() instanceof ProbeMatchesType) {
                recvProbeMatches(m, message); // Add services from probe matches
            } else
            // RESOLVE
            if (m.getJAXBBody() instanceof ResolveType) {
                if (isMulticast && isProxy) { // Respond to multicast resolves with unicast proxy announcement
                    logger.fine("Sending proxy announce in response to multicast Resolve with MessageID " + m.getWsaMessageId().getValue());
                    sendProxyAnnounce(m, message);
                }
                recvResolve(m, message); // Send resolve match
            } else
            // RESOLVE MATCHES
            if (m.getJAXBBody() instanceof ResolveMatchesType) {
                recvResolveMatches(m, message); // Add updates from resolve matches
            } else
            // BYE
            if (m.getJAXBBody() instanceof ByeType) {
                recvBye(m); // Remove service
            } else
                throw new WsDiscoveryNetworkException("Don't know how to handle message " + message.toString());
        } finally {
            // Mark the message ID as received.
            registerReceived(m);
        }
    }
    
    /**
     * Are we a proxy server?
     * @return True if this instance is acting as a proxy server.
     */
    public boolean isProxy() {
        return isProxy;
    }
        
    /**
     * End main loop and stop thread.
     */
    public void done()
            throws WsDiscoveryException { // The exception is actually thrown from descendant WsDiscoveryServer, so this is a bit ugly...
        threadDone = true;
        while (isRunning)
            try {
                Thread.sleep(100); // wait for threads to shut down
            } catch (InterruptedException ex) {
                break; // if interrupted, just exit
            }
    }
    
    @Override
    public void start() {
        super.start();
        
        // Wait for thread to start
        while (!isRunning)
            synchronized(this) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    break;
                }
            }                
    }
        
    /**
     * Main loop. Starts transport layer and continues to loop over dispatch().
     */
    @Override
    public void run() {
        threadDone = false;
        
        logger.finer("Started " + getName());
        
        transport.start();
        
        isRunning = true;        
        
        // Notify waiting threads that we have started.
        synchronized(this) {
            notifyAll();
        }
        
        try {
            while (!threadDone) {
                try {
                    dispatch();
                    //resolveUnknown(); // Send resolve-packets for services with no xaddrs
                } catch (Exception ex) {
                    logger.severe(ex.getMessage());
                    ex.printStackTrace();
                }   
            }
        } finally {        
            transport.done();
            logger.finer("Stopped " + getName());
            isRunning = false;
            // Notify waiting threads that we stopped
            synchronized(this) {
                notifyAll();
            }
        }       
    }

    // Method to get first non-loopback address. Used as fallback when proxy-address is not specified by user.
    // Courtesy of http://stackoverflow.com/questions/901755/how-to-get-the-ip-of-the-computer-on-linux-through-java/901943#901943
    private static InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) en.nextElement();
            for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        return addr;
                    }
                    if (addr instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        return addr;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the address of an active proxy server or null if no proxy server
     * is in use. {@see isUsingProxy}
     *
     * @return address of proxy server or null.
     */
    public InetSocketAddress getProxyServer() {
        if (isUsingProxy()) {
            return new InetSocketAddress(useProxyAddress, useProxyPort);
        } else
            return null;
    }

    /**
     * Returns true when a suppression message has been received from a remote
     * proxy server.
     *
     * @return true when using a proxy server, otherwise false.
     */
    public boolean isUsingProxy() {
        return useProxy;
    }
}
