package tukano.impl.users;

import tukano.api.Users;
import utils.ResourceUtils;

import java.util.Properties;

/**
 * This class is a singleton factory for the Users interface.
 */
public class UsersImpl {

    private static Users impl;

    private UsersImpl(){

    }

    synchronized public static Users getInstance() {
        if (impl == null) {
            Properties DBProps = new Properties();
            ResourceUtils.loadPropertiesFromResources(DBProps, "db.properties");
            String dbtype = DBProps.getProperty("dbtype", "cosmosdb");
            switch (dbtype) {
                case "cosmosdb":
                    impl = AzureUsers.getInstance();
                    break;
                case "postgresql":
                    impl = SQLUsers.getInstance();
                    break;
                default:
                    impl = JavaUsers.getInstance();
            }
        }
        return impl;
    }
}
