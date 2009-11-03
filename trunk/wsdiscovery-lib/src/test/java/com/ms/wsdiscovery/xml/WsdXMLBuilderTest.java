/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.xml;

import com.ms.wsdiscovery.xml.jaxb_generated.AttributedQName;
import com.ms.wsdiscovery.xml.jaxb_generated.AttributedURI;
import com.ms.wsdiscovery.xml.jaxb_generated.EndpointReferenceType;
import com.ms.wsdiscovery.xml.jaxb_generated.ReferenceParametersType;
import com.ms.wsdiscovery.xml.jaxb_generated.ReferencePropertiesType;
import com.ms.wsdiscovery.xml.jaxb_generated.Relationship;
import com.ms.wsdiscovery.xml.jaxb_generated.ServiceNameType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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
public class WsdXMLBuilderTest {

    static WsDiscoveryXMLBuilder instance;

    public WsdXMLBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        instance = new WsDiscoveryXMLBuilder();
        assertNotNull(instance);
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
     * Test of createAttributedURI method, of class WsdXMLBuilder.
     */
    @Test
    public void testCreateAttributedURI() {
        System.out.println("createAttributedURI");
        String uri = "http://www.example.com/a/b/c";        
        AttributedURI result = instance.createAttributedURI(uri);
        assertEquals(result.getValue(), uri);
    }

    /**
     * Test of createAttributedQName method, of class WsdXMLBuilder.
     */
    @Test
    public void testCreateAttributedQName() {
        System.out.println("createAttributedQName");
        QName name = new QName("http://www.example.com/a/b/c/d", "localPart", "ns");
        AttributedQName result = instance.createAttributedQName(name);
        assertEquals(result.getValue(), name);
    }

    /**
     * Test of createRelationship method, of class WsdXMLBuilder.
     */
    @Test
    public void testCreateRelationship() {
        System.out.println("createRelationship");
        String value = "relationshipVal";
        Relationship result = instance.createRelationship(value);
        assertNull(result.getRelationshipType());
        assertEquals(result.getValue(), value);

        QName name = new QName("aaaa", "bbbb", "cccc");
        result.setRelationshipType(name);
        assertEquals(result.getRelationshipType(),name);
    }

    /**
     * Test of createEndpointReference method, of class WsdXMLBuilder.
     */
    @Test
    public void testCreateEndpointReference() {
        System.out.println("createEndpointReference");
        String uri = "http://www.a.com:3030/a/b/c/d";
        EndpointReferenceType result = instance.createEndpointReference(uri);
        assertEquals(result.getAddress().getValue(), uri);
    }

    /**
     * Test of newInstance method, of class WsdXMLBuilder.
     */
    @Test
    public void testNewInstance() throws Exception {
        System.out.println("newInstance");
        JAXBContext j = instance.newInstance();
        assertNotNull(j);
    }

    /**
     * Test of createMarshaller method, of class WsdXMLBuilder.
     */
    @Test
    public void testCreateMarshaller() throws Exception {
        System.out.println("createMarshaller");
        Marshaller expResult = null;
        Marshaller result = instance.createMarshaller();
        assertNotSame(expResult, result);
    }

    /**
     * Test of createUnmarshaller method, of class WsdXMLBuilder.
     */
    @Test
    public void testCreateUnmarshaller() throws Exception {
        System.out.println("createUnmarshaller");
        Unmarshaller expResult = null;
        Unmarshaller result = instance.createUnmarshaller();
        assertNotSame(expResult, result);
    }

    /**
     * Test of cloneEndpointReference method, of class WsdXMLBuilder.
     */
    @Test
    public void testCloneEndpointReference() {
        System.out.println("cloneEndpointReference");
        String uri = "http://www.example.com/a/b/c";
        EndpointReferenceType expResult = instance.createEndpointReference(uri);

        ServiceNameType snt = new ServiceNameType();
        snt.setValue(new QName("http://www.example.com/a", "bbb", "cccc"));
        snt.setPortName("portName");
        expResult.setServiceName(snt);
        
        ReferenceParametersType rpt = new ReferenceParametersType();
        rpt.getAny().add(new String("referenceParameter"));
        expResult.setReferenceParameters(rpt);
        
        ReferencePropertiesType rpt2 = new ReferencePropertiesType();
        rpt2.getAny().add(new String("referenceProperty"));
        expResult.setReferenceProperties(rpt2);
        
        expResult.setPortType(instance.createAttributedQName(new QName("a","b","c")));        
        expResult.setAddress(instance.createAttributedURI("http://www.example.com/a"));

        EndpointReferenceType result = instance.cloneEndpointReference(expResult);

        assertEquals(result.getAddress().getValue(), expResult.getAddress().getValue());
        assertEquals(result.getServiceName().getValue(), expResult.getServiceName().getValue());
        assertEquals(result.getServiceName().getPortName(), expResult.getServiceName().getPortName());
        assertEquals(result.getReferenceParameters().getAny(), expResult.getReferenceParameters().getAny());
        assertEquals(result.getReferenceProperties().getAny(), expResult.getReferenceProperties().getAny());
        assertEquals(result.getPortType().getValue(), expResult.getPortType().getValue());
        assertEquals(result.getAddress().getValue(), expResult.getAddress().getValue());


    }

}