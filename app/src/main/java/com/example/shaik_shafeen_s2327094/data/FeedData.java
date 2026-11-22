package com.example.shaik_shafeen_s2327094.data;

// Stores the XML feed along with its source and download time.
public class FeedData {
    public enum Source { LIVE, CACHE, SAMPLE, NONE }

    public final String xml;          // XML data
    public final Source source;       // Where the data came from
    public final long timeMillis;     // when the data was downloaded / saved

    public FeedData(String xml, Source source, long timeMillis) {
        this.xml = xml;
        this.source = source;
        this.timeMillis = timeMillis;
    }
}
