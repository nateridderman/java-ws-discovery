/*
IWsDiscoveryServiceDirectory.java

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

package com.ms.wsdiscovery.servicedirectory.interfaces;

import com.ms.wsdiscovery.servicedirectory.*;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import com.ms.wsdiscovery.xml.jaxb_generated.ByeType;
import com.ms.wsdiscovery.xml.jaxb_generated.EndpointReferenceType;
import com.ms.wsdiscovery.xml.jaxb_generated.ProbeMatchesType;
import com.ms.wsdiscovery.xml.jaxb_generated.ScopesType;
import java.net.URI;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author Magnus Skjegstad
 */
public interface IWsDiscoveryServiceDirectory {

    /**
     * Locate a service in the directory based on the endpoint address.
     * @param address Endpoint address.
     * @return Service description or <code>null</code> if not found.
     */
    WsDiscoveryService findService(String address);    
    
    /**
     * Locate a service in the directory based on the endpoint address.
     * @param endpoint Endpoint reference.
     * @return Service description or <code>null</code> if not found.
     */
    WsDiscoveryService findService(EndpointReferenceType endpoint);
    
    /**
     * Get the name of this service directory.
     * @return Name of the service directory.
     */
    String getName();

    /**
     * Creates a new service collection containing the services in the directory.
     * that matches the parameters. Matching algorithm is specified in <code>probeScopes</code>.
     * See also {@link ScopesType} and {@link WsDiscoveryBuilder#getMatcher(wsdiscovery.xml.jaxb_generated.ScopesType)}.
     * @param probeTypes List of probe types to match.
     * @param probeScopes List of scopes to match.
     * @return Service directory with matching services. May contain 0 items, but is never <code>null</code>.
     * @throws WsDiscoveryServiceDirectoryException on failure when creating new service directory.
     */
    IWsDiscoveryServiceCollection matchBy(List<QName> probeTypes, ScopesType probeScopes) throws WsDiscoveryServiceDirectoryException;

    /**
     * Creates a new service collection containing the services in the directory.
     * that matches the parameters.
     * @param probeTypes List of probe types to match.
     * @param scopes List of scopes to match.
     * @param matchBy Matching algorithm.
     * @return Service directory with matching services. May contain 0 items, but is never <code>null</code>.
     * @throws WsDiscoveryServiceDirectoryException on failure when creating new service directory.
     */
    IWsDiscoveryServiceCollection matchBy(List<QName> probeTypes, List<URI> scopes, MatchBy matchBy) throws WsDiscoveryServiceDirectoryException;

    /**
     * Creates a new service collection containing all the services in the directory.
     * that matches the parameters.
     * @return Service directory with matching services. May contain 0 items, but is never <code>null</code>.
     * @throws WsDiscoveryServiceDirectoryException on failure when creating new service directory.
     */
    IWsDiscoveryServiceCollection matchAll() throws WsDiscoveryServiceDirectoryException;

    /**
     * Remove service from service directory based on endpoint address.
     * @param address Endpoint address.
     */
    void remove(String address);

    /**
     * Remove service from service directory based on endpoint address.
     * @param endpoint Endpoint with address.
     */
    void remove(EndpointReferenceType endpoint);

    /**
     * Remove service from service directory based on endpoint address.
     * @param service Service with endpoint address.
     */
    void remove(WsDiscoveryService service);

    /**
     * Remove service based on endpoint address received in a Bye-message.
     * @param bye Bye-message with endpoint address to remove.
     */
    void remove(ByeType bye);

    /**
     * Number of items in the service directory.
     * @return Number of items in the service directory.
     */
    int size();

    /**
     * Store a service description in the service directory.
     * @param service Service description.
     * @throws WsDiscoveryServiceDirectoryException
     */
    void store(WsDiscoveryService service) throws WsDiscoveryServiceDirectoryException;

    /**
     * Store the entries in a {@link ProbeMatchesType} in the service directory.
     * @param probe Probe matches.
     * @throws WsDiscoveryServiceDirectoryException if store failes.
     */
    void store(ProbeMatchesType probe) throws WsDiscoveryServiceDirectoryException;

    /**
     * Store a JAXB object in the service directory. The JAXB object must be
     * recognized by {@link WsDiscoveryService#WsDiscoveryService(java.lang.Object)}.
     *
     * @param jaxbobject JAXB object.
     * @throws WsDiscoveryServiceDirectoryException on failure.
     */
    void store(Object jaxbobject) throws WsDiscoveryServiceDirectoryException;

    /**
     * Add all entries from another implementation of IWsDiscoveryServiceCollction.
     *
     * @param collection Collection to add.
     * @throws WsDiscoveryServiceDirectoryException if an error occurs while adding the new services to the service directory.
     */
    void addAll(IWsDiscoveryServiceCollection collection) throws WsDiscoveryServiceDirectoryException;

    /**
     * Start registering services in a a new service collection. If <code>addExistingServices</code> is true,
     * existing services are moved from the current service collection to the new one with a call to <code>newServiceCollection.addAll()</code>.
     * The previous service collection is discarded.
     *
     * @param newServiceCollection an instance of an implementation of IWsDiscoveryServiceCollection.
     * @param addExistingServices when true, already known services will be imported into <code>newServiceCollection</code>.
     */
    void useStorage(IWsDiscoveryServiceCollection newServiceCollection, boolean addExistingServices);
}
