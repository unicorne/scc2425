package utils;

import tukano.api.User;
import tukano.impl.Token;

public class AuthUtils {
    public static boolean authirizationOk(User user, String pwd){
        // password field can also be used for tokens
        if (Token.hasTokenFormat(pwd)) {
            return Token.isValid(pwd, user.getId());
        } else {
            if (user.getPwd().equals(pwd)){
                // generate a new token because none exists yet
                String token = Token.get(user.getId());
                CacheUtils.storeTokenInCache(user.getId(), token);
                return true;
            }
        }
        return false;
    }
}
