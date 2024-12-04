package tukano.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(RestLogin.PATH)
public interface RestLogin {
    String PATH = "/login";
    String USER = "username";
    String PWD = "password";

    @GET
    @Produces(MediaType.TEXT_HTML)
    String login();

    @POST
    Response login(@FormParam(USER) String user, @FormParam(PWD) String password);

    @GET
    @Path("/confirmation")
    @Produces(MediaType.TEXT_HTML)
    String loggedIn();
}
