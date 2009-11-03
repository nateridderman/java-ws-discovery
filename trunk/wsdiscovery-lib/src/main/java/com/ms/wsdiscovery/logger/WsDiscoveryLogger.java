/*
WsdLogger.java

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

package com.ms.wsdiscovery.logger;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import java.util.logging.Handler;

/** 
 * Contains helper methods for logging debug messages.
 * @author Magnus Skjegstad
 */
public class WsDiscoveryLogger {
    private Logger logger;
    private Handler handler;
    private static ReentrantLock loggerLock = new ReentrantLock();
  
    /**
     * Constructs a new {@link WsdLogger} instance.
     * @param name Name of instance. Will be prefixed to all log messages.
     * @param handler Descendant of java.util.logging.Handler that can handle the log messages.
     * @param level Detail level. See {@link Level}.
     */    
    public WsDiscoveryLogger(String name, Level level, Handler handler) {
        loggerLock.lock();

        this.handler = handler;        
        this.logger = Logger.getLogger(name);        
        logger.removeHandler(this.handler);
        logger.setUseParentHandlers(false);
        logger.addHandler(this.handler);
        logger.setLevel(level);

        // The handler logs everything passed to it, content is filtered at the Logger level. As
        // each instance of WsdLogger uses the same handler, this could be placed in a static-block.
        // However, if the user has changed WsDiscoveryConstants.loggerHandler it should be called
        // again.... so we call it once for every instance, just in case :-)
        this.handler.setLevel(Level.ALL);
        
        loggerLock.unlock();
    }

    /**
     * Constructs a new {@link WsdLogger} instance. The message handler will be 
     * set to the value of WsDiscoveryConstants.loggerHandler.
     *
     * @param name Name of instance. Will be prefixed to all log messages.
     * @param level Detail level. See {@link Level}.
     */
    public WsDiscoveryLogger(String name, Level level) {
        this(name, level, WsDiscoveryConstants.loggerHandler);
    }
        
    /**
     * Constructs a new {@link Logger} instance with detail level as specified in
     * {@link WsDiscoveryConstants#loggerLevel}.
     * @param name Name of instance. Will be prefixed to all log messages. 
     */
    public WsDiscoveryLogger (String name) {
        this(name, WsDiscoveryConstants.loggerLevel);
    }
    
    /**
     * 
     * @param message
     */
    public synchronized void warning(String message) {
        logger.warning(message);
    }
    
    /**
     * 
     * @param message
     */
    public synchronized void finer(String message) {
        logger.finer(message);        
    }
      
    /**
     * 
     * @param message
     */
    public synchronized void finest(String message) {
        logger.finest(message);
    }
    
    /**
     * 
     * @param message
     */
    public synchronized void fine(String message) {
        logger.fine(message);
    }
    
    /**
     * 
     * @param message
     */
    public synchronized void info(String message) {
        logger.info(message);        
    }
    
    /**
     * 
     * @param message
     */
    public synchronized void severe(String message) {
        logger.severe(message);
    }

    public Logger getLogger() {
        return logger;
    }
}
