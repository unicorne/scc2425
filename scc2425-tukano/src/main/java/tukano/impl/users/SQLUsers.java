package tukano.impl.users;

import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import tukano.impl.JavaBlobs;
import tukano.impl.shorts.SQLShorts;
import tukano.impl.Token;
import tukano.impl.shorts.ShortsImpl;
import utils.AuthUtils;
import utils.CacheUtils;
import utils.ResourceUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static tukano.api.Result.error;
import static tukano.api.Result.ok;
import static utils.AuthUtils.authorizationOk;

public class SQLUsers implements Users {
    private static final Logger Log = Logger.getLogger(SQLUsers.class.getName());
    private final Connection connection;
    private static SQLUsers instance;

    private SQLUsers() {
        Properties props = new Properties();
        ResourceUtils.loadPropertiesFromResources(props, "db.properties");
        String connectionString = props.getProperty("connectionString");
        String user = props.getProperty("username");
        String pass = props.getProperty("password");

        try {
            this.connection = DriverManager.getConnection(connectionString, user, pass);
            initializeTables();
        } catch (SQLException e) {
            Log.severe("Failed to initialize SQL connection: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void initializeTables() throws SQLException {
        String createUserTable = """
                    CREATE TABLE IF NOT EXISTS Users (
                        id VARCHAR(255) PRIMARY KEY,
                        pwd VARCHAR(255) NOT NULL,
                        displayName VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL
                    )
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUserTable);
        }
    }

    public static SQLUsers getInstance() {
        if (instance == null) {
            instance = new SQLUsers();
        }
        return instance;
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info(() -> String.format("createUser : %s\n", user));

        if (badUserInfo(user))
            return error(Result.ErrorCode.BAD_REQUEST);

        String sql = "INSERT INTO Users (id, pwd, displayName, email) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getPwd());
            pstmt.setString(3, user.getDisplayName());
            pstmt.setString(4, user.getEmail());
            pstmt.executeUpdate();
            return ok(user.getId());
        } catch (SQLException e) {
            Log.severe(() -> String.format("Error creating User %s\n%s", user, e.getMessage()));
            return error(Result.ErrorCode.CONFLICT);
        }
    }

    public Result<User> getUser(String userId, String pwd, boolean useCache) {
        Log.info(() -> String.format("getUser : userId = %s, pwd = %s, useCache = %b\n", userId, pwd, useCache));

        User user = null;
        if (useCache) {
            CacheUtils.CacheResult<User> cacheResult = CacheUtils.getUserFromCache(userId);
            if (cacheResult.isCacheHit()) {
                Log.info(() -> String.format("Cache hit for user with Id %s\n", userId));
                user = cacheResult.getObject();
            }
        }

        if (user == null) {
            String sql = "SELECT * FROM Users WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();

                if (!rs.next()) {
                    return error(Result.ErrorCode.NOT_FOUND);
                }

                user = new User(
                        rs.getString("id"),
                        rs.getString("pwd"),
                        rs.getString("displayName"),
                        rs.getString("email")
                );

                if (useCache) {
                    CacheUtils.storeUserInCache(user);
                }

            } catch (SQLException e) {
                Log.severe(() -> String.format("Error getting User with Id %s\n%s", userId, e.getMessage()));
                return error(Result.ErrorCode.NOT_FOUND);
            }
        }

        if (!authorizationOk(user, pwd)) {
            Log.severe(() -> String.format("Invalid cookie or password for user with Id %s\n", userId));
            return error(Result.ErrorCode.UNAUTHORIZED);
        }
        return ok(user);
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {
        return getUser(userId, pwd, true);
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User newUserInfo) {
        Log.info(() -> String.format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, newUserInfo));

        String sql = "UPDATE Users SET displayName = ?, email = ? WHERE id = ? AND pwd = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newUserInfo.getDisplayName());
            pstmt.setString(2, newUserInfo.getEmail());
            pstmt.setString(3, userId);
            pstmt.setString(4, pwd);

            int updatedRows = pstmt.executeUpdate();
            if (updatedRows == 0) {
                return error(Result.ErrorCode.NOT_FOUND);
            }

            return getUser(userId, pwd);
        } catch (SQLException e) {
            Log.severe(() -> String.format("Error updating User with Id %s\n%s", userId, e.getMessage()));
            return error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        Log.info(() -> String.format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

        Result<User> user = getUser(userId, pwd);
        if (!user.isOK()) {
            return user;
        }
        // delete associated shorts
        ShortsImpl.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
        // delete associated blobs
        var cookie = AuthUtils.createCookie(userId);
        JavaBlobs.getInstance().deleteAllBlobs(userId, cookie);
        // delete user
        String sql = "DELETE FROM Users WHERE id = ? AND pwd = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, pwd);
            pstmt.executeUpdate();
            // update cache
            CacheUtils.removeUserFromCache(userId);
            return user;
        } catch (SQLException e) {
            Log.severe(() -> String.format("Error deleting User with Id %s\n%s", userId, e.getMessage()));
            return error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info(() -> String.format("searchUsers : pattern = %s\n", pattern));

        String sql = "SELECT * FROM Users WHERE UPPER(id) LIKE UPPER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + pattern + "%");
            ResultSet rs = pstmt.executeQuery();

            List<User> users = new ArrayList<>();
            while (rs.next()) {
                CacheUtils.storeUserInCache(new User(
                        rs.getString("id"),
                        rs.getString("pwd"),
                        rs.getString("displayName"),
                        rs.getString("email")));

                users.add(new User(
                        rs.getString("id"),
                        null,
                        rs.getString("displayName"),
                        rs.getString("email")
                ));
            }

            return ok(users);
        } catch (SQLException e) {
            Log.severe("Error searching users: " + e.getMessage());
            return error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private boolean badUserInfo(User user) {
        return (user.getId() == null || user.getPwd() == null ||
                user.getDisplayName() == null || user.getEmail() == null);
    }
}