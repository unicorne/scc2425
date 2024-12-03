package tukano.api.rest;


import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;

import static tukano.impl.rest.RestLoginResource.COOKIE_KEY;

@Path(RestBlobs.PATH)
public interface RestBlobs {

    String PATH = "/blobs";
    String BLOB_ID = "blobId";
    String TOKEN = "token";
    String BLOBS = "blobs";
    String USER_ID = "userId";

    @POST
    @Path("/{" + BLOB_ID + "}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    void upload(@PathParam(BLOB_ID) String blobId, byte[] bytes, @CookieParam(COOKIE_KEY) Cookie cookie);

    @GET
    @Path("/{" + BLOB_ID + "}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    byte[] download(@PathParam(BLOB_ID) String blobId, @CookieParam(COOKIE_KEY) Cookie cookie);

    @DELETE
    @Path("/{" + BLOB_ID + "}")
    void delete(@PathParam(BLOB_ID) String blobId, @CookieParam(COOKIE_KEY) Cookie cookie);

    @DELETE
    @Path("/{" + USER_ID + "}/" + BLOBS)
    void deleteAllBlobs(@PathParam(USER_ID) String userId, @CookieParam(COOKIE_KEY) Cookie cookie);
}
