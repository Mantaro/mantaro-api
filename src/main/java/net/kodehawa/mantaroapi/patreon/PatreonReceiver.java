package net.kodehawa.mantaroapi.patreon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kodehawa.mantaroapi.utils.Config;
import net.kodehawa.mantaroapi.utils.Utils;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;

import java.security.MessageDigest;
import java.util.Iterator;

import static spark.Spark.*;

public class PatreonReceiver {
    private final JsonParser parser = new JsonParser();

    public PatreonReceiver(Logger logger, Config config) {
        //Handle patreon webhooks.
        post("/mantaroapi/patreon", (req, res) -> {
            final String body = req.body();
            final String signature = req.headers("X-Patreon-Signature");

            if(signature == null) {
                logger.warn("Patreon webhook had no signature! Probably fake / invalid request.");
                logger.warn("Patreon webhook data: " + req.body());
                halt(401);
                return "";
            }

            final String hmac = HmacUtils.hmacMd5Hex(config.getPatreonSecret(), body);
            if(!MessageDigest.isEqual(hmac.getBytes(), signature.getBytes())) {
                logger.warn("Patreon webhook signature was invalid! Probably fake / invalid request.");
                halt(401);
                return "";
            } else {
                logger.info("Accepted Patreon signed data");
                logger.debug("Accepted Patreon signed data <- {}", body);
            }

            // Events are pledges:{create,update,delete}
            final String patreonEvent = req.headers("X-Patreon-Event");
            final JsonObject json = parser.parse(body).getAsJsonObject();

            //what the fuck
            final String patronId = json.get("data").getAsJsonObject().get("relationships").getAsJsonObject().get("patron")
                    .getAsJsonObject().get("data").getAsJsonObject().get("id").getAsString();

            final Iterator<JsonElement> included = json.get("included").getAsJsonArray().iterator();
            final long pledgeAmountCents = json.get("data").getAsJsonObject().get("attributes").getAsJsonObject()
                    .get("amount_cents").getAsLong();

            JsonObject patronObject = null;

            while(included.hasNext()) {
                final JsonElement next = included.next();
                final JsonObject includedObject = next.getAsJsonObject();
                if(includedObject.get("id").getAsString().equals(patronId)) {
                    patronObject = includedObject;
                    break;
                }
            }
            try {
                if(patronObject != null) {
                    final String discordUserId = patronObject.get("attributes").getAsJsonObject().get("social_connections")
                            .getAsJsonObject().get("discord").getAsJsonObject().get("user_id").getAsString();
                    double pledgeAmountDollars = pledgeAmountCents / 100D;

                    logger.info("Recv. Patreon event '{}' for Discord user '{}' with amount ${}", patreonEvent,
                            discordUserId, String.format("%.2f", pledgeAmountDollars));
                    switch(patreonEvent) {
                        case "pledges:create":
                        case "pledges:update":
                            // pledge updated / created, we need to set it on both cases.
                            Utils.accessRedis(jedis -> jedis.hset("donators", discordUserId, String.valueOf(pledgeAmountDollars)));
                            break;
                        case "pledges:delete":
                            Utils.accessRedis(jedis -> jedis.hdel("donators", discordUserId));
                            break;
                        default:
                            logger.info("Got unknown patreon event for Discord user: " + patreonEvent);
                            break;
                    }
                } else {
                    logger.info("Null patron object?");
                }
            } catch(final Exception e) {
                e.printStackTrace();
            }

            return "{\"status\":\"ok\"}";
        });
    }
}
