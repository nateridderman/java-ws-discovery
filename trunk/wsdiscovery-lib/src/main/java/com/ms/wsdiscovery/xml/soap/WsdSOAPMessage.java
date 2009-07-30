/*
WsdSOAPMessage.java

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

package com.ms.wsdiscovery.xml.soap;

import com.ms.wsdiscovery.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Document;
import com.ms.wsdiscovery.xml.WsdXMLBuilder;
import com.ms.wsdiscovery.xml.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.xml.jaxb_generated.EndpointReferenceType;
import com.ms.wsdiscovery.xml.jaxb_generated.AttributedURI;
import com.ms.wsdiscovery.xml.jaxb_generated.AppSequenceType;
import com.ms.wsdiscovery.xml.jaxb_generated.Relationship;

/**
 * Class representing a WS-Discovery SOAP message. <p>
 * The SOAP body can contain one of the following JAXB elements:
 * <li>HelloType</li>
 * <li>ByeType</li>
 * <li>ProbeType</li>
 * <li>ProbeMatchesType</li>
 * <li>ResolveType</li>
 * <li>ResolveMatchesType</li>
 * 
 * @param <E> JAXB element type in SOAP body.
 * @author Magnus Skjegstad
 */

public class WsdSOAPMessage<E> {
    /**
     * SOAP builder instance
     */
    protected static WsdSOAPMessageBuilder soapbuilder = 
            WsDiscoveryConstants.SOAPBUILDER;
    /**
     * XML builder instance
     */
    protected static WsdXMLBuilder jaxbbuilder = 
            WsDiscoveryConstants.XMLBUILDER;
    
    /**
     * Namespaces used in the SOAP message. These are added to the 
     * header. The first String is the prefix, the second is the URI.
     */
    protected Map<String,URI> namespaces = new HashMap<String,URI>();
    
    /**
     * WS-Addressing ReplyTo
     */
    protected EndpointReferenceType wsaReplyTo = null;

    /**
     * WS-Addressing Message ID
     */
    protected AttributedURI wsaMessageId = null;
    /**
     * WS-Addressing To
     */
    protected AttributedURI wsaTo = WsDiscoveryConstants.defaultTo;
    /**
     * WS-Addressing Action
     */
    protected AttributedURI wsaAction = null;
    /**
     * WS-Addressing RelatesTo
     */
    protected Relationship wsaRelatesTo = null;
    
    /**
     * WS-Discovery Instance ID
     */
    protected long wsdInstanceId;
    /**
     * WS-Discovery Sequence ID
     */
    protected String wsdSequenceId;
    /**
     * WS-Discovery Message Number.
     */
    protected long wsdMessageNumber;
    
    /**
     * SOAP body.
     */
    protected SOAPBody soapBody;
    /**
     * SOAP header.
     */
    protected SOAPHeader soapHeader;
    /**
     * SOAP message (envelope, body, header)
     */
    protected SOAPMessage soapMessage;
    
    /**
     * JAXB body of message.
     */
    protected JAXBElement<E> jaxbBody = null;
    
    /**
     * Create SOAP message of specified action type containing a JAXB element.
     * @param action Action type.
     * @param jaxb JAXB element.
     */
    public WsdSOAPMessage(WsaActionType action, JAXBElement<E> jaxb) {
        wsaMessageId = jaxbbuilder.createAttributedURI("urn:uuid:" + 
                UUID.randomUUID());
        wsdInstanceId = WsDiscoveryConstants.instanceId; 
        wsdSequenceId = "urn:uuid:" + WsDiscoveryConstants.sequenceId;
        wsdMessageNumber = ++LastMessageNumber;
        
        wsaAction = action.toAttributedURI();
        jaxbBody = jaxb;
        
        // Namespaces added here are included in the envelope
        addNamespace("wsd", WsDiscoveryConstants.defaultNsDiscovery);
        addNamespace("wsa", WsDiscoveryConstants.defaultNsAddressing);
    }
    
    /**
     * Create new instance based on an existing SOAP message.
     * @param soap SOAP message.
     * @throws WsDiscoveryXMLException on failure.
     */
    public WsdSOAPMessage(SOAPMessage soap) throws WsDiscoveryXMLException {
        parseSoap(soap);
    }
       
    /**
     * Parse SOAP message.
     * 
     * @param soap SOAP message.
     * @throws WsDiscoveryXMLException
     */
    protected void parseSoap(SOAPMessage soap) throws WsDiscoveryXMLException {
        soapMessage = soap;
        try {
            soapHeader = soap.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to create SOAP header.");
        }
        
        try {
            soapBody = soap.getSOAPBody();
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to create SOAP body.");
        }
           
        // Add all namespaces visible from the SOAP body to the global namespace list
        for (Iterator i = soapBody.getVisibleNamespacePrefixes(); i.hasNext(); ) {
            String prefix = (String)i.next();
            //System.out.println("prefix: " + prefix + " ns: " + soap.getSOAPBody().getNamespaceURI(prefix));
            try {
                addNamespace(prefix,URI.create(soap.getSOAPBody().getNamespaceURI(prefix)));                        
            } catch (SOAPException ex) {
                throw new WsDiscoveryXMLException("Unable to retrieve SOAP body.");
            }
        }
        
        // All namespaces defined in the SOAP-message must be defined in the body for JAXB to recognize them.
        // This is a hack that loops through the childs of the SOAP-body and adds namespaces
        for (Iterator i  = soapBody.getChildElements(); i.hasNext(); ) {
            Object o = i.next();            
            if (o instanceof SOAPElement) { // Is this a SOAP element ?
                SOAPElement e = (SOAPElement)o;                
                // Declare all namespaces we know about
                for (Entry<String,URI> ns : getNamespaces().entrySet()) 
                    try {
                        e.addNamespaceDeclaration(ns.getKey(), ns.getValue().toString());
                    } catch (SOAPException ex) {
                        throw new WsDiscoveryXMLException("Unable to declare namespace " + 
                                ns.getKey() + ":" + ns.getValue().toString());
                    }
            }             
        }
                
        Unmarshaller u; 
        Document soapDoc;               
        
        try {
            soapDoc = soapBody.extractContentAsDocument();
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to extract document from SOAP body");
        }
        
        u = jaxbbuilder.createUnmarshaller();
        try {            
            jaxbBody = (JAXBElement<E>)u.unmarshal(soapDoc);
        } catch (JAXBException ex) {
            throw new WsDiscoveryXMLException("Unable to unmarshal SOAP document.");
        }
                
        JAXBElement j = null;
        
        for (Iterator<SOAPHeaderElement> i = soapHeader.extractAllHeaderElements(); i.hasNext();) {
            SOAPHeaderElement headerElement = i.next();
            try {
                // Unmarshal the header element
                j = (JAXBElement) u.unmarshal(headerElement);
            } catch (JAXBException ex) {
                throw new WsDiscoveryXMLException("Unable to unmarshal header element " + headerElement.getTagName());
            }

            String tag = j.getName().getLocalPart();
            
            if (j.getValue().getClass().equals(AttributedURI.class)) {
                AttributedURI a = (AttributedURI)j.getValue();
                if (tag.equals("To")) 
                    wsaTo = a; 
                else                     
                if (tag.equals("Action"))
                    wsaAction = a;
                else
                if (tag.equals("MessageID"))
                    wsaMessageId = a;                        
            } else
            if (j.getValue().getClass().equals(Relationship.class)) {
                Relationship r = (Relationship)j.getValue();
                if (tag.equals("RelatesTo"))
                    wsaRelatesTo = r;
            } else
            if (j.getValue().getClass().equals(EndpointReferenceType.class)) {
                EndpointReferenceType e = (EndpointReferenceType)j.getValue();
                if (tag.equals("ReplyTo"))
                    wsaReplyTo = e;
            }
            if (j.getValue().getClass().equals(AppSequenceType.class)) {
                AppSequenceType a = (AppSequenceType)j.getValue();
                if (tag.equals("AppSequence")) {
                    wsdInstanceId = a.getInstanceId();
                    wsdMessageNumber = a.getMessageNumber();
                    wsdSequenceId = a.getSequenceId();
                }
            }                                        
        }            
    }
                
    /**
     * Add namespace to SOAP message envelope.
     * @param prefix Prefix to use for this namespace.
     * @param uri Namespace URI.
     */
    public void addNamespace(String prefix, URI uri) {
        getNamespaces().put(prefix, uri);        
    }
    
    /**
     * Get list of namespaces. First String is prefix, second is namespace URI.
     * @return Namespaces.
     */
    public Map<String, URI> getNamespaces() {
        return namespaces;
    }
       
    /**
     * Convert to {@link SOAPMessage}.
     * @return SOAP message.
     * @throws WsDiscoveryXMLException on failure.
     */
    public SOAPMessage toSoap() throws WsDiscoveryXMLException {
        // Instantiate JAXB marshaller        
        Marshaller m = jaxbbuilder.createMarshaller();
               
        // Create soap empty message
        SOAPMessage message = soapbuilder.createSOAPMessage();
                      
        // Add namespaces to header and body
        try {
            for (Entry<String, URI> n : namespaces.entrySet()) 
                message.getSOAPPart().getEnvelope().addNamespaceDeclaration(n.getKey(), n.getValue().toString());
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to add namespaces to SOAP message.");
        }
                
        
        SOAPHeader header;
        SOAPBody body;
        
        try {
            body = message.getSOAPBody();
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to get SOAP body.");
        }
        
        try {
            header = message.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to get SOAP header.");
        }
        
        // Create header wsd:AppSequenceType
        AppSequenceType a = soapbuilder.createAppSequenceType();
        
        a.setInstanceId(wsdInstanceId);
        a.setMessageNumber(wsdMessageNumber);
        a.setSequenceId(wsdSequenceId);        
        try {
            m.marshal(soapbuilder.createAppSequence(a), header);
        } catch (JAXBException ex) {
            throw new WsDiscoveryXMLException("Unable to marshal AppSequence into SOAP header.");
        }
                
        // Create other header-elements
        try {
            if (wsaTo != null)
                m.marshal(soapbuilder.createTo(wsaTo), 
                        header);
            if (wsaAction != null)
                m.marshal(soapbuilder.createAction(wsaAction),
                        header);
            if (wsaRelatesTo != null) 
                m.marshal(soapbuilder.createRelatesTo(wsaRelatesTo), 
                        header); 
            if (wsaReplyTo != null)
                m.marshal(soapbuilder.createReplyTo(wsaReplyTo),
                        header);
            if (wsaMessageId != null)
                m.marshal(soapbuilder.createMessageID(wsaMessageId),
                        header);
        } catch (JAXBException ex) {
            throw new WsDiscoveryXMLException("Unable to add WS-Addressing to SOAP header.");
        }
                            
        // Marshal JAXB elements to SOAP body
        if (jaxbBody != null)
            try {
                m.marshal(jaxbBody, body);
            } catch (JAXBException ex) {
                throw new WsDiscoveryXMLException("Unable to marshal JAXB into SOAP body.");
            }
        
        try {
            message.saveChanges();
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to save SOAP message.");
        }
        
        return message;        
    }
    
    /**
     * Get WS-Addressing RelatesTo value.
     * @return RelatesTo.
     */
    public Relationship getWsaRelatesTo() {
        return wsaRelatesTo;
    }

    /**
     * Set WS-Addressing RelatesTo value.
     * @param wsaRelatesTo
     */
    public void setWsaRelatesTo(Relationship wsaRelatesTo) {
        this.wsaRelatesTo = wsaRelatesTo;
    }

    /**
     * Get WS-Addressing Endpoint reference.
     * @return Endpoint reference.
     */
    public EndpointReferenceType getWsaReplyTo() {
        return wsaReplyTo;
    }

    /**
     * Set WS-Addressing Reply to.
     * @param wsaReplyTo
     */
    public void setWsaReplyTo(EndpointReferenceType wsaReplyTo) {
        this.wsaReplyTo = wsaReplyTo;
    }

    /**
     * Get WS-Addressing To.
     * @return Value of the To-field.
     */
    public AttributedURI getWsaTo() {
        return wsaTo;
    }

    /**
     * Set WS-Addressing To
     * @param wsaTo
     */
    public void setWsaTo(AttributedURI wsaTo) {
        this.wsaTo = wsaTo;
    }

    /**
     * Get WS-Addressing Action.
     * @return Value of Action attribute
     */
    public AttributedURI getWsaAction() {
        return wsaAction;
    }

    /**
     * Get WS-Addressing Message ID.
     * @return Message ID.
     */
    public AttributedURI getWsaMessageId() {
        return wsaMessageId;
    }

    /**
     * Get WS-Discovery Instance ID.
     * @return Instance ID
     */
    public long getWsdInstanceId() {
        return wsdInstanceId;
    }

    /**
     * Get WS-Discovery Message number.
     * @return Message number
     */
    public long getWsdMessageNumber() {
        return wsdMessageNumber;
    }

    /**
     * Get WS-Discovery Sequence ID.
     * @return Sequence ID
     */
    public String getWsdSequenceId() {
        return wsdSequenceId;
    }
    
    /**
     * Get the JAXB element that represents the body of the SOAP message.
     * @return JAXB element.
     */
    public E getJAXBBody() {
        return jaxbBody.getValue();
    }
    
    /**
     * Returns a String containing this SOAP message.
     * @param encoding Encoding
     * @return A String representation of this SOAP message.
     */
    public String toString(Charset encoding) {        
        ByteArrayOutputStream out = new ByteArrayOutputStream();              
                        
        try {
            // Convert to SOAP and let SOAPMessage-object write to out-stream
            toSoap().writeTo(out);        
            // Convert to string with the right encoding
            return out.toString(encoding.name());            
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (WsDiscoveryXMLException ex) {
            ex.printStackTrace();
            return null;
        } catch (SOAPException ex) {
            ex.printStackTrace();
            return null;
        }
    }    
    
    @Override
    public String toString() {
        return this.toString(WsDiscoveryConstants.defaultEncoding);
    }
        
    private static int LastMessageNumber = 0; // "Identifies a message within the context of a sequence number and an instance identifier."             
}
