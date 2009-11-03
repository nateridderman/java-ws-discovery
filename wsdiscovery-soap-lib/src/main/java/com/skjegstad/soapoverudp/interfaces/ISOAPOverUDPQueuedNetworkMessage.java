/**
ISOAPNetworkMessage.java

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
**/
package com.skjegstad.soapoverudp.interfaces;

import java.util.concurrent.Delayed;

/**
 * Interface used for SOAP messages. It extends {@link ISOAPOverUDPNetworkMessage} with support
 * for the {@link Delayed} interface. Implemented by {@link SOAPNetworkMessage}.
 *
 * @author Magnus Skjegstad
 */
public interface ISOAPOverUDPQueuedNetworkMessage extends ISOAPOverUDPNetworkMessage, Delayed {

}
