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
import com.skjegstad.soapoverudp.generic.SOAPOverUDPGeneric;
import com.skjegstad.soapoverudp.interfaces.ISOAPTransport;

/**
 * SOAPOverUDP configured to work as specified in the standard, v 1.1.
 * http://docs.oasis-open.org/ws-dd/soapoverudp/1.1/wsdd-soapoverudp-1.1-spec.html
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDP11 extends SOAPOverUDPGeneric implements ISOAPTransport {

    public SOAPOverUDP11() {
        super();
        SOAPOverUDPConfiguration c = new SOAPOverUDPConfiguration();

        c.setMulticastUDPRepeat(2);
        c.setUnicastUDPRepeat(1);
        c.setUDPUpperDelay(500);
        c.setUDPMaxDelay(250);
        c.setUDPMinDelay(50);

        this.setConfiguration(c);
    }
}
