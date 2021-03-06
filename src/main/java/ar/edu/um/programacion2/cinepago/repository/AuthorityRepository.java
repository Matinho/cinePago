package ar.edu.um.programacion2.cinepago.repository;

import ar.edu.um.programacion2.cinepago.domain.Authority;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Authority entity.
 */
public interface AuthorityRepository extends JpaRepository<Authority, String> {
}
