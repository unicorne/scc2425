package tukano.impl.rest;

import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;

import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.core.Application;

import tukano.impl.Token;
import utils.Args;
import utils.IP;
import tukano.impl.rest.utils.cookies.RequestCookiesCleanupFilter;
import tukano.impl.rest.utils.cookies.RequestCookiesFilter;


public class TukanoRestServer extends Application {
	final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());

	static final String INETADDR_ANY = "0.0.0.0";
	static String SERVER_BASE_URI = "http://%s:%s/rest";

	public static final int PORT = 8080;

	public static String serverURI;

	private final Set<Class<?>> resources = new HashSet<>();
			
	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	public TukanoRestServer() {
		serverURI = String.format(SERVER_BASE_URI, IP.hostname(), PORT);

		resources.add(RestBlobsResource.class);
		resources.add(RestShortsResource.class);
		resources.add(RestUsersResource.class);
		resources.add(RestLoginResource.class);
		resources.add(RequestCookiesFilter.class);
		resources.add(RequestCookiesCleanupFilter.class);
	}


	protected void start() {
	
		ResourceConfig config = new ResourceConfig();
		
		config.register(RestBlobsResource.class);
		config.register(RestUsersResource.class); 
		config.register(RestShortsResource.class);

		Log.info(String.format("Tukano Server ready @ %s\n",  serverURI));
	}
	
	
	public static void main(String[] args) {
		Args.use(args);
		
		Token.setSecret( Args.valueOf("-secret", ""));

		new TukanoRestServer().start();
	}

	public Set<Class<?>> getClasses(){
		return resources;
	}
}
