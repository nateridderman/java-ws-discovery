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

package com.skjegstad.soapoverudp.transport;

import com.skjegstad.soapoverudp.messages.SOAPOverUDPNetworkMessage;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPNetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPConfigurable;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;

/**
 * An implementation of SOAP-over-UDP using ZLib-compression.
 * 
 * @author Magnus Skjegstad
 */
public abstract class SOAPOverUDPTransportZlib extends SOAPOverUDPTransport implements ISOAPOverUDPTransport, ISOAPOverUDPConfigurable {
    
    public SOAPOverUDPTransportZlib() {
        super();
    }

    /**
     * Receive message.
     * 
     * @param timeoutInMillis Time to wait for new message.
     * @return Message or <code>null</code> on timeout.
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    @Override
    public ISOAPOverUDPNetworkMessage recv(long timeoutInMillis) throws InterruptedException {
        ISOAPOverUDPNetworkMessage nm = super.recv(timeoutInMillis);
        
        String data = decompress(nm.getPayload());
        nm.setMessage(data, encoding);
        
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
    public void send(ISOAPOverUDPNetworkMessage message, boolean blockUntilSent)
            throws InterruptedException {
              
        byte[] payload = compress(message.getMessage(encoding));
        ISOAPOverUDPNetworkMessage nm = new SOAPOverUDPNetworkMessage(
                payload, payload.length,
                message.getSrcAddress(), message.getSrcPort(), 
                message.getDstAddress(), message.getDstPort());        

        super.send(nm, blockUntilSent);            
    }

    protected String decompress(byte[] message) {
        byte[] data = new byte[0xfffff];
        String ret = null;

        Inflater decompresser = new Inflater();
        try {
            decompresser.setInput(message);
            int len = decompresser.inflate(data);
            ret = new String(data, 0, len, encoding);
        } catch (DataFormatException ex) {
            ret = null;
        }
        decompresser.end();
        return ret;
    }

    protected byte[] compress(String message) {
        Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);

        byte[] payload = message.getBytes(encoding);
        byte[] data = new byte[payload.length]; // assume compressed output is always less than input size
        compresser.setInput(payload);
        compresser.finish();
        int len = compresser.deflate(data);

        compresser.deflate(data);
        byte[] ret = new byte[data.length];
        // TODO More efficient with inputstreams ?
        System.arraycopy(data, 0, ret, 0, len);
        return ret;
    }

}
