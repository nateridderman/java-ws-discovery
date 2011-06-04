/*
SOAPOverUDPMessage.java

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

package com.skjegstad.soapoverudp.messages;

import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPMessage;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPWsAddressingNamespaces;
import com.skjegstad.soapoverudp.SOAPOverUDPUtilities;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPEndpointReferenceType;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

/**
 * Class representing a SOAP-over-UDP message.
 *
 * @author Magnus Skjegstad
 */

public abstract class SOAPOverUDPMessage implements ISOAPOverUDPMessage {
    /**
     * Namespaces used in the SOAP message. These are added to the
     * header. The first String is the prefix, the second is the URI.
     */
    protected Map<String,URI> namespaces = new HashMap<String,URI>();

    /**
     * WS-Addressing ReplyTo
     */
    protected SOAPOverUDPEndpointReferenceType replyTo = null;

    /**
     * WS-Addressing Message ID
     */
    protected URI messageId = URI.create("urn:uuid:"+UUID.randomUUID());
    /**
     * WS-Addressing To
     */
    protected URI to;
    /**
     * WS-Addressing Action
     */
    protected URI action = null;
    /**
     * WS-Addressing RelatesTo
     */
    protected URI relatesTo;
    protected String relationshipType;
    boolean saveRequired = false;

    protected InetAddress srcAddress, dstAddress;
    protected int srcPort, dstPort;

    /**
     * SOAP message (envelope, body, header)
     */
    protected SOAPMessage soapMessage;

    public SOAPOverUDPMessage(String soapProtocol, Charset encoding) throws SOAPOverUDPException {
        this(null, soapProtocol, encoding);
    }

    public SOAPOverUDPMessage() {
        super();
    }

    public SOAPOverUDPMessage(String soapAsXML, String soapProtocol, Charset encoding) throws SOAPOverUDPException {
        MessageFactory factory;
        SOAPMessage message = null;
        try {
            factory = MessageFactory.newInstance(soapProtocol);
            message = factory.createMessage();                     
            if (soapAsXML != null) {
                ByteArrayInputStream i = new ByteArrayInputStream(soapAsXML.getBytes(encoding));
                message.getSOAPPart().setContent(new StreamSource(i));
                this.soapMessage = message;
                this.readWSAHeader(); // read header
            } else
                this.soapMessage = message;
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to create SOAP message.");
        }
    }

    public SOAPOverUDPMessage(SOAPMessage message) throws SOAPOverUDPException {
        this.setSOAPMessage(message);
    }

    public void setSOAPMessage(SOAPMessage soapMessage) throws SOAPOverUDPException {
        this.soapMessage = soapMessage;
        readWSAHeader();
    }

    public SOAPBody getSOAPBody() throws SOAPOverUDPException {
        this.saveChanges();
        try {
            return this.soapMessage.getSOAPBody();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read SOAP body.", ex);
        }
    }

    public SOAPHeader getSOAPHeader() throws SOAPOverUDPException {
        this.saveChanges();
        try {
            return this.soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read SOAP header.", ex);
        }
    }
    
    public SOAPPart getSOAPPart() throws SOAPOverUDPException {
        this.saveChanges();
        return this.soapMessage.getSOAPPart();
    }


    public SOAPMessage getSOAPMessage() throws SOAPOverUDPException {
        this.saveChanges();
        return soapMessage;
    }

    public boolean saveRequired() {
        return saveRequired;
    }

    protected void setSaveRequired() {
        this.saveRequired = true;
    }

    public void saveChanges() throws SOAPOverUDPException {
        // store everything
        if (this.saveRequired) {
            saveWSAHeader();
            saveRequired = false;
        }
        /*if (soapMessage.saveRequired()) {
            try {
                soapMessage.saveChanges();
            } catch (SOAPException ex) {
                throw new SOAPOverUDPException("Unable to save SOAP message", ex);
            }
        }*/
    }

    protected abstract void saveWSAHeader() throws SOAPOverUDPException;

    /**
     * Read SOAP header containing WS-Addressing. Determines WS-Addressing version
     * by looking at the namespaces declared in the SOAP header. Latest version is
     * checked first.
     *
     * @param soap SOAP message.
     * @throws SOAPOverUDPException on error.
     */
    protected void readWSAHeader() throws SOAPOverUDPException {
        if (this.getHeaderNamespaceByURI(SOAPOverUDPWsAddressingNamespaces.WS_ADDRESSING_2005_08.getNamespace()) != null)
            readWSA200508Header();
        else
        if (this.getHeaderNamespaceByURI(SOAPOverUDPWsAddressingNamespaces.WS_ADDRESSING_2004_08.getNamespace()) != null)
            readWSA200408Header();
        else
            throw new SOAPOverUDPException("WS-Addressing not found in header.");
    }

    /**
     * Read SOAP header containing WS-Addressing 2004/08 elements.
     *
     * @param soap SOAP message.
     * @throws SOAPOverUDPException
     */
    private void readWSA200408Header() throws SOAPOverUDPException {
        JAXBElement j = null;
        Unmarshaller u = SOAPOverUDPWsAddressingNamespaces.WS_ADDRESSING_2004_08.getUnmarshaller();

        SOAPHeader header;
        try {
            header = soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read WS-Addressing 2004/08 SOAP header", ex);
        }

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

            if (j.getValue().getClass().equals(com.skjegstad.soapoverudp.jaxb.wsaddressing200408.AttributedURI.class)) {
                com.skjegstad.soapoverudp.jaxb.wsaddressing200408.AttributedURI a =
                        (com.skjegstad.soapoverudp.jaxb.wsaddressing200408.AttributedURI)j.getValue();
                if (tag.equalsIgnoreCase("To"))
                    to = URI.create(a.getValue());
                else
                if (tag.equalsIgnoreCase("Action"))
                    action = URI.create(a.getValue());
                else
                if (tag.equalsIgnoreCase("MessageID"))
                    messageId = URI.create(a.getValue());
            } else
            if (j.getValue().getClass().equals(com.skjegstad.soapoverudp.jaxb.wsaddressing200408.Relationship.class)) {
                com.skjegstad.soapoverudp.jaxb.wsaddressing200408.Relationship r =
                        (com.skjegstad.soapoverudp.jaxb.wsaddressing200408.Relationship)j.getValue();
                if (tag.equalsIgnoreCase("RelatesTo")) {
                    relatesTo = URI.create(r.getValue());
                    if (r.getRelationshipType() != null)
                        relationshipType = r.getRelationshipType().toString();
                }
            } else
            if (j.getValue().getClass().equals(com.skjegstad.soapoverudp.jaxb.wsaddressing200408.EndpointReferenceType.class)) {
                com.skjegstad.soapoverudp.jaxb.wsaddressing200408.EndpointReferenceType e =
                        (com.skjegstad.soapoverudp.jaxb.wsaddressing200408.EndpointReferenceType)j.getValue();
                if (tag.equalsIgnoreCase("ReplyTo"))
                    replyTo = SOAPOverUDPUtilities.createSOAPOverUDPEndpointReferenceType(e);
            }

        }
    }

    /*
     * Read SOAP header containing elements from WS-Addressing 2005/08
     *
     * @param soap SOAP message.
     * @throws SOAPOverUDPException
     */
    private void readWSA200508Header() throws SOAPOverUDPException {
        JAXBElement j = null;
        Unmarshaller u = SOAPOverUDPWsAddressingNamespaces.WS_ADDRESSING_2005_08.getUnmarshaller();

        SOAPHeader header;
        try {
            header = soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read WS-Addressing 2005/08 SOAP header", ex);
        }

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

            if (j.getValue().getClass().equals(com.skjegstad.soapoverudp.jaxb.wsaddressing200508.AttributedURIType.class)) {
                com.skjegstad.soapoverudp.jaxb.wsaddressing200508.AttributedURIType a =
                        (com.skjegstad.soapoverudp.jaxb.wsaddressing200508.AttributedURIType)j.getValue();
                if (tag.equalsIgnoreCase("To"))
                    to = URI.create(a.getValue());
                else
                if (tag.equalsIgnoreCase("Action"))
                    action = URI.create(a.getValue());
                else
                if (tag.equalsIgnoreCase("MessageID"))
                    messageId = URI.create(a.getValue());
            } else
            if (j.getValue().getClass().equals(com.skjegstad.soapoverudp.jaxb.wsaddressing200508.RelatesToType.class)) {
                com.skjegstad.soapoverudp.jaxb.wsaddressing200508.RelatesToType r =
                        (com.skjegstad.soapoverudp.jaxb.wsaddressing200508.RelatesToType)j.getValue();
                if (tag.equalsIgnoreCase("RelatesTo")) {
                    relatesTo = URI.create(r.getValue());                    
                    relationshipType = r.getRelationshipType();
                }
            } else
            if (j.getValue().getClass().equals(com.skjegstad.soapoverudp.jaxb.wsaddressing200508.EndpointReferenceType.class)) {
                com.skjegstad.soapoverudp.jaxb.wsaddressing200508.EndpointReferenceType e =
                        (com.skjegstad.soapoverudp.jaxb.wsaddressing200508.EndpointReferenceType)j.getValue();
                if (tag.equalsIgnoreCase("ReplyTo"))
                    replyTo = SOAPOverUDPUtilities.createSOAPOverUDPEndpointReferenceType(e);
            }

        }
    }

    /**
     * Add namespace to SOAP message envelope.
     * @param prefix Prefix to use for this namespace.
     * @param uri Namespace URI. */
     
    protected void addEnvelopeNamespace(String prefix, URI uri) throws SOAPOverUDPException {
        try {
            soapMessage.getSOAPPart().getEnvelope().removeNamespaceDeclaration(prefix); // TODO is this redundant?
            soapMessage.getSOAPPart().getEnvelope().addNamespaceDeclaration(prefix, uri.toString());
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to add namespace " + prefix + ":" + uri.toString(), ex);
        }
    }

    protected String getEnvelopeNamespaceByPrefix(String prefix) throws SOAPOverUDPException {
        try {
            return soapMessage.getSOAPPart().getEnvelope().getNamespaceURI(prefix);
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to get namespace with prefix " + prefix, ex);
        }
    }

    protected String getBodyNamespaceByURI(String namespace) throws SOAPOverUDPException {
        SOAPBody soapBody;
        try {
            soapBody = soapMessage.getSOAPBody();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read namespaces from SOAP body", ex);
        }
        for (Iterator i = soapBody.getVisibleNamespacePrefixes(); i.hasNext(); ) {
            String prefix = (String)i.next();
            //System.out.println("prefix: " + prefix + " ns: " + soap.getSOAPBody().getNamespaceURI(prefix));
            if (soapBody.getNamespaceURI(prefix).equals(namespace))
                return prefix;
        }
        return null;
    }

     protected String getHeaderNamespaceByURI(String namespace) throws SOAPOverUDPException {
        SOAPHeader soapHeader;
        try {
            soapHeader = soapMessage.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new SOAPOverUDPException("Unable to read namespaces from SOAP header", ex);
        }
        for (Iterator i = soapHeader.getVisibleNamespacePrefixes(); i.hasNext(); ) {
            String prefix = (String)i.next();
            //System.out.println("prefix: " + prefix + " ns: " + soap.getSOAPBody().getNamespaceURI(prefix));
            if (soapHeader.getNamespaceURI(prefix).equals(namespace))
                return prefix;
        }
        return null;
    }
  
    /**
     * Returns a String containing this SOAP message.
     * @param encoding Encoding
     * @return A String representation of this SOAP message.
     */
    public String toString(boolean writeXMLDeclaration, Charset encoding) throws SOAPOverUDPException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (encoding == null)
            throw new SOAPOverUDPException("Encoding is (null)");

        if (soapMessage == null)
            throw new SOAPOverUDPException("SOAP message is (null)");
        
        try {
            soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, encoding.toString());
            soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, String.valueOf(writeXMLDeclaration));

            this.saveChanges();

            // Convert to SOAP and let SOAPMessage-object write to out-stream
            soapMessage.writeTo(out);

            // Convert to string with the right encoding
            return out.toString(encoding.name());
        } catch (Exception ex) {
            throw new SOAPOverUDPException("Unable to convert SOAPMessage to XML.", ex);
        }
    }

    @Override
    public String toString() {
        try {
            return this.toString(false, Charset.defaultCharset());
        } catch (SOAPOverUDPException ex) {
            return ex.getMessage();
        }
    }

    public URI getAction() {
        return action;
    }

    public void setAction(URI action) {
        saveRequired = true;
        this.action = action;
    }

    public URI getMessageId() {
        return messageId;
    }

    public void setMessageId(URI messageId) {
        saveRequired = true;
        this.messageId = messageId;
    }

    public URI getRelatesTo() {
        return relatesTo;
    }

    public void setRelatesTo(URI relatesTo) {
        saveRequired = true;
        this.relatesTo = relatesTo;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        saveRequired = true;
        this.relationshipType = relationshipType;
    }

    public SOAPOverUDPEndpointReferenceType getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(SOAPOverUDPEndpointReferenceType replyTo) {
        saveRequired = true;
        this.replyTo = replyTo;
    }

    public URI getTo() {
        return to;
    }

    public void setTo(URI to) {
        saveRequired = true;
        this.to = to;
    }

    public InetAddress getSrcAddress() {
        return srcAddress;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcAddress(InetAddress src) {
        srcAddress = src;
    }

    public void setSrcPort(int port) {
        srcPort = port;
    }

    public InetAddress getDstAddress() {
        return dstAddress;
    }

    public void setDstAddress(InetAddress dstAddress) {
        this.dstAddress = dstAddress;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public abstract boolean isReplyToAnonymous();

    public int getReplyPort() {
        if (isReplyToAnonymous())
            return getSrcPort();
        try {
            return this.getReplyTo().getAddress().getPort();
        } catch (Exception ex) {
            return getSrcPort();
        }
    }

    public InetAddress getReplyAddress() {
        if (isReplyToAnonymous())
            return getSrcAddress();
        try {
            return InetAddress.getByName(this.getReplyTo().getAddress().getHost());
        } catch (UnknownHostException ex) {
            return getSrcAddress();
        }
    }

    public String getReplyProto() {
        if (isReplyToAnonymous())
            return "soap.udp";
        return this.getReplyTo().getAddress().getScheme();
    }
}
