package com.dist.system.info.server;

import com.dist.system.info.util.Payload;
import org.hyperic.sigar.Sigar;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ServerUI extends JFrame implements PropertyChangeListener {
    static final String HOSTNAME_COLUMN = "Hostname";
    static final String ADDRESS_COLUMN = "Direccion IP";
    static final String STATUS_COLUMN = "Conectado";
    static final String RANK_COLUMN = "Calificacion";

    static final String KEY_COLUMN = ADDRESS_COLUMN;

    DefaultTableModel model;

    /**
     * UI Constructor.
     */
    public ServerUI() {
        String[] headers = {
                HOSTNAME_COLUMN,
                ADDRESS_COLUMN,
                "Sistema Operativo",
                "Modelo CPU",
                "Velocidad CPU",
                "Nucleos CPU",
                "CPU % Libre",
                "Memoria RAM",
                "Memoria RAM % Libre",
                "Almacenamiento Total",
                "Almacenamiento Libre",
                "Almacenamiento % Libre",
                "% Libre de red",
                STATUS_COLUMN,
                RANK_COLUMN,
        };

        Object[][] data = new Object[0][];

        model = new DefaultTableModel(data, headers);
        final JTable table = new JTable(model){
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                Object value = getModel().getValueAt(row, column);

                if (value != null && value.equals(false)) {
                    comp.setForeground(Color.red);
                    comp.setBackground(Color.red);
                } else if (value != null && value.equals(true)) {
                    comp.setForeground(Color.GREEN);
                    comp.setBackground(Color.green);
                }
                else {
                    comp.setForeground(Color.BLACK);
                    comp.setBackground(Color.white);
                }
                return comp;
            }
        };
        table.getTableHeader().setReorderingAllowed(false);
        table.setPreferredScrollableViewportSize(new Dimension(1920, 1080));
        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * Insert or update row.
     * @param payload
     */
    void insertOrUpdate(Payload payload) {
        String address = payload.getHeaderAddress();
        Object[] row = parseObject(payload);

        boolean updated = update(address, row);
        if(!updated) insert(row);
    }

    /**
     * Find row.
     * @param key
     * @return
     */
    int findRow(String key) {
        int row = -1;

        int c = model.findColumn(KEY_COLUMN);
        for(int i = 0; i < model.getRowCount(); ++i) {
            if(model.getValueAt(i, c).equals(key)) {
                row = i;
                break;
            }
        }

        return row;
    }

    /**
     * Update status.
     */
    void setDisconnected(String key) {
        int row = findRow(key);

        if(row >= 0) {
            int col = model.findColumn(STATUS_COLUMN);
            model.setValueAt(false, row, col);
        }
    }

    /**
     * Update rank.
     * @param key
     * @param rank
     */
    void updateRank(String key, Long rank) {
        int row = findRow(key);

        if(row >= 0) {
            int col = model.findColumn(RANK_COLUMN);
            model.setValueAt(rank, row, col);
        }
    }

    /**
     * Update row.
     * @param key
     * @param row
     */
    boolean update(String key, Object[] row) {
        boolean updated = false;

        int c = model.findColumn(KEY_COLUMN);
        for(int i = 0; i < model.getRowCount(); ++i) {
            if(model.getValueAt(i, c).equals(key)) {
                updated = true;
                for(int j = 0; j < model.getColumnCount(); ++j) {
                    if(j == c) continue;
                    if(j >= row.length) break;
                    model.setValueAt(row[j], i, j);
                }
                break;
            }
        }

        return updated;
    }

    /**
     * Insert row.
     * @param row
     */
    void insert(Object[] row) {
        model.addRow(row);
    }

    /**
     * Parse Payload to row.
     * @param payload
     */
    Object[] parseObject(Payload payload) {
        JSONObject headers = payload.getHeaders();
        JSONObject body = payload.getBody();

        JSONObject cpu = body.getJSONObject("cpu");
        JSONObject ram = body.getJSONObject("ram");
        JSONObject disk = body.getJSONObject("disk");

        Long cpuMhz = cpu.getLong("frecuency_mhz");

        StringBuilder cpuGhz = new StringBuilder();
        cpuGhz.append(cpuMhz.doubleValue() / 1000);
        cpuGhz.append("GHz");

        long ramBytes = ram.getLong("total_megabytes");

        StringBuilder ramGB = new StringBuilder();
        ramGB.append(ramBytes / 1000);
        ramGB.append("GB");

        long diskTotalBytes = disk.getLong("total_bytes"),
                diskFreeBytes = disk.getLong("free_bytes");

        StringBuilder diskTotalGB = new StringBuilder(),
                diskFreeGB = new StringBuilder();

        diskTotalGB.append(diskTotalBytes / 1e6);
        diskTotalGB.append("GB");
        diskFreeGB.append(diskFreeBytes / 1e6);
        diskFreeGB.append("GB");

        Object[] row = {
                headers.get("hostname"),
                headers.get("address"),
                body.getString("os"),
                cpu.get("model"),
                cpuGhz.toString(),
                cpu.get("cores"),
                cpu.get("free_percentage"),
                ramGB.toString(),
                ram.get("free_percentage"),
                diskTotalGB.toString(),
                diskFreeGB.toString(),
                disk.get("free_percentage"),
                body.get("network"),
                true
        };

        return row;
    }

    /**
     * PropertyChangeListener propertyChange.
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String eventType = evt.getPropertyName();
        Object newValue = evt.getNewValue();

        switch (eventType) {
            case "server:read": {
                Payload payload = (Payload) newValue;
                String type = payload.getHeaderType();

                switch (type) {
                    case "client:system:info": {
                        insertOrUpdate(payload);
                        break;
                    }
                }

                break;
            }
            case "server:client:disconnected": {
                setDisconnected((String) newValue);
                break;
            }
            case "ranking:new:rank": {
                updateRank((String) evt.getOldValue(), (Long) newValue);
                break;
            }
            case "ui:show": {
                setVisible(true);
                break;
            }
            case "ui:hide": {
                setVisible(false);
                break;
            }
        }
    }
}
