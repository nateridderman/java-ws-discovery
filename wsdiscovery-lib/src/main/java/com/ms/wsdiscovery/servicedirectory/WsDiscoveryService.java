/*
WsDiscoveryService.java

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

package com.ms.wsdiscovery.servicedirectory;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryFactory;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPEndpointReferenceType;
import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;

/**
 * Class for storing a WS-Discovery service description. See the specification
 * or the schema for details on the different data types.
 * <p>
 * This class is thread safe.
 * 
 * @author Magnus Skjegstad
 */
public class WsDiscoveryService {
    /**
     * Endpoint reference
     */
    protected SOAPOverUDPEndpointReferenceType endpointReference = null;
    
    /**
     * Port types
     */
    protected List<QName> portTypes = null;
    
    /**
     * Scopes
     */
    protected List<URI> scopes = null;
    
    /**
     * Algorithm for matching scopes
     */
    protected MatchBy scopesMatchBy = null;
    
    /**
     * Invocation addresses (XAddrs)
     */
    protected List<String> xAddrs = null;
    
    /**
     * Metadata version
     */
    protected long metadataVersion = 0;
    
    /**
     * Creation time
     */
    protected Date created = new Date();
    
    /**
     * Last time a Resolve-packet was sent for this service.
     * Set when a resolve-packet has been sent for this endpoint
     */
    protected Date triedToResolve = null;
    
    /**
     * Set for all hosts that has received a ResolveMatch for this service. Used to enforce timeouts per host.
     */
    protected Map<InetAddress, Date> sentResolveMatch = new HashMap<InetAddress, Date>();

    /**
     * Create a new, empty WS-Discovery service description.
     */
    public WsDiscoveryService() {
        
    }
    
    /**
     * Create a new WS-Discovery service description.
     * 
     * @param endpoint Service endpoint.
     * @param portTypes List of port types supported by the service.
     * @param scopes List of scopes the service is in. <code>null</code> matches all scopes.
     * @param XAddrs Invocation address.
     * @param version Metadata version. 
     */
    public WsDiscoveryService(SOAPOverUDPEndpointReferenceType endpoint, List<QName> portTypes, WsDiscoveryScopesType scopes, List<String> XAddrs, long version) {
        setEndpointReferenceType(endpoint);
        setPortTypes(portTypes);
        setScopesType(scopes);
        setXAddrs(XAddrs);
        setMetadataVersion(version); 
    }
    
    /**
     * Create a new WS-Discovery service description. Endpoint address is set to 
     * a random UUID and metadata version is added automatically, starting at 1.
     * 
     * @param portTypes List of port types supported by the service. 
     * @param scopes List of scopes the service is in. <code>null</code> matches all scopes.
     * @param XAddrs Invocation address.
     */
    public WsDiscoveryService(List<QName> portTypes, WsDiscoveryScopesType scopes, List<String> XAddrs) {
        SOAPOverUDPEndpointReferenceType endpoint = new SOAPOverUDPEndpointReferenceType();
        endpoint.setAddress(URI.create("urn:uuid:"+UUID.randomUUID().toString()));
        setEndpointReferenceType(endpoint);
        setPortTypes(portTypes);
        setScopesType(scopes);
        setXAddrs(XAddrs);
        setMetadataVersion(1);
    }
            
    /**
     * Create a new WS-Discovery service description. Endpoint address is set to 
     * a random UUID and metadata version is added automatically, starting at 1.
     * 
     * @param portType Port type supported by the service.
     * @param scopes Scopes the service is in. <code>null</code> matches all scopes.
     * @param XAddr Invocation address.
     */
    public WsDiscoveryService(QName portType, WsDiscoveryScopesType scopes, String XAddr) {
        this(Collections.singletonList(portType),scopes,Collections.singletonList(XAddr));
    }
       
    /**
     * Decide whether the given port types and scopes match this service. The 
     * matching algorithm is taken from <code>probeScopes</code>. If <code>probeScopes</code>
     * does not have a matching algorithm specified (matchBy is null) the matcher from
     * {@link WsDiscoveryConstants#defaultMatchBy} is used.
     * 
     * @param probeTypes Port types in probe. <code>null</code> is always a match.
     * @param probeScopes Scopes in probe. <code>null</code> is always a match.
     * @return True if match.
     */
    public boolean isMatchedBy(List<QName> probeTypes, WsDiscoveryScopesType probeScopes) {
        MatchBy m = WsDiscoveryConstants.defaultMatchBy;
        if (probeScopes != null)
            m = WsDiscoveryFactory.getMatcher(probeScopes.getMatchBy());

        if (m == null)
            return false;        
        
        return m.match(this, probeTypes, probeScopes);
    }
           
    

    /**
     * Stores a copy of an endpoint reference.
     * @param er Endpoint reference.
     */
    public synchronized void setEndpointReferenceType(SOAPOverUDPEndpointReferenceType er) {
        this.endpointReference = (SOAPOverUDPEndpointReferenceType) er.clone();
    }

    /**
     * Sets the endpoint reference.
     * @param endpointReference WS-Discovery endpoint reference.
     */
    public void setEndpointReference(SOAPOverUDPEndpointReferenceType endpointReference) {
        this.endpointReference = endpointReference;
    }

    /**
     * Get endpoint reference.
     * @return Endpoint reference.
     */
    public synchronized SOAPOverUDPEndpointReferenceType getEndpointReference() {
        return this.endpointReference;
    }
    
    /**
     * Create a copy of the current endpoint reference object.
     * @return Clone of endpoint reference.
     */
    public synchronized SOAPOverUDPEndpointReferenceType createEndpointReferenceObject() {
        return (SOAPOverUDPEndpointReferenceType) this.endpointReference.clone();
    }

    /**
     * Get metadata version.
     * @return Metadata version.
     */
    public synchronized long getMetadataVersion() {
        return metadataVersion;
    }

    /**
     * Set metadata version.
     * @param metadataVersion New metadata version.
     */
    public synchronized void setMetadataVersion(long metadataVersion) {
        this.metadataVersion = metadataVersion;
    }

    /**
     * Get list of scope URIs 
     * @return Scope URIs
     */
    public synchronized List<URI> getScopes() {
        return scopes;
    }
    
    /**
     * Get algorithm set in the MatchBy-attribute of the scopes.
     * @return MatchBy-algorithm
     */
    public synchronized MatchBy getScopesMatchBy() {
        return scopesMatchBy;
    }
    
    /**
     * Set scopes.
     * @param scopes New scopes.
     */
    public synchronized void setScopesType(WsDiscoveryScopesType scopes) {
        this.scopes = Collections.synchronizedList(new ArrayList<URI>());
        
        if (scopes != null) {
            if (scopes.getValue() != null)
                for (String s : scopes.getValue())
                    this.scopes.add(URI.create(s));
            scopesMatchBy = WsDiscoveryFactory.getMatcher(scopes.getMatchBy());
        }
    }

    /**
     * Set scopes.
     * @param scopes List of new scopes.
     */
    public void setScopes(List<URI> scopes) {
        WsDiscoveryScopesType scopesType = new WsDiscoveryScopesType();
        for (URI scope : scopes)
            scopesType.getValue().add(scope.toString());
        setScopesType(scopesType);
    }

    /**
     * Get port types.
     * @return List of port types.
     */
    public synchronized List<QName> getPortTypes() {
        return Collections.synchronizedList(portTypes);
    }

    /**
     * Set port types.
     * @param types List of port types.
     */
    public synchronized void setPortTypes(List<QName> types) {
        if (types != null) {
            this.portTypes = Collections.synchronizedList(new ArrayList<QName>());
            this.portTypes.addAll(types);
        } else
            this.portTypes = null;
    }

    /**
     * Get invocation addresses.
     * @return List of invocation addresses.
     */
    public synchronized List<String> getXAddrs() {
        return xAddrs;
    }

    /**
     * Set invocation addresses.
     * @param xAddrs List of invocation addresses.
     */
    public synchronized void setXAddrs(List<String> xAddrs) {
        if (xAddrs != null) {
            this.xAddrs =  Collections.synchronizedList(new ArrayList<String>());
            this.xAddrs.addAll(xAddrs);
        } else
            this.xAddrs = null;        
    }    
    
    @Override
    public synchronized String toString() {
        List<String> l = new ArrayList<String>();
        
        if (this.endpointReference != null)
            l.add(this.endpointReference.toString());
        
        if ((getPortTypes() != null) && (getPortTypes().size() > 0)) {
            String s = new String();
            for (QName q : getPortTypes())
                if (q != null)
                    s += "Types: " + q.toString() + "\n";
            if (s.length() != 0)
                l.add(s);
        }
        
        if ((getXAddrs() != null) && (!getXAddrs().isEmpty())) {
            String s = new String();
            for (String addr : getXAddrs())
                s += "XAddrs: " + addr + "\n";
            if (s.length() != 0)
                l.add(s);
        }
        
        if (getScopes() != null) {
            String s = new String();
            if (getScopesMatchBy() != null)
                s += "Scopes.MatchBy: " + getScopesMatchBy()+ "\n";
            for (URI scope : getScopes())
                if (scope != null)
                    s += "Scopes: " + scope + "\n";            
            if (s.length() != 0)
                l.add(s);
        }
        
        String result = new String();
        for (String n : l) 
            if (n.length() != 0)
                result += n;
        
        return result;
    }

    /**
     * Get number of times the service has tried to resolve the invocation address.
     * @return Number of Resolve-messages sent.
     */
    public synchronized Date getTriedToResolve() {
        return triedToResolve;
    }

    /**
     * Sets the date and time of the last Resolve-message was sent for this service.
     * @param lastResolved Date of last sent Resolve-message.
     */
    public synchronized void setTriedToResolve(Date lastResolved) {
        this.triedToResolve = lastResolved;
    }
    
    /**
     * Get timestamp for when a ResolveMatch was sent to the specified address.
     * @param address Receiver of ResolveMatch.
     * @return Timestamp for last ResolveMatch sent to this address.
     */
    public synchronized Date getSentResolveMatch(InetAddress address) {
        return sentResolveMatch.get(address);
    }    
    
    /**
     * Set timestamp for ResolveMatch sent.
     * @param address Address the ResolveMatch was sent to.
     */
    public synchronized void setSentResolveMatch(InetAddress address) {
        sentResolveMatch.put(address, new Date());
    }
    
    @Override
    public synchronized int hashCode() {
        return super.hashCode();
    }

    @Override
    public synchronized boolean equals(Object obj) {
        return super.equals(obj);
    }

}
