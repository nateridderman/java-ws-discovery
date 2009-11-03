/*
SOAPOverUDPNamespaces.java

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
package com.skjegstad.soapoverudp.datatypes;

import com.skjegstad.soapoverudp.*;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

/**
 * WS-Addressing namespaces supported by SOAP-over-UDP.
 *
 * @author Magnus Skjegstad
 */
public enum SOAPOverUDPNamespaces {
    WS_ADDRESSING_2004_08 (
            "http://schemas.xmlsoap.org/ws/2004/08/addressing",
            com.skjegstad.soapoverudp.jaxb.wsaddressing200408.EndpointReferenceType.class.getPackage().getName()),
    WS_ADDRESSING_2005_08 (
            "http://www.w3.org/2005/08/addressing",
            com.skjegstad.soapoverudp.jaxb.wsaddressing200508.EndpointReferenceType.class.getPackage().getName());

    private final String namespace;
    private final String contextPath;
    
    private JAXBContext jaxbContext = null;
    private Marshaller marshaller = null;
    private Unmarshaller unmarshaller = null;

    SOAPOverUDPNamespaces(String namespace, String contextPath) {
        this.namespace = namespace;
        this.contextPath = contextPath;
    }

    public JAXBContext getJAXBContext() throws SOAPOverUDPException {
        if (jaxbContext == null) // only do this once
            jaxbContext = SOAPOverUDPUtilities.createJAXBContext(this.contextPath);

        return jaxbContext;
    }

    public Marshaller getMarshaller() throws SOAPOverUDPException {
        if (marshaller == null) // only do this once
            marshaller = SOAPOverUDPUtilities.createMarshaller(this.getJAXBContext());
        return marshaller;
    }

    public Unmarshaller getUnmarshaller() throws SOAPOverUDPException {
        if (unmarshaller == null) // only do this once
            unmarshaller = SOAPOverUDPUtilities.createUnmarshaller(this.getJAXBContext());
        return unmarshaller;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getContextPath() {
        return contextPath;
    }
}
