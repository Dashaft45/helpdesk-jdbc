package ru.example.helpdesk.repository;

import ru.example.helpdesk.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findById(long id);
    List<User> findAll();
}