package ar.edu.unsada.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.unsada.stock.model.Cliente;
import ar.edu.unsada.stock.model.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Optional<Factura> findByNumeroFactura(String numeroFactura);

    List<Factura> findByCliente(Cliente cliente);

    List<Factura> findByFechaEmisionBetween(LocalDate desde, LocalDate hasta);

    List<Factura> findByImpresaFalse();
}
