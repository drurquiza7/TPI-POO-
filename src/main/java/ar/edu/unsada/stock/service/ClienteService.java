package ar.edu.unsada.stock.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Cliente;
import ar.edu.unsada.stock.repository.ClienteRepository;
import ar.edu.unsada.stock.repository.FacturaRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final FacturaRepository facturaRepository;

    public ClienteService(ClienteRepository clienteRepository, FacturaRepository facturaRepository) {
        this.clienteRepository = clienteRepository;
        this.facturaRepository = facturaRepository;
    }

    @Transactional
    public Cliente alta(String codigo, String apellido, String nombre, String dni,
                        String direccion, String telefono, String email) {
        codigo = normalizarCodigo(codigo);
        validarDatos(apellido, nombre, dni);

        if (clienteRepository.existsByCodigo(codigo)) {
            throw new NegocioException("Ya existe un cliente con el codigo " + codigo);
        }
        if (clienteRepository.findByDni(dni.trim()).isPresent()) {
            throw new NegocioException("Ya existe un cliente con el DNI " + dni.trim());
        }

        Cliente cliente = new Cliente(codigo, apellido.trim(), nombre.trim(), dni.trim(),
                limpiar(direccion), limpiar(telefono), limpiar(email));
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente modificar(String codigo, String apellido, String nombre, String dni,
                             String direccion, String telefono, String email) {
        Cliente cliente = buscarPorCodigo(codigo);
        validarDatos(apellido, nombre, dni);

        // El DNI puede repetirse solamente si es el del mismo cliente que se esta modificando
        Optional<Cliente> conMismoDni = clienteRepository.findByDni(dni.trim());
        if (conMismoDni.isPresent() && !conMismoDni.get().getCodigo().equals(cliente.getCodigo())) {
            throw new NegocioException("Ya existe otro cliente con el DNI " + dni.trim());
        }

        cliente.setApellido(apellido.trim());
        cliente.setNombre(nombre.trim());
        cliente.setDni(dni.trim());
        cliente.setDireccion(limpiar(direccion));
        cliente.setTelefono(limpiar(telefono));
        cliente.setEmail(limpiar(email));
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void baja(String codigo) {
        Cliente cliente = buscarPorCodigo(codigo);

        if (!facturaRepository.findByCliente(cliente).isEmpty()) {
            throw new NegocioException("No se puede eliminar el cliente " + cliente.getCodigo()
                    + " porque tiene facturas asociadas");
        }
        clienteRepository.delete(cliente);
    }

    public Cliente buscarPorCodigo(String codigo) {
        String codigoBuscado = normalizarCodigo(codigo);
        return clienteRepository.findByCodigo(codigoBuscado)
                .orElseThrow(() -> new NegocioException("No existe un cliente con el codigo " + codigoBuscado));
    }

    public Cliente buscarPorDni(String dni) {
        if (dni == null || dni.isBlank()) {
            throw new NegocioException("Ingrese un DNI para buscar");
        }
        String dniBuscado = dni.trim();
        return clienteRepository.findByDni(dniBuscado)
                .orElseThrow(() -> new NegocioException("No existe un cliente con el DNI " + dniBuscado));
    }

    /**
     * Busca por parte del apellido, sin distinguir mayusculas.
     * Se filtra en memoria para no modificar el repositorio de la Etapa 1.
     */
    public List<Cliente> buscarPorApellido(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException("Ingrese un texto para buscar");
        }
        String buscado = texto.trim().toLowerCase();

        List<Cliente> encontrados = new ArrayList<>();
        for (Cliente cliente : clienteRepository.findAll()) {
            if (cliente.getApellido().toLowerCase().contains(buscado)) {
                encontrados.add(cliente);
            }
        }
        return encontrados;
    }

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    // ---------- validaciones ----------

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("El codigo no puede estar vacio");
        }
        return codigo.trim().toUpperCase();
    }

    private void validarDatos(String apellido, String nombre, String dni) {
        if (apellido == null || apellido.isBlank()) {
            throw new NegocioException("El apellido no puede estar vacio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre no puede estar vacio");
        }
        if (dni == null || dni.isBlank()) {
            throw new NegocioException("El DNI no puede estar vacio");
        }
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
