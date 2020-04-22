/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantaroapi.patreon;

import com.patreon.PatreonAPI;
import com.patreon.models.Pledge;
import net.kodehawa.mantaroapi.utils.Config;
import net.kodehawa.mantaroapi.utils.Utils;
import org.slf4j.Logger;

import java.util.List;

public class PledgeLoader {
    public static void checkPledges(Logger logger, Config config, boolean force) {
        if (config.checkOldPatrons() || force) {
            try {
                logger.info("Checking pledges...");
                PatreonAPI patreonAPI = new PatreonAPI(config.getPatreonToken());
                List<Pledge> pledges = patreonAPI.fetchAllPledges("328369");
                logger.info("Total pledges: " + pledges.size());

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
