package io.annovibe.examples;

import io.annovibe.vibe;

public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @vibe(value = "create a new User with id and email, save user into repo, and return it", provider = "mock")
    public User registerUser(String id, String email) {
        return null;
    }
}
