package com.dist.system.info.client;

import org.hyperic.sigar.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NetworkRxInfo {
    Map<String, Long> rxCurrentMap = new HashMap<String, Long>();
    Map<String, List<Long>> rxChangeMap = new HashMap<String, List<Long>>();

    Sigar sigar;

    public NetworkRxInfo() {
        this.sigar = new Sigar();

        try {
            getMetric();
        } catch (SigarException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        NetworkRxInfo networkInfo = new NetworkRxInfo();

        while(true) {
            double download = networkInfo.get();
            //System.out.println("download: " + download);
        }
    }

    public double get() {
        try {
            Long download = getMetric();
            System.out.println("download: " + Sigar.formatSize(download));
            return download.doubleValue();
        } catch (SigarException e) {
            e.printStackTrace();
        }

        return 0;
    }

    Long getMetric() throws SigarException {
        for (String ni : sigar.getNetInterfaceList()) {
            NetInterfaceStat netStat = sigar.getNetInterfaceStat(ni);
            NetInterfaceConfig ifConfig = sigar.getNetInterfaceConfig(ni);
            String hwaddr = null;

            if (!NetFlags.NULL_HWADDR.equals(ifConfig.getHwaddr())) {
                hwaddr = ifConfig.getHwaddr();
            }

            if (hwaddr != null) {
                long rxCurrenttmp = netStat.getRxBytes();
                saveChange(rxCurrentMap, rxChangeMap, hwaddr, rxCurrenttmp, ni);
            }
        }

        long totalrxDown = getMetricData(rxChangeMap);

        for (List<Long> l : rxChangeMap.values())
            l.clear();

        return totalrxDown;
    }

    long getMetricData(Map<String, List<Long>> rxChangeMap) {
        long total = 0;
        for (Map.Entry<String, List<Long>> entry : rxChangeMap.entrySet()) {
            int average = 0;
            for (Long l : entry.getValue()) {
                average += l;
            }
            total += average / entry.getValue().size();
        }
        return total;
    }

    void saveChange(Map<String, Long> currentMap,
                    Map<String, List<Long>> changeMap,
                    String hwaddr,
                    long current,
                    String ni)
    {
        Long oldCurrent = currentMap.get(ni);
        if (oldCurrent != null) {
            List<Long> list = changeMap.get(hwaddr);
            if (list == null) {
                list = new LinkedList<Long>();
                changeMap.put(hwaddr, list);
            }
            list.add((current - oldCurrent));
        }
        currentMap.put(ni, current);
    }
}
