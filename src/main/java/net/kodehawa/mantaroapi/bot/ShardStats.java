package net.kodehawa.mantaroapi.bot;

public class ShardStats {
    private int guilds;
    private int users;
    private int ping;
    private int queue;
    private int eventTime;

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public int getQueue() {
        return queue;
    }

    public void setQueue(int queue) {
        this.queue = queue;
    }

    public int getEventTime() {
        return eventTime;
    }

    public void setEventTime(int eventTime) {
        this.eventTime = eventTime;
    }

    public int getGuilds() {
        return guilds;
    }

    public void setGuilds(int guilds) {
        this.guilds = guilds;
    }

    public int getUsers() {
        return users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

}
