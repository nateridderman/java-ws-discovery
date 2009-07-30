/*
SOAPSenderThread.java

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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import com.ms.wsdiscovery.logger.WsdLogger;
import com.ms.wsdiscovery.network.NetworkMessage;
import java.util.concurrent.Delayed;

/**
 * SOAP-over-UDP sender thread.
 * 
 * @author Magnus Skjegstad
 */
public class SOAPSenderThread extends Thread {
    /**
     * Instance of Logger used for debug messages.
     */
    protected WsdLogger logger = new WsdLogger(SOAPSenderThread.class.getName());
    
    private boolean threadDone = false;
    private boolean isRunning = false;
    
    /**
     * Queue for outgoing messages. DelayQueue supports delayed entries, which
     * can be used for retransmission with random delays. {@link SOAPNetworkMessage}
     * is an extension to {@link NetworkMessage} that implements {@link Delayed}.
     */
    protected DelayQueue<SOAPNetworkMessage> sendQueue;
    
    /**
     * Socket used for sending messages.
     */
    protected DatagramSocket socket;
            
    /**
     * Create sender thread on an existing socket.
     * 
     * @param name Name of thread.
     * @param queue Queue for outgoing messages.
     * @param socket Socket to send on.
     */
    public SOAPSenderThread(String name, DelayQueue<SOAPNetworkMessage> queue, DatagramSocket socket) {
        super(name);
        this.sendQueue = queue;
        this.socket = socket;
        setDaemon(true);
    }
    
    /**
     * Create sender thread.
     * 
     * @param name Name of thread.
     * @param queue Queue for outgoing messages.
     * @throws SocketException 
     */
    public SOAPSenderThread(String name, DelayQueue<SOAPNetworkMessage> queue) throws SocketException {
        this(name, queue, new DatagramSocket());
    }
    
    /**
     * Get socket used for sending messages.
     * 
     * @return Socket used for sending messages.
     */
    public DatagramSocket getSocket() {
        return socket;
    }
    
    /**
     * Tell thread to stop. Returns immediately. Use {@link Thread#isAlive()]} to
     * determine if the thread has ended.
     */
    public void done() {
        threadDone = true;
    } 
    
    public boolean isRunning() {
        return isRunning;
    }
      
    @Override
    public void run() {
        threadDone = false;
        
        SOAPNetworkMessage nm;
        
        logger.finer("Started " + getName());
        
        isRunning = true;
        
        try {
            while (!threadDone) {
                try {
                    nm = sendQueue.poll(1, TimeUnit.SECONDS);

                    if (nm == null)
                        continue;

                    logger.finest("Send: " + nm.toString());

                    DatagramPacket packet = new DatagramPacket(nm.getPayload(), nm.getPayloadLen(), 
                                                                nm.getDstAddress(), nm.getDstPort());

                    socket.send(packet);                    

                    // Adjust internal values according to SOAP-over-UDP retry/back-off algo.
                    nm.adjustValuesAfterSend();
                    // Should the packet be resent? 
                    if (! nm.isDone()) 
                        sendQueue.put(nm);

                    // Notify listeners of state change
                    synchronized(this) {
                        notifyAll();
                    }

                } catch (IOException ex) {
                    logger.severe(ex.toString());
                    threadDone = true;
                } catch (InterruptedException ex) {
                    logger.severe(ex.toString());
                    threadDone = true;
                }
            }
        } finally {        
            socket.close();
            isRunning = false;
        }
        
        logger.finer("Stopped " + getName());
    }

}
