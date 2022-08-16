package util;

public final class SQLiteQueries {

    // == SQLite constants ==
    public static final String DB_NAME = "Pokemon.db";
    public static final String CONNECTION_STRING = "jdbc:sqlite:/home/sean/IdeaProjects/PoGoStacker/src/main/java/" + DB_NAME;

    public static final String TABLE_POKEMON = "pokemon";
    public static final String TABLE_RESEARCH_REWARDS = "rewards";
    public static final String TABLE_LEGACY_REWARDS = "legacy_rewards";

    public static final String COLUMN_POKEMON_NAME = "pokemon_name";
    public static final String COLUMN_POKEDEX_NUMBER = "pokedex_number";
    public static final String COLUMN_BASE_ATTACK = "attack";
    public static final String COLUMN_BASE_DEFENSE = "defense";
    public static final String COLUMN_BASE_STAMINA = "stamina";
    public static final int INDEX_POKEMON_NAME = 1;
    public static final int INDEX_POKEDEX_NUMBER = 2;
    public static final int INDEX_BASE_ATTACK = 3;
    public static final int INDEX_BASE_DEFENSE = 4;
    public static final int INDEX_BASE_STAMINA = 5;
    public static final String COLUMN_CP = "CP";
    public static final String TABLE_STACK = "stack";
    public static final String CREATE_STACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_STACK + " (\n" +
            COLUMN_POKEMON_NAME + " text NOT NULL, \n" +
            COLUMN_CP + " integer NOT NULL)\n";

    public static final String INSERT_POKEMON = "INSERT INTO " + TABLE_POKEMON + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_POKEDEX_NUMBER + ", " + COLUMN_BASE_ATTACK + ", " +
            COLUMN_BASE_DEFENSE + ", " + COLUMN_BASE_STAMINA + ") VALUES(?, ?, ?, ?, ?)";

    public static final String INSERT_STACK = "INSERT INTO " + TABLE_STACK + " (" + COLUMN_POKEMON_NAME + ", " +
            COLUMN_CP + ") VALUES(?, ?)";
    public static final String QUERY_FOR_FILLING_REWARDS = "SELECT * FROM " + TABLE_POKEMON + " WHERE " +
            COLUMN_POKEMON_NAME + " = ?";
    public static final String QUERY_RESEARCH_POKEMON = "SELECT * FROM " + TABLE_RESEARCH_REWARDS + " ORDER BY " + COLUMN_POKEDEX_NUMBER;
    public static final String QUERY_LEGACY_POKEMON = "SELECT * FROM " + TABLE_LEGACY_REWARDS;
    public static final String QUERY_STACK = "SELECT * FROM " + TABLE_STACK;
    public static final String QUERY_ALL = "SELECT * FROM " + TABLE_POKEMON;
    public static final String REMOVE_TOP_STACK = "DELETE FROM " + TABLE_STACK + " WHERE ROWID in (SELECT ROWID FROM " + TABLE_STACK + " LIMIT 1)";
    public static final String REMOVE_ALL_STACK = "DELETE FROM " + TABLE_STACK;

    public static final int INDEX_CP = 2;


    private SQLiteQueries() {}
}
