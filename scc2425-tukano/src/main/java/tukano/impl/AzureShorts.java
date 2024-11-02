package tukano.impl;

import com.azure.cosmos.CosmosContainer;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import utils.ResourceUtils;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class AzureShorts implements Shorts {

    private static Logger Log = Logger.getLogger(AzureShorts.class.getName());

    private final CosmosContainer container;

    public AzureShorts(){

        Properties props = new Properties();
        ResourceUtils.loadPropertiesFromResources(props, "cosmosdb.properties");
        String containerName = props.getProperty("shortContainerName");

        this.container = CosmosClientContainer.getContainer(containerName);
    }

    @Override
    public Result<Short> createShort(String userId, String password) {
        return null;
    }

    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        return null;
    }

    @Override
    public Result<Short> getShort(String shortId) {
        return null;
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        return null;
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        return null;
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        return null;
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        return null;
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        return null;
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        return null;
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String password, String token) {
        return null;
    }
}
