/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.servicedirectory.matcher;

import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.xml.jaxb_generated.ScopesType;
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
public class MatchScopeStrcmp0Test {

    private WsDiscoveryService service;
    private QName servicePortType;
    private String serviceScope;
    private String serviceXAddr;
    private MatchScopeStrcmp0 instance;

    public MatchScopeStrcmp0Test() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new MatchScopeStrcmp0();
        servicePortType = new QName("http://localhost/portType", "localPart", "ns");
        serviceScope = "www.test.com/a/b";
        serviceXAddr = "http://10.0.0.1:1234/localPart";
        service = WsDiscoveryBuilder.createService(servicePortType, serviceScope, serviceXAddr);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of matchScope method, of class MatchScopeStrcmp0.
     */
    @Test
    public void testMatchScope() {
        System.out.println("matchScope");
        
        ScopesType probeScopes = null;

        probeScopes = new ScopesType();
        probeScopes.getValue().add(serviceScope);

        boolean expResult = true;
        boolean result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        probeScopes = new ScopesType();
        probeScopes.getValue().add(serviceScope.toUpperCase());
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);


        probeScopes = new ScopesType();
        probeScopes.getValue().add("www.test.com/a/");
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        probeScopes = new ScopesType();
        probeScopes.getValue().add("www.test.com/aa/");
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        probeScopes = new ScopesType();
        probeScopes.getValue().add("http://www.test.com/a/");
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

    }

}