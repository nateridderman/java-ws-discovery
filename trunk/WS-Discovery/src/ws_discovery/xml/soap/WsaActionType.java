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

package ws_discovery.xml.soap;

import ws_discovery.xml.jaxb_generated.AttributedURI;

/**
 * Valid JAXB ActionTypes used for WS-Addressing.
 * 
 * @author Magnus Skjegstad
 */
public enum WsaActionType {
    /**
     * Hello-message
     */
    HELLO          ("http://schemas.xmlsoap.org/ws/2005/04/discovery/Hello"),
    /**
     * Bye-message
     */
    BYE            ("http://schemas.xmlsoap.org/ws/2005/04/discovery/Bye"),
    /**
     * Probe-message
     */
    PROBE          ("http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe"),
    /**
     * ProbeMatches-message
     */
    PROBEMATCHES   ("http://schemas.xmlsoap.org/ws/2005/04/discovery/ProbeMatches"),
    /**
     * Resolve-message
     */
    RESOLVE        ("http://schemas.xmlsoap.org/ws/2005/04/discovery/Resolve"),
    /**
     * ResolveMatches-message
     */
    RESOLVEMATCHES("http://schemas.xmlsoap.org/ws/2005/04/discovery/ResolveMatches");
       
    private final AttributedURI action;
    
    /**
     * Return the ActionType as an AttributedURI.
     * @return ActionType represented as AttributedURI.
     */
    public AttributedURI toAttributedURI() {
        return action;
    }
       
    @Override
    public String toString() {
        return action.getValue();
    }
    
    WsaActionType(String action) {
        this.action = new AttributedURI();
        this.action.setValue(action);
    }
}
