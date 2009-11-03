/*
WsDiscoveryDispatchThread.java

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
package com.ms.wsdiscovery.common;

import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.WsDiscoveryFactory;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.interfaces.IWsDiscoveryDispatchThread;
import com.ms.wsdiscovery.logger.WsDiscoveryLogger;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import com.skjegstad.soapoverudp.interfaces.ISOAPOverUDP;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Generic dispatch thread class with methods that are common between 
 * all WS-Discovery versions.
 *
 * @author Magnus Skjegstad
 */
public abstract class WsDiscoveryDispatchThread extends Thread implements IWsDiscoveryDispatchThread {

    protected boolean threadDone = false; // Thread aborts when set to true
    protected boolean useProxy = false; // Address to proxy server. Null when disabled.
    protected int useProxyPort = WsDiscoveryConstants.multicastPort; // Default the same as the multicast port
    protected InetAddress useProxyAddress = null; // Address of proxy server
    protected WsDiscoveryService remoteProxyService = null;
    protected boolean isProxy = false; // TRUE when functioning as a proxy server
    protected WsDiscoveryService localProxyService = null; // Must be a service description registered in localServices when isProxy is set
    protected boolean isRunning = false;
    protected IWsDiscoveryServiceDirectory localServices;  // Service directory containing published local services
    protected IWsDiscoveryServiceDirectory serviceDirectory;  // Service directory containing discovered services (including local)
    protected WsDiscoveryLogger logger = new WsDiscoveryLogger(this.getName());
    protected ISOAPOverUDP soapOverUDP;

    public WsDiscoveryDispatchThread(ISOAPOverUDP soapOverUDP) throws WsDiscoveryNetworkException {
        this.soapOverUDP = soapOverUDP;
        try {
            this.soapOverUDP.setTransport(WsDiscoveryConstants.defaultTransportType.newInstance());
        } catch (InstantiationException ex) {
            throw new WsDiscoveryNetworkException("Unable to instantiate transport layer", ex);
        } catch (IllegalAccessException ex) {
            throw new WsDiscoveryNetworkException("Illegal Access while instantiating transport layer", ex);
        }
        this.soapOverUDP.setEncoding(WsDiscoveryConstants.defaultEncoding);
    }

    public void done() throws WsDiscoveryException {
        // The exception is actually thrown from descendant WsDiscoveryServer, so this is a bit ugly...
        threadDone = true;
        while (isRunning) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

    public IWsDiscoveryServiceDirectory getLocalServices() {
        return localServices;
    }

    public InetSocketAddress getProxyServer() {
        if (isUsingProxy()) {
            return new InetSocketAddress(useProxyAddress, useProxyPort);
        } else {
            return null;
        }
    }

    public IWsDiscoveryServiceDirectory getServiceDirectory() {
        return serviceDirectory;
    }

    /**
     * Are we a proxy server?
     * @return True if this instance is acting as a proxy server.
     */
    public boolean isProxy() {
        return isProxy;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns true when a suppression message has been received from a remote
     * proxy server.
     *
     * @return true when using a proxy server, otherwise false.
     */
    public boolean isUsingProxy() {
        return useProxy;
    }

    @Override
    public void start() {
        super.start();
        // Wait for thread to start
        while (!isRunning) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }

    public void useServiceStore(IWsDiscoveryServiceCollection newServiceStore) {
        synchronized (serviceDirectory) {
            serviceDirectory.useStorage(newServiceStore, true);
        }
    }

    /**
     * Create a proxy service description that can be added to the service directory later if
     * proxy mode is enabled on this node.
     */
    protected void createProxyService() {
        InetAddress proxyIp = null;
        Enumeration<InetAddress> ips = null;

        if (WsDiscoveryConstants.proxyAddress != null) {
            proxyIp = WsDiscoveryConstants.proxyAddress;
        } else if (WsDiscoveryConstants.multicastInterface != null) {
            ips = WsDiscoveryConstants.multicastInterface.getInetAddresses();

            while ((ips != null) && (ips.hasMoreElements())) {
                proxyIp = ips.nextElement();
                logger.info("IP detected on multicastinterface: " + proxyIp.getHostAddress());
                if (proxyIp instanceof Inet4Address) // Prefer IPv4
                {
                    break;
                }
            }
        } else {
            try {
                proxyIp = WsDiscoveryUtilities.getFirstNonLoopbackAddress(true, false);
                logger.warning("Proxy address guessed as " + proxyIp.toString() + ". Set proxyAddress to override.");
            } catch (SocketException ex) {
                logger.warning("Unable to enumerate IP address for proxy service.");
            }
        }

        if (proxyIp != null) {
            logger.info("Proxy-service bound to " + proxyIp.getHostAddress() + " on port " + this.soapOverUDP.getTransport().getUnicastPort() + " (not enabled)");

            localProxyService = WsDiscoveryFactory.createService(WsDiscoveryConstants.proxyPortType,
                    WsDiscoveryConstants.proxyScope,
                    "soap.udp://" +
                    proxyIp.getHostAddress() +
                    ":" +
                    this.soapOverUDP.getTransport().getUnicastPort() +
                    "/" +
                    WsDiscoveryConstants.proxyPortType.getLocalPart());
        } else {
            logger.warning("Unable to assign IP-address to proxy-service. This thread may not act as a proxy server.");
            localProxyService = null;
        }
    }

        /**
     * Enables proxy announcements. All multicast Hello-messages will be
     * responded to with a suppression message asking the recipient to
     * use unicast against us instead.
     * @throws WsDiscoveryServiceDirectoryException
     */
    public void enableProxyAnnouncements() throws WsDiscoveryServiceDirectoryException {
        if (isProxy) {
            return; // Already enabled
        }
        isProxy = true;
        if (localProxyService == null) {
            throw new WsDiscoveryServiceDirectoryException("Local proxy service not available.");
        }
        localServices.store(localProxyService);
        serviceDirectory.store(localProxyService);
        // All Hello's should now be answered by multicast suppression messages
    }

    /**
     * Disable proxy announcements.
     */
    public void disableProxyAnnouncements() throws WsDiscoveryXMLException, WsDiscoveryNetworkException {
        if (!isProxy) // Not enabled
        {
            return;
        }

        isProxy = false;
        try {
            sendBye(localProxyService);
        } catch (WsDiscoveryNetworkException ex) {
            throw new WsDiscoveryNetworkException("Unable to send Bye for proxy server", ex);
        }
        localServices.remove(localProxyService);
        serviceDirectory.remove(localProxyService);
    }


     /**
     * Dispatcher. Called from the while-loop in run(). Should receive and parse one or zero messages.   .
     */
    protected abstract void dispatch() throws InterruptedException, WsDiscoveryException;

     /**
     * Main loop. Starts transport layer and continues to loop over dispatch().
     */
    @Override
    public void run() {
        threadDone = false;

        logger.finer("Started " + getName());

        try {
            try {
                // Attempt to start transport layer
                this.soapOverUDP.start(WsDiscoveryConstants.multicastInterface, WsDiscoveryConstants.multicastPort, WsDiscoveryConstants.multicastAddress, logger.getLogger());
            } catch (SOAPOverUDPException ex) {
                logger.severe(ex.getMessage());
                ex.printStackTrace();
                return;
            }
            // Create proxy service
            createProxyService();
            isRunning = true;

            // Notify waiting threads that we have started.
            synchronized (this) {
                notifyAll();
            }

            while (!threadDone) {
                try {
                    dispatch();
                    //resolveUnknown(); // Send resolve-packets for services with no xaddrs
                } catch (Exception ex) {
                    logger.severe(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        } finally {
            this.soapOverUDP.done();
            logger.finer("Stopped " + getName());
            isRunning = false;
            // Notify waiting threads that we stopped
            synchronized (this) {
                notifyAll();
            }
            threadDone = true;
        }
    }    
}
