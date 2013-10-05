package org.grouplens.mooc.cbf.dao;

import org.grouplens.lenskit.data.dao.ItemDAO;

import java.util.List;
import java.util.Set;

/**
 * Data access object providing access to item tags.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public interface ItemTagDAO extends ItemDAO {
    /**
     * Get the tags for a particular item.  A tag can appear multiple times if multiple users have
     * applied it.
     * @param item The item.
     * @return The item's tags.
     */
    List<String> getItemTags(long item);

    /**
     * Get all known tags.
     * @return The set of known tags.
     */
    Set<String> getTagVocabulary();
}
