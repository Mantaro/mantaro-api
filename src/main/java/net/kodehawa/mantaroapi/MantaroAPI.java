/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantaroapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.patreon.PatreonAPI;
import com.patreon.models.Pledge;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import spark.Spark;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class MantaroAPI {
    private final Logger logger = LoggerFactory.getLogger(MantaroAPI.class);
    private final String version = "2.0.1";
    private final List<PokemonData> pokemon = new ArrayList<>();
    private final List<String> splashes = new ArrayList<>();
    private final Random r = new Random();
    private final JSONObject hush;
    private final JsonParser parser = new JsonParser();
    private String patreonSecret;
    private int port;
    private String patreonToken;
    private boolean checkOldPatrons;
    private String auth;

    private final JedisPoolConfig poolConfig = buildPoolConfig();
    private JedisPool jedisPool = new JedisPool(poolConfig, "localhost");
    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    public static void main(String[] args) {
        try {
            new MantaroAPI();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private MantaroAPI() throws Exception {
        logger.info("\n" +
                "||        ______ ___  ___            _                 ______       _    ______        ||\n" +
                "||       / / / / |  \\/  |           | |                | ___ \\     | |   \\ \\ \\ \\       ||\n" +
                "||      / / / /  | .  . | __ _ _ __ | |_ __ _ _ __ ___ | |_/ / ___ | |_   \\ \\ \\ \\      ||\n" +
                "||     < < < <   | |\\/| |/ _` | '_ \\| __/ _` | '__/ _ \\| ___ \\/ _ \\| __|   > > > >     ||\n" +
                "||      \\ \\ \\ \\  | |  | | (_| | | | | || (_| | | | (_) | |_/ / (_) | |_   / / / /      ||\n" +
                "||       \\_\\_\\_\\ \\_|  |_/\\__,_|_| |_|\\__\\__,_|_|  \\___/\\____/ \\___/ \\__| /_/_/_/       ||\n" +
                "\n" +
                ":: Mantaro API {} :: Made by Kodehawa ::\n", version);

        try {
            loadConfig();
        } catch (IOException e) {
            logger.error("An error occurred while loading the configuration file!", e);
            System.exit(100);
        }

        if (checkOldPatrons) {
            try {
                PatreonAPI patreonAPI = new PatreonAPI(patreonToken);
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
                            redis(jedis -> {
                                if (jedis.hexists("donators", discordId))
                                    return null;

                                return jedis.hset("donators", discordId, String.valueOf(amountDollars));
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        logger.info("Reading pokemon data << pokemon_data.txt");
        InputStream stream = getClass().getClassLoader().getResourceAsStream("pokemon_data.txt");
        List<String> pokemonLines = IOUtils.readLines(stream, Charset.forName("UTF-8"));
        for (String s : pokemonLines) {
            String[] data = s.replace("\r", "").split("`");
            String image = data[0];
            String[] names = Arrays.copyOfRange(data, 1, data.length);
            pokemon.add(new PokemonData(names[0], image, names));
        }

        logger.info("Reading hush data << hush.json");
        InputStream hushStream = getClass().getClassLoader().getResourceAsStream("hush.json");
        List<String> hushLines = IOUtils.readLines(hushStream, Charset.forName("UTF-8"));
        hush = new JSONObject(hushLines.stream().collect(Collectors.joining("")));

        logger.info("Reading splashes data << splashes.txt");
        InputStream splashesStream = getClass().getClassLoader().getResourceAsStream("splashes.txt");
        List<String> splashesLines = IOUtils.readLines(splashesStream, Charset.forName("UTF-8"));
        for (String s : splashesLines) {
            splashes.add(s.replace("\r", ""));
        }

        splashes.removeIf(s -> s == null || s.isEmpty());
        port(port);
        Spark.init();

        get("/mantaroapi/ping", (req, res) -> new JSONObject().put("status", "ok").put("version", version).toString());

        path("/mantaroapi/bot", () -> {
            //Spark why does this work like this but not without an argument, I'M LITERALLY GIVING YOU AN EMPTY STRING
            before("", (request, response) -> handleAuthentication(request.headers("Authorization"), request.headers("User-Agent")));
            before("/*", (request, response) -> handleAuthentication(request.headers("Authorization"), request.headers("User-Agent")));

            get("/pokemon", (req, res) -> {
                try {
                    logger.debug("Retrieving pokemon data << pokemon_data.txt");
                    PokemonData pokemonData = pokemon.get(r.nextInt(pokemon.size()));
                    String image = pokemonData.getUrl();
                    String[] names = pokemonData.getNames();
                    String name = pokemonData.getName();
                    return new JSONObject()
                            .put("name", name)
                            .put("names", names)
                            .put("image", image)
                            .toString();
                } catch (Exception e) {
                    return new JSONObject().put("error", e.getMessage()).toString();
                }
            });

            get("/splashes/random", (req, res) -> new JSONObject().put("splash", splashes.get(r.nextInt(splashes.size()))).toString());

            post("/patreon/check", (req, res) -> {
                JSONObject obj = new  JSONObject(req.body());
                String id = obj.getString("id");
                String placeholder = new JSONObject().put("active", false).put("amount", 0).toString();

                return redis(jedis -> {
                    try {
                        if(!jedis.hexists("donators", id)) {
                            return placeholder;
                        }

                        String amount = jedis.hget("donators", id);

                        return new JSONObject().put("active", true).put("amount", amount).toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                        halt(500);
                        return placeholder;
                    }
                });
            });

            post("/hush", (req, res) -> {
                JSONObject obj = new  JSONObject(req.body());
                String name = obj.getString("name");
                String type = obj.getString("type").toLowerCase();

                String answer;
                try {
                    answer = hush.getJSONObject(type).getString(name);
                } catch (JSONException e) {
                    answer = "NONE";
                }

                return new JSONObject().put("hush", answer);
            });

        });

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

            final String hmac = HmacUtils.hmacMd5Hex(patreonSecret, body);
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
                            // pledge created with cents
                            redis(jedis -> jedis.hset("donators", discordUserId, String.valueOf(pledgeAmountDollars)));
                            break;
                        case "pledges:update":
                            // pledge updated to cents
                            //just set it again
                            redis(jedis -> jedis.hset("donators", discordUserId, String.valueOf(pledgeAmountDollars)));
                            break;
                        case "pledges:delete":
                            redis(jedis -> jedis.hdel("donators", discordUserId));
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

    //bootleg af honestly
    private boolean handleAuthentication(String auth, String agent) {
        if(this.auth.equals(auth) && agent.contains("Mantaro")) {
            return true;
        }

        halt(403);
        return false;
    }

    private void loadConfig() throws IOException{
        logger.info("Loading configuration file << api.json");
        File config = new File("api.json");
        if(!config.exists()) {
            JSONObject obj = new JSONObject();
            obj.put("patreon_secret", "secret");
            obj.put("patreon_token", "token");
            obj.put("port", 5874);
            obj.put("check", true);
            obj.put("auth", "uuid");

            FileOutputStream fos = new FileOutputStream(config);
            ByteArrayInputStream bais = new ByteArrayInputStream(obj.toString(4).getBytes(Charset.defaultCharset()));
            byte[] buffer = new byte[1024];
            int read;
            while((read = bais.read(buffer)) != -1)
                fos.write(buffer, 0, read);
            fos.close();
            logger.error("Could not find config file at " + config.getAbsolutePath() + ", creating a new one...");
            logger.error("Generated new config file at " + config.getAbsolutePath() + ".");
            logger.error("Please, fill the file with valid properties.");
            System.exit(-1);
        }

        JSONObject obj; {
            FileInputStream fis = new FileInputStream(config);
            byte[] buffer = new byte[1024];
            int read;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((read = fis.read(buffer)) != -1)
                baos.write(buffer, 0, read);
            obj = new JSONObject(new String(baos.toByteArray(), Charset.defaultCharset()));
        }

        patreonSecret = obj.getString("patreon_secret");
        port = obj.getInt("port");
        patreonToken = obj.getString("patreon_token");
        checkOldPatrons = obj.getBoolean("check");
        auth = obj.getString("auth");
    }

    private <T> T redis(Function<Jedis, T> consumer) {
        try(Jedis jedis = jedisPool.getResource()) {
            logger.debug("Accessing redis instance");
            return consumer.apply(jedis);
        }
    }
}
