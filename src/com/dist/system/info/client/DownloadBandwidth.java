package com.dist.system.info.client;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadBandwidth {
    static final String DOWNLOAD_URL = "https://www.pexels.com/video/5652690/download";

    public double calculate() {
        double speed = 0;

        try {
            URL url = new URL(DOWNLOAD_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            InputStream is = connection.getInputStream();

            byte [] buf = new byte[2048];
            int off = -1;
            int total = 0;
            long startTime = System.nanoTime();
            while((off = is.read(buf)) > 0) {
                total += off;
            }
            long endTime = System.nanoTime();

            double MB = total / 1e6;
            double duration = (endTime - startTime) / 1e9; // Seconds

            if(duration > 0) speed = MB / duration;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return speed;
    }
}
