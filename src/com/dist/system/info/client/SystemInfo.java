package com.dist.system.info.client;

import com.dist.system.info.util.Observable;
import org.hyperic.sigar.*;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;

public class SystemInfo extends Observable implements PropertyChangeListener {
    Sigar sigar;

    /**
     * SystemInfo constructor.
     */
    public SystemInfo() {
        sigar = new Sigar();
    }

    /**
     * Get system information.
     */
    public JSONObject get() {
        // TODO: Get real information.
        JSONObject object = new JSONObject();

        try {
            object.put("cpu", getCPUInfo());
            object.put("disk", getDiskInfo());
            object.put("ram", getRAMInfo());
            object.put("os", getOSInfo());
        } catch (SigarException e) {
            e.printStackTrace();
        }

        return object;
    }

    /**
     * Get disk info.
     * @throws SigarException
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String event = evt.getPropertyName();

        switch (event) {
            case "client:connected": {
                notify(
                        "system:info:ready",
                        evt.getOldValue(),
                        get());
                break;
            }
            default: break;
        }
    }
}
