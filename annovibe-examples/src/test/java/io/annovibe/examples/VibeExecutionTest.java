package io.annovibe.examples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VibeExecutionTest {

    @Test
    public void testMathVibesExecution() {
        // Assert that the compile-time injected method bodies executed and returned correct results!
        assertEquals(120L, MathVibes.factorial(5));
        assertEquals(1L, MathVibes.factorial(0));
        assertEquals(42, MathVibes.add(40, 2));
        assertTrue(MathVibes.isPalindrome("Race car"));
        assertFalse(MathVibes.isPalindrome("Annovibe"));
        assertEquals("ebivonnA", MathVibes.reverseString("Annovibe"));
    }

    @Test
    public void testMultiClassCoherence() {
        UserRepository repo = new UserRepository();
        UserService service = new UserService(repo);

        User created = service.registerUser("u100", "alice@example.com");
        assertNotNull(created);
        assertEquals("u100", created.getId());
        assertEquals("alice@example.com", created.getEmail());

        User found = repo.findById("u100");
        assertNotNull(found);
        assertEquals("alice@example.com", found.getEmail());
    }
}
