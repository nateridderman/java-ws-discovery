/*
Calculator.java

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

package com.ms.wsdiscovery.examples.calculatorservice;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * A simple Web Service that can be published through WS-Discovery.
 *
 * A client example is implemented in CalculatorWSClient. Please note that WSPublisherExample
 * must also be running to enable the client to find the service.
 *
 * @author Magnus Skjegstad
 */
@WebService()
public class Calculator {

    /**
     * Web service operation
     */
    @WebMethod(operationName = "add")
    public int add(@WebParam(name = "value1")
    int value1, @WebParam(name = "value2")
    int value2) {
        return value1 + value2;
    }

}
