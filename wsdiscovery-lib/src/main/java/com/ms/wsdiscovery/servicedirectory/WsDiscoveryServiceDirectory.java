/*
WsDiscoveryServiceDirectory.java

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

package com.ms.wsdiscovery.servicedirectory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.logger.WsdLogger;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.ms.wsdiscovery.xml.jaxb_generated.ByeType;
import com.ms.wsdiscovery.xml.jaxb_generated.EndpointReferenceType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeMatchType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ScopesType;

/**
 * Thread safe directory of {@link WsDiscoveryService} instances. Used by
 * WS-Discovery to keep track of remote and local services.
 * <p>
 * This class is thread safe.
 * 
 * @author Magnus Skjegstad
 */
public class WsDiscoveryServiceDirectory {
    private ArrayList<WsDiscoveryService> services;
    private String name;
    private WsdLogger logger = 
            new WsdLogger(WsDiscoveryServiceDirectory.class.getName());
    private ReadWriteLock rwl = 
            new ReentrantReadWriteLock(); // multiple read, single write
    private Lock r = rwl.readLock();
    private Lock w = rwl.writeLock();
           
    /**
     * Create a new service directory.
     * @param name Name of service directory.
     */
    public WsDiscoveryServiceDirectory(String name) {
        services = new ArrayList<WsDiscoveryService>();
        this.name = name;
    }
    
    /**
     * Create a new service directory with service descriptions from a ProbeMatch-message.
     * @param name Name of service directory.
     * @param probe Contents of a ProbeMatch-message as {@link ProbeMatchesType}.
     * @throws WsDiscoveryServiceDirectoryException if unable to store the probe 
     * matches in the directory.
     */
    public WsDiscoveryServiceDirectory(String name, ProbeMatchesType probe) 
            throws WsDiscoveryServiceDirectoryException {
        this(name);
        
        this.store(probe);        
    }
        
    /**
     * Create a new service directory with service descriptions from a ProbeMatch-message.
     * @param probe Contents of a ProbeMatch-message as {@link ProbeMatchesType}.
     * @throws WsDiscoveryServiceDirectoryException if unable to store the probe 
     * matches in the directory.
     */
    public WsDiscoveryServiceDirectory(ProbeMatchesType probe) 
            throws WsDiscoveryServiceDirectoryException {
        this(null, probe);
    }
    
    /**
     * Create a new, empty service directory.
     */
    public WsDiscoveryServiceDirectory() {
        this("");
    }
        
    /**
     * Number of items in the service directory.
     * @return Number of items in the service directory.
     */
    public int size() {
        r.lock();
        try {
            return services.size();
        } finally {
            r.unlock();
        }
    }
    
    /**
     * Get specified item from the service directory.
     * @param item Item number.
     * @return Service description.
     */
    public WsDiscoveryService get(int item) {
        r.lock();
        try {
            return services.get(item);
        } finally {
            r.unlock();
        }
    }
        
    private void set(int i, WsDiscoveryService service) {
        w.lock();
        try {
            services.set(i, service);
        } finally {
            w.unlock();
        }
    }
    
    private void remove(int i) {
        w.lock();
        try {
            services.remove(i);
        } finally {
            w.unlock();
        }
    }
    
    /**
     * Get the name of this service directory.
     * @return Name of the service directory. 
     */
    public String getName() {
        /* Since we never write to this value after construction, locking is 
         * probably not necessary, but still... */
        r.lock();
        try {
            return name;
        } finally {
            r.unlock();
        }
    }    
    
    /**
     * Locate a service in the directory based on the endpoint address.
     * @param address Endpoint address.
     * @return Service description or <code>null</code> if not found.
     */
    public WsDiscoveryService findService(String address) {
        int i = findServiceIndex(address); // r locks
        if (i > -1)
            return this.get(i); // r locks
        return null;
    }
    
    /**
     * Locate a service in the directory based on the endpoint address.
     * @param address Endpoint address.
     * @return Item number in service directory or -1 on failure.
     */
    public int findServiceIndex(String address) {
        if (address == null)
            return -1;

        for (int i = 0; i < this.size(); i++) { // this.size() r locks
            try {
                // this.get() r locks
                if (this.get(i).getEndpointReferenceAddress().equals(address)) 
                    return i;
            } catch (NullPointerException e) {
                continue;
            }
        }
        return -1;
    }
    
    /**
     * Locate a service in the directory based on the endpoint address.
     * @param endpoint Endpoint.
     * @return Item number in service directory or -1 on failure.
     */
    public int findServiceIndex(EndpointReferenceType endpoint) {
        return findServiceIndex(endpoint.getAddress().getValue());
    }
    
    private synchronized void add(WsDiscoveryService service) 
            throws WsDiscoveryServiceDirectoryException {
        Boolean res = false;
        logger.fine("Added service " + service.toString());
        w.lock();
        try {
            res = services.add(service);
        } finally {
            w.unlock();
        }
        if (!res)
            throw new WsDiscoveryServiceDirectoryException("Unable to add new service to service directory.");
    }
       
    private synchronized void update(WsDiscoveryService service) // Must be synchronized....
            throws WsDiscoveryServiceDirectoryException {
        int i = findServiceIndex(service.getEndpointReferenceAddress()); 
        if (i > -1) {
            logger.fine("Updating service " + service.toString());

            // Increase metadataversion if hashcode differs
            if (service.hashCode() != this.get(i).hashCode()) // this.get() r locks
                service.setMetadataVersion(this.get(i).getMetadataVersion()+1);  // this.get() r locks
            
            // Update service
            this.set(i, service); // this.set() w locks                       
        } else
            throw new WsDiscoveryServiceDirectoryException("Unable to update service. Service not found.");
    }
    
    /**
     * Store a service description in the service directory.
     * @param service Service description.
     * @throws WsDiscoveryServiceDirectoryException 
     * @throws WsDiscoveryServiceDescriptionException on failure.
     */
    public void store(WsDiscoveryService service) 
        throws WsDiscoveryServiceDirectoryException {
        
        // No locking necessary here
        try {
            update(service);
        } catch (WsDiscoveryServiceDirectoryException ex) {
            add(service);
        }
        
    }
    
    /**
     * Add all services from a service directory.
     * @param directory Service directory that contains the services to add.
     * @throws WsDiscoveryServiceDirectoryException if unable to add one of the services.
     */
    public void addAll(WsDiscoveryServiceDirectory directory) 
            throws WsDiscoveryServiceDirectoryException {
        if (directory == null)
            return;
        
        // No locking necessary here
        for (int i = 0; i < directory.size(); i++)        
            store(directory.get(i));                
    }
    
    /**
     * Store the entries in a {@link ProbeMatchesType} in the service directory.
     * @param probe Probe matches.
     * @throws WsDiscoveryServiceDirectoryException if store failes.
     */
    public void store(ProbeMatchesType probe) 
            throws WsDiscoveryServiceDirectoryException {
        // No locking necessary here
        for (ProbeMatchType p : probe.getProbeMatch())
            this.store(new WsDiscoveryService(p));
    }
    
    /**
     * Store a JAXB object in the service directory. The JAXB object must be
     * recognized by {@link WsDiscoveryService#WsDiscoveryService(java.lang.Object)}.
     * 
     * @param jaxbobject JAXB object.
     * @throws WsDiscoveryServiceDirectoryException on failure.
     */
    public void store(Object jaxbobject) 
            throws WsDiscoveryServiceDirectoryException {
        // No locking necessary here
        this.store(new WsDiscoveryService(jaxbobject));
    }
    
    /**
     * Remove service from service directory based on endpoint address.
     * @param address Endpoint address.
     */
    public void remove(String address) {
        int i = findServiceIndex(address);
        if (i > -1) {
            w.lock();
            try {
                this.remove(i); // this.remove() w locks
            } finally {
                w.unlock();
            }
        }        
    }
    
    /**
     * Remove service from service directory based on endpoint address.
     * @param endpoint Endpoint with address.
     */
    public void remove(EndpointReferenceType endpoint) {
        remove(endpoint.getAddress().getValue());
    }
    
    /**
     * Remove service from service directory based on endpoint address.
     * @param service Service with endpoint address.
     */
    public void remove(WsDiscoveryService service) {
        remove(service.getEndpointReferenceAddress());
    }
    
    /**
     * Remove service based on endpoint address received in a Bye-message.
     * @param bye Bye-message with endpoint address to remove.
     */
    public void remove(ByeType bye) {
        remove(bye.getEndpointReference());
    }        
    
    /**
     * Creates a new service directory containing the services in the directory
     * that matches the parameters. Matching algorithm is specified in <code>probeScopes</code>.
     * See also {@link ScopesType} and {@link WsDiscoveryBuilder#getMatcher(wsdiscovery.xml.jaxb_generated.ScopesType)}.
     * @param probeTypes List of probe types to match.
     * @param probeScopes List of scopes to match.
     * @return Service directory with matching services. May contain 0 items, but is never <code>null</code>.
     * @throws WsDiscoveryServiceDirectoryException on failure when creating new service directory.
     */
    public WsDiscoveryServiceDirectory matchBy(List<QName> probeTypes, 
            ScopesType probeScopes) throws WsDiscoveryServiceDirectoryException {
        
        WsDiscoveryServiceDirectory d = new WsDiscoveryServiceDirectory();
               
        // No locking required. get() and size() have read locks, store has write lock
        for (int i = 0; i < this.size(); i++) {
            WsDiscoveryService s = this.get(i);
            if (s.isMatchedBy(probeTypes, probeScopes))
                try {
                    d.store(s);
                } catch (WsDiscoveryServiceDirectoryException ex) {
                    throw new WsDiscoveryServiceDirectoryException("Unable to create service directory for storing matchBy-results.");
                }                        
        }
        return d;        
    }
    
    /**
     * Creates a new service directory containing the services in the directory
     * that matches the parameters. 
     * @param probeTypes List of probe types to match.
     * @param scopes List of scopes to match.
     * @param matchBy Matching algorithm.
     * @return Service directory with matching services. May contain 0 items, but is never <code>null</code>.
     * @throws WsDiscoveryServiceDirectoryException on failure when creating new service directory.
     */
    public WsDiscoveryServiceDirectory matchBy(List<QName> probeTypes, 
            List<URI> scopes, MatchBy matchBy) throws WsDiscoveryServiceDirectoryException {
        
        ScopesType st = null;
        if (scopes != null) {
            st = new ScopesType();
            for (URI u : scopes)
                st.getValue().add(u.toString());
            if (matchBy != null)
                st.setMatchBy(matchBy.toString());
        }        
        return matchBy(probeTypes, st);
    }
    
}
