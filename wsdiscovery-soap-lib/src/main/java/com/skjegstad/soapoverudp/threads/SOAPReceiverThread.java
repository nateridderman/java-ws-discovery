/*
SOAPReceiverThread.java

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

package com.skjegstad.soapoverudp.threads;

import com.skjegstad.soapoverudp.messages.NetworkMessage;
import java.net.SocketException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import com.skjegstad.soapoverudp.interfaces.INetworkMessage;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * SOAP-over-UDP receiver thread.
 * <p>
 * Listens to a socket and stores received messages in a thread safe 
 * message queue. 
 * 
 * @author Magnus Skjegstad
 */
public class SOAPReceiverThread extends Thread {
    private boolean threadDone = false;
    private boolean isRunning = false;
    private final Logger logger;
    
    /**
     * Queue for received messages.
     */
    protected BlockingQueue<INetworkMessage> queue;
    
    /**
     * Socket messages are received on.
     */
    protected DatagramSocket socket;
        
    /**
     * Create new receiver thread on an existing socket.
     * 
     * @param name Name of thread.
     * @param queue Queue for received messages.
     * @param socket Listening socket.
     * @param logger Instance of Logger used for debugging. May be set to null.
     * @throws SocketException 
     */
    public SOAPReceiverThread(String name, BlockingQueue<INetworkMessage> queue, DatagramSocket socket, Logger logger) throws SocketException {
        super(name);
        this.queue = queue;        
        this.socket = socket;
        this.socket.setSoTimeout(1000);
        this.logger = logger;
        setDaemon(true);
    }
    
    /**
     * Create a new receiver thread listening on the specified port.
     * 
     * @param name Name of thread.
     * @param queue Queue for received messages.
     * @param listenport Port to listen to.
     * @param logger Instance of Logger used for debugging. May be set to null.
     * @throws SocketException 
     */
    public SOAPReceiverThread(String name, BlockingQueue<INetworkMessage> queue, int listenport, Logger logger) throws SocketException {
        this (name, queue, new DatagramSocket(listenport), logger);
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
        isRunning = true;

        if (logger != null)
                synchronized (logger) {
                    logger.finer("Started receiver thread " + getName());
                }
        
        try {
            while (!threadDone) {
                byte[] buf = new byte[0xffff];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {                
                    socket.receive(packet);

                    InetSocketAddress sender = (InetSocketAddress) packet.getSocketAddress();
                    
                    INetworkMessage nm = new NetworkMessage(packet.getData(), packet.getLength(),
                                                           sender.getAddress(), sender.getPort(),
                                                           socket.getLocalAddress(), socket.getLocalPort());

                    queue.add(nm);                

                    // Notify listeners of state change
                    synchronized(this) {
                        notifyAll();
                    }                
                } catch (SocketTimeoutException ex) {
                    continue;
                } catch (IOException ex) {
                    if (logger != null) {
                        synchronized (logger) {
                            logger.severe(ex.getMessage());
                        }
                    } else {
                        ex.printStackTrace();
                    }
                    break;
                }
            }
        } finally {   
            socket.close();
            if (logger != null)
                synchronized (logger) {
                    logger.finer("Stopped " + this.getName());
                }
            isRunning = false;
        }                
    }
}
