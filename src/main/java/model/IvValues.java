package model;

import java.text.DecimalFormat;

public class IvValues {
    private final int attackIV;
    private final int defenseIV;
    private final int staminaIV;
    private static final DecimalFormat df2 = new DecimalFormat("##.##");
    private final int IvPercentage;

    public IvValues(int attackIV, int defenseIV, int staminaIV) {
        this.attackIV = attackIV;
        this.defenseIV = defenseIV;
        this.staminaIV = staminaIV;

        // Round the result to the nearest whole value and store as int. We don't talk about IV percentages as
        // 88.89 or 93.33, we say 89 or 93.
        IvPercentage = (int) Math.round((((double) (attackIV + defenseIV + staminaIV)) / 45) * 100);
    }

    public int getIvPercentage() {
        return IvPercentage;
    }

    public int getAttackIV() {
        return attackIV;
    }

    public int getDefenseIV() {
        return defenseIV;
    }

    public int getStaminaIV() {
        return staminaIV;
    }
}