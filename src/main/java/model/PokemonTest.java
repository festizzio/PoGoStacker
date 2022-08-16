package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonTest {

    @Test
    void getSpriteFileName() {
        Pokemon aerodactyl = new Pokemon(150, "Aerodactyl", 155, 222, 555, 0);
        Pokemon nidoranf = new Pokemon(51, "F Nidoran", 58, 12, 99, 0);
        assertEquals("aerodactyl.png", aerodactyl.getSpriteFileName());
        assertEquals("f nidoran.png", nidoranf.getSpriteFileName());
    }
}