/*
WsaActionType.java

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

package com.ms.wsdiscovery.datatypes;

import com.ms.wsdiscovery.WsDiscoveryConstants;
import java.net.URI;

/**
 * Valid JAXB ActionTypes used for WS-Addressing.
 * 
 * @author Magnus Skjegstad
 */
public enum WsDiscoveryActionTypes {
    /**
     * Hello-message
     */
    HELLO          (WsDiscoveryConstants.defaultNsDiscovery.getWsDiscoveryNamespace() + "/Hello"),
    /**
     * Bye-message
     */
    BYE            (WsDiscoveryConstants.defaultNsDiscovery.getWsDiscoveryNamespace() + "/Bye"),
    /**
     * Probe-message
     */
    PROBE          (WsDiscoveryConstants.defaultNsDiscovery.getWsDiscoveryNamespace() + "/Probe"),
    /**
     * ProbeMatches-message
     */
    PROBEMATCHES   (WsDiscoveryConstants.defaultNsDiscovery.getWsDiscoveryNamespace() + "/ProbeMatches"),
    /**
     * Resolve-message
     */
    RESOLVE        (WsDiscoveryConstants.defaultNsDiscovery.getWsDiscoveryNamespace() + "/Resolve"),
    /**
     * ResolveMatches-message
     */
    RESOLVEMATCHES(WsDiscoveryConstants.defaultNsDiscovery.getWsDiscoveryNamespace() + "/ResolveMatches");
       
    private final URI action;
    
    /**
     * Return the ActionType as an AttributedURI.
     * @return ActionType represented as AttributedURI.
     */
    public URI toURI() {
        return action;
    }
       
    @Override
    public String toString() {
        return action.toString();
    }
    
    WsDiscoveryActionTypes(String action) {
        this.action = URI.create(action);
    }
}
