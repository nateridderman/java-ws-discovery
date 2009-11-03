/*
MatchScopeStrcmp0.java

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

/**
 * Match scope against target service using the string comparison.
 * See the WS-Discovery specification or {@link MatchBy} for details.
 * @author Magnus Skjegstad
 */
public class MatchScopeStrcmp0 implements IWsDiscoveryMatchScope {
   /**
     * Match scope against target service using the strcmp0 algorithm.
     * See the WS-Discovery specification or {@link MatchBy} for details.
     * @param target Target service. 
     * @param probeScopes Scopes to probe for.
     * @return True on success, false on failure.
     */
    public boolean matchScope(WsDiscoveryService target, WsDiscoveryScopesType probeScopes) {
        if (probeScopes == null) // The probe didn't include scope
            return true;
        
        // "Using a case-sensitive comparison, the string representation of S1 and S2 is the same"        
        boolean found = false;
        for (String s : probeScopes.getValue()) {
            found = false;
            for (URI u : target.getScopes())
                if (u.toString().equals(s)) {
                    found = true;
                    continue;
                }
            if (!found)
                break;
        }


        return found;
    }
}
