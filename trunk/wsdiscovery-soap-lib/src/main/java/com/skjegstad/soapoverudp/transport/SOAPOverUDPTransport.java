/*
SOAPOverUDP.java

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
package com.skjegstad.soapoverudp.transport;

import com.skjegstad.soapoverudp.configurations.SOAPOverUDPConfiguration;
import com.skjegstad.soapoverudp.threads.SOAPReceiverThread;
import com.skjegstad.soapoverudp.threads.SOAPSenderThread;
import com.skjegstad.soapoverudp.messages.SOAPOverUDPQueuedNetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPTransport;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDPNetworkMessage;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPNotInitializedException;
import com.skjegstad.soapoverudp.messages.SOAPOverUDPNetworkMessage;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.logging.Logger;

/**
 * Implementation of SOAP-over-UDP for WS-Discovery as specified in 
 * http://schemas.xmlsoap.org/ws/2004/09/soap-over-udp/.
 * 
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPTransport implements ISOAPOverUDPTransport {
    /**
     * Instance of Logger used for debug messages.
     */
    protected Logger logger;
    
    /**
     * Set to true after init() has been called.
     */
    protected boolean initialized = false;

    /**
     * Set to true by start(), false by done(). Used by isRunning() to determine status.
     */
    protected boolean running = false;

    protected Charset encoding = Charset.defaultCharset();

    protected int multicastTtl;

    /**
     * SOAPOverUDP configuration.
     */
    SOAPOverUDPConfiguration soapConfig = null;
    
    // Threads and stuff
    private SOAPReceiverThread receiverThread; // Thread listening for incoming unicast messages
    private SOAPReceiverThread multicastReceiverThread; // Thread listening for incoming multicast messages
    private SOAPSenderThread senderThread; // Thread sending multicast and unicast messages
    private LinkedBlockingQueue<ISOAPOverUDPNetworkMessage> inQueue = new LinkedBlockingQueue<ISOAPOverUDPNetworkMessage>(); // Queue used by the receiver threads
    private DelayQueue<SOAPOverUDPQueuedNetworkMessage> outQueue = new DelayQueue<SOAPOverUDPQueuedNetworkMessage>(); // Queue used by unicastSenderThread
    private int multicastPort;
    private InetAddress multicastAddress;
    private InetAddress unicastAddress;
    private int unicastPort;
    
    /**
     * Empty constructor for use with newInstance(). Call init() to initialize the
     * new instance.
     */
    public SOAPOverUDPTransport() {
        super();
    }
            
    @Override
    public void finalize() throws Throwable {
        try {            
            this.done();
        } finally {
            super.finalize();
        }
    }
    
    /**
     * Put SOAP-message in send queue. 
     * 
     * @param message SOAP message.
     * @param blockUntilSent When true the method will wait until the send-queue is empty. False returns immediately.
     * @throws java.lang.InterruptedException if interrupted while waiting for the message to be sent.
     */
    public void send(ISOAPOverUDPNetworkMessage message, boolean blockUntilSent) throws InterruptedException {
        outQueue.add(new SOAPOverUDPQueuedNetworkMessage(soapConfig, message, message.getDstAddress().equals(this.multicastAddress)));
            if (blockUntilSent)
                while (!outQueue.isEmpty())
                    synchronized (senderThread) {
                        senderThread.wait();
                    }                  
    }

    /**
     * Put SOAP message in send-queue. Returns immediately.
     * 
     * @param message
     */
    public void send(ISOAPOverUDPNetworkMessage message) {
        try {
            send(message, false);
        } catch (InterruptedException ex) {
            // Will never be thrown, as blockUntilSent is false            
        }
    }
    
    /**
     * Receive a SOAP message.
     * 
     * @param timeoutInMillis Time to wait for a message.
     * @return SOAP message. <code>null</code> on timeout.
     * @throws java.lang.InterruptedException if interrupted while waiting for data.
     */
    public ISOAPOverUDPNetworkMessage recv(long timeoutInMillis) throws InterruptedException {
        return inQueue.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Receive a SOAP message.
     * 
     * @return SOAP message. <code>null</code> if interrupted while waiting.
     */
    public ISOAPOverUDPNetworkMessage recv() {
        try {
            return inQueue.take();
        } catch (InterruptedException ex) {
            return null;
        }
    }

    /**
     * Start transport layer.
     */
    public void start() throws SOAPOverUDPNotInitializedException {
        // Check if the class has been initialized
        if (!isInitialized())
            throw new SOAPOverUDPNotInitializedException("start() called before init(). SOAPOverUDP is not initialized.");

        // Start threads
        receiverThread.start();
        senderThread.start();
        multicastReceiverThread.start();
        
        // Wait for threads to get into main loop
        while ((!receiverThread.isRunning()) ||
               (!multicastReceiverThread.isRunning()) ||
               (!senderThread.isRunning()))
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }

        running = true;
    }

    /**
     * Tell transport layer to stop. Returns immediately. Use 
     * {@link SOAPOverUDPTransport#isRunning()} to determine when thread has ended.
     */
    public void done() {
        if (!isRunning())
            return;
        try {
            // Make sure out queues are empty
            while (!outQueue.isEmpty()) {
                synchronized (senderThread) {
                    try {
                        senderThread.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // Stop child threads
            receiverThread.done();
            multicastReceiverThread.done();
            senderThread.done();

            // Wait for childs to complete
            while (receiverThread.isRunning() ||
                    multicastReceiverThread.isRunning() ||
                    senderThread.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        } finally {
            running = false;
        }

    }

    /**
     * Returns the port we listen for multicasts on.
     * @return Multicast port.
     */
    public int getMulticastPort() {
        return multicastPort;
    }

    /**
     * Returns the port used for sending and receiving unicast packets.
     * @return Port used for unicasts.
     */
    public int getUnicastPort() {
        return unicastPort;
    }
    
    /** Get the address of the unicast socket we are listening on. 
     * 
     * @return Addressed used for listening for unicast messages.
     */
    public InetAddress getUnicastAddress() {
        return unicastAddress;
    }

    /**
     * @inheritDoc
     */
    public void init(NetworkInterface multicastInterface, int multicastPort, InetAddress multicastAddress, int multicastTtl, Logger logger) throws SOAPOverUDPException {
        if (soapConfig == null)
            throw new SOAPOverUDPException("SOAPOverUDP not configured.");

        this.logger = logger;

        if (this.logger != null)
            logger.finest("Entering transport.init()");

        this.multicastPort = multicastPort;
        this.multicastAddress = multicastAddress;
        if (multicastTtl > 0)
            this.multicastTtl = multicastTtl;
        else
            this.multicastTtl = 1; // recommended by spec

        MulticastSocket multicastReceive = null;
        try {
            multicastReceive = new MulticastSocket(null);
            if (multicastInterface != null)
                multicastReceive.setNetworkInterface(multicastInterface);
            multicastReceive.setReuseAddress(true); // Required by spec.
            if (!multicastReceive.getReuseAddress())
                throw new SOAPOverUDPException("Platform does not support SO_REUSEADDR");            
            multicastReceive.bind(new InetSocketAddress(multicastPort));
            if (multicastReceive.getLocalPort() != multicastPort)
                throw new SOAPOverUDPException("Unable to bind multicast socket to multicast port.");
            multicastReceive.joinGroup(multicastAddress);
        } catch (IOException ex) {
            throw new SOAPOverUDPException("Unable to open multicast listen socket.", ex);
        }
        
        MulticastSocket mainSocket = null;
        try {
            mainSocket = new MulticastSocket(null); // create unbound socket
            if (multicastInterface != null)
                mainSocket.setNetworkInterface(multicastInterface);
            mainSocket.setReuseAddress(true); // Required by spec.
            if (!mainSocket.getReuseAddress())
                throw new SOAPOverUDPException("Platform does not support SO_REUSEADDR");            
            mainSocket.bind(null); // bind to ephemeral port
        } catch (IOException ex) {
            throw new SOAPOverUDPException("Unable to open main socket.", ex);
        }
                
        this.unicastPort = mainSocket.getLocalPort();
        this.unicastAddress = mainSocket.getLocalAddress();


        try {
            multicastReceive.setTimeToLive(this.multicastTtl);
            mainSocket.setTimeToLive(this.multicastTtl);
        } catch (IOException ex) {
            throw new SOAPOverUDPException("Unable to set TTL to " + this.multicastTtl, ex);
        }

        try {
            receiverThread = new SOAPReceiverThread("recv thread", inQueue, mainSocket, logger);
            multicastReceiverThread = new SOAPReceiverThread("multicast recv thread", inQueue, multicastReceive, logger);
        } catch (SocketException ex) {
            throw new SOAPOverUDPException("Unable to start receiver threads", ex);
        }

        senderThread = new SOAPSenderThread("send thread",
                        outQueue, mainSocket, logger);
        
        initialized = true;
    }


    public boolean isInitialized() {
        return initialized;
    }

    public void setConfiguration(SOAPOverUDPConfiguration configuration) {
        this.soapConfig = configuration;
    }

    public boolean isRunning() {
        return running;
    }

    public void setEncoding(Charset encoding) {
        if (logger != null)
            logger.finer("SOAPOverUDPTransport set encoding to " + encoding.toString());
        this.encoding = encoding;
    }

    public void sendStringMulticast(String string, boolean blockUntilSent) throws InterruptedException {
        this.sendStringUnicast(string, multicastAddress, multicastPort, blockUntilSent);
    }

    public void sendStringUnicast(String string, InetAddress destAddress, int destPort, boolean blockUntilSent) throws InterruptedException {
        if (logger != null)
            logger.finest("sendString: " + string);
        byte[] payload = string.getBytes(encoding);
        ISOAPOverUDPNetworkMessage m = new SOAPOverUDPNetworkMessage(payload, null, 0, destAddress, destPort);
        this.send(m, blockUntilSent);
    }

    public InetAddress getMulticastAddress() {
        return multicastAddress;
    }
}
