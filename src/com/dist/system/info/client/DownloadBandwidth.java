package com.dist.system.info.client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadBandwidth {
    static final String SPEEDTEST_EXE = "speedtest.exe";

    public static void main(String[] args) {
        DownloadBandwidth downloadBandwidth = new DownloadBandwidth();
        double maxSpeed = downloadBandwidth.calculate();
        System.out.println("Download bandwidth: " + maxSpeed);
    }

    public double calculate() {
        try {
            String pwd = System.getProperty("user.dir");
            Path path = Paths.get(pwd, SPEEDTEST_EXE);

            Runtime runtime = Runtime.getRuntime();
            String [] command = {
                    path.toAbsolutePath().toString(),
                    "--format=json"
            };

            Process proc = runtime.exec(command);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));

            StringBuilder json = new StringBuilder();

            String part = null;
            while((part = stdInput.readLine()) != null)
                json.append(part);

            JSONObject jsonObject = new JSONObject(json.toString());

            long maxSpeed = jsonObject.getJSONObject("download").getLong("bandwidth");
            return ((double) maxSpeed) / 1e6;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
