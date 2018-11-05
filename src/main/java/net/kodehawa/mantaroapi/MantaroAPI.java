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

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class MantaroAPI {
    private final Logger logger = LoggerFactory.getLogger(MantaroAPI.class);
    private final int version = 1;
    private final List<PokemonData> pokemon = new ArrayList<>();
    private final List<String> splashes = new ArrayList<>();
    private final Random r = new Random();
    private final JSONObject hush;

    public static void main(String[] args) {
        try {
            new MantaroAPI();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private MantaroAPI() throws Exception {
        logger.info("Starting up Mantaro API...");
        logger.info("Reading data from filesystem");
        InputStream stream = getClass().getClassLoader().getResourceAsStream("pokemon_data.txt");
        List<String> pokemonLines = IOUtils.readLines(stream, Charset.forName("UTF-8"));

        //---------------
        logger.info("Reading pokemon data");
        for (String s : pokemonLines) {
            String[] data = s.replace("\r", "").split("`");
            String image = data[0];
            String[] names = Arrays.copyOfRange(data, 1, data.length);
            pokemon.add(new PokemonData(names[0], image, names));
        }

        //---------------
        logger.info("Reading hush data");
        InputStream hushStream = getClass().getClassLoader().getResourceAsStream("hush.json");
        List<String> hushLines = IOUtils.readLines(hushStream, Charset.forName("UTF-8"));
        hush = new JSONObject(hushLines.stream().collect(Collectors.joining("")));

        //---------------
        logger.info("Reading splashes data");
        InputStream splashesStream = getClass().getClassLoader().getResourceAsStream("splashes.txt");
        List<String> splashesLines = IOUtils.readLines(splashesStream, Charset.forName("UTF-8"));
        for (String s : splashesLines) {
            splashes.add(s.replace("\r", ""));
        }

        splashes.removeIf(s -> s == null || s.isEmpty());

        //Set port.
        port(5874);

        //Init webserver.
        Spark.init();
        get("/mantaroapi/ping", (req, res) -> "pong! (V:" + version + ")");

        get("/mantaroapi/pokemon", (req, res) -> {
            try {
                logger.debug("Retrieving pokemon data.");
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

        get("/mantaroapi/splashes/random", (req, res) -> new JSONObject().put("splash", splashes.get(r.nextInt(splashes.size()))).toString());

        post("/mantaroapi/hush", (req, res) -> {
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

        logger.info("Mantaro API 1.0 up.");
    }
}
