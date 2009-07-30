/*
ITransportType.java

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

package com.ms.wsdiscovery.network.transport;

import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.network.*;

/**
 * Transport types in {@link TransportType} must implement this interface.
 * <p>
 * Implementations must include a constructor without parameters.
 * 
 * @author Magnus Skjegstad
 */
public interface ITransportType {

    /**
     * Receive a packet from the transport layer. Implementer must listen to 
     * unicast and multicast. Multicast messages are sent to the address
     * specified in {@link WsDiscoveryConstants#multicastAddress}.
     * 
     * @param timeoutInMillis Time to wait for data before returning to caller.
     * @return Received packet stored in a {@link NetworkMessage}.
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    NetworkMessage recv(long timeoutInMillis) throws InterruptedException;
    
    /**
     * Receive a packet from the transport layer. Blocks until a packet is 
     * received.
     * 
     * @return Received packet stored in a {@link NetworkMessage}.
     */
    NetworkMessage recv();

    /**
     * Send a message to the transport layer. The implementer must support 
     * unicast and multicast. Multicast messages are always sent to the address
     * specified in {@link WsDiscoveryConstants#multicastAddress}.
     * 
     * @param message Message to send.
     */
    void send(NetworkMessage message);

    /**
     * Shuts down the transport layer. Called by {@link DispatchThread#dispatch()} 
     * on shutdown. 
     */
    void done();
    
    /**
     * Intializes the transport layer. Called by {@link DispatchThread#dispatch()}
     * on startup.
     */
    void start();
}
