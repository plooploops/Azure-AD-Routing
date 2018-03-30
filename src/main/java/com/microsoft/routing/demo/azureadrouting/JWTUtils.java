package com.microsoft.routing.demo.azureadrouting;
import java.util.Base64;

public class JWTUtils {

    public static String[] decoded(String JWTEncoded) throws Exception {
        String[] res = new String[2];

        try {
            String[] split = JWTEncoded.split("\\.");
            //Log.d("JWT_DECODED", "Header: " + getJson(split[0]));
            //Log.d("JWT_DECODED", "Body: " + getJson(split[1]));
            res[0] = getJson(split[0]);
            res[1] = getJson(split[1]);
        } catch (Exception e) {
            //Error
        }

        return res;
    }

    private static String getJson(String strEncoded) throws Exception{
        byte[] decodedBytes = Base64.getDecoder().decode(strEncoded);
        return new String(decodedBytes, "UTF-8");
    }
}