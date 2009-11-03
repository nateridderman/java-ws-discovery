/*
SOAPOverUDPWSA200408Message.java

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
package com.skjegstad.soapoverudp.messages;

import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPNamespaces;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPEndpointReferenceType;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.AttributedQName;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.AttributedURI;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.EndpointReferenceType;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.ObjectFactory;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.ReferenceParametersType;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.ReferencePropertiesType;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.Relationship;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200408.ServiceNameType;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;

/**
 * SOAP-over-UDP message using WS-Addressing 2004/08.
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPWSA200408Message extends SOAPOverUDPMessage {

    public SOAPOverUDPWSA200408Message() {
        super();
    }

    public SOAPOverUDPWSA200408Message(SOAPMessage message) throws SOAPOverUDPException {
        super(message);
    }

    public SOAPOverUDPWSA200408Message(String soapAsXML, String soapProtocol, Charset encoding) throws SOAPOverUDPException {
        super(soapAsXML, soapProtocol, encoding);
    }

    public SOAPOverUDPWSA200408Message(String soapProtocol, Charset encoding) throws SOAPOverUDPException {
        super(soapProtocol, encoding);
    }

    private void removeWSAHeader() throws SOAPOverUDPException {
        JAXBElement j = null;
        Unmarshaller u = SOAPOverUDPNamespaces.WS_ADDRESSING_2004_08.getUnmarshaller();
        SOAPHeader header;
        try {
            header = soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to get SOAP header");
        }

        // Loop through header and remove WSA elements
        for (Iterator<SOAPHeaderElement> i = header.examineAllHeaderElements(); i.hasNext();) {
            SOAPHeaderElement headerElement = i.next();

            try {
                // Unmarshal the header element
                j = (JAXBElement) u.unmarshal(headerElement);
            } catch (UnmarshalException ex) {
                continue; // unknown element probably, just continue
            } catch (JAXBException ex) {
                throw new SOAPOverUDPException("An unexpected error occured while unmarshalling header element " + headerElement.getTagName(), ex);
            }

            String tag = j.getName().getLocalPart();

            if (j.getValue().getClass().equals(AttributedURI.class)) {
                if (tag.equalsIgnoreCase("To") ||
                        tag.equalsIgnoreCase("Action") ||
                        tag.equalsIgnoreCase("MessageID")) {
                    headerElement.detachNode();
                }
            } else if (j.getValue().getClass().equals(Relationship.class)) {
                if (tag.equalsIgnoreCase("RelatesTo")) {
                    headerElement.detachNode();
                }
            } else if (j.getValue().getClass().equals(EndpointReferenceType.class)) {
                if (tag.equalsIgnoreCase("ReplyTo")) {
                    headerElement.detachNode();
                }
            }
        }
    }

    @Override
    protected void saveWSAHeader() throws SOAPOverUDPException {
        if (!this.saveRequired) // only save header if any changes have been made
        {
            return;
        }

        // Remove old header
        removeWSAHeader();

        // Instantiate JAXB marshaller
        String namespace = SOAPOverUDPNamespaces.WS_ADDRESSING_2004_08.getNamespace();
        Marshaller m = SOAPOverUDPNamespaces.WS_ADDRESSING_2004_08.getMarshaller();

        SOAPHeader header;
        try {
            header = soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read body and header from SOAP message", ex);
        }

        // Add WSA namespace to header
        {
            boolean found = false;
            for (Iterator<String> i = header.getVisibleNamespacePrefixes(); i.hasNext();) {
                String s = i.next();
                String ns = header.getNamespaceURI(s);
                if (ns.equalsIgnoreCase(namespace)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                try {
                    // TODO make sure the prefix is unique
                    header.addNamespaceDeclaration("wsa", namespace);
                } catch (SOAPException ex) {
                    throw new SOAPOverUDPException("Unable to add namespace " + namespace + " to header.", ex);
                }
            }
        }


        // Create other header-elements
        ObjectFactory o = new ObjectFactory();
        try {
            if (this.getTo() != null) {
                AttributedURI a = o.createAttributedURI();
                a.setValue(this.getTo().toString());
                m.marshal(o.createTo(a),
                        header);
            }
            if (this.getAction() != null) {
                AttributedURI a = o.createAttributedURI();
                a.setValue(this.getAction().toString());
                m.marshal(o.createAction(a),
                        header);
            }
            if (this.getRelatesTo() != null) {
                Relationship r = o.createRelationship();
                if (this.getRelationshipType() != null)
                    r.setRelationshipType(QName.valueOf(this.getRelationshipType()));
                r.setValue(this.getRelatesTo().toString());

                m.marshal(o.createRelatesTo(r),
                        header);
            }
            if (this.getReplyTo() != null) {
                SOAPOverUDPEndpointReferenceType ep = this.getReplyTo();

                EndpointReferenceType reply = o.createEndpointReferenceType();

                // Address
                if (ep.getAddress() != null) {
                    AttributedURI address = o.createAttributedURI();
                    address.setValue(ep.getAddress().toString());
                    reply.setAddress(address);
                }

                // Port type
                if (ep.getPortType() != null) {
                    AttributedQName q = o.createAttributedQName();
                    q.setValue(ep.getPortType());
                    
                }

                // Reference parameters
                if (ep.getReferenceParameters() != null) {
                    ReferenceParametersType r = o.createReferenceParametersType();
                    r.getAny().addAll(ep.getReferenceParameters().getAny());
                    reply.setReferenceParameters(r);
                }

                // Reference properties
                if (ep.getReferenceProperties() != null) {
                    ReferencePropertiesType r = o.createReferencePropertiesType();
                    r.getAny().addAll(ep.getReferenceParameters().getAny());
                    reply.setReferenceProperties(r);
                }

                // Service name
                if (ep.getServiceName() != null) {
                    ServiceNameType s = o.createServiceNameType();
                    s.setPortName(ep.getServiceName().getPortName());
                    s.setValue(ep.getServiceName().getValue());
                    reply.setServiceName(s);
                }

                // Anything else
                reply.getAny().addAll(ep.getAny());

                m.marshal(o.createReplyTo(reply),
                        header);
            }
            if (this.getMessageId() != null) {
                AttributedURI a = o.createAttributedURI();
                a.setValue(this.getMessageId().toString());
                m.marshal(o.createMessageID(a),
                        header);
            }
        } catch (JAXBException ex) {
            throw new SOAPOverUDPException("Unable to add WS-Addressing to SOAP header.");
        }
     }
}
