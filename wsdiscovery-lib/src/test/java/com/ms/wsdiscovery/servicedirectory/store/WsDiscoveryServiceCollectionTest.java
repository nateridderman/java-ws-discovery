/*
WsDiscoveryServiceCollectionTest.java

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

package com.ms.wsdiscovery.servicedirectory.store;

import com.ms.wsdiscovery.WsDiscoveryConstants;
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
 * @author Magnus Skjegstad
 */
public class WsDiscoveryServiceCollectionTest {

    public WsDiscoveryServiceCollectionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
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
     * Test of indexOf method, of class WsDiscoveryServiceCollection.
     */
    @Test
    public void testIndexOf() {
        System.out.println("indexOf");

        WsDiscoveryService service = new WsDiscoveryService(new QName("a"), new WsDiscoveryScopesType(), "xaddr");
        String endpointReference = service.getEndpointReference().getAddress().toString();
        WsDiscoveryServiceCollection instance = new WsDiscoveryServiceCollection();

        instance.add(service);
        int expResult = 0;
        int result = instance.indexOf(endpointReference);

        assertEquals(expResult, result);

        instance.add(service);        
        expResult = 0;
        result = instance.indexOf(endpointReference);

        assertEquals(expResult, result);
        
        endpointReference = UUID.randomUUID().toString();
        EndpointReferenceType er = new EndpointReferenceType();
        service = new WsDiscoveryService(new QName("b"), new ScopesType(), "xaddr");
        er.setAddress(WsDiscoveryConstants.XMLBUILDER.createAttributedURI(endpointReference));
        service.setEndpointReferenceType(er);
        
        expResult = 2;
        instance.add(service);
        result = instance.indexOf(endpointReference);
        
        assertEquals(expResult, result);
    }

    /**
     * Test of update method, of class WsDiscoveryServiceCollection.
     */
    @Test
    public void testUpdate() {
        System.out.println("update");

        WsDiscoveryServiceCollection instance = new WsDiscoveryServiceCollection();

        // Service description 1
        WsDiscoveryService service1 = new WsDiscoveryService(new QName("a"), new ScopesType(), "xaddr1");
        String endpointReference = service1.getEndpointReference();

        // Service description 2, with the same endpoint reference as service 1
        WsDiscoveryService service2 = new WsDiscoveryService(new QName("b"), new ScopesType(), "xaddr2");
        EndpointReferenceType er = new EndpointReferenceType();
        er.setAddress(WsDiscoveryConstants.XMLBUILDER.createAttributedURI(endpointReference));
        service2.setEndpointReferenceType(er);

        // Add service 1
        instance.add(service1);
        assertEquals(instance.indexOf(service1), 0);

        // Change service 2 to contain description from service 2
        assertTrue(instance.update(service2));

        // Both should return true, as only the endpoint reference is checked by indexOf
        assertEquals(instance.indexOf(service1.getEndpointReference()), 0);
        assertEquals(instance.indexOf(service2.getEndpointReference()), 0);

        // Service1 should now be replaced by Service2
        assertEquals(instance.indexOf(service1), -1);
        assertEquals(instance.indexOf(service2), 0);

        // Verify that the service description was changed
        assertTrue(instance.get(0).getEndpointReference().equals(endpointReference));
        assertTrue(instance.get(0).getXAddrs().get(0).equals("xaddr2"));
        assertTrue(instance.get(0).getPortTypes().get(0).equals(new QName("b")));
    }

    /**
     * Test of contains method, of class WsDiscoveryServiceCollection.
     */
    @Test
    public void testContains() {
        System.out.println("contains");
        WsDiscoveryServiceCollection instance = new WsDiscoveryServiceCollection();

        // Service description 1
        WsDiscoveryService service1 = new WsDiscoveryService(new QName("a"), new ScopesType(), "xaddr1");

        // Service description 2
        WsDiscoveryService service2 = new WsDiscoveryService(new QName("b"), new ScopesType(), "xaddr2");

        // Service description 3
        WsDiscoveryService service3 = new WsDiscoveryService(new QName("3"), new ScopesType(), "xaddr3");

        // Add service 1
        instance.add(service1);
        instance.add(service2);

        assertTrue(instance.contains(service1.getEndpointReference()));
        assertTrue(instance.contains(service2.getEndpointReference()));
        assertFalse(instance.contains(service3.getEndpointReference()));

        // This is a different method, but test it anyway
        assertTrue(instance.contains(service1));
        assertTrue(instance.contains(service2));
        assertFalse(instance.contains(service3));
    }

}