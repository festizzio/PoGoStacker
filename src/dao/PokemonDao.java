package dao;

import model.Pokemon;

public interface PokemonDao {
    boolean open();
    boolean reopen();
    boolean close();
    PokemonDao getInstance();
    void loadStack();
    void loadRewardsFromDb();
    void addReward(Pokemon pokemon);
    void catchReward();
    void catchAll();
}
