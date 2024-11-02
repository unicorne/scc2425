package tukano.impl;

import com.azure.cosmos.*;
import utils.ResourceUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CosmosClientContainer {

    private static CosmosClientContainer instance;
    private final CosmosClient client;
    private final CosmosDatabase database;

    private CosmosClientContainer(){
        Properties props = new Properties();
        ResourceUtils.loadPropertiesFromResources(props, "cosmosdb.properties");
        String CONNECTION_URL = props.getProperty("connectionUrl");
        String DB_KEY = props.getProperty("dbKey");
        String DATABASE_NAME = props.getProperty("dbName");

        client = new CosmosClientBuilder()
                .endpoint(CONNECTION_URL)
                .key(DB_KEY)
                .gatewayMode()
                .consistencyLevel(ConsistencyLevel.SESSION)
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();

        database = client.getDatabase(DATABASE_NAME);
    }

    public static CosmosClientContainer getInstance(){
        if (instance == null){
            instance = new CosmosClientContainer();
        }
        return instance;
    }

    public static CosmosClient getClient(){
        return getInstance().client;
    }

    public static CosmosDatabase getDatabase(){
        return getInstance().database;
    }

    public static CosmosContainer getContainer(String containerName){
        return getDatabase().getContainer(containerName);
    }
}
