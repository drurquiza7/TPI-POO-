package ar.edu.unsada.stock.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.repository.ArticuloRepository;
import ar.edu.unsada.stock.repository.CompraProveedorRepository;
import ar.edu.unsada.stock.repository.ProveedorRepository;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ArticuloRepository articuloRepository;
    private final CompraProveedorRepository compraProveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository,
                            ArticuloRepository articuloRepository,
                            CompraProveedorRepository compraProveedorRepository) {
        this.proveedorRepository = proveedorRepository;
        this.articuloRepository = articuloRepository;
        this.compraProveedorRepository = compraProveedorRepository;
    }

    @Transactional
    public Proveedor alta(String codigo, String cuit, String razonSocial,
                          String direccion, String telefono, String email) {
        codigo = normalizarCodigo(codigo);
        validarDatos(cuit, razonSocial);

        if (proveedorRepository.existsByCodigo(codigo)) {
            throw new NegocioException("Ya existe un proveedor con el codigo " + codigo);
        }
        if (proveedorRepository.findByCuit(cuit.trim()).isPresent()) {
            throw new NegocioException("Ya existe un proveedor con el CUIT " + cuit.trim());
        }

        Proveedor proveedor = new Proveedor(codigo, cuit.trim(), razonSocial.trim(),
                limpiar(direccion), limpiar(telefono), limpiar(email));
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public Proveedor modificar(String codigo, String cuit, String razonSocial,
                               String direccion, String telefono, String email) {
        Proveedor proveedor = buscarPorCodigo(codigo);
        validarDatos(cuit, razonSocial);

        // El CUIT puede repetirse solamente si es el del mismo proveedor que se esta modificando
        Optional<Proveedor> conMismoCuit = proveedorRepository.findByCuit(cuit.trim());
        if (conMismoCuit.isPresent() && !conMismoCuit.get().getCodigo().equals(proveedor.getCodigo())) {
            throw new NegocioException("Ya existe otro proveedor con el CUIT " + cuit.trim());
        }

        proveedor.setCuit(cuit.trim());
        proveedor.setRazonSocial(razonSocial.trim());
        proveedor.setDireccion(limpiar(direccion));
        proveedor.setTelefono(limpiar(telefono));
        proveedor.setEmail(limpiar(email));
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public void baja(String codigo) {
        Proveedor proveedor = buscarPorCodigo(codigo);

        if (!articuloRepository.findByProveedor(proveedor).isEmpty()) {
            throw new NegocioException("No se puede eliminar el proveedor " + proveedor.getCodigo()
                    + " porque tiene articulos asociados");
        }
        if (!compraProveedorRepository.findByProveedor(proveedor).isEmpty()) {
            throw new NegocioException("No se puede eliminar el proveedor " + proveedor.getCodigo()
                    + " porque tiene compras registradas");
        }
        proveedorRepository.delete(proveedor);
    }

    public Proveedor buscarPorCodigo(String codigo) {
        String codigoBuscado = normalizarCodigo(codigo);
        return proveedorRepository.findByCodigo(codigoBuscado)
                .orElseThrow(() -> new NegocioException("No existe un proveedor con el codigo " + codigoBuscado));
    }

    /**
     * Busca por parte de la razon social, sin distinguir mayusculas.
     * Se filtra en memoria para no modificar el repositorio de la Etapa 1
     * (son pocos proveedores, no hace falta una consulta especial).
     */
    public List<Proveedor> buscarPorRazonSocial(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException("Ingrese un texto para buscar");
        }
        String buscado = texto.trim().toLowerCase();

        List<Proveedor> encontrados = new ArrayList<>();
        for (Proveedor proveedor : proveedorRepository.findAll()) {
            if (proveedor.getRazonSocial().toLowerCase().contains(buscado)) {
                encontrados.add(proveedor);
            }
        }
        return encontrados;
    }

    public List<Proveedor> listar() {
        return proveedorRepository.findAll();
    }

    // ---------- validaciones ----------

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("El codigo no puede estar vacio");
        }
        return codigo.trim().toUpperCase();
    }

    private void validarDatos(String cuit, String razonSocial) {
        if (cuit == null || cuit.isBlank()) {
            throw new NegocioException("El CUIT no puede estar vacio");
        }
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new NegocioException("La razon social no puede estar vacia");
        }
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
