package io.annovibe.examples;

import io.annovibe.AI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final Map<String, User> store = new ConcurrentHashMap<>();

    @AI(value = "save user into store keyed by id and return the saved user", provider = "mock")
    public User save(User user) {
        return null;
    }

    @AI(value = "find and return user from store matching id, or null if not found", provider = "mock")
    public User findById(String id) {
        return null;
    }
}
