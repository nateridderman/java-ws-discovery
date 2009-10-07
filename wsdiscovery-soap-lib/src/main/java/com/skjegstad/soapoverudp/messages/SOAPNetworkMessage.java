/*
SOAPNetworkMessage.java

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
package com.skjegstad.soapoverudp.messages;

import com.skjegstad.soapoverudp.SOAPOverUDP;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import com.skjegstad.soapoverudp.interfaces.INetworkMessage;
import com.skjegstad.soapoverudp.interfaces.ISOAPNetworkMessage;

/**
 * Extended version of {@link NetworkMessage} that supports the retry and 
 * exponential back-off algorithm suggested in the SOAP-over-UDP specification, 
 * Appendix I: "Example retransmission algorithm".
 * 
 * @author Magnus Skjegstad
 */
public class SOAPNetworkMessage extends NetworkMessage implements ISOAPNetworkMessage {
    /**
     * Random delay between {@link SOAPOverUDP#UDP_MIN_DELAY} and 
     * {@link SOAPOverUDP#UDP_MAX_DELAY} chosen at startup. T is doubled
     * every retry until it reaches {@link SOAPOverUDP#UDP_UPPER_DELAY}.
     */
    protected long T;
    
    /**
     * Times to resend the message. Decremented by 1 every resend. Initial
     * value is determined by {@link SOAPOverUDP#MULTICAST_UDP_REPEAT} and
     * {@link SOAPOverUDP#UNICAST_UDP_REPEAT}.
     */
    protected long UDP_REPEAT;
    
    /**
     * When to resend this message. Timestamp in millis from epoch.
     */
    protected long nextSend = 0;
    
    /**
     * Create new <code>SOAPNetworkMessage</code> from existing 
     * <code>NetworkMessage</code>.
     * 
     * @param nm Original network message.
     * @param multicast True if <code>nm</code> is to be sent multicast. Affects
     * the number of resends. 
     */
    public SOAPNetworkMessage(INetworkMessage nm, boolean multicast) {
        super(nm.getPayload(), nm.getPayloadLen(), 
                nm.getSrcAddress(), nm.getSrcPort(), 
                nm.getDstAddress(), nm.getDstPort());
        
        // Set initial UDP_REPEAT value
        if (multicast)
            UDP_REPEAT = SOAPOverUDP.MULTICAST_UDP_REPEAT;
        else
            UDP_REPEAT = SOAPOverUDP.UNICAST_UDP_REPEAT;
        
        // Initialize T to a random value between UDP_MIN_DELAY and UDP_MAX_DELAY
        T = (int)Math.round(Math.random() * 
                (SOAPOverUDP.UDP_MAX_DELAY - SOAPOverUDP.UDP_MIN_DELAY) + 
                SOAPOverUDP.UDP_MIN_DELAY);
                
    }        
       
    /**
     * Doubles the value of T. If T is larger than 
     * {@link SOAPOverUDP#UDP_UPPER_DELAY}, T is set to 
     * {@link SOAPOverUDP#UDP_UPPER_DELAY}.
     */
    public void increaseT() {
        T = T * 2;
        if (T > SOAPOverUDP.UDP_UPPER_DELAY)
            T = SOAPOverUDP.UDP_UPPER_DELAY;
    }

    /**
     * Decrease the value of UDP_REPEAT. 
     */
    public void decreaseUDP_REPEAT() {
        this.UDP_REPEAT--;
    }
    
    /**
     * True when the message has been retransmitted enough times. 
     * @return True when UDP_REPEAT reaches 0.
     */
    public boolean isDone() {
        return (UDP_REPEAT <= 0);
    }
    
    /**
     * Records the retransmission and updates internal counters and delays.
     */
    public void adjustValuesAfterSend() {
        decreaseUDP_REPEAT();
        increaseT();        
        // Set timestamp for next send
        nextSend = System.currentTimeMillis() + T;
    }

    /**
     * Get remaining time until retransmission.
     * 
     * @param unit {@link TimeUnit} used in the result.
     * @return Delay converted to <code>unit</code>.
     */
    public long getDelay(TimeUnit unit) {
        return unit.convert(nextSend - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Compare the delays of two messages.
     * 
     * @param o Delayed message.
     * @return 0 if equal, -1 if <code>o</code> has smaller delay, 1 if <code>o</code> has larger delay.
     */
    public int compareTo(Delayed o) {
        long a = o.getDelay(TimeUnit.MILLISECONDS);
        long b = this.getDelay(TimeUnit.MILLISECONDS);
        
        if (a < b)
            return -1;
        if (a > b)
            return 1;
        
        // they must be equal
        return 0;
    }

}
