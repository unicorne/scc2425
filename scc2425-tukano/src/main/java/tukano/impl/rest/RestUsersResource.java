package tukano.impl.rest;

import jakarta.inject.Singleton;
import tukano.api.User;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.AzureUsers;
import tukano.impl.SQLUsers;
import utils.ResourceUtils;

import java.util.List;
import java.util.Properties;

@Singleton
public class RestUsersResource extends RestResource implements RestUsers {

    private final Users impl;

    public RestUsersResource() {
        Properties cosmosDBProps = new Properties();
        ResourceUtils.loadPropertiesFromResources(cosmosDBProps, "db.properties");
        String dbtype = cosmosDBProps.getProperty("dbtype", "cosmosdb");
        switch (dbtype) {
            case "cosmosdb":
                this.impl = AzureUsers.getInstance();
                break;
            case "postgresql":
                this.impl = SQLUsers.getInstance();
            default:
                throw new IllegalArgumentException("Unknown dbtype: " + dbtype);
        }
    }

    @Override
    public String createUser(User user) {
        return super.resultOrThrow(impl.createUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return super.resultOrThrow(impl.getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
        return super.resultOrThrow(impl.updateUser(name, pwd, user));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return super.resultOrThrow(impl.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String pattern) {
        return super.resultOrThrow(impl.searchUsers(pattern));
    }
}
