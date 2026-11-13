package ar.edu.unsada.stock.observer;

import ar.edu.unsada.stock.model.Articulo;

/**
 * Patron Observer - rol "Sujeto".
 * Mantiene la lista de observadores y les avisa cuando corresponde.
 * En este sistema el sujeto concreto es ArticuloService, porque es el unico
 * lugar por donde pasan los cambios de stock.
 */
public interface SujetoStock {

    void agregarObservador(ObservadorStock observador);

    void quitarObservador(ObservadorStock observador);

    void notificarObservadores(Articulo articulo);
}
