package ar.edu.unsada.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.DetalleCompra;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {

    boolean existsByArticulo(Articulo articulo);
}
