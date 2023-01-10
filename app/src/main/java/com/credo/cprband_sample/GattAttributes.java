package com.credo.cprband_sample;

import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String HEART_RATE_MEASUREMENT = "0000fff0-0000-1000-8000-00805f9b34fb";

    static {
        //   # set subscriber depth.
        attributes.put("0000fff1-0000-1000-8000-00805f9b34fb", "position");
        attributes.put("0000fff2-0000-1000-8000-00805f9b34fb", "breath");
        attributes.put("0000fff3-0000-1000-8000-00805f9b34fb", "depth");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
