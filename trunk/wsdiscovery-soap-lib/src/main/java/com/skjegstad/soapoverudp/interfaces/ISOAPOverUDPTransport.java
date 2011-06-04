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

package com.skjegstad.soapoverudp.interfaces;

import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPNotInitializedException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * SOAP transport types must implement this interface.
 * <p>
 * Implementations must include a constructor without parameters.
 * 
 * @author Magnus Skjegstad
 */
public interface ISOAPOverUDPTransport extends ISOAPOverUDPConfigurable {
    
    /**
     * Receive a packet from the transport layer. Implementer must listen to 
     * unicast and multicast. 
     * 
     * @param timeoutInMillis Time to wait for data before returning to caller.
     * @return Received packet stored in a {@link NetworkMessage}.
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    ISOAPOverUDPNetworkMessage recv(long timeoutInMillis) throws InterruptedException;
    
    /**
     * Receive a packet from the transport layer. Blocks until a packet is 
     * received.
     * 
     * @return Received packet stored in a {@link NetworkMessage}.
     */
    ISOAPOverUDPNetworkMessage recv();

    /**
     * Send a message to the transport layer. The implementer must support 
     * unicast and multicast.
     * 
     * @param message Message to send.
     */

    void send(ISOAPOverUDPNetworkMessage message, boolean blockUntilSent) throws InterruptedException;
    void sendStringMulticast(String string, boolean blockUntilSent) throws InterruptedException;
    void sendStringUnicast(String string, InetAddress destAddress, int destPort, boolean blockUntilSent) throws InterruptedException;

    /**
     * Returns the port used for sending and receiving multicasted packets.
     * @return Port used for multicasts.
     */
    int getMulticastPort();

    /**
     * Returns the port used for sending and receiving unicasted packets.
     * @return Port used for unicasts.
     */
    int getUnicastPort();

    InetAddress getMulticastAddress();
    InetAddress getUnicastAddress();

    /**
     * Shuts down the transport layer.
     */
    void done();

    void setEncoding(Charset encoding);
    
    /**
     * Starts the transport threads.
     *
     * @throws SOAPOverUDPNotInitializedException if isInitialized() returns false.
     */
    void start() throws SOAPOverUDPNotInitializedException;

    /**
     * Returns true after start() has return successfully. Set to false by done().
     *
     * @return true when the transport layer is started, otherwise false.
     */
    boolean isRunning();

    /**
     * Initialize the transport layer. This method can be called automatically from
     * the constructor, but is included to support instantiations using newInstance().
     *
     * @param multicastInterface Network interface to use for multicasting. Set to null to use default.
     * @param multicastPort Port for sending and receiving multicast messages
     * @param multicastAddress Address for sending and listening to multicast messages.
     * @param logger Instance of Logger used for debugging. May be set to null.
     * @throws SOAPOverUDPException if an error occured while opening
     * the sockets or creating child threads.
     */
    void init(NetworkInterface multicastInterface, int multicastPort, InetAddress multicastAddress, int multicastTtl, Logger logger) throws SOAPOverUDPException;

    /**
     * Returns true after init() has been called successfully.
     */
    boolean isInitialized();

}
