/*
WsDiscoveryTransportTypes.java

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

import com.skjegstad.soapoverudp.SOAPOverUDP11;
import com.skjegstad.soapoverudp.SOAPOverUDPdraft2004;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import com.skjegstad.soapoverudp.transport.SOAPOverUDPTransport;
import com.skjegstad.soapoverudp.transport.SOAPOverUDPTransportZlib;
import com.skjegstad.soapoverudp.transport.SOAPOverUDPTransportEfx;

/**
 * Contains supported transport protocols. Transport types must implement 
 * {@link ITransportType} and contain a constructor without parameters. 
 * 
 * @author Magnus Skjegstad
 */
public enum WsDiscoveryTransportTypes {
    /**
     * Plain SOAP-over-UDP. See {@link SOAPOverUDPTransport}.
     */
    UNCOMPRESSED(SOAPOverUDPTransport.class, "Uncompressed"),
    /**
     * Compressed version of SOAP-over-UDP. See {@link SOAPOverUDPTransportZlib}.
     */
    COMPRESSED_ZLIB(SOAPOverUDPTransportZlib.class, "ZLIB"),
    COMPRESSED_EFX(SOAPOverUDPTransportEfx.class, "EFX");

    private final Class transportLayer;
    private final String friendlyName;
    
    /**
     * Get a new instance of this transport type.
     * 
     * @return New instance.
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */
    public ISOAPOverUDPTransport newInstance() throws InstantiationException, IllegalAccessException {
        return (ISOAPOverUDPTransport)transportLayer.newInstance();
    }

    public String getFriendlyName() {
        return friendlyName;
    }        
        
    WsDiscoveryTransportTypes(Class layer, String friendlyName) {
        this.transportLayer = layer;
        this.friendlyName = friendlyName;
    }
}
