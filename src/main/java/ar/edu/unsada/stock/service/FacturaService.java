package ar.edu.unsada.stock.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.Cliente;
import ar.edu.unsada.stock.model.DetalleFactura;
import ar.edu.unsada.stock.model.Factura;
import ar.edu.unsada.stock.repository.FacturaRepository;

/**
 * Facturacion a clientes. Todo movimiento de stock se hace a traves de
 * ArticuloService (descontarStock / aumentarStock), asi la alerta de stock
 * minimo del patron Observer sale en el momento.
 */
@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ClienteService clienteService;
    private final ArticuloService articuloService;

    public FacturaService(FacturaRepository facturaRepository,
                          ClienteService clienteService,
                          ArticuloService articuloService) {
        this.facturaRepository = facturaRepository;
        this.clienteService = clienteService;
        this.articuloService = articuloService;
    }

    // ================= Alta =================

    /**
     * Emite la factura y descuenta el stock de cada articulo en una sola
     * transaccion: si algo falla no queda ni la factura ni el stock modificado.
     * La factura nace como pendiente de impresion (impresa = false).
     *
     * @param renglones lista con articulo y cantidad; el precio se toma del articulo
     */
    @Transactional
    public Factura registrar(String codigo, String numeroFactura, String codigoCliente,
                             LocalDate fechaEmision, List<DetalleFactura> renglones) {
        String codigoFactura = validarCodigoDisponible(codigo);
        String numero = validarNumeroDisponible(numeroFactura);
        validarFecha(fechaEmision);
        Cliente cliente = clienteService.buscarPorCodigo(codigoCliente);

        // Primero se valida todo y recien despues se toca el stock
        validarRenglones(renglones);
        validarStockDisponible(renglones);

        Factura factura = new Factura(codigoFactura, numero, cliente, fechaEmision);
        descontarYCargarRenglones(factura, renglones);
        return facturaRepository.save(factura);
    }

    /** Verifica que el codigo no este vacio ni repetido y lo devuelve normalizado. */
    public String validarCodigoDisponible(String codigo) {
        String codigoFactura = normalizarCodigo(codigo);
        if (facturaRepository.existsByCodigo(codigoFactura)) {
            throw new NegocioException("Ya existe una factura con el codigo " + codigoFactura);
        }
        return codigoFactura;
    }

    /** Verifica que el numero de factura no este vacio ni usado por otra factura. */
    public String validarNumeroDisponible(String numeroFactura) {
        return validarNumero(numeroFactura, null);
    }

    // ================= Modificacion y baja =================

    /**
     * Cambia numero, fecha y renglones. Para ajustar el stock primero se
     * devuelve todo lo que tenia la factura y despues se descuentan los
     * renglones nuevos (con el precio actual de cada articulo).
     * Como el comprobante cambia, la factura vuelve a quedar pendiente de impresion.
     */
    @Transactional
    public Factura modificar(String codigo, String nuevoNumeroFactura, LocalDate nuevaFecha,
                             List<DetalleFactura> nuevosRenglones) {
        Factura factura = buscarFactura(codigo);
        String numero = validarNumero(nuevoNumeroFactura, factura);
        validarFecha(nuevaFecha);
        validarRenglones(nuevosRenglones);

        // Copia por si nos pasan la misma lista que tiene la factura
        List<DetalleFactura> renglones = new ArrayList<>(nuevosRenglones);

        // 1) Se devuelve al deposito lo que tenia facturado
        devolverStock(factura);

        // 2) Con el stock ya devuelto se verifica que alcance para lo nuevo
        validarStockDisponible(renglones);

        // 3) Se reemplazan los renglones (orphanRemoval borra los viejos de la base)
        factura.getDetalles().clear();
        descontarYCargarRenglones(factura, renglones);
        factura.calcularTotal();

        factura.setNumeroFactura(numero);
        factura.setFechaEmision(nuevaFecha);
        factura.setImpresa(false);
        return facturaRepository.save(factura);
    }

    /** Da de baja la factura y devuelve el stock de todos sus renglones. */
    @Transactional
    public void anularOBorrar(String codigo) {
        Factura factura = buscarFactura(codigo);
        devolverStock(factura);
        facturaRepository.delete(factura);
    }

    // ================= Impresion =================

    @Transactional
    public Factura marcarComoImpresa(String codigoFactura) {
        Factura factura = buscarFactura(codigoFactura);
        if (factura.isImpresa()) {
            throw new NegocioException("La factura " + factura.getCodigo() + " ya fue impresa");
        }
        factura.setImpresa(true);
        Factura guardada = facturaRepository.save(factura);
        cargarDetalles(guardada);
        return guardada;
    }

    /**
     * Arma el texto del comprobante listo para mandar a la impresora (o a la consola).
     * La factura tiene que venir de un metodo de este servicio, con los detalles cargados.
     */
    public String generarComprobante(Factura factura) {
        Cliente cliente = factura.getCliente();
        String linea = "=".repeat(78);
        String separador = "-".repeat(78);

        StringBuilder texto = new StringBuilder();
        texto.append(linea).append("\n");
        texto.append("                  MAYORISTA DE ARTICULOS DE COMPUTACION\n");
        texto.append("                                 FACTURA\n");
        texto.append(linea).append("\n");
        texto.append(String.format("Factura Nro: %-25s Fecha: %s%n", factura.getNumeroFactura(), factura.getFechaEmision()));
        texto.append(String.format("Codigo interno: %s%n", factura.getCodigo()));
        texto.append(separador).append("\n");
        texto.append(String.format("Cliente  : %s (%s)%n", cliente.getNombreCompleto(), cliente.getCodigo()));
        texto.append(String.format("DNI      : %s%n", cliente.getDni()));
        texto.append(String.format("Direccion: %s%n", cliente.getDireccion()));
        texto.append(separador).append("\n");
        texto.append(String.format("%-8s %-30s %9s %14s %13s%n",
                "CODIGO", "DESCRIPCION", "CANTIDAD", "PRECIO UNIT.", "SUBTOTAL"));
        texto.append(separador).append("\n");
        for (DetalleFactura detalle : factura.getDetalles()) {
            texto.append(String.format("%-8s %-30s %9d %14.2f %13.2f%n",
                    detalle.getArticulo().getCodigo(), detalle.getArticulo().getDescripcion(),
                    detalle.getCantidad(), detalle.getPrecioUnitario(), detalle.getSubtotal()));
        }
        texto.append(separador).append("\n");
        texto.append(String.format("%64s %13.2f%n", "TOTAL:", factura.getTotal()));
        texto.append(linea);
        return texto.toString();
    }

    // ================= Consultas =================
    // Los detalles son LAZY: se cargan aca adentro, con la transaccion abierta,
    // para que el menu pueda mostrarlos sin LazyInitializationException.

    @Transactional(readOnly = true)
    public Factura buscarPorCodigo(String codigo) {
        return buscarFactura(codigo);
    }

    @Transactional(readOnly = true)
    public List<Factura> listar() {
        List<Factura> facturas = facturaRepository.findAll();
        for (Factura factura : facturas) {
            cargarDetalles(factura);
        }
        return facturas;
    }

    /** Facturas guardadas en la base que todavia no se imprimieron. */
    @Transactional(readOnly = true)
    public List<Factura> listarPendientesDeImpresion() {
        List<Factura> facturas = facturaRepository.findByImpresaFalse();
        for (Factura factura : facturas) {
            cargarDetalles(factura);
        }
        return facturas;
    }

    // ================= Metodos privados =================

    private Factura buscarFactura(String codigo) {
        String codigoBuscado = normalizarCodigo(codigo);
        Factura factura = facturaRepository.findByCodigo(codigoBuscado)
                .orElseThrow(() -> new NegocioException("No existe una factura con el codigo " + codigoBuscado));
        cargarDetalles(factura);
        return factura;
    }

    // Pedir el tamanio obliga a Hibernate a traer los renglones de la base
    private void cargarDetalles(Factura factura) {
        factura.getDetalles().size();
    }

    // Descuenta el stock de cada renglon y lo agrega a la factura con el precio actual del articulo
    private void descontarYCargarRenglones(Factura factura, List<DetalleFactura> renglones) {
        for (DetalleFactura renglon : renglones) {
            Articulo actualizado = articuloService.descontarStock(
                    renglon.getArticulo().getCodigo(), renglon.getCantidad());

            // agregarDetalle enlaza el renglon con la factura y recalcula el total
            factura.agregarDetalle(new DetalleFactura(actualizado, renglon.getCantidad(),
                    actualizado.getPrecioUnitario()));
        }
    }

    private void devolverStock(Factura factura) {
        for (DetalleFactura detalle : factura.getDetalles()) {
            articuloService.aumentarStock(detalle.getArticulo().getCodigo(), detalle.getCantidad());
        }
    }

    private void validarRenglones(List<DetalleFactura> renglones) {
        if (renglones == null || renglones.isEmpty()) {
            throw new NegocioException("La factura debe tener al menos un articulo");
        }

        List<String> codigosCargados = new ArrayList<>();
        for (DetalleFactura renglon : renglones) {
            if (renglon.getArticulo() == null) {
                throw new NegocioException("Hay un renglon sin articulo");
            }
            String codigoArticulo = renglon.getArticulo().getCodigo();

            if (renglon.getCantidad() <= 0) {
                throw new NegocioException("La cantidad de " + codigoArticulo + " debe ser mayor a cero");
            }
            if (codigosCargados.contains(codigoArticulo)) {
                throw new NegocioException("El articulo " + codigoArticulo + " esta repetido en la factura");
            }
            codigosCargados.add(codigoArticulo);
        }
    }

    // Se revisa el stock de todos los renglones antes de descontar el primero
    private void validarStockDisponible(List<DetalleFactura> renglones) {
        for (DetalleFactura renglon : renglones) {
            Articulo articulo = articuloService.buscarPorCodigo(renglon.getArticulo().getCodigo());
            if (renglon.getCantidad() > articulo.getStockActual()) {
                throw new NegocioException("Stock insuficiente de " + articulo.getCodigo()
                        + ": hay " + articulo.getStockActual() + " y se pidieron " + renglon.getCantidad());
            }
        }
    }

    // facturaActual es null en el alta; en la modificacion se permite conservar el propio numero
    private String validarNumero(String numeroFactura, Factura facturaActual) {
        if (numeroFactura == null || numeroFactura.isBlank()) {
            throw new NegocioException("El numero de factura no puede estar vacio");
        }
        String numero = numeroFactura.trim();

        Optional<Factura> conMismoNumero = facturaRepository.findByNumeroFactura(numero);
        boolean esLaMisma = facturaActual != null && conMismoNumero.isPresent()
                && conMismoNumero.get().getCodigo().equals(facturaActual.getCodigo());
        if (conMismoNumero.isPresent() && !esLaMisma) {
            throw new NegocioException("Ya existe una factura con el numero " + numero);
        }
        return numero;
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new NegocioException("La fecha de emision es obligatoria");
        }
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("El codigo de la factura no puede estar vacio");
        }
        return codigo.trim().toUpperCase();
    }
}
