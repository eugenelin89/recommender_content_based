package org.grouplens.mooc.cbf.dao;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.grouplens.lenskit.collections.LongUtils;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.dao.DataAccessException;
import org.grouplens.lenskit.util.DelimitedTextCursor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class MOOCUserDAO implements UserNameDAO {
    private final File userFile;
    private transient volatile Object2LongMap<String> nameCache;
    private transient volatile LongSet userIds;

    @Inject
    public MOOCUserDAO(@UserFile File file) {
        userFile = file;
    }

    private void ensureNameCache() {
        if (nameCache == null) {
            synchronized (this) {
                if (nameCache == null) {
                    nameCache = loadNameCache();
                    userIds = LongUtils.packedSet(nameCache.values());
                }
            }
        }
    }

    private Object2LongMap<String> loadNameCache() {
        Object2LongMap<String> cache = new Object2LongOpenHashMap<String>();
        // make the cache return -1 for missing users
        cache.defaultReturnValue(-1);
        Cursor<String[]> lines = null;
        try {
            lines = new DelimitedTextCursor(userFile, ",");
        } catch (FileNotFoundException e) {
            throw new DataAccessException("cannot open file", e);
        }
        try {
            for (String[] line: lines) {
                long uid = Long.parseLong(line[0]);
                cache.put(line[1], uid);
            }
        } finally {
            lines.close();
        }
        return cache;
    }

    @Override
    public LongSet getUserIds() {
        ensureNameCache();
        return userIds;
    }

    @Override
    public long getUserByName(String name) {
        ensureNameCache();
        return nameCache.get(name);
    }
}
