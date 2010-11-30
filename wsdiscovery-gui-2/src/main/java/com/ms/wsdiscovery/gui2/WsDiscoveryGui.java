/*
WsDiscoveryGui.java

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

package com.ms.wsdiscovery.gui2;

import com.ms.wsdiscovery.WsDiscoveryFactory;
import com.ms.wsdiscovery.WsDiscoveryConstants;
import com.ms.wsdiscovery.WsDiscoveryServer;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.exception.WsDiscoveryXMLException;
import com.ms.wsdiscovery.gui2.dialogs.WsDiscoveryCustomProbeDialog;
import com.ms.wsdiscovery.gui2.dialogs.WsDiscoveryServiceDialog;
import com.ms.wsdiscovery.exception.WsDiscoveryNetworkException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.matcher.MatchBy;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.xml.namespace.QName;

/**
 *
 * @author Magnus Skjegstad
 */
public class WsDiscoveryGui extends javax.swing.JFrame {

    private int rotationCounter = 0;
    private String rotor = "/-\\|";
    private Level logLevel = Level.FINE;


    class myConsoleHandler extends Handler {
        @Override
        public synchronized void publish(LogRecord arg0) {
            log(arg0);
        }

        @Override
        public synchronized void flush() {
            log_finer("Log flushed.");
        }

        @Override
        public synchronized void close() throws SecurityException {
            log_finer("Log closed.");
        }       
    }

    private WsDiscoveryServer wsdiscovery = null;
    private Timer discoveryTimer;

    /** Creates new form WsDiscoveryGui */
    public WsDiscoveryGui() {
        initComponents();

        WsDiscoveryConstants.loggerHandler = new myConsoleHandler();

        discoveryTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((wsdiscovery != null) && (wsdiscovery.isAlive())) {
                    rotationCounter = (rotationCounter + 1) % rotor.length();

                    labelStatus.setText("Running " + rotor.charAt(rotationCounter));
                    labelStatus.setForeground(Color.green);

                    buttonPublishService.setEnabled(true);
                    buttonProxyControl.setEnabled(true);
                    buttonSendProbe.setEnabled(true);
                    buttonSendCustomProbe.setEnabled(true);

                    DefaultTableModel model;
                    IWsDiscoveryServiceDirectory services;

                    // Local services
                    try {
                        services = wsdiscovery.getLocalServices();
                        model = (DefaultTableModel) tableLocalServices.getModel();
                        addServicesToTable(model, services);
                    } catch (WsDiscoveryServiceDirectoryException ex) {
                        log_fatal(ex.toString());
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                    }

                    // Remote services
                    try {
                        services = wsdiscovery.getServiceDirectory();
                        model = (DefaultTableModel) tableServiceDirectory.getModel();
                        addServicesToTable(model, services);
                    } catch (WsDiscoveryServiceDirectoryException ex) {
                        log_fatal(ex.toString());
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                    }

                    // Update buttons
                    if (wsdiscovery.isProxy()) {
                        labelIsProxy.setText("Yes");
                        labelIsProxy.setForeground(Color.red);
                    } else {
                        labelIsProxy.setText("No");
                        labelIsProxy.setForeground(Color.black);
                    }

                    if (wsdiscovery.isUsingProxy()) {
                        labelProxyIP.setText(wsdiscovery.getProxyServer().toString());
                    } else
                        labelProxyIP.setText("No");
                    
                } else {
                    // Empty the tables
                    DefaultTableModel model;
                    model = (DefaultTableModel) tableServiceDirectory.getModel();
                    while (model.getRowCount() > 0)
                        model.removeRow(0);
                    model = (DefaultTableModel) tableLocalServices.getModel();
                    while (model.getRowCount() > 0)
                        model.removeRow(0);
                    
                    labelStatus.setText("Not running");
                    labelStatus.setForeground(Color.red);
                    labelProxyIP.setText(" ");
                    labelIsProxy.setText(" ");
                    buttonPublishService.setEnabled(false);
                    buttonRemoveService.setEnabled(false);
                    buttonProxyControl.setEnabled(false);
                    buttonSendProbe.setEnabled(false);
                    buttonSendCustomProbe.setEnabled(false);
                }
            }
        });
        discoveryTimer.setRepeats(true);
        discoveryTimer.start();

        tableLocalServices.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                buttonRemoveService.setEnabled(true);
            }
        });
    }

    protected synchronized void log(LogRecord entry) {
        if (entry.getLevel().intValue() < logLevel.intValue())
                return;

        if (entry.getLevel() == Level.FINER) {
            log_finer(entry.getMessage());
        } else
        if (entry.getLevel() == Level.FINEST) {
            log_finest(entry.getMessage());
        } else
        if (entry.getLevel() == Level.WARNING) {
            log_warning(entry.getMessage());
        } else
        if (entry.getLevel() == Level.SEVERE) {
            log_fatal(entry.getMessage());
        } else
            log_fine(entry.getMessage());
    }

    protected synchronized void log(final String message, final Color color) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Document d = textLog.getDocument();
                SimpleAttributeSet s = new SimpleAttributeSet();
                try {
                    StyleConstants.setForeground(s, color);
                    d.insertString(d.getLength(), new Date().toString() + ": ", null);
                    d.insertString(d.getLength(), message + "\n", s);
                    textLog.setCaretPosition(d.getLength());
                } catch (BadLocationException ex) {
                    JOptionPane.showMessageDialog(null, "Internal error in log(). Unable to append text to textLog.");
                }
            }
        });
    }

    protected synchronized void log_info(String message) {
        log(message, Color.BLUE);
    }

    protected synchronized void log_warning(String message) {
        log(message, Color.red);
    }

    protected synchronized void log_fatal(String message) {
        log(message, Color.red);
    }

    protected synchronized void log_fine(String message) {
        log(message, Color.black);
    }

    protected synchronized void log_finer(String message) {
        log(message, Color.darkGray);
    }

    protected synchronized void log_finest(String message) {
        log(message, Color.gray);
    }

    public int findRowByUUID(DefaultTableModel model, String uuid) {
        for (int j = 0; j < model.getRowCount(); j++) {
            if (model.getValueAt(j, 0).equals(uuid)) {
                return j;
            }
        }
        return -1;
    }

    public void updateRow(DefaultTableModel model, int row, String[] rowCells) {
        // Add row to table
       for (int k = 0; (k < rowCells.length) && (k < model.getColumnCount()); k++)
        model.setValueAt(rowCells[k], row, k);
    }

    public String[] serviceToRow(WsDiscoveryService service) {
        String[] row = new String[4];

        // UUID
        row[0] = service.getEndpointReference().getAddress().toString();

        // TYPES
        row[1] = "";
        for (QName q : service.getPortTypes()) {
            row[1] += q.getPrefix() + ":" + q.getLocalPart() + " ";
        }
        row[2] = "";
        for (URI u : service.getScopes()) {
            row[2] += u.toString() + " ";
        }
        row[3] = "";
        for (String s : service.getXAddrs()) {
            row[3] += s + " ";
        }
        return row;
    }

    public void addServicesToTable(DefaultTableModel model, IWsDiscoveryServiceDirectory services) throws WsDiscoveryServiceDirectoryException {
        // Add or update all rows
        for (WsDiscoveryService s : services.matchAll()) {
            String[] row = serviceToRow(s);
            int j = findRowByUUID(model, row[0]);
            if (j > -1)
                updateRow(model, j, row);
            else

                model.addRow(row);
        }

        // Find rows to delete
        List<Integer> deleteRows = new ArrayList<Integer>();
        for (int i = 0; i < model.getRowCount(); i++)
            if (services.findService((String)model.getValueAt(i, 0)) == null)
                deleteRows.add(i);

        // Delete rows from bottom to top
        for (int i = deleteRows.size()-1; i >= 0; i--)
            model.removeRow(deleteRows.get(i));
    }

    protected void startWsDiscovery() {
        buttonWsDiscoveryControl.setEnabled(false);
        log_info("Starting WS-Discovery...");
        try {
            try {
                if (wsdiscovery != null) {
                    log_warning("WS-Discovery already running. Trying to stop...");
                    stopWsDiscovery();
                }
               
                wsdiscovery = new WsDiscoveryServer();
                wsdiscovery.start();

                buttonWsDiscoveryControl.setText("Stop WS-Discovery");
            } catch (WsDiscoveryException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
                log_fatal(ex.getMessage());
                Throwable cause = ex.getCause();
                while (cause != null) {
                    log_fatal(cause.getClass() + ": " + ex.getCause().getMessage());
                    cause = cause.getCause();
                }
                log_fine("Stacktrace:");
                for (StackTraceElement e : ex.getStackTrace())
                    log_fine(e.toString());
            }
        } finally {
            if ((wsdiscovery != null) && (wsdiscovery.isAlive()))
                log_info("WS-Discovery started.");
            buttonWsDiscoveryControl.setEnabled(true);
            panelLocalServices.setEnabled(true);
            panelServiceDirectory.setEnabled(true);
        }
    }

    protected void stopWsDiscovery() {
        buttonWsDiscoveryControl.setEnabled(false);
        log_info("Shutting down...");

        try {
            if ((wsdiscovery != null) && (wsdiscovery.isAlive())) {
                try {
                    wsdiscovery.done();
                } catch (WsDiscoveryException ex) {
                    log_fatal(ex.toString());
                    JOptionPane.showMessageDialog(this, ex.toString());
                }
            } 
        } finally {
            log_info("WS-Discovery stopped.");
            wsdiscovery = null;
            buttonWsDiscoveryControl.setText("Start WS-Discovery");
            buttonWsDiscoveryControl.setEnabled(true);
            panelLocalServices.setEnabled(false);
            panelServiceDirectory.setEnabled(false);            
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelServiceDirectory = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableServiceDirectory = new javax.swing.JTable();
        buttonSendProbe = new javax.swing.JButton();
        buttonSendCustomProbe = new javax.swing.JButton();
        panelControls = new javax.swing.JPanel();
        buttonWsDiscoveryControl = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        labelStatus = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        labelProxyIP = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        labelIsProxy = new javax.swing.JLabel();
        buttonProxyControl = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        textLog = new javax.swing.JTextPane();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        panelLocalServices = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableLocalServices = new javax.swing.JTable();
        buttonPublishService = new javax.swing.JButton();
        buttonRemoveService = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        panelServiceDirectory.setBorder(javax.swing.BorderFactory.createTitledBorder("Service Directory (all known services)"));

        tableServiceDirectory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "UUID", "portType", "scope", "xAddrs"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableServiceDirectory.setRowSelectionAllowed(false);
        tableServiceDirectory.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tableServiceDirectory);

        buttonSendProbe.setText("Send probe");
        buttonSendProbe.setEnabled(false);
        buttonSendProbe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSendProbeActionPerformed(evt);
            }
        });

        buttonSendCustomProbe.setText("Send custom probe");
        buttonSendCustomProbe.setEnabled(false);
        buttonSendCustomProbe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSendCustomProbeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelServiceDirectoryLayout = new javax.swing.GroupLayout(panelServiceDirectory);
        panelServiceDirectory.setLayout(panelServiceDirectoryLayout);
        panelServiceDirectoryLayout.setHorizontalGroup(
            panelServiceDirectoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelServiceDirectoryLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelServiceDirectoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 768, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelServiceDirectoryLayout.createSequentialGroup()
                        .addComponent(buttonSendProbe)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSendCustomProbe)))
                .addContainerGap())
        );
        panelServiceDirectoryLayout.setVerticalGroup(
            panelServiceDirectoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelServiceDirectoryLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelServiceDirectoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSendCustomProbe)
                    .addComponent(buttonSendProbe)))
        );

        panelControls.setBorder(javax.swing.BorderFactory.createTitledBorder("Controls"));

        buttonWsDiscoveryControl.setText("Start WS-Discovery");
        buttonWsDiscoveryControl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonWsDiscoveryControlActionPerformed(evt);
            }
        });

        jLabel1.setText("Status:");

        labelStatus.setForeground(javax.swing.UIManager.getDefaults().getColor("nb.errorForeground"));
        labelStatus.setText("Not running");

        jLabel3.setText("Using proxy: ");

        labelProxyIP.setText("No");

        jLabel5.setText("Is proxy server:");

        labelIsProxy.setText("No");

        buttonProxyControl.setText("Enable proxy server");
        buttonProxyControl.setEnabled(false);
        buttonProxyControl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonProxyControlActionPerformed(evt);
            }
        });

        textLog.setEditable(false);
        jScrollPane3.setViewportView(textLog);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Severe", "Warning", "Fine", "Finer", "Finest" }));
        jComboBox1.setSelectedIndex(2);
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel4.setText("Log level:");

        javax.swing.GroupLayout panelControlsLayout = new javax.swing.GroupLayout(panelControls);
        panelControls.setLayout(panelControlsLayout);
        panelControlsLayout.setHorizontalGroup(
            panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelControlsLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(panelControlsLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelControlsLayout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(labelProxyIP))
                                    .addGroup(panelControlsLayout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(labelIsProxy))
                                    .addComponent(jLabel5)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4)))
                            .addGroup(panelControlsLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(labelStatus))))
                    .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(buttonWsDiscoveryControl, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonProxyControl, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelControlsLayout.setVerticalGroup(
            panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsLayout.createSequentialGroup()
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelControlsLayout.createSequentialGroup()
                        .addComponent(buttonWsDiscoveryControl)
                        .addGap(1, 1, 1)
                        .addComponent(buttonProxyControl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelStatus)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelIsProxy)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelProxyIP)
                        .addGap(7, 7, 7)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        panelLocalServices.setBorder(javax.swing.BorderFactory.createTitledBorder("Local services"));

        tableLocalServices.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "UUID", "portType", "scope", "xAddrs"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableLocalServices.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(tableLocalServices);

        buttonPublishService.setText("Publish service");
        buttonPublishService.setEnabled(false);
        buttonPublishService.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPublishServiceActionPerformed(evt);
            }
        });

        buttonRemoveService.setText("Remove service");
        buttonRemoveService.setEnabled(false);
        buttonRemoveService.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveServiceActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLocalServicesLayout = new javax.swing.GroupLayout(panelLocalServices);
        panelLocalServices.setLayout(panelLocalServicesLayout);
        panelLocalServicesLayout.setHorizontalGroup(
            panelLocalServicesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLocalServicesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelLocalServicesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLocalServicesLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 768, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLocalServicesLayout.createSequentialGroup()
                        .addComponent(buttonPublishService)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemoveService)
                        .addGap(7, 7, 7))))
        );
        panelLocalServicesLayout.setVerticalGroup(
            panelLocalServicesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLocalServicesLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLocalServicesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonPublishService)
                    .addComponent(buttonRemoveService)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelServiceDirectory, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelLocalServices, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelControls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelLocalServices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelServiceDirectory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelControls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        panelServiceDirectory.getAccessibleContext().setAccessibleName("Service directory (all known services)");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonProxyControlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonProxyControlActionPerformed
        buttonProxyControl.setEnabled(false);
        java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (wsdiscovery != null) {
                            if (wsdiscovery.isProxy()) {
                                log_info("Disabling proxy server");
                            try {
                                wsdiscovery.disableProxyMode();
                            } catch (WsDiscoveryException ex) {
                                log_fatal(ex.getMessage());
                            } 
                                buttonProxyControl.setText("Enable proxy server");
                            } else
                                try {
                                    log_info("Enabling proxy server");
                                    wsdiscovery.enableProxyMode();
                                    buttonProxyControl.setText("Disable proxy server");
                                } catch (WsDiscoveryException ex) {
                                    log_fatal(ex.toString());
                                }
                        }
                    } finally {
                        buttonProxyControl.setEnabled((wsdiscovery != null) && wsdiscovery.isAlive());
                    }
                }});
}//GEN-LAST:event_buttonProxyControlActionPerformed

    private void buttonWsDiscoveryControlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonWsDiscoveryControlActionPerformed
        buttonWsDiscoveryControl.setEnabled(false);
        java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (wsdiscovery == null) {
                        startWsDiscovery();
                    } else {
                        stopWsDiscovery();
                    }
                }});
}//GEN-LAST:event_buttonWsDiscoveryControlActionPerformed

    private void buttonSendProbeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSendProbeActionPerformed
        buttonSendProbe.setEnabled(false);
        java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        if ((wsdiscovery != null) && (wsdiscovery.isRunning())) {
                            log_info("Sending probe");
                            try {
                                wsdiscovery.probe();
                            } catch (WsDiscoveryXMLException ex) {
                                log_fatal(ex.getMessage());
                            } catch (WsDiscoveryNetworkException ex) {
                                log_fatal(ex.getMessage());
                            }
                        }
                    } finally {
                        buttonSendProbe.setEnabled(true);
                    }
                }});
    }//GEN-LAST:event_buttonSendProbeActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        String sel = (String) jComboBox1.getModel().getSelectedItem();
        if (sel.equals("Severe"))
            logLevel = Level.SEVERE;
        else
        if (sel.equals("Warning"))
            logLevel = Level.WARNING;
        else
        if (sel.equals("Fine"))
            logLevel = Level.FINE;
        else
        if (sel.equals("Finer"))
            logLevel = Level.FINER;
        else
        if (sel.equals("Finest"))
            logLevel = Level.FINEST;
        else
            JOptionPane.showMessageDialog(this, "Unknown logging level: " + sel);
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void buttonPublishServiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPublishServiceActionPerformed
        if ((wsdiscovery != null) && (wsdiscovery.isAlive())) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    WsDiscoveryServiceDialog dialog = new WsDiscoveryServiceDialog(new javax.swing.JFrame(), true);
                    dialog.setLocationRelativeTo(dialog.getParent());
                    dialog.setVisible(true);
                    if (!dialog.isCancelled()) {
                        WsDiscoveryService service =
                            WsDiscoveryFactory.createService(
                                new QName(dialog.getTextPortTypeSchema().getText(), dialog.getTextPortTypeName().getText()),
                                dialog.getTextScope().getText(),
                                dialog.getTextXAddr().getText());
                        try {
                            log_info("Publishing service " + dialog.getTextXAddr().getText() + " as endpoint " + service.getEndpointReference());
                            wsdiscovery.publish(service);
                        } catch (WsDiscoveryException ex) {
                            log_fatal(ex.toString());
                            JOptionPane.showMessageDialog(null, ex.getMessage());
                        }
                    }
                }
            });
        }
    }//GEN-LAST:event_buttonPublishServiceActionPerformed

    private void buttonRemoveServiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveServiceActionPerformed
        buttonRemoveService.setEnabled(false);
        java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            int i = tableLocalServices.getSelectedRow();
                            if (i > -1) {
                                String uuid = (String) tableLocalServices.getModel().getValueAt(i, 0);
                                log_info("Unpublishing " + uuid);
                                try {
                                    wsdiscovery.unpublish(uuid);
                                } catch (WsDiscoveryException ex) {
                                    log_fatal(ex.getMessage());
                                }
                            }
                        }});
    }//GEN-LAST:event_buttonRemoveServiceActionPerformed

    private void buttonSendCustomProbeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSendCustomProbeActionPerformed
        buttonSendCustomProbe.setEnabled(false);
        java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        if ((wsdiscovery != null) && (wsdiscovery.isAlive())) {
                            WsDiscoveryCustomProbeDialog dialog = new WsDiscoveryCustomProbeDialog(new javax.swing.JFrame(), true, wsdiscovery.getLocalServices().getDefaultMatcher());
                            dialog.setLocationRelativeTo(dialog.getParent());
                            dialog.setVisible(true);
                            if (!dialog.isCancelled()) {
                                QName portType = null;
                                URI scope = null;

                                if (!dialog.getTextPortType().getText().trim().equals(""))
                                    portType = new QName(dialog.getTextPortType().getText());

                                if (!dialog.getTextScope().getText().trim().equals(""))
                                    scope = URI.create(dialog.getTextScope().getText());

                                MatchBy matcher = (MatchBy) dialog.getComboMatchBy().getModel().getSelectedItem();

                                log_info("Sending custom probe (\"" + portType + "\" in scope " + scope + ") using matcher " + matcher.name());

                                try {
                                    wsdiscovery.probe(portType, scope, matcher);
                                } catch (WsDiscoveryException ex) {
                                    log_fatal(ex.getMessage());
                                }
                            }
                        }
                    } finally {
                        buttonSendCustomProbe.setEnabled(true);
                    }
                }});
    }//GEN-LAST:event_buttonSendCustomProbeActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                final WsDiscoveryGui dialog = new WsDiscoveryGui();
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                dialog.stopWsDiscovery();
                                System.exit(0);
                            }
                        });
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonProxyControl;
    private javax.swing.JButton buttonPublishService;
    private javax.swing.JButton buttonRemoveService;
    private javax.swing.JButton buttonSendCustomProbe;
    private javax.swing.JButton buttonSendProbe;
    private javax.swing.JButton buttonWsDiscoveryControl;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel labelIsProxy;
    private javax.swing.JLabel labelProxyIP;
    private javax.swing.JLabel labelStatus;
    private javax.swing.JPanel panelControls;
    private javax.swing.JPanel panelLocalServices;
    private javax.swing.JPanel panelServiceDirectory;
    private javax.swing.JTable tableLocalServices;
    private javax.swing.JTable tableServiceDirectory;
    private javax.swing.JTextPane textLog;
    // End of variables declaration//GEN-END:variables

}
