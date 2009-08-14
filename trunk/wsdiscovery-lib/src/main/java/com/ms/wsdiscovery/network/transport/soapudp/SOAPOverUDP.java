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
package com.ms.wsdiscovery.network.transport.soapudp;

import com.ms.wsdiscovery.network.transport.interfaces.ITransportType;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.logger.WsdLogger;
import com.ms.wsdiscovery.network.NetworkMessage;
import com.ms.wsdiscovery.network.transport.exception.WsDiscoveryTransportException;

/**
 * Implementation of SOAP-over-UDP for WS-Discovery as specified in 
 * http://schemas.xmlsoap.org/ws/2004/09/soap-over-udp/.
 * 
 * @author Magnus Skjegstad
 */
public class SOAPOverUDP implements ITransportType {
    /**
     * Instance of Logger used for debug messages.
     */
    protected WsdLogger logger;
    
    // Threads and stuff
    private SOAPReceiverThread multicastReceiverThread; // Thread listening for incoming multicast messages
    private SOAPReceiverThread unicastReceiverThread; // Thread listening for incoming unicast messages
    private SOAPSenderThread multicastSenderThread; // Thread sending multicast messages
    private SOAPSenderThread unicastSenderThread; // Thread sending unicast messages
    private LinkedBlockingQueue<NetworkMessage> inQueue = new LinkedBlockingQueue<NetworkMessage>(); // Queue used by the receiver threads
    private DelayQueue<SOAPNetworkMessage> outUnicastQueue = new DelayQueue<SOAPNetworkMessage>(); // Queue used by unicastSenderThread
    private DelayQueue<SOAPNetworkMessage> outMulticastQueue = new DelayQueue<SOAPNetworkMessage>(); // Queue used by multicastSenderThread
    private final int multicastPort;
    private final InetAddress multicastAddress;
    
    // Default vaules for retry and back-off algorithm (see Appendix I in the SOAP-over-UDP draft)
    /**
     * Number of times to repeat unicast messages.
     */
    public static final int UNICAST_UDP_REPEAT = 2;
    /**
     * Number of times to repeat multicast messages.
     */
    public static final int MULTICAST_UDP_REPEAT = 4;
    /**
     * Minimum initial delay for resend.
     */
    public static final int UDP_MIN_DELAY = 50;
    /**
     * Maximum initial delay for resend.
     */
    public static final int UDP_MAX_DELAY = 250;
    /**
     * Maximum delay between resent messages.
     */
    public static final int UDP_UPPER_DELAY = 500;

    /**
     * 
     * @param multicastPort Port for sending and receiving multicast messages
     * @param multicastAddress Address for sending and listening to multicast messages.
     * @throws WsDiscoveryTransportException if an error occured while opening 
     * the sockets or creating child threads.
     */
    public SOAPOverUDP(int multicastPort, InetAddress multicastAddress) 
            throws WsDiscoveryTransportException {
        
        logger = new WsdLogger(this.getClass().getName());
        
        this.multicastPort = multicastPort;
        this.multicastAddress = multicastAddress;
        
        MulticastSocket multiSock = null;
        DatagramSocket uniSock = null;
        
        try {
            multiSock = new MulticastSocket(null);        
            multiSock.setReuseAddress(true); // Required by spec.
            if (!multiSock.getReuseAddress())
                throw new WsDiscoveryTransportException("Platform doesn't support SO_REUSEADDR");        
            multiSock.setTimeToLive(1); // Suggested by spec
            multiSock.bind(new InetSocketAddress(multicastPort));
            multiSock.joinGroup(multicastAddress);
        } catch (IOException ex) {
            throw new WsDiscoveryTransportException("Unable to open multicast socket.");
        }        
            
        try {
            uniSock = new DatagramSocket(null);
            uniSock.setReuseAddress(true);
            if (!uniSock.getReuseAddress())
                throw new WsDiscoveryTransportException("Platform doesn't support SO_REUSEADDR");        
            uniSock.bind(new InetSocketAddress(multicastPort));        
        } catch (IOException ex) {
            throw new WsDiscoveryTransportException("Unable to open unicast socket.");
        }
        
        multicastReceiverThread = new SOAPReceiverThread("multicast_recv", 
                    inQueue, multiSock);
        
        unicastSenderThread = new SOAPSenderThread("unicast_send", 
                    outUnicastQueue, uniSock);
        try {
            multicastSenderThread = new SOAPSenderThread("multicast_send", 
                        outMulticastQueue);
        } catch (SocketException ex) {
            throw new WsDiscoveryTransportException("Unable to start multicast send thread.");
        }
        
        unicastReceiverThread = new SOAPReceiverThread("unicast_recv", 
                    inQueue, multicastSenderThread.getSocket());
    }
    
    /**
     * Create new instance using multicast configuration from the default values in
     * {@link WsDiscoveryConstants#multicastAddress} and {@link WsDiscoveryConstants#multicastPort}.
     * 
     * @throws WsDiscoveryTransportException if an error occured while opening 
     * the sockets or creating child threads.
     */
    public SOAPOverUDP() throws WsDiscoveryTransportException {
        this(WsDiscoveryConstants.multicastPort, WsDiscoveryConstants.multicastAddress);
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
    public void send(NetworkMessage message, boolean blockUntilSent) throws InterruptedException {
        // Multicast
        if (message.getDstAddress().equals(this.multicastAddress)) { 
            outMulticastQueue.add(new SOAPNetworkMessage(message, true));
            if (blockUntilSent)
                while (!outMulticastQueue.isEmpty())
                    synchronized (multicastSenderThread) {
                        multicastSenderThread.wait();
                    }
        // Unicast
        } else {
            outUnicastQueue.add(new SOAPNetworkMessage(message, false));
            if (blockUntilSent)
                while (!outUnicastQueue.isEmpty())
                    synchronized (unicastSenderThread) {
                        unicastSenderThread.wait();
                    }            
        }
    }
    /**
     * Put SOAP message in send-queue. Returns immediately.
     * 
     * @param message
     */
    public void send(NetworkMessage message) {
        try {
            send(message, false);
        } catch (InterruptedException ex) {} // Will never be thrown, since blockUntilSent is false
    }
    
    /**
     * Receive a SOAP message.
     * 
     * @param timeoutInMillis Time to wait for a message.
     * @return SOAP message. <code>null</code> on timeout.
     * @throws java.lang.InterruptedException if interrupted while waiting for data.
     */
    public NetworkMessage recv(long timeoutInMillis) throws InterruptedException {
        return inQueue.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Receive a SOAP message.
     * 
     * @return SOAP message. <code>null</code> if interrupted while waiting.
     */
    public NetworkMessage recv() {
        try {
            return inQueue.take();
        } catch (InterruptedException ex) {
            return null;
        }
    }

    /**
     * Start transport layer.
     */
    public void start() {
        // Start threads
        multicastReceiverThread.start();
        multicastSenderThread.start();
        unicastReceiverThread.start();
        unicastSenderThread.start();
        
        // Wait for threads to get into main loop
        while ((!multicastReceiverThread.isRunning()) ||
               (!multicastSenderThread.isRunning()) ||
               (!unicastReceiverThread.isRunning()) ||
               (!unicastSenderThread.isRunning()))
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
    }

    /**
     * Tell transport layer to stop. Returns immediately. Use 
     * {@link Thread#isAlive()} to determine when thread has ended.
     */
    public void done() {
        // Make sure out queues are empty
        while (!outUnicastQueue.isEmpty()) {
            synchronized (unicastSenderThread) {
                try {
                    unicastSenderThread.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        while (!outMulticastQueue.isEmpty()) {
            synchronized (multicastSenderThread) {
                try {
                    multicastSenderThread.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        
        // Stop child threads
        multicastReceiverThread.done();
        multicastSenderThread.done();
        unicastReceiverThread.done();
        unicastSenderThread.done();                        
    }
    
}
