/*
SOAPOverUDP11.java

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
package com.skjegstad.soapoverudp;

import com.skjegstad.soapoverudp.configurations.SOAPOverUDPConfiguration;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPNamespaces;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDP;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPNetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import com.skjegstad.soapoverudp.messages.SOAPOverUDPWSA200508Message;
import com.skjegstad.soapoverudp.transport.SOAPOverUDPTransport;
import java.net.URI;
import java.nio.charset.Charset;
import javax.xml.soap.SOAPConstants;

/**
 * SOAPOverUDP configured to work as specified in the standard, v 1.1.
 * http://docs.oasis-open.org/ws-dd/soapoverudp/1.1/wsdd-soapoverudp-1.1-spec.html
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDP11 extends SOAPOverUDP implements ISOAPOverUDP {
    private URI anonymousTo = URI.create(SOAPOverUDPNamespaces.WS_ADDRESSING_2005_08.getNamespace() + "/role/anonymous");

    public SOAPOverUDP11(ISOAPOverUDPTransport transportLayer, Charset encoding) {
        soapConfig = new SOAPOverUDPConfiguration();

        soapConfig.setMulticastUDPRepeat(2);
        soapConfig.setUnicastUDPRepeat(1);
        soapConfig.setUDPUpperDelay(500);
        soapConfig.setUDPMaxDelay(250);
        soapConfig.setUDPMinDelay(50);

        this.setTransport(transportLayer);
        this.getTransport().setConfiguration(soapConfig);
        
        this.setEncoding(encoding);
    }

    public ISOAPOverUDPMessage createSOAPOverUDPMessageFromXML(String soapAsXML) throws SOAPOverUDPException {
        return new SOAPOverUDPWSA200508Message(soapAsXML, SOAPConstants.SOAP_1_2_PROTOCOL, encoding);
    }

    public ISOAPOverUDPMessage createSOAPOverUDPMessage() throws SOAPOverUDPException {
        return new SOAPOverUDPWSA200508Message(SOAPConstants.SOAP_1_2_PROTOCOL, encoding);
    }
    
}
