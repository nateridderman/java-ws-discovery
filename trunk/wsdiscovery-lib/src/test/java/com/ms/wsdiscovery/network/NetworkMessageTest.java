/*
NetworkMessageTest.java

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

import com.ms.wsdiscovery.WsDiscoveryConstants;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Magnus Skjegstad
 */
public class NetworkMessageTest {

    public NetworkMessageTest() {
    }

    private static NetworkMessage instance;
    private static InetAddress srcAddress;
    private static InetAddress dstAddress;
    private static int dstPort;
    private static int srcPort;
    private static String message;

    @BeforeClass
    public static void setUpClass() throws Exception {
        dstPort = 1234;
        srcPort = 4321;
        srcAddress = InetAddress.getByName("10.0.0.1");
        dstAddress = InetAddress.getByName("10.0.0.2");
        message = "This is a test message";

        instance = new NetworkMessage(message, srcAddress, srcPort, dstAddress, dstPort);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSrcAddress method, of class NetworkMessage.
     */
    @Test
    public void testGetSrcAddress() {
        System.out.println("getSrcAddress");
        InetAddress expResult = srcAddress;
        InetAddress result = instance.getSrcAddress();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDstAddress method, of class NetworkMessage.
     */
    @Test
    public void testGetDstAddress() {
        System.out.println("getDstAddress");
        InetAddress expResult = dstAddress;
        InetAddress result = instance.getDstAddress();
        assertEquals(expResult, result);
    }

    /**
     * Test of setSrcAddress method, of class NetworkMessage.
     */
    @Test
    public void testSetSrcAddress() throws UnknownHostException {
        System.out.println("setSrcAddress");
        InetAddress newAddress = InetAddress.getByName("10.0.0.3");
        instance.setSrcAddress(newAddress);
        assertEquals(instance.getSrcAddress(), newAddress);
        instance.setSrcAddress(srcAddress);
        assertEquals(instance.getSrcAddress(), srcAddress);
    }

    /**
     * Test of setDstAddress method, of class NetworkMessage.
     */
    @Test
    public void testSetDstAddress() throws UnknownHostException {
        System.out.println("setDstAddress");
        InetAddress newAddress = InetAddress.getByName("10.0.0.4");
        instance.setDstAddress(newAddress);
        assertEquals(instance.getDstAddress(), newAddress);
        instance.setDstAddress(srcAddress);
        assertEquals(instance.getDstAddress(), srcAddress);
    }

    /**
     * Test of getMessage method, of class NetworkMessage.
     */
    @Test
    public void testGetMessage() {
        System.out.println("getMessage");
        String expResult = message;
        String result = instance.getMessage();
        assertEquals(expResult, result);
    }

    /**
     * Test of setMessage method, of class NetworkMessage.
     */
    @Test
    public void testSetMessage() {
        System.out.println("setMessage");
        String newMessage = "This is another testmessage.";
        instance.setMessage(newMessage);
        assertEquals(instance.getMessage(), newMessage);
        instance.setMessage(message);
        assertEquals(instance.getMessage(), message);
    }

    /**
     * Test of getPayload method, of class NetworkMessage.
     */
    @Test
    public void testGetPayload() {
        System.out.println("getPayload");        
        byte[] expResult = instance.payload;
        byte[] result = instance.getPayload();
        assertEquals(expResult.length, result.length);
        assertEquals(expResult, result);
    }

    /**
     * Test of getPayloadLen method, of class NetworkMessage.
     */
    @Test
    public void testGetPayloadLen() {
        System.out.println("getPayloadLen");
        int expResult = instance.payload.length;
        int result = instance.getPayloadLen();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPayload method, of class NetworkMessage.
     */
    @Test
    public void testSetPayload_byteArr_int() throws UnsupportedEncodingException {
        System.out.println("setPayload");
        byte[] payload = "This is a payload test1".getBytes(WsDiscoveryConstants.defaultEncoding.name());
        int len = payload.length;

        instance.setPayload(payload, len);

        assertEquals(payload, instance.payload);
        assertEquals(len, instance.payloadLen);
    }

    /**
     * Test of setPayload method, of class NetworkMessage.
     */
    @Test
    public void testSetPayload_byteArr() throws UnsupportedEncodingException {
        System.out.println("setPayload");
        byte[] payload = "This is a payload test2".getBytes(WsDiscoveryConstants.defaultEncoding.name());

        instance.setPayload(payload);

        assertEquals(payload, instance.payload);
        assertEquals(payload.length, instance.payloadLen);
    }

    /**
     * Test of getSrcPort method, of class NetworkMessage.
     */
    @Test
    public void testGetSrcPort() {
        System.out.println("getSrcPort");
        int expResult = srcPort;
        int result = instance.getSrcPort();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDstPort method, of class NetworkMessage.
     */
    @Test
    public void testGetDstPort() {
        System.out.println("getDstPort");
        int expResult = dstPort;
        int result = instance.getDstPort();
        assertEquals(expResult, result);
    }

    /**
     * Test of setSrcPort method, of class NetworkMessage.
     */
    @Test
    public void testSetSrcPort() {
        System.out.println("setSrcPort");
        int newPort = 1111;
        instance.setSrcPort(newPort);
        assertEquals(newPort, instance.srcPort);
    }

    /**
     * Test of setDstPort method, of class NetworkMessage.
     */
    @Test
    public void testSetDstPort() {
        System.out.println("setDstPort");
        int newPort = 2222;
        instance.setDstPort(newPort);
        assertEquals(newPort, instance.dstPort);
    }

    /**
     * Test of getTimestamp method, of class NetworkMessage.
     */
    @Test
    public void testGetTimestamp() {
        System.out.println("getTimestamp");
        long expResult = instance.timestamp;
        long result = instance.getTimestamp();
        assertEquals(expResult, result);
        assertFalse(result == 0);
    }

    /**
     * Test of getAgeInMillis method, of class NetworkMessage.
     */
    @Test
    public void testGetAgeInMillis() throws InterruptedException {
        System.out.println("getAgeInMillis");        
        Thread.sleep(500);
        long result = instance.getAgeInMillis();
        assertTrue(result > 450);
        Thread.sleep(500);
        assertTrue(result+450 < instance.getAgeInMillis());
    }

    /**
     * Test of md5 method, of class NetworkMessage.
     */
    @Test
    public void testMd5() throws Exception {
        System.out.println("md5");

        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        
        byte[] expResult = digest.digest(message.getBytes(WsDiscoveryConstants.defaultEncoding));
        
        instance.setMessage(message);
        
        byte[] result = instance.md5();

        assertTrue(Arrays.equals(result, expResult));
    }

    /**
     * Test of toString method, of class NetworkMessage.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        String result = instance.toString();
        assertNotNull(result);
    }

}