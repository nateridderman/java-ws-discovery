/*
WSDiscoveryGUIView.java

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
package com.ms.wsdiscovery.gui;

import com.ms.wsdiscovery.servicedirectory.exception.WsDiscoveryServiceDirectoryException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.xml.namespace.QName;
import com.ms.wsdiscovery.WsDiscoveryBuilder;
import com.ms.wsdiscovery.WsDiscoveryServer;
import com.ms.wsdiscovery.exception.WsDiscoveryException;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryService;
import com.ms.wsdiscovery.servicedirectory.WsDiscoveryServiceDirectory;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceCollection;
import com.ms.wsdiscovery.servicedirectory.interfaces.IWsDiscoveryServiceDirectory;


/**
 * The application's main frame.
 */
public class WSDiscoveryGUIView extends FrameView {

    protected WsDiscoveryServer wsdiscovery = null;

    
    public int findRowByUUID(DefaultTableModel model, String uuid) {
        for (int j = 0; j < model.getRowCount(); j++) 
            if (model.getValueAt(j, 0).equals(uuid))
                return j;
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
        row[0] = service.getEndpointReferenceAddress();

        // TYPES
        row[1] = "";
        for (QName q : service.getTypes()) {
            row[1] += q.getPrefix() + ":" + q.getLocalPart() + " ";      
        }
        row[2] = "";
        for (URI u : service.getScopesValues()) {
            row[2] += u.toString() + " ";        
        }
        row[3] = "";
        for (String s : service.getXAddrs()) {
            row[3] += s + " ";        
        }
        return row;
    }
    
    public void addServicesToTable(DefaultTableModel model, IWsDiscoveryServiceCollection services) {
        // Add or update all rows
        for (WsDiscoveryService service : services) {
            String[] row = serviceToRow(service);
            int j = findRowByUUID(model, row[0]);
            if (j > -1)
                updateRow(model, j, row);
            else
                model.addRow(row);
        }
        
        // Find rows to delete
        List<Integer> deleteRows = new ArrayList<Integer>();        
        for (int i = 0; i < model.getRowCount(); i++)
            if (!services.contains((String)model.getValueAt(i, 0)))
                deleteRows.add(i);
        
        // Delete rows from bottom to top
        for (int i = deleteRows.size()-1; i >= 0; i--)
            model.removeRow(deleteRows.get(i));
    }

    public WSDiscoveryGUIView(SingleFrameApplication app) {
        super(app);

        initComponents();

        discoveryTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((wsdiscovery != null) && (wsdiscovery.isAlive())) {
                    DefaultTableModel model;
                    IWsDiscoveryServiceCollection services;

                    // Local services
                    try {
                        services = wsdiscovery.getLocalServiceDirectory().matchAll();
                        model = (DefaultTableModel) tableLocalServices.getModel();
                        addServicesToTable(model, services);
                    } catch (WsDiscoveryServiceDirectoryException ex) {
                        JOptionPane.showMessageDialog(null, "alert", ex.getMessage(),  JOptionPane.ERROR_MESSAGE);
                    }
                    
                    // Remote services
                    try {
                        services = wsdiscovery.getRemoteServiceDirectory().matchAll();
                        model = (DefaultTableModel) tableRemoteServices.getModel();
                        addServicesToTable(model, services);
                    } catch (WsDiscoveryServiceDirectoryException ex) {
                        JOptionPane.showMessageDialog(null, "alert", ex.getMessage(),  JOptionPane.ERROR_MESSAGE);
                    }
                    
                    buttonStop.setEnabled(true);
                    buttonStart.setEnabled(false);
                    buttonProbe.setEnabled(true);
                    buttonAddLocalService.setEnabled(true);
                    buttonProxy.setEnabled(true);
                } else {
                    buttonProbe.setEnabled(false);
                    buttonStop.setEnabled(false);
                    buttonStart.setEnabled(true);
                    buttonAddLocalService.setEnabled(false);
                    buttonProxy.setEnabled(false);
                }
            }
        });
        discoveryTimer.setRepeats(true);
        discoveryTimer.start();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = WSDiscoveryGUIApp.getApplication().getMainFrame();
            aboutBox = new WSDiscoveryGUIAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        WSDiscoveryGUIApp.getApplication().show(aboutBox);
    }

    @Action
    public void startWsDiscovery() throws IOException, Exception {
        if ((wsdiscovery != null) && (wsdiscovery.isAlive())) // Check if thread is already running
        {
            JOptionPane.showMessageDialog(new JFrame("WS-Discovery already running"), 
                    "The WS-Discovery threads are already started.\nUse \"Stop threads\" first if you want to restart them.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        wsdiscovery = WsDiscoveryBuilder.createServer();
        wsdiscovery.start();
    }

    @Action
    public void stopWsDiscovery() throws WsDiscoveryException {
        wsdiscovery.done();
        buttonStop.setEnabled(false);
        buttonStart.setEnabled(true);
    }

    @Action
    public void showAddServiceDialog() {
        if ((wsdiscovery == null) || (!wsdiscovery.isAlive())) {            
            JOptionPane.showMessageDialog(new JFrame("WS-Discovery not running"), 
                    "Use \"Start threads\" to start the background threads before adding local services.", 
                    "WS-Discovery not running", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (addServiceBox == null) {
            JFrame mainFrame = WSDiscoveryGUIApp.getApplication().getMainFrame();
            addServiceBox = new WSDiscoveryGUIAddServiceDialog(mainFrame, true, this);
            addServiceBox.setLocationRelativeTo(mainFrame);
        }
        WSDiscoveryGUIApp.getApplication().show(addServiceBox);
    }

    @Action
    public void sendProbe() {
        if ((wsdiscovery != null) && (wsdiscovery.isAlive())) {
            wsdiscovery.probe();
        }
    }
    
    @Action
    public void toggleProxyMode() throws WsDiscoveryException {
        if (wsdiscovery.isProxy()) {            
            wsdiscovery.disableProxyMode();
            buttonProxy.setText("Enable proxy mode");
        } else {
            wsdiscovery.enableProxyMode();
            buttonProxy.setText("Disable proxy mode");
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        buttonStart = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        buttonAddLocalService = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableRemoteServices = new javax.swing.JTable();
        buttonProbe = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableLocalServices = new javax.swing.JTable();
        buttonProxy = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.ms.wsdiscovery.gui.WSDiscoveryGUIApp.class).getContext().getActionMap(WSDiscoveryGUIView.class, this);
        buttonStart.setAction(actionMap.get("startWsDiscovery")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.ms.wsdiscovery.gui.WSDiscoveryGUIApp.class).getContext().getResourceMap(WSDiscoveryGUIView.class);
        buttonStart.setText(resourceMap.getString("buttonStart.text")); // NOI18N
        buttonStart.setName("buttonStart"); // NOI18N

        buttonStop.setAction(actionMap.get("stopWsDiscovery")); // NOI18N
        buttonStop.setText(resourceMap.getString("buttonStop.text")); // NOI18N
        buttonStop.setEnabled(false);
        buttonStop.setName("buttonStop"); // NOI18N

        buttonAddLocalService.setAction(actionMap.get("showAddServiceDialog")); // NOI18N
        buttonAddLocalService.setText(resourceMap.getString("buttonAddLocalService.text")); // NOI18N
        buttonAddLocalService.setEnabled(false);
        buttonAddLocalService.setName("buttonAddLocalService"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        tableRemoteServices.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "UID", "Types", "Scopes", "XAddrs"
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
        tableRemoteServices.setName("tableRemoteServices"); // NOI18N
        jScrollPane2.setViewportView(tableRemoteServices);

        buttonProbe.setAction(actionMap.get("sendProbe")); // NOI18N
        buttonProbe.setText(resourceMap.getString("buttonProbe.text")); // NOI18N
        buttonProbe.setEnabled(false);
        buttonProbe.setName("buttonProbe"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        tableLocalServices.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "UUID", "Types", "Scopes", "XAddrs"
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
        tableLocalServices.setName("tableLocalServices"); // NOI18N
        jScrollPane1.setViewportView(tableLocalServices);

        buttonProxy.setAction(actionMap.get("toggleProxyMode")); // NOI18N
        buttonProxy.setText(resourceMap.getString("buttonProxy.text")); // NOI18N
        buttonProxy.setEnabled(false);
        buttonProxy.setName("buttonProxy"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 782, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 782, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 514, Short.MAX_VALUE)
                        .addComponent(buttonStart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonStop, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(buttonProxy)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonAddLocalService))
                    .addComponent(buttonProbe, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(6, 6, 6))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonStart, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonStop, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonAddLocalService)
                    .addComponent(buttonProxy))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonProbe, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 632, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_aboutMenuItemActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddLocalService;
    private javax.swing.JButton buttonProbe;
    private javax.swing.JButton buttonProxy;
    private javax.swing.JButton buttonStart;
    private javax.swing.JButton buttonStop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTable tableLocalServices;
    private javax.swing.JTable tableRemoteServices;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Timer discoveryTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
    private JDialog addServiceBox;
}
