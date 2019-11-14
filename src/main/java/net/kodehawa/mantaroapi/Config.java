package net.kodehawa.mantaroapi;

public class Config {
    private String patreonSecret;
    private int port;
    private String patreonToken;
    private boolean checkOldPatrons;
    private String auth;
    private String userAgent;

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
}
