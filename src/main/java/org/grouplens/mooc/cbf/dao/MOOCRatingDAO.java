package org.grouplens.mooc.cbf.dao;

import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.cursors.Cursors;
import org.grouplens.lenskit.data.dao.EventCollectionDAO;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.dao.SimpleFileRatingDAO;
import org.grouplens.lenskit.data.dao.SortOrder;
import org.grouplens.lenskit.data.event.Event;

import javax.inject.Inject;
import java.io.File;

/**
 * Customized rating DAO for MOOC ratings.  This just wraps some standard LensKit DAOs in an
 * easy-to-configure interface.
 *
 * @see org.grouplens.lenskit.data.dao.EventCollectionDAO
 * @see org.grouplens.lenskit.data.dao.SimpleFileRatingDAO
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class MOOCRatingDAO implements EventDAO {
    private final SimpleFileRatingDAO csvDao;
    private transient volatile EventCollectionDAO cache;

    @Inject
    public MOOCRatingDAO(@RatingFile File file) {
        csvDao = new SimpleFileRatingDAO(file, ",");
    }

    /**
     * Pre-fetch the ratings into memory if we haven't done so already.
     */
    private void ensureRatingCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = new EventCollectionDAO(Cursors.makeList(csvDao.streamEvents()));
                }
            }
        }
    }

    @Override
    public Cursor<Event> streamEvents() {
        // delegate to the cached event collection DAO
        ensureRatingCache();
        return cache.streamEvents();
    }

    @Override
    public <E extends Event> Cursor<E> streamEvents(Class<E> type) {
        // delegate to the cached event collection DAO
        ensureRatingCache();
        return cache.streamEvents(type);
    }

    @Override
    public <E extends Event> Cursor<E> streamEvents(Class<E> type, SortOrder order) {
        // delegate to the cached event collection DAO
        ensureRatingCache();
        return cache.streamEvents(type, order);
    }
}
