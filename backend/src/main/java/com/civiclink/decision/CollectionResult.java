package com.civiclink.decision;

import java.util.ArrayList;
import java.util.List;

public class CollectionResult {
    public int totalCount;
    public int fetched;
    public int saved;
    public int skipped;
    public int failed;
    public List<Entry> results = new ArrayList<>();

    public static class Entry {
        public long documentNumber;
        public String status;
        public String message;

        public Entry(long id, String status, String message) {
            this.documentNumber = id;
            this.status = status;
            this.message = message;
        }
    }
}
