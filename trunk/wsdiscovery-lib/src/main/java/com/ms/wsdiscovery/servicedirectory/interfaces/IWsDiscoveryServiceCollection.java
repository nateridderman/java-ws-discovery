/*
IWsDiscoveryServiceCollection.java

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

import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import java.util.Collection;

/**
 * Generic interface for classes storing service descriptions for the service
 * directory. Implementations of this interface do not need to be thread-safe
 * as this should be handled by the service directory implementation.
 *
 * @author Magnus Skjegstad
 */
public interface IWsDiscoveryServiceCollection extends Collection<WsDiscoveryService> {
    /**
     * Update the description of an existing service. The services
     * are matched by endpoint reference.
     * 
     * @param service updated service information. The endpoint reference must match an existing service.
     * @return false if the service is not found.
     */
    public boolean update(WsDiscoveryService service);
    /**
     * Search for a given endpoint reference.
     *
     * @param endpointReference is a String-representation of the endpoint reference to search for.
     * @return true if a service with the given endpoint reference is found.
     */
    public boolean contains(String endpointReference);
    /**
     * Retrieve the first service object in the collection that describes the given endpoint reference.
     *
     * @param endpointReference is a String-representation of the endpoint reference to search for.
     * @return WsDiscoveryService-object on success or null on failure.
     */
    public WsDiscoveryService get(String endpointReference);
}
