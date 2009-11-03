/*
SOAPOverUDPWSA200508Message.java

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
import com.skjegstad.soapoverudp.jaxb.wsaddressing200508.AttributedURIType;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200508.EndpointReferenceType;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200508.MetadataType;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200508.ObjectFactory;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200508.ReferenceParametersType;
import com.skjegstad.soapoverudp.jaxb.wsaddressing200508.RelatesToType;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;

/**
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPWSA200508Message extends SOAPOverUDPMessage {

    public SOAPOverUDPWSA200508Message(SOAPMessage message) throws SOAPOverUDPException {
        super(message);
    }

    public SOAPOverUDPWSA200508Message(String soapAsXML, String soapProtocol, Charset encoding) throws SOAPOverUDPException {
        super(soapAsXML, soapProtocol, encoding);
    }

    public SOAPOverUDPWSA200508Message(String soapProtocol, Charset encoding) throws SOAPOverUDPException {
        super(soapProtocol, encoding);
    }

    private void removeWSAHeader() throws SOAPOverUDPException {
        JAXBElement j = null;
        Unmarshaller u = SOAPOverUDPNamespaces.WS_ADDRESSING_2005_08.getUnmarshaller();
        SOAPHeader header;
        try {
            header = soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to get SOAP header", ex);
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

            if (j.getValue().getClass().equals(AttributedURIType.class)) {
                if (tag.equalsIgnoreCase("To") ||
                    tag.equalsIgnoreCase("Action") ||
                    tag.equalsIgnoreCase("MessageID"))
                        headerElement.detachNode();                
            } else
            if (j.getValue().getClass().equals(RelatesToType.class)) {
                if (tag.equalsIgnoreCase("RelatesTo")) {
                    headerElement.detachNode();
                }
            } else
            if (j.getValue().getClass().equals(EndpointReferenceType.class)) {
                if (tag.equalsIgnoreCase("ReplyTo"))
                    headerElement.detachNode();
            }
        }
    }

    @Override
    protected void saveWSAHeader() throws SOAPOverUDPException {
        if (!this.saveRequired) // only save header if any changes have been made
            return;

        // Remove old header
        removeWSAHeader();

        // Instantiate JAXB marshaller
        String namespace = SOAPOverUDPNamespaces.WS_ADDRESSING_2005_08.getNamespace();
        Marshaller m = SOAPOverUDPNamespaces.WS_ADDRESSING_2005_08.getMarshaller();
        SOAPHeader header;
        try {
            header = soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read header from SOAP message", ex);
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
                AttributedURIType a = o.createAttributedURIType();
                a.setValue(this.getTo().toString());
                m.marshal(o.createTo(a),
                        header);
            }
            if (this.getAction() != null) {
                AttributedURIType a = o.createAttributedURIType();
                a.setValue(this.getAction().toString());
                m.marshal(o.createAction(a),
                        header);
            }
            if (this.getRelatesTo() != null) {
                RelatesToType r = o.createRelatesToType();
                r.setRelationshipType(this.getRelationshipType());
                r.setValue(this.getRelatesTo().toString());

                m.marshal(o.createRelatesTo(r),
                        header);
            }
            if (this.getReplyTo() != null) {
                SOAPOverUDPEndpointReferenceType ep = this.getReplyTo();

                EndpointReferenceType reply = o.createEndpointReferenceType();

                // Address
                if (ep.getAddress() != null) {
                    AttributedURIType address = o.createAttributedURIType();
                    address.setValue(ep.getAddress().toString());
                    reply.setAddress(address);
                }

                // Metadata
                if (ep.getMetadata() != null) {
                    MetadataType metadata = o.createMetadataType();
                    metadata.getAny().addAll(ep.getMetadata().getAny());
                    reply.setMetadata(metadata);
                }

                // Reference parameters
                if (ep.getReferenceParameters() != null) {
                    ReferenceParametersType r = o.createReferenceParametersType();
                    r.getAny().addAll(ep.getReferenceParameters().getAny());
                    reply.setReferenceParameters(r);
                }

                // Anything else
                reply.getAny().addAll(ep.getAny());

                m.marshal(o.createReplyTo(reply),
                        header);
            }
            if (this.getMessageId() != null) {
                AttributedURIType a = o.createAttributedURIType();
                a.setValue(this.getMessageId().toString());
                m.marshal(o.createMessageID(a),
                        header);
            }
        } catch (JAXBException ex) {
            throw new SOAPOverUDPException("Unable to add WS-Addressing to SOAP header.");
        }
    }
        
}
