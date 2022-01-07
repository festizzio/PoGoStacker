package model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.*;

public class Pokemon {

    // == field variables ==
    private final int baseAttack;
    private final int baseDefense;
    private final int baseStamina;
    private final int pokedexNumber;
    private String name;
    private int CP;
    private String ivValuesPerCp;
    private final List<Integer> possibleCPValues;
    private final int minCP;
    private final int maxCP;
    private final int stardustValue;
    private final Map<Integer, List<IvValues>> mapOfIvValues;
    private static final List<IvValues> ivList = calculateListOfIVs();

    // Deprecate soon, currently the file names in the sprites folder are all lowercase, but in the database, the first letter is capitalized.
    private String spriteFileName;

    public Pokemon(int pokedexNumber, String name, int baseAttack, int baseDefense, int baseStamina) {

        this.pokedexNumber = pokedexNumber;
        this.name = name;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseStamina = baseStamina;
        possibleCPValues = new ArrayList<>();
        mapOfIvValues = new HashMap<>();
        calculatePossibleCPValues();
        possibleCPValues.sort(Comparator.naturalOrder());
        this.minCP = possibleCPValues.get(0);
        this.maxCP = possibleCPValues.get(possibleCPValues.size() - 1);

        // == initialize list of evolved Pokemon - these are worth extra stardust ==
        final List<String> stage1EvoName = Arrays.asList("Graveler", "Rhydon", "Poliwhirl", "Monferno",
                "Combusken", "Porygon2", "Raichu", "Skiploom", "Loudred", "Umbreon", "Azumarill");
        final List<String> stage2EvoName = Arrays.asList("Venusaur", "Charizard");
        if(stage2EvoName.contains(name)) {
            stardustValue = 500;
        } else if(stage1EvoName.contains(name)) {
            stardustValue = 300;
        } else {
            stardustValue = 100;
        }

        spriteFileName = name.toLowerCase() + ".png";
    }

    // IV floor for research tasks is 10/10/10, and these values don't change between Pokemon.
    // No reason to call it every time you instantiate a new Pokemon object hence it being static.
    private static List<IvValues> calculateListOfIVs() {
        List<IvValues> ivValues = new ArrayList<>();
        for(int attackIV = 10; attackIV <= 15; attackIV++) {
            for (int defenseIV = 10; defenseIV <= 15; defenseIV++) {
                for (int staminaIV = 10; staminaIV <= 15; staminaIV++) {
                    ivValues.add(new IvValues(attackIV, defenseIV, staminaIV));
                }
            }
        }
        return ivValues;
    }

    // Calculate the list of possible CP values for this Pokemon based on level 15 with any of the IV values given.
    private void calculatePossibleCPValues() {
        int CP;
        List<IvValues> listOfIvValues;
        for(IvValues currentIVs : ivList) {

            CP = calculateCP(currentIVs.getAttackIV(), currentIVs.getDefenseIV(), currentIVs.getStaminaIV());
            listOfIvValues = new ArrayList<>();

            if(!mapOfIvValues.isEmpty()) {
                if(mapOfIvValues.containsKey(CP)) {
                    listOfIvValues = mapOfIvValues.get(CP);
                }
            }

            listOfIvValues.add(currentIVs);

            listOfIvValues.sort(Comparator.comparingDouble(IvValues::getIvPercentage));
            mapOfIvValues.put(CP, listOfIvValues);
        }
        fillPossibleCPValueList();
    }

    private int calculateCP(int attackIV, int defenseIV, int staminaIV) {
        // Formula to calculate the CP of a Pokemon based on its known IVs and level per gamepress.gg.
        // The arbitrary CP multiplier for level 15 (the level of all research rewards) is 0.51739395.
        // https://gamepress.gg/pokemongo/cp-multiplier
        return (int) ((baseAttack + attackIV) * Math.pow((baseDefense + defenseIV), 0.5) *
                Math.pow((baseStamina + staminaIV), 0.5) * Math.pow(0.51739395, 2)) / 10;
    }

    private void fillPossibleCPValueList(){
        possibleCPValues.addAll(mapOfIvValues.keySet());
        possibleCPValues.sort(Comparator.naturalOrder());
    }

    public String getName() {
        return this.name;
    }

    public int getStardustValue() {
        return stardustValue;
    }

    // For each CP, there are several different possibilities for IV percentages. This sets the range.
    private boolean calculateIvPercentagePerCP() {
        StringBuilder sb = new StringBuilder();
        List<IvValues> valuesPerCp;

        if(!mapOfIvValues.containsKey(CP)){
            System.out.println("Invalid CP value for this Pokemon");
            return false;
        } else {
            valuesPerCp = mapOfIvValues.get(CP);
            sb.append(valuesPerCp.get(0).getIvPercentage());
            sb.append("% - ");
            sb.append(valuesPerCp.get(valuesPerCp.size() - 1).getIvPercentage());
            sb.append("%");
            this.ivValuesPerCp = sb.toString();
            return true;
        }
    }

    public boolean setCP(int CP) {
        this.CP = CP;
        return calculateIvPercentagePerCP();
    }

    public int getCP() {
        return CP;
    }

    public List<Integer> getPossibleCPValues() {
        return possibleCPValues;
    }

    public String getSpriteFileName() {
        return spriteFileName;
    }

    public int getPokedexNumber() {
        return this.pokedexNumber;
    }

    public int getMinCP() {
        return minCP;
    }

    public int getMaxCP() {
        return maxCP;
    }

    public int getBaseAttack() {
        return baseAttack;
    }

    public int getBaseDefense() {
        return baseDefense;
    }

    public int getBaseStamina() {
        return baseStamina;
    }

    public String getIvValuesPerCp() {
        return this.ivValuesPerCp;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getName() + ": CP " + getCP();
    }
}
