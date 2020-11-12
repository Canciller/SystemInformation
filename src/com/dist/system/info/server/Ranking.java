package com.dist.system.info.server;

import com.dist.system.info.util.Observable;
import org.json.JSONObject;

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
        String key = object.getString("hostname");

        long r = calculateRank(object);
        ranks.put(key, r);

        boolean newMax = false;

        for(String hostname : ranks.keySet()) {
            Long rank = ranks.get(key);
            if(rank > maxRank) {
                newMax = true;

                maxRank = rank;
                maxHostname = hostname;
            }
        }

        if(newMax) {
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
            default: break;
        }
    }
}
