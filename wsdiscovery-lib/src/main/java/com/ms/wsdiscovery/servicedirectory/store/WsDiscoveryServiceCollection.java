/*
WsDiscoveryServiceCollection.java

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

package com.ms.wsdiscovery.servicedirectory.store;

import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import java.util.ArrayList;

/**
 * A memory based, transient implementation of IWsDiscoveryServiceCollection. Stores
 * a collection of WsDiscoveryService-instances.
 * 
 * @author Magnus Skjegstad
 */
public class WsDiscoveryServiceCollection extends ArrayList<WsDiscoveryService> implements IWsDiscoveryServiceCollection  {

    /**
     * Return the index of the first service matching the given endpoint reference.
     * @param endpointReference String representation of the endpoint reference.
     * @return position of the first service having the given endpoint reference.
     */
    protected int indexOf(String endpointReference) {
        for (int i = 0; i < this.size(); i++)
            if (this.get(i).getEndpointReference().equals(endpointReference))
                return i;
        return -1;

    }

    public boolean update(WsDiscoveryService service) {
        int i = this.indexOf(service.getEndpointReference());
        
        if (i < 0)
            return false;

        this.set(i, service);

        return true;
    }

    public boolean contains(String endpointReference) {
        return (indexOf(endpointReference) > -1);
    }

    public WsDiscoveryService get(String endpointReference) {
        int i = this.indexOf(endpointReference);
        if (i < 0)
            return null;
        return this.get(i);
    }

}
