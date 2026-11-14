package ar.edu.unsada.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.CompraProveedor;
import ar.edu.unsada.stock.model.DetalleCompra;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.repository.CompraProveedorRepository;

/**
 * Compras a proveedores. Cada renglon de una compra aumenta el stock del
 * articulo a traves de ArticuloService, asi se respeta el patron Observer.
 */
@Service
public class CompraService {

    private final CompraProveedorRepository compraProveedorRepository;
    private final ProveedorService proveedorService;
    private final ArticuloService articuloService;

    public CompraService(CompraProveedorRepository compraProveedorRepository,
                         ProveedorService proveedorService,
                         ArticuloService articuloService) {
        this.compraProveedorRepository = compraProveedorRepository;
        this.proveedorService = proveedorService;
        this.articuloService = articuloService;
    }

    /**
     * Registra la compra completa. Todo ocurre en una sola transaccion:
     * si algo falla no queda ni la compra ni el stock modificado.
     *
     * @param renglones lista con articulo, cantidad y costo unitario de cada item
     */
    @Transactional
    public CompraProveedor registrar(String codigo, String codigoProveedor, LocalDate fecha,
                                     List<DetalleCompra> renglones) {
        String codigoCompra = validarCodigoDisponible(codigo);
        if (fecha == null) {
            throw new NegocioException("La fecha de la compra es obligatoria");
        }
        Proveedor proveedor = proveedorService.buscarPorCodigo(codigoProveedor);

        // Primero se valida todo y recien despues se toca el stock
        validarRenglones(renglones);

        CompraProveedor compra = new CompraProveedor(codigoCompra, proveedor, fecha);
        for (DetalleCompra renglon : renglones) {
            Articulo actualizado = articuloService.aumentarStock(
                    renglon.getArticulo().getCodigo(), renglon.getCantidad());

            // agregarDetalle enlaza el renglon con la compra y recalcula el total
            compra.agregarDetalle(new DetalleCompra(actualizado, renglon.getCantidad(), renglon.getCostoUnitario()));
        }
        return compraProveedorRepository.save(compra);
    }

    /** Verifica que el codigo no este vacio ni repetido y lo devuelve normalizado. */
    public String validarCodigoDisponible(String codigo) {
        String codigoCompra = normalizarCodigo(codigo);
        if (compraProveedorRepository.existsByCodigo(codigoCompra)) {
            throw new NegocioException("Ya existe una compra con el codigo " + codigoCompra);
        }
        return codigoCompra;
    }

    // ================= Consultas =================
    // Los detalles son LAZY: se cargan aca adentro, con la transaccion abierta,
    // para que el menu pueda mostrarlos sin LazyInitializationException.

    @Transactional(readOnly = true)
    public CompraProveedor buscarPorCodigo(String codigo) {
        String codigoBuscado = normalizarCodigo(codigo);
        CompraProveedor compra = compraProveedorRepository.findByCodigo(codigoBuscado)
                .orElseThrow(() -> new NegocioException("No existe una compra con el codigo " + codigoBuscado));
        cargarDetalles(compra);
        return compra;
    }

    @Transactional(readOnly = true)
    public List<CompraProveedor> listar() {
        List<CompraProveedor> compras = compraProveedorRepository.findAll();
        for (CompraProveedor compra : compras) {
            cargarDetalles(compra);
        }
        return compras;
    }

    @Transactional(readOnly = true)
    public List<CompraProveedor> listarPorFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new NegocioException("Debe indicar las dos fechas del rango");
        }
        if (desde.isAfter(hasta)) {
            throw new NegocioException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        List<CompraProveedor> compras = compraProveedorRepository.findByFechaCompraBetween(desde, hasta);
        for (CompraProveedor compra : compras) {
            cargarDetalles(compra);
        }
        return compras;
    }

    // ================= Metodos privados =================

    // Pedir el tamanio obliga a Hibernate a traer los renglones de la base
    private void cargarDetalles(CompraProveedor compra) {
        compra.getDetalles().size();
    }

    private void validarRenglones(List<DetalleCompra> renglones) {
        if (renglones == null || renglones.isEmpty()) {
            throw new NegocioException("La compra debe tener al menos un renglon");
        }

        List<String> codigosCargados = new ArrayList<>();
        for (DetalleCompra renglon : renglones) {
            if (renglon.getArticulo() == null) {
                throw new NegocioException("Hay un renglon sin articulo");
            }
            String codigoArticulo = renglon.getArticulo().getCodigo();

            if (renglon.getCantidad() <= 0) {
                throw new NegocioException("La cantidad de " + codigoArticulo + " debe ser mayor a cero");
            }
            if (renglon.getCostoUnitario() == null
                    || renglon.getCostoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new NegocioException("El costo unitario de " + codigoArticulo + " debe ser mayor a cero");
            }
            if (codigosCargados.contains(codigoArticulo)) {
                throw new NegocioException("El articulo " + codigoArticulo + " esta repetido en la compra");
            }
            codigosCargados.add(codigoArticulo);
        }
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("El codigo de la compra no puede estar vacio");
        }
        return codigo.trim().toUpperCase();
    }
}
