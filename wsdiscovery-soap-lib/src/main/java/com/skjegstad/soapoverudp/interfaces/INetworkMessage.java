/*
INetworkMessage.java

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

import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

/**
 * Interface implemented by {@link NetworkMessage}.
 *
 * @author Magnus Skjegstad
 */
public interface INetworkMessage {

    /**
     * Get destination address.
     *
     * @return Destination address
     */
    InetAddress getDstAddress();

    /**
     * Get destination port.
     * @return Destination port.
     */
    int getDstPort();

    /**
     * Get current payload.
     *
     * @return Payload as array of bytes.
     */
    byte[] getPayload();

    /**
     * Get length of payload in bytes.
     *
     * @return Length of current payload in bytes.
     */
    int getPayloadLen();

    /**
     * Get source address.
     *
     * @return Source address
     */
    InetAddress getSrcAddress();

    /**
     * Get source port.
     * @return Source port.
     */
    int getSrcPort();

    /**
     * Get timestamp for when this object was created.
     * @return Timestamp (in milliseconds after epoch).
     */
    long getTimestamp();

    /**
     * MD5 of the payload.
     *
     * @return MD5 of payload.
     * @throws NoSuchAlgorithmException if MD5 is not recognized by
     * java.security.MessageDigest.
     */
    byte[] md5() throws NoSuchAlgorithmException;

    /**
     * Set destination address.
     *
     * @param newAddress New destination address
     */
    void setDstAddress(InetAddress newAddress);

    /**
     * Set destination port.
     * @param newPort New destination port.
     */
    void setDstPort(int newPort);  

    /**
     * Set payload.
     *
     * @param payload An array of bytes representing the payload.
     * @param len Length of payload in bytes.
     */
    void setPayload(byte[] payload, int len);

    /**
     * Set payload. Payload length will be determined by the size of the array.
     * @param payload An array of bytes representing the payload.
     */
    void setPayload(byte[] payload);

    /**
     * Set source address.
     *
     * @param newAddress New source address
     */
    void setSrcAddress(InetAddress newAddress);

    /**
     * Set source port.
     * @param newPort New source port.
     */
    void setSrcPort(int newPort);

    /**
     * String representation of the network message.
     * @return String containing source, destination and message content.
     */
    @Override
    String toString();

}
