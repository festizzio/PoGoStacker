package sample;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.List;

import Model.DataSource;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;

// Class for updating the current and legacy rewards by pulling data from both https://thesilphroad.com/research-tasks
// and https://pokemongo.fandom.com/wiki/List_of_Field_Research_tasks_and_rewards/2021

public class PokemonUpdate {
    private static final String DB_NAME = "Pokemon.db";
    private static final String CONNECTION_STRING = "jdbc:sqlite:C:/Users/festi/IdeaProjects/Stacker JavaFX2/src/" + DB_NAME;
    private static final String TABLE_POKEMON = "pokemon";
    private static final String COLUMN_POKEMON_NAME = "pokemon_name";
    private static final String COLUMN_POKEDEX_NUMBER = "pokedex_number";
    private static final String COLUMN_BASE_ATTACK = "attack";
    private static final String COLUMN_BASE_DEFENSE = "defense";
    private static final String COLUMN_BASE_STAMINA = "stamina";
    private static final int INDEX_POKEMON_NAME = 1, INDEX_POKEDEX_NUMBER = 2, INDEX_BASE_ATTACK = 3, INDEX_BASE_DEFENSE = 4, INDEX_BASE_STAMINA = 5;
    private static final String TABLE_RESEARCH_REWARDS = "rewards";
    private static final String TABLE_LEGACY_REWARDS = "legacy_rewards";
    private static final String DELETE_RESEARCH_TABLE = "DELETE FROM " + TABLE_RESEARCH_REWARDS;
    private static final String DELETE_LEGACY_TABLE = "DELETE FROM " + TABLE_LEGACY_REWARDS;

    private static final String INSERT_REWARD = "INSERT INTO " + TABLE_RESEARCH_REWARDS + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_POKEDEX_NUMBER + ", " + COLUMN_BASE_ATTACK + ", " +
            COLUMN_BASE_DEFENSE + ", " + COLUMN_BASE_STAMINA + ") VALUES(?, ?, ?, ?, ?)";

    private static final String INSERT_LEGACY_REWARD = "INSERT INTO " + TABLE_LEGACY_REWARDS + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_POKEDEX_NUMBER + ", " + COLUMN_BASE_ATTACK + ", " +
            COLUMN_BASE_DEFENSE + ", " + COLUMN_BASE_STAMINA + ") VALUES(?, ?, ?, ?, ?)";

    private static final String QUERY_FOR_FILLING_REWARDS_TABLE_POKEDEX = "SELECT * FROM " + TABLE_POKEMON + " WHERE " +
            COLUMN_POKEDEX_NUMBER + " = ?";
    private static final String QUERY_FOR_FILLING_REWARDS_TABLE_NAME = "SELECT * FROM " + TABLE_POKEMON + " WHERE " +
            COLUMN_POKEMON_NAME + " = ?";
    private static final String QUERY_FOR_FILLING_LEGACY_TABLE = "SELECT * FROM " + TABLE_POKEMON + " WHERE " + COLUMN_POKEMON_NAME + " = '";

    private final List<String> specialCharacterPokemon = new ArrayList<>(Arrays.asList("ho-oh", "porygon-z", "nidoran-f", "nidoran-m"));
    private PreparedStatement fillLegacyRewardsStatement;

    public List<String> getSpriteFileNames() {
        File spriteFile = new File("C:/Users/festi/IdeaProjects/Stacker JavaFX2/src/sprites/");
        return new ArrayList<>(Arrays.asList(Objects.requireNonNull(spriteFile.list())));
    }

    private Connection conn;

    private final Set<String> newPokemonSet = new HashSet<>();
    private final Set<String> legacyPokemonSet = new LinkedHashSet<>();
    private Document tsrDoc;
    private List<Document> legacyDocs = new ArrayList<>();
    private Document legacy;
    private static PokemonUpdate instance = new PokemonUpdate();
    @FXML
    public ProgressBar updateProgressBar;
    @FXML
    public Label updateLabel;

    public SimpleStringProperty updateProperty;

    private double updateProg = 0;


    public static PokemonUpdate getInstance() {
        if(instance == null) {
            instance = new PokemonUpdate();
        }
        return instance;
    }

    public PokemonUpdate() {

    }

    @FXML
    public void initialize() {
        updateProperty = new SimpleStringProperty("Updating...");
        updateLabel.textProperty().bind(updateTask.messageProperty());
        updateProgressBar.progressProperty().bind(updateTask.progressProperty());
        try {
            tsrDoc = Jsoup.connect("https://thesilphroad.com/research-tasks").get();
        } catch(IOException e) {
            System.out.println("Error creating tsrDoc from thesilphroad.com: " + e.getMessage());
        }
        try {
            legacy = Jsoup.connect("https://pokemongo.fandom.com/wiki/List_of_Field_Research_tasks_and_rewards/2021").get();
        } catch(IOException e) {
            System.out.println("Error creating legacy doc(s) from pokemon.fandom.com: " + e.getMessage());
        }
        try {
            conn = DriverManager.getConnection(CONNECTION_STRING);
        } catch(SQLException e) {
            System.out.println("Error starting SQL connection in PokemonUpdate: " + e.getMessage());
        }
        try {
            conn.setAutoCommit(false);
        } catch (SQLException g) {
            Alert autoCommitError = new Alert(Alert.AlertType.ERROR);
            autoCommitError.setContentText("Error setting connection's autoCommit to false.");
        }
    }

    public Task<Void> updateTask = new Task<>() {
        @Override
        protected Void call() {
            Elements rewards = tsrDoc.getElementsByClass("pokemon");

            // Increments can go up to 100, since the maximum number of updates per list is 50.
            // Current rewards never go over 30-40 unique so the limit of 50 is mainly for legacy rewards.
            double incrementProgress = 0.008;
            try(PreparedStatement deleteResearch = conn.prepareStatement(DELETE_RESEARCH_TABLE)) {
                deleteResearch.execute();
            } catch(SQLException e) {
                System.out.println("Error deleting research table");
                e.printStackTrace();
            }

            try (PreparedStatement fillRewardsStatement = conn.prepareStatement(QUERY_FOR_FILLING_REWARDS_TABLE_POKEDEX)){
                for (Element element : rewards) {
                    Elements pngs = element.getElementsByTag("img");
                    if (!(pngs.first() == null)) {
                        String pokemonIconImage = pngs.first().toString();
                        String[] splitPokemonIconImage = pokemonIconImage.split("/");
                        for (String s : splitPokemonIconImage) {
                            if (s.matches(".+\\.png.*")) {
                                String sFileNameOnly = s.replaceAll("\\.png.*", "");
                                String newPokemonName = "";
                                try {
                                    int pokedexNum = Integer.parseInt(sFileNameOnly);
                                    if(DataSource.getInstance().getPokemon(pokedexNum) == null) {
                                        // Was somehow getting an index 1 out of bounds for length 1 in the below try/catch block
                                        // Found it after digging in the debugger and adding the block.
                                        // In total, it catches the IndexOutOfBoundsException 5-6 times or so.
                                        try {
                                            fillRewardsStatement.setInt(1, pokedexNum);
                                        } catch(IndexOutOfBoundsException ie) {
                                            ie.printStackTrace();
                                        }
                                        try (ResultSet rewardResult = fillRewardsStatement.executeQuery()) {
                                            if(rewardResult.next()) {
                                                newPokemonName = rewardResult.getString(INDEX_POKEMON_NAME);
                                            }
                                        } catch(SQLException addUpdatedPokemon) {
                                            System.out.println("Error getting newPokemonName from pokedex number: " +
                                                    addUpdatedPokemon.getMessage());
                                            addUpdatedPokemon.printStackTrace();
                                        }
                                    } else {
                                        System.out.println("Pokemon found in current rewards list");
                                        newPokemonName = DataSource.getInstance().getPokemon(pokedexNum).getName();
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("file name was a string, not int");
                                    newPokemonName = capitalizeFirstLetterOfEachWord(convertName(sFileNameOnly).getPokemonSqlDbName());
                                }
                                if(newPokemonSet.add(newPokemonName)) {
                                    createSqlRewardsTableUsingStatementAndString(false, newPokemonName);
                                    updateMessage("Added " + newPokemonName + " to current rewards.");
                                    updateProg += incrementProgress;
                                    updateProgress(updateProg, 1);
                                }
                            }

                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                System.out.println("Error creating statement to load new research rewards: " + e.getMessage());
                e.printStackTrace();
                try {
                    conn.rollback();
                } catch (SQLException f) {
                    Alert rollbackError = new Alert(Alert.AlertType.ERROR);
                    rollbackError.setContentText("Unable to rollback data changes. BIG DANGER");
                }
            }
            try (Statement statement = conn.createStatement()) {
                statement.execute(DELETE_LEGACY_TABLE);
                Elements legacyRewards = legacy.getElementsByAttributeValue("width", "100");
                for(int i = legacyRewards.size() - 1; legacyPokemonSet.size() < 50; i--) {
                    Element nameElement = legacyRewards.get(i);
                    Attributes nameAttributes = nameElement.attributes();
                    if (nameAttributes.hasDeclaredValueForKey("alt")) {
                        String nameFromAltAttribute = nameAttributes.get("alt");
                        nameFromAltAttribute = simplifyName(nameFromAltAttribute);

                        if(!newPokemonSet.contains(nameFromAltAttribute)) {
                            if(legacyPokemonSet.add(nameFromAltAttribute)) {
                                createSqlRewardsTableUsingStatementAndString(true, nameFromAltAttribute);
                                updateProg += incrementProgress;
                                updateMessage("Added " + nameFromAltAttribute + " to legacy rewards.");
                                updateProgress(updateProg, 1);
                            }
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                System.out.println("Error creating statement to load new research rewards: " + e.getMessage());
                e.printStackTrace();
                try {
                    conn.rollback();
                } catch (SQLException f) {
                    Alert rollbackError = new Alert(Alert.AlertType.ERROR);
                    rollbackError.setContentText("Unable to rollback data changes. BIG DANGER");
                }
            }
            updateProgress(1, 1);
            updateMessage("Finished updating rewards!");
            return null;
        }
    };

    Thread updateThread = new Thread(updateTask);

    private String simplifyName(String nameToSimplify) {
        List<String> nameValues = Arrays.asList("Spinda", "Eeevee", "Pikachu",
                "Squirtle", "Cherrim");
        for(String name : nameValues) {
            if(nameToSimplify.contains(name)) {
                return name;
            }
        }
        if (nameToSimplify.matches(".+\\u2642")) {
            return "M Nidoran";
        } else if (nameToSimplify.matches(".+\\u2640")) {
            return "F Nidoran";
        }

        return nameToSimplify;
    }

    public boolean checkThreadStatus() {
        return updateThread.isAlive();
    }

    public void updateRewardsTable() {
        updateThread.start();
    }

    public Thread getUpdateThread() {
        return this.updateThread;
    }

    public double getTaskProgress() {
        return updateTask.getProgress();
    }

    public void updateLegacyRewardsTable() {

    }

    private void createSqlRewardsTableUsingStatementAndString(boolean isLegacy, String pokemonName) {

        PreparedStatement createRewardTable = null;
        try {
            fillLegacyRewardsStatement = conn.prepareStatement(QUERY_FOR_FILLING_REWARDS_TABLE_NAME);
            fillLegacyRewardsStatement.setString(INDEX_POKEMON_NAME, pokemonName);
        } catch(SQLException e) {
            System.out.println("Error preparing statement to fill legacy rewards table");
            e.printStackTrace();
        }

        try (ResultSet results = fillLegacyRewardsStatement.executeQuery()) {

            if (isLegacy) {
                createRewardTable = conn.prepareStatement(INSERT_LEGACY_REWARD);
            } else {
                createRewardTable = conn.prepareStatement(INSERT_REWARD);
            }
            createRewardTable.setString(INDEX_POKEMON_NAME, pokemonName);
            createRewardTable.setInt(INDEX_POKEDEX_NUMBER, results.getInt(INDEX_POKEDEX_NUMBER));
            createRewardTable.setInt(INDEX_BASE_ATTACK, results.getInt(INDEX_BASE_ATTACK));
            createRewardTable.setInt(INDEX_BASE_DEFENSE, results.getInt(INDEX_BASE_DEFENSE));
            createRewardTable.setInt(INDEX_BASE_STAMINA, results.getInt(INDEX_BASE_STAMINA));
            createRewardTable.execute();
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Interrupted while sleeping for 0.1 second to prevent overloading pokemongo.fandom.com...");
            }
        } catch (SQLException g) {
            g.printStackTrace();
            System.out.println("Error adding " + pokemonName + " to rewards table: " + g.getMessage());
        } finally {
            try {
                if(createRewardTable != null) {
                    createRewardTable.close();
                }
            } catch(SQLException close) {
                System.out.println("Error closing createRewardTable");
            }
        }
    }

    public PokemonName convertName (String pokemonFromSilphRoad) {
        String pokemonSqlDbName;
        String basePokemonName;
        String spriteName;
        String pokemonNameModifier;
        String basePokemonName2;
        if(pokemonFromSilphRoad.matches(".*[Aa]lola.*")) {
            basePokemonName = pokemonFromSilphRoad.replaceAll("[Aa]lola", "");
            spriteName = pokemonFromSilphRoad + "n";
            pokemonNameModifier = pokemonFromSilphRoad.replaceAll(basePokemonName, "");
            basePokemonName2 = basePokemonName.replaceAll("\\W+", "");
            pokemonSqlDbName = pokemonNameModifier + "n " + basePokemonName2;
        } else if(pokemonFromSilphRoad.matches(".*[Gg]alaria.*")) {
            basePokemonName = pokemonFromSilphRoad.replaceAll("[Gg]alaria", "");
            spriteName = pokemonFromSilphRoad + "n";
            pokemonNameModifier = pokemonFromSilphRoad.replaceAll(basePokemonName, "");
            basePokemonName2 = basePokemonName.replaceAll("\\W+", "");
            pokemonSqlDbName = pokemonNameModifier + "n " + basePokemonName2;
        } else if(pokemonFromSilphRoad.matches(".*[Gg]alar$")) {
            basePokemonName = pokemonFromSilphRoad.replaceAll("[Gg]alar", "").replaceAll("-","");
            // basePokemonName = zigzagoon
            pokemonNameModifier = (pokemonFromSilphRoad.replaceAll(basePokemonName, "") + "ian").
                    replaceAll("-","");
            // pokemonNameModifier = galarian
            spriteName = pokemonNameModifier + " " + basePokemonName;
            pokemonSqlDbName = capitalizeFirstLetterOfEachWord(spriteName);
        } else if(pokemonFromSilphRoad.matches(".*[Rr]ainy.*")) {
            basePokemonName = pokemonFromSilphRoad.replaceAll("[Rr]ainy", "");
            spriteName = pokemonFromSilphRoad;
            pokemonNameModifier = pokemonFromSilphRoad.replaceAll(basePokemonName, "");
            basePokemonName2 = basePokemonName.replaceAll("\\W+", "");
            pokemonSqlDbName = pokemonNameModifier + " " + basePokemonName2;
        } else if(pokemonFromSilphRoad.matches(".*[Bb]urmy.*")) {
            // basePokemonName = "trash"
            basePokemonName = pokemonFromSilphRoad.replaceAll("[Bb]urmy-", "");
            // pokemonNameModifier = burmy-trash with trash removed = "burmy-"
            pokemonNameModifier = pokemonFromSilphRoad.replaceAll(basePokemonName, "");
            // basePokemonName2 = remove the '-' from "burmy-" left with "burmy"
            basePokemonName2 = pokemonNameModifier.replaceAll("\\W+", "");
            // pokemonSqlDbName = trash + " " + burmy
            pokemonSqlDbName = basePokemonName + " " + basePokemonName2;
            spriteName = pokemonSqlDbName;
        } else {
            basePokemonName = "Missingno";
            spriteName = "Missingno";
            pokemonNameModifier = pokemonFromSilphRoad.replaceAll(basePokemonName, "");
            basePokemonName2 = basePokemonName.replaceAll("\\W+", "");
            pokemonSqlDbName = pokemonNameModifier + "n " + basePokemonName2;
        }

        return new PokemonName(pokemonSqlDbName, spriteName);
    }

    public String capitalizeFirstLetterOfEachWord(String wordToCapitalize) {
        char[] wordToCapitalizeArray = wordToCapitalize.toCharArray();
        StringBuilder capitalizedWord = new StringBuilder();

        wordToCapitalizeArray[0] = Character.toUpperCase(wordToCapitalizeArray[0]);
        capitalizedWord.append(wordToCapitalizeArray[0]);
        for(int i = 1; i < wordToCapitalizeArray.length; i++) {
            char c = wordToCapitalizeArray[i];
            if(!(Character.isAlphabetic(c))) {
                wordToCapitalizeArray[i+1] = Character.toUpperCase(wordToCapitalizeArray[i+1]);
            }
            capitalizedWord.append(c);
        }
        return capitalizedWord.toString();
    }

    public static class PokemonName {
        private final String pokemonSqlDbName;
        private final String pokemonDbNameForSprite;

        public PokemonName(String sqlName, String spriteName) {
            this.pokemonSqlDbName = sqlName;
            this.pokemonDbNameForSprite = spriteName;
        }

        public String getPokemonSqlDbName() {
            return this.pokemonSqlDbName;
        }

        public String getPokemonDbNameForSprite() {
            return this.pokemonDbNameForSprite;
        }
    }

//     Method to download any new sprites that are needed. Saving here just in case I decide to use it in the future.
//     In the ideal version of this app, all of the necessary sprites would already be packaged with it. No need to
//     force the user to use their own mobile data to download these.
    public void downloadSpritesData(List<String> pokemonList) {
        for (String pokemon : pokemonList) {
            String lowerCasePokemon = pokemon.toLowerCase();

            if(lowerCasePokemon.contains("'")) {
                // Remove any apostrophes for Pokemon like Sirfetch'd, Farfetch'd, etc.
                lowerCasePokemon = lowerCasePokemon.replaceAll("'", "");
            }

            if(lowerCasePokemon.contains("(")) {
                continue;
            }

            if(lowerCasePokemon.contains(" ")) {

                if(!lowerCasePokemon.contains(".")) {
                    // All names with a space have only two words
                    // Isolate both words into their own strings, then add them in reverse order.
                    // galarian darmanitan becomes darmanitan-galarian etc.
                    String[] firstAndLast = lowerCasePokemon.split(" ");
                    lowerCasePokemon = firstAndLast[1] + "-" + firstAndLast[0];
                } else {
                    // All names with a space AND a period have only two words
                    // Replace the . and \s with a dash '-'
                    lowerCasePokemon = lowerCasePokemon.replaceAll("\\.", "");
                    lowerCasePokemon = lowerCasePokemon.replaceAll("\\s", "-");
                }
            }

            String newFileNameLowerCasePokemon;

            if(lowerCasePokemon.contains("-") && !(specialCharacterPokemon.contains(lowerCasePokemon))) {
                String[] newFileName = lowerCasePokemon.split("-");
                newFileNameLowerCasePokemon = (newFileName[1] + " " + newFileName[0]).toLowerCase();
            } else {
                newFileNameLowerCasePokemon = lowerCasePokemon;
            }

            if (!getSpriteFileNames().contains(newFileNameLowerCasePokemon + ".png")) {
                try {
                    URL url = new URL("https://img.pokemondb.net/sprites/go/normal/" + lowerCasePokemon + ".png");
                    BufferedImage image = ImageIO.read(url);
                    ImageIO.write(image, "png",
                            new File("C:/Users/festi/IdeaProjects/Stacker JavaFX2/src/sprites/" + newFileNameLowerCasePokemon + ".png"));
                } catch (IOException e) {
                    System.out.println("Error in downloadSpritesData downloading sprite for " +
                                    lowerCasePokemon + ":\n" + e.getMessage());
                }
            }
        }
    }
}


