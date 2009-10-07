/*
SOAPOverUDPzlib.java

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

package com.skjegstad.soapoverudp;

import com.skjegstad.soapoverudp.messages.NetworkMessage;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import com.skjegstad.soapoverudp.interfaces.INetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPTransport;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.logging.Logger;

/**
 * An implementation of SOAP-over-UDP using ZLib-compression.
 * 
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPzlib extends SOAPOverUDP implements ISOAPTransport {
    
    /**
     * @param multicastInterface Network interface to use for multicasting. Set to null to use default.
     * @param multicastPort Port for sending and receiving multicast messages
     * @param multicastAddress Address for sending and listening to multicast messages.
     * @param logger Instance of Logger used for debugging. May be set to null.
     * @throws WsDiscoveryTransportException if an error occured while opening
     * the sockets or creating child threads.
     */
    public SOAPOverUDPzlib(NetworkInterface multicastInterface, int multicastPort, InetAddress multicastAddrses, Logger logger) throws SOAPOverUDPException {
        super(multicastInterface,multicastPort,multicastAddrses, logger);
    }

    /**
     * Receive message.
     * 
     * @param timeoutInMillis Time to wait for new message.
     * @return Message or <code>null</code> on timeout.
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    @Override
    public INetworkMessage recv(long timeoutInMillis) throws InterruptedException {
        INetworkMessage nm = super.recv(timeoutInMillis);
        
        if (nm != null) {
            Inflater decompresser = new Inflater();
            byte[] data = new byte[0xffff];
            try {
                decompresser.setInput(nm.getPayload(), 0, nm.getPayloadLen());
                int len = decompresser.inflate(data);                
                nm.setPayload(data, len);
                decompresser.end();
            } catch (DataFormatException ex) {
                if (logger != null) {
                    synchronized (logger) {
                        logger.warning(ex.toString());
                    }
                } else {
                    ex.printStackTrace();
                }
                return null;
            }
        }
        return nm;
    }

    /**
     * Send message.
     * 
     * @param message Message to be sent.
     * @param blockUntilSent If true, block until all messages are sent (queue is empty).
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    @Override
    public void send(INetworkMessage message, boolean blockUntilSent)
            throws InterruptedException {
              
        Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);

        byte[] data = new byte[0xffff];
        compresser.setInput(message.getPayload(), 0, message.getPayloadLen());
        compresser.finish();
        int len = compresser.deflate(data);

        INetworkMessage nm = new NetworkMessage(
                data, len, 
                message.getSrcAddress(), message.getSrcPort(), 
                message.getDstAddress(), message.getDstPort());        

        super.send(nm, blockUntilSent);            
    }

}