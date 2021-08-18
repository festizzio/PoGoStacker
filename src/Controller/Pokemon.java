package Controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;

import java.util.*;

public class Pokemon {

    @FXML
    private final int baseAttack;
    private final int baseDefense;
    private final int baseStamina;
    private final int pokedexNumber;

    private final SimpleStringProperty name;
    private final SimpleIntegerProperty CP;
    private final SimpleStringProperty ivValuesPerCp;
    private final List<Integer> possibleCPValues;
    private final int minCP;
    private final int maxCP;
    private final int stardustValue;
    private final Map<Integer, List<IvValues>> mapOfIvValues;
    private static final List<IvValues> ivList = calculateListOfIVs();

    private String spriteFileName;

    public Pokemon(int pokedexNumber, String name, int baseAttack, int baseDefense, int baseStamina) {

        this.CP = new SimpleIntegerProperty();
        this.ivValuesPerCp = new SimpleStringProperty();
        this.pokedexNumber = pokedexNumber;
        this.name = new SimpleStringProperty(name);
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseStamina = baseStamina;
        possibleCPValues = new ArrayList<>();
        mapOfIvValues = new HashMap<>();
        calculatePossibleCPValues();
        possibleCPValues.sort(Comparator.naturalOrder());
        this.minCP = possibleCPValues.get(0);
        this.maxCP = possibleCPValues.get(possibleCPValues.size() - 1);

        final List<String> stage1EvoName = Arrays.asList("Graveler", "Rhydon", "Poliwhirl", "Monferno",
                "Combusken", "Porygon2", "Raichu", "Skiploom", "Loudred", "Umbreon", "Azumarill");
        final List<String> stage2EvoName = Arrays.asList("Venusaur", "Charizard");
        if(stage2EvoName.contains(name)) {
            stardustValue = 500;
        } else if(stage1EvoName.contains(name)) {
            stardustValue = 300;
        } else{
            stardustValue = 100;
        }

        spriteFileName = name.toLowerCase() + ".png";
    }

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

    private void calculatePossibleCPValues() {
        int CP;
        IvValues currentIVs;
        List<IvValues> listOfIvValues;
        // IV floor for research tasks is 10/10/10
        for(int i = 0; i < ivList.size(); i++) {
            currentIVs = ivList.get(i);
            int attackIV = (int) currentIVs.getAttackIV();
            int defenseIV = (int) currentIVs.getDefenseIV();
            int staminaIV = (int) currentIVs.getStaminaIV();

            CP = calculateCP(attackIV, defenseIV, staminaIV);
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
        // Based on information found on gamepress.gg
        return (int) ((baseAttack + attackIV) * Math.pow((baseDefense + defenseIV), 0.5) *
                Math.pow((baseStamina + staminaIV), 0.5) * Math.pow(0.51739395, 2)) / 10;
    }

    private void fillPossibleCPValueList(){
        possibleCPValues.addAll(mapOfIvValues.keySet());
        possibleCPValues.sort(Comparator.naturalOrder());
    }

    public String getName() {
        return this.name.get();
    }

    public int getStardustValue() {
        return stardustValue;
    }

    private boolean calculateIvPercentagePerCP() {
        StringBuilder sb = new StringBuilder();
        List<IvValues> valuesPerCp;

        if(!mapOfIvValues.containsKey(this.CP.get())){
            System.out.println("Invalid CP value for this Pokemon");
            return false;
        } else {
            valuesPerCp = mapOfIvValues.get(this.CP.get());
            sb.append(valuesPerCp.get(0).getIvPercentage());
            sb.append("% - ");
            sb.append(valuesPerCp.get(valuesPerCp.size() - 1).getIvPercentage());
            sb.append("%");
            this.ivValuesPerCp.set(sb.toString());
            return true;
        }
    }

    public boolean setCP(int CP) {
        this.CP.set(CP);
        return calculateIvPercentagePerCP();
    }

    public int getCP() {
        return CP.get();
    }

    public List<Integer> getPossibleCPValues() {
        return possibleCPValues;
    }

    public String getSpriteFileName() {
        return spriteFileName;
    }

    @Override
    public String toString() {
        return getName() + ": CP " + getCP();
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
        return this.ivValuesPerCp.get();
    }
}
