/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.servicedirectory.matcher;

import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.xml.jaxb_generated.ScopesType;
import java.net.URI;
import java.net.URISyntaxException;
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
public class MatchScopeRFC2396Test {

    private WsDiscoveryService service;
    private QName servicePortType;
    private String serviceScope;
    private String serviceXAddr;
    private MatchScopeRFC2396 instance;

    public MatchScopeRFC2396Test() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new MatchScopeRFC2396();
        servicePortType = new QName("http://localhost/portType", "localPart", "ns");
        serviceScope = "http://www.test.com/a/b";
        serviceXAddr = "http://10.0.0.1:1234/localPart";
        service = WsDiscoveryBuilder.createService(servicePortType, serviceScope, serviceXAddr);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of matchScope method, of class MatchScopeRFC2396.
     */
    @Test
    public void testMatchScope() {
        System.out.println("matchScope");
        ScopesType probeScopes = new ScopesType();

        probeScopes.getValue().add("HTTP://www.test.com/a/");
        probeScopes.getValue().add("http://www.test.com/a/b/");

        boolean expResult = true;
        boolean result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);

        probeScopes.getValue().add("fail");
        expResult = false;
        result = instance.matchScope(service, probeScopes);
        assertEquals(expResult, result);
    }

    /**
     * Test of matchURIByRFC2396 method, of class MatchScopeRFC2396.
     */
    @Test
    public void testMatchURIByRFC2396() throws URISyntaxException {
        System.out.println("matchURIByRFC2396");
        
        URI target = new URI("http://www.examples.com/a/b");
        URI probe = new URI("http://www.examples.com/a/b");
        MatchScopeRFC2396 instance = new MatchScopeRFC2396();
        boolean expResult = true;
        boolean result = instance.matchURIByRFC2396(target, probe);
        assertEquals(expResult, result);

        target = new URI("http://www.examples.com/a/b");
        probe = new URI("http://www.examples.com/a/b///");
        instance = new MatchScopeRFC2396();
        expResult = true;
        result = instance.matchURIByRFC2396(target, probe);
        assertEquals(expResult, result);

        target = new URI("http://www.examples.com/a/b");
        probe = new URI("http://www.examples.com/a/");
        instance = new MatchScopeRFC2396();
        expResult = true;
        result = instance.matchURIByRFC2396(target, probe);
        assertEquals(expResult, result);

        target = new URI("http://www.examples.com/a/b");
        probe = new URI("http://www.examples.com/aa");
        instance = new MatchScopeRFC2396();
        expResult = false;
        result = instance.matchURIByRFC2396(target, probe);
        assertEquals(expResult, result);

        target = new URI("http://www.a.com");
        probe = new URI("http://www.b.com");
        instance = new MatchScopeRFC2396();
        expResult = false;
        result = instance.matchURIByRFC2396(target, probe);
        assertEquals(expResult, result);

        target = new URI("http://www.examples.com/a/b");
        probe = new URI("http://www.examples.com/a/b/c/");
        instance = new MatchScopeRFC2396();
        expResult = false;
        result = instance.matchURIByRFC2396(target, probe);
        assertEquals(expResult, result);

    }

}