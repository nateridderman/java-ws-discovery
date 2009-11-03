/*
MatchScopeUUID.java

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

import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryMatchScope;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import java.net.URI;
import java.util.UUID;

/**
 * Match scope against target service using the UUID algorithm.
 * See the WS-Discovery specification or {@link MatchBy} for details.
 * @author Magnus Skjegstad
 */
public class MatchScopeUUID implements IWsDiscoveryMatchScope {

    private UUID urnToUUID(String urn) {
        UUID uuid = null; // default to null 
        try {
            URI uri = URI.create(urn);
            
            if (uri.getScheme() == null) // no scheme?
                uuid = UUID.fromString(urn);
            else {
                if (uri.getScheme().equals("urn")) // urn prefix
                    uri = URI.create(uri.getSchemeSpecificPart()); // extract inner uuid from urn:uuid: prefix
                
                if (uri.getScheme().equals("uuid")) // uuid prefix
                    uuid = UUID.fromString(uri.getSchemeSpecificPart());
            }
        } finally {
            return uuid;
        }
    }

    /**
     * Match scope against target service using the UUID algorithm.
     * See the WS-Discovery specification or {@link MatchBy} for details.
     * @param target Target service. 
     * @param probeScopes Scopes to probe for.
     * @return True on success, false on failure.
     */
    public boolean matchScope(WsDiscoveryService target, WsDiscoveryScopesType probeScopes) {
        /**
     * From the WS-Discovery Specification Draft, 2005:<p>
     * Using a case-insensitive comparison, the scheme of S1 and S2 is "uuid" and each of the
     * unsigned integer fields in S1 is equal to the corresponding field in S2, or equivalently, the
     * 128 bits of the in-memory representation of S1 and S2 are the same 128 bit unsigned integer.
     */
         if (probeScopes == null) // The probe didn't include scope
            return true;

        boolean found = false;
        for (String s : probeScopes.getValue()) {

            UUID uuid = urnToUUID(s);
            if (uuid == null)
                continue;

            for (URI u : target.getScopes())
                if (urnToUUID(u.toString()).equals(uuid)) {
                    found = true;
                    break;
                }

            if (found)
                break;
        }

        return found;
    }

}
