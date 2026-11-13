package ar.edu.unsada.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.DetalleFactura;

public interface DetalleFacturaRepository extends JpaRepository<DetalleFactura, Long> {

    // Sirve para no dejar borrar un articulo que ya fue facturado
    boolean existsByArticulo(Articulo articulo);
}
