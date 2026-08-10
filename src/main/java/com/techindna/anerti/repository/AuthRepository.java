package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<JUser, UUID> {

  Optional<JUser> findByEmail(String email);

  Optional<JUser> findByUsername(String username);
}
