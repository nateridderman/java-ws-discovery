/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.network.interfaces;

import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

/**
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
