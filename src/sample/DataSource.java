package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.io.*;
import java.sql.*;
import java.util.*;

public class DataSource {

    // The stack is an ObservableList so we can bind it to the stack table view in the main controller window.
    // The Pokemon currently in the stack are loaded from the database at startup and added to this list.
    private final ObservableList<Pokemon> stack = FXCollections.observableList(new LinkedList<>());

    // Current rewards and legacy rewards are separated so we can create multiple dialogs later.
    // They are tree maps so they are sorted alphabetically.
    private final Map<String, Pokemon> researchRewards = new TreeMap<>();
    private final Map<String, Pokemon> legacyResearchRewards = new LinkedHashMap<>();

    // Creating a secondary map of both that use the pokedex number as a key instead of a string.
    // This would normally cause issues with different forms of the same Pokemon that share pokedex numbers, but
    // this is only used to pull the Pokemon from thesilphroad.com, which already accounts for that and only uses
    // pokedex numbers for Pokemon that do not share their pokedex numbers.
    private final Map<Integer, Pokemon> researchRewardsPokedexValue = new TreeMap<>();
    private final Map<Integer, Pokemon> legacyResearchRewardsPokedexValue = new LinkedHashMap<>();
    private int stardustValue = 0;

    // Steps to add new Pokemon to currently available research rewards:
    // 1. Insert pokemon name, pokedex number, attack, defense, and stamina to either rewards table (research or legacy)
    // 2. Insert Pokemon's stats into main pokemon table if missing (Gen 5 and later)
    // 3. Claim (legacy) reward and the Pokemon should now be available
    // If you encounter an error after adding a new Pokemon, it's most likely a skipped step above,
    // especially a NullPointerException.
    // Will add stardust value to SQL table so it doesn't need to be hardcoded into the Pokemon class later.
    /*
    The above steps were for an older version of the app that had all of the reward names hardcoded rather than in the SQL database.
    This was also prior to implementing the web scraping for TSR (for current rewards) and Pokemon GO Fandom (for legacy rewards).
     */

    // Initializing all of the SQLite constants.
    private static final String DB_NAME = "Pokemon.db";
    private static final String CONNECTION_STRING = "jdbc:sqlite:C:/Users/festi/IdeaProjects/Stacker JavaFX2/src/" + DB_NAME;
    private static final String TABLE_POKEMON = "pokemon";
    private static final String COLUMN_POKEMON_NAME = "pokemon_name";
    private static final String COLUMN_POKEDEX_NUMBER = "pokedex_number";
    private static final String COLUMN_BASE_ATTACK = "attack";
    private static final String COLUMN_BASE_DEFENSE = "defense";
    private static final String COLUMN_BASE_STAMINA = "stamina";
    private static final int INDEX_POKEMON_NAME = 1;
    private static final int INDEX_POKEDEX_NUMBER = 2;
    private static final int INDEX_BASE_ATTACK = 3;
    private static final int INDEX_BASE_DEFENSE = 4;
    private static final int INDEX_BASE_STAMINA = 5;
    private static final String TABLE_RESEARCH_REWARDS = "rewards";
    private static final String TABLE_LEGACY_REWARDS = "legacy_rewards";
    private static final String COLUMN_CP = "CP";
    private static final String TABLE_STACK = "stack";
    private static final String CREATE_POKEMON_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_POKEMON + " (\n" +
            COLUMN_POKEMON_NAME + " text NOT NULL PRIMARY KEY, \n" +
            COLUMN_POKEDEX_NUMBER + " integer,\n" +
            COLUMN_BASE_ATTACK + " integer,\n" +
            COLUMN_BASE_DEFENSE + " integer,\n" +
            COLUMN_BASE_STAMINA + " integer)\n";

    private static final String INSERT_POKEMON = "INSERT INTO " + TABLE_POKEMON + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_POKEDEX_NUMBER + ", " + COLUMN_BASE_ATTACK + ", " +
            COLUMN_BASE_DEFENSE + ", " + COLUMN_BASE_STAMINA + ") VALUES(?, ?, ?, ?, ?)";

    private static final String INSERT_STACK = "INSERT INTO " + TABLE_STACK + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_CP + ") VALUES(?, ?)";
    private static final String QUERY_FOR_FILLING_REWARDS_PREPARED = "SELECT * FROM " + TABLE_POKEMON + " WHERE " +
            COLUMN_POKEMON_NAME + " = ?";
    private static final String QUERY_RESEARCH_POKEMON = "SELECT * FROM " + TABLE_RESEARCH_REWARDS + " ORDER BY " + COLUMN_POKEDEX_NUMBER;
    private static final String QUERY_LEGACY_POKEMON = "SELECT * FROM " + TABLE_LEGACY_REWARDS;
    private static final String QUERY_STACK = "SELECT * FROM " + TABLE_STACK;
    private static final String REMOVE_TOP_STACK = "DELETE FROM " + TABLE_STACK + " WHERE ROWID in (SELECT ROWID FROM " + TABLE_STACK + " LIMIT 1)";
    private static final String REMOVE_ALL_STACK = "DELETE FROM " + TABLE_STACK;

    private static final String INSERT_REWARD = "INSERT INTO " + TABLE_RESEARCH_REWARDS + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_POKEDEX_NUMBER + ", " + COLUMN_BASE_ATTACK + ", " +
            COLUMN_BASE_DEFENSE + ", " + COLUMN_BASE_STAMINA + ") VALUES(?, ?, ?, ?, ?)";
    private static final String INSERT_LEGACY_REWARD = "INSERT INTO " + TABLE_LEGACY_REWARDS + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_POKEDEX_NUMBER + ", " + COLUMN_BASE_ATTACK + ", " +
            COLUMN_BASE_DEFENSE + ", " + COLUMN_BASE_STAMINA + ") VALUES(?, ?, ?, ?, ?)";
    private static final int INDEX_CP = 2;
    private static final String QUERY_ALL_POKEMON = "SELECT * FROM " + TABLE_POKEMON;
    private static final String CREATE_STACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_STACK + " (\n" +
            COLUMN_POKEMON_NAME + " text NOT NULL, \n" +
            COLUMN_CP + " integer)\n";
    private static final String CREATE_RESEARCH_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_RESEARCH_REWARDS + " (\n" +
            COLUMN_POKEMON_NAME + " text NOT NULL PRIMARY KEY, \n" +
            COLUMN_POKEDEX_NUMBER + " integer,\n" +
            COLUMN_BASE_ATTACK + " integer,\n" +
            COLUMN_BASE_DEFENSE + " integer,\n" +
            COLUMN_BASE_STAMINA + " integer)\n";

    private static DataSource instance = new DataSource();

    private Connection conn;

    private final SimpleStringProperty stackStardustValue = new SimpleStringProperty();
    private final SimpleStringProperty stackNumPokemon = new SimpleStringProperty();

    private DataSource() {

    }

    public boolean open() {
        try {
            conn = DriverManager.getConnection(CONNECTION_STRING);
            loadResearchRewardsFromSql();
            loadStack();
            try {
                conn.setAutoCommit(false);
            } catch(SQLException g) {
                Alert autoCommitError = new Alert(Alert.AlertType.ERROR);
                autoCommitError.setContentText("Error setting connection's autoCommit to false.");
                return false;
            }
            return true;
        } catch(SQLException e) {
            System.out.println("Error opening database");
            return false;
        }
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
            conn = DriverManager.getConnection(CONNECTION_STRING);
            loadResearchRewardsFromSql();
            return true;
        } catch(SQLException e) {
            System.out.println("Error opening database");
            return false;
        }
    }

    public static DataSource getInstance() {
        if(instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    private void loadStack() {
        stack.clear();
        try(PreparedStatement statement = conn.prepareStatement(QUERY_STACK);
            ResultSet results = statement.executeQuery()) {

            while(results.next()) {
                String pokemonName = results.getString(1);
                int CP = results.getInt(2);

                Pokemon pokemon = researchRewards.get(pokemonName);
                if(pokemon == null) {
                    pokemon = legacyResearchRewards.get(pokemonName);
                    if(pokemon == null) {
                        try (PreparedStatement queryPokemonTableForRewards = conn.prepareStatement(QUERY_FOR_FILLING_REWARDS_PREPARED)) {
                            queryPokemonTableForRewards.setString(INDEX_POKEMON_NAME, pokemonName);
                            try (ResultSet newResults = queryPokemonTableForRewards.executeQuery()) {
                                int pokedexNumber = newResults.getInt(COLUMN_POKEDEX_NUMBER);
                                int baseAttack = newResults.getInt(COLUMN_BASE_ATTACK);
                                int baseDefense = newResults.getInt(COLUMN_BASE_DEFENSE);
                                int baseStamina = newResults.getInt(COLUMN_BASE_STAMINA);
                                pokemon = new Pokemon(pokedexNumber, pokemonName, baseAttack, baseDefense, baseStamina);
                            } catch (SQLException f) {
                                System.out.println("Error finding " + pokemonName + ": " + f.getMessage());
                            }
                        }
                    }
                }
                if(pokemon != null) {
                    if (!pokemon.setCP(CP)) {
                        System.out.println("Error setting CP for " + pokemonName + ": CP of " + CP + " is invalid.");
                        break;
                    } else {
                        stack.add(pokemon);
                        stardustValue += pokemon.getStardustValue();
                    }
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
        fillList(QUERY_RESEARCH_POKEMON, false);
        fillList(QUERY_LEGACY_POKEMON, true);

    }

    private void fillList(String QUERY_RESEARCH, boolean isLegacy) {
        try (PreparedStatement statement = conn.prepareStatement(QUERY_RESEARCH);
             ResultSet results = statement.executeQuery()) {

            int count = 50;
            while(results.next() && count > 0) {
                String pokemonName = results.getString(INDEX_POKEMON_NAME);
                int pokedexNum = results.getInt(INDEX_POKEDEX_NUMBER);
                Pokemon newPokemon = new Pokemon(pokedexNum, pokemonName,
                        results.getInt(INDEX_BASE_ATTACK), results.getInt(INDEX_BASE_DEFENSE), results.getInt(INDEX_BASE_STAMINA));
                if(!isLegacy) {
                    researchRewards.put(pokemonName, newPokemon);
                    researchRewardsPokedexValue.put(pokedexNum, newPokemon);
                } else {
                    legacyResearchRewards.put(pokemonName, newPokemon);
                    legacyResearchRewardsPokedexValue.put(pokedexNum, newPokemon);
                }
                count--;
            }

        } catch(SQLException e) {
            System.out.println("Error Updating Research List: " + e.getMessage());
            e.printStackTrace();

        }
    }

    public void downloadAllSprites() {
        fillList(QUERY_ALL_POKEMON);
    }

    public void fillList(String QUERY_RESEARCH) {
        List<String> spriteList = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            try(ResultSet results = statement.executeQuery(QUERY_RESEARCH)) {
                while(results.next()) {
                    String pokemonName = results.getString(INDEX_POKEMON_NAME);
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
        try (PreparedStatement insertIntoStack = conn.prepareStatement(INSERT_STACK)) {
            insertIntoStack.setString(INDEX_POKEMON_NAME, pokemon.getName());
            insertIntoStack.setInt(INDEX_CP, pokemon.getCP());
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
        removeFromStack(REMOVE_TOP_STACK);
    }

    public void catchAll() {
        stack.clear();
        setStackStardustValue(0);
        removeFromStack(REMOVE_ALL_STACK);
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

    public ObservableList<Pokemon> getStack() {
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

    /* The below method is used in the event of a stat rebalance, where the stat values are
     ** arbitrarily changed to a new value (assuming it's not a flat calculation for all Pokemon).
     ** Since we only care about the research table, we can usually add a few Pokemon manually to the db.
     */

    public void writeToSQLFullPokemon() {
        try(Scanner pokeScanner = new Scanner(new BufferedReader(
                new FileReader("C:\\Users\\festi\\IdeaProjects\\Stacker JavaFX2\\src\\Pokemon.txt")))) {

            String pokemon;

            try (Statement statement = conn.createStatement()) {
                statement.execute(CREATE_POKEMON_TABLE);
            } catch(SQLException e) {
                System.out.println("SQL Exception, loading database, creating table, and writing prepared statement: " + e.getMessage());
            }

            while(pokeScanner.hasNextLine()) {
                pokemon = pokeScanner.nextLine();
                String[] pokemonData = pokemon.split("\t");
                int pokedexNumber = Integer.parseInt(pokemonData[0]);
                String pokemonName = pokemonData[1];
                int baseAttack = Integer.parseInt(pokemonData[2]);
                int baseDefense = Integer.parseInt(pokemonData[3]);
                int baseStamina = Integer.parseInt(pokemonData[4]);

                try {

                    PreparedStatement createPokemonTable = conn.prepareStatement(INSERT_POKEMON);
                    createPokemonTable.setString(INDEX_POKEMON_NAME, pokemonName);
                    createPokemonTable.setInt(INDEX_POKEDEX_NUMBER, pokedexNumber);
                    createPokemonTable.setInt(INDEX_BASE_ATTACK, baseAttack);
                    createPokemonTable.setInt(INDEX_BASE_DEFENSE, baseDefense);
                    createPokemonTable.setInt(INDEX_BASE_STAMINA, baseStamina);
                    createPokemonTable.execute();
                } catch(SQLException e) {
                    System.out.println("Error writing " + pokemonName + ": " + e.getMessage());

                }

            }
            try {
                conn.commit();
            } catch(SQLException f) {
                System.out.println("Error committing changes in writeToSQLFullPokemon");
            }
        } catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public Pokemon getPokemon(int pokedexNumber) {
        // Returns null if the Pokemon is new and not found in research rewards.
        // How can I get a Pokemon using its Pokedex number if I haven't previously loaded it into rewards?
        // The only way is to pull it from SQL.
        return researchRewardsPokedexValue.get(pokedexNumber);
    }

    public Pokemon getPokemon(String pokemonName) {
        return researchRewards.get(pokemonName);
    }

    //    // Generate list of buttons based on research list being used, GridPane, cpListView, and ToggleGroup,
//    // and return the GridPane to the dialog to populate the Dialog.
//    public GridPane generateToggleButtons(ObservableList<String> dialogResearchList, GridPane dialogGrid, ListView<Integer> cpView,
//                                          ToggleGroup buttonToggleGroup) {
//        int numColumns = 4;
//        Collections.sort(dialogResearchList);
//        int count = 0;
//        boolean quitOuterLoop = false;
//        for(int i = 0; i <= (dialogResearchList.size() / 4); i++) {
//            for(int j = 0; j < numColumns; j++) {
//                if(count >= dialogResearchList.size()) {
//                    quitOuterLoop = true;
//                    break;
//                } else {
//                    String pokemonName = dialogResearchList.get(count);
//                    ToggleButton button = new ToggleButton(pokemonName, new ImageView(
//                            new Image("/sprites/" + pokemonName.toLowerCase() + ".png",
//                                    25, 25, true, false)));
//
//                    button.setToggleGroup(buttonToggleGroup);
//                    button.setUserData(pokemonName);
//                    button.setMaxWidth(Double.MAX_VALUE);
//                    button.setMinWidth(pokemonName.length());
//                    button.setMaxHeight(Double.MAX_VALUE);
//                    if(!(researchRewards.get(pokemonName) == null)) {
//                        button.setOnAction(actionEvent -> cpView.getItems().setAll(researchRewards.get(pokemonName).getPossibleCPValues()));
//                    } else {
//                        button.setOnAction(actionEvent -> cpView.getItems().setAll(legacyResearchRewards.get(pokemonName).getPossibleCPValues()));
//                    }
//
//                    dialogGrid.add(button, j, i);
//                    count++;
//                }
//            }
//            if(quitOuterLoop) {
//                break;
//            }
//        }
//        return dialogGrid;
//    }

    // Deprecating this for now as writeResearchRewards does the same thing but doesn't rely on a separate SQL table.
//    private void loadResearchRewardsListOnly() {
//
//        try(Statement statement = conn.createStatement();
//            ResultSet results = statement.executeQuery(QUERY_RESEARCH_POKEMON)) {
//
//            while(results.next()) {
//                String pokemonName = results.getString(1);
//                int pokedexNumber = results.getInt(2);
//                int baseAttack = results.getInt(3);
//                int baseDefense = results.getInt(4);
//                int baseStamina = results.getInt(5);
//                Pokemon pokemon = new Pokemon(pokedexNumber, pokemonName, baseAttack, baseDefense, baseStamina);
//                researchRewards.put(pokemonName, pokemon);
//            }
//
//        } catch(SQLException e) {
//            System.out.println("Error loading database from loadResearchRewardsListOnly(): " + e.getMessage());
//        }
//    }
}