/*
WsDiscoveryFinder.java

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

package com.ms.wsdiscovery;

import com.ms.wsdiscovery.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.interfaces.IWsDiscoveryServer;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;

/**
 * A wrapper for the WS-Discovery server providing helper methods for service discovery.
 * <p>
 * Unless an existing WS-Discovery thread is provided as a parameter to 
 * the constructor of this class, a new instance of {@link WsDiscoveryServer} is created 
 * and started. 
 * 
 * @author Magnus Skjegstad
 */
public class WsDiscoveryFinder {
    /**
     * The WS-Discovery thread used to discover services.
     */
    protected IWsDiscoveryServer wsd = null;
    /**
     * When true, the WS-Discovery thread will be stopped before returning a 
     * search result to the caller. If the caller has specified an existing 
     * WS-Discovery thread to be used, this value will be false and it is up to 
     * the client to start and stop the discovery service.
     */
    protected boolean stopServerOnExit = false;

    /**
     * Constructor. Creates and starts a WS-Discovery thread.
     * @throws WsDiscoveryNetworkException
     */
    public WsDiscoveryFinder() throws WsDiscoveryException {
        startServer();
        stopServerOnExit = true;
    }
    /**
     * Constructor. Uses an existing WS-Discovery thread. The thread must 
     * already be running.
     * 
     * @param server Existing (running) WS-Discovery thread.
     */
    public WsDiscoveryFinder(WsDiscoveryServer server) {
        this.wsd = server;
        stopServerOnExit = false;
    }
    
    /**
     * Stops threads before exiting.
     * @throws InterruptedException 
     */
    @Override
    protected void finalize() throws InterruptedException, Throwable {        
        done();
        super.finalize();
    }
   
    /**
     * Creates and starts the WS-Discovery thread.
     * @throws WsDiscoveryNetworkException
     */
    protected void startServer() throws WsDiscoveryException {
        if (wsd == null) {
            wsd = WsDiscoveryFactory.createServer();
            wsd.start();
        }
    }
    
    /**
     * Stops the running WS-Discovery thread.
     * @throws InterruptedException when interrupted while waiting for 
     * server thread to exit.
     * @throws WsDiscoveryException if an error occures during shutdown of the main thread.
     */
    protected void stopServer() throws InterruptedException, WsDiscoveryException {
        if (wsd != null) {
            try {
                wsd.done(); 
                while (wsd.isRunning())
                    Thread.sleep(100); // Wait for server to stop
            } finally {
                wsd = null;            
            }
        }
    }
    
    /**
     * Find one or more services.
     * 
     * @param portTypes List of portTypes to search for. <code>null</code> 
     * searches for all portTypes.
     * @param scopes List of scopes to search in. <code>null</code> searches 
     * within all scopes.
     * @param matchBy Match algorithm that clients should use to match the 
     * scope. <code>null</code> uses the default defined in WsDiscoveryConstants.
     * @param timeoutInMs Time to wait for a match before returning. 
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException on failure.
     */
    public IWsDiscoveryServiceCollection find(List<QName> portTypes, List<URI> scopes,
            MatchBy matchBy, int timeoutInMs) throws InterruptedException, WsDiscoveryException {
        // Search in remote services first
        IWsDiscoveryServiceCollection sd = null;
        try {
            sd = wsd.getServiceDirectory().
                    matchBy(portTypes, scopes, matchBy);
        } catch (WsDiscoveryServiceDirectoryException ex) {
            throw new WsDiscoveryException("An error occured while trying to " +
                    "search the remote service directory.");
        }

        if (sd.size() > 0)
            return sd;
        
        // Send probe
        wsd.probe(portTypes, scopes, matchBy);
        
        long started = System.currentTimeMillis();
        
        // Wait for results
        while (sd.size() == 0) {
            try {
                sd = wsd.getServiceDirectory().matchBy(portTypes, scopes, matchBy);
            } catch (WsDiscoveryServiceDirectoryException ex) {
                throw new WsDiscoveryException("Unable to search remote service directory.");
            }
            
            // Break if we found something and timeoutInMs == 0, or if timeout expired
            if ( ((sd.size() > 0) && (timeoutInMs == 0)) || 
                    (System.currentTimeMillis() - started > timeoutInMs))
                break;
            
            Thread.sleep(100);
        }
        return sd;        
    }
    /**
     * Find one or more services.
     * <p>
     * Uses default scope match algorithm.
     * @param portTypes List of portTypes to search for. <code>null</code> 
     * searches for all portTypes.
     * @param scopes List of scopes to search in. <code>null</code> searches 
     * within all scopes.
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting.
     * @throws WsDiscoveryException 
     */
    public IWsDiscoveryServiceCollection find(List<QName> portTypes,
            List<URI> scopes) throws InterruptedException, WsDiscoveryException {
        return find(portTypes, scopes, null, 0);
    }
    
    /**
     * Find one or more services.
     * <p>
     * Uses default scope match algorithm.
     * @param portType A portType to search for. <code>null</code> searches 
     * for all portType.
     * @param scope The scope to search in. <code>null</code> searches within 
     * all scopes.
     * @param timeoutInMs Time to wait for a match before returning. 
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException 
     */
    public IWsDiscoveryServiceCollection find(QName portType, URI scope,
            int timeoutInMs) throws InterruptedException, WsDiscoveryException {
        List<QName> ports = new ArrayList<QName>();
        ports.add(portType);
        
        if (scope != null) {
            List<URI> scopes = new ArrayList<URI>();
            scopes.add(scope);
            return find(ports, scopes, null, timeoutInMs);
        }
        
        return find(ports, null, null, timeoutInMs);
    }
    /**
     * Find one or more services.
     * <p>
     * Uses default scope match algorithm.
     * @param portType A portType to search for. <code>null</code> searches 
     * for all portTypes.
     * @param scope The scope to search in. <code>null</code> searches within 
     * all scopes.
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException 
     */
    public IWsDiscoveryServiceCollection find(QName portType, URI scope)
            throws InterruptedException, WsDiscoveryException {
        return find(portType, scope, 0);
    }
        
    /**
     * Find one or more services.
     * <p>
     * Uses default scope match algorithm.
     * @param service A service to search for. The portType and scope of this 
     * service is matched against remote services.
     * @param timeoutInMs Time to wait for a match before returning. 
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException 
     */
    public IWsDiscoveryServiceCollection find(WsDiscoveryService service, int timeoutInMs)
            throws InterruptedException, WsDiscoveryException {
        
        return find(service.getPortTypes(), service.getScopes(),
                service.getScopesMatchBy(), timeoutInMs);
    }    
    /**
     * Find one or more services.
     * <p>
     * Uses default scope match algorithm.
     * @param service A service to search for. The portType and scope of this 
     * service is matched against remote services.
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException 
     */
    public IWsDiscoveryServiceCollection find(WsDiscoveryService service)
            throws InterruptedException, WsDiscoveryException {
        return find(service, 0);
    }
    
    /**
     * Find one or more services based on a JAX-WS service description. The 
     * JAX-WS service is converted to {@link WsDiscoveryService} by calling 
     * {@link WsDiscoveryFactory#createService(javax.xml.ws.Service)}
     * <p>
     * Uses default scope match algorithm.
     * @param service A JAX-WS service to search for. 
     * @param timeoutInMs 
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException 
     * @see WsDiscoveryFactory#createService(javax.xml.ws.Service)
     */
    public IWsDiscoveryServiceCollection find(Service service, int timeoutInMs)
            throws InterruptedException, WsDiscoveryException {
        return find(WsDiscoveryFactory.createService(service), timeoutInMs);
    }
    
    /**
     * Find one or more services based on a JAX-WS service description. The 
     * JAX-WS service is converted to {@link WsDiscoveryService} by calling 
     * {@link WsDiscoveryFactory#createService(javax.xml.ws.Service)}
     * <p>
     * Uses default scope match algorithm.
     * @param service A JAX-WS service to search for. 
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws IOException 
     * @throws Exception 
     * @see WsDiscoveryFactory#createService(javax.xml.ws.Service)
     */
    public IWsDiscoveryServiceCollection find(Service service)
            throws InterruptedException, IOException, Exception {
        return find(service, 0);
    }
    
    /**
     * Find services based on portType.
     * 
     * @param portType portType to search for.
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException 
     */
    public IWsDiscoveryServiceCollection find(QName portType)
            throws InterruptedException, WsDiscoveryException {
        return find(portType, null);
    }
    
    /**
     * Find services based on portType.
     * 
     * @param portType portType to search for.
     * @param timeoutInMs Time to wait for a match before returning. 
     * @return Found services.
     * @throws InterruptedException Thrown if interrupted while waiting
     * @throws WsDiscoveryException 
     */
    public IWsDiscoveryServiceCollection find(QName portType, int timeoutInMs)
            throws InterruptedException, WsDiscoveryException {
        return find(portType, null, timeoutInMs);
    }
    
    /**
     * Probe for all services and wait for the result.
     * 
     * @param timeoutInMs Time to wait before returning with result.
     * @return Service collection with all services that was detected.
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    public IWsDiscoveryServiceCollection findAll(int timeoutInMs)
            throws InterruptedException, WsDiscoveryServiceDirectoryException,
            WsDiscoveryXMLException, WsDiscoveryNetworkException {
        wsd.probe();
        Thread.sleep(timeoutInMs);
        return wsd.getServiceDirectory().matchAll();
    }        
    
    /**
     * Stops the background WS-Discovery server (if it was created by this instance). 
     * 
     * @throws java.lang.InterruptedException if interrupted while waiting for the thread to complete.
     * @throws WsDiscoveryException if an error occured while the shutting down the thread.
     */
    public void done() throws InterruptedException, WsDiscoveryException {
        if (stopServerOnExit) 
            stopServer();
    }
}
