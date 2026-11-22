package com.example.shaik_shafeen_s2327094.data;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


public class TrafficRepository {
    private static final String TAG = "TrafficRepository";

    // URLs for the traffic and roadworks feeds.
    public static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    public static final String ROADWORKS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private static final String UNPLANNED_CACHE = "cache_unplanned.xml";
    private static final String ROADWORKS_CACHE = "cache_roadworks.xml";
    private static final String UNPLANNED_SAMPLE = "sample_unplanned.xml";
    private static final String ROADWORKS_SAMPLE = "sample_roadworks.xml";

    private final Context context;
    private final String clientId;
    private final String clientKey;

    // Loads the API credentials from the config file.
    public TrafficRepository(Context context) {
        this.context = context.getApplicationContext();
        Properties props = new Properties();
        try (InputStream in = this.context.getAssets().open("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            Log.w(TAG, "No config.properties found");
        }
        clientId = props.getProperty("clientId", "").trim();
        clientKey = props.getProperty("clientKey", "").trim();
    }

    // Checks if the API credentials have been added.
    private boolean hasCredentials() {
        return !clientId.isEmpty() && !clientKey.isEmpty()
                && !clientId.startsWith("YOUR_") && !clientKey.startsWith("YOUR_");
    }

    public FeedData fetchUnplanned() {
        return fetch(UNPLANNED_URL, UNPLANNED_CACHE, UNPLANNED_SAMPLE);
    }

    public FeedData fetchRoadworks() {
        return fetch(ROADWORKS_URL, ROADWORKS_CACHE, ROADWORKS_SAMPLE);
    }

    // Tries to download the feed, using sample data or cached data if needed
    private FeedData fetch(String url, String cacheName, String sampleAsset) {
        boolean urlOk = url.startsWith("http");
        if (!hasCredentials() || !urlOk) {
            try {
                return new FeedData(readAsset(sampleAsset), FeedData.Source.SAMPLE,
                        System.currentTimeMillis());
            } catch (IOException e) {
                Log.e(TAG, "Sample file missing: " + sampleAsset, e);
                return new FeedData(null, FeedData.Source.NONE, 0);
            }
        }
        try {
            String xml = download(url);
            saveCache(cacheName, xml);
            return new FeedData(xml, FeedData.Source.LIVE, System.currentTimeMillis());
        } catch (IOException e) {
            Log.w(TAG, "Download failed: " + e.getMessage());
            return loadCache(cacheName);
        }
    }

    // ---------- network ----------
    // Downloads the XML feed from the API.
    private String download(String address) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        try {
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            String credentials = clientId + ":" + clientKey;
            conn.setRequestProperty("Authorization", Base64.encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code);
            }
            try (Reader in = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                return readAll(in);
            }
        } finally {
            conn.disconnect();
        }
    }

    // ---------- cache ----------
    // Saves downloaded data so it can be used later.
    private void saveCache(String name, String xml) {
        try (FileOutputStream out = context.openFileOutput(name, Context.MODE_PRIVATE)) {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "Could not save cache " + name);
        }
    }

    // Loads previously saved data if the download fails.
    private FeedData loadCache(String name) {
        File f = new File(context.getFilesDir(), name);
        if (!f.exists()) return new FeedData(null, FeedData.Source.NONE, 0);
        try (Reader in = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            return new FeedData(readAll(in), FeedData.Source.CACHE, f.lastModified());
        } catch (IOException e) {
            return new FeedData(null, FeedData.Source.NONE, 0);
        }
    }

    // ---------- helpers ----------
    // Reads the sample XML file from the assets folder.
    private String readAsset(String name) throws IOException {
        try (InputStream in = context.getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    // Reads all the content from a reader into a string.
    private static String readAll(Reader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = in.read(buf)) != -1) sb.append(buf, 0, n);
        return sb.toString();
    }
}
