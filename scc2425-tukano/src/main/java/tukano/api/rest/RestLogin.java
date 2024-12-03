package tukano.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tukano.impl.rest.RestLoginResource;

@Path(RestLogin.PATH)
public interface RestLogin {
    static final String PATH = "login";
    static final String USER = "username";
    static final String PWD = "password";

    @GET
    @Produces(MediaType.TEXT_HTML)
    String login();

    @POST
    Response login(@FormParam(USER) String user, @FormParam(PWD) String password );

    @GET
    @Path("/confirmation")
    @Produces(MediaType.TEXT_HTML)
    String loggedIn();
}
