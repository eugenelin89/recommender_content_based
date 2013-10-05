package org.grouplens.mooc.cbf.dao;

import org.grouplens.lenskit.data.dao.ItemDAO;

import javax.annotation.Nullable;

/**
 * Extended item DAO that loads item titles.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public interface ItemTitleDAO extends ItemDAO {
    /**
     * Get the title for a movie.
     * @param item The movie ID.
     * @return The movie's title, or {@code null} if no such movie exists.
     */
    @Nullable
    public String getItemTitle(long item);
}
