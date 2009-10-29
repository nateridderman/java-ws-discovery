/*
SOAPOverUDPNotInitializedException.java

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
package com.skjegstad.soapoverudp.exceptions;

/**
 * SOAPOverUDPNotInitializedException is thrown when SOAPOverUDP-classes are 
 * used before they have been initialized properly.
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPNotInitializedException extends SOAPOverUDPException {

    /**
     * Creates a new instance of <code>SOAPOverUDPNotInitializedException</code> without detail message.
     */
    public SOAPOverUDPNotInitializedException() {
    }


    /**
     * Constructs an instance of <code>SOAPOverUDPNotInitializedException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SOAPOverUDPNotInitializedException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>SOAPOverUDPNotInitializedException</code> with the specified detail message.
     * @param msg the detail message.
     * @param cause Throwable that caused the exception.
     */
    public SOAPOverUDPNotInitializedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
