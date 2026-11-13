package ar.edu.unsada.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.unsada.stock.model.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Optional<Proveedor> findByCuit(String cuit);
}
