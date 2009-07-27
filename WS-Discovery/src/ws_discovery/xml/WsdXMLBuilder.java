/*
WsdXMLBuilder.java

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
package ws_discovery.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import ws_discovery.WsDiscoveryConstants;
import ws_discovery.xml.exception.WsDiscoveryXMLException;
import ws_discovery.xml.jaxb_generated.AttributedQName;
import ws_discovery.xml.jaxb_generated.AttributedURI;
import ws_discovery.xml.jaxb_generated.EndpointReferenceType;
import ws_discovery.xml.jaxb_generated.ObjectFactory;
import ws_discovery.xml.jaxb_generated.ReferenceParametersType;
import ws_discovery.xml.jaxb_generated.ReferencePropertiesType;
import ws_discovery.xml.jaxb_generated.Relationship;
import ws_discovery.xml.jaxb_generated.ServiceNameType;

/**
 * Helper methods for creating JAXB objects.
 * 
 * @author Magnus Skjegstad
 */
public class WsdXMLBuilder extends ObjectFactory {
    /**
     * Create JAXB AttributedURI from Java URI.
     * @param uri
     * @return Attributed URI.
     */
    public AttributedURI createAttributedURI(String uri) {
        AttributedURI a = new AttributedURI();
        a.setValue(uri);
        return a;
    }

    /**
     * Create JAXB AttributedQName from Java QName.
     * @param name
     * @return Attributed QName.
     */
    public AttributedQName createAttributedQName(QName name) {
        AttributedQName q = new AttributedQName();
        q.setValue(name);
        return q;
    }

    /**
     * Create JAXB RelationshipType from value.
     * @param value 
     * @return RelationShipType 
     */
    public Relationship createRelationship(String value) {
        Relationship r = new Relationship();
        r.setValue(value);
        return r;
    }
    
    /**
     * Create JAXB EndpointReference with specified address.
     * @param uri Endpiont address.
     * @return New EndpointReference with specified address.
     */
    public EndpointReferenceType createEndpointReference(String uri) {
        EndpointReferenceType e = new EndpointReferenceType();
        e.setAddress(createAttributedURI(uri));
        return e;
    }

    /**
     * Create new JAXB instance for instance name specified in 
     * {@link WsDiscoveryConstants#defaultJAXBInstanceName}.
     * @return JAXB instance.
     * @throws WsDiscoveryXMLException
     */
    public JAXBContext newInstance() throws WsDiscoveryXMLException {
        try {
            return JAXBContext.newInstance(WsDiscoveryConstants.defaultJAXBInstanceName);
        } catch (JAXBException ex) {
            throw new WsDiscoveryXMLException("Unable to create JAXB instance: " + WsDiscoveryConstants.defaultJAXBInstanceName);
        }
    }

    /**
     * Create new JAXB marshaller.
     * @return JAXB marshaller.
     * @throws WsDiscoveryXMLException
     */
    public Marshaller createMarshaller() throws WsDiscoveryXMLException {
        Marshaller m;
        try {
            m = this.newInstance().createMarshaller();
        } catch (JAXBException ex) {
            throw new WsDiscoveryXMLException("Unable to create new instance of JAXB marshaller.");
        }
        try {

            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        } catch (PropertyException ex) {
            throw new WsDiscoveryXMLException("Unable to set JAXB marshaller property JAXB_FORMATTED_OUTPUT.");
        }

        return m;
    }

    /**
     * Create new JAXB unmarshaller.
     * @return JAXB unmarshaller.
     * @throws WsDiscoveryXMLException
     */
    public Unmarshaller createUnmarshaller() throws WsDiscoveryXMLException {
        Unmarshaller u;
        try {
            u = this.newInstance().createUnmarshaller();
        } catch (JAXBException ex) {
            throw new WsDiscoveryXMLException("Unable to create new instance of JAXB unmarshaller.");
        }

        return u;
    }
    
    /**
     * Create a copy of an EndpointReferenceType 
     * @param er Original endpoint reference
     * @return New (identical) endpoint reference.
     */
    public EndpointReferenceType cloneEndpointReference(EndpointReferenceType er) {
        EndpointReferenceType endpointReference = new EndpointReferenceType();
                
        if ((er.getAddress() != null) && (er.getAddress().getValue() != null))
            endpointReference.setAddress(createAttributedURI(er.getAddress().getValue()));
        
        if ((er.getPortType() != null) && (er.getPortType().getValue() != null))
            endpointReference.setPortType(createAttributedQName(
                                new QName(endpointReference.getPortType().getValue().getNamespaceURI(),
                                    endpointReference.getPortType().getValue().getLocalPart(),
                                    endpointReference.getPortType().getValue().getPrefix())));
        
        if ((er.getReferenceParameters() != null) && (er.getReferenceParameters().getAny() != null)) {
            ReferenceParametersType rp = new ReferenceParametersType();
            rp.getAny().addAll(er.getReferenceParameters().getAny());            
            endpointReference.setReferenceParameters(rp);
        }
        if ((er.getReferenceProperties() != null) && (er.getReferenceProperties().getAny() != null)) {
            ReferencePropertiesType rp = new ReferencePropertiesType();
            rp.getAny().addAll(er.getReferenceProperties().getAny());            
            endpointReference.setReferenceProperties(rp);
        }
        
        if ((er.getServiceName() != null)) {
            ServiceNameType sn = new ServiceNameType();
            sn.setPortName(er.getServiceName().getPortName());
            if (er.getServiceName().getValue() != null)
                sn.setValue(new QName(er.getServiceName().getValue().getNamespaceURI(),
                        er.getServiceName().getValue().getLocalPart(),
                        er.getServiceName().getValue().getPrefix()));
            
            if (er.getServiceName().getOtherAttributes() != null)
                sn.getOtherAttributes().putAll(er.getServiceName().getOtherAttributes());
        }   
        
        return endpointReference;
    }
}
