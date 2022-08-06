package net.kodehawa.mantaroapi.patreon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.beans.ConstructorProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PatreonPledge {
    private double amount;
    private PatreonReward reward;
    private final boolean active;

    @ConstructorProperties({"amount", "active", "reward"})
    @JsonCreator
    public PatreonPledge(double amount, boolean active, PatreonReward reward) {
        this.amount = amount;
        this.reward = reward;
        this.active = active;
    }

    public boolean isActive() {
        return active;
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
