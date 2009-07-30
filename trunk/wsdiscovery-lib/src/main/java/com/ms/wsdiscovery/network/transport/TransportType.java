/*
TransportType.java

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

package com.ms.wsdiscovery.network.transport;

import com.ms.wsdiscovery.network.transport.soapudp.SOAPOverUDP;
import com.ms.wsdiscovery.network.transport.soapudp.zlib.SOAPOverUDPzlib;

/**
 * Contains supported transport protocols. Transport types must implement 
 * {@link ITransportType} and contain a constructor without parameters. 
 * 
 * @author Magnus Skjegstad
 */
public enum TransportType {
    /**
     * Plain SOAP-over-UDP. See {@link SOAPOverUDP}.
     */
    SOAP_OVER_UDP(SOAPOverUDP.class),
    /**
     * Compressed version of SOAP-over-UDP. See {@link SOAPOverUDPzlib}.
     */
    SOAP_OVER_UDP_ZLIB(SOAPOverUDPzlib.class);
    
        
    private final Class networkLayer;
    
    /**
     * Get a new instance of this transport type.
     * 
     * @return New instance.
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */
    public ITransportType newInstance() throws InstantiationException, IllegalAccessException {
        return (ITransportType)networkLayer.newInstance();
    }
        
    TransportType(Class layer) {
        networkLayer = layer;
    }
}
