package com.microsoft.routing.demo.azureadrouting;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.bind.annotation.RequestMethod;
import com.microsoft.aad.adal4j.*;
import org.springframework.http.ResponseEntity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.naming.ServiceUnavailableException;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;

@RestController
class GreetingController {

  private String clientId = Configurations.GetValue(Configurations.Keys.ClientId); // app registration id
  private String clientSecret = Configurations.GetValue(Configurations.Keys.ClientSecret); // the key for the app
  private String tenant = Configurations.GetValue(Configurations.Keys.Tenant); // the tenant id for azure AD
  private String authority = Configurations.GetValue(Configurations.Keys.Authority); // source authority
  private String tokenAuthority = Configurations.GetValue(Configurations.Keys.TokenAuthority); // token authority
  private String tenantId = Configurations.GetValue(Configurations.Keys.TenantId); // tenant id attribute in json
  private String webAppUrl = Configurations.GetValue(Configurations.Keys.WebAppUrl); // web app url for redirect
  private String webApiUrl = Configurations.GetValue(Configurations.Keys.WebApiUrl); // web api url for redirect

  @RequestMapping("/webapp")
  RedirectView WebApp()  throws Exception{
    
    AuthenticationResult result = getAccessTokenFromClientCredentials();
    System.out.println("Access Token - " + result.getAccessToken());
    System.out.println("Refresh Token - " + result.getRefreshToken());
    System.out.println("ID Token - " + result.getIdToken());
    String[] res = JWTUtils.decoded(result.getAccessToken()); //header is in res[0], body is in res[1]

    String tid = JSONHelper.getJson(res[1], tenantId);
    String tenantConfig = MongoDBHelper.FindTenantConfig(tid);
    
    String redirectUrl = JSONHelper.getJson(tenantConfig, webAppUrl);
    RedirectView rv = new RedirectView();
    rv.setUrl(redirectUrl);

    return rv;
  }

  @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
  @RequestMapping(value = "/webapi",  method = RequestMethod.POST)
  @ResponseBody
  ResponseEntity<Object> WebApi()  throws Exception{
    //cache token for request?
    AuthenticationResult result = getAccessTokenFromClientCredentials();
    System.out.println("Access Token - " + result.getAccessToken());
    System.out.println("Refresh Token - " + result.getRefreshToken());
    System.out.println("ID Token - " + result.getIdToken());
    String[] res = JWTUtils.decoded(result.getAccessToken()); //header is in res[0], body is in res[1]

    String tid = JSONHelper.getJson(res[1], tenantId);
    String tenantConfig = MongoDBHelper.FindTenantConfig(tid);
    
    String redirectUrl = JSONHelper.getJson(tenantConfig, webApiUrl);
    ResponseEntity<Object> ret = ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).header("Location", redirectUrl).build();

    return ret;
  }

  private AuthenticationResult getAccessTokenFromClientCredentials()throws Exception {
    AuthenticationContext context = null;
    AuthenticationResult result = null;
    ExecutorService service = null;
    try {
      service = Executors.newFixedThreadPool(1);
      context = new AuthenticationContext(authority + tenant + "/", true,
              service);
      Future<AuthenticationResult> future = context.acquireToken(
              tokenAuthority, new ClientCredential(clientId,
                      clientSecret), null);
      result = future.get();
    } catch (Exception e) {
      //throw e;
    } finally {
      service.shutdown();
    }

    if (result == null) {
      throw new ServiceUnavailableException(
              "authentication result was null");
    }
    return result;
    }

} 