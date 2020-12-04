package com.dist.system.info.client;

import com.dist.system.info.server.Server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ClientUI extends JFrame implements PropertyChangeListener {
    JLabel labelM;

    public ClientUI(){
        setTitle("Cliente");

        labelM = new JLabel("Conectado a: " + Server.connectedHost);
        labelM.setBounds(100, 25, 200, 30);

        JButton b = new JButton("Realizar benchmark");
        b.setBounds(100,50,200, 40);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("CACA");
            }
        });

        add(labelM);
        add(b);
        setLayout(null);
        setSize(390, 300);
        setLocation(100, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String eventType = evt.getPropertyName();

        switch (eventType) {
            case "server:ui:show": {
                setVisible(false);
                break;
            }
            case "server:ui:hide": {
                labelM.setText(Server.connectedHost);
                setVisible(true);
                break;
            }
        }
    }
}
