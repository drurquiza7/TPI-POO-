package ar.edu.unsada.stock.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.observer.ObservadorStock;
import ar.edu.unsada.stock.observer.SujetoStock;
import ar.edu.unsada.stock.repository.ArticuloRepository;
import ar.edu.unsada.stock.repository.DetalleCompraRepository;
import ar.edu.unsada.stock.repository.DetalleFacturaRepository;
import ar.edu.unsada.stock.repository.ProveedorRepository;
import ar.edu.unsada.stock.repository.RubroRepository;

/**
 * ABM de articulos y manejo de stock.
 * Es el "Sujeto concreto" del patron Observer: todo cambio de stock pasa por
 * esta clase, y cuando un articulo queda con stockActual <= stockMinimo
 * se avisa a los observadores registrados.
 *
 * Las etapas 2 (compras) y 3 (facturacion) tienen que usar aumentarStock()
 * y descontarStock() en lugar de tocar el stock a mano, asi la alerta
 * sale siempre.
 */
@Service
public class ArticuloService implements SujetoStock {

    private final ArticuloRepository articuloRepository;
    private final RubroRepository rubroRepository;
    private final ProveedorRepository proveedorRepository;
    private final DetalleFacturaRepository detalleFacturaRepository;
    private final DetalleCompraRepository detalleCompraRepository;

    private final List<ObservadorStock> observadores = new ArrayList<>();

    // Spring inyecta en la lista todos los componentes que implementan ObservadorStock
    public ArticuloService(ArticuloRepository articuloRepository,
                           RubroRepository rubroRepository,
                           ProveedorRepository proveedorRepository,
                           DetalleFacturaRepository detalleFacturaRepository,
                           DetalleCompraRepository detalleCompraRepository,
                           List<ObservadorStock> observadoresIniciales) {
        this.articuloRepository = articuloRepository;
        this.rubroRepository = rubroRepository;
        this.proveedorRepository = proveedorRepository;
        this.detalleFacturaRepository = detalleFacturaRepository;
        this.detalleCompraRepository = detalleCompraRepository;

        for (ObservadorStock observador : observadoresIniciales) {
            agregarObservador(observador);
        }
    }

    // ================= Patron Observer =================

    @Override
    public void agregarObservador(ObservadorStock observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    @Override
    public void quitarObservador(ObservadorStock observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(Articulo articulo) {
        for (ObservadorStock observador : observadores) {
            observador.stockMinimoAlcanzado(articulo);
        }
    }

    // ================= ABM =================

    @Transactional
    public Articulo alta(String codigo, String descripcion, String codigoRubro, String codigoProveedor,
                         int stockActual, int stockMinimo, BigDecimal precioUnitario) {
        codigo = normalizarCodigo(codigo);
        validarDatos(descripcion, stockMinimo, precioUnitario);
        if (stockActual < 0) {
            throw new NegocioException("El stock inicial no puede ser negativo");
        }
        if (articuloRepository.existsByCodigo(codigo)) {
            throw new NegocioException("Ya existe un articulo con el codigo " + codigo);
        }

        Rubro rubro = buscarRubro(codigoRubro);
        Proveedor proveedor = buscarProveedorOpcional(codigoProveedor);

        Articulo articulo = new Articulo(codigo, descripcion.trim(), rubro, proveedor,
                stockActual, stockMinimo, precioUnitario);
        return guardarYVerificarStock(articulo);
    }

    /** Modifica los datos del articulo. El stock actual se cambia con los metodos de stock. */
    @Transactional
    public Articulo modificar(String codigo, String descripcion, String codigoRubro, String codigoProveedor,
                              int stockMinimo, BigDecimal precioUnitario) {
        Articulo articulo = buscarPorCodigo(codigo);
        validarDatos(descripcion, stockMinimo, precioUnitario);

        articulo.setDescripcion(descripcion.trim());
        articulo.setRubro(buscarRubro(codigoRubro));
        articulo.setProveedor(buscarProveedorOpcional(codigoProveedor));
        articulo.setStockMinimo(stockMinimo);
        articulo.setPrecioUnitario(precioUnitario);

        // Si subieron el stock minimo puede que ahora este en alerta
        return guardarYVerificarStock(articulo);
    }

    @Transactional
    public void baja(String codigo) {
        Articulo articulo = buscarPorCodigo(codigo);

        if (detalleFacturaRepository.existsByArticulo(articulo)
                || detalleCompraRepository.existsByArticulo(articulo)) {
            throw new NegocioException("No se puede eliminar el articulo " + articulo.getCodigo()
                    + " porque figura en facturas o compras");
        }
        articuloRepository.delete(articulo);
    }

    // ================= Consultas =================

    public Articulo buscarPorCodigo(String codigo) {
        String codigoBuscado = normalizarCodigo(codigo);
        return articuloRepository.findByCodigo(codigoBuscado)
                .orElseThrow(() -> new NegocioException("No existe un articulo con el codigo " + codigoBuscado));
    }

    public List<Articulo> buscarPorDescripcion(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException("Ingrese un texto para buscar");
        }
        return articuloRepository.findByDescripcionContainingIgnoreCase(texto.trim());
    }

    public List<Articulo> listar() {
        return articuloRepository.findAll();
    }

    /** Reporte: articulos con stock actual menor o igual al minimo. */
    public List<Articulo> listarConStockMinimoAlcanzado() {
        return articuloRepository.buscarConStockMinimoAlcanzado();
    }

    // ================= Stock =================

    /** Entrada de mercaderia (la va a usar el modulo de compras). */
    @Transactional
    public Articulo aumentarStock(String codigo, int cantidad) {
        validarCantidad(cantidad);
        Articulo articulo = buscarPorCodigo(codigo);
        articulo.setStockActual(articulo.getStockActual() + cantidad);
        return guardarYVerificarStock(articulo);
    }

    /** Salida de mercaderia (la va a usar el modulo de facturacion). */
    @Transactional
    public Articulo descontarStock(String codigo, int cantidad) {
        validarCantidad(cantidad);
        Articulo articulo = buscarPorCodigo(codigo);

        if (cantidad > articulo.getStockActual()) {
            throw new NegocioException("Stock insuficiente de " + articulo.getCodigo()
                    + ": hay " + articulo.getStockActual() + " y se pidieron " + cantidad);
        }
        articulo.setStockActual(articulo.getStockActual() - cantidad);
        return guardarYVerificarStock(articulo);
    }

    /** Ajuste manual: pisa el stock con el valor contado en el deposito. */
    @Transactional
    public Articulo ajustarStock(String codigo, int nuevoStock) {
        if (nuevoStock < 0) {
            throw new NegocioException("El stock no puede ser negativo");
        }
        Articulo articulo = buscarPorCodigo(codigo);
        articulo.setStockActual(nuevoStock);
        return guardarYVerificarStock(articulo);
    }

    // ================= Metodos privados =================

    // Unico punto donde se guarda un articulo: aca se dispara la notificacion
    private Articulo guardarYVerificarStock(Articulo articulo) {
        Articulo guardado = articuloRepository.save(articulo);
        if (guardado.estaEnStockMinimo()) {
            notificarObservadores(guardado);
        }
        return guardado;
    }

    private Rubro buscarRubro(String codigoRubro) {
        String codigo = normalizarCodigo(codigoRubro);
        return rubroRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NegocioException("No existe un rubro con el codigo " + codigo));
    }

    // Si viene vacio el articulo queda sin proveedor asignado
    private Proveedor buscarProveedorOpcional(String codigoProveedor) {
        if (codigoProveedor == null || codigoProveedor.isBlank()) {
            return null;
        }
        String codigo = codigoProveedor.trim().toUpperCase();
        return proveedorRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NegocioException("No existe un proveedor con el codigo " + codigo));
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("El codigo no puede estar vacio");
        }
        return codigo.trim().toUpperCase();
    }

    private void validarDatos(String descripcion, int stockMinimo, BigDecimal precioUnitario) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new NegocioException("La descripcion no puede estar vacia");
        }
        if (stockMinimo < 0) {
            throw new NegocioException("El stock minimo no puede ser negativo");
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El precio unitario debe ser mayor a cero");
        }
    }

    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor a cero");
        }
    }
}
