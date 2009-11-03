/*
WsDiscoveryServiceNameType.java

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
package com.skjegstad.soapoverudp.datatypes;

import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPGenericOtherAttributesType;
import javax.xml.namespace.QName;

/**
 * Generic implementation of ServiceNameType from WS-Addressing 2004/08.
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPServiceNameType extends SOAPOverUDPGenericOtherAttributesType implements Cloneable {
    protected QName value;
    protected String portName;

    public SOAPOverUDPServiceNameType(QName value, String portName) {
        this.value = value;
        this.portName = portName;
    }

    public SOAPOverUDPServiceNameType() {
    }
    

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public QName getValue() {
        return value;
    }

    public void setValue(QName value) {
        this.value = value;
    }

    @Override
    protected Object clone() {
        SOAPOverUDPServiceNameType n = new SOAPOverUDPServiceNameType();

        // Portname
        if (this.getPortName() != null)
            n.setPortName(new String(this.getPortName()));

        // Value
        if (this.getValue() != null)
            n.setValue(new QName(this.getValue().getNamespaceURI(), this.getValue().getLocalPart(), this.getValue().getPrefix()));

        // Other attributes
        SOAPOverUDPGenericOtherAttributesType at = (SOAPOverUDPGenericOtherAttributesType) super.clone();
        n.setOtherAttributes(at.getOtherAttributes());

        return n;

    }

    
}
