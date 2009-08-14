/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ms.wsdiscovery.xml.soap;

import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.xml.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.xml.jaxb_generated.AttributedURI;
import com.ms.wsdiscovery.xml.jaxb_generated.EndpointReferenceType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeType;
import com.ms.wsdiscovery.xml.jaxb_generated.Relationship;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
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
public class WsdSOAPMessageTest {

    static WsdSOAPMessageBuilder builder;
    static String wsdString;

    public WsdSOAPMessageTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        builder = WsDiscoveryConstants.SOAPBUILDER;
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
     * Test of parseSoap method, of class WsdSOAPMessage.
     */
    @Test
    public void testParseSoap() throws Exception {
        System.out.println("parseSoap");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.HELLO, null); // initial values will be overwritten by parseSoap()
        SOAPMessage soap = builder.createSOAPMessage(wsdString);
        instance.parseSoap(soap);
        assertEquals(instance.getWsaAction().getValue(), WsaActionType.PROBE.toString());
        assertEquals(instance.getWsaTo().getValue(), "urn:schemas-xmlsoap-org:ws:2005:04:discovery");
        assertEquals(instance.getWsaMessageId().getValue(), "urn:uuid:62e53589-fd2f-4882-b8ad-6b7250a085eb");
        assertEquals(instance.getWsdSequenceId(), "urn:uuid:ad721ed8-7c22-40fb-84f3-7145a34fd715");
        assertEquals(instance.getWsdMessageNumber(), 1);
        assertTrue(instance.getJAXBBody() instanceof ProbeType);
        ProbeType pt = (ProbeType) instance.getJAXBBody(); // We know it is a probe
        assertEquals(pt.getTypes().get(0).getLocalPart(), "CalculatorService");
        assertEquals(pt.getTypes().get(0).getNamespaceURI(), "http://calculatorservice.examples.wsdiscovery.ms.com/");
    }

    /**
     * Test of addNamespace method, of class WsdSOAPMessage.
     */
    @Test
    public void testAddNamespace() throws URISyntaxException, WsDiscoveryXMLException {
        System.out.println("addNamespace");

        String prefix = "asdf";
        URI uri = new URI("http://www.example.com/ns/a/b/c");

        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.HELLO, null);

        instance.addNamespace(prefix, uri);
        
        assertTrue(instance.getNamespaces().containsKey(prefix));
        assertTrue(instance.getNamespaces().containsValue(uri));
        assertEquals(instance.getNamespaces(), instance.namespaces);

    }

    /**
     * Test of getNamespaces method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetNamespaces() throws URISyntaxException, WsDiscoveryXMLException {
        System.out.println("getNamespaces");

        WsdSOAPMessage instance = new WsdSOAPMessage(builder.createSOAPMessage(wsdString));

        assertTrue(instance.getNamespaces().containsKey("env"));
        assertTrue(instance.getNamespaces().containsValue(new URI("http://www.w3.org/2003/05/soap-envelope")));

        assertEquals(instance.getNamespaces().get("env"), new URI("http://www.w3.org/2003/05/soap-envelope"));
        assertEquals(instance.getNamespaces().get("wsa"), new URI("http://schemas.xmlsoap.org/ws/2004/08/addressing"));
        assertEquals(instance.getNamespaces().get("wsd"), new URI("http://schemas.xmlsoap.org/ws/2005/04/discovery"));
    }

    /**
     * Test of toSoap method, of class WsdSOAPMessage.
     */
    @Test
    public void testToSoap() throws Exception {
        System.out.println("toSoap");

        ByteArrayOutputStream res = new ByteArrayOutputStream();

        WsdSOAPMessage instance = new WsdSOAPMessage(builder.createSOAPMessage(wsdString));
        SOAPMessage result = instance.toSoap();
        result.writeTo(res);
        
        assertEquals(res.toString(), wsdString);       
    }

    /**
     * Test of getWsaRelatesTo method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsaRelatesTo() {
        System.out.println("getWsaRelatesTo");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.HELLO, null);
        QName name = new QName("a", "b", "c");

        Relationship expResult = new Relationship();
        expResult.setRelationshipType(name);
        expResult.setValue("val");
        instance.setWsaRelatesTo(expResult);

        Relationship result = instance.getWsaRelatesTo();
        assertEquals(expResult.getValue(), result.getValue());
        assertEquals(expResult.getRelationshipType(), result.getRelationshipType());
        assertEquals(instance.getWsaRelatesTo(), instance.wsaRelatesTo);

    }

    /**
     * Test of setWsaRelatesTo method, of class WsdSOAPMessage.
     */
    @Test
    public void testSetWsaRelatesTo() {
        System.out.println("setWsaRelatesTo");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.BYE, null);
        QName name = new QName("a", "b", "c");

        Relationship expResult = new Relationship();
        expResult.setRelationshipType(name);
        expResult.setValue("val");
        instance.setWsaRelatesTo(expResult);

        Relationship result = instance.getWsaRelatesTo();
        assertEquals(expResult.getValue(), result.getValue());
        assertEquals(expResult.getRelationshipType(), result.getRelationshipType());
        assertEquals(instance.getWsaRelatesTo(), instance.wsaRelatesTo);
    }

    /**
     * Test of getWsaReplyTo method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsaReplyTo() {
        System.out.println("getWsaReplyTo");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.BYE, null);
        
        EndpointReferenceType expResult = new EndpointReferenceType();

        /*
        String uri = "http://www.example.com/a/b/c";
         *
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
        
        AttributedQName name = new AttributedQName();
        name.setValue(new QName("a","b","c"));
        
        expResult.setPortType(name);        
        
        AttributedURI aUri = new AttributedURI();
        aUri.setValue(uri);
        
        expResult.setAddress(aUri);*/

        instance.setWsaReplyTo(expResult);

        EndpointReferenceType result = instance.getWsaReplyTo();
        assertEquals(expResult, result);
        assertEquals(instance.getWsaReplyTo(), instance.wsaReplyTo);

    }

    /**
     * Test of setWsaReplyTo method, of class WsdSOAPMessage.
     */
    @Test
    public void testSetWsaReplyTo() {
        System.out.println("setWsaReplyTo");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.BYE, null);

        EndpointReferenceType expResult = new EndpointReferenceType();

        instance.setWsaReplyTo(expResult);

        assertEquals(expResult, instance.wsaReplyTo);
    }

    /**
     * Test of getWsaTo method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsaTo() {
        System.out.println("getWsaTo");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.PROBEMATCHES, null);
        AttributedURI expResult = new AttributedURI();
        instance.setWsaTo(expResult);
        AttributedURI result = instance.getWsaTo();
        assertEquals(expResult, result);
        assertEquals(instance.getWsaTo(), instance.wsaTo);
    }

    /**
     * Test of setWsaTo method, of class WsdSOAPMessage.
     */
    @Test
    public void testSetWsaTo() {
        System.out.println("setWsaTo");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.PROBEMATCHES, null);
        AttributedURI expResult = new AttributedURI();
        instance.setWsaTo(expResult);
        AttributedURI result = instance.getWsaTo();
        assertEquals(expResult, result);
    }
    
    /**
     * Test of getWsaAction method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsaAction() {
        System.out.println("getWsaAction");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.RESOLVE, null);
        AttributedURI expResult = WsaActionType.RESOLVE.toAttributedURI();
        AttributedURI result = instance.getWsaAction();
        assertEquals(expResult, result);
    }

    /**
     * Test of getWsaMessageId method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsaMessageId() {
        System.out.println("getWsaMessageId");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.RESOLVEMATCHES, null);
        AttributedURI expResult = instance.getWsaMessageId();
        assertEquals(expResult.getValue(), instance.wsaMessageId.getValue());
    }

    /**
     * Test of getWsdInstanceId method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsdInstanceId() {
        System.out.println("getWsdInstanceId");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.RESOLVEMATCHES, null);
        long expResult = instance.wsdInstanceId;
        long result = instance.getWsdInstanceId();
        assertEquals(expResult, result);
        assertEquals(WsDiscoveryConstants.instanceId, result);
    }

    /**
     * Test of getWsdMessageNumber method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsdMessageNumber() {
        System.out.println("getWsdMessageNumber");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.PROBEMATCHES, null);
        long result = instance.getWsdMessageNumber();
        assertEquals(result, instance.wsdMessageNumber);
    }

    /**
     * Test of getWsdSequenceId method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetWsdSequenceId() {
        System.out.println("getWsdSequenceId");
        WsdSOAPMessage instance = new WsdSOAPMessage(WsaActionType.PROBE, null);
        String expResult = instance.getWsdSequenceId();
        String result = instance.getWsdSequenceId();
        assertEquals(expResult, result);
        assertEquals(result, "urn:uuid:"+WsDiscoveryConstants.sequenceId);
    }

    /**
     * Test of getJAXBBody method, of class WsdSOAPMessage.
     */
    @Test
    public void testGetJAXBBody() throws WsDiscoveryXMLException {
        System.out.println("getJAXBBody");
        WsdSOAPMessage instance = new WsdSOAPMessage(builder.createSOAPMessage(wsdString));
        Object result = instance.getJAXBBody();
        assertTrue(result instanceof ProbeType);
    }

    /**
     * Test of toString method, of class WsdSOAPMessage.
     */
    @Test
    public void testToString_Charset() throws WsDiscoveryXMLException {
        System.out.println("toString");
        Charset encoding = WsDiscoveryConstants.defaultEncoding;
        WsdSOAPMessage instance = new WsdSOAPMessage(builder.createSOAPMessage(wsdString));
        String expResult = wsdString;
        String result = instance.toString(encoding);
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class WsdSOAPMessage.
     */
    @Test
    public void testToString_0args() throws WsDiscoveryXMLException {
        System.out.println("toString");
        WsdSOAPMessage instance = new WsdSOAPMessage(builder.createSOAPMessage(wsdString));
        String expResult = wsdString;
        String result = instance.toString();
        assertEquals(expResult, result);
    }

}