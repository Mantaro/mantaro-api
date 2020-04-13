package net.kodehawa.mantaroapi.entities;

public class AnimeData {
    private String name;
    private String url;

    public AnimeData(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
