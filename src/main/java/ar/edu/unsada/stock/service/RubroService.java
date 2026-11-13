package ar.edu.unsada.stock.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.repository.ArticuloRepository;
import ar.edu.unsada.stock.repository.RubroRepository;

@Service
public class RubroService {

    private final RubroRepository rubroRepository;
    private final ArticuloRepository articuloRepository;

    public RubroService(RubroRepository rubroRepository, ArticuloRepository articuloRepository) {
        this.rubroRepository = rubroRepository;
        this.articuloRepository = articuloRepository;
    }

    @Transactional
    public Rubro alta(String codigo, String nombre, String descripcion) {
        codigo = normalizarCodigo(codigo);
        validarNombre(nombre);

        if (rubroRepository.existsByCodigo(codigo)) {
            throw new NegocioException("Ya existe un rubro con el codigo " + codigo);
        }

        Rubro rubro = new Rubro(codigo, nombre.trim(), limpiar(descripcion));
        return rubroRepository.save(rubro);
    }

    @Transactional
    public Rubro modificar(String codigo, String nuevoNombre, String nuevaDescripcion) {
        Rubro rubro = buscarPorCodigo(codigo);
        validarNombre(nuevoNombre);

        rubro.setNombre(nuevoNombre.trim());
        rubro.setDescripcion(limpiar(nuevaDescripcion));
        return rubroRepository.save(rubro);
    }

    @Transactional
    public void baja(String codigo) {
        Rubro rubro = buscarPorCodigo(codigo);

        // No se puede borrar un rubro que todavia tiene articulos
        if (articuloRepository.existsByRubro(rubro)) {
            throw new NegocioException("No se puede eliminar el rubro " + rubro.getCodigo()
                    + " porque tiene articulos asociados");
        }
        rubroRepository.delete(rubro);
    }

    public Rubro buscarPorCodigo(String codigo) {
        String codigoBuscado = normalizarCodigo(codigo);
        return rubroRepository.findByCodigo(codigoBuscado)
                .orElseThrow(() -> new NegocioException("No existe un rubro con el codigo " + codigoBuscado));
    }

    public List<Rubro> buscarPorNombre(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException("Ingrese un texto para buscar");
        }
        return rubroRepository.findByNombreContainingIgnoreCase(texto.trim());
    }

    public List<Rubro> listar() {
        return rubroRepository.findAll();
    }

    // ---------- validaciones ----------

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("El codigo no puede estar vacio");
        }
        return codigo.trim().toUpperCase();
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre del rubro no puede estar vacio");
        }
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
