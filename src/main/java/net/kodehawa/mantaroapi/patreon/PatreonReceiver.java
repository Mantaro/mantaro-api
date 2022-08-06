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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kodehawa.mantaroapi.utils.Config;
import net.kodehawa.mantaroapi.utils.Utils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.security.MessageDigest;
import java.util.Iterator;

import static spark.Spark.halt;
import static spark.Spark.post;

// This is painful...
public class PatreonReceiver {
    public PatreonReceiver(Logger logger, Config config) {
        //Handle patreon webhooks.
        post("/mantaroapi/patreon", (req, res) -> {
            final String body = req.body();
            final String signature = req.headers("X-Patreon-Signature");

            if(signature == null) {
                logger.warn("Patreon webhook had no signature! Probably fake or invalid request.");
                logger.warn("Patreon webhook data: {}", req.body());
                halt(401);
                return "";
            }

            final String hmac = new HmacUtils(HmacAlgorithms.HMAC_MD5, config.getPatreonSecret()).hmacHex(body);
            if(!MessageDigest.isEqual(hmac.getBytes(), signature.getBytes())) {
                logger.warn("Patreon webhook signature was invalid! Probably fake or invalid request.");
                halt(401);
                return "";
            } else {
                logger.info("Accepted Patreon signed data.");
                logger.debug("Accepted Patreon signed data <- {}", body);
            }

            // Events are pledges:{create,update,delete}
            final String patreonEvent = req.headers("X-Patreon-Event");
            final JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            try {
                //what the fuck
                final String patronId = json
                        .get("data").getAsJsonObject()
                        .get("relationships").getAsJsonObject()
                        .get("patron").getAsJsonObject()
                        .get("data").getAsJsonObject()
                        .get("id").getAsString();

                final Iterator<JsonElement> included = json
                        .get("included").getAsJsonArray()
                        .iterator();

                JsonObject patronObject = null;
                while(included.hasNext()) {
                    final JsonElement next = included.next();
                    final JsonObject includedObject = next.getAsJsonObject();
                    if(includedObject.get("id").getAsString().equals(patronId)) {
                        patronObject = includedObject;
                        break;
                    }
                }

                if(patronObject != null) {
                    final long pledgeAmountCents = json
                            .get("data").getAsJsonObject()
                            .get("attributes").getAsJsonObject()
                            .get("amount_cents").getAsLong();

                    final JsonObject socialConnection = patronObject
                            .get("attributes").getAsJsonObject()
                            .get("social_connections").getAsJsonObject()
                            .get("discord").getAsJsonObject();

                    if (socialConnection == null) {
                        logger.info("Received Patreon event {}, but without a Discord ID. Cannot process.", patreonEvent);
                        return "{\"status\":\"ok\"}";
                    }

                    final String discordUserId = socialConnection.get("user_id").getAsString();
                    double pledgeAmountDollars = pledgeAmountCents / 100D;
                    logger.info("Received Patreon event '{}' for Discord ID '{}' with amount ${}", patreonEvent,
                            discordUserId, String.format("%.2f", pledgeAmountDollars)
                    );

                    switch(patreonEvent) {
                        case "pledges:create":
                        case "pledges:update":
                            //what the fuck part 2
                            final String pledgeReward = json
                                    .get("data").getAsJsonObject()
                                    .get("relationships").getAsJsonObject()
                                    .get("reward").getAsJsonObject()
                                    .get("data").getAsJsonObject()
                                    .get("id").getAsString();

                            // pledge updated / created, we need to set it on both cases.
                            if (pledgeReward == null) {
                                logger.error("Unknown reward. Can't find it?");
                                return "{\"status\":\"ok\"}";
                            }

                            long tier = Long.parseLong(pledgeReward);
                            // Why does this exist?
                            if (tier == 0 || tier == -1) {
                                logger.error("Unknown tier reward for {} (tier == 0 | tier == -1)", discordUserId);
                                return "{\"status\":\"ok\"}";
                            }

                            var patreonReward = PatreonReward.fromId(tier);
                            if (patreonReward == null) {
                                logger.error("Unknown reward? Can't convert to enum! {}", tier);
                                return "{\"status\":\"ok\"}";
                            }

                            var pledgeObject = new PatreonPledge(pledgeAmountDollars, true, patreonReward);
                            Utils.accessRedis(jedis ->
                                    jedis.hset("donators", discordUserId, new JSONObject(pledgeObject).toString())
                            );

                            logger.info("Added pledge data: Discord ID: {}, Pledge tier: {}", discordUserId, patreonReward);
                            break;
                        case "pledges:delete":
                            Utils.accessRedis(jedis ->
                                    jedis.hdel("donators", discordUserId)
                            );

                            break;
                        default:
                            logger.info("Got unknown patreon event {} for Discord ID: {}", patreonEvent, discordUserId);
                            break;
                    }
                } else {
                    logger.info("Null patron object?");
                }
            } catch(final Exception e) {
                logger.error("(!!!) Failed to process data, dumping <- {}", body);
                e.printStackTrace();
            }

            return "{\"status\":\"ok\"}";
        });
    }
}
