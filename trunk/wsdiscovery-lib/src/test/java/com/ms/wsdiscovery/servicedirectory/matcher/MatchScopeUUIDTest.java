/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.servicedirectory.matcher;

import com.ms.wsdiscovery.WsDiscoveryFactory;
import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import java.util.UUID;
import javax.xml.namespace.QName;
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
public class MatchScopeUUIDTest {

    private WsDiscoveryService service;
    private QName servicePortType;
    private String serviceScope;
    private String serviceXAddr;
    private MatchScopeUUID instance;

    public MatchScopeUUIDTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new MatchScopeUUID();        
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of matchScope method, of class MatchScopeUUID.
     */
    @Test
    public void testMatchScope() {
        System.out.println("matchScope");
        String randomUUID = UUID.randomUUID().toString();
        String otherUUID = UUID.randomUUID().toString();

        assertFalse(randomUUID.equals(otherUUID));

        servicePortType = new QName("http://localhost/portType", "localPart", "ns");        
        serviceXAddr = "http://10.0.0.1:1234/localPart";

        WsDiscoveryScopesType probeScopes = null;

        boolean expResult;
        boolean result;
        
        ///////////
        serviceScope = randomUUID; // without prefix first
        service = WsDiscoveryFactory.createService(servicePortType, serviceScope, serviceXAddr);
        ///////////

        // First, just use uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Try uppercase of uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID.toUpperCase());
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Prefix with urn:uuid:
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("urn:uuid:"+randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // prefix with uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("uuid:"+randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);


        ///////////
        serviceScope = "uuid:" + randomUUID; // add prefix uuid
        service = WsDiscoveryFactory.createService(servicePortType, serviceScope, serviceXAddr);
        ///////////

        // First, just use uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Try uppercase of uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID.toUpperCase());
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Prefix with urn:uuid:
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("urn:uuid:"+randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // prefix with uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("uuid:"+randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        ///////////
        serviceScope = "urn:uuid:" + randomUUID; // add prefix urn:uuid
        service = WsDiscoveryFactory.createService(servicePortType, serviceScope, serviceXAddr);
        ///////////

        // First, just use uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Try uppercase of uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID.toUpperCase());
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Prefix with urn:uuid:
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("urn:uuid:"+randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // prefix with uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("uuid:"+randomUUID);
        expResult = true;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        ///////////
        serviceScope = "urn:uuid:" + otherUUID; // try with something else 
        service = WsDiscoveryFactory.createService(servicePortType, serviceScope, serviceXAddr);
        ///////////

        // All these should be false.

        // First, just use uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID);
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Try uppercase of uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add(randomUUID.toUpperCase());
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // Prefix with urn:uuid:
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("urn:uuid:"+randomUUID);
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        // prefix with uuid
        probeScopes = new WsDiscoveryScopesType();
        probeScopes.getValue().add("uuid:"+randomUUID);
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

    }
}