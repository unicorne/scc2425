package tukano.impl;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static tukano.api.Result.*;

public class AzureUsers implements Users {

    private static final Logger Log = Logger.getLogger(AzureUsers.class.getName());

    private final CosmosContainer container;

    private static AzureUsers instance;

    private AzureUsers() {

        Properties props = new Properties();
        ResourceUtils.loadPropertiesFromResources(props, "cosmosdb.properties");
        String containerName = props.getProperty("userContainerName");

        this.container = CosmosClientContainer.getContainer(containerName);
    }

    public static AzureUsers getInstance(){
        if (instance == null){
            instance = new AzureUsers();
        }
        return instance;
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info(() -> String.format("createUser : %s\n", user));

        if (badUserInfo(user))
            return error(Result.ErrorCode.BAD_REQUEST);

        try {
            CosmosItemResponse<User> response = container.createItem(user, new PartitionKey(user.getId()), new CosmosItemRequestOptions());
            User item = response.getItem();
            if(item == null){
                Log.severe(() -> String.format("Error creating User %s\n", user));
                return error(Result.ErrorCode.INTERNAL_ERROR);
            }
            return ok(item.getId());
        } catch (CosmosException e) {
            Log.severe(() -> String.format("Error creating User %s\n%s", user, e.getMessage()));
            return error(Result.ErrorCode.CONFLICT);
        }
}

    @Override
    public Result<User> getUser(String userId, String pwd) {
        Log.info(() -> String.format("getUser : userId = %s, pwd = %s\n", userId, pwd));

        try {
            User user = container.readItem(userId, new PartitionKey(userId), User.class).getItem();
            if(user == null){
                Log.severe(() -> String.format("Error getting User with Id %s. Null result\n", userId));
                return error(ErrorCode.NOT_FOUND);
            }
            if(!user.getPwd().equals(pwd)){
                Log.severe(() -> String.format("Wrong password for user with Id %s\n", userId));
                return error(ErrorCode.UNAUTHORIZED);
            }
            return ok(user);
        } catch (CosmosException e) {
            Log.severe(() -> String.format("Error getting User with Id %s\n%s", userId, e.getMessage()));
            return error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User newUserInfo) {
        Log.info(() -> String.format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, newUserInfo));

        try {
            User existingUser = container.readItem(userId, new PartitionKey(userId), User.class).getItem();
            if (existingUser == null) {
                Log.severe(() -> String.format("Could not find user to update. user-id=%s\n", userId));
                return error(ErrorCode.NOT_FOUND);
            }
            User updatedUser = existingUser.updateFrom(newUserInfo);
            CosmosItemResponse<User> response = container.replaceItem(updatedUser, userId, new PartitionKey(userId), new CosmosItemRequestOptions());
            User item = response.getItem();
            if(item == null){
                Log.severe(() -> String.format("Error updating User with Id %s\n", userId));
                return error(ErrorCode.INTERNAL_ERROR);
            }
            if(!item.getPwd().equals(pwd)){
                Log.severe(() -> String.format("Wrong password for user with Id %s\n", userId));
                return error(ErrorCode.UNAUTHORIZED);
            }
            return ok(response.getItem());
        } catch (CosmosException e) {
            Log.severe(() -> String.format("Error updating User with Id %s\n%s", userId, e.getMessage()));
            return error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        Log.info(() -> String.format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

        try {
            User user = container.readItem(userId, new PartitionKey(userId), User.class).getItem();
            if (user == null) {
                Log.severe(() -> String.format("Could not find user to delete. user-id=%s\n", userId));
                return error(ErrorCode.NOT_FOUND);
            }
            if(!user.getPwd().equals(pwd)){
                Log.severe(() -> String.format("Wrong password for user with Id %s\n", userId));
                return error(ErrorCode.UNAUTHORIZED);
            }
            container.deleteItem(userId, new PartitionKey(userId), new CosmosItemRequestOptions());
            return ok(user);
        } catch (CosmosException e) {
            Log.severe(() -> String.format("Error deleting User with Id %s\n%s", userId, e.getMessage()));
            return error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info(() -> String.format("searchUsers : pattern = %s\n", pattern));

        String query = String.format("SELECT * FROM c WHERE CONTAINS(UPPER(c.id), '%s')", pattern.toUpperCase());
        CosmosPagedIterable<User> users = container.queryItems(query, new CosmosQueryRequestOptions(), User.class);

        List<User> userList = new ArrayList<>();
        users.forEach(user -> userList.add(user.copyWithoutPassword()));
        Log.info(() -> String.format("query returned %d items\n", userList.size()));

        return ok(userList);
    }

    private boolean badUserInfo(User user) {
        return (user.getId() == null || user.getPwd() == null || user.getDisplayName() == null || user.getEmail() == null);
    }
}
