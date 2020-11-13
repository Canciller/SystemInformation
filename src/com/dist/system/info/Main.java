package com.dist.system.info;

import com.dist.system.info.client.Client;
import com.dist.system.info.client.SystemInfo;
import com.dist.system.info.server.Ranking;
import com.dist.system.info.server.Server;

public class Main {
    public static void main(String[] args) {
        try {
            // Port
            int port = 25565;
            // Server bind host.
            String serverHost = "192.168.0.30";
            String clientHost = "192.168.0.42";

            // SystemInfo
            SystemInfo systemInfo = new SystemInfo();

            // Ranking
            Ranking ranking = new Ranking();

            // UI
            UI ui = new UI();
            ui.pack();
            ui.setVisible(true);

            // Server
            Server server = new Server(serverHost, port);

            // Add server listeners.
            server.addPropertyChangeListener(ui);
            server.addPropertyChangeListener(ranking);

            // Add ranking listeners.
            ranking.addPropertyChangeListener(server);
            ranking.addPropertyChangeListener(ui);

            // Start server in new thread.
            Thread serverThread = new Thread(server);
            serverThread.start();

            // Client
            Client client = new Client(clientHost, port);

            // Add client listeners.
            client.addPropertyChangeListener(systemInfo);

            // Add system info listeners.
            systemInfo.addPropertyChangeListener(client);

            // Start client in new thread.
            Thread clientThread = new Thread(client);
            clientThread.start();

            while(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
