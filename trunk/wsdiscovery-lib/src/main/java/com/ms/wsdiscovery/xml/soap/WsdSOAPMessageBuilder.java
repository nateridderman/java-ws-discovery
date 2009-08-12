/*
WsdSOAPMessageBuilder.java

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
import java.io.ByteArrayInputStream;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.xml.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.xml.jaxb_generated.ByeType;
import com.ms.wsdiscovery.xml.jaxb_generated.HelloType;
import com.ms.wsdiscovery.xml.jaxb_generated.ObjectFactory;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeType;
import com.ms.wsdiscovery.xml.jaxb_generated.ResolveMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ResolveType;

/**
 * Helper methods for WS-Discovery SOAP message building. This method is not static
 * because it has to extend the automatically generated ObjectFactory.
 * 
 * @author Magnus Skjegstad
 */
public class WsdSOAPMessageBuilder extends ObjectFactory {
    /**
     * Create a new {@link SOAPMessage} instance from XML data.
     * @param SoapAsXML SOAP message in XML.
     * @return SOAP message as a {@link SOAPMessage}.
     * @throws WsDiscoveryXMLException on failure.
     */
    public SOAPMessage createSOAPMessage(String SoapAsXML) throws WsDiscoveryXMLException {
        MessageFactory factory;
        SOAPMessage message = null;
        try {            
            factory = MessageFactory.newInstance(WsDiscoveryConstants.defaultSoapProtocol);
            message = factory.createMessage();
            message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, (WsDiscoveryConstants.defaultAddXMLHeaderToSOAP ? "true" : "false"));
            message.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, WsDiscoveryConstants.defaultEncoding.toString());
            if (SoapAsXML != null) {
                ByteArrayInputStream i = new ByteArrayInputStream(SoapAsXML.getBytes());
                message.getSOAPPart().setContent(new StreamSource(i));
            }
        } catch (SOAPException ex) {
            throw new WsDiscoveryXMLException("Unable to create new SOAP message.");
        }
        return message;
    }
    /**
     * Create a new, empty {@link SOAPMessage} instance.
     * @return A new instance of {@link SOAPMessage}
     * @throws WsDiscoveryXMLException
     */
    public SOAPMessage createSOAPMessage() throws WsDiscoveryXMLException {
        return createSOAPMessage(null);
    }

    /**
     * Create a new instance of {@link WsdSOAPMessage} from XML data.
     * @param SoapAsXML XML data.
     * @return New instance of {@link WsdSOAPMessage} containing the XML data.
     * @throws WsDiscoveryXMLException on failure.
     */
    public WsdSOAPMessage createWsdSOAPMessage(String SoapAsXML) throws WsDiscoveryXMLException {
        return createWsdSOAPMessage(createSOAPMessage(SoapAsXML));
    }

    /**
     * Create a new instance of {@link WsdSOAPMessage} from data in a {@link SOAPMessage} instance.
     * @param soap SOAP message.
     * @return SOAP message wrapped in {@link WsdSOAPMessage}.
     * @throws WsDiscoveryXMLException on failure.
     */
    public WsdSOAPMessage createWsdSOAPMessage(SOAPMessage soap) throws WsDiscoveryXMLException {
        return new WsdSOAPMessage(soap);
    }
    
    /**
     * Create a blank WS-Discovery Hello-message.
     * @return Hello-message.
     */
    public WsdSOAPMessage<HelloType> createWsdSOAPMessageHello() {
        return new WsdSOAPMessage<HelloType>(WsaActionType.HELLO,
                this.createHello(this.createHelloType()));
    }
    
    /**
     * Create a WS-Discovery Hello-message announcing <code>service</code>.
     * @param service Service to announce in Hello-message.
     * @return Hello-message.
     */
    public WsdSOAPMessage<HelloType> createWsdSOAPMessageHello(WsDiscoveryService service) {
        WsdSOAPMessage<HelloType> m = createWsdSOAPMessageHello();
        HelloType h = m.getJAXBBody();
        
        h.setEndpointReference(service.createEndpointReferenceObject());
        h.setMetadataVersion(service.getMetadataVersion());
                
        h.setScopes(service.createScopesObject());
        
        if (service.getTypes() != null)
            h.getTypes().addAll(service.getTypes());
        
        if (service.getXAddrs() == null)
            throw new NullPointerException("XAddrs can't be null.");
        
        h.getXAddrs().addAll(service.getXAddrs());
        
        return m;
    }

    /**
     * Create a blank WS-Discovery Bye-message.
     * @return Bye-message.
     */
    public WsdSOAPMessage<ByeType> createWsdSOAPMessageBye() {
        return new WsdSOAPMessage<ByeType>(WsaActionType.BYE,
                this.createBye(this.createByeType()));
    }

    /**
     * Create a WS-Discovery Bye-message for <code>service</code>.
     * @param service Service that is about to leave.
     * @return Bye-message.
     */
    public WsdSOAPMessage<ByeType> createWsdSOAPMessageBye(WsDiscoveryService service) {
        WsdSOAPMessage<ByeType> m = createWsdSOAPMessageBye();
        ByeType b = m.getJAXBBody();        
        b.setEndpointReference(service.createEndpointReferenceObject());
        return m;
    }
    
    /**
     * Create blank WS-Discovery Probe-message.
     * @return Probe-message.
     */
    public WsdSOAPMessage<ProbeType> createWsdSOAPMessageProbe() {
        return new WsdSOAPMessage<ProbeType>(WsaActionType.PROBE,
                this.createProbe(this.createProbeType()));
    }

    /**
     * Create blank WS-Discovery ProbeMatches-message.
     * @return ProbeMatches-message.
     */
    public WsdSOAPMessage<ProbeMatchesType> createWsdSOAPMessageProbeMatches() {
        return new WsdSOAPMessage<ProbeMatchesType>(WsaActionType.PROBEMATCHES,
                this.createProbeMatches(this.createProbeMatchesType()));
    }

    /**
     * Create blank WS-Discovery Resolve-message.
     * @return Resolve-message.
     */
    public WsdSOAPMessage<ResolveType> createWsdSOAPMessageResolve() {
        return new WsdSOAPMessage<ResolveType>(WsaActionType.RESOLVE,
                this.createResolve(this.createResolveType()));
    }

    /**
     * Create blank WS-Discovery ResolveMatches-message.
     * @return ResolveMatchse-message.
     */
    public WsdSOAPMessage<ResolveMatchesType> createWsdSOAPMessageResolveMatches() {
        return new WsdSOAPMessage<ResolveMatchesType>(WsaActionType.RESOLVEMATCHES,
                this.createResolveMatches(this.createResolveMatchesType()));
    }
}
