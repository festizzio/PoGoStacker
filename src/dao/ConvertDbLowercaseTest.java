package dao;

import controller.Pokemon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConvertDbLowercaseTest {
    @Test
    public void loadALlPokemonTest() {
        ConvertDbLowercase.getInstance().open();
        ConvertDbLowercase.getInstance().loadAllPokemon();
        List<Pokemon> allPokemon = ConvertDbLowercase.getInstance().getAllPokemon();
        List<Pokemon> allLowercasePokemon = ConvertDbLowercase.getInstance().getAllLowercasePokemon();

        int numPokemon = (allPokemon.size() + allLowercasePokemon.size()) / 2;

        assertEquals(numPokemon, 724);
        assertTrue(check(allPokemon, allLowercasePokemon, numPokemon));

        ConvertDbLowercase.getInstance().close();
    }

    @Test
    public void saveAllPokemonTest() {

    }

    // Helper method to confirm all the names are identical for loadAllPokemonTest
    public boolean check(List<Pokemon> pokemonList, List<Pokemon> lowercasePokemonList, int numPokemon) {
        for(int i = 0; i < numPokemon; i++) {
            if(!pokemonList.get(i).getName().toLowerCase()
                    .equals(lowercasePokemonList.get(i).getName())) {
                return false;
            }
        }
        return true;
    }
}