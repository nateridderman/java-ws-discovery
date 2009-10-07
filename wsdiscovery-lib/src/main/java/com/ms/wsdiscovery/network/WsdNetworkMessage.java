/*
WsdNetworkMessage.java

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
package com.ms.wsdiscovery.network;

import com.skjegstad.soapoverudp.messages.NetworkMessage;
import com.skjegstad.soapoverudp.interfaces.INetworkMessage;
import java.net.InetAddress;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.xml.soap.WsdSOAPMessage;

/**
 * Class used to represent messages received or sent on the network. Contains 
 * payload, source port/address and destination port/address. A timestamp is 
 * generated when the class is instantiated.
 * 
 * @author Magnus Skjegstad
 */
public class WsdNetworkMessage extends NetworkMessage implements INetworkMessage {
    /**
     * Stores a network message.
     * 
     * @param message Payload as a string. The string will be converted to an 
     * array of bytes with the encoding specified in WsDiscoveryConstants.
     * @param srcAddress Source address
     * @param srcPort Source port
     * @param dstAddress Destination addrses
     * @param dstPort Destination port
     */
    public WsdNetworkMessage(String message, InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort) {
        super(message.getBytes(WsDiscoveryConstants.defaultEncoding),
             srcAddress, srcPort, dstAddress, dstPort);
    }
    
    /**
     * This constructor will use the multicast port and address specified in 
     * WsDiscoveryConstants as the default destination. Source port and address 
     * will be set to 0 and null respectively. The message is converted to an 
     * array of bytes with the encoding specified in 
     * WsDiscoveryConstants.defaultEncoding.
     * 
     * @param message String representing the payload. The string will be converted to an array of bytes with the encoding specified in WsDiscoveryConstants.defaultEncoding.
     */
    public WsdNetworkMessage(String message) {
        super(message.getBytes(WsDiscoveryConstants.defaultEncoding),
             null, 0, WsDiscoveryConstants.multicastAddress, WsDiscoveryConstants.multicastPort);
    }
    
    /**
     * Construct a NetworkMessage based on a WsdSOAPMessage. Converts the WsdSOAPMessage to string and 
     * calls NetworkMessage(String). 
     * 
     * @param soap WsdSOAPMessage containing the message.
     */
    public WsdNetworkMessage(WsdSOAPMessage soap) {
        this(soap.toString(WsDiscoveryConstants.defaultEncoding));
    }

    /**
     * Get payload as a String with the encoding specified in 
     * WsDiscoveryConstants.defaultEncoding.
     * 
     * @return String representation of payload
     */
    @Override
    public String getMessage() {
        return new String(payload, 0, payloadLen, WsDiscoveryConstants.defaultEncoding);
    }        
    
    /**
     * Set new payload. The input string will be converted to an array of bytes 
     * with the encoding specified in WsDiscoveryConstants.defaultEncoding.
     * 
     * @param newMessage Payload represented as a string.
     */
    public synchronized void setMessage(String newMessage) {
        setPayload(newMessage.getBytes(WsDiscoveryConstants.defaultEncoding));
    }    
    
}
