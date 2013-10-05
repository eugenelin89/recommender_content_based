package org.grouplens.mooc.cbf.dao;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.dao.DataAccessException;
import org.grouplens.lenskit.util.DelimitedTextCursor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class CSVItemTagDAO extends MOOCItemDAO implements ItemTagDAO {
    private final File tagFile;
    private transient volatile Long2ObjectMap<List<String>> tagCache;
    private transient volatile Set<String> vocabCache;

    @Inject
    public CSVItemTagDAO(@TitleFile File titles, @TagFile File tags) {
        super(titles);
        tagFile = tags;
    }

    private void ensureTagCache() {
        if (tagCache == null) {
            synchronized (this) {
                if (tagCache == null) {
                    tagCache = new Long2ObjectOpenHashMap<List<String>>();
                    ImmutableSet.Builder<String> vocabBuilder = ImmutableSet.builder();
                    Cursor<String[]> lines = null;
                    try {
                        lines = new DelimitedTextCursor(tagFile, ",");
                    } catch (FileNotFoundException e) {
                        throw new DataAccessException("cannot open file", e);
                    }
                    try {
                        for (String[] line: lines) {
                            long mid = Long.parseLong(line[0]);
                            List<String> tags = tagCache.get(mid);
                            if (tags == null) {
                                tags = new ArrayList<String>();
                                tagCache.put(mid, tags);
                            }
                            tags.add(line[1]);
                            vocabBuilder.add(line[1]);
                        }
                    } finally {
                        lines.close();
                    }
                    vocabCache = vocabBuilder.build();
                }
            }
        }
    }

    @Override
    public List<String> getItemTags(long item) {
        ensureTagCache();
        List<String> tags = tagCache.get(item);
        if (tags != null) {
            return Collections.unmodifiableList(tags);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getTagVocabulary() {
        ensureTagCache();
        return vocabCache;
    }
}
