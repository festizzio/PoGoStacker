package dao;

import javafx.beans.value.ObservableValue;
import model.Pokemon;

import java.util.List;
import java.util.Map;

public interface PokemonDao {
    boolean open();
    boolean reopen();
    void close();
    void loadStack();
    void loadRewardsFromDb();
    void addReward(Pokemon pokemon);
    void catchReward();
    void catchAll();
    List<Pokemon> getStack();
    ObservableValue<String> getStackStardustValue();
    ObservableValue<String> getTotalPokemon();
    Map<String, Pokemon> getResearchRewards();
    Map<String, Pokemon> getLegacyResearchRewards();
    Pokemon getPokemon(int pokedexNumber);
    Pokemon getPokemon(String pokemonName);
}
