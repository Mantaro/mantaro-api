package net.kodehawa.mantaroapi.patreon;

public class PatreonPledge {
    private double amount;
    private PatreonReward reward;

    public PatreonPledge(double amount, PatreonReward reward) {
        this.amount = amount;
        this.reward = reward;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PatreonReward getReward() {
        return reward;
    }

    public void setReward(PatreonReward reward) {
        this.reward = reward;
    }

}
