/*
WsDiscoveryEndpointReferenceType.java

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

import java.net.URI;
import javax.xml.namespace.QName;

/**
 * Generic type for storing the JAXB-generated WS-Addressing EndpointReferenceType. Notice
 * that there is some overlap between the datatypes. This is due to differences in
 * the WS-Addressing versions.
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPEndpointReferenceType extends SOAPOverUDPGenericAnyType implements Cloneable {
    protected URI address;
    protected QName portType;
    protected SOAPOverUDPServiceNameType serviceName;
    protected SOAPOverUDPGenericAnyType referenceParameters;
    protected SOAPOverUDPGenericAnyType referenceProperties;
    protected SOAPOverUDPGenericAnyType metadata;

    public SOAPOverUDPEndpointReferenceType(URI address) {
        super();
        this.address = address;
    }

    public SOAPOverUDPEndpointReferenceType(String address) {
        this(URI.create(address));
    }

    public SOAPOverUDPEndpointReferenceType() {
        super();
    }

    public void setAddress(URI address) {
        this.address = address;
    }

    public void setMetadata(SOAPOverUDPGenericAnyType metadata) {
        this.metadata = metadata;
    }

    public void setReferenceParameters(SOAPOverUDPGenericAnyType referenceParameters) {
        this.referenceParameters = referenceParameters;
    }

    public void setReferenceProperties(SOAPOverUDPGenericAnyType referenceProperties) {
        this.referenceProperties = referenceProperties;
    }

    public URI getAddress() {
        return address;
    }

    public SOAPOverUDPGenericAnyType getMetadata() {
        return metadata;
    }

    public SOAPOverUDPGenericAnyType getReferenceParameters() {
        return referenceParameters;
    }

    public SOAPOverUDPGenericAnyType getReferenceProperties() {
        return referenceProperties;
    }

    public QName getPortType() {
        return portType;
    }

    public void setPortType(QName portType) {
        this.portType = portType;
    }

    public SOAPOverUDPServiceNameType getServiceName() {
        return serviceName;
    }

    public void setServiceName(SOAPOverUDPServiceNameType serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Object clone() {
        SOAPOverUDPEndpointReferenceType endpointReference = new SOAPOverUDPEndpointReferenceType();

        if (this.getAddress() != null)
            endpointReference.setAddress(URI.create(this.getAddress().toString()));

        if (this.getServiceName() != null)
            endpointReference.setServiceName((SOAPOverUDPServiceNameType) this.getServiceName().clone());

        if (this.getReferenceParameters() != null) {
            endpointReference.setReferenceParameters((SOAPOverUDPGenericAnyType)this.getReferenceParameters().clone());
        }
        
        if (this.getReferenceProperties() != null) {
            endpointReference.setReferenceProperties((SOAPOverUDPGenericAnyType)this.getReferenceProperties().clone());
        }

        if (this.getMetadata() != null) {
            endpointReference.setMetadata((SOAPOverUDPGenericAnyType)this.getMetadata().clone());
        }

        if (this.getPortType() != null) {
            endpointReference.setPortType(new QName(
                    portType.getNamespaceURI(),
                    portType.getLocalPart(),
                    portType.getPrefix()));
        }

        return endpointReference;
    }

    @Override
    public String toString() {
        String s = new String();
        SOAPOverUDPEndpointReferenceType e = this;
        if (e.getAddress() != null) {
            s += "\tAddress: " + e.getAddress().toString() + "\n";
        }
        if (e.getPortType() != null)  {
            s += "\tPortType: " + e.getPortType().toString() + "\n";
        }
        if (e.getReferenceParameters() != null) 
            s += "\tReferenceParameters: " + e.getReferenceParameters().toString() + "\n";
        if (e.getReferenceProperties() != null) {
            s += "\tReferenceProperties: " + e.getReferenceProperties().toString() + "\n";
        }
        if ((e.getServiceName() != null) && (e.getServiceName().getPortName() != null)) {
            s += "\tServiceName.PortName: " + e.getServiceName().getPortName() + "\n";
        }
        if ((e.getServiceName() != null) && (e.getServiceName().getValue() != null)) {
            s += "\tServiceName.Value: " + e.getServiceName().getValue().toString() + "\n";
        }
        if ((e.getMetadata() != null))
            s += "\tMetadata: " + e.getMetadata().toString();

        if ((e.getOtherAttributes() != null) || (e.getAny() != null))
            s += "\t"+super.toString()+"\n";

        return s;
    }


}
