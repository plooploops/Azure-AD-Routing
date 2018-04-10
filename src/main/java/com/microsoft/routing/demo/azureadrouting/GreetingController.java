package com.microsoft.routing.demo.azureadrouting;
import java.io.IOException;
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

import com.microsoft.aad.adal4j.ClientCredential;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
  private String resource = Configurations.GetValue(Configurations.Keys.Resource); // https://graph.microsoft.com
  private String redirectUrlTemplate = Configurations.GetValue(Configurations.Keys.RedirectUrlTemplate); // template https://login.microsoftonline.com/common/oauth2/authorize?client_id=%s&state=%s&resource=%s&redirect_uri=%s&response_type=code&response_mode=form_post

  //Step 1 web app
  @RequestMapping("/webAppDirect")
  RedirectView WebAppRedirect(HttpServletRequest request)  throws Exception{
    int state = (int )(Math.random() * 1000000 + 1);
    String base_uri = URLHelper.GetBaseUrl(request);

    String redirect_uri = base_uri + "/context/webapp/authorized";
    String redirectUrl = String.format(redirectUrlTemplate, 
                                      clientId, 
                                      state,
                                      resource, 
                                      redirect_uri);
    RedirectView rv = new RedirectView();
    rv.setUrl(redirectUrl);

    return rv;
  }

  //redirect uri Step 2 web app
  @RequestMapping("/context/webapp/authorized")
  RedirectView ContextWebappAuthorized(ServletRequest request, ServletResponse response
  ) throws IOException, ServletException{
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    try{
      String base_url = URLHelper.GetBaseUrl(request);
      String currentUri = base_url
                          + httpRequest.getRequestURI();
      String fullUrl = currentUri
              + (httpRequest.getQueryString() != null ? "?"
                      + httpRequest.getQueryString() : "");
      Map<String, String> params = new HashMap<String, String>();
      for (String key : request.getParameterMap().keySet()) {
          params.put(key,
                  request.getParameterMap().get(key)[0]);
      }
      AuthenticationResponse authResponse = AuthenticationResponseParser
              .parse(new URI(fullUrl), params);
      AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
      AuthenticationResult result = getAccessToken(
              oidcResponse.getAuthorizationCode(),
              currentUri); //this obtains the token.  We can use the rest of the flow.
      HttpSession session = httpRequest.getSession();
      session.setAttribute("token", result);
      String redirectUrl = base_url
                          + "/webapp"; //redirect once more
      RedirectView rv = new RedirectView();
      rv.setUrl(redirectUrl);
  
      return rv;
    }
    catch (Throwable exc) {
      httpResponse.setStatus(500);
      request.setAttribute("error", exc.getMessage());
      httpResponse.sendRedirect(((HttpServletRequest) request)
              .getContextPath() + "/error.jsp");

      return null;
    }                
  }

  //Step 3 web app
  @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
  @RequestMapping("/webapp")
  @ResponseBody
  private ResponseEntity<Object> WebApp(ServletRequest request)  throws Exception{
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpSession session = httpRequest.getSession();
    AuthenticationResult result = (AuthenticationResult)session.getAttribute("token");

    //obtain the access token from session instead of service to service flow
    // AuthenticationResult result = getAccessTokenFromCodeFlow();
    System.out.println("Access Token - " + result.getAccessToken());
    System.out.println("Refresh Token - " + result.getRefreshToken());
    System.out.println("ID Token - " + result.getIdToken());
    String[] res = JWTUtils.decoded(result.getAccessToken()); //header is in res[0], body is in res[1]

    String tid = JSONHelper.getJson(res[1], tenantId);
    String tenantConfig = MongoDBHelper.FindTenantConfig(tid);
    
    String redirectUrl = JSONHelper.getJson(tenantConfig, webAppUrl);
    ResponseEntity<Object> ret = ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).header("Location", redirectUrl).build();
    return ret;
  }

  //step 1 web api
  @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
  @RequestMapping(value = "/webApiDirect",  method = RequestMethod.POST)
  @ResponseBody
  ResponseEntity<Object> WebApiRedirect(HttpServletRequest request)  throws Exception{
    int state = (int )(Math.random() * 1000000 + 1);
    String base_uri = URLHelper.GetBaseUrl(request);

    String redirect_uri = base_uri + "/context/webapi/authorized";
    String redirectUrl = String.format(redirectUrlTemplate, 
                                      clientId, 
                                      state,
                                      resource, 
                                      redirect_uri);
    ResponseEntity<Object> ret = ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).header("Location", redirectUrl).build();

    return ret;
  }

  //redirect uri step 2
  @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
  @RequestMapping(value = "/context/webapi/authorized",  method = RequestMethod.POST)
  @ResponseBody
  ResponseEntity<Object>  ContextWebApiAuthorized(ServletRequest request, ServletResponse response
  ) throws IOException, ServletException{
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    try{
      String base_url = URLHelper.GetBaseUrl(request);
      String currentUri = base_url
                          + httpRequest.getRequestURI();
      String fullUrl = currentUri
              + (httpRequest.getQueryString() != null ? "?"
                      + httpRequest.getQueryString() : "");
      Map<String, String> params = new HashMap<String, String>();
      for (String key : request.getParameterMap().keySet()) {
          params.put(key,
                  request.getParameterMap().get(key)[0]);
      }
      AuthenticationResponse authResponse = AuthenticationResponseParser
              .parse(new URI(fullUrl), params);
      AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
      AuthenticationResult result = getAccessToken(
              oidcResponse.getAuthorizationCode(),
              currentUri); //this obtains the token.  We can use the rest of the flow.
      HttpSession session = httpRequest.getSession();
      session.setAttribute("token", result);
      String redirectUrl = base_url
                          + "/webapi"; //redirect once more
      
      ResponseEntity<Object> ret = ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).header("Location", redirectUrl).build();

      return ret;
    }
    catch (Throwable exc) {
      httpResponse.setStatus(500);
      request.setAttribute("error", exc.getMessage());
      httpResponse.sendRedirect(((HttpServletRequest) request)
              .getContextPath() + "/error.jsp");

      return null;
    }                
  }

  //step 3 web api
  @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
  @RequestMapping(value = "/webapi",  method = RequestMethod.POST)
  @ResponseBody
  private ResponseEntity<Object> WebApi(ServletRequest request)  throws Exception{
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpSession session = httpRequest.getSession();
    AuthenticationResult result = (AuthenticationResult)session.getAttribute("token");           
    //cache token for request?
    // AuthenticationResult result = getAccessTokenFromCodeFlow();
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

  @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
  @RequestMapping(value = "/webapiclientcredential",  method = RequestMethod.POST)
  @ResponseBody
  private ResponseEntity<Object> WebApiClientCredentials(ServletRequest request)  throws Exception{
    AuthenticationResult result = getAccessTokenFromCodeFlow();
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

  @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
  @RequestMapping("/webappclientcredential")
  @ResponseBody
  private ResponseEntity<Object> WebAppClientCredential(ServletRequest request)  throws Exception{
    //obtain the access token from session instead of service to service flow
    AuthenticationResult result = getAccessTokenFromCodeFlow();
    System.out.println("Access Token - " + result.getAccessToken());
    System.out.println("Refresh Token - " + result.getRefreshToken());
    System.out.println("ID Token - " + result.getIdToken());
    String[] res = JWTUtils.decoded(result.getAccessToken()); //header is in res[0], body is in res[1]

    String tid = JSONHelper.getJson(res[1], tenantId);
    String tenantConfig = MongoDBHelper.FindTenantConfig(tid);
    
    String redirectUrl = JSONHelper.getJson(tenantConfig, webAppUrl);
    ResponseEntity<Object> ret = ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).header("Location", redirectUrl).build();
    return ret;
  }

  private AuthenticationResult getAccessToken(
            AuthorizationCode authorizationCode, String currentUri)
            throws Throwable {
              
        String authCode = authorizationCode.getValue();
        ClientCredential credential = new ClientCredential(clientId,
                clientSecret);
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(authority + tenant + "/", true,
                    service);
            Future<AuthenticationResult> future = context
                    .acquireTokenByAuthorizationCode(authCode, new URI(
                            currentUri), credential, null);
            result = future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException(
                    "authentication result was null");
        }
        //at this point, we have the access token.  We should be able to resolve the tenant next.
        return result;
    }

    private AuthenticationResult getAccessTokenFromCodeFlow()throws Exception {
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