/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.datatypes;

import com.ms.wsdiscovery.common.WsDiscoveryUtilities;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Namespaces used by WS-Discovery. 
 *
 * @author Magnus Skjegstad
 */
public enum WsDiscoveryNamespaces {
    WS_DISCOVERY_2005_04 (
            "http://schemas.xmlsoap.org/ws/2005/04/discovery",
            "http://schemas.xmlsoap.org/ws/2004/08/addressing",
            com.ms.wsdiscovery.jaxb.draft2005.wsdiscovery.HelloType.class.getPackage().getName()),
    WS_DISCOVERY_2009_01 (
            "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01",
            "",
            com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.HelloType.class.getPackage().getName());

    private final String wsDiscoveryNamespace;
    private final String wsAddressingNamespace;
    private final String contextPath;

    private JAXBContext jaxbContext = null;
    private Marshaller marshaller = null;
    private Unmarshaller unmarshaller = null;

    WsDiscoveryNamespaces(String wsdNamespace, String wsaNamespace, String contextPath) {
        this.wsDiscoveryNamespace = wsdNamespace;
        this.wsAddressingNamespace = wsaNamespace;
        this.contextPath = contextPath;
    }

    public JAXBContext getJAXBContext() throws WsDiscoveryException {
        if (jaxbContext == null) // only do this once
            jaxbContext = WsDiscoveryUtilities.createJAXBContext(this.contextPath);

        return jaxbContext;
    }

    public Marshaller getMarshaller() throws WsDiscoveryException {
        if (marshaller == null) // only do this once
            marshaller = WsDiscoveryUtilities.createMarshaller(this.getJAXBContext());
        return marshaller;
    }

    public Unmarshaller getUnmarshaller() throws WsDiscoveryException {
        if (unmarshaller == null) // only do this once
            unmarshaller = WsDiscoveryUtilities.createUnmarshaller(this.getJAXBContext());
        return unmarshaller;
    }

    public String getWsDiscoveryNamespace() {
        return wsDiscoveryNamespace;
    }

    public String getWsAddressingNamespace() {
        return wsAddressingNamespace;
    }

    public String getContextPath() {
        return contextPath;
    }
}
