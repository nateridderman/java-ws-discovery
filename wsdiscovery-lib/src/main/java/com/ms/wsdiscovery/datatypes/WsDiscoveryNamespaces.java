/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.datatypes;

import com.ms.wsdiscovery.common.WsDiscoveryUtilities;
import com.ms.wsdiscovery.draft2005.WsDiscoveryD2005DispatchThread;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.interfaces.IWsDiscoveryDispatchThread;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.ms.wsdiscovery.standard11.WsDiscoveryS11DispatchThread;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import java.nio.charset.Charset;
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
            com.ms.wsdiscovery.jaxb.draft2005.wsdiscovery.HelloType.class.getPackage().getName(),
            com.ms.wsdiscovery.draft2005.WsDiscoveryD2005DispatchThread.class,
            MatchBy.WSD200504_RFC2396),
    WS_DISCOVERY_2009_01 (
            "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01",
            "http://www.w3.org/2005/08/addressing",
            com.ms.wsdiscovery.jaxb.standard11.wsdiscovery.HelloType.class.getPackage().getName(),
            com.ms.wsdiscovery.standard11.WsDiscoveryS11DispatchThread.class,
            MatchBy.WSD200901_RFC2396);

    private final String wsDiscoveryNamespace;
    private final String wsAddressingNamespace;
    private final String contextPath;
    private final Class dispatchThreadClass;
    private final MatchBy defaultMatcher;

    private JAXBContext jaxbContext = null;
    private Marshaller marshaller = null;
    private Unmarshaller unmarshaller = null;

    WsDiscoveryNamespaces(String wsdNamespace, String wsaNamespace, String contextPath, Class dispatchThreadClass, MatchBy defaultMatcher) {
        this.wsDiscoveryNamespace = wsdNamespace;
        this.wsAddressingNamespace = wsaNamespace;
        this.contextPath = contextPath;
        this.dispatchThreadClass = dispatchThreadClass;
        this.defaultMatcher = defaultMatcher;
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

    public IWsDiscoveryDispatchThread getNewDispatchThreadInstance() throws InstantiationException, IllegalAccessException {
        return (IWsDiscoveryDispatchThread) dispatchThreadClass.newInstance();
    }
    
    public IWsDiscoveryDispatchThread getNewDispatchThreadInstance(ISOAPOverUDPTransport transportType, Charset encoding) throws WsDiscoveryNetworkException, InstantiationException {
        if (dispatchThreadClass.equals(WsDiscoveryS11DispatchThread.class)) {
            return new WsDiscoveryS11DispatchThread(transportType, encoding);
        } else
        if (dispatchThreadClass.equals(WsDiscoveryD2005DispatchThread.class)) {
            return new WsDiscoveryD2005DispatchThread(transportType, encoding);
        } else 
            throw new InstantiationException("Unknown dispatch thread class");
    }

    public MatchBy getDefaultMatcher() {
        return defaultMatcher;
    }

    
}
