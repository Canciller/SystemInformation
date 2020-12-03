package com.dist.system.info.client;

import com.dist.system.info.util.Observer;
import org.hyperic.sigar.*;
import org.json.JSONObject;

import java.util.Properties;

public class SystemInfo extends Observer implements Runnable {
    Sigar sigar;
    DownloadBandwidth downloadBandwidth;
    NetworkRxInfo networkRxInfo;

    double networkFreePercentage = 100;

    /**
     * SystemInfo constructor.
     */
    public SystemInfo() {
        sigar = new Sigar();
        downloadBandwidth = new DownloadBandwidth();
        networkRxInfo = new NetworkRxInfo();
    }

    /**
     * Get system information.
     * @return
     */
    public JSONObject get() {
        // TODO: Get real information.
        JSONObject object = new JSONObject();

        try {
            object.put("cpu", getCPUInfo());
            object.put("disk", getDiskInfo());
            object.put("ram", getRAMInfo());
            object.put("os", getOSInfo());
            //object.put("network", getNetworkInfo());
            object.put("network", networkFreePercentage);
        } catch (SigarException e) {
            e.printStackTrace();
        }

        return object;
    }

    /**
     * Get disk info.
     * @throws SigarException
     * @return
     */
    public JSONObject getDiskInfo() throws SigarException {
        FileSystemUsage diskUsage = sigar.getFileSystemUsage("/");

        Long freeSpace = diskUsage.getFree();
        Long totalSpace = diskUsage.getTotal();

        Double freePercentage = freeSpace.doubleValue() * 100.0 / totalSpace.doubleValue();

        JSONObject object = new JSONObject();

        object.put("free_bytes", freeSpace);
        object.put("total_bytes", totalSpace);
        object.put("free_percentage", freePercentage);

        return object;
    }

    /**
     * Get cpu info.
     * @throws SigarException
     * @return
     */
    public JSONObject getCPUInfo() throws SigarException {
        CpuInfo infos[] = sigar.getCpuInfoList();
        CpuInfo info = infos[0];

        String vendor = info.getVendor();
        String model = info.getModel();
        int cores = info.getTotalCores();
        int mhz = info.getMhz();

        CpuPerc cpuPerc = sigar.getCpuPerc();
        Double freeCpu = 100.0 - cpuPerc.getCombined() * 100.0;

        JSONObject object = new JSONObject();

        object.put("vendor", vendor);
        object.put("model", model);
        object.put("cores", cores);
        object.put("frecuency_mhz", mhz);

        object.put("free_percentage", freeCpu);

        return object;
    }

    /**
     * Get RAM info.
     * @throws SigarException
     * @return
     */
    public JSONObject getRAMInfo() throws SigarException {
        Mem mem = sigar.getMem();

        long totalRAM = mem.getRam();
        Double freeRAM = mem.getFreePercent();

        JSONObject object = new JSONObject();

        object.put("total_megabytes", totalRAM);
        object.put("free_percentage", freeRAM);

        return object;
    }

    /**
     * Get OS info.
     */
    public String getOSInfo() {
        Properties properties = System.getProperties();
        return properties.getProperty("os.name");
    }

    /**
     * Get Network info.
     * @return
     */
    public double getNetworkInfo() {
        double maxSpeed = downloadBandwidth.calculate();
        double currSpeed = networkRxInfo.get();

        return 100 - currSpeed * 100 / maxSpeed;
    }

    @Override
    public void update(String eventType, Object oldValue, Object newValue) {
        //System.out.println("[SystemInfo] SystemInfo event: " + eventType);

        switch (eventType) {
            case "system:info:get": {
                JSONObject data = get();

                //System.out.println("[SystemInfo] System info retrieved: " + data.toString());

                notifyObservers("system:info:get:done", null, data);

                break;
            }
        }
    }

    @Override
    public void run() {
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    networkFreePercentage = getNetworkInfo();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        networkThread.start();

        while(true) {
            update("system:info:get", null, null);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
