package dao;

import model.Pokemon;
import controller.PokemonUpdate;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import util.SQLiteQueries;

import java.sql.*;
import java.util.*;

// Single-time use class to load all of the Pokemon from the database and convert to lowercase.
// Going to create a test class as well to compare the list we loaded from the database to
// the updated list with the all-lowercase names and make sure they are converted properly.
public class ConvertDbLowercase {

    private final LinkedList<Pokemon> stack = new LinkedList<>();

    // Current rewards and legacy rewards are separated so we can create multiple dialogs later.
    // They are tree maps so they are sorted alphabetically.
    private final Map<String, Pokemon> researchRewards = new TreeMap<>();
    private final Map<String, Pokemon> legacyResearchRewards = new LinkedHashMap<>();

    // Creating a secondary map of both that use the pokedex number as a key instead of a string.
    // This would normally cause issues with different forms of the same Pokemon that share pokedex numbers, but
    // this is only used to pull the Pokemon from thesilphroad.com, which already accounts for that and only uses
    // pokedex numbers for Pokemon that do not share their pokedex numbers.
    private final Map<Integer, Pokemon> researchRewardsPokedexValue = new TreeMap<>();

    private static ConvertDbLowercase instance = new ConvertDbLowercase();

    private Connection conn;

    private int stardustValue = 0;
    private final SimpleStringProperty stackStardustValue = new SimpleStringProperty();
    private final SimpleStringProperty stackNumPokemon = new SimpleStringProperty();

    private ConvertDbLowercase() {

    }

    public boolean open() {
        if(reopen()) {
            loadStack();
            try {
                conn.setAutoCommit(false);
            } catch (SQLException g) {
                Alert autoCommitError = new Alert(Alert.AlertType.ERROR);
                autoCommitError.setContentText("Error setting connection's autoCommit to false.");
                return false;
            }
            return true;
        }
        System.out.println("Error opening database");
        return false;
    }

    public void close() {
        try {
            if(conn != null) {
                conn.close();
            }
        } catch(SQLException e) {
            System.out.println("Unable to close connection: " + e.getMessage());
        }
    }

    public boolean reopen() {
        try {
            conn = DriverManager.getConnection(SQLiteQueries.CONNECTION_STRING);
            loadResearchRewardsFromSql();
            return true;
        } catch(SQLException e) {
            System.out.println("Error opening database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static ConvertDbLowercase getInstance() {
        if(instance == null) {
            instance = new ConvertDbLowercase();
        }
        return instance;
    }

    // Load Pokemon stack from SQLite database.
    private void loadStack() {
        try (PreparedStatement statement = conn.prepareStatement(SQLiteQueries.CREATE_STACK_TABLE)) {
            statement.execute();
        } catch(SQLException e) {
            System.out.println("Error creating stack table: " + e.getMessage());
            e.printStackTrace();
        }
        stack.clear();
        try(PreparedStatement statement = conn.prepareStatement(SQLiteQueries.QUERY_STACK);
            ResultSet results = statement.executeQuery()) {

            while(results.next()) {
                String pokemonName = results.getString(1);
                int CP = results.getInt(2);

                Pokemon pokemon = researchRewards.get(pokemonName);
                if(pokemon == null) {
                    pokemon = legacyResearchRewards.get(pokemonName);
                    if(pokemon == null) {
                        try (PreparedStatement queryPokemonTableForRewards = conn.prepareStatement(SQLiteQueries.QUERY_FOR_FILLING_REWARDS)) {
                            queryPokemonTableForRewards.setString(SQLiteQueries.INDEX_POKEMON_NAME, pokemonName);
                            try (ResultSet newResults = queryPokemonTableForRewards.executeQuery()) {
                                int pokedexNumber = newResults.getInt(SQLiteQueries.COLUMN_POKEDEX_NUMBER);
                                int baseAttack = newResults.getInt(SQLiteQueries.COLUMN_BASE_ATTACK);
                                int baseDefense = newResults.getInt(SQLiteQueries.COLUMN_BASE_DEFENSE);
                                int baseStamina = newResults.getInt(SQLiteQueries.COLUMN_BASE_STAMINA);
                                pokemon = new Pokemon(pokedexNumber, pokemonName, baseAttack, baseDefense, baseStamina, CP);
                            } catch (SQLException f) {
                                System.out.println("Error finding " + pokemonName + ": " + f.getMessage());
                            }
                        }
                    }
                }
                if(pokemon != null) {
                    stack.add(pokemon);
                    stardustValue += pokemon.getStardustValue();
                } else {
                    System.out.println("Error setting CP for Pokemon. Pokemon not found.");
                }
            }

        } catch(SQLException e) {
            System.out.println("Error loading database from loadStack(): " + e.getMessage());
        }
        setStackStardustValue(stardustValue);
    }

    // Writes research table from the full list using the list of current rewards.
    // For now, it pulls from hardcoded lists but ideally this will be in a SQL table in the future.
    public void loadResearchRewardsFromSql() {
        loadRewardsUsingQuery(SQLiteQueries.QUERY_RESEARCH_POKEMON, false);
        loadRewardsUsingQuery(SQLiteQueries.QUERY_LEGACY_POKEMON, true);
    }

    private void loadRewardsUsingQuery(String QUERY_RESEARCH, boolean isLegacy) {
        try (PreparedStatement statement = conn.prepareStatement(QUERY_RESEARCH);
             ResultSet results = statement.executeQuery()) {

            int count = 0;
            while(results.next() && count < 60) {
                String pokemonName = results.getString(SQLiteQueries.INDEX_POKEMON_NAME);
                int pokedexNum = results.getInt(SQLiteQueries.INDEX_POKEDEX_NUMBER);
                Pokemon newPokemon = new Pokemon(pokedexNum, pokemonName,
                        results.getInt(SQLiteQueries.INDEX_BASE_ATTACK), results.getInt(SQLiteQueries.INDEX_BASE_DEFENSE),
                        results.getInt(SQLiteQueries.INDEX_BASE_STAMINA), 0);
                if(!isLegacy) {
                    researchRewards.put(pokemonName, newPokemon);
                    researchRewardsPokedexValue.put(pokedexNum, newPokemon);
                } else {
                    legacyResearchRewards.put(pokemonName, newPokemon);
                }
                count++;
            }

        } catch(SQLException e) {
            System.out.println("Error Updating Research List: " + e.getMessage());
            e.printStackTrace();

        }
    }

    public void loadRewardsUsingQuery(String QUERY_RESEARCH) {
        List<String> spriteList = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            try(ResultSet results = statement.executeQuery(QUERY_RESEARCH)) {
                while(results.next()) {
                    String pokemonName = results.getString(SQLiteQueries.INDEX_POKEMON_NAME);
                    spriteList.add(pokemonName);
                }
            }
        } catch(SQLException e) {
            System.out.println("Error getting all Pokemon from SQL to download sprites: " + e.getMessage());
            e.printStackTrace();
        }
        PokemonUpdate.getInstance().downloadSpritesData(spriteList);
    }


    private void writeToStack(Pokemon pokemon) {
        try (PreparedStatement insertIntoStack = conn.prepareStatement(SQLiteQueries.INSERT_STACK)) {
            insertIntoStack.setString(SQLiteQueries.INDEX_POKEMON_NAME, pokemon.getName());
            insertIntoStack.setInt(SQLiteQueries.INDEX_CP, pokemon.getCP());
            insertIntoStack.execute();
            conn.commit();
        } catch(SQLException e) {
            System.out.println("Error creating statement in writeToStack()");
        }
    }

    public void addReward(Pokemon pokemon) {
        stack.add(pokemon);
        stardustValue += pokemon.getStardustValue();
        setStackStardustValue(stardustValue);
        System.out.println(pokemon.getName() + " : " + pokemon.getStardustValue() + " added to the stack.");
        writeToStack(pokemon);
    }

    public void catchReward() {
        int pokemonStardust = stack.get(0).getStardustValue();
        stack.remove(0);
        setStackStardustValue(stardustValue - pokemonStardust);
        removeFromStack(SQLiteQueries.REMOVE_TOP_STACK);
    }

    public void catchAll() {
        stack.clear();
        setStackStardustValue(0);
        removeFromStack(SQLiteQueries.REMOVE_ALL_STACK);
    }

    public final Map<String, Pokemon> getResearchRewards() {
        return Collections.unmodifiableMap(researchRewards);
    }

    public final Map<String, Pokemon> getLegacyResearchRewards() {
        return Collections.unmodifiableMap(legacyResearchRewards);
    }

    public ObservableValue<String> getStackStardustValue() {
        return stackStardustValue;
    }

    public ObservableValue<String> getTotalPokemon() {
        return stackNumPokemon;
    }

    public List<Pokemon> getStack() {
        return stack;
    }

    public void setStackStardustValue(int stardust) {
        stardustValue = stardust;
        stackStardustValue.set("Total stardust value: " + stardustValue);
        stackNumPokemon.set("Total Pokemon: " + stack.size());
    }

    private void removeFromStack(String whatToRemove) {
        try (PreparedStatement removeFromStack = conn.prepareStatement(whatToRemove)) {
            removeFromStack.execute();
            conn.commit();
        } catch(SQLException e) {
            System.out.println("Error creating prepared statement for remove: " + e.getMessage());
        }
    }

    public Pokemon getPokemon(int pokedexNumber) {
        return researchRewardsPokedexValue.get(pokedexNumber);
    }

    public Pokemon getPokemon(String pokemonName) {
        return researchRewards.get(pokemonName);
    }


    // Object to hold the Pokemon we want to convert.
    private final List<Pokemon> allPokemon = new LinkedList<>();
    private final List<Pokemon> allLowercasePokemon = new LinkedList<>();

    // We need a method to load the list of all Pokemon
    public void loadAllPokemon() {
        try(PreparedStatement statement = conn.prepareStatement(SQLiteQueries.QUERY_ALL);
            ResultSet resultSet = statement.executeQuery()) {
            while(resultSet.next()) {
                String name = resultSet.getString(SQLiteQueries.COLUMN_POKEMON_NAME);
                String lowercase = name.toLowerCase();
                int pokedex = resultSet.getInt(SQLiteQueries.COLUMN_POKEDEX_NUMBER),
                        attack = resultSet.getInt(SQLiteQueries.COLUMN_BASE_ATTACK),
                        defense = resultSet.getInt(SQLiteQueries.COLUMN_BASE_DEFENSE),
                        stamina = resultSet.getInt(SQLiteQueries.COLUMN_BASE_STAMINA);
                Pokemon newPokemon = new Pokemon(pokedex, name, attack, defense, stamina, 0);
                allPokemon.add(newPokemon);
                newPokemon.setName(lowercase);
                allLowercasePokemon.add(newPokemon);
            }
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<Pokemon> getAllPokemon() {
        return allPokemon;
    }

    public List<Pokemon> getAllLowercasePokemon() {
        return allLowercasePokemon;
    }

    // We need a method to write the new list of lowercase Pokemon names
    public void saveLowercasePokemon() {

    }
}