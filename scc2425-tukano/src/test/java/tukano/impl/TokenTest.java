package tukano.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenTest {

    @Test
    void testIsValidEmptyId() {
        String token = Token.get();
        assertTrue(Token.isValid(token, ""));
        assertFalse(Token.isValid(token, "some id"));
    }

    @Test
    void testIsValidWithId() {
        String idString = "id";
        String token = Token.get(idString);
        assertTrue(Token.isValid(token, idString));

        assertFalse(Token.isValid(token, ""));
        assertFalse(Token.isValid(token, "something else"));
    }
}
