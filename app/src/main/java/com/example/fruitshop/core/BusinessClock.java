package com.example.fruitshop.core;

import java.util.TimeZone;

/** No java.time dependency: works on API 24 without adding desugaring to teammates' builds. */
public interface BusinessClock {
    long nowMillis();
    TimeZone timeZone();
    static BusinessClock system() {
        return new BusinessClock() {
            public long nowMillis() { return System.currentTimeMillis(); }
            public TimeZone timeZone() { return TimeZone.getDefault(); }
        };
    }
    static BusinessClock fixed(long millis, String zone) {
        return new BusinessClock() {
            public long nowMillis() { return millis; }
            public TimeZone timeZone() { return TimeZone.getTimeZone(zone); }
        };
    }
}
