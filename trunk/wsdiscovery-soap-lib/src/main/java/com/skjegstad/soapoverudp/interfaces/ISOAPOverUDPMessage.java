/*
ISOAPOverUDPMessage.java

Copyright (C) 2009 Magnus Skjegstad

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

import com.skjegstad.soapoverudp.datatypes.SOAPOverUDPEndpointReferenceType;
import com.skjegstad.soapoverudp.exceptions.SOAPOverUDPException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

/**
 *
 * @author Magnus Skjegstad
 */
public interface ISOAPOverUDPMessage {

    URI getAction();

    URI getMessageId();

    URI getRelatesTo();

    String getRelationshipType();

    SOAPOverUDPEndpointReferenceType getReplyTo();

    SOAPBody getSOAPBody() throws SOAPOverUDPException;

    SOAPHeader getSOAPHeader() throws SOAPOverUDPException;

    SOAPMessage getSOAPMessage() throws SOAPOverUDPException;

    SOAPPart getSOAPPart() throws SOAPOverUDPException;

    URI getTo();

    void saveChanges() throws SOAPOverUDPException;

    boolean saveRequired();

    void setAction(URI action);

    void setMessageId(URI messageId);

    void setRelatesTo(URI relatesTo);

    void setRelationshipType(String relationshipType);

    void setReplyTo(SOAPOverUDPEndpointReferenceType replyTo);

    void setTo(URI to);

    void setSrcAddress(InetAddress src);
    InetAddress getSrcAddress();
    void setSrcPort(int port);
    int getSrcPort();
    void setDstAddress(InetAddress dstAddress);
    InetAddress getDstAddress();
    void setDstPort(int dstPort);
    int getDstPort();
    int getReplyPort();
    InetAddress getReplyAddress();
    String getReplyProto();
    boolean isReplyToAnonymous();
    /**
     * Returns a String containing this SOAP message.
     * @param encoding Encoding
     * @return A String representation of this SOAP message.
     */
    String toString(boolean writeXMLDeclaration, Charset encoding) throws SOAPOverUDPException;

    @Override
    String toString();

}
