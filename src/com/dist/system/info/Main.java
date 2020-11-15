package com.dist.system.info;

import com.dist.system.info.client.Client;
import com.dist.system.info.client.OnlyClient;
import com.dist.system.info.client.SystemInfo;
import com.dist.system.info.server.OnlyServer;
import com.dist.system.info.server.Ranking;
import com.dist.system.info.server.Server;
import com.dist.system.info.util.Observer;
import com.dist.system.info.util.Payload;
import org.json.JSONObject;

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

            /*
            // UI
            UI ui = new UI();
            ui.pack();
            ui.setVisible(true);
            */

            // Server
            //Server server = new Server(myAddress, port);
            OnlyServer server = new OnlyServer(myAddress, port);

            // Add server observers.
            server.addObserver(ranking);
            //server.addObserver(ui);

            // Add ranking observers.
            ranking.addObserver(server);

            /*
            // Add server observers.
            ranking.addListener(ui);
             */

            // Start server in new thread.
            Thread serverThread = new Thread(server);
            serverThread.start();

            // Client
            OnlyClient client = new OnlyClient(serverAddress, port);

            // Add client observers.
            client.addObserver(systemInfo);

            // Add system info observers.
            systemInfo.addObserver(client);

            // Start client in new thread.
            Thread clientThread = new Thread(client);
            clientThread.start();

            // Run system info and ranking in main thread.
            while(true) {
                systemInfo.update("system:info:get", null, null);
                ranking.update("ranking:calculate:max:rank", null, null);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
