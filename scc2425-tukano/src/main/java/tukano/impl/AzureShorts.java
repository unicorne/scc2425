package tukano.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.util.CosmosPagedIterable;
import tukano.api.*;
import tukano.api.Short;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestServer;
import utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import java.util.UUID;
import java.util.stream.Collectors;
import static java.lang.String.format;
import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.*;

public class AzureShorts implements Shorts {

    private static final Logger Log = Logger.getLogger(AzureShorts.class.getName());

    private final CosmosContainer container;
    private final String blobContainerName;

    private static AzureShorts instance;

    private AzureShorts(){

        Properties cosmosDBProps = new Properties();
        ResourceUtils.loadPropertiesFromResources(cosmosDBProps, "cosmosdb.properties");
        String shortContainerName = cosmosDBProps.getProperty("shortContainerName");
        Properties blobContainerProps = new Properties();
        ResourceUtils.loadPropertiesFromResources(blobContainerProps, "azureblob.properties");
        blobContainerName = blobContainerProps.getProperty("blobContainerName");

        this.container = CosmosClientContainer.getContainer(shortContainerName);
    }

    public static AzureShorts getInstance(){
        if (instance == null){
            instance = new AzureShorts();
        }
        return instance;
    }

    @Override
    public Result<Short> createShort(String userId, String password) {
        Log.info(() -> format("createShort : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {
            var shortId = format("%s+%s", userId, UUID.randomUUID());
            var blobUrl = format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId);
            var shrt = new Short(shortId, userId, blobUrl);

            try {
                container.createItem(shrt);
                return ok(shrt.copyWithLikes_And_Token(0));
            } catch (Exception e) {
                Log.severe("Error creating short: " + e.getMessage());
                return error(INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<Short> getShort(String shortId) {
        Log.info(() -> format("getShort : shortId = %s\n", shortId));

        if (shortId == null)
            return error(BAD_REQUEST);

        try {
            // Get the short
            var short_response = container.readItem(shortId, new PartitionKey(shortId), Short.class);
            var shrt = short_response.getItem();

            // Count likes using a query
            String query = "SELECT VALUE COUNT(1) FROM c WHERE c.type = 'LIKE' AND c.shortId = '" + shortId + "'";
            var likes_response = container.queryItems(query, new CosmosQueryRequestOptions(), Long.class);
            long likesCount = likes_response.iterator().next();

            return ok(shrt.copyWithLikes_And_Token(likesCount));
        } catch (Exception e) {
            Log.severe("Error getting short: " + e.getMessage());
            return error(NOT_FOUND);
        }
    }

    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shrt ->
                errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
                    try {
                        // Delete the short
                        container.deleteItem(shortId, new PartitionKey(shortId), new CosmosItemRequestOptions());

                        // Delete associated likes
                        String query = "SELECT * FROM c WHERE c.type = 'LIKE' AND c.shortId = '" + shortId + "'";
                        container.queryItems(query, new CosmosQueryRequestOptions(), Likes.class)
                                .forEach(like -> container.deleteItem(like.getId(), new PartitionKey(like.getId()), new CosmosItemRequestOptions()));

                        // Delete the blob
                        String blobName = shrt.getBlobUrl().substring(shrt.getBlobUrl().lastIndexOf('/') + 1, shrt.getBlobUrl().lastIndexOf('?'));
                        JavaBlobs.getInstance().delete(blobName, Token.get());

                        return ok();
                    } catch (Exception e) {
                        Log.severe("Error deleting short: " + e.getMessage());
                        return error(INTERNAL_ERROR);
                    }
                })
        );
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        Log.info(() -> format("getShorts : userId = %s\n", userId));

        String query = "SELECT * FROM c WHERE c.type = 'SHORT' AND c.ownerId = '" + userId + "'";
        try {
            var response = container.queryItems(query, new CosmosQueryRequestOptions(), Short.class);
            List<String> ids = response.stream().map(Short::getId).toList();
            return ok(ids);
        } catch (Exception e) {
            Log.severe("Error getting shorts: " + e.getMessage());
            return error(INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n",
                userId1, userId2, isFollowing, password));

        return errorOrResult(okUser(userId1, password), user -> {
            try {
                String followId = format("%s-%s", userId1, userId2);
                if (isFollowing) {
                    var following = new Following(followId, userId1, userId2);
                    container.createItem(following);
                } else {
                    container.deleteItem(followId, new PartitionKey(followId), new CosmosItemRequestOptions());
                }
                return ok();
            } catch (Exception e) {
                Log.severe("Error managing follow: " + e.getMessage());
                return error(INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {
            String query = "SELECT * FROM c WHERE c.type = 'FOLLOWING' AND c.followee = '" + userId + "'";
            try {
                var response = container.queryItems(query, new CosmosQueryRequestOptions(), Following.class);
                List<String> ids = response.stream().map(Following::getFollower).toList();
                return ok(ids);
            } catch (Exception e) {
                Log.severe("Error getting followers: " + e.getMessage());
                return error(INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n",
                shortId, userId, isLiked, password));

        return errorOrResult(getShort(shortId), shrt -> {
            try {
                String likeId = format("%s-%s", userId, shortId);
                if (isLiked) {
                    var like = new Likes(likeId, userId, shortId, shrt.getOwnerId());
                    container.createItem(like);
                } else {
                    container.deleteItem(likeId, new PartitionKey(likeId), new CosmosItemRequestOptions());
                }
                return ok();
            } catch (Exception e) {
                Log.severe("Error managing like: " + e.getMessage());
                return error(INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shrt ->
                errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
                    String query = "SELECT * FROM c WHERE c.type = 'LIKE' AND c.shortId = '" + shortId + "'";
                    try {
                        var response = container.queryItems(query, new CosmosQueryRequestOptions(), Likes.class);
                        List<String> ids = response.stream().map(Likes::getUserId).toList();
                        return ok(ids);
                    } catch (Exception e) {
                        Log.severe("Error getting likes: " + e.getMessage());
                        return error(INTERNAL_ERROR);
                    }
                })
        );
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {
            String queryFollowees = "SELECT * FROM f WHERE f.type = 'FOLLOWING' AND f.follower = '%s'".formatted(userId);
            String queryFeedBase = """
                    SELECT *
                    FROM c
                    WHERE c.type = 'SHORT' AND (
                        c.ownerId = '%s' OR
                        c.ownerId IN (%s)
                    )
                    ORDER BY c._ts DESC
                    """;
            try {
                var followeeResponse = container.queryItems(queryFollowees, new CosmosQueryRequestOptions(), Following.class);
                String followeeIds = followeeResponse.stream().map(f -> "'" + f.getFollowee() + "'").collect(Collectors.joining(", "));
                String queryFeed = queryFeedBase.formatted(userId, followeeIds);
                var feedResponse = container.queryItems(queryFeed, new CosmosQueryRequestOptions(), Short.class);
                List<String> shortIds = feedResponse.stream().map(Short::getId).toList();
                return ok(shortIds);
            } catch (Exception e) {
                Log.severe("Error getting feed: " + e.getMessage());
                return error(INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String password, String token) {
        Log.info(() -> format("deleteAllShorts : userId = %s, password = %s, token = %s\n",
                userId, password, token));

        if (!Token.isValid(token, userId))
            return error(FORBIDDEN);

        try {
            // Delete all shorts
            String shortsQuery = "SELECT * FROM c WHERE c.type = 'SHORT' AND c.ownerId = '" + userId + "'";
            container.queryItems(shortsQuery, new CosmosQueryRequestOptions(), Short.class)
                    .forEach(s -> container.deleteItem(s.getId(), new PartitionKey(s.getId()), new CosmosItemRequestOptions()));

            // Delete all follows
            String followsQuery = "SELECT * FROM c WHERE c.type = 'FOLLOWING' AND (c.follower = '" + userId + "' OR c.followee = '" + userId + "')";
            container.queryItems(followsQuery, new CosmosQueryRequestOptions(), Following.class)
                    .forEach(f -> container.deleteItem(f.getId(), new PartitionKey(f.getId()), new CosmosItemRequestOptions()));

            // Delete all likes
            String likesQuery = "SELECT * FROM c WHERE c.type = 'LIKE' AND (c.userId = '" + userId + "' OR c.ownerId = '" + userId + "')";
            container.queryItems(likesQuery, new CosmosQueryRequestOptions(), Likes.class)
                    .forEach(l -> container.deleteItem(l.getId(), new PartitionKey(l.getId()), new CosmosItemRequestOptions()));

            return ok();
        } catch (Exception e) {
            Log.severe("Error deleting all shorts: " + e.getMessage());
            return error(INTERNAL_ERROR);
        }
    }

    protected Result<User> okUser(String userId, String pwd) {
        return AzureUsers.getInstance().getUser(userId, pwd);
    }
}
