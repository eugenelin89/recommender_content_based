package org.grouplens.mooc.cbf;

import org.grouplens.lenskit.ItemRecommender;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.core.LenskitRecommender;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.dao.ItemDAO;
import org.grouplens.lenskit.data.dao.UserDAO;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.mooc.cbf.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Simple hello-world program.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class CBFMain {
    private static final Logger logger = LoggerFactory.getLogger(CBFMain.class);

    public static void main(String[] args) throws RecommenderBuildException {
        LenskitConfiguration config = configureRecommender();

        logger.info("building recommender");
        Recommender rec = LenskitRecommender.build(config);

        if (args.length == 0) {
            logger.error("No users specified; provide user IDs as command line arguments");
        }

        // we automatically get a useful recommender since we have a scorer
        ItemRecommender irec = rec.getItemRecommender();
        assert irec != null;
        try {
            // Generate 5 recommendations for each user
            for (String user: args) {
                long uid;
                try {
                    uid = Long.parseLong(user);
                } catch (NumberFormatException e) {
                    logger.error("cannot parse user {}", user);
                    continue;
                }
                logger.info("searching for recommendations for user {}", user);
                List<ScoredId> recs = irec.recommend(uid, 5);
                if (recs.isEmpty()) {
                    logger.warn("no recommendations for user {}, do they exist?", uid);
                }
                System.out.format("recommendations for user %d:\n", uid);
                for (ScoredId id: recs) {
                    System.out.format("  %d: %.4f\n", id.getId(), id.getScore());
                }
            }
        } catch (UnsupportedOperationException e) {
            if (e.getMessage().equals("stub implementation")) {
                System.out.println("Congratulations, the stub builds and runs!");
            }
        }
    }

    /**
     * Create the LensKit recommender configuration.
     * @return The LensKit recommender configuration.
     */
    // LensKit configuration API generates some unchecked warnings, turn them off
    @SuppressWarnings("unchecked")
    private static LenskitConfiguration configureRecommender() {
        LenskitConfiguration config = new LenskitConfiguration();
        // configure the rating data source
        config.bind(EventDAO.class)
              .to(MOOCRatingDAO.class);
        config.set(RatingFile.class)
              .to(new File("data/ratings.csv"));

        // use custom item and user DAOs
        // specify item DAO implementation with tags
        config.bind(ItemDAO.class)
              .to(CSVItemTagDAO.class);
        // specify tag file
        config.set(TagFile.class)
              .to(new File("data/movie-tags.csv"));
        // and title file
        config.set(TitleFile.class)
              .to(new File("data/movie-titles.csv"));

        // our user DAO can look up by user name
        config.bind(UserDAO.class)
              .to(MOOCUserDAO.class);
        config.set(UserFile.class)
              .to(new File("data/users.csv"));

        // use the TF-IDF scorer you will implement to score items
        config.bind(ItemScorer.class)
              .to(TFIDFItemScorer.class);
        return config;
    }
}