package com.dist.system.info;

import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class UI extends JFrame implements PropertyChangeListener {
    static final String HOSTNAME_COLUMN = "Hostname";
    static final String STATUS_COLUMN = "Conectado";
    static final String RANK_COLUMN = "Rank";

    DefaultTableModel model;
    Object[][] data;

    /**
     * UI Constructor.
     */
    public UI() {
        String[] headers = {
                HOSTNAME_COLUMN,
                "Direccion IP",
                "Modelo CPU",
                "Velocidad CPU",
                "Nucleos CPU",
                "CPU % Libre",
                "Memoria RAM",
                "Memoria RAM % Libre",
                "Almacenamiento Total",
                "Almacenamiento Libre",
                "Almacenamiento % Libre",
                "Sistema Operativo",
                STATUS_COLUMN,
                RANK_COLUMN
        };

        data = new Object[0][];

        model = new DefaultTableModel(data, headers);
        final JTable table = new JTable(model){
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                Object value = getModel().getValueAt(row, column);

                if (value.equals(false)) {
                    comp.setForeground(Color.red);
                    comp.setBackground(Color.red);
                } else if (value.equals(true)) {
                    comp.setForeground(Color.GREEN);
                    comp.setBackground(Color.green);
                }
                else {
                    comp.setForeground(Color.BLACK);
                    comp.setBackground(Color.white);
                }
                return comp;
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                super.valueChanged(e);
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
     * @param object
     */
    void insertOrUpdate(JSONObject object) {
        System.out.println(object.toString(2));

        String hostname = object.getString("hostname");
        Object[] row = parseObject(object);

        boolean updated = update(hostname, row);
        if(!updated) insert(row);
    }

    /**
     * Find row.
     * @param hostname
     * @return
     */
    int findRow(String hostname) {
        int row = -1;

        int c = model.findColumn(HOSTNAME_COLUMN);
        for(int i = 0; i < model.getRowCount(); ++i) {
            if(model.getValueAt(i, c).equals(hostname)) {
                row = i;
                break;
            }
        }

        return row;
    }

    /**
     * Update status.
     * @param object
     */
    void updateStatus(JSONObject object) {
        boolean connected = object.getBoolean("connected");
        String hostname = object.getString("hostname");

        int row = findRow(hostname);

        if(row >= 0) {
            int col = model.findColumn(STATUS_COLUMN);
            model.setValueAt(connected, row, col);
        }
    }

    /**
     * Update rank.
     * @param hostname
     * @param rank
     */
    void updateRank(String hostname, Long rank) {
        int row = findRow(hostname);

        if(row >= 0) {
            int col = model.findColumn(RANK_COLUMN);
            model.setValueAt(rank, row, col);
        }
    }

    /**
     * Update row.
     * @param hostname
     * @param row
     */
    boolean update(String hostname, Object[] row) {
        boolean updated = false;

        int c = model.findColumn(HOSTNAME_COLUMN);
        for(int i = 0; i < model.getRowCount(); ++i) {
            if(model.getValueAt(i, c).equals(hostname)) {
                updated = true;
                for(int j = 0; j < model.getColumnCount(); ++j) {
                    if(j == c) continue;
                    if(j >= row.length) continue;
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
     * Parse JSONObject to row.
     * @param object
     */
    Object[] parseObject(JSONObject object) {
        JSONObject data = object.getJSONObject("data");

        JSONObject cpu = data.getJSONObject("cpu");
        JSONObject ram = data.getJSONObject("ram");
        JSONObject disk = data.getJSONObject("disk");

        Object[] row = {
                object.get("hostname"),
                object.get("ip_address"),
                cpu.get("model"),
                cpu.get("frecuency_mhz"),
                cpu.get("cores"),
                cpu.get("free_percentage"),
                ram.get("total_megabytes"),
                ram.get("free_percentage"),
                disk.get("total_bytes"),
                disk.get("free_bytes"),
                disk.get("free_percentage"),
                data.getString("os"),
                object.getBoolean("connected")
        };

        return row;
    }

    /**
     * PropertyChangeListener propertyChange.
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String event = evt.getPropertyName();

        switch (event) {
            // Handle system info receive.
            case "system:info": {
                insertOrUpdate((JSONObject) evt.getNewValue());
                break;
            }
            // Handle client disconnected.
            case "client:disconnected": {
                updateStatus((JSONObject) evt.getNewValue());
                break;
            }
            case "ranking:new": {
                updateRank((String) evt.getOldValue(), (Long) evt.getNewValue());
                break;
            }
            default: break;
        }
    }
}
