package tukano.impl.shorts;

import tukano.api.Shorts;
import utils.ResourceUtils;

import java.util.Properties;

/**
 * This class is a singleton factory for the Shorts interface.
 */
public class ShortsImpl {

    private static Shorts impl;

    private ShortsImpl() {

    }

    synchronized public static Shorts getInstance() {
        if (impl == null) {
            Properties DBProps = new Properties();
            ResourceUtils.loadPropertiesFromResources(DBProps, "db.properties");
            String dbtype = DBProps.getProperty("dbtype", "cosmosdb");
            switch (dbtype) {
                case "cosmosdb":
                    impl = AzureShorts.getInstance();
                    break;
                case "postgresql":
                    impl = SQLShorts.getInstance();
                default:
                    impl = JavaShorts.getInstance();
            }
        }
        return impl;
    }
}
