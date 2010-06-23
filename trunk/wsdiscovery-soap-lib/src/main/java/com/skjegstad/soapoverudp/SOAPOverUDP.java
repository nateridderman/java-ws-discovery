/*
SOAPOverUDP.java

Copyright (C) 2009 Magnus Skjegstad

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

package com.skjegstad.soapoverudp;

import com.skjegstad.soapoverudp.configurations.SOAPOverUDPConfiguration;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDP;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPNetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Abstract SOAPOverUDP class. Handles transport layer and duplicate messages.
 *
 * @author Magnus Skjegstad
 */
public abstract class SOAPOverUDP implements ISOAPOverUDP {

    protected ISOAPOverUDPTransport transport;
    protected Charset encoding = Charset.defaultCharset();
    protected SOAPOverUDPConfiguration soapConfig;
    private LinkedList<URI> messagesReceived = new LinkedList<URI>(); // list of received message IDs
    protected Logger logger;

    public void start(NetworkInterface multicastInterface, int multicastPort, InetAddress multicastAddress, int multicastTtl, Logger logger) throws SOAPOverUDPException {
        if (logger != null)
            synchronized (logger) {
                logger.finer("Starting transport layer...");
            }
        this.logger = logger;
        transport.init(multicastInterface, multicastPort, multicastAddress, multicastTtl, logger);
        transport.start();
    }

    public void sendBlocking(ISOAPOverUDPMessage soapMessage, InetAddress destAddress, int destPort) throws SOAPOverUDPException, InterruptedException{
        if (soapMessage == null)
            throw new SOAPOverUDPException("SOAPOverUDP message is null");
        registerReceived(soapMessage);
        String message = soapMessage.toString(false, encoding);
        transport.sendStringUnicast(message, destAddress, destPort, true);
    }

    public void send(ISOAPOverUDPMessage soapMessage, InetAddress destAddress, int destPort) throws SOAPOverUDPException {
        if (soapMessage == null)
            throw new SOAPOverUDPException("SOAPOverUDP message is null");
        registerReceived(soapMessage);
        String message = soapMessage.toString(false, encoding);
        try {
            transport.sendStringUnicast(message, destAddress, destPort, false);
        } catch (InterruptedException ex) {
            throw new SOAPOverUDPException("Received InterruptedException when sending non-blocking.", ex);
        }
    }

    public void sendMulticast(ISOAPOverUDPMessage soapMessage) throws SOAPOverUDPException {
        if (soapMessage == null)
            throw new SOAPOverUDPException("SOAPOverUDP message is null");
        registerReceived(soapMessage);
        try {
            transport.sendStringMulticast(soapMessage.toString(false, encoding), false);
        } catch (InterruptedException ex) {
            throw new SOAPOverUDPException("Received InterruptedException when sending non-blocking", ex);
        }
    }

    public void sendMulticastBlocking(ISOAPOverUDPMessage soapMessage) throws SOAPOverUDPException, InterruptedException {
        if (soapMessage == null)
            throw new SOAPOverUDPException("SOAPOverUDP message is null");
        registerReceived(soapMessage);
        transport.sendStringMulticast(soapMessage.toString(false, encoding), true);
    }

    protected ISOAPOverUDPMessage _recv(ISOAPOverUDPNetworkMessage m) throws SOAPOverUDPException {
        if (m == null) // this may happen, e.g. when recv() times out
            return null;
        
        String soapAsXML = m.getMessage(encoding);
        ISOAPOverUDPMessage soapMessage = this.createSOAPOverUDPMessageFromXML(soapAsXML);

        if (isAlreadyReceived(soapMessage)) // discard duplicates
            return null;

        soapMessage.setSrcAddress(m.getSrcAddress());
        soapMessage.setSrcPort(m.getSrcPort());
        soapMessage.setDstAddress(m.getDstAddress());
        soapMessage.setDstPort(m.getDstPort());

        registerReceived(soapMessage);

        return soapMessage;
    }

    public ISOAPOverUDPMessage recv(long timeoutInMilliseconds) throws InterruptedException, SOAPOverUDPException {
        ISOAPOverUDPNetworkMessage m = transport.recv(timeoutInMilliseconds);
        return _recv(m);
    }

    public ISOAPOverUDPMessage recv() throws SOAPOverUDPException {
        ISOAPOverUDPNetworkMessage m = transport.recv();
        return _recv(m);
    }

    public boolean isRunning() {
        return transport.isRunning();
    }

    public void done() {
        transport.done();
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
        transport.setEncoding(encoding);
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setTransport(ISOAPOverUDPTransport transportLayer) {
        transport = transportLayer;
        transport.setEncoding(encoding);
        transport.setConfiguration(soapConfig); // configure the layer
    }

    public ISOAPOverUDPTransport getTransport() {
        return transport;
    }

    /**
     * Reads MessageId in <code>soap</code> and registers this messages as received. Used to avoid duplicates.
     * @param soap SOAP-message
     * @throws WsDiscoveryNetworkException if getWsaMessageId() returns null.
     */
    private void registerReceived(ISOAPOverUDPMessage soap) throws SOAPOverUDPException {
        if (soap.getMessageId() == null) {
            throw new SOAPOverUDPException("MessageId was null");
        }
        // TODO Use ringbuffer instead?
        // trim at 1000 entries
        while (messagesReceived.size() > 1000) {
            messagesReceived.removeFirst();
        }
        // add to end
        messagesReceived.add(soap.getMessageId());
    }

    /**
     * Check if the MessageId in the SOAP-messages is already received (used to avoid duplicates)
     * @param soap A SOAP-message
     * @return Whether a message with the same MessageID as the message in <code>soap</code> has been received earlier.
     * @throws SOAPOverUDPException if getMessageId() returns null.
     */
    private boolean isAlreadyReceived(ISOAPOverUDPMessage soap) throws SOAPOverUDPException {
        if (soap.getMessageId() == null) {
            throw new SOAPOverUDPException("Message ID was null.");
        }

        for (URI a : messagesReceived) {
            try {
                if (a.equals(soap.getMessageId())) {
                    return true;
                }
            } catch (NullPointerException ex) {
                logger.finer("isAlreadyReceived() got null pointer exception");
            }
        }
        return false;
    }

    public abstract ISOAPOverUDPMessage createSOAPOverUDPMessageFromXML(String soapAsXML) throws SOAPOverUDPException;
    public abstract ISOAPOverUDPMessage createSOAPOverUDPMessage() throws SOAPOverUDPException;
}
