package com.skjegstad.soapoverudp.transport;

import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.FidelityOptions;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.siemens.ct.exi.api.sax.EXIResult;
import com.siemens.ct.exi.api.sax.EXISource;
import com.siemens.ct.exi.helpers.DefaultEXIFactory;

import com.skjegstad.soapoverudp.messages.SOAPOverUDPNetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPNetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPConfigurable;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

/**
 * An implementation of SOAP-over-UDP using EFX-compression.
 *
 * @author Frank T. Johnsen
 */
public class SOAPOverUDPTransportEfx extends SOAPOverUDPTransport implements ISOAPOverUDPTransport, ISOAPOverUDPConfigurable {

    private EfficientXML efx;

    public SOAPOverUDPTransportEfx() {
        super();
        efx = new EfficientXML();
    }

    /**
     * Receive message.
     *
     * @param timeoutInMillis Time to wait for new message.
     * @return Message or <code>null</code> on timeout.
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    @Override
    public ISOAPOverUDPNetworkMessage recv(long timeoutInMillis) throws InterruptedException {
        ISOAPOverUDPNetworkMessage nm = super.recv(timeoutInMillis);
        
        if (nm != null && nm.getPayload() != null) {
	    String data = decompress(nm.getPayload());
	    nm.setMessage(data, encoding);
        }
	
        return nm;
    }

    /**
     * Send message.
     *
     * @param message Message to be sent.
     * @param blockUntilSent If true, block until all messages are sent (queue is empty).
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    @Override
    public void send(ISOAPOverUDPNetworkMessage message, boolean blockUntilSent)
            throws InterruptedException {

	//System.err.println("EFX going to compress:\n---\n"+message.getMessage(encoding)+"\n---\n");
        byte[] payload = compress(message.getMessage(encoding));
        ISOAPOverUDPNetworkMessage nm = new SOAPOverUDPNetworkMessage(
                payload, payload.length,
                message.getSrcAddress(), message.getSrcPort(),
                message.getDstAddress(), message.getDstPort());

        super.send(nm, blockUntilSent);
    }

    protected String decompress(byte[] message) {
        String s = null;
        try {
            s = efx.decompress(message);
	    s = s.substring(38);
	    //System.err.println("EFX decompressed:\n---\n"+s+"\n---\n");
        } catch (EfficientXMLException ex) {
            Logger.getLogger(SOAPOverUDPTransportEfx.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("FATAL: " + ex.getMessage());
            System.exit(0);
        }
        return s;
    }

    protected byte[] compress(String message) {
        byte[] b = null;
        try {
            b = efx.compress(message);
        } catch (EfficientXMLException ex) {
            Logger.getLogger(SOAPOverUDPTransportEfx.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("FATAL: " + ex.getMessage());
            System.exit(0);
        }
        return b;
    }
}

class EfficientXMLException extends Exception {
    public EfficientXMLException() {
        super();
    }

    public EfficientXMLException(String message) {
        super(message);
    }

    public EfficientXMLException(String message, Throwable cause) {
        super(message, cause);
    }

    public EfficientXMLException(Throwable cause) {
        super(cause);
    }
}

class EfficientXML {
            
    // decompress a byte array to an xml document using schema (or not)
    public String decompress(byte[] data) throws EfficientXMLException {
        String s = "";

        // TODO: put in constructor
        EXIFactory exiFactory = null;
        exiFactory = DefaultEXIFactory.newInstance();
        exiFactory.setFidelityOptions(FidelityOptions.createAll());
        exiFactory.setCodingMode(CodingMode.COMPRESSION);

        try {        
            // decode
            EXISource saxSource = new EXISource(exiFactory);
            XMLReader xmlReader = saxSource.getXMLReader();
            s = decode(xmlReader, data);
        } catch (Exception e) {
            throw new EfficientXMLException("ExiLib: error decompressing", e);
        }

        return s;
    }

    private String decode(XMLReader exiReader, byte[] data)
            throws SAXException, IOException, TransformerException {

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        InputStream exiIS = new ByteArrayInputStream(data);
        SAXSource exiSource = new SAXSource(new InputSource(exiIS));
        exiSource.setXMLReader(exiReader);

        OutputStream os = new ByteArrayOutputStream();
        transformer.transform(exiSource, new StreamResult(os));
        ByteArrayOutputStream bos = (ByteArrayOutputStream) os;
        String xml = new String(bos.toByteArray());
        os.close();

        return xml;
    }

    private void encode(ContentHandler ch, String data) throws SAXException, IOException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(ch);

        ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes());

        // parse xml file
        //xmlReader.parse(new InputSource(xmlLocation));
        xmlReader.parse(new InputSource(bais));

    }

    // compress an xml document using schema (or not)
    public byte[] compress(String data) throws EfficientXMLException {
        byte[] c;

        // TODO: put in constructor
        EXIFactory exiFactory = null;
        exiFactory = DefaultEXIFactory.newInstance();
        exiFactory.setFidelityOptions(FidelityOptions.createAll());
        exiFactory.setCodingMode(CodingMode.COMPRESSION);

        try {           
            // encode
            OutputStream exiOS = new ByteArrayOutputStream();
            SAXResult exiResult = new EXIResult(exiOS, exiFactory);
            encode(exiResult.getHandler(), data);
            ByteArrayOutputStream bos = (ByteArrayOutputStream) exiOS;
            c = bos.toByteArray();
            exiOS.close();
        } catch (Exception e) {
            throw new EfficientXMLException("ExiLib: error compressing", e);
        }

        return c;
    }
}
