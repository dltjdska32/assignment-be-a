package com.assginment.be_a.domain.repo;

import com.assginment.be_a.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u " +
            "WHERE u.username = :username")
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}
