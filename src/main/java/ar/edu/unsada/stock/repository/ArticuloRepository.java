package ar.edu.unsada.stock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.model.Rubro;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    Optional<Articulo> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Articulo> findByRubro(Rubro rubro);

    boolean existsByRubro(Rubro rubro);

    List<Articulo> findByProveedor(Proveedor proveedor);

    List<Articulo> findByDescripcionContainingIgnoreCase(String descripcion);

    // Compara dos columnas entre si, por eso va con @Query y no con nombre derivado
    @Query("SELECT a FROM Articulo a WHERE a.stockActual <= a.stockMinimo ORDER BY a.descripcion")
    List<Articulo> buscarConStockMinimoAlcanzado();
}
