package ar.edu.unsada.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.unsada.stock.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Optional<Cliente> findByDni(String dni);
}
