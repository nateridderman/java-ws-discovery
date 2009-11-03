/*
SOAPOverUDPUtilities.java

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
package com.skjegstad.soapoverudp;

import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPEndpointReferenceType;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPGenericAnyType;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPServiceNameType;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import java.net.URI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

/**
 * Static helper methods used by SOAP-over-UDP
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPUtilities {
    /**
     * Create new JAXB instance.
     * @param JAXB context instance name
     * @return JAXB instance.
     * @throws SOAPOverUDPException
     */
    public static JAXBContext createJAXBContext(String contextPath) throws SOAPOverUDPException {
        try {
            return JAXBContext.newInstance(contextPath);
        } catch (JAXBException ex) {
            throw new SOAPOverUDPException("Unable to create JAXB instance: " + contextPath, ex);
        }
    }
    /**
     * Create new JAXB marshaller.
     * @return JAXB marshaller.
     * @throws SOAPOverUDPException
     */
    public static Marshaller createMarshaller(JAXBContext jaxbContext) throws SOAPOverUDPException {
        Marshaller m;
        try {
            m = jaxbContext.createMarshaller();
        } catch (JAXBException ex) {
            throw new SOAPOverUDPException("Unable to create new instance of JAXB marshaller.", ex);
        }
        try {
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        } catch (PropertyException ex) {
            throw new SOAPOverUDPException("Unable to set JAXB marshaller property JAXB_FORMATTED_OUTPUT.", ex);
        }

        return m;
    }

    /**
     * Create new JAXB unmarshaller.
     * @return JAXB unmarshaller.
     * @throws SOAPOverUDPException
     */
    public static Unmarshaller createUnmarshaller(JAXBContext jaxbContext) throws SOAPOverUDPException {
        Unmarshaller u;
        try {
            u = jaxbContext.createUnmarshaller();
        } catch (JAXBException ex) {
            throw new SOAPOverUDPException("Unable to create new instance of JAXB unmarshaller.", ex);
        }

        return u;
    }

    /**
     * Create a {@link SOAPOverUDPEndpointReferenceType} object with values from a {@link com.skjegstad.soapoverudp.jaxb.wsaddressing200408.EndpointReferenceType} object.
     * @param endpointReference JAXB object
     * @return The endpoint reference from <code>endpointReference</code> represented as a {@link SOAPOverUDPEndpointReferenceType}.
     */
    public static SOAPOverUDPEndpointReferenceType createSOAPOverUDPEndpointReferenceType(com.skjegstad.soapoverudp.jaxb.wsaddressing200408.EndpointReferenceType endpointReference) {
        SOAPOverUDPEndpointReferenceType s = new SOAPOverUDPEndpointReferenceType();

        if (endpointReference.getAddress() != null)
            s.setAddress(URI.create(endpointReference.getAddress().getValue()));

        if (endpointReference.getPortType() != null)
            s.setPortType(endpointReference.getPortType().getValue());

        if (endpointReference.getServiceName() != null)
            s.setServiceName(new SOAPOverUDPServiceNameType(endpointReference.getServiceName().getValue(), endpointReference.getServiceName().getPortName()));

        if (endpointReference.getReferenceParameters() != null)
            s.setReferenceParameters(new SOAPOverUDPGenericAnyType(endpointReference.getReferenceParameters().getAny()));

        if (endpointReference.getReferenceProperties() != null)
            s.setReferenceProperties(new SOAPOverUDPGenericAnyType(endpointReference.getReferenceProperties().getAny()));

        if (endpointReference.getOtherAttributes() != null)
            s.setOtherAttributes(endpointReference.getOtherAttributes());

        return s;
    }

    /**
     * Create a {@link SOAPOverUDPEndpointReferenceType} object with values from a {@link com.skjegstad.soapoverudp.jaxb.wsaddressing200508.EndpointReferenceType} object.
     * @param endpointReference JAXB object
     * @return The endpoint reference from <code>endpointReference</code> represented as a {@link SOAPOverUDPEndpointReferenceType}.
     */
    public static SOAPOverUDPEndpointReferenceType createSOAPOverUDPEndpointReferenceType(com.skjegstad.soapoverudp.jaxb.wsaddressing200508.EndpointReferenceType endpointReference) {
        SOAPOverUDPEndpointReferenceType s = new SOAPOverUDPEndpointReferenceType();

        if (endpointReference.getAddress() != null)
            s.setAddress(URI.create(endpointReference.getAddress().getValue()));        
        if (endpointReference.getReferenceParameters() != null)
            s.setReferenceParameters(new SOAPOverUDPGenericAnyType(endpointReference.getReferenceParameters().getAny()));
        if (endpointReference.getOtherAttributes() != null)
            s.setOtherAttributes(endpointReference.getOtherAttributes());
        if (endpointReference.getMetadata() != null)
            s.setMetadata(new SOAPOverUDPGenericAnyType(endpointReference.getMetadata().getAny(), endpointReference.getMetadata().getOtherAttributes()));

        return s;
    }

}
