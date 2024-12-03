package utils;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import tukano.api.User;
import tukano.impl.rest.utils.cookies.RequestCookies;

import java.util.UUID;

import static tukano.impl.rest.RestLoginResource.COOKIE_KEY;
import static tukano.impl.rest.RestLoginResource.MAX_COOKIE_AGE;

public class AuthUtils {
    public static boolean authorizationOk(User user, String pwd) {
        return user.getPwd().equals(pwd);
    }

    public static NewCookie createCookie(String username) {
        String uuid = UUID.randomUUID().toString();
        var cookie = new NewCookie.Builder(COOKIE_KEY)
                .value(uuid).path("/")
                .comment("sessionid")
                .maxAge(MAX_COOKIE_AGE)
                .secure(false) //ideally it should be true to only work for https requests
                .httpOnly(true)
                .build();

        CacheUtils.storeSessionInCache(new Session(uuid, username));
        return cookie;
    }

    /**
     * validate session
     * retrieves the cookie from the request parameters
     *
     * @param username
     * @return
     * @throws NotAuthorizedException
     */
    public static Session validateSession(String username) throws NotAuthorizedException {
        var cookies = RequestCookies.get();
        return validateSession(cookies.get(COOKIE_KEY), username);
    }

    /**
     * validate session
     * This method only validates if the cookie is correct
     *
     * @param cookie
     * @return
     */
    public static Session validateSession(Cookie cookie) throws NotAuthorizedException {
        if (cookie == null)
            throw new NotAuthorizedException("No session initialized");

        var session = CacheUtils.getSessionFromCache(cookie.getValue()).getObject();
        if (session == null)
            throw new NotAuthorizedException("No valid session initialized");

        return session;
    }

    /**
     * validate session
     * This method validates if the cookie is correct and the username is the user from that session
     *
     * @param cookie
     * @param username
     * @return
     * @throws NotAuthorizedException
     */
    public static Session validateSession(Cookie cookie, String username) throws NotAuthorizedException {

        var session = validateSession(cookie);

        String user = session.user();
        if (user == null || user.isEmpty())
            throw new NotAuthorizedException("No valid session initialized");

        if (!user.equals(username))
            throw new NotAuthorizedException("Invalid user : " + user);

        return session;
    }
}
