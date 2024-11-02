package tukano.impl;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static tukano.api.Result.*;

public class AzureUsers implements Users {

    private static final String CONNECTION_URL;
    private static final String DB_KEY;
    private static final String DATABASE_NAME;
    private static final String CONTAINER_NAME;

    private static Logger Log = Logger.getLogger(AzureUsers.class.getName());

    private CosmosClient client;
    private CosmosDatabase database;
    private CosmosContainer container;

    public AzureUsers() {

        Properties props = new Properties();
        CONNECTION_URL = props.getProperty("connectionUrl");
        DB_KEY = props.getProperty("dbKey");
        DATABASE_NAME = props.getProperty("dbName");
        CONTAINER_NAME = props.getProperty("dbContainerName");

        this.client = new CosmosClientBuilder()
                .endpoint(CONNECTION_URL)
                .key(DB_KEY)
                .gatewayMode()
                .consistencyLevel(ConsistencyLevel.SESSION)
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();

        this.database = client.getDatabase(DATABASE_NAME);
        this.container = database.getContainer(CONTAINER_NAME);
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info(() -> String.format("createUser : %s\n", user));

        if (badUserInfo(user))
            return error(Result.ErrorCode.BAD_REQUEST);

        try {
            container.createItem(user, new PartitionKey(user.getId()), new CosmosItemRequestOptions());
            return ok(user.getId());
        } catch (CosmosException e) {
            return error(Result.ErrorCode.CONFLICT);
        }
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {
        Log.info(() -> String.format("getUser : userId = %s, pwd = %s\n", userId, pwd));

        try {
            User user = container.readItem(userId, new PartitionKey(userId), User.class).getItem();
            return user != null && user.getPwd().equals(pwd) ? ok(user) : error(Result.ErrorCode.FORBIDDEN);
        } catch (CosmosException e) {
            return error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User newUserInfo) {
        Log.info(() -> String.format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, newUserInfo));

        try {
            User existingUser = container.readItem(userId, new PartitionKey(userId), User.class).getItem();
            if (existingUser != null && existingUser.getPwd().equals(pwd)) {
                existingUser.updateFrom(newUserInfo);
                container.replaceItem(existingUser, userId, new PartitionKey(userId), new CosmosItemRequestOptions());
                return ok(existingUser);
            }
            return error(Result.ErrorCode.FORBIDDEN);
        } catch (CosmosException e) {
            return error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        Log.info(() -> String.format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

        try {
            User user = container.readItem(userId, new PartitionKey(userId), User.class).getItem();
            if (user != null && user.getPwd().equals(pwd)) {
                container.deleteItem(userId, new PartitionKey(userId), new CosmosItemRequestOptions());
                return ok(user);
            }
            return error(Result.ErrorCode.FORBIDDEN);
        } catch (CosmosException e) {
            return error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info(() -> String.format("searchUsers : pattern = %s\n", pattern));

        String query = String.format("SELECT * FROM c WHERE CONTAINS(UPPER(c.userId), '%s')", pattern.toUpperCase());
        CosmosPagedIterable<User> users = container.queryItems(query, new CosmosQueryRequestOptions(), User.class);

        List<User> userList = new ArrayList<>();
        users.forEach(user -> userList.add(user.copyWithoutPassword()));

        return ok(userList);
    }

    private boolean badUserInfo(User user) {
        return (user.getId() == null || user.getPwd() == null || user.getDisplayName() == null || user.getEmail() == null);
    }
}
