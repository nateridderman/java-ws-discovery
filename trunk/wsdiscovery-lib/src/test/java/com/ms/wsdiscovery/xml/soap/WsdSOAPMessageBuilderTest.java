/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.xml.soap;

import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.xml.jaxb_generated.ByeType;
import com.ms.wsdiscovery.xml.jaxb_generated.EndpointReferenceType;
import com.ms.wsdiscovery.xml.jaxb_generated.HelloType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeType;
import com.ms.wsdiscovery.xml.jaxb_generated.ResolveMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ResolveType;
import java.io.ByteArrayOutputStream;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
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
public class WsdSOAPMessageBuilderTest {

    static String wsdString;
    static WsdSOAPMessageBuilder instance;

    public WsdSOAPMessageBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        wsdString = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsd=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\">"+
                "<env:Header>"+
                "<wsd:AppSequence InstanceId=\"1249385563071\" MessageNumber=\"1\" SequenceId=\"urn:uuid:ad721ed8-7c22-40fb-84f3-7145a34fd715\"/>"+
                "<wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To>"+
                "<wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>"+
                "<wsa:MessageID>urn:uuid:62e53589-fd2f-4882-b8ad-6b7250a085eb</wsa:MessageID>"+
                "</env:Header>"+
                "<env:Body>"+
                "<wsd:Probe><wsd:Types xmlns=\"http://calculatorservice.examples.wsdiscovery.ms.com/\">CalculatorService</wsd:Types></wsd:Probe>" +
                "</env:Body></env:Envelope>";
        instance = new WsdSOAPMessageBuilder();
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
     * Test of createSOAPMessage method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateSOAPMessage_String() throws Exception {
        System.out.println("createSOAPMessage");
        String SoapAsXML = wsdString;
        
        SOAPMessage result = instance.createSOAPMessage(SoapAsXML);

        ByteArrayOutputStream res = new ByteArrayOutputStream();
        result.writeTo(res);
        
        assertEquals(SoapAsXML, res.toString());
    }

    /**
     * Test of createSOAPMessage method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateSOAPMessage_0args() throws Exception {
        System.out.println("createSOAPMessage");
        
        SOAPMessage result = instance.createSOAPMessage();
        assertNotNull(result);
        assertNull(result.getSOAPPart().getValue());        
    }

    /**
     * Test of createWsdSOAPMessage method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessage_String() throws Exception {
        System.out.println("createWsdSOAPMessage");
        String SoapAsXML = wsdString;
        WsdSOAPMessage expResult = new WsdSOAPMessage(instance.createSOAPMessage(SoapAsXML));
        WsdSOAPMessage result = instance.createWsdSOAPMessage(SoapAsXML);
        assertEquals(expResult.toString(), result.toString());
    }

    /**
     * Test of createWsdSOAPMessage method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessage_SOAPMessage() throws Exception {
        System.out.println("createWsdSOAPMessage");
        String SoapAsXML = wsdString;
        WsdSOAPMessage expResult = new WsdSOAPMessage(instance.createSOAPMessage(SoapAsXML));
        WsdSOAPMessage result = instance.createWsdSOAPMessage(instance.createSOAPMessage(SoapAsXML));
        assertEquals(expResult.toString(), result.toString());
    }

    /**
     * Test of createWsdSOAPMessageHello method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageHello_0args() {
        System.out.println("createWsdSOAPMessageHello");
        WsdSOAPMessage result = instance.createWsdSOAPMessageHello();
        assertTrue(result.getJAXBBody() instanceof HelloType);
    }

    /**
     * Test of createWsdSOAPMessageHello method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageHello_WsDiscoveryService() {
        System.out.println("createWsdSOAPMessageHello");
        QName sName = new QName("a","b","c");
        WsDiscoveryService service = WsDiscoveryBuilder.createService(sName, "d", "e");
        WsdSOAPMessage result = instance.createWsdSOAPMessageHello(service);
        assertTrue(result.getJAXBBody() instanceof HelloType);
        HelloType body = (HelloType) result.getJAXBBody();
        assertTrue(body.getTypes().contains(sName));
        EndpointReferenceType expER = service.createEndpointReferenceObject();
        assertEquals(body.getEndpointReference().getAddress().getValue(), expER.getAddress().getValue());
        // TODO More should be tested here..
    }

    /**
     * Test of createWsdSOAPMessageBye method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageBye_0args() {
        System.out.println("createWsdSOAPMessageBye");
        WsdSOAPMessage result = instance.createWsdSOAPMessageBye();
        assertTrue(result.getJAXBBody() instanceof ByeType);
    }

    /**
     * Test of createWsdSOAPMessageBye method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageBye_WsDiscoveryService() {
        System.out.println("createWsdSOAPMessageBye");

        QName sName = new QName("a","b","c");
        WsDiscoveryService service = WsDiscoveryBuilder.createService(sName, "d", "e");
        WsdSOAPMessage result = instance.createWsdSOAPMessageBye(service);
        assertTrue(result.getJAXBBody() instanceof ByeType);
        EndpointReferenceType expER = service.createEndpointReferenceObject();
        ByeType body = (ByeType)result.getJAXBBody();
        assertEquals(body.getEndpointReference().getAddress().getValue(), expER.getAddress().getValue());
    }

    /**
     * Test of createWsdSOAPMessageProbe method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageProbe() {
        System.out.println("createWsdSOAPMessageProbe");
        WsdSOAPMessage result = instance.createWsdSOAPMessageProbe();
        assertTrue(result.getJAXBBody() instanceof ProbeType);
    }

    /**
     * Test of createWsdSOAPMessageProbeMatches method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageProbeMatches() {
        System.out.println("createWsdSOAPMessageProbeMatches");
        WsdSOAPMessage result = instance.createWsdSOAPMessageProbeMatches();
        assertTrue(result.getJAXBBody() instanceof ProbeMatchesType);
    }

    /**
     * Test of createWsdSOAPMessageResolve method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageResolve() {
        System.out.println("createWsdSOAPMessageResolve");
        WsdSOAPMessage result = instance.createWsdSOAPMessageResolve();
        assertTrue(result.getJAXBBody() instanceof ResolveType);
    }

    /**
     * Test of createWsdSOAPMessageResolveMatches method, of class WsdSOAPMessageBuilder.
     */
    @Test
    public void testCreateWsdSOAPMessageResolveMatches() {
        System.out.println("createWsdSOAPMessageResolveMatches");
        WsdSOAPMessageBuilder instance = new WsdSOAPMessageBuilder();
        WsdSOAPMessage result = instance.createWsdSOAPMessageResolveMatches();
        assertTrue(result.getJAXBBody() instanceof ResolveMatchesType);
    }

}