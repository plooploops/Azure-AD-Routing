package com.microsoft.routing.demo.azureadrouting;


public class Configurations
{
    //these should be populated in the environment.
    public enum Keys
    {
        ClientId,
        ClientSecret,
        Tenant,
        Authority,
        TokenAuthority,
        TenantConnection,
        TenantDb,
        TenantCollection,
        TenantId,
        WebAppUrl,
        WebApiUrl
    }

    public static String GetValue(Keys k)
    {
        String key = k.toString();
        if(System.getProperties().containsKey(key))
        {
            return System.getProperty((key));
        }
        else
        {
            //try with environment settings
            try
            {
                return System.getenv(key);
            }
            catch (Exception e)
            {
            }
            return null;
        }
    }
}