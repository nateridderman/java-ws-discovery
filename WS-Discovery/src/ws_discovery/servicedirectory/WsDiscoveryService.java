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

package ws_discovery.servicedirectory;

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
import ws_discovery.WsDiscoveryBuilder;
import ws_discovery.WsDiscoveryConstants;
import ws_discovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import ws_discovery.xml.jaxb_generated.ByeType;
import ws_discovery.xml.jaxb_generated.EndpointReferenceType;
import ws_discovery.xml.jaxb_generated.HelloType;
import ws_discovery.xml.jaxb_generated.ProbeMatchType;
import ws_discovery.xml.jaxb_generated.ProbeMatchesType;
import ws_discovery.xml.jaxb_generated.ProbeType;
import ws_discovery.xml.jaxb_generated.ResolveMatchType;
import ws_discovery.xml.jaxb_generated.ResolveMatchesType;
import ws_discovery.xml.jaxb_generated.ScopesType;
import ws_discovery.servicedirectory.matcher.MatchBy;

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
    protected EndpointReferenceType endpointReference = null;
    
    /**
     * Port types
     */
    protected List<QName> types = null;
    
    /**
     * Scopes
     */
    protected List<URI> scopesValues = null;
    
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
     * Create a new WS-Discovery service description.
     * 
     * @param endpoint Service endpoint.
     * @param portTypes List of port types supported by the service.
     * @param scopes List of scopes the service is in. <code>null</code> matches all scopes.
     * @param XAddrs Invocation address.
     * @param version Metadata version. 
     */
    public WsDiscoveryService(EndpointReferenceType endpoint, List<QName> portTypes, ScopesType scopes, List<String> XAddrs, long version) {
        setEndpointReference(endpoint);
        setTypes(portTypes);
        setScopes(scopes);
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
    public WsDiscoveryService(List<QName> portTypes, ScopesType scopes, List<String> XAddrs) {
        EndpointReferenceType endpoint = new EndpointReferenceType();
        endpoint.setAddress(WsDiscoveryConstants.XMLBUILDER.createAttributedURI("urn:uuid:"+UUID.randomUUID().toString()));                     
        setEndpointReference(endpoint);
        setTypes(portTypes);
        setScopes(scopes);
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
    public WsDiscoveryService(QName portType, ScopesType scopes, String XAddr) {
        this(Collections.singletonList(portType),scopes,Collections.singletonList(XAddr));
    }
       
    /**
     * Creates a WS-Discovery service description based on a received Hello-packet.
     * 
     * @param m Hello-packet.
     * @throws WsDiscoveryServiceDirectoryException 
     */
    public WsDiscoveryService(HelloType m) throws WsDiscoveryServiceDirectoryException {
        this((Object)m);
    }

    /**
     * Creates a WS-Discovery service description based on a received Bye-packet.
     * 
     * @param m Bye-packet.
     * @throws WsDiscoveryServiceDirectoryException 
     */
    public WsDiscoveryService(ByeType m) throws WsDiscoveryServiceDirectoryException {
        this((Object)m);
    }
    
    /**
     * Creates a WS-Discovery service description based on a received ProbeMatch-packet.
     * 
     * @param m ProbeMatch-packet.
     * @throws WsDiscoveryServiceDirectoryException 
     */
    public WsDiscoveryService(ProbeMatchType m) throws WsDiscoveryServiceDirectoryException {
        this((Object)m);
    }    
    
    /**
     * Creates a WS-Discovery service description based on ResolveMatches in a ResolveMatch-packet.
     * 
     * @param m ResolveMatches data.
     * @throws WsDiscoveryServiceDirectoryException 
     */
    public WsDiscoveryService(ResolveMatchesType m) throws WsDiscoveryServiceDirectoryException {
        this((Object)m);
    }    
    
    /**
     * Creates a WS-Discovery service description based on a ResolveMatch-packet.
     * @param m ResolveMatch-packet.
     * @throws WsDiscoveryServiceDirectoryException 
     */
    public WsDiscoveryService(ResolveMatchType m) throws WsDiscoveryServiceDirectoryException {
        this((Object)m);
    }
        
    /**
     * Creates a WS-Discovery service description based on a JAXB object. The 
     * object must an instance of one of: 
     * <li>{@link HelloType}</li>
     * <li>{@link ByeType}</li>
     * <li>{@link ProbeMatchType}</li>
     * <li>{@link ResolveMatchesType}</li>
     * <li>{@link ResolveMatchType}</li>
     * When extracting service descriptions from a {@link ProbeMatchesType}, the
     * constructor of {@link WsDiscoveryServiceDirectory} must be used, since 
     * {@link ProbeMatchesType} can contain multiple service descriptions.
     * @param jaxbbody JAXB object.
     * @throws WsDiscoveryServiceDirectoryException 
     */
    public WsDiscoveryService(Object jaxbbody) throws WsDiscoveryServiceDirectoryException {
        if (jaxbbody instanceof HelloType) {
            HelloType m = (HelloType)jaxbbody;
            setEndpointReference(m.getEndpointReference());
            setTypes(m.getTypes());
            setScopes(m.getScopes());
            setXAddrs(m.getXAddrs());
            setMetadataVersion(m.getMetadataVersion());
        } else
        if (jaxbbody instanceof ByeType) {
            ByeType m = (ByeType)jaxbbody;
            setEndpointReference(m.getEndpointReference());
        } else
        if (jaxbbody instanceof ProbeMatchType) {
            ProbeMatchType m = (ProbeMatchType)jaxbbody;
            setEndpointReference(m.getEndpointReference());
            setTypes(m.getTypes());
            setScopes(m.getScopes());
            setXAddrs(m.getXAddrs());
            setMetadataVersion(m.getMetadataVersion());
        } else
        if (jaxbbody instanceof ResolveMatchesType) {
            ResolveMatchType m = ((ResolveMatchesType)jaxbbody).getResolveMatch();
            setEndpointReference(m.getEndpointReference());
            setTypes(m.getTypes());
            setScopes(m.getScopes());
            setXAddrs(m.getXAddrs());
            setMetadataVersion(m.getMetadataVersion());
        } else
        if (jaxbbody instanceof ResolveMatchType) {
            ResolveMatchType m = (ResolveMatchType)jaxbbody;
            setEndpointReference(m.getEndpointReference());
            setTypes(m.getTypes());
            setScopes(m.getScopes());
            setXAddrs(m.getXAddrs());
            setMetadataVersion(m.getMetadataVersion());
        } else
        if (jaxbbody instanceof ProbeMatchesType) {
            throw new WsDiscoveryServiceDirectoryException("Multiple service descriptions found. Use ServiceDirectory to extract them.");
        } else
            throw new WsDiscoveryServiceDirectoryException("Unsupported object type.");
    }    
   
    /**
     * Decide whether the given port types and scopes match this service. The 
     * matching algorithm is taken from <code>probeScope</code>. See 
     * {@link ScopesType#getMatchBy()} and 
     * {@link WsDiscoveryBuilder#getMatcher(ws_discovery.xml.jaxb_generated.ScopesType)}
     * 
     * @param probeTypes Port types in probe. <code>null</code> is always a match.
     * @param probeScopes Scopes in probe. <code>null</code> is always a match.
     * @return True if match.
     */
    public boolean isMatchedBy(List<QName> probeTypes, ScopesType probeScopes) {
        MatchBy m = WsDiscoveryBuilder.getMatcher(probeScopes);

        if (m == null)
            return false;        
        
        return m.match(this, probeTypes, probeScopes);
    }
           
    /**
     * Decide whether a Probe-packet matches this service. 
     * See also {@link WsDiscoveryService#isMatchedBy(java.util.List, ws_discovery.xml.jaxb_generated.ScopesType)}.
     * 
     * @param probe Probe-message.
     * @return True if <code>probe</code> matches.
     */
    public synchronized boolean isMatchedBy(ProbeType probe) {
        return isMatchedBy(probe.getTypes(), probe.getScopes());
    }

    /**
     * Stores a copy of an endpoint reference.
     * @param er Endpoint reference.
     */
    public synchronized void setEndpointReference(EndpointReferenceType er) {
        this.endpointReference = WsDiscoveryConstants.XMLBUILDER.cloneEndpointReference(er);
    }
    
    /**
     * Get endpoint reference.
     * @return Endpoint reference.
     */
    public synchronized String getEndpointReferenceAddress() {
        if (endpointReference.getAddress() != null)
            return endpointReference.getAddress().getValue();
        return null;
    }
    
    /**
     * Create a copy of the current endpoint reference object.
     * @return Clone of endpoint reference.
     */
    public synchronized EndpointReferenceType createEndpointReferenceObject() {
        return WsDiscoveryConstants.XMLBUILDER.cloneEndpointReference(this.endpointReference);
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
    public synchronized List<URI> getScopesValues() {
        return scopesValues;
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
    public synchronized void setScopes(ScopesType scopes) {
        this.scopesValues = Collections.synchronizedList(new ArrayList<URI>());
        
        if (scopes != null) {
            if (scopes.getValue() != null)
                for (String s : scopes.getValue())
                    this.scopesValues.add(URI.create(s));
            scopesMatchBy = WsDiscoveryBuilder.getMatcher(scopes);
        }
    }
    
    /**
     * Set scopes.
     * @param scopes List of new scopes.
     */
    public void setScopes(List<String> scopes) {
        ScopesType scopesType = new ScopesType();
        scopesType.getValue().addAll(scopes);
        setScopes(scopesType);
    }

    /**
     * Get port types.
     * @return List of port types.
     */
    public synchronized List<QName> getTypes() {
        return Collections.synchronizedList(types);
    }

    /**
     * Set port types.
     * @param types List of port types.
     */
    public synchronized void setTypes(List<QName> types) {
        if (types != null) {
            this.types = Collections.synchronizedList(new ArrayList<QName>());
            this.types.addAll(types);
        } else
            this.types = null;
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
        
        if (this.endpointReference != null) {
            String s = new String();
            EndpointReferenceType e = this.endpointReference;
            if ((e.getAddress() != null) && (e.getAddress().getValue() != null))
                s += "\tAddress: " + (e.getAddress().getValue()) + "\n";
            if ((e.getPortType() != null) && (e.getPortType().getValue() != null))
                s += "\tPortType: " + e.getPortType().getValue() + "\n";
            if ((e.getReferenceParameters() != null) && (e.getReferenceParameters().getAny() != null)) {
                for (Object o : e.getReferenceParameters().getAny())
                    s += "\tReferenceParameter: " + o.toString() + "\n";
            }
            if ((e.getReferenceProperties() != null) && (e.getReferenceProperties().getAny() != null)) {
                for (Object o : e.getReferenceProperties().getAny())
                    if (o != null)
                        s += "\tReferenceProperties: " + o.toString() + "\n";
            }
            if ((e.getServiceName() != null) && (e.getServiceName().getPortName() != null))
                s += "\tServiceName.PortName: " + e.getServiceName().getPortName() + "\n";
            if ((e.getServiceName() != null) && (e.getServiceName().getValue() != null))
                s += "\tServiceName.Value: " + e.getServiceName().getValue().toString() + "\n";
            
            if (!s.isEmpty())
                l.add("EndpointReference:\n" + s);
        }
        
        if ((getTypes() != null) && (getTypes().size() > 0)) {
            String s = new String();
            for (QName q : getTypes()) 
                if (q != null)
                    s += "Types: " + q.toString() + "\n";
            if (!s.isEmpty())
                l.add(s);
        }
        
        if ((getXAddrs() != null) && (!getXAddrs().isEmpty())) {
            String s = new String();
            for (String addr : getXAddrs())
                s += "XAddrs: " + addr + "\n";
            if (!s.isEmpty())
                l.add(s);
        }
        
        if (getScopesValues() != null) {
            String s = new String();
            if (getScopesMatchBy() != null)
                s += "Scopes.MatchBy: " + getScopesMatchBy()+ "\n";
            for (URI scope : getScopesValues())
                if (scope != null)
                    s += "Scopes: " + scope + "\n";            
            if (!s.isEmpty())
                l.add(s);
        }
        
        String result = new String();
        for (String n : l) 
            if (!n.isEmpty())
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
    
    /**
     * Create a {@link ScopesType} object with values from this service description.
     * 
     * @return The scopes of this service represented as a {@link ScopesType}.
     */
    public synchronized ScopesType createScopesObject() {
        ScopesType s = new ScopesType();
        if (getScopesMatchBy() != null)
            s.setMatchBy(getScopesMatchBy().toString());
        for (URI u : getScopesValues())
            s.getValue().add(u.toString());
        return s;
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
