package org.grouplens.mooc.cbf.dao;

import org.grouplens.lenskit.data.dao.UserDAO;

/**
 * Extended user DAO that finds users by name.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public interface UserNameDAO extends UserDAO {
    /**
     * Look up a user by name.
     * @param name The user name.
     * @return The user ID (>= 0), or -1 if the user does not exist.
     */
    public long getUserByName(String name);
}
