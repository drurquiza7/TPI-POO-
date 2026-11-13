package ar.edu.unsada.stock.exception;

/**
 * Excepcion propia para las reglas de negocio (codigo duplicado,
 * stock insuficiente, datos vacios, etc.). El menu la atrapa y
 * muestra el mensaje al usuario.
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
