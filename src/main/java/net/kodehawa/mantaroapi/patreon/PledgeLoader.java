package net.kodehawa.mantaroapi.patreon;

import com.patreon.PatreonAPI;
import com.patreon.models.Pledge;
import net.kodehawa.mantaroapi.utils.Config;
import net.kodehawa.mantaroapi.utils.Utils;
import org.slf4j.Logger;

import java.util.List;

public class PledgeLoader {
    public static void checkPledges(Logger logger, Config config) {
        if (config.checkOldPatrons()) {
            try {
                PatreonAPI patreonAPI = new PatreonAPI(config.getPatreonToken());
                List<Pledge> pledges = patreonAPI.fetchAllPledges("328369");
                System.out.println("Total pledges: " + pledges.size());

                for (Pledge pledge : pledges) {
                    String declinedSince = pledge.getDeclinedSince();
                    //logger.info("Pledge email {}: declined: {}, discordId {}", pledge.getPatron().getEmail(), declinedSince, discordId);

                    if (declinedSince == null) {
                        String discordId = pledge.getPatron().getDiscordId();

                        //come on guys, use integrations
                        if (discordId != null) {
                            double amountDollars = pledge.getAmountCents() / 100D;
                            logger.info("Processed pledge for {} for ${} (dollars)", discordId, amountDollars);
                            Utils.accessRedis(jedis -> {
                                if (jedis.hexists("donators", discordId))
                                    return null;

                                return jedis.hset("donators", discordId, String.valueOf(amountDollars));
                            });
                        }
                    } else {
                        Utils.accessRedis(jedis -> {
                            String discordId = pledge.getPatron().getDiscordId();

                            if(discordId != null && jedis.hexists("donators", discordId)) {
                                return jedis.hdel("donators", discordId);
                            }

                            //Placeholder.
                            return null;
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
