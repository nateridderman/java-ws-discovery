/*
WsDiscoveryGenericAnyType.java

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
package com.skjegstad.soapoverudp.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * Generic class for storing types that include "any" and "otherAttributes" generated
 * by JAXB.
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPGenericAnyType extends SOAPOverUDPGenericOtherAttributesType implements Cloneable {
    protected List<Object> any;

    public SOAPOverUDPGenericAnyType(List<Object> any) {
        this.any = any;
    }

    public SOAPOverUDPGenericAnyType(List<Object> any, Map<QName, String> otherAttributes) {
        this.any = any;
        this.otherAttributes = otherAttributes;
    }

    public SOAPOverUDPGenericAnyType() {
    }

    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return any;
    }

    public void setAny(List<Object> any) {
        this.any = any;
    }

    @Override
    protected Object clone() {
        SOAPOverUDPGenericAnyType n = new SOAPOverUDPGenericAnyType();
        
        if (any != null) {
            // The objects themselves are not cloned
            ArrayList<Object> l = new ArrayList<Object>();
            l.addAll(any);
            n.setAny(l);
        }
        
        SOAPOverUDPGenericOtherAttributesType ot = (SOAPOverUDPGenericOtherAttributesType) super.clone();
        n.setOtherAttributes(ot.getOtherAttributes());
        
        return n;
    }

    @Override
    public String toString() {
        String s = new String();
        if (any != null) {
            for (Object o : this.getAny()) {
                    if (o != null) {
                        if (s.length() > 0)
                            s += ", ";
                        s += o.toString();
                    }
                }
        } else
            s = "(null)";
        return "Any: " + s + " " + super.toString();
    }



}
