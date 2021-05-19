package sample;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonUpdateTest {

    @Test
    void getPokemon() {
        if(DataSource.getInstance().open()) {
            assertEquals("Alolan Grimer", DataSource.getInstance().getPokemon("Alolan Grimer").getName());
            assertEquals("Bulbasaur", DataSource.getInstance().getPokemon(1).getName());
        }
    }

    @Test
    void convertName() {
        assertEquals("alolan grimer", PokemonUpdate.getInstance()
                .convertName("grimer-alola").getPokemonSqlDbName());
        assertEquals("galarian meowth", PokemonUpdate.getInstance()
                .convertName("meowth-galaria").getPokemonSqlDbName());
        assertEquals("alolan muk", PokemonUpdate.getInstance()
                .convertName("muk-alola").getPokemonSqlDbName());
        assertEquals("rainy castform", PokemonUpdate.getInstance()
                .convertName("castform-rainy").getPokemonSqlDbName());

        assertEquals("grimer-alolan", PokemonUpdate.getInstance()
                .convertName("grimer-alola").getPokemonDbNameForSprite());
        assertEquals("meowth-galarian", PokemonUpdate.getInstance()
                .convertName("meowth-galaria").getPokemonDbNameForSprite());
        assertEquals("meowth-alolan", PokemonUpdate.getInstance()
                .convertName("meowth-alola").getPokemonDbNameForSprite());
        assertEquals("castform-rainy", PokemonUpdate.getInstance()
                .convertName("castform-rainy").getPokemonDbNameForSprite());
    }

    @Test
    void capitalizeFirstLetterOfEachWord() {
        assertEquals("Alolan Grimer", PokemonUpdate.getInstance().capitalizeFirstLetterOfEachWord("alolan grimer"));
        assertEquals("Alolan Grimer Says", PokemonUpdate.getInstance().capitalizeFirstLetterOfEachWord("alolan grimer says"));
        assertEquals("Sean Lubbers", PokemonUpdate.getInstance().capitalizeFirstLetterOfEachWord("sean lubbers"));

    }
}