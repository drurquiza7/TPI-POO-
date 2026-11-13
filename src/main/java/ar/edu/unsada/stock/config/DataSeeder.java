package ar.edu.unsada.stock.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.Cliente;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.repository.ArticuloRepository;
import ar.edu.unsada.stock.repository.ClienteRepository;
import ar.edu.unsada.stock.repository.ProveedorRepository;
import ar.edu.unsada.stock.repository.RubroRepository;

/**
 * Carga datos de prueba (ficticios) la primera vez que se ejecuta el sistema.
 * Corre antes que el menu (Order 1) y no hace nada si la base ya tiene datos.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final RubroRepository rubroRepository;
    private final ProveedorRepository proveedorRepository;
    private final ArticuloRepository articuloRepository;
    private final ClienteRepository clienteRepository;

    public DataSeeder(RubroRepository rubroRepository, ProveedorRepository proveedorRepository,
                      ArticuloRepository articuloRepository, ClienteRepository clienteRepository) {
        this.rubroRepository = rubroRepository;
        this.proveedorRepository = proveedorRepository;
        this.articuloRepository = articuloRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void run(String... args) {
        if (rubroRepository.count() > 0) {
            return;
        }

        // ---- Rubros ----
        Rubro perifericos = rubroRepository.save(new Rubro("PER", "Perifericos", "Teclados, mouses, auriculares"));
        Rubro almacenamiento = rubroRepository.save(new Rubro("ALM", "Almacenamiento", "Discos SSD, HDD y pendrives"));
        Rubro monitores = rubroRepository.save(new Rubro("MON", "Monitores", "Monitores LED e IPS"));
        Rubro redes = rubroRepository.save(new Rubro("RED", "Redes", "Routers, switches y cables"));
        Rubro componentes = rubroRepository.save(new Rubro("COM", "Componentes", "Memorias, micros, fuentes"));

        // ---- Proveedores ----
        Proveedor tecnoSur = proveedorRepository.save(new Proveedor("PROV01", "30-11111111-1",
                "Distribuidora TecnoSur S.A.", "Av. Corrientes 1234, CABA", "011-4555-1000", "ventas@tecnosur.example"));
        Proveedor dataPampa = proveedorRepository.save(new Proveedor("PROV02", "30-22222222-2",
                "DataPampa Mayorista S.R.L.", "Mitre 450, Rosario", "0341-455-2000", "pedidos@datapampa.example"));
        Proveedor redLitoral = proveedorRepository.save(new Proveedor("PROV03", "30-33333333-3",
                "Red Litoral Insumos S.A.", "San Martin 820, San Pedro", "03329-42-3000", "info@redlitoral.example"));

        // ---- Articulos (algunos ya arrancan en stock minimo para probar el reporte) ----
        articuloRepository.save(new Articulo("ART001", "Teclado mecanico USB", perifericos, tecnoSur,
                40, 10, new BigDecimal("45000.00")));
        articuloRepository.save(new Articulo("ART002", "Mouse optico inalambrico", perifericos, tecnoSur,
                8, 15, new BigDecimal("18500.00")));
        articuloRepository.save(new Articulo("ART003", "Auriculares con microfono", perifericos, dataPampa,
                25, 8, new BigDecimal("32000.00")));
        articuloRepository.save(new Articulo("ART004", "Disco SSD 480GB SATA", almacenamiento, dataPampa,
                30, 10, new BigDecimal("52000.00")));
        articuloRepository.save(new Articulo("ART005", "Pendrive 64GB USB 3.0", almacenamiento, dataPampa,
                5, 20, new BigDecimal("9800.00")));
        articuloRepository.save(new Articulo("ART006", "Monitor LED 24 pulgadas", monitores, tecnoSur,
                12, 4, new BigDecimal("210000.00")));
        articuloRepository.save(new Articulo("ART007", "Router WiFi doble banda", redes, redLitoral,
                6, 6, new BigDecimal("68000.00")));
        articuloRepository.save(new Articulo("ART008", "Cable de red UTP Cat6 x 2m", redes, redLitoral,
                120, 30, new BigDecimal("3500.00")));
        articuloRepository.save(new Articulo("ART009", "Memoria RAM DDR4 8GB", componentes, tecnoSur,
                18, 6, new BigDecimal("38000.00")));
        articuloRepository.save(new Articulo("ART010", "Fuente 600W 80 Plus", componentes, redLitoral,
                9, 5, new BigDecimal("75000.00")));

        // ---- Clientes ----
        clienteRepository.save(new Cliente("CLI001", "Gomez", "Marcela", "30111222",
                "Anchorena 150, Baradero", "03329-48-1111", "mgomez@correo.example"));
        clienteRepository.save(new Cliente("CLI002", "Fernandez", "Lucas", "33444555",
                "Belgrano 980, San Pedro", "03329-42-2222", "lfernandez@correo.example"));
        clienteRepository.save(new Cliente("CLI003", "Ruiz", "Carolina", "28777888",
                "Rivadavia 45, Zarate", "03487-43-3333", "cruiz@correo.example"));

        System.out.println("Base vacia: se cargaron los datos de prueba iniciales.");
    }
}
