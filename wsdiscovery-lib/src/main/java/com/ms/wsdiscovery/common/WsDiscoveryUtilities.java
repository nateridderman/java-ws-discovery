/*
WsDiscoveryUtilities.java

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
package com.ms.wsdiscovery.common;

import com.ms.wsdiscovery.exception.WsDiscoveryException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

/**
 * Static helper functions used by WS-Discovery
 *
 * @author Magnus Skjegstad
 */
public class WsDiscoveryUtilities {
    /**
     * Create new JAXB instance.
     * @param JAXB context instance name
     * @return JAXB instance.
     * @throws SOAPOverUDPException
     */
    public static JAXBContext createJAXBContext(String contextPath) throws WsDiscoveryException {
        try {
            return JAXBContext.newInstance(contextPath);
        } catch (JAXBException ex) {
            throw new WsDiscoveryException("Unable to create JAXB instance: " + contextPath, ex);
        }
    }
    /**
     * Create new JAXB marshaller.
     * @return JAXB marshaller.
     * @throws SOAPOverUDPException
     */
    public static Marshaller createMarshaller(JAXBContext jaxbContext) throws WsDiscoveryException {
        Marshaller m;
        try {
            m = jaxbContext.createMarshaller();
        } catch (JAXBException ex) {
            throw new WsDiscoveryException("Unable to create new instance of JAXB marshaller.", ex);
        }
        try {
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        } catch (PropertyException ex) {
            throw new WsDiscoveryException("Unable to set JAXB marshaller property JAXB_FORMATTED_OUTPUT.", ex);
        }

        return m;
    }

    /**
     * Create new JAXB unmarshaller.
     * @return JAXB unmarshaller.
     * @throws SOAPOverUDPException
     */
    public static Unmarshaller createUnmarshaller(JAXBContext jaxbContext) throws WsDiscoveryException {
        Unmarshaller u;
        try {
            u = jaxbContext.createUnmarshaller();
        } catch (JAXBException ex) {
            throw new WsDiscoveryException("Unable to create new instance of JAXB unmarshaller.", ex);
        }

        return u;
    }

    // Method to get first non-loopback address. Used as fallback when proxy-address is not specified by user.
    // Courtesy of http://stackoverflow.com/questions/901755/how-to-get-the-ip-of-the-computer-on-linux-through-java/901943#901943
    public static InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) en.nextElement();
            for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        return addr;
                    }
                    if (addr instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        return addr;
                    }
                }
            }
        }
        return null;
    }
}
