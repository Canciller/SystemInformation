package com.dist.system.info.server;

import com.dist.system.info.util.Observer;
import com.dist.system.info.util.Payload;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class Ranking extends Observer {
    ConcurrentHashMap<String, Long> ranks;

    String maxAddress;
    long maxRank = 0;

    public Ranking() {
        ranks = new ConcurrentHashMap<>();
    }

    public String getMaxAddress() {
        return maxAddress;
    }

    public long getMaxRank() {
        return maxRank;
    }

    private boolean calculateMaxRank()
    {
        boolean currentMaxValid = false;
        if(ranks.isEmpty()) currentMaxValid = true;

        long newMaxRank = -1;
        String newMaxAddress = "";
        for(String address : ranks.keySet()) {
            if(address.equals(maxAddress)) currentMaxValid = true;

            Long rank = ranks.get(address);
            if(rank > newMaxRank) {
                newMaxRank = rank;
                newMaxAddress = address;
            }
        }

        boolean newMax = false;

        if(newMaxAddress.equals(maxAddress)) {
            newMax = true;
        } else if (!currentMaxValid) {
            newMax = true;
        } else if(newMaxRank > maxRank) {
            newMax = true;
        }

        if(newMax) {
            maxRank = newMaxRank;
            maxAddress = newMaxAddress;
        }

        return newMax;
    }

    private void saveRank(Payload payload) {
        String address = payload.getHeaderAddress();
        long rank = calculateRank(payload.getBody());

        ranks.put(address, rank);

        //System.out.format("[Ranking] New rank for %s %d\n", address, rank);

        notifyObservers("ranking:new:rank", address, rank);
    }

    private long calculateRank(JSONObject body) {
        JSONObject cpu = body.getJSONObject("cpu");
        JSONObject ram = body.getJSONObject("ram");
        JSONObject disk = body.getJSONObject("disk");

        int cpuCores = cpu.getInt("cores");
        double cpuFree = cpu.getDouble("free_percentage");
        int cpuMhz = cpu.getInt("frecuency_mhz");

        int totalRAM = ram.getInt("total_megabytes");
        double freeRAM = ram.getDouble("free_percentage");

        double freeNetwork = body.getDouble("network");

        long rank = 0;

        // CPU
        rank += cpuCores;
        rank += cpuFree * 2;
        rank += cpuMhz * .1;

        // RAM
        rank += totalRAM;
        rank += freeRAM;

        // Network
        rank += freeNetwork;

        return rank;
    }

    private void removeRank(String address) {
        ranks.remove(address);
    }

    @Override
    public void update(String eventType, Object oldValue, Object newValue) {
        //System.out.println("[Ranking] Ranking event: " + eventType);

        switch (eventType) {
            case "server:read": {
                Payload payload = (Payload) newValue;
                String type = payload.getHeaderType();

                switch (type) {
                    case "client:system:info": {
                        saveRank(payload);
                        break;
                    }
                }

                break;
            }
            case "server:client:disconnected": {
                removeRank((String) newValue);
            }
            case "ranking:calculate:max:rank": {
                boolean newMax = calculateMaxRank();
                if(newMax) notifyObservers("ranking:calculate:max:rank:done", maxAddress, maxRank);
                break;
            }
            default: break;
        }
    }
}
