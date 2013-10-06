package org.grouplens.mooc.cbf;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.core.Transient;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.grouplens.mooc.cbf.dao.ItemTagDAO;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder for computing {@linkplain TFIDFModel TF-IDF models} from item tag data.  Each item is
 * represented by a normalized TF-IDF vector.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TFIDFModelBuilder implements Provider<TFIDFModel> {
    private final ItemTagDAO dao;

    /**
     * Construct a model builder.  The {@link Inject} annotation on this constructor tells LensKit
     * that it can be used to build the model builder.
     *
     * @param dao The item-tag DAO.  This is where the builder will get access to items and their
     *            tags.
     *            <p>{@link Transient} means that the provider promises that the DAO is no longer
     *            needed once the object is built (that is, the model will not contain a reference
     *            to the DAO).  This allows LensKit to configure your recommender components
     *            properly.  It's up to you to keep this promise.</p>
     */
    @Inject
    public TFIDFModelBuilder(@Transient ItemTagDAO dao) {
        this.dao = dao;
    }

    /**
     * This method is where the model should actually be computed.
     * @return The TF-IDF model (a model of item tag vectors).
     */
    @Override
    public TFIDFModel get() {
        // Build a map of tags to numeric IDs.  This lets you convert tags (which are strings)
        // into long IDs that you can use as keys in a tag vector.
        Map<String, Long> tagIds = buildTagIdMap();

        // Create a vector to accumulate document frequencies for the IDF computation
        MutableSparseVector docFreq = MutableSparseVector.create(tagIds.values());
        docFreq.fill(0);

        // We now proceed in 2 stages. First, we build a TF vector for each item.
        // While we do this, we also build the DF vector.
        // We will then apply the IDF to each TF vector and normalize it to a unit vector.

        // Create a map to store the item TF vectors.
        Map<Long,MutableSparseVector> itemVectors = Maps.newHashMap();

        // Create a work vector to accumulate each item's tag vector.
        // This vector will be re-used for each item.
        MutableSparseVector work = MutableSparseVector.create(tagIds.values());

        // Iterate over the items to compute each item's vector.
        LongSet items = dao.getItemIds();
        for (long item: items) {
            // Reset the work vector for this item's tags.
            work.clear();
            // Now the vector is empty (all keys are 'unset').

            // TODO Populate the work vector with the number of times each tag is applied to this item.
            List<String> tags =  dao.getItemTags(item);
            for(String tag: tags){
                // from tagIds we find out what is the long value for this tag
                long tagId = tagIds.get(tag);

                // check work to see if this tagId already exist.  If so, increment by 1.  Else init to 1.
                try{
                    work.set(tagId, work.get(tagId)+1);
                }catch(IllegalArgumentException e){
                    // init value to 1
                    work.set(tagId, 1);
                }
            }

            // TODO Increment the document frequency vector once for each unique tag on the item.
            // create another sparse vector to see if a tag already been applied to this item
            MutableSparseVector temp = MutableSparseVector.create(tagIds.values());

            for(String tag: tags){
                long tagId = tagIds.get(tag);
                try{
                    // we already saw this tag in this item.
                    temp.get(tagId);
                    continue;
                }catch(IllegalArgumentException e){
                    // first time we see this tag in this item.
                    temp.set(tagId, 1);
                    docFreq.set(tagId, docFreq.get(tagId)+1);
                }

            }


            // Save a shrunk copy of the vector (only storing tags that apply to this item) in
            // our map, we'll add IDF and normalize later.
            itemVectors.put(item, work.shrinkDomain());
            // work is ready to be reset and re-used for the next item
        }

        // Now we've seen all the items, so we have each item's TF vector and a global vector
        // of document frequencies.
        // Invert and log the document frequency.  We can do this in-place.
        for (VectorEntry e: docFreq.fast()) {
            // TODO Update this document frequency entry to be a log-IDF value
            // debug before
            //System.out.print("Before...");
            //System.out.print(e.getKey());
            //System.out.print(": ");
            //System.out.print(docFreq.get(e.getKey()));
            docFreq.set(e.getKey(), Math.log10(e.getValue()));
            //System.out.print("   After...");
            //System.out.print(e.getKey());
            //System.out.print(": ");
            //System.out.println(docFreq.get(e.getKey()));
        }

        // Now docFreq is a log-IDF vector.
        // So we can use it to apply IDF to each item vector to put it in the final model.
        // Create a map to store the final model data.
        Map<Long,SparseVector> modelData = Maps.newHashMap();
        for (Map.Entry<Long,MutableSparseVector> entry: itemVectors.entrySet()) {
            MutableSparseVector tv = entry.getValue(); // tv is the TF of each tag for this item
            // TODO Convert this vector to a TF-IDF vector
            for(VectorEntry v: tv.fast()){
                //System.out.print("Movie: ");
                //System.out.print(entry.getKey());
                //System.out.print(" Tag: ");
                //System.out.print(v.getKey());

                double tf = v.getValue();
                //System.out.print(" tf: ");
                //System.out.print(tf);


                double idf = docFreq.get(v.getKey());
                //System.out.print(" idf: ");
                //System.out.print(idf);

                tv.set(v.getKey(), tf*idf);
                //System.out.print(" tf*idf ");
                //System.out.println(tv.get(v.getKey()));
            }
            // tv is now the tf*idf vector

            // TODO Normalize the TF-IDF vector to be a unit vector
            // HINT The method tv.norm() will give you the Euclidian length of the vector
            double len = tv.norm();
            System.out.print("Length: ");
            System.out.println(len);
            for(VectorEntry v: tv.fast()){
                tv.set(v.getKey(), v.getValue()/len);
            }
            
            // Store a frozen (immutable) version of the vector in the model data.
            modelData.put(entry.getKey(), tv.freeze());
        }

        // we technically don't need the IDF vector anymore, so long as we have no new tags
        return new TFIDFModel(tagIds, modelData);
    }

    /**
     * Build a mapping of tags to numeric IDs.
     *
     * @return A mapping from tags to IDs.
     */
    private Map<String,Long> buildTagIdMap() {
        // Get the universe of all tags
        Set<String> tags = dao.getTagVocabulary();
        // Allocate our new tag map
        Map<String,Long> tagIds = Maps.newHashMap();

        for (String tag: tags) {
            // Map each tag to a new number.
            tagIds.put(tag, tagIds.size() + 1L);
        }
        return tagIds;
    }
}
