package ar.edu.unsada.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.observer.ObservadorStock;
import ar.edu.unsada.stock.repository.ArticuloRepository;
import ar.edu.unsada.stock.repository.DetalleCompraRepository;
import ar.edu.unsada.stock.repository.DetalleFacturaRepository;
import ar.edu.unsada.stock.repository.ProveedorRepository;
import ar.edu.unsada.stock.repository.RubroRepository;

/**
 * Pruebas del patron Observer sin levantar Spring ni la base:
 * los repositorios y el observador son mocks.
 */
class ArticuloServiceTest {

    private ArticuloRepository articuloRepository;
    private ObservadorStock observador;
    private ArticuloService articuloService;

    @BeforeEach
    void preparar() {
        articuloRepository = mock(ArticuloRepository.class);
        observador = mock(ObservadorStock.class);

        articuloService = new ArticuloService(articuloRepository,
                mock(RubroRepository.class), mock(ProveedorRepository.class),
                mock(DetalleFacturaRepository.class), mock(DetalleCompraRepository.class),
                List.of(observador));

        // Articulo de prueba: stock 10, minimo 5
        Articulo mouse = new Articulo("ART1", "Mouse", new Rubro("PER", "Perifericos", ""), null,
                10, 5, new BigDecimal("1000"));
        when(articuloRepository.findByCodigo("ART1")).thenReturn(Optional.of(mouse));
        // save devuelve el mismo objeto que recibe
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void avisaAlObservadorCuandoElStockLlegaAlMinimo() {
        Articulo resultado = articuloService.descontarStock("ART1", 5);

        assertEquals(5, resultado.getStockActual());
        verify(observador).stockMinimoAlcanzado(resultado);
    }

    @Test
    void noAvisaSiElStockQuedaPorEncimaDelMinimo() {
        articuloService.descontarStock("ART1", 2);

        verify(observador, never()).stockMinimoAlcanzado(any());
    }

    @Test
    void noPermiteDescontarMasDeLoQueHay() {
        assertThrows(NegocioException.class, () -> articuloService.descontarStock("ART1", 11));

        verify(articuloRepository, never()).save(any());
    }
}
