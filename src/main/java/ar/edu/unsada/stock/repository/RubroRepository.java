package ar.edu.unsada.stock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.unsada.stock.model.Rubro;

public interface RubroRepository extends JpaRepository<Rubro, Long> {

    Optional<Rubro> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Rubro> findByNombreContainingIgnoreCase(String nombre);
}
