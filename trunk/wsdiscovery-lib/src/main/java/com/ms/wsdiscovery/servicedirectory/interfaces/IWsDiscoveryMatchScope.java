/*
IMatchScope.java

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
package com.ms.wsdiscovery.servicedirectory.interfaces;

import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;

/**
 * Interface implemented by matching algorithms. New algorithms
 * must be added to {@link MatchBy}. The default MatchBy-algorithm
 * (used when MatchBy in {@link ScopesType} is empty or <code>null</code>) is
 * specified in {@link WsDiscoveryConstants#defaultMatchBy}. The 
 * WS-Discovery specification defaults to {@link MatchBy#RFC2396}.
 * 
 * @author Magnus Skjegstad
 */
public interface IWsDiscoveryMatchScope {
    /**
     * Match a target service with a probe.
     * 
     * @param target Target service description.
     * @param probeScopes Scopes in probe.
     * @return True if there's a match.
     */
    public boolean matchScope(WsDiscoveryService target, WsDiscoveryScopesType probeScopes);
}
