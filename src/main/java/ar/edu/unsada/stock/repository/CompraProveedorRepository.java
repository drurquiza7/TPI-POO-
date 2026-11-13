package ar.edu.unsada.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.unsada.stock.model.CompraProveedor;
import ar.edu.unsada.stock.model.Proveedor;

public interface CompraProveedorRepository extends JpaRepository<CompraProveedor, Long> {

    Optional<CompraProveedor> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<CompraProveedor> findByProveedor(Proveedor proveedor);

    // Filtro de compras por rango de fechas (Etapa 2)
    List<CompraProveedor> findByFechaCompraBetween(LocalDate desde, LocalDate hasta);
}
