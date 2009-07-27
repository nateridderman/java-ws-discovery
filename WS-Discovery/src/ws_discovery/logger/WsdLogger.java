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

package ws_discovery.logger;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import ws_discovery.WsDiscoveryConstants;

/** 
 * Contains helper methods for logging debug messages.
 * @author Magnus Skjegstad
 */
public class WsdLogger {
    private static ConsoleHandler consoleHandler = new ConsoleHandler();
    private Logger logger;
    private static ReentrantLock loggerLock = new ReentrantLock();

    static {        
        // The handler logs everything passed to it, content is filtered at the Logger level.
       consoleHandler = new ConsoleHandler(); 
       consoleHandler.setLevel(Level.ALL);
    }
   
    /**
     * Constructs a new {@link WsdLogger} instance.
     * @param name Name of instance. Will be prefixed to all log messages.
     * @param level Detail level. See {@link Level}.
     */    
    public WsdLogger(String name, Level level) {
        loggerLock.lock();
        
        this.logger = Logger.getLogger(name);        
        logger.removeHandler(consoleHandler);
        logger.setUseParentHandlers(false);
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
        
        loggerLock.unlock();
    }
        
    /**
     * Constructs a new {@link Logger} instance with detail level as specified in
     * {@link WsDiscoveryConstants#loggerLevel}.
     * @param name Name of instance. Will be prefixed to all log messages. 
     */
    public WsdLogger (String name) {
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
}
