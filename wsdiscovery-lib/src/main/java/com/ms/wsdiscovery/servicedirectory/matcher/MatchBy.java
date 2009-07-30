/*
MatchBy.java

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
package com.ms.wsdiscovery.servicedirectory.matcher;

import java.net.URI;
import java.util.List;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.xml.jaxb_generated.ScopesType;

/**
 * Enum class with the available match algorithms used to match
 * scopes when receiving a Probe-message.
 * <p>
 * All algorithms must implement the {@link IMatchScope} interface.
 * 
 * @author Magnus Skjegstad
 */
public enum MatchBy {
    /** 
     * From the WS-Discovery Specification Draft, 2005:
     * Using a case-insensitive comparison,
     * <li>The scheme of S1 and S2 is the same and</li>
     * <li>The authority of S1 and S2 is the same and</li>
     * <p>
     * Using a case-sensitive comparison,
     * <li>The path_segments of S1 is a segment-wise (not string) prefix of the 
     *    path_segments of S2 and</li>
     * <li>Neither S1 nor S2 contain the "." segment or the ".." segment.</li>
     * <p>
     * All other components (e.g., query and fragment) are explicitly excluded from comparision. 
     * <p>
     * Note: this matching rule does NOT test whether the string representation of S1 is
     * a prefix of the string representation of S2. For example, 
     * "http://example.com/abc" matches "http://example.com/abc/def" using this rule
     * but "http://example.com/a" does not.
     */
    RFC2396("http://schemas.xmlsoap.org/ws/2005/04/discovery/rfc2396", new MatchScopeRFC2396()),
    /**
     * From the WS-Discovery Specification Draft, 2005:<p>
     * Using a case-insensitive comparison, the scheme of S1 and S2 is "uuid" and each of the 
     * unsigned integer fields in S1 is equal to the corresponding field in S2, or equivalently, the 
     * 128 bits of the in-memory representation of S1 and S2 are the same 128 bit unsigned integer.
     */
    UUID("http://schemas.xmlsoap.org/ws/2005/04/discovery/uuid", new MatchScopeUUID()),
    /**     
     * From the WS-Discovery Specification Draft, 2005:<p>
     * Using case-insensitive comparison, the scheme of S1 and S2 is "ldap" and the
     * hostport [RFC 2255] of S1 and S2 is the same and the RDNSequence [RFC 2253]
     * of the dn of S1 is a prefix of the RDNSequence of the dn of S2, where comparison
     * does not support the variants in an RDNSequence described in Section 4 of
     * RFC 2253.
     */
    LDAP("http://schemas.xmlsoap.org/ws/2005/04/discovery/ldap", new MatchScopeLDAP()),
    
    /**
     * From the WS-Discovery Specification Draft, 2005:
     * Using a case-sensitive comparison, the string representation of S1 and S2 is the same.
     */
    strcmp0("http://schemas.xmlsoap.org/ws/2005/04/discovery/strcmp0", new MatchScopeStrcmp0());
        
    private final URI matchType;
    private final IMatchScope serviceMatcher;
    
    @Override
    public String toString() {
        synchronized (this) {
            return matchType.toString();
        }
    }
    
    /**
     * The MatchBy-algorithm represented as a URI.
     * @return URI of algorithm.
     */
    public synchronized URI toURI() {
        return matchType;
    }

    /**
     * Match target service with a given port type and list of scopes. See
     * the WS-Discovery specification for details on the scope matching process.
     * @param target Target service description.
     * @param probeTypes Port types to probe for (may be <code>null</code>).
     * @param probeScopes Scopes to probe for (may be <code>null</code>).
     * @return True if there is a match, false otherwise.
     */
    public synchronized boolean match(WsDiscoveryService target, List<QName> probeTypes, ScopesType probeScopes) {
        // Match types first. 
        if ((probeTypes != null) && (!target.getTypes().containsAll(probeTypes)))
                return false; 
        
        // Match scopes 
        return serviceMatcher.matchScope(target, probeScopes);
    }

    MatchBy(String matchType, IMatchScope matcher) {
        this.matchType = URI.create(matchType);
        this.serviceMatcher = matcher;
    }
}
