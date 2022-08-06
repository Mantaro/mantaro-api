/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantaroapi.patreon;

import com.patreon.PatreonAPI;
import com.patreon.models.Pledge;
import com.patreon.models.Reward;
import net.kodehawa.mantaroapi.utils.Config;
import net.kodehawa.mantaroapi.utils.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PledgeLoader {
    public static void checkPledges(Logger logger, Config config, boolean force) {
        if (config.checkOldPatrons() || force) {
            try {
                logger.info("Checking pledges...");
                PatreonAPI patreonAPI = new PatreonAPI(config.getPatreonToken());

                // This is for debugging reasons. Patreon's API is as painful as it gets.
                var rewards = patreonAPI.fetchCampaigns().get().get(0).getRewards();
                logger.info("Printing current known rewards:");
                for (Reward r : rewards) {
                    System.out.println(r.getId() + " " + r.getTitle());
                }

                List<Pledge> pledges = patreonAPI.fetchAllPledges("328369");
                AtomicInteger active = new AtomicInteger();
                for (Pledge pledge : pledges) {
                    String declinedSince = pledge.getDeclinedSince();

                    if (declinedSince == null) {
                        String discordId = pledge.getPatron().getDiscordId();

                        //come on guys, use integrations
                        if (discordId != null) {
                            double amount = pledge.getAmountCents() / 100D;
                            var reward = pledge.getReward();
                            if (reward == null) {
                                logger.error("(!!) Unknown tier reward for {}", discordId);
                                return;
                            }

                            long tier = Long.parseLong(reward.getId());
                            if (tier == 0 || tier == -1) {
                                logger.error("(!!) Unknown tier reward for {} (tier == 0 | tier == -1)", discordId);
                                return;
                            }

                            var tierName = reward.getTitle();
                            var patreonReward = PatreonReward.fromId(tier);
                            logger.info("!! Processed pledge for {} for ${} -- Tier: {} ({} / {})", discordId, amount, tier, patreonReward, tierName);
                            Utils.accessRedis(jedis -> {
                                if (jedis.hget("donators", discordId) == null) {
                                    logger.info("(!!) Processed new: Pledge email {}: declined: {}, discordId {}",
                                            pledge.getPatron().getEmail(), declinedSince, discordId
                                    );
                                }

                                if (patreonReward == null) {
                                    logger.error("Unknown reward? Can't convert to enum! {}", tier);
                                    return null;
                                }

                                var pledgeObject = new PatreonPledge(amount, true, patreonReward);
                                active.getAndIncrement();
                                return jedis.hset("donators", discordId, new JSONObject(pledgeObject).toString());
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

                logger.info("(!!!) Updated all pledges! Total active: {}", active.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
