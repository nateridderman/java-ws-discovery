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
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.logger.WsdLogger;
import com.ms.wsdiscovery.network.transport.ITransportType;
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
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.ms.wsdiscovery.xml.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.xml.soap.WsdSOAPMessage;
import com.ms.wsdiscovery.xml.soap.WsdSOAPMessageBuilder;
import com.ms.wsdiscovery.xml.jaxb_generated.AttributedURI;
import com.ms.wsdiscovery.xml.jaxb_generated.Relationship;

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
    private WsDiscoveryServiceDirectory localServices = new WsDiscoveryServiceDirectory(); // Service directory containing published local services 
    private WsDiscoveryServiceDirectory remoteServices = new WsDiscoveryServiceDirectory(); // Service directory containing discovered remote services
    private WsdSOAPMessageBuilder soapBuilder = WsDiscoveryConstants.SOAPBUILDER; // Helper functions for building SOAP-messages
    private WsdXMLBuilder jaxbBuilder = WsDiscoveryConstants.XMLBUILDER; // Helper functions for building XML with JAXB
    private ArrayList<AttributedURI> messagesReceived = new ArrayList<AttributedURI>(); // list of received message IDs
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
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException 
     * Thrown when unable to instantiate the transport layer.
     */
    public DispatchThread() throws WsDiscoveryNetworkException {
        this.setDaemon(true);        
        // Create a proxy service description that can be added to the directory later if a proxy is enabled on this host               
        try {
            localProxyService = WsDiscoveryBuilder.createService(WsDiscoveryConstants.proxyPortType,
                                                                    WsDiscoveryConstants.proxyScope,
                                                                    "http://" + 
                                                                    InetAddress.getLocalHost().getHostAddress() + 
                                                                    ":" + 
                                                                    WsDiscoveryConstants.multicastPort + 
                                                                    "/" + 
                                                                    WsDiscoveryConstants.proxyPortType.getLocalPart());
        } catch (UnknownHostException ex) {
            localProxyService = null;
            logger.warning("Unable to get local IP address. This node can not function as a proxy server.");            
        }
        try {
            this.transport = WsDiscoveryConstants.transportType.newInstance();
        } catch (IllegalAccessException ex) {
            throw new WsDiscoveryNetworkException("Unable to instantiate transport type: " + ex.toString());
        } catch (InstantiationException ex) {
            throw new WsDiscoveryNetworkException("Unable to instantiate transport type: " + ex.toString());
        }                
    }
    
    /**
     * Get local services.
     * @return Service directory containing local services.
     */
    public WsDiscoveryServiceDirectory getLocalServices() {
        return localServices;
    }

    /**
     * Get remote services. Services can be discovered with <code>sendProbe</code>
     * @return Service directory containing remote services.
     */
    public WsDiscoveryServiceDirectory getRemoteServices() {
        return remoteServices; 
    }
    
    /**
     * Check if the AppSequence in the soap-messages is already received (used to avoid duplicates)
     * @param soap A SOAP-message
     * @return Whether a message with the same MessageID as the message in <code>soap</code> has been received earlier.
     */ 
    private boolean isAlreadyReceived(WsdSOAPMessage soap) {
        // TODO Eats memory
        if (soap.getWsaMessageId() == null)
            return true;
        
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
     * @throws java.lang.Exception
     */
    private void registerReceived(WsdSOAPMessage soap) throws WsDiscoveryNetworkException {
        // TODO Eats memory. Shoudl discard oldest entries.
        if (soap.getWsaMessageId() == null)
            throw new WsDiscoveryNetworkException("MessageID was null");
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
        if (useProxy) 
            transport.send(new NetworkMessage(probe.toString(), null, 0, useProxyAddress, useProxyPort)); // Unicast to proxy
        else
            transport.send(new NetworkMessage(probe));  // Multicast
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
        localServices.store(localProxyService);
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
        
        if (m.getJAXBBody() instanceof HelloType) {
            logger.finer("Hello received.");
            
            HelloType hello = (HelloType)m.getJAXBBody();

            // if RelatesTo is set and @Relationship="Suppression", use sender as proxy.
            if (m.getWsaRelatesTo() != null) 
                if (m.getWsaRelatesTo().getRelationshipType() != null)
                    if (m.getWsaRelatesTo().getRelationshipType().equals(WsDiscoveryConstants.defaultProxyRelatesToRelationship)) {
                        logger.finer("Received proxy suppression.");
                        try {
                            // Find proxy address from hello body                            
                            URI addr = URI.create(hello.getXAddrs().get(0));
                            useProxyAddress = InetAddress.getByName((addr.getHost()));
                            if (addr.getPort() == -1)
                                useProxyPort = WsDiscoveryConstants.multicastPort;
                            else
                                useProxyPort = addr.getPort();                            
                            
                            remoteProxyService = new WsDiscoveryService(hello);
                            
                            useProxy = true;
                            logger.finer("Using proxy server at " + useProxyAddress.toString() + ", port " + useProxyPort);
                        } catch (Exception ex) {
                            logger.finer("Proxy suppression contained invalid data. Aborted (not using proxy).");
                            ex.printStackTrace();
                            useProxy = false;
                        }
                    }
            
            // Store service information
            try {
                remoteServices.store(hello);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store service received in Hello-message.");
            }
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvHello()");
    }
    
    /**
     * Recive ProbeMatch message.
     * 
     * @param m SOAP message with a ProbeMatch.
     * @throws WsDiscoveryNetworkException if m is not an instance of ProbeMatchesType.
     */
    private void recvProbeMatches(WsdSOAPMessage m) 
            throws WsDiscoveryNetworkException {
        
        if (m.getJAXBBody() instanceof ProbeMatchesType) {
            logger.finer("ProbeMatches received.");
            try {
                remoteServices.store((ProbeMatchesType)m.getJAXBBody());
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store remote service.");
            }
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvProbeMatches()");
    }
    
    /**
     * Receive ResolveMatches.
     * @param m SOAP message.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ResolveMatchesType.
     */
    private void recvResolveMatches(WsdSOAPMessage m) 
            throws WsDiscoveryNetworkException {
        
        if (m.getJAXBBody() instanceof ResolveMatchesType) {
            logger.finer("ResolveMatches received.");
            try {
                remoteServices.store((ResolveMatchesType) m.getJAXBBody());
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
        if (m.getJAXBBody() instanceof ByeType) {
            logger.finer("Bye received.");
            if (useProxy) // Check if the proxy server sent bye
                if (((ByeType)m.getJAXBBody()).getEndpointReference().getAddress().getValue().equals(remoteProxyService.getEndpointReferenceAddress()))  {
                    logger.finer("Proxy service left the network. Disabling proxy.");
                    useProxy = false; // Stop using the proxy server
                }
            remoteServices.remove((ByeType)m.getJAXBBody());
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
        
        if (m.getJAXBBody() instanceof ResolveType) {
            logger.finer("Received Resolve for service " + 
                    ((ResolveType)m.getJAXBBody()).getEndpointReference().getAddress().getValue());
           
            // See if we have the service
            // If we are running in proxy mode, search local services first, then remote
            int i = localServices.findServiceIndex(((ResolveType)m.getJAXBBody()).getEndpointReference());
            if (i > -1) {
                logger.finer("Service found (local). Sending resolve match.");

                // Service found, send resolve match
                sendResolveMatch(localServices.get(i), m, 
                        originalMessage.getSrcAddress(), originalMessage.getSrcPort());
            } else {
                if (isProxy) { // We are running in proxy mode. Check remote services as well
                    int j = remoteServices.findServiceIndex(((ResolveType)m.getJAXBBody()).getEndpointReference());
                    if (j > -1) {
                        logger.finer("Service found (remote). Sending resolve match.");
                        sendResolveMatch(remoteServices.get(i), m, 
                                originalMessage.getSrcAddress(), originalMessage.getSrcPort());
                    } else // Remote service was not found
                        logger.finer("Service not found remote or locally (in proxy mode). No reply sent.");
                } else // If in normal mode, just log failure.
                    logger.finer("Service not found locally. No reply sent.");
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
        
        if (m.getJAXBBody() instanceof ProbeType) {
            
            logger.finer("Probe received. Running matchBy...");
            ProbeType probe = (ProbeType)m.getJAXBBody();
            
            // Match local services first
            WsDiscoveryServiceDirectory totalMatches;
            try {
                totalMatches = localServices.matchBy(probe.getTypes(), probe.getScopes());
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to get MatchBy-results for received Probe-message.");
            }
            
            if (isProxy) // If we are in proxy mode, search remote services as well
                try {
                    totalMatches.addAll(remoteServices.matchBy(probe.getTypes(), probe.getScopes()));
                } catch (WsDiscoveryServiceDirectoryException ex) {
                    throw new WsDiscoveryNetworkException("Unable to search remote services for match.");
                }
            
            if (totalMatches.size() > 0) {
                logger.finer("Probe match sent.");
                sendProbeMatch(totalMatches, m, originalMessage.getSrcAddress(), originalMessage.getSrcPort());
            } else
                logger.finer("Probe match NOT found. No reply sent.");
        } else
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvProbe()");
    }
    
    /**
     * Send Resolve. Only sent every 10 seconds for each service to avoid network floods.
     * 
     * @param service Service to resolve
     */
    protected void sendResolve(WsDiscoveryService service) {
        // Return if less than 10 seconds from last time we tried to resolve this service
        if (service.getTriedToResolve() != null)
            if (service.getTriedToResolve().getTime()+10000 > (new Date()).getTime()) {
                logger.finer("sendResolve() called too often for service " + service.getEndpointReferenceAddress());
                return;
            }
        
        logger.finer("Sent Resolve for service " + service.getEndpointReferenceAddress());
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
        
        logger.finer("Sending proxy announce to " + originalMessage.getSrcAddress().getHostName());
        WsdSOAPMessage<HelloType> m = soapBuilder.createWsdSOAPMessageHello(localProxyService);
        Relationship r = new Relationship();
        r.setValue(relatesToMessage.getWsaMessageId().getValue());
        m.setWsaRelatesTo(r);
        // Set relationShipType to suppression (client should suppress multicast and use unicast to proxy instead)
        r.setRelationshipType(WsDiscoveryConstants.defaultProxyRelatesToRelationship);
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
        logger.finer("sendHello()");
    }
    
    /**
     * Send Bye.
     * 
     * @param service Service that says Bye.
     */
    protected void sendBye(WsDiscoveryService service) {
        WsdSOAPMessage<ByeType> m = soapBuilder.createWsdSOAPMessageBye(service);
        transport.send(new NetworkMessage(m));
        logger.finer("sendBye()");
    }
    
    /**
     * Send ResolveMatch. Will only be sent once every 10 seconds for each 
     * service to avoid network floods.
     * 
     * @param matchedService The service that matched the Resolve.
     * @param originalMessage Original message as received from transport layer.
     * @param dstAddress Destination address.
     * @param dstPort Destination port.
     */
    private void sendResolveMatch(WsDiscoveryService matchedService, 
            WsdSOAPMessage originalMessage, InetAddress dstAddress, int dstPort) {
        // Return if less than 10 seconds since we sent a resolve match for this service to the requesting host
        if (matchedService.getTriedToResolve() != null)
            if (matchedService.getTriedToResolve().getTime() + 10000 > (new Date()).getTime()) {
                logger.finer("sendResolveMatch() called too often for service " + matchedService.getEndpointReferenceAddress());
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
        
        ResolveMatchType match = new ResolveMatchType();
               
        match.setEndpointReference(matchedService.createEndpointReferenceObject());
        match.setMetadataVersion(matchedService.getMetadataVersion());
        match.setScopes(matchedService.createScopesObject());
        match.getTypes().addAll(matchedService.getTypes());
        match.getXAddrs().addAll(matchedService.getXAddrs());

        m.getJAXBBody().setResolveMatch(match);
                        
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
    private void sendProbeMatch(WsDiscoveryServiceDirectory matches, 
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
        
        for (int i = 0; i < matches.size(); i++) {
            WsDiscoveryService service = matches.get(i);
            ProbeMatchType match = new ProbeMatchType();
            
            match.setEndpointReference(service.createEndpointReferenceObject());
            match.setMetadataVersion(service.getMetadataVersion());
            match.setScopes(service.createScopesObject());
            match.getTypes().addAll(service.getTypes());
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
        boolean isMulticast = message.getDstAddress().equals(WsDiscoveryConstants.multicastAddress);
        
        // Parse message
        WsdSOAPMessage m;
        try {
            m = soapBuilder.createWsdSOAPMessage(message.getMessage());
        } catch (WsDiscoveryXMLException ex) {
            throw new WsDiscoveryNetworkException("Unable to create WS-Discovery SOAP message.");
        }             
        
        // Return if the message was from us
        if ((m.getWsdInstanceId() == WsDiscoveryConstants.instanceId) &&
                ((m.getWsdSequenceId() == null) ||
                (m.getWsdSequenceId().equals("urn:uuid:" + WsDiscoveryConstants.sequenceId)))) { 
            return;
        }
        
        // Return if the message has already been handled
        if (isAlreadyReceived(m)) {
            logger.finest("Dup discarded! MessageID: " + m.getWsaMessageId().getValue() + " " + message.toString());
            return;
        }

        // HELLO
        if (m.getJAXBBody() instanceof HelloType) {
            if (isMulticast && useProxy) // Multicast Hello-packets are ignored when using proxy
                return;
            recvHello(m); // Add new service
        } else        
        // PROBE
        if (m.getJAXBBody() instanceof ProbeType) {
            if (isMulticast && isProxy) // Respond to multicast probes with unicast proxy announcement
                sendProxyAnnounce(m, message); 
            recvProbe(m, message);
        } else
        // PROBE MATCHES
        if (m.getJAXBBody() instanceof ProbeMatchesType) {
            recvProbeMatches(m); // Add services from probe matches
        } else
        // RESOLVE    
        if (m.getJAXBBody() instanceof ResolveType) {
            if (isMulticast && isProxy) // Respond to multicast resolves with unicast proxy announcement
                sendProxyAnnounce(m, message);
            recvResolve(m, message); // Send resolve match
        } else
        // RESOLVE MATCHES    
        if (m.getJAXBBody() instanceof ResolveMatchesType) {
            recvResolveMatches(m); // Add updates from resolve matches
        } else
        // BYE
        if (m.getJAXBBody() instanceof ByeType) {
            if (isMulticast && useProxy) // Multicast Bye-packets are ignored when using proxy
                return;
            recvBye(m); // Remove service         
        } else
            throw new WsDiscoveryNetworkException("Don't know how to handle message " + message.toString());
        
        // Mark the message ID as received.
        registerReceived(m);        
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
    public void done() {
        threadDone = true;
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
            isRunning = false;
        }       
        logger.finer("Stopped " + getName());
        
        // Notify waiting threads that we stopped
        synchronized(this) {
            notifyAll();
        }
    }
}
