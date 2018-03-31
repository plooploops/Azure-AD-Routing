# Azure-AD-Routing
Testing out Azure AD Routing with ADAL JDK for multitenancy apps.

This is intended to work with Azure App Services (API App), Mongo DB / Cosmos DB, and Azure AD.

We can post to the endpoint at /webapi and this will redirect to the end point assuming that the credentials are located in the tenant db.

We can get on the endpoint at /webapp and this will redirect to the end point assuming that the credentials are located in the tenant db.

![](images/AzureADCodeFlow.png?raw=true)

## Helpful Links

https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code#request-an-authorization-code

https://docs.microsoft.com/en-us/azure/app-service/web-sites-configure

https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-java

https://docs.microsoft.com/en-us/azure/app-service/web-sites-configure#application-configuration-examples

https://docs.microsoft.com/en-us/azure/cosmos-db/create-mongodb-java

## Building

We can build the project with gradle.  This will generate a jar file for deployment later.

```
gradle clean build
```

## Deployment

### Azure AD
We'll want to ensure that our organization will be able to test, so we can login to Azure and check our own organization's tenant ID.  This way we can test using our own domain first.

![](images/AzureADDirectoryId.png?raw=true)
Take note of this directory ID, as this is the tenant id that we will resolve for a user from the domain.

### Azure Cosmos DB for Mongo API
Set up a Cosmos DB with Mongo API in Azure.
https://docs.microsoft.com/en-us/azure/cosmos-db/create-mongodb-java

We are also going to use the Mongo DB SDK and include it in gradle.  It can be found here:
https://mvnrepository.com/artifact/org.mongodb/mongo-java-driver

We'll want to take note of a tenant db (we can call it TenantConfig), the collection (TenantConfig), the connection string, and the schema.

Add a document to the database:
![](images/AzureCosmosConfig0.png?raw=true)

Add a tenant:
![](images/AzureCosmosConfig1.png?raw=true)

Connection String:
![](images/AzureCosmosConfig2.png?raw=true)

The schema should be in the form of:
```
{
	"_id" : ObjectId(""), //Auto generated guid
	"tid" : "", //GUID for Azure AD Directory ID
	"webAppUrl" : "http://bing.com", //example url for redirect with GET
	"webApiUrl" : "http://bing.com" //example url for redirect with POST
}
```
### Azure App Services API App

We'll want to set up an Azure App Service API App.  This can be added in the marketplace, and we'll want to set some settings for it.
![](images/AzureAppServiceConfig1.png?raw=true)

These are the application settings that will be read by the app service.
![](images/AzureAppServiceConfig2.png?raw=true)

### Azure App Service Kudu Deployment

The Kudu powershell console can be reached under the https://appservicename.scm.azurewebsites.net.  This console will let us deploy the jar file that we can build for the app, which will sit in the /build/Routing-x.jar.  We can drag and drop the jar file into $home/site/wwwroot/webapps/. which is for Springboot.

![](images/AzureAppServiceKudu0.png?raw=true)

We will also want to place a web.config for the spring boot app to run a given jar file, which we'll place under site/wwwroot/.

![](images/AzureAppServiceKudu1.png?raw=true)
![](images/AzureAppServiceKudu2.png?raw=true)

The Web Config has a path specified which should be on the Azure App Service.  In this case we point to the jar file that we deployed at $home/site/wwwroot/webapps/.

```
<!--this is for the azure app service configuration-->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <system.webServer>
    <handlers>
      <add name="httpPlatformHandler" path="*" verb="*" modules="httpPlatformHandler" resourceType="Unspecified" />
    </handlers>
    <httpPlatform processPath="%JAVA_HOME%\bin\java.exe"
        arguments="-Djava.net.preferIPv4Stack=true -Dserver.port=%HTTP_PLATFORM_PORT% -jar &quot;%HOME%\site\wwwroot\webapps\Routing-0.0.1-SNAPSHOT.jar&quot;">
    </httpPlatform>
  </system.webServer>
</configuration>
```

## Testing

### Local testing

We can use swap in values or else set environment variables that will be picked up at run time.

Springboot will use a default endpoint for testing:

http://localhost:8080/

And we can test with http://localhost:8080/webapp endpoint in the browser.

We can also utilize CURL or Postman for the other endpoint too.

POST http://localhost:8080/webapi and the JSON payload.

Assuming that our credentials are picked up in the domain, we should have redirects to the URL that we specified in the tenant db.

### App Service Testing

We can swap in the values for the application and deploy the jar.

And we can test with https://appservice.azurewebsites.net/webapp endpoint in the browser.

![](images/AzureAppServiceRedirectWebAppTest.png?raw=true)

We can also utilize CURL or Postman for the other endpoint too.

POST https://appservice.azurewebsites.net/webapi and the JSON payload.
![](images/AzureAppServiceRedirectWebApiTest.png?raw=true)

Assuming that our credentials are picked up in the domain, we should have redirects to the URL that we specified in the tenant db.

## Contributors
A special thanks to Stewart Adam, Prashant Karbhari, Doris Chen, Marc Kuperstein, Max Knor, Justine Cocchi, Reenu Saluja, Denis Kisselev, Neeraj Joshi, Ryan CrawCour, Bruno Terkaly, Robin Drolet, and George LeBlanc for patience and great discussion.

Andy Gee

## Todos
It would be great to cover some of the automation for deployment to Azure.