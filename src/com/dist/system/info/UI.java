package com.dist.system.info;

import com.dist.system.info.util.Payload;
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
    static final String ADDRESS_COLUMN = "Direccion IP";
    static final String STATUS_COLUMN = "Conectado";
    static final String RANK_COLUMN = "Calificacion";

    static final String KEY_COLUMN = ADDRESS_COLUMN;

    DefaultTableModel model;

    /**
     * UI Constructor.
     */
    public UI() {
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

        Object[] row = {
                headers.get("hostname"),
                headers.get("address"),
                body.getString("os"),
                cpu.get("model"),
                cpu.get("frecuency_mhz"),
                cpu.get("cores"),
                cpu.get("free_percentage"),
                ram.get("total_megabytes"),
                ram.get("free_percentage"),
                disk.get("total_bytes"),
                disk.get("free_bytes"),
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
        }

        /*
        switch (eventType) {
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
         */
    }
}
