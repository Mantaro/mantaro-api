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

import net.kodehawa.mantaroapi.bot.ShardStats;
import net.kodehawa.mantaroapi.bot.ShardType;
import net.kodehawa.mantaroapi.patreon.PatreonReceiver;
import net.kodehawa.mantaroapi.patreon.PledgeLoader;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

import static spark.Spark.*;

public class MantaroAPI {
    private final Logger logger = LoggerFactory.getLogger(MantaroAPI.class);
    private final String version = "2.0.2";
    private final List<PokemonData> pokemon = new ArrayList<>();
    private final List<AnimeData> anime = new ArrayList<>();
    private final List<String> splashes = new ArrayList<>();
    private final Random r = new Random();
    private JSONObject hush;
    private Map<Integer, ShardStats> shardStatsMap = new HashMap<>();
    private Config config;

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
            config = Utils.loadConfig();
        } catch (IOException e) {
            logger.error("An error occurred while loading the configuration file!", e);
            System.exit(100);
        }

        Executors.newSingleThreadExecutor().submit(() -> {
            if (config.checkOldPatrons()) {
                PledgeLoader.checkPledges(logger, config);
            }
        });

        readFiles();
        port(config.getPort());
        Spark.init();

        //Receive webhooks from Patreon.
        new PatreonReceiver(logger, config);

        get("/mantaroapi/ping", (req, res) -> new JSONObject().put("status", "ok").put("version", version).toString());

        path("/mantaroapi/bot", () -> {
            //Spark why does this work like this but not without an argument, I'M LITERALLY GIVING YOU AN EMPTY STRING
            before("", (request, response) -> handleAuthentication(request.headers("Authorization")));
            before("/*", (request, response) -> handleAuthentication(request.headers("Authorization")));

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

            get("/character", (req, res) -> {
                try {
                    logger.debug("Retrieving anime data << anime_data.txt");
                    AnimeData animeData = anime.get(r.nextInt(anime.size()));
                    String name = animeData.getName();
                    String image = animeData.getUrl();

                    return new JSONObject()
                            .put("name", name)
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
                String placeholder = new JSONObject().put("active", false).put("amount", "0").toString();

                return Utils.accessRedis(jedis -> {
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
                    answer = hush.getJSONObject(type).getString(name.replace(" ", "_"));
                } catch (JSONException e) {
                    answer = "NONE";
                }

                return new JSONObject().put("hush", answer);
            });

            post("/stats/shards", (req, res) -> {
                JSONObject object = new JSONObject(req.body());
                JSONArray shards = object.getJSONArray("shards");

                for(Object shard : shards) {
                    JSONObject shardObject = (JSONObject) shard;
                    ShardStats stats = new ShardStats();
                    stats.setType(ShardType.valueOf(shardObject.getString("type")));
                    stats.setGuilds(shardObject.getInt("guilds"));
                    stats.setUsers(shardObject.getInt("users"));
                    stats.setPing(shardObject.getInt("ping"));
                    stats.setEventTime(shardObject.getInt("evt_time"));
                    stats.setQueue(shardObject.getInt("queue"));

                    shardStatsMap.put(object.getInt("id"), stats);
                }

                return "{\"status\":\"ok\"}";
            });

            get("/stats/shardinfo", (req, res) -> new JSONObject(shardStatsMap));
        });
    }

    //bootleg af honestly
    private void handleAuthentication(String auth) {
        if(!config.getAuth().equals(auth))
            halt(403);
    }

    public void readFiles() throws IOException {
        logger.info("Reading pokemon data << pokemon_data.txt");
        InputStream stream = getClass().getClassLoader().getResourceAsStream("pokemon_data.txt");
        if(stream != null) {
            List<String> pokemonLines = IOUtils.readLines(stream, StandardCharsets.UTF_8);
            for (String s : pokemonLines) {
                String[] data = s.replace("\r", "").split("`");
                String image = data[0];
                String[] names = Arrays.copyOfRange(data, 1, data.length);
                pokemon.add(new PokemonData(names[0], image, names));
            }
        } else {
            logger.error("Error loading Pokemon data!");
        }

        logger.info("Reading anime data << anime_data.txt");
        InputStream animeStream = getClass().getClassLoader().getResourceAsStream("anime_data.txt");
        if(animeStream != null) {
            List<String> animeLines = IOUtils.readLines(animeStream, StandardCharsets.UTF_8);
            for (String s : animeLines) {
                String[] data = s.replace("\r", "").split(";");
                String name = data[0];
                String image = data[1];
                anime.add(new AnimeData(name, image));
            }
        } else {
            logger.error("Error loading anime data!");
        }

        logger.info("Reading hush data << hush.json");
        InputStream hushStream = getClass().getClassLoader().getResourceAsStream("hush.json");
        if(hushStream != null) {
            List<String> hushLines = IOUtils.readLines(hushStream, StandardCharsets.UTF_8);
            hush = new JSONObject(String.join("", hushLines));
        } else {
            logger.error("Error loading hush badges!");
        }

        logger.info("Reading splashes data << splashes.txt");
        InputStream splashesStream = getClass().getClassLoader().getResourceAsStream("splashes.txt");
        if(splashesStream != null) {
            List<String> splashesLines = IOUtils.readLines(splashesStream, StandardCharsets.UTF_8);
            for (String s : splashesLines) {
                splashes.add(s.replace("\r", ""));
            }

            splashes.removeIf(s -> s == null || s.isEmpty());
        } else {
            logger.error("Error loading splashes!");
        }
    }
}
