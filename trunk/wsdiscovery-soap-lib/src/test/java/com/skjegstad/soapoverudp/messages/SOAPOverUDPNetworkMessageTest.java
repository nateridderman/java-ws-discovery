/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.skjegstad.soapoverudp.messages;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
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
 * @author magnus
 */
public class SOAPOverUDPNetworkMessageTest {

    private static SOAPOverUDPNetworkMessage instance;
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

        instance = new SOAPOverUDPNetworkMessage(message.getBytes(), srcAddress, srcPort, dstAddress, dstPort);
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
     * Test of getPayload method, of class NetworkMessage.
     */
    @Test
    public void testGetPayload() {
        System.out.println("getPayload");
        byte[] expResult = instance.getPayload();
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
        int expResult = instance.getPayload().length;
        int result = instance.getPayloadLen();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPayload method, of class NetworkMessage.
     */
    @Test
    public void testSetPayload_byteArr_int() throws UnsupportedEncodingException {
        System.out.println("setPayload");
        byte[] payload = "This is a payload test1".getBytes();
        int len = payload.length;

        instance.setPayload(payload, len);

        assertEquals(payload, instance.getPayload());
        assertEquals(len, instance.getPayloadLen());
    }

    /**
     * Test of setPayload method, of class NetworkMessage.
     */
    @Test
    public void testSetPayload_byteArr() throws UnsupportedEncodingException {
        System.out.println("setPayload");
        byte[] payload = "This is a payload test2".getBytes();

        instance.setPayload(payload);

        assertEquals(payload, instance.getPayload());
        assertEquals(payload.length, instance.getPayloadLen());
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
        assertEquals(newPort, instance.getSrcPort());
    }

    /**
     * Test of setDstPort method, of class NetworkMessage.
     */
    @Test
    public void testSetDstPort() {
        System.out.println("setDstPort");
        int newPort = 2222;
        instance.setDstPort(newPort);
        assertEquals(newPort, instance.getDstPort());
    }

    /**
     * Test of getTimestamp method, of class NetworkMessage.
     */
    @Test
    public void testGetTimestamp() {
        System.out.println("getTimestamp");
        long expResult = instance.getTimestamp();
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

        byte[] expResult = digest.digest(message.getBytes());

        instance.setPayload(message.getBytes());

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