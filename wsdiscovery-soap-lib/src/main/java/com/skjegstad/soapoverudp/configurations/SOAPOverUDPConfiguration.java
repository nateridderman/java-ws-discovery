/**
ISOAPOverUDPConfiguration.java

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

package com.skjegstad.soapoverudp.configurations;

/**
 * Class used for storing SOAPOverUDP configurations.
 *
 * @author Magnus Skjegstad
 */
public class SOAPOverUDPConfiguration {
     // Default vaules for retry and back-off algorithm (see Appendix I in the SOAP-over-UDP draft)

    /**
     * Number of times to repeat unicast messages.
     */
    protected int UNICAST_UDP_REPEAT = 2;
    /**
     * Number of times to repeat multicast messages.
     */
    protected int MULTICAST_UDP_REPEAT = 4;
    /**
     * Minimum initial delay for resend.
     */
    protected int UDP_MIN_DELAY = 50;
    /**
     * Maximum initial delay for resend.
     */
    protected int UDP_MAX_DELAY = 250;
    /**
     * Maximum delay between resent messages.
     */
    protected int UDP_UPPER_DELAY = 500;

    public int getMulticastUDPRepeat() {
        return MULTICAST_UDP_REPEAT;
    }

    public void setMulticastUDPRepeat(int MULTICAST_UDP_REPEAT) {
        this.MULTICAST_UDP_REPEAT = MULTICAST_UDP_REPEAT;
    }

    public int getUDPMaxDelay() {
        return UDP_MAX_DELAY;
    }

    public void setUDPMaxDelay(int UDP_MAX_DELAY) {
        this.UDP_MAX_DELAY = UDP_MAX_DELAY;
    }

    public int getUDPMinDelay() {
        return UDP_MIN_DELAY;
    }

    public void setUDPMinDelay(int UDP_MIN_DELAY) {
        this.UDP_MIN_DELAY = UDP_MIN_DELAY;
    }

    public int getUDPUpperDelay() {
        return UDP_UPPER_DELAY;
    }

    public void setUDPUpperDelay(int UDP_UPPER_DELAY) {
        this.UDP_UPPER_DELAY = UDP_UPPER_DELAY;
    }

    public int getUnicastUDPRepeat() {
        return UNICAST_UDP_REPEAT;
    }

    public void setUnicastUDPRepeat(int UNICAST_UDP_REPEAT) {
        this.UNICAST_UDP_REPEAT = UNICAST_UDP_REPEAT;
    }



}
