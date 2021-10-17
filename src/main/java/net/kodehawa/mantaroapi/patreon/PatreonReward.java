package net.kodehawa.mantaroapi.patreon;

public enum PatreonReward {
    NONE(-100),
    SUPPORTER(1487355),
    FRIEND(1487342),
    PATREON_BOT(1700160),
    MILESTONER(1487346),
    SERVER_SUPPORTER(1487350),
    AWOOSOME(1670005),
    FUNDER(1670076),
    BUT_WHY(1669990);

    private final long id;
    PatreonReward(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static PatreonReward fromId(long id) {
        for (PatreonReward pr : values()) {
            if (pr.getId() == id) {
                return pr;
            }
        }

        return null;
    }
}
