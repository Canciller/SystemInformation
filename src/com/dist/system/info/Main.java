package com.dist.system.info;

import com.dist.system.info.client.Client;
import com.dist.system.info.client.SystemInfo;
import com.dist.system.info.server.Server;
import com.dist.system.info.server.Ranking;
import com.dist.system.info.server.ServerUI;

public class Main {
    public static void main(String[] args) {
        try {
            // Port
            int port = 25565;

            // Addresses
            String myAddress = "25.100.136.188";
            String serverAddress = "25.100.136.188";

            // SystemInfo
            SystemInfo systemInfo = new SystemInfo();

            // Ranking
            Ranking ranking = new Ranking();

            // Server UI
            ServerUI serverUI = new ServerUI();
            serverUI.pack();

            // Server
            Server server = new Server(myAddress, port);

            if(!myAddress.equals(serverAddress)) {
                serverUI.setVisible(false);   
            } else {
                serverUI.setVisible(true);
            }

            // Add server observers.
            server.addObserver(ranking);
            server.addObserver(serverUI);

            // Add ranking observers.
            ranking.addObserver(server);
            ranking.addObserver(serverUI);

            // Start server in new thread.
            Thread serverThread = new Thread(server);
            serverThread.start();

            // Client
            final Client client = new Client(serverAddress, port);

            // Add client observers.
            client.addObserver(systemInfo);
            client.addObserver(serverUI);

            // Add system info observers.
            systemInfo.addObserver(client);

            // Start client in new thread.
            Thread clientThread = new Thread(client);
            clientThread.start();

            // Start system info in new thread.
            Thread systemInfoThread = new Thread(systemInfo);
            systemInfoThread.start();

            // Run ranking in main thread.
            while(true) {
                //ranking.update("ranking:calculate:max:rank", null, null);

                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
