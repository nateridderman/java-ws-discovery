/*
MatchScopeNone.java

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
package com.ms.wsdiscovery.servicedirectory.matcher;

import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryMatchScope;

/**
 * Match all services that are not registered with a matching scope.
 *
 * @author Magnus Skjegstad
 */
public class MatchScopeNone implements IWsDiscoveryMatchScope {
    /**
     * Match services without scope.
     *
     * @param target Target service.
     * @param probeScopes Scopes to probe for. This parameter is ignored.
     * @return True on success, false on failure.
     */
    public boolean matchScope(WsDiscoveryService target, WsDiscoveryScopesType probeScopes) {
        return (target.getScopes().size() == 0); // match services without scopes
    }

}
