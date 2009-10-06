/*
NetworkMessage.java

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

import com.ms.wsdiscovery.network.interfaces.INetworkMessage;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class used to represent messages received or sent on the network. Contains
 * payload, source port/address and destination port/address. A timestamp is
 * generated when the class is instantiated.
 *
 * @author Magnus Skjegstad
 */
public class NetworkMessage implements INetworkMessage {
    /**
     * Source and destination address.
     */
    protected InetAddress srcAddress, dstAddress;
    /**
     * Source and destination port.
     */
    protected int srcPort, dstPort;
    /**
     * Payload as an array of bytes.
     */
    protected byte[] payload;
    /**
     * Length of payload in bytes.
     */
    protected int payloadLen;
    /**
     * When the message was received, measured in milliseconds after epoch.
     */
    protected long timestamp;

    /**
     * Stores a network message.
     *
     * @param payload Packet payload
     * @param payloadLen Length of payload in bytes
     * @param srcAddress Source address
     * @param srcPort Source port
     * @param dstAddress Destination address
     * @param dstPort Destination port
     */
    public NetworkMessage(byte[] payload, int payloadLen, InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort) {
        this.srcAddress = srcAddress;
        this.srcPort = srcPort;

        this.dstAddress = dstAddress;
        this.dstPort = dstPort;

        this.payload = payload;
        this.payloadLen = payloadLen;

        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Stores a network message.
     *
     * @param payload Payload as an array of bytes. The length is determined by
     * the size of the array.
     * @param srcAddress Source address
     * @param srcPort Source port
     * @param dstAddress Destination address
     * @param dstPort Destination port
     */
    public NetworkMessage(byte[] payload, InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort) {
        this(payload, payload.length, srcAddress, srcPort, dstAddress, dstPort);
    }

    /**
     * Get source address.
     *
     * @return Source address
     */
    public InetAddress getSrcAddress() {
        return srcAddress;
    }

    /**
     * Get destination address.
     *
     * @return Destination address
     */
    public InetAddress getDstAddress() {
        return dstAddress;
    }

    /**
     * Set source address.
     *
     * @param newAddress New source address
     */
    public synchronized void setSrcAddress(InetAddress newAddress) {
        srcAddress = newAddress;
    }

    /**
     * Set destination address.
     *
     * @param newAddress New destination address
     */
    public synchronized void setDstAddress(InetAddress newAddress) {
        dstAddress = newAddress;
    }

    /**
     * Get payload as a String using default encoding
     *
     * @return String representation of payload
     */
    protected String getMessage() {
        return new String(payload, 0, payloadLen);
    }

    /**
     * Get current payload.
     *
     * @return Payload as array of bytes.
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Get length of payload in bytes.
     *
     * @return Length of current payload in bytes.
     */
    public int getPayloadLen() {
        return payloadLen;
    }

    /**
     * Set payload.
     *
     * @param payload An array of bytes representing the payload.
     * @param len Length of payload in bytes.
     */
    public void setPayload(byte[] payload, int len) {
        this.payload = payload;
        this.payloadLen = len;
    }

    /**
     * Set payload. Payload length will be determined by the size of the array.
     * @param payload An array of bytes representing the payload.
     */
    public void setPayload(byte[] payload) {
        setPayload(payload, payload.length);
    }

    /**
     * Get source port.
     * @return Source port.
     */
    public int getSrcPort() {
        return srcPort;
    }

    /**
     * Get destination port.
     * @return Destination port.
     */
    public int getDstPort() {
        return dstPort;
    }

    /**
     * Set source port.
     * @param newPort New source port.
     */
    public synchronized void setSrcPort(int newPort) {
        srcPort = newPort;
    }

    /**
     * Set destination port.
     * @param newPort New destination port.
     */
    public synchronized void setDstPort(int newPort) {
        dstPort = newPort;
    }

    /**
     * Get timestamp for when this object was created.
     * @return Timestamp (in milliseconds after epoch).
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Get the age of this object.
     * @return Age of object in milliseconds after epoch.
     */
    public long getAgeInMillis() {
        return System.currentTimeMillis() - getTimestamp();
    }

    /**
     * MD5 of the payload.
     *
     * @return MD5 of payload.
     * @throws NoSuchAlgorithmException if MD5 is not recognized by
     * java.security.MessageDigest.
     */
    public byte[] md5() throws NoSuchAlgorithmException {
        MessageDigest digest = null;

        digest = java.security.MessageDigest.getInstance("MD5");

        return digest.digest(payload);
    }

    /**
     * String representation of the network message.
     * @return String containing source, destination and message content.
     */
    @Override
    public String toString() {
        String src = "(null)";
        String dst = "(null)";
        String msg = "";

        if (getSrcAddress() != null)
            src = getSrcAddress().toString();

        if (getDstAddress() != null)
            dst = getDstAddress().toString();

        msg = getMessage();

        return src + ":" + getSrcPort() + "->" +
               dst + ":" + getDstPort() + " - \"" +
               msg + "\"";
    }
}
