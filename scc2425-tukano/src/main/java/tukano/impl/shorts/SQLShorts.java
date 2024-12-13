package tukano.impl.shorts;

import jakarta.ws.rs.core.Cookie;
import tukano.api.Short;
import tukano.api.*;
import tukano.impl.JavaBlobs;
import tukano.impl.rest.TukanoRestServer;
import tukano.impl.users.UsersImpl;
import utils.ResourceUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import static tukano.api.Result.*;
import static utils.AuthUtils.createCookie;

public class SQLShorts implements Shorts {
    private static final Logger Log = Logger.getLogger(SQLShorts.class.getName());
    private final Connection connection;
    private static SQLShorts instance;

    private SQLShorts() {
        try {
            // Explicitly load the PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            Log.severe("PostgreSQL JDBC Driver not found");
            throw new RuntimeException("PostgreSQL JDBC Driver missing", e);
        }

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
        String[] createTableStatements = {
                """
            CREATE TABLE IF NOT EXISTS Shorts (
                id VARCHAR(255) PRIMARY KEY,
                ownerId VARCHAR(255) NOT NULL,
                blobUrl TEXT NOT NULL,
                FOREIGN KEY (ownerId) REFERENCES Users(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Follows (
                id VARCHAR(255) PRIMARY KEY,
                follower VARCHAR(255) NOT NULL,
                followee VARCHAR(255) NOT NULL,
                FOREIGN KEY (follower) REFERENCES Users(id),
                FOREIGN KEY (followee) REFERENCES Users(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Likes (
                id VARCHAR(255) PRIMARY KEY,
                userId VARCHAR(255) NOT NULL,
                shortId VARCHAR(255) NOT NULL,
                ownerId VARCHAR(255) NOT NULL,
                FOREIGN KEY (userId) REFERENCES Users(id),
                FOREIGN KEY (shortId) REFERENCES Shorts(id),
                FOREIGN KEY (ownerId) REFERENCES Users(id)
            )
            """
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTableStatements) {
                stmt.execute(sql);
            }
        }
    }

    public static SQLShorts getInstance() {
        if (instance == null) {
            instance = new SQLShorts();
        }
        return instance;
    }

    @Override
    public Result<Short> createShort(String userId, String password) {
        Log.info(() -> String.format("createShort : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {
            String shortId = String.format("%s+%s", userId, UUID.randomUUID());
            String blobUrl = String.format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId);

            String sql = "INSERT INTO Shorts (id, ownerId, blobUrl) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, shortId);
                pstmt.setString(2, userId);
                pstmt.setString(3, blobUrl);
                pstmt.executeUpdate();

                return ok(new Short(shortId, userId, blobUrl).copyWithLikes(0));
            } catch (SQLException e) {
                Log.severe("Error creating short: " + e.getMessage());
                return error(Result.ErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<Short> getShort(String shortId) {
        Log.info(() -> String.format("getShort : shortId = %s\n", shortId));

        if (shortId == null)
            return error(ErrorCode.BAD_REQUEST);

        String sql = """
                    SELECT s.*, COUNT(l.id) as likes_count 
                    FROM Shorts s 
                    LEFT JOIN Likes l ON s.id = l.shortId 
                    WHERE s.id = ? 
                    GROUP BY s.id, s.ownerId, s.blobUrl
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, shortId);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                return error(ErrorCode.NOT_FOUND);
            }

            Short shrt = new Short(
                    rs.getString("id"),
                    rs.getString("ownerId"),
                    rs.getString("blobUrl")
            );

            return ok(shrt.copyWithLikes(rs.getLong("likes_count")));
        } catch (SQLException e) {
            Log.severe("Error getting short: " + e.getMessage());
            return error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        // for some reason the interface does not have a cookie parameter so we have to create a cookie first
        return errorOrResult(getShort(shortId), shrt -> deleteShort(shortId, password, createCookie(shrt.getOwnerId())));
    }

    private Result<Void> deleteShort(String shortId, String password, Cookie cookie) {
        Log.info(() -> String.format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shrt ->
                errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
                    try {
                        connection.setAutoCommit(false);
                        try {
                            // Delete likes first due to foreign key constraint
                            String deleteLikes = "DELETE FROM Likes WHERE shortId = ?";
                            try (PreparedStatement pstmt = connection.prepareStatement(deleteLikes)) {
                                pstmt.setString(1, shortId);
                                pstmt.executeUpdate();
                            }

                            // Delete the short
                            String deleteShort = "DELETE FROM Shorts WHERE id = ?";
                            try (PreparedStatement pstmt = connection.prepareStatement(deleteShort)) {
                                pstmt.setString(1, shortId);
                                pstmt.executeUpdate();
                            }

                            connection.commit();

                            // Delete the blob
                            String blobName = shrt.getBlobUrl().substring(
                                    shrt.getBlobUrl().lastIndexOf('/') + 1,
                                    shrt.getBlobUrl().lastIndexOf('?'));
                            JavaBlobs.getInstance().delete(blobName, cookie);

                            return ok();
                        } catch (SQLException e) {
                            connection.rollback();
                            throw e;
                        } finally {
                            connection.setAutoCommit(true);
                        }
                    } catch (SQLException e) {
                        Log.severe("Error deleting short: " + e.getMessage());
                        return error(ErrorCode.INTERNAL_ERROR);
                    }
                })
        );
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        Log.info(() -> String.format("getShorts : userId = %s\n", userId));

        String sql = "SELECT id FROM Shorts WHERE ownerId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            List<String> shortIds = new ArrayList<>();
            while (rs.next()) {
                shortIds.add(rs.getString("id"));
            }
            return ok(shortIds);
        } catch (SQLException e) {
            Log.severe("Error getting shorts: " + e.getMessage());
            return error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        Log.info(() -> String.format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n",
                userId1, userId2, isFollowing, password));

        return errorOrResult(okUser(userId1, password), user -> {
            String followId = String.format("%s-%s", userId1, userId2);
            try {
                if (isFollowing) {
                    String sql = "INSERT INTO Follows (id, follower, followee) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        pstmt.setString(1, followId);
                        pstmt.setString(2, userId1);
                        pstmt.setString(3, userId2);
                        pstmt.executeUpdate();
                    }
                } else {
                    String sql = "DELETE FROM Follows WHERE id = ?";
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        pstmt.setString(1, followId);
                        pstmt.executeUpdate();
                    }
                }
                return ok();
            } catch (SQLException e) {
                Log.severe("Error managing follow: " + e.getMessage());
                return error(ErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        Log.info(() -> String.format("followers : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {
            String sql = "SELECT follower FROM Follows WHERE followee = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();

                List<String> followers = new ArrayList<>();
                while (rs.next()) {
                    followers.add(rs.getString("follower"));
                }
                return ok(followers);
            } catch (SQLException e) {
                Log.severe("Error getting followers: " + e.getMessage());
                return error(ErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        Log.info(() -> String.format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n",
                shortId, userId, isLiked, password));

        return errorOrResult(getShort(shortId), shrt -> {
            String likeId = String.format("%s-%s", userId, shortId);
            try {
                if (isLiked) {
                    String sql = "INSERT INTO Likes (id, userId, shortId, ownerId) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        pstmt.setString(1, likeId);
                        pstmt.setString(2, userId);
                        pstmt.setString(3, shortId);
                        pstmt.setString(4, shrt.getOwnerId());
                        pstmt.executeUpdate();
                    }
                } else {
                    String sql = "DELETE FROM Likes WHERE id = ?";
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        pstmt.setString(1, likeId);
                        pstmt.executeUpdate();
                    }
                }
                return ok();
            } catch (SQLException e) {
                Log.severe("Error managing like: " + e.getMessage());
                return error(ErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        Log.info(() -> String.format("likes : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shrt ->
                errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
                    String sql = "SELECT userId FROM Likes WHERE shortId = ?";
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        pstmt.setString(1, shortId);
                        ResultSet rs = pstmt.executeQuery();

                        List<String> likeUsers = new ArrayList<>();
                        while (rs.next()) {
                            likeUsers.add(rs.getString("userId"));
                        }
                        return ok(likeUsers);
                    } catch (SQLException e) {
                        Log.severe("Error getting likes: " + e.getMessage());
                        return error(ErrorCode.INTERNAL_ERROR);
                    }
                })
        );
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Log.info(() -> String.format("getFeed : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {
            String sql = """
                    SELECT DISTINCT s.id 
                    FROM Shorts s
                    WHERE s.ownerId = ?
                    OR s.ownerId IN (
                        SELECT followee 
                        FROM Follows 
                        WHERE follower = ?
                    )
                    ORDER BY s.id DESC
                    """;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, userId);
                ResultSet rs = pstmt.executeQuery();

                List<String> shortIds = new ArrayList<>();
                while (rs.next()) {
                    shortIds.add(rs.getString("id"));
                }
                return ok(shortIds);
            } catch (SQLException e) {
                Log.severe("Error getting feed: " + e.getMessage());
                return error(ErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String password, Cookie cookie) {
        Log.info(() -> String.format("deleteAllShorts : userId = %s, password = %s, token = %s\n",
                userId, password, cookie));

        try {
            connection.setAutoCommit(false);
            try {
                // Delete all likes
                String deleteLikes = """
                        DELETE FROM Likes 
                        WHERE userId = ? 
                        OR shortId IN (SELECT id FROM Shorts WHERE ownerId = ?)
                        """;
                try (PreparedStatement pstmt = connection.prepareStatement(deleteLikes)) {
                    pstmt.setString(1, userId);
                    pstmt.setString(2, userId);
                    pstmt.executeUpdate();
                }

                // Delete all follows
                String deleteFollows = """
                        DELETE FROM Follows 
                        WHERE follower = ? OR followee = ?
                        """;
                try (PreparedStatement pstmt = connection.prepareStatement(deleteFollows)) {
                    pstmt.setString(1, userId);
                    pstmt.setString(2, userId);
                    pstmt.executeUpdate();
                }

                // Delete all blobs
                JavaBlobs.getInstance().deleteAllBlobs(userId, cookie);

                // Delete all shorts
                String deleteShorts = "DELETE FROM Shorts WHERE ownerId = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(deleteShorts)) {
                    pstmt.setString(1, userId);
                    pstmt.executeUpdate();
                }

                connection.commit();
                return ok();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            Log.severe("Error deleting all shorts: " + e.getMessage());
            return error(ErrorCode.INTERNAL_ERROR);
        }
    }

    protected Result<User> okUser(String userId, String pwd) {
        return UsersImpl.getInstance().getUser(userId, pwd);
    }
}
