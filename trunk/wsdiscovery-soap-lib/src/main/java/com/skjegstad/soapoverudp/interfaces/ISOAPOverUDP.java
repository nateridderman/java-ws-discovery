/*
ISOAPOverUDP.java

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
package com.skjegstad.soapoverudp.interfaces;

import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 *
 * @author Magnus Skjegstad
 */
public interface ISOAPOverUDP {
    public void setEncoding(Charset encoding);
    public Charset getEncoding();
    public ISOAPOverUDPMessage createSOAPOverUDPMessage() throws SOAPOverUDPException;
    public ISOAPOverUDPMessage createSOAPOverUDPMessageFromXML(String soapAsXML) throws SOAPOverUDPException;
    public void start(NetworkInterface multicastInterface, int multicastPort, InetAddress multicastAddress, int multicastTtl, Logger logger) throws SOAPOverUDPException;
    public void send(ISOAPOverUDPMessage soapMessage, InetAddress destAddress, int destPort) throws SOAPOverUDPException;
    public void sendBlocking(ISOAPOverUDPMessage soapMessage, InetAddress destAddress, int destPort) throws SOAPOverUDPException, InterruptedException;
    public void sendMulticast(ISOAPOverUDPMessage soapMessage) throws SOAPOverUDPException;
    public void sendMulticastBlocking(ISOAPOverUDPMessage soapMessage) throws SOAPOverUDPException, InterruptedException;
    public ISOAPOverUDPMessage recv(long timeoutInMilliseconds) throws InterruptedException, SOAPOverUDPException;
    public ISOAPOverUDPMessage recv() throws SOAPOverUDPException;
    public boolean isRunning();
    public void done();
    public void setTransport(ISOAPOverUDPTransport transportLayer);
    public ISOAPOverUDPTransport getTransport();
}
