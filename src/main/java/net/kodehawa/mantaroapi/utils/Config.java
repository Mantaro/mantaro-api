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

public class Config {
    private String patreonSecret;
    private int port;
    private String patreonToken;
    private boolean checkOldPatrons;
    private String auth;
    private String userAgent;
    private boolean constantCheck;
    private int constantCheckDelay;

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPatreonSecret() {
        return patreonSecret;
    }

    public void setPatreonSecret(String patreonSecret) {
        this.patreonSecret = patreonSecret;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPatreonToken() {
        return patreonToken;
    }

    public void setPatreonToken(String patreonToken) {
        this.patreonToken = patreonToken;
    }

    public boolean checkOldPatrons() {
        return checkOldPatrons;
    }

    public void setCheckOldPatrons(boolean checkOldPatrons) {
        this.checkOldPatrons = checkOldPatrons;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public boolean isConstantCheck() {
        return constantCheck;
    }

    public void setConstantCheck(boolean constantCheck) {
        this.constantCheck = constantCheck;
    }

    public int getConstantCheckDelay() {
        return constantCheckDelay;
    }

    public void setConstantCheckDelay(int constantCheckDelay) {
        this.constantCheckDelay = constantCheckDelay;
    }
}
