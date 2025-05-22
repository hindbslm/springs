package com.hind.spring.authentication.repository;

import com.hind.spring.authentication.model.ERole;
import com.hind.spring.authentication.model.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}
