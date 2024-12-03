package tukano.impl.rest;

import jakarta.inject.Singleton;
import tukano.api.User;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.users.UsersImpl;

import java.util.List;

@Singleton
public class RestUsersResource extends RestResource implements RestUsers {

    private final Users impl;

    public RestUsersResource() {
        impl = UsersImpl.getInstance();
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
