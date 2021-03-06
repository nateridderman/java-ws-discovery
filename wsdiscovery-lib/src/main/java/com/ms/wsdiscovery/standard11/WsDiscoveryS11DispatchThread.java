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
package com.ms.wsdiscovery.standard11;

import com.ms.wsdiscovery.common.WsDiscoveryDispatchThread;
import java.net.InetAddress;
import java.net.URI;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.datatypes.WsDiscoveryNamespaces;
import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.ms.wsdiscovery.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ByeType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.HelloType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ProbeMatchType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ProbeMatchesType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ProbeType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ResolveMatchType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ResolveMatchesType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ResolveType;
import com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.ScopesType;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryServiceDirectory;
import com.skjegstad.soapoverudp.SOAPOverUDP11;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPEndpointReferenceType;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPWsAddressingNamespaces;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import java.nio.charset.Charset;

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
public class WsDiscoveryS11DispatchThread extends WsDiscoveryDispatchThread {
    protected URI multicastTo = URI.create("urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01");
    protected String proxySuppressionRelationship = "Suppression";
    protected final MatchBy defaultMatcher = WsDiscoveryNamespaces.WS_DISCOVERY_2009_01.getDefaultMatcher();
    
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
    public WsDiscoveryS11DispatchThread() throws WsDiscoveryNetworkException {
        super();
        
        try {
            this.soapOverUDP = new SOAPOverUDP11(WsDiscoveryConstants.defaultTransportType.newInstance(), 
                WsDiscoveryConstants.defaultEncoding);
        } catch (InstantiationException ex) {
            throw new WsDiscoveryNetworkException("Unable to instantiate transport layer", ex);
        } catch (IllegalAccessException ex) {
            throw new WsDiscoveryNetworkException("Illegal Access while instantiating transport layer", ex);
        }      
        
        localServices = new WsDiscoveryServiceDirectory(defaultMatcher);
        serviceDirectory = new WsDiscoveryServiceDirectory(defaultMatcher);
        
        this.setDaemon(true);
    }
    
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
    public WsDiscoveryS11DispatchThread(ISOAPOverUDPTransport transportType, Charset encoding) throws WsDiscoveryNetworkException {
        super();
        
        this.soapOverUDP = new SOAPOverUDP11(transportType, 
            encoding);
        
        localServices = new WsDiscoveryServiceDirectory(defaultMatcher);
        serviceDirectory = new WsDiscoveryServiceDirectory(defaultMatcher);
        
        this.setDaemon(true);
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
    public void sendProbe(List<QName> types, List<URI> scopes, MatchBy matchBy) throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        WsDiscoveryS11SOAPMessage<ProbeType> probe;
        try {
            probe = WsDiscoveryS11Utilities.createWsdSOAPMessageProbe();
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryXMLException("Unable to create probe message", ex);
        }

        logger.finer("sendProbe() Sending probe with @MatchBy=" + matchBy);

        // Create new scope
        ScopesType scopesType = new ScopesType();

        // Set @MatchBy. If null it defaults to rfc2396 - see WsdConstants.matchBy* for valid options
        if (matchBy != null) {
            scopesType.setMatchBy(matchBy.toString());
        }

        // Place all URIs we want to search for in scopeType
        if (scopes != null) {
            for (URI u : scopes) {
                scopesType.getValue().add(u.toString());
            }
            probe.getJAXBBody().setScopes(scopesType);
        }

        // Add types 
        if (types != null) {
            probe.getJAXBBody().getTypes().addAll(types);
        }

        // Send packet multicast or to proxy
        if (useProxy) {
            logger.fine("Sending probe unicast to proxy at " + useProxyAddress + ":" + useProxyPort);
            probe.setTo(remoteProxyService.getEndpointReference().getAddress());
            try {
                soapOverUDP.send(probe, useProxyAddress, useProxyPort);
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to unicast probe to proxy server", ex);
            }
        } else {
            logger.fine("Multicasting probe (not using proxy).");
            probe.setTo(multicastTo);
            try {
                soapOverUDP.sendMulticast(probe);
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to multicast probe", ex);
            }
        }
    }

    /**
     * Sends a blank probe. Matches all services.
     */
    public void sendProbe() throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        this.sendProbe(null, null, null);
    }

    /**
     * Receive Hello-message.
     * 
     * @param m SOAP message
     * @throws WsDiscoveryNetworkException if <code>m</code> is not an instance of HelloType.
     */
    private void recvHello(WsDiscoveryS11SOAPMessage m)
            throws WsDiscoveryNetworkException {
        logger.finer("recvHello()");

        if (m.getJAXBBody() instanceof HelloType) {
            HelloType hello = (HelloType) m.getJAXBBody();
            if ((hello.getEndpointReference() != null) && (hello.getEndpointReference().getAddress() != null)) {
                logger.fine("Hello received for service " + hello.getEndpointReference() + ", metadata version " + hello.getMetadataVersion());
                logger.finest("Hello message contained: " + m);
            } else {
                logger.warning("Hello received without endpoint reference.");
            }

            // if RelatesTo is set and @Relationship="Suppression", use sender as proxy.
            if (m.getRelatesTo() != null) {
                // TODO Verify relatesTo
                logger.finer("relatesTo: " + m.getRelatesTo().toString());
                if (m.getRelationshipType() != null) {
                    logger.finer("relatesTo.relationshipType: " + m.getRelationshipType());
                    if (m.getRelationshipType().equals(proxySuppressionRelationship)) {
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

                            remoteProxyService = WsDiscoveryS11Utilities.createWsDiscoveryService(hello);

                            if (remoteProxyService.getEndpointReference() == null)
                                throw new WsDiscoveryNetworkException("Received proxy without endpoint reference");
                            if (remoteProxyService.getEndpointReference().getAddress() == null)
                                throw new WsDiscoveryNetworkException("Received proxy without endpoint address");

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
                WsDiscoveryS11Utilities.storeJAXBObject(serviceDirectory, hello);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store service received in Hello-message.", ex);
            }
        } else {
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvHello()");
        }
    }

    /**
     * Recieve ProbeMatch message.
     * 
     * @param m SOAP message with a ProbeMatch.
     * @param originalMessage The original SOAPOverUDPNetworkMessage.
     * @throws WsDiscoveryNetworkException if m is not an instance of ProbeMatchesType.
     */
    private void recvProbeMatches(WsDiscoveryS11SOAPMessage m)
            throws WsDiscoveryNetworkException {
        logger.finer("recvProbeMatches()");
        if (m.getJAXBBody() instanceof ProbeMatchesType) {
            ProbeMatchesType pmt = (ProbeMatchesType) m.getJAXBBody();
            if (pmt.getProbeMatch() != null) {
                logger.fine("ProbeMatches received with " + pmt.getProbeMatch().size() + " matches from " + m.getSrcAddress() + ":" + m.getSrcPort());
            } else {
                logger.fine("ProbeMatches received from " + m.getSrcAddress() + ":" + m.getSrcPort() + ", but it contained no results (was null).");
            }

            try {
                WsDiscoveryS11Utilities.storeProbesMatch(serviceDirectory, pmt);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store remote service.", ex);
            }
        } else {
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvProbeMatches()");
        }
    }

    /**
     * Receive ResolveMatches.
     * @param m SOAP message.
     * @param originalMessage The original SOAPOverUDPNetworkMessage.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ResolveMatchesType.
     */
    private void recvResolveMatches(WsDiscoveryS11SOAPMessage m)
            throws WsDiscoveryNetworkException {
        logger.finer("recvResolveMatches()");
        if (m.getJAXBBody() instanceof ResolveMatchesType) {
            ResolveMatchesType rmt = (ResolveMatchesType) m.getJAXBBody();
            logger.fine("ResolveMatches received for " + rmt.getResolveMatch().getEndpointReference() + " from " + m.getSrcAddress() + ":" + m.getSrcPort());
            try {
                WsDiscoveryS11Utilities.storeJAXBObject(serviceDirectory, rmt);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryNetworkException("Unable to store results from ResolveMatches-message.", ex);
            }
        } else {
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvResolveMatches()");
        }
    }

    /**
     * Receive Bye.
     * 
     * @param m SOAP message.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ByeType.
     */
    private void recvBye(WsDiscoveryS11SOAPMessage m)
            throws WsDiscoveryNetworkException {
        logger.finer("recvBye()");
        if (m.getJAXBBody() instanceof ByeType) {
            ByeType bt = (ByeType) m.getJAXBBody();
            SOAPOverUDPEndpointReferenceType btEndpoint =
                    WsDiscoveryS11Utilities.createSOAPOverUDPEndpointReferenceType(bt.getEndpointReference());

            if ((btEndpoint.getAddress() != null)) {
                logger.fine("Bye received for " + btEndpoint.getAddress());
            } else {
                logger.warning("Bye received without endpoint reference.");
            }

            if (useProxy) // Check if the proxy server sent bye                
            {
                if ((btEndpoint.getAddress() != null) &&
                        (btEndpoint.getAddress().equals(remoteProxyService.getEndpointReference().getAddress()))) {
                    logger.fine("Proxy service left the network. Disabling proxy.");
                    useProxy = false; // Stop using the proxy server
                }
            }

            WsDiscoveryS11Utilities.removeServiceBye(serviceDirectory, bt);

        } else {
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvBye()");
        }
    }

    /**
     * Receive Resolve.
     * 
     * @param m SOAP message.
     * @param originalMessage Original message as received from the transport layer.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ResolveType.
     */
    private void recvResolve(WsDiscoveryS11SOAPMessage m)
            throws WsDiscoveryNetworkException, WsDiscoveryXMLException {
        logger.finer("recvResolve()");
        if (m.getJAXBBody() instanceof ResolveType) {
            logger.fine("Received Resolve for service " +
                    ((ResolveType) m.getJAXBBody()).getEndpointReference().getAddress().getValue());

            // See if we have the service
            // If we are running in proxy mode, search local services first, then remote
            SOAPOverUDPEndpointReferenceType resolveEndpoint =
                    WsDiscoveryS11Utilities.createSOAPOverUDPEndpointReferenceType(((ResolveType) m.getJAXBBody()).getEndpointReference());
            WsDiscoveryService match = localServices.findService(resolveEndpoint);
            if (match != null) {
                logger.fine("Service found locally. Sending resolve match.");

                // Service found, send resolve match
                sendResolveMatch(match, m);
            } else {
                if (isProxy) { // We are running in proxy mode. Check full service directory                   
                    match = serviceDirectory.findService(resolveEndpoint);
                    if (match != null) {
                        logger.fine("Service found in service directory. Sending resolve match in proxy mode.");
                    } else {
                        logger.fine("Service not found. Sending empty resolve match in proxy mode.");
                    }
                    sendResolveMatch(match, m);
                } else // If in normal mode, just log failure.
                {
                    logger.fine("Service not found locally. No reply sent.");
                }
            }
        } else {
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvResolve()");
        }
    }

    /**
     * Receive Probe.
     * 
     * @param m SOAP msesage.
     * @param originalMessage Original message as received from the transport layer.
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException if m is not an instance of ProbeType.
     */
    private void recvProbe(WsDiscoveryS11SOAPMessage m)
            throws WsDiscoveryNetworkException {
        logger.finer("recvProbe()");

        if (m.getJAXBBody() instanceof ProbeType) {

            ProbeType probe = (ProbeType) m.getJAXBBody();
            logger.fine("Probe received from " + m.getSrcAddress() + ", port " + m.getSrcPort());

            IWsDiscoveryServiceCollection totalMatches;
            WsDiscoveryScopesType scopes = null;
            if (probe.getScopes() != null)
                scopes = WsDiscoveryS11Utilities.createWsDiscoveryScopesObject(probe.getScopes());

            if (!isProxy) { // Not in proxy mode; match local services only                
                try {
                    totalMatches = localServices.matchBy(probe.getTypes(), scopes);
                } catch (WsDiscoveryServiceDirectoryException ex) {
                    throw new WsDiscoveryNetworkException("Unable to get MatchBy-results for received Probe-message.", ex);
                }
            } else { // If we are in proxy mode, search full service directory
                try {
                    totalMatches = serviceDirectory.matchBy(probe.getTypes(), scopes);
                } catch (WsDiscoveryServiceDirectoryException ex) {
                    throw new WsDiscoveryNetworkException("Unable to search remote services for match.", ex);
                }
            }

            if ((totalMatches.size() > 0) || isProxy) { // Proxy MUST reply with match, even if empty                
                try {
                    sendProbeMatch(totalMatches, m);
                } catch (WsDiscoveryException ex) {
                    throw new WsDiscoveryNetworkException("Unable to send ProbeMatch", ex);
                }
            } else {
                logger.fine("ProbeMatches NOT found. No reply sent.");
            }
        } else {
            throw new WsDiscoveryNetworkException("Message of unknown type passed to recvProbe()");
        }
    }

    /**
     * Send Resolve. Only sent every 10 seconds for each service to avoid network floods.
     * 
     * @param service Service to resolve
     */
    public void sendResolve(WsDiscoveryService service) throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        logger.finer("sendResolve()");
        // Return if less than 10 seconds from last time we tried to resolve this service
        if (service.getTriedToResolve() != null) {
            if (service.getTriedToResolve().getTime() + 10000 > (new Date()).getTime()) {
                logger.finer("sendResolve() called too often for service " + service.getEndpointReference().getAddress());
                return;
            }
        }

        logger.finer("sendResolve() Sent Resolve for service " + service.getEndpointReference().getAddress());
        // Send resolve package
        WsDiscoveryS11SOAPMessage<ResolveType> resolve;
        try {
            resolve = WsDiscoveryS11Utilities.createWsdSOAPMessageResolve();
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryXMLException("Unable to create resolve message", ex);
        }

        resolve.getJAXBBody().setEndpointReference(
                WsDiscoveryS11Utilities.createEndpointReferenceTypeObject(service.getEndpointReference()));

        // Send multicast in normal mode or unicast in proxy mode
        if (useProxy) // Unicast
        {
            resolve.setTo(remoteProxyService.getEndpointReference().getAddress());
            try {
                // Unicast
                soapOverUDP.send(resolve, useProxyAddress, useProxyPort);
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to send resolve to proxy server.");
            }
        } else // Multicast
        {
            resolve.setTo(multicastTo);
            try {
                // Multicast
                soapOverUDP.sendMulticast(resolve);
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to send multicast resolve.");
            }
        }

        service.setTriedToResolve(new Date());
    }

    /**
     * Send proxy announce / suppression message.
     * 
     * @param relatesToMessage The message ID of the message we are suppressing.
     * @param originalMessage Original message as received from the transport layer.
     */
    protected void sendProxyAnnounce(WsDiscoveryS11SOAPMessage relatesToMessage) throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        logger.finer("sendProxyAnnounce()");

        logger.fine("Sending proxy announce to " + relatesToMessage.getSrcAddress() + ":" + relatesToMessage.getSrcPort());
        WsDiscoveryS11SOAPMessage<HelloType> proxyAnnounce;
        try {
            proxyAnnounce = WsDiscoveryS11Utilities.createWsdSOAPMessageHello(localProxyService);
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryXMLException("Unable to create proxy announcement (Hello message)", ex);
        }

        proxyAnnounce.setRelatesTo(relatesToMessage.getMessageId());
        proxyAnnounce.setRelationshipType(WsDiscoveryConstants.defaultProxyRelatesToRelationship);
        
        try {
            soapOverUDP.send(proxyAnnounce, relatesToMessage.getReplyAddress(), relatesToMessage.getReplyPort());
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryNetworkException("Unable to send proxy announcement (Hello message)", ex);
        }
    }

    /**
     * Send Hello.
     * 
     * @param service Service that says Hello.
     */
    public void sendHello(WsDiscoveryService service)  throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        WsDiscoveryS11SOAPMessage<HelloType> hello;
        try {
            hello = WsDiscoveryS11Utilities.createWsdSOAPMessageHello(service);
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryXMLException("Unable to create Hello message", ex);
        }
        if (useProxy) { // managed mode
            hello.setTo(remoteProxyService.getEndpointReference().getAddress()); // add proxy address as To (MUST)
            hello.setAddAppSequence(false); // SHOULD be false in managed mode
            try {
                soapOverUDP.send(hello, useProxyAddress, useProxyPort); // unicast to proxy
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to send managed Hello message", ex);
            }
        } else { // ad hoc mode
            hello.setTo(multicastTo);
            hello.setAddAppSequence(true); // MUST be true in ad-hoc mode
            try {
                soapOverUDP.sendMulticast(hello);
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to send ad-hoc Hello message",ex);
            }
        }
        logger.finer("sendHello() called for service " + service.getEndpointReference().getAddress().toString());
    }

    /**
     * Send Bye.
     * 
     * @param service Service that says Bye.
     */
    public void sendBye(WsDiscoveryService service) throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        WsDiscoveryS11SOAPMessage<ByeType> bye;
        try {
            bye = WsDiscoveryS11Utilities.createWsdSOAPMessageBye(service);
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryXMLException("Unable to create Bye message", ex);
        }
        if (useProxy) { // managed mode
            bye.setTo(remoteProxyService.getEndpointReference().getAddress());
            bye.setAddAppSequence(false); // SHOULD be false in managed mode
            try {
                soapOverUDP.send(bye, useProxyAddress, useProxyPort);
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to multicast Bye message in managed mode");
            }
        } else { // ad hoc mode
            bye.setTo(multicastTo);
            bye.setAddAppSequence(true); // MUST be true in ad-hoc mode
            try {
                soapOverUDP.sendMulticast(bye);
            } catch (SOAPOverUDPException ex) {
                throw new WsDiscoveryNetworkException("Unable to multicast Bye message in ad-hoc mode");
            }
        }
        logger.finer("sendBye() called for service " + service.getEndpointReference().getAddress().toString());
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
            WsDiscoveryS11SOAPMessage originalMessage) throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        
        // Return if less than 10 seconds since we sent a resolve match for this service to the requesting host
        if ((matchedService != null) && (matchedService.getTriedToResolve() != null)) {
            if (matchedService.getTriedToResolve().getTime() + 10000 > (new Date()).getTime()) {
                logger.finer("sendResolveMatch() called too often for service " + matchedService.getEndpointReference());
                return;
            }
        }

        // Send resolve match
        WsDiscoveryS11SOAPMessage<ResolveMatchesType> m;
        try {
            m = WsDiscoveryS11Utilities.createWsdSOAPMessageResolveMatches();
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryXMLException("Unable to create ResolveMatches message", ex);
        }
        
        m.setAddAppSequence(!useProxy); // SHOULD be false in managed mode

        // RelatesTo must contain the original MessageID
        m.setRelatesTo(originalMessage.getMessageId());

        // Set To to ReplyTo (or keep the default anonymous To)
        if ((originalMessage.getReplyTo() != null) && (originalMessage.getReplyTo().getAddress() != null)) {
            m.setTo(originalMessage.getReplyTo().getAddress());
        } else
            m.setTo(SOAPOverUDPWsAddressingNamespaces.WS_ADDRESSING_2005_08.getAnonymousReplyTo());

        ResolveMatchType match = null;
        if (matchedService != null) {
            match = new ResolveMatchType();

            match.setEndpointReference(WsDiscoveryS11Utilities.createEndpointReferenceTypeObject(matchedService.getEndpointReference()));
            match.setMetadataVersion(matchedService.getMetadataVersion());
            match.setScopes(WsDiscoveryS11Utilities.createScopesObject(matchedService));
            match.getTypes().addAll(matchedService.getPortTypes());
            match.getXAddrs().addAll(matchedService.getXAddrs());

            m.getJAXBBody().setResolveMatch(match);
        }
        try {
            // Try to send to the port/address set by ReplyTo. If it is not set, we default to srcPort/address
            soapOverUDP.send(m, originalMessage.getReplyAddress(), originalMessage.getReplyPort());
            // Store time
            matchedService.setSentResolveMatch(originalMessage.getSrcAddress());
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryNetworkException("Unable to send ResolveMatch", ex);
        }
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
            WsDiscoveryS11SOAPMessage originalMessage) throws WsDiscoveryException {
        
        // Create probe match
        WsDiscoveryS11SOAPMessage<ProbeMatchesType> m;
        try {
            m = WsDiscoveryS11Utilities.createWsdSOAPMessageProbeMatches();
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryXMLException("Unable to create ProbeMatches message", ex);
        }

        // RelatesTo must contain the original MessageID
        m.setRelatesTo(originalMessage.getMessageId());
       
        m.setAddAppSequence(!useProxy); // MUST be included in ad-hoc, SHOULD not be included in managed (using http)

        for (WsDiscoveryService service : matches) {
            ProbeMatchType match = new ProbeMatchType();

            match.setEndpointReference(WsDiscoveryS11Utilities.createEndpointReferenceTypeObject(service.getEndpointReference()));
            match.setMetadataVersion(service.getMetadataVersion());
            match.setScopes(WsDiscoveryS11Utilities.createScopesObject(service));
            match.getTypes().addAll(service.getPortTypes());
            match.getXAddrs().addAll(service.getXAddrs());

            m.getJAXBBody().getProbeMatch().add(match);
        }
        
        try {
            logger.fine("ProbeMatches sent with " + matches.size() + " matches to " + originalMessage.getReplyAddress() + ":" + originalMessage.getReplyPort());
            soapOverUDP.send(m, originalMessage.getReplyAddress(), originalMessage.getReplyPort());            
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryNetworkException("Unable to send ProbeMatch",ex);
        }
    }

    /**
     * Dispatcher. Should be called from the while-loop in run().
     * 
     * @throws wsdiscovery.network.exception.WsDiscoveryNetworkException on errors.
     */
    protected void dispatch() throws InterruptedException, WsDiscoveryException {

        WsDiscoveryS11SOAPMessage message;
        try {
            ISOAPOverUDPMessage m = soapOverUDP.recv(1000);
            if (m == null) // recv() timed out
                return;
            message = new WsDiscoveryS11SOAPMessage(m);
        } catch (SOAPOverUDPException ex) {
            throw new WsDiscoveryNetworkException("Unable to receive message from SOAPOverUDP", ex);
        }

        if (message == null) {
            return;
        }

        // Was message sent multicast or unicast?
        boolean isMulticast = (message.getDstAddress().equals(soapOverUDP.getTransport().getMulticastAddress()) ||
                (message.getDstPort() == soapOverUDP.getTransport().getMulticastPort()));

        // Return if the message was from us
        // This should actually never happen, as SOAPOverUDP should have discarded
        // the message already...
        if ((message.getInstanceId() == WsDiscoveryConstants.instanceId) &&
           ((message.getSequenceId() == null) ||
             (message.getSequenceId().equals("urn:uuid:" + WsDiscoveryConstants.sequenceId)))) {
            // Since this should have been handled by SOAPOverUDP and MessageId we log it as a warning
            logger.warning("** Discarded message sent from us: " + message.getMessageId());
            return;
        }

        // HELLO
        if (message.getJAXBBody() == null) {
            logger.fine("Received empty message (JAXB did not return data)");
        } else
        if (message.getJAXBBody() instanceof HelloType) {
            recvHello(message); // Add new service, even when using proxy
        } else // PROBE
        if (message.getJAXBBody() instanceof ProbeType) {
            if (isMulticast && isProxy) { // Respond to multicast probes with unicast proxy announcement
                logger.fine("Sending proxy announce in response to multicast Probe with MessageID: " + message.getMessageId());
                sendProxyAnnounce(message);
            }
            recvProbe(message);
        } else // PROBE MATCHES
        if (message.getJAXBBody() instanceof ProbeMatchesType) {
            recvProbeMatches(message); // Add services from probe matches
        } else // RESOLVE
        if (message.getJAXBBody() instanceof ResolveType) {
            if (isMulticast && isProxy) { // Respond to multicast resolves with unicast proxy announcement
                logger.fine("Sending proxy announce in response to multicast Resolve with MessageID " + message.getMessageId());
                sendProxyAnnounce(message);
            }
            recvResolve(message); // Send resolve match
        } else // RESOLVE MATCHES
        if (message.getJAXBBody() instanceof ResolveMatchesType) {
            recvResolveMatches(message); // Add updates from resolve matches
        } else // BYE
        if (message.getJAXBBody() instanceof ByeType) {
            recvBye(message); // Remove service
        } else {
            throw new WsDiscoveryNetworkException("Don't know how to handle message with element " + message.getJAXBBody().getClass().getName());
        }

    }

    public MatchBy getDefaultMatchBy() {
        return WsDiscoveryS11Utilities.getDefaultMatchBy();
    }


}
