package ar.edu.unsada.stock.observer;

import ar.edu.unsada.stock.model.Articulo;

/**
 * Patron Observer - rol "Observador".
 * Lo implementa cualquier clase que quiera enterarse cuando un articulo
 * llega a su stock minimo.
 */
public interface ObservadorStock {

    void stockMinimoAlcanzado(Articulo articulo);
}
