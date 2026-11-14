package ar.edu.unsada.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ar.edu.unsada.stock.model.CompraProveedor;
import ar.edu.unsada.stock.model.DetalleCompra;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.observer.ObservadorStock;
import ar.edu.unsada.stock.repository.ArticuloRepository;
import ar.edu.unsada.stock.repository.CompraProveedorRepository;
import ar.edu.unsada.stock.repository.DetalleCompraRepository;
import ar.edu.unsada.stock.repository.DetalleFacturaRepository;
import ar.edu.unsada.stock.repository.ProveedorRepository;
import ar.edu.unsada.stock.repository.RubroRepository;

/**
 * Pruebas de la Etapa 2 sin levantar Spring ni la base.
 * Los repositorios son mocks; los tres servicios son los reales,
 * asi se prueba que la compra mueve el stock de verdad.
 */
class ProveedorCompraServiceTest {

    private ProveedorRepository proveedorRepository;
    private ArticuloRepository articuloRepository;
    private CompraProveedorRepository compraProveedorRepository;

    private ProveedorService proveedorService;
    private CompraService compraService;

    private Proveedor proveedor;
    private Articulo mouse;

    @BeforeEach
    void preparar() {
        proveedorRepository = mock(ProveedorRepository.class);
        articuloRepository = mock(ArticuloRepository.class);
        compraProveedorRepository = mock(CompraProveedorRepository.class);

        ArticuloService articuloService = new ArticuloService(articuloRepository,
                mock(RubroRepository.class), proveedorRepository,
                mock(DetalleFacturaRepository.class), mock(DetalleCompraRepository.class),
                List.of(mock(ObservadorStock.class)));
        proveedorService = new ProveedorService(proveedorRepository, articuloRepository, compraProveedorRepository);
        compraService = new CompraService(compraProveedorRepository, proveedorService, articuloService);

        // Datos de prueba: un proveedor y un articulo con stock 10
        proveedor = new Proveedor("PROV1", "30-11111111-1", "Proveedor de Prueba S.A.", "", "", "");
        mouse = new Articulo("ART1", "Mouse", new Rubro("PER", "Perifericos", ""), proveedor,
                10, 5, new BigDecimal("1000"));

        when(proveedorRepository.findByCodigo("PROV1")).thenReturn(Optional.of(proveedor));
        when(articuloRepository.findByCodigo("ART1")).thenReturn(Optional.of(mouse));
        // save devuelve el mismo objeto que recibe
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(compraProveedorRepository.save(any(CompraProveedor.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrarCompraAumentaElStockDelArticulo() {
        List<DetalleCompra> renglones = new ArrayList<>();
        renglones.add(new DetalleCompra(mouse, 15, new BigDecimal("800")));

        CompraProveedor compra = compraService.registrar("C001", "PROV1", LocalDate.of(2026, 11, 13), renglones);

        assertEquals(25, mouse.getStockActual());                              // 10 + 15
        assertEquals(1, compra.getDetalles().size());
        assertEquals(0, compra.getTotal().compareTo(new BigDecimal("12000"))); // 15 x 800
        verify(compraProveedorRepository).save(compra);
    }

    @Test
    void noPermiteRegistrarCompraSinRenglones() {
        assertThrows(NegocioException.class,
                () -> compraService.registrar("C001", "PROV1", LocalDate.of(2026, 11, 13), new ArrayList<>()));

        verify(compraProveedorRepository, never()).save(any());
    }

    @Test
    void noPermiteCodigoDeCompraRepetido() {
        when(compraProveedorRepository.existsByCodigo("C001")).thenReturn(true);
        List<DetalleCompra> renglones = new ArrayList<>();
        renglones.add(new DetalleCompra(mouse, 15, new BigDecimal("800")));

        assertThrows(NegocioException.class,
                () -> compraService.registrar("C001", "PROV1", LocalDate.of(2026, 11, 13), renglones));

        assertEquals(10, mouse.getStockActual()); // el stock no se toco
    }

    @Test
    void noPermiteBorrarProveedorConArticulosAsociados() {
        when(articuloRepository.findByProveedor(proveedor)).thenReturn(List.of(mouse));

        assertThrows(NegocioException.class, () -> proveedorService.baja("PROV1"));

        verify(proveedorRepository, never()).delete(any());
    }

    @Test
    void borraProveedorSinArticulosNiCompras() {
        when(articuloRepository.findByProveedor(proveedor)).thenReturn(new ArrayList<>());
        when(compraProveedorRepository.findByProveedor(proveedor)).thenReturn(new ArrayList<>());

        proveedorService.baja("PROV1");

        verify(proveedorRepository).delete(proveedor);
    }

    @Test
    void listarPorFechasDevuelveLasComprasDelRango() {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 30);
        CompraProveedor compraDeNoviembre = new CompraProveedor("C001", proveedor, LocalDate.of(2026, 11, 10));
        when(compraProveedorRepository.findByFechaCompraBetween(desde, hasta))
                .thenReturn(List.of(compraDeNoviembre));

        List<CompraProveedor> resultado = compraService.listarPorFechas(desde, hasta);

        assertEquals(1, resultado.size());
        assertEquals("C001", resultado.get(0).getCodigo());
    }

    @Test
    void listarPorFechasRechazaRangoInvertido() {
        LocalDate desde = LocalDate.of(2026, 11, 30);
        LocalDate hasta = LocalDate.of(2026, 11, 1);

        assertThrows(NegocioException.class, () -> compraService.listarPorFechas(desde, hasta));

        verify(compraProveedorRepository, never()).findByFechaCompraBetween(any(), any());
    }
}
