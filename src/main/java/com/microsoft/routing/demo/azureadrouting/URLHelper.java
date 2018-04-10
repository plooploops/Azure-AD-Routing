package com.microsoft.routing.demo.azureadrouting;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
public class URLHelper{
    public static String GetBaseUrl(ServletRequest request){
        String base_url = request.getScheme()
                        + "://"
                        + request.getServerName()
                        + ("http".equals(request.getScheme())
                                && request.getServerPort() == 80
                                || "https".equals(request.getScheme())
                                && request.getServerPort() == 443 ? "" : ":"
                                + request.getServerPort());
        return base_url;
    }

    public static String GetBaseUrl(HttpServletRequest request){
        String base_uri = request.getRequestURL().toString();

        base_uri = base_uri.substring(0, base_uri.length() - request.getServletPath().length());

        return base_uri;
    }
}