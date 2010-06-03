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

import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;
import java.net.URI;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryFactory;
import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPEndpointReferenceType;
import com.ms.wsdiscovery.datatypes.WsDiscoveryScopesType;
import com.ms.wsdiscovery.logger.WsDiscoveryLogger;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.ms.wsdiscovery.servicedirectory.store.WsDiscoveryServiceCollection;

/**
 * Thread safe directory of {@link WsDiscoveryService} instances. Used by
 * WS-Discovery to keep track of remote and local services. The services are
 * stored in an instance of IWsDiscoveryServiceCollection and accessed
 * in a thread-safe manner.
 * <p>
 * This class is thread safe.
 * 
 * @author Magnus Skjegstad
 */
public class WsDiscoveryServiceDirectory implements IWsDiscoveryServiceDirectory {
    private IWsDiscoveryServiceCollection services;
    private String name;
    private WsDiscoveryLogger logger =
            new WsDiscoveryLogger(WsDiscoveryServiceDirectory.class.getName());
    private ReadWriteLock rwl = 
            new ReentrantReadWriteLock(); // multiple read, single write. Read not allowed while writing.
    private Lock r = rwl.readLock();
    private Lock w = rwl.writeLock();
    private MatchBy defaultMatcher;
           
    /**
     * Create a new service directory.
     * @param name Name of service directory.
     */
    public WsDiscoveryServiceDirectory(String name, MatchBy defaultMatcher) {
        services = new WsDiscoveryServiceCollection();
        this.name = name;
        this.defaultMatcher = defaultMatcher;
    }
    
    /**
     * Create a new, empty service directory.
     */
    public WsDiscoveryServiceDirectory(MatchBy defaultMatcher) {
        this("", defaultMatcher);
    }
        
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public void clear() {
        w.lock();
        try {
            services.clear();
        } finally {
            w.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public WsDiscoveryService findService(String address) {
        if (address == null)
            return null;

        return this.findService(URI.create(address));
    }

    /**
     * {@inheritDoc}
     */
    public WsDiscoveryService findService(URI address) {
        if (address == null)
            return null;

        r.lock();
        try {
            for (WsDiscoveryService s : services)
                if ((s.getEndpointReference() != null) &&
                    (s.getEndpointReference().getAddress() != null) &&
                    (s.getEndpointReference().getAddress().equals(address)))
                            return s;
        } finally {
            r.unlock();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public WsDiscoveryService findService(SOAPOverUDPEndpointReferenceType endpoint) {
        return findService(endpoint.getAddress());
    }
           
    private void add(WsDiscoveryService service) 
            throws WsDiscoveryServiceDirectoryException {

        logger.finer("serviceDirectory.add()");
        logger.fine("Adding service " + service.getEndpointReference().getAddress().toString());
        w.lock();
        try {
            Boolean res = false;
                           
            res = services.add(service);
            if (!res)
                throw new WsDiscoveryServiceDirectoryException("Unable to add new service to service directory.");
        } finally {
            w.unlock();
        }
     
        logger.finest("Added service: \n" + service.toString());
    }
       
    private void update(WsDiscoveryService service) 
            throws WsDiscoveryServiceDirectoryException {

        WsDiscoveryService foundService;
        
        logger.finer("serviceDirectory.update()");

        if ((service == null) || (service.getEndpointReference() == null)) {
            logger.finer("Parameter or endpoint reference was (null). Call to update() aborted.");
            return;
        }
  

        synchronized (this) { // Synchronize to avoid race cond. between r/w locks. findService read-locks, so we must wait with w lock.
            foundService = findService(service.getEndpointReference());
            
            if (foundService == null) {
                throw new WsDiscoveryServiceDirectoryException("Unable to update service. Service not found.");
            }

            w.lock(); // get write lock
        }
        
        try {
            if (service.getEndpointReference().getAddress() != null) {
                logger.fine("Updating service @ " + service.getEndpointReference().getAddress().toString());
            } else {
                logger.fine("Updating service " + service.getEndpointReference().toString());
            }

            // Increase metadataversion if hashcode differs
            if (service.hashCode() != foundService.hashCode()) {
                service.setMetadataVersion(foundService.getMetadataVersion() + 1);
            }

            // Update service
            if (!services.update(service)) {
                throw new WsDiscoveryServiceDirectoryException("Unable to update service. Update failed.");
            }

        } finally {
            w.unlock();
        }

        logger.finest("Updated service " + service.toString());
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public void remove(URI address) {
        WsDiscoveryService foundService;
        
        // Synchronize to avoid race cond. when acquiring w-lock, as findservice r-locks.
        synchronized(this) {
            foundService = findService(address);
            if (foundService == null)
                return;
            w.lock();
        }

        try {
            services.remove(foundService);
        } finally {
            w.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String address) {
        remove(URI.create(address));
    }
    
    /**
     * {@inheritDoc}
     */
    public void remove(SOAPOverUDPEndpointReferenceType endpoint) {
        remove(endpoint.getAddress());
    }
    
   /**
     * {@inheritDoc}
     */
    public void remove(WsDiscoveryService service) {
        remove(service.getEndpointReference());
    }
      
    
    /**
     * {@inheritDoc}
     */
    public IWsDiscoveryServiceCollection matchBy(List<QName> probeTypes,
            WsDiscoveryScopesType probeScopes) throws WsDiscoveryServiceDirectoryException {
        
        IWsDiscoveryServiceCollection d = new WsDiscoveryServiceCollection();

        // We must obtain a read lock to avoid changes while we loop through the iterator
        r.lock();
        try {
            for (WsDiscoveryService s : services)
                if (s.isMatchedBy(probeTypes, probeScopes, defaultMatcher))
                    if (!d.add(s))
                        throw new WsDiscoveryServiceDirectoryException("Unable to create Service collection for storing matchBy-results.");
        } finally {
            r.unlock();
        }

        return d;        
    }

    /**
     * {@inheritDoc}
     */
    public IWsDiscoveryServiceCollection matchBy(List<QName> probeTypes,
            List<URI> scopes, MatchBy matchBy) throws WsDiscoveryServiceDirectoryException {

        if (matchBy == null)
            throw new WsDiscoveryServiceDirectoryException("MatchBy must not be null");
        
        WsDiscoveryScopesType st = null;
        if (scopes != null) {
            st = new WsDiscoveryScopesType(matchBy);
            for (URI u : scopes)
                st.getValue().add(u.toString());
        }        
        return matchBy(probeTypes, st);
    }

   /**
     * {@inheritDoc}
     */
    public IWsDiscoveryServiceCollection matchAll() throws WsDiscoveryServiceDirectoryException {
        IWsDiscoveryServiceCollection d = new WsDiscoveryServiceCollection();

        // We must obtain a read lock to avoid changes while we loop through the iterator
        r.lock();
        try {
            for (WsDiscoveryService s : services)
                if (!d.add(s))
                    throw new WsDiscoveryServiceDirectoryException("Unable to create Service collection for storing matchBy-results.");
        } finally {
            r.unlock();
        }

        return d;
    }

    /**
     * {@inheritDoc}
     */
    public void addAll(IWsDiscoveryServiceCollection collection)
            throws WsDiscoveryServiceDirectoryException {
        if (collection != null)
            for (WsDiscoveryService s : collection)
                store(s);
    }

    /**
     * {@inheritDoc}
     */
    public void useStorage(IWsDiscoveryServiceCollection newServiceCollection, boolean addExistingServices) {
        w.lock();
        try {
            if (addExistingServices)
                newServiceCollection.addAll(services);
            services = newServiceCollection;
        } finally {
            w.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public MatchBy getDefaultMatcher() {
        return defaultMatcher;
    }

    /**
     * {@inheritDoc} 
     */
    public void setDefaultMatcher(MatchBy defaultMatcher) {
        this.defaultMatcher = defaultMatcher;
    }

    
}
