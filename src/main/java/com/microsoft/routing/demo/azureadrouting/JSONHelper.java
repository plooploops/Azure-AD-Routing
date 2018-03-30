package com.microsoft.routing.demo.azureadrouting;

import org.json.JSONObject;

public class JSONHelper {
    public static String getJson(String strEncoded, String key) throws Exception{
        JSONObject jo = new JSONObject(strEncoded);
        
        return jo.getString(key);
    }
}