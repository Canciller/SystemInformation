package com.dist.system.info;

import com.dist.system.info.client.Client;
import com.dist.system.info.client.SystemInfo;
import com.dist.system.info.server.Server;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Iniciar como servidor (Y/y): ");
            String startAsServerResponse = scanner.next();

            Boolean startAsServer = startAsServerResponse.equals("Y") || startAsServerResponse.equals("y");

            Server server = null;

            Client client = null;
            SystemInfo systemInfo = new SystemInfo();

            if(startAsServer) {
                server = new Server();
                server.start("25.100.136.188", 25565);

                // Add server listeners.
                server.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        String event = evt.getPropertyName();

                        switch (event) {
                            // Handle system info receive.
                            case "system:info": {
                                JSONObject object = (JSONObject) evt.getNewValue();
                                System.out.println(object.toString(2));

                                break;
                            }
                            // Handle client disconnected.
                            case "client:disconnected": {
                                JSONObject object = (JSONObject) evt.getNewValue();
                                System.out.println(object.toString(2));

                                break;
                            }
                            default: break;
                        }
                    }
                });
            } else {
                client = new Client();

                // Add client listeners.
                client.addPropertyChangeListener(systemInfo);

                // Add system info listeners.
                systemInfo.addPropertyChangeListener(client);

                client.start("25.100.136.188", 25565);
            }

            while(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
