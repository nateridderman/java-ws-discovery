/*
WsDiscoveryTransportException.java

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

package ws_discovery.network.transport.exception;

/**
 * Base class for exceptions in the transport package.
 * 
 * @author Magnus Skjegstad
 */
public class WsDiscoveryTransportException extends Exception {

    /**
     * Creates a new instance of <code>WsDiscoveryTransportException</code> without detail message.
     */
    public WsDiscoveryTransportException() {
    }


    /**
     * Constructs an instance of <code>WsDiscoveryTransportException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public WsDiscoveryTransportException(String msg) {
        super(msg);
    }
}
