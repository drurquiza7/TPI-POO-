package ar.edu.unsada.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.Cliente;
import ar.edu.unsada.stock.model.DetalleFactura;
import ar.edu.unsada.stock.model.Factura;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.observer.ObservadorStock;
import ar.edu.unsada.stock.repository.ArticuloRepository;
import ar.edu.unsada.stock.repository.ClienteRepository;
import ar.edu.unsada.stock.repository.DetalleCompraRepository;
import ar.edu.unsada.stock.repository.DetalleFacturaRepository;
import ar.edu.unsada.stock.repository.FacturaRepository;
import ar.edu.unsada.stock.repository.ProveedorRepository;
import ar.edu.unsada.stock.repository.RubroRepository;

/**
 * Pruebas de la Etapa 3 sin levantar Spring ni la base.
 * Los repositorios y el observador son mocks; los servicios son los reales,
 * asi se prueba que la factura mueve el stock de verdad.
 */
class ClienteFacturaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 13);

    private ArticuloRepository articuloRepository;
    private ClienteRepository clienteRepository;
    private FacturaRepository facturaRepository;
    private ObservadorStock observador;

    private ClienteService clienteService;
    private FacturaService facturaService;

    private Cliente cliente;
    private Articulo mouse;

    @BeforeEach
    void preparar() {
        articuloRepository = mock(ArticuloRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        facturaRepository = mock(FacturaRepository.class);
        observador = mock(ObservadorStock.class);

        ArticuloService articuloService = new ArticuloService(articuloRepository,
                mock(RubroRepository.class), mock(ProveedorRepository.class),
                mock(DetalleFacturaRepository.class), mock(DetalleCompraRepository.class),
                List.of(observador));
        clienteService = new ClienteService(clienteRepository, facturaRepository);
        facturaService = new FacturaService(facturaRepository, clienteService, articuloService);

        // Datos de prueba: un cliente y un articulo con stock 10, minimo 5 y precio 1000
        cliente = new Cliente("CLI1", "Perez", "Ana", "30111222", "", "", "");
        mouse = new Articulo("ART1", "Mouse", new Rubro("PER", "Perifericos", ""), null,
                10, 5, new BigDecimal("1000"));

        when(clienteRepository.findByCodigo("CLI1")).thenReturn(Optional.of(cliente));
        when(articuloRepository.findByCodigo("ART1")).thenReturn(Optional.of(mouse));
        // save devuelve el mismo objeto que recibe
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // Arma una lista con un unico renglon del mouse
    private List<DetalleFactura> renglonDeMouse(int cantidad) {
        List<DetalleFactura> renglones = new ArrayList<>();
        renglones.add(new DetalleFactura(mouse, cantidad, mouse.getPrecioUnitario()));
        return renglones;
    }

    // Deja en el repositorio una factura F001 ya emitida con 3 mouses (stock queda en 7)
    private Factura facturaYaEmitidaConTresMouses() {
        Factura factura = facturaService.registrar("F001", "0001-00000001", "CLI1", HOY, renglonDeMouse(3));
        when(facturaRepository.findByCodigo("F001")).thenReturn(Optional.of(factura));
        return factura;
    }

    @Test
    void emitirFacturaDescuentaStockYCalculaTotal() {
        Factura factura = facturaService.registrar("F001", "0001-00000001", "CLI1", HOY, renglonDeMouse(3));

        assertEquals(7, mouse.getStockActual());                               // 10 - 3
        assertEquals(1, factura.getDetalles().size());
        assertEquals(0, factura.getTotal().compareTo(new BigDecimal("3000"))); // 3 x 1000
        assertFalse(factura.isImpresa());                                      // nace pendiente
        verify(facturaRepository).save(factura);
    }

    @Test
    void siNoHayStockSuficienteNoGuardaLaFactura() {
        assertThrows(NegocioException.class,
                () -> facturaService.registrar("F001", "0001-00000001", "CLI1", HOY, renglonDeMouse(11)));

        assertEquals(10, mouse.getStockActual()); // el stock no se toco
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void facturarHastaElMinimoAvisaAlObservador() {
        facturaService.registrar("F001", "0001-00000001", "CLI1", HOY, renglonDeMouse(5));

        assertEquals(5, mouse.getStockActual());
        verify(observador).stockMinimoAlcanzado(mouse);
    }

    @Test
    void marcarComoImpresaCambiaElEstado() {
        facturaYaEmitidaConTresMouses();

        Factura impresa = facturaService.marcarComoImpresa("F001");

        assertTrue(impresa.isImpresa());
    }

    @Test
    void noPermiteImprimirDosVecesLaMismaFactura() {
        facturaYaEmitidaConTresMouses();
        facturaService.marcarComoImpresa("F001");

        assertThrows(NegocioException.class, () -> facturaService.marcarComoImpresa("F001"));
    }

    @Test
    void anularFacturaDevuelveElStock() {
        Factura factura = facturaYaEmitidaConTresMouses();

        facturaService.anularOBorrar("F001");

        assertEquals(10, mouse.getStockActual()); // 7 + 3
        verify(facturaRepository).delete(factura);
    }

    @Test
    void modificarFacturaAjustaLaDiferenciaDeStock() {
        facturaYaEmitidaConTresMouses();
        facturaService.marcarComoImpresa("F001");

        // De 3 mouses pasa a 5: el stock tiene que quedar en 10 - 5
        Factura modificada = facturaService.modificar("F001", "0001-00000001", HOY, renglonDeMouse(5));

        assertEquals(5, mouse.getStockActual());
        assertEquals(1, modificada.getDetalles().size());
        assertEquals(0, modificada.getTotal().compareTo(new BigDecimal("5000")));
        assertFalse(modificada.isImpresa()); // vuelve a quedar pendiente de impresion
    }

    @Test
    void noPermiteBorrarClienteConFacturas() {
        Factura factura = new Factura("F001", "0001-00000001", cliente, HOY);
        when(facturaRepository.findByCliente(cliente)).thenReturn(List.of(factura));

        assertThrows(NegocioException.class, () -> clienteService.baja("CLI1"));

        verify(clienteRepository, never()).delete(any());
    }

    @Test
    void noPermiteClienteConDniRepetido() {
        when(clienteRepository.findByDni("30111222")).thenReturn(Optional.of(cliente));

        assertThrows(NegocioException.class,
                () -> clienteService.alta("CLI2", "Gomez", "Luis", "30111222", "", "", ""));

        verify(clienteRepository, never()).save(any());
    }
}
