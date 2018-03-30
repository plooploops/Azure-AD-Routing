package com.microsoft.routing.demo.azureadrouting;

import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
public class MongoDBHelper {
    
    private static String tenantDb = Configurations.GetValue(Configurations.Keys.TenantDb); // name of tenant db
    private static String tenantConnection = Configurations.GetValue(Configurations.Keys.TenantConnection); // connection string for cosmos db / mongo db
    private static String tenantCollection = Configurations.GetValue(Configurations.Keys.TenantCollection); // name of tenant collection
    private static String tenantId = Configurations.GetValue(Configurations.Keys.TenantId); // property for tenant id in json doc
  
    public static String FindTenantConfig(String tid){

        String ret = null;
        MongoClientURI uri = new MongoClientURI(tenantConnection);
        MongoClient mongoClient = null;

        try {

            mongoClient = new MongoClient(uri);        
            // Get database
            MongoDatabase database = mongoClient.getDatabase(tenantDb);

            // Get collection
            MongoCollection<Document> collection = database.getCollection(tenantCollection);

            Document queryResult = collection.find(Filters.eq(tenantId, tid)).first();
            ret = queryResult.toJson();

            System.out.println(queryResult.toJson());    	
            System.out.println( "Completed successfully" );  
        }
        catch (Exception e){
            //log it?
        } 
        finally {
        	if (mongoClient != null) {
        		mongoClient.close();
            }
            
            return ret;
        }
    }
}