package sample;

import java.text.DecimalFormat;

public class IvValues {
    private final double attackIV;
    private final double defenseIV;
    private final double staminaIV;
    private static final DecimalFormat df2 = new DecimalFormat("##.##");
    private final double IvPercentage;

    public IvValues(double attackIV, double defenseIV, double staminaIV) {
        this.attackIV = attackIV;
        this.defenseIV = defenseIV;
        this.staminaIV = staminaIV;
        IvPercentage = Double.parseDouble(df2.format((((attackIV + defenseIV + staminaIV) / 45) * 100)));
    }

    public double getIvPercentage() {
        return IvPercentage;
    }

    public double getAttackIV() {
        return attackIV;
    }

    public double getDefenseIV() {
        return defenseIV;
    }

    public double getStaminaIV() {
        return staminaIV;
    }
}