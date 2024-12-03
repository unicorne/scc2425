package tukano.impl.rest;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.rest.RestLogin;
import tukano.impl.users.UsersImpl;
import utils.AuthUtils;
import utils.ResourceUtils;

import java.net.URI;

public class RestLoginResource extends RestResource implements RestLogin {
    public static final String COOKIE_KEY = "blobs:sessioncookie";
    private static final String LOGIN_PAGE = "loginpage.html";
    private static final String LOGIN_CONFIRMATION_PAGE = "loginconfirm.html";
    public static final int MAX_COOKIE_AGE = 3600000;
    public static final String REDIRECT_TO_AFTER_LOGIN = "/login/confirmation";

    @Override
    public String login() {
        return super.resultOrThrow(Result.ok(ResourceUtils.loadResourceAsString(LOGIN_PAGE)));
    }

    @Override
    public Response login(@FormParam(USER) String username, @FormParam(PWD) String password) {
        Result<User> res = UsersImpl.getInstance().getUser(username, password);
        if (res.isOK()) {
            var cookie = AuthUtils.createCookie(username);

            return Response.seeOther(URI.create(REDIRECT_TO_AFTER_LOGIN))
                    .cookie(cookie)
                    .build();
        } else
            throw new NotAuthorizedException("Incorrect login");
    }

    @Override
    public String loggedIn() {
        return super.resultOrThrow(Result.ok(ResourceUtils.loadResourceAsString(LOGIN_CONFIRMATION_PAGE)));
    }
}
