package ar.edu.unsada.stock.observer;

import org.springframework.stereotype.Component;

import ar.edu.unsada.stock.model.Articulo;

/**
 * Patron Observer - "Observador concreto".
 * Muestra el aviso por consola en el mismo momento en que cambia el stock.
 * Cuando se haga la interfaz grafica alcanza con agregar otro observador
 * (por ejemplo una ventana de alerta) sin tocar ArticuloService.
 */
@Component
public class AlertaStockConsola implements ObservadorStock {

    @Override
    public void stockMinimoAlcanzado(Articulo articulo) {
        System.out.println();
        System.out.println("  *************** ALERTA DE STOCK ***************");
        System.out.println("  Articulo : " + articulo.getCodigo() + " - " + articulo.getDescripcion());
        System.out.println("  Stock actual: " + articulo.getStockActual()
                + "  |  Stock minimo: " + articulo.getStockMinimo());
        if (articulo.getProveedor() != null) {
            System.out.println("  Reponer con: " + articulo.getProveedor().getRazonSocial());
        }
        System.out.println("  ***********************************************");
    }
}
