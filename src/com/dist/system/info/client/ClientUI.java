package com.dist.system.info.client;

import com.dist.system.info.server.Server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientUI extends JFrame {
    private String server;

    public ClientUI(String server){
        JFrame f = new JFrame("Cliente UI");
        JLabel labelM = new JLabel("Conectado a: " + server);
        labelM.setBounds(100, 25, 200, 30);


        JButton b=new JButton("Realizar benchmark");
        b.setBounds(100,50,200, 40);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("CACA");
            }
        });

        f.add(labelM);
        f.add(b);
        f.setLayout(null);
        f.setSize(390, 300);
        f.setLocation(100, 150);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }



}
