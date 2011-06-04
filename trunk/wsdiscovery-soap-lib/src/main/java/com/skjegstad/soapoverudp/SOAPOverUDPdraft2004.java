/*
SOAPOverUDPdraft2004.java

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
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDP;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import com.skjegstad.soapoverudp.messages.SOAPOverUDPWSA200408Message;
import java.nio.charset.Charset;
import javax.xml.soap.SOAPConstants;

/**
 * SOAPOverUDP configured to work as specified in specification draft from 2004.
 * http://specs.xmlsoap.org/ws/2004/09/soap-over-udp/soap-over-udp.pdf
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPdraft2004 extends SOAPOverUDP implements ISOAPOverUDP {
    public SOAPOverUDPdraft2004(ISOAPOverUDPTransport transportLayer, Charset encoding) {
        super();

        this.soapConfig = new SOAPOverUDPConfiguration();

        soapConfig.setMulticastUDPRepeat(4);
        soapConfig.setUnicastUDPRepeat(2);
        soapConfig.setUDPUpperDelay(500);
        soapConfig.setUDPMaxDelay(250);
        soapConfig.setUDPMinDelay(50);

        this.setTransport(transportLayer);
        this.getTransport().setConfiguration(soapConfig);
        
        this.setEncoding(encoding);
    }

    public ISOAPOverUDPMessage createSOAPOverUDPMessageFromXML(String soapAsXML) throws SOAPOverUDPException {
        return new SOAPOverUDPWSA200408Message(soapAsXML, SOAPConstants.SOAP_1_2_PROTOCOL, encoding);
    }

    public ISOAPOverUDPMessage createSOAPOverUDPMessage() throws SOAPOverUDPException {
        return new SOAPOverUDPWSA200408Message(SOAPConstants.SOAP_1_2_PROTOCOL, encoding);
    }
}
