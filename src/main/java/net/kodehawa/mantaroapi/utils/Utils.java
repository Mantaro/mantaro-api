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

package net.kodehawa.mantaroapi.utils;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.*;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.Function;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final JedisPoolConfig poolConfig = buildPoolConfig();
    private static final JedisPool jedisPool = new JedisPool(poolConfig, "redis://localhost:6379");
    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    //Load the config from file.
    public static Config loadConfig() throws IOException {
        Config cfg = new Config();
        logger.info("Loading configuration file << api.json");
        File config = new File("api.json");
        if(!config.exists()) {
            JSONObject obj = new JSONObject();
            obj.put("patreon_secret", "secret");
            obj.put("patreon_token", "token");
            obj.put("port", 5874);
            obj.put("check", true);
            obj.put("auth", "uuid");
            obj.put("user_agent", "agent");
            obj.put("constant_check", false);
            obj.put("constant_check_delay_days", 1);

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
            try (FileInputStream fis = new FileInputStream(config)) {
                byte[] buffer = new byte[1024];
                int read;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while((read = fis.read(buffer)) != -1)
                    baos.write(buffer, 0, read);
                obj = new JSONObject(baos.toString(Charset.defaultCharset()));
            }
        }

        cfg.setPatreonSecret(obj.getString("patreon_secret"));
        cfg.setPort(obj.getInt("port"));
        cfg.setPatreonToken(obj.getString("patreon_token"));
        cfg.setCheckOldPatrons(obj.getBoolean("check"));
        cfg.setConstantCheck(obj.getBoolean("constant_check"));
        cfg.setConstantCheckDelay(obj.getInt("constant_check_delay_days"));
        cfg.setAuth(obj.getString("auth"));
        cfg.setUserAgent(obj.getString("user_agent"));

        return cfg;
    }

    public static <T> T accessRedis(Function<Jedis, T> consumer) {
        try(Jedis jedis = jedisPool.getResource()) {
            logger.debug("Accessing redis instance");
            return consumer.apply(jedis);
        }
    }

}
