package org.grouplens.mooc.cbf.dao;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.dao.DataAccessException;
import org.grouplens.lenskit.util.DelimitedTextCursor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class MOOCItemDAO implements ItemTitleDAO {
    private final File titleFile;
    private transient volatile Long2ObjectMap<String> titleCache;

    @Inject
    public MOOCItemDAO(@TitleFile File file) {
        titleFile = file;
    }

    private void ensureTitleCache() {
        if (titleCache == null) {
            synchronized (this) {
                if (titleCache == null) {
                    titleCache = loadTitleCache();
                }
            }
        }
    }

    private Long2ObjectMap<String> loadTitleCache() {
        Long2ObjectMap<String> cache = new Long2ObjectOpenHashMap<String>();
        Cursor<String[]> lines = null;
        try {
            lines = new DelimitedTextCursor(titleFile, ",");
        } catch (FileNotFoundException e) {
            throw new DataAccessException("cannot open file", e);
        }
        try {
            for (String[] line: lines) {
                long mid = Long.parseLong(line[0]);
                cache.put(mid, line[1]);
            }
        } finally {
            lines.close();
        }
        return cache;
    }

    @Override
    public LongSet getItemIds() {
        ensureTitleCache();
        return LongSets.unmodifiable(titleCache.keySet());
    }

    @Override
    public String getItemTitle(long item) {
        return titleCache.get(item);
    }
}
