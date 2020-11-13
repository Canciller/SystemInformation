package com.dist.system.info.server;

import com.dist.system.info.util.Observable;
import org.json.JSONObject;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentHashMap;

public class Ranking extends Observable implements PropertyChangeListener {
    ConcurrentHashMap<String, Long> ranks;
    long maxRank = 0;
    String maxHostname;

    public Ranking() {
        ranks = new ConcurrentHashMap<>();
    }

    /**
     * Calculate ranking.
     * @param object
     */
    private void calculateMaxRank(JSONObject object) {
        String hostname = object.getString("hostname");

        long rank = calculateRank(object);
        ranks.put(hostname, rank);

        calculateMaxRank();
    }

    private void calculateMaxRank() {
        boolean currentMaxValid = false;

        long newMaxRank = -1;
        String newMaxHostname = "";
        for(String hostname : ranks.keySet()) {
            if(hostname.equals(maxHostname)) currentMaxValid = true;

            Long rank = ranks.get(hostname);
            if(rank > newMaxRank) {
                newMaxRank = rank;
                newMaxHostname = hostname;
            }
        }

        boolean newMax = false;

        if(newMaxHostname.equals(maxHostname)) {
            newMax = true;
        } else if (!currentMaxValid) {
            newMax = true;
        } else if(newMaxRank > maxRank) {
            newMax = true;
        }

        if(newMax) {
            maxHostname = newMaxHostname;
            maxRank = newMaxRank;

            notify("ranking:new:max", maxHostname, maxRank);
        }
    }

    private long calculateRank(JSONObject object) {
        JSONObject data = object.getJSONObject("data");

        String hostname = object.getString("hostname");
        JSONObject cpu = data.getJSONObject("cpu");
        JSONObject ram = data.getJSONObject("ram");
        JSONObject disk = data.getJSONObject("disk");

        // TODO: Implement ranking logic.

        int cpuCores = cpu.getInt("cores");
        double cpuFree = cpu.getDouble("free_percentage");
        int cpuMhz = cpu.getInt("frecuency_mhz");

        int totalRAM = ram.getInt("total_megabytes");
        double freeRAM = ram.getDouble("free_percentage");

        long rank = 0;

        // CPU
        rank += cpuCores;
        rank += cpuFree;
        rank += cpuMhz;

        // RAM
        rank += totalRAM;
        rank += freeRAM;

        notify("ranking:new", hostname, rank);

        return rank;
    }

    private void removeRank(String hostname) {
        if(ranks.containsKey(hostname)) {
            ranks.remove(hostname);

            System.out.println("[Server] Rank of " + hostname + " removed.");

            calculateMaxRank();
        }
    }

    /**
     * PropertyChangeListener propertyChange.
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String event = evt.getPropertyName();

        switch (event) {
            case "system:info": {
                calculateMaxRank((JSONObject) evt.getNewValue());
                break;
            }
            case "client:disconnected": {
                JSONObject object = (JSONObject) evt.getNewValue();
                removeRank(object.getString("hostname"));
                break;
            }
            default: break;
        }
    }
}
