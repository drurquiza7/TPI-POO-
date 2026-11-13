package ar.edu.unsada.stock.ui;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.repository.ProveedorRepository;
import ar.edu.unsada.stock.service.ArticuloService;
import ar.edu.unsada.stock.service.RubroService;

/**
 * Interfaz por consola. Solo lee datos y muestra resultados:
 * las reglas de negocio estan en los servicios.
 */
@Component
@Order(2)
public class MenuConsola implements CommandLineRunner {

    private final RubroService rubroService;
    private final ArticuloService articuloService;
    // Solo para mostrar los proveedores al cargar un articulo.
    // En la Etapa 2 se reemplaza por ProveedorService.
    private final ProveedorRepository proveedorRepository;

    private final Scanner scanner = new Scanner(System.in);

    public MenuConsola(RubroService rubroService, ArticuloService articuloService,
                       ProveedorRepository proveedorRepository) {
        this.rubroService = rubroService;
        this.articuloService = articuloService;
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public void run(String... args) {
        try {
            menuPrincipal();
        } catch (NoSuchElementException e) {
            // Se cerro la entrada estandar (Ctrl+D / Ctrl+Z)
            System.out.println("\nEntrada finalizada. Saliendo del sistema...");
        }
    }

    // =====================================================
    //                    MENU PRINCIPAL
    // =====================================================

    private void menuPrincipal() {
        int opcion;
        do {
            System.out.println();
            System.out.println("==============================================");
            System.out.println("   SISTEMA DE GESTION DE STOCK - MAYORISTA");
            System.out.println("==============================================");
            System.out.println(" 1. Gestion de Rubros");
            System.out.println(" 2. Gestion de Articulos");
            System.out.println(" 3. Gestion de Proveedores y Compras");
            System.out.println(" 4. Gestion de Clientes y Facturacion");
            System.out.println(" 0. Salir");
            opcion = leerEntero("Opcion: ");

            switch (opcion) {
                case 1 -> menuRubros();
                case 2 -> menuArticulos();
                case 3 -> System.out.println("Modulo en desarrollo - Etapa 2");
                case 4 -> System.out.println("Modulo en desarrollo - Etapa 3");
                case 0 -> System.out.println("Hasta luego!");
                default -> System.out.println("Opcion invalida.");
            }
        } while (opcion != 0);
    }

    // =====================================================
    //                        RUBROS
    // =====================================================

    private void menuRubros() {
        int opcion;
        do {
            System.out.println();
            System.out.println("------------ GESTION DE RUBROS ------------");
            System.out.println(" 1. Alta de rubro");
            System.out.println(" 2. Baja de rubro");
            System.out.println(" 3. Modificar rubro");
            System.out.println(" 4. Buscar rubro");
            System.out.println(" 5. Listar rubros");
            System.out.println(" 0. Volver");
            opcion = leerEntero("Opcion: ");

            // Cualquier error de negocio se muestra y se vuelve al submenu
            try {
                switch (opcion) {
                    case 1 -> altaRubro();
                    case 2 -> bajaRubro();
                    case 3 -> modificarRubro();
                    case 4 -> buscarRubro();
                    case 5 -> mostrarRubros(rubroService.listar());
                    case 0 -> { }
                    default -> System.out.println("Opcion invalida.");
                }
            } catch (NegocioException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    private void altaRubro() {
        System.out.println("-- Alta de rubro --");
        String codigo = leerTexto("Codigo: ");
        String nombre = leerTexto("Nombre: ");
        String descripcion = leerTexto("Descripcion: ");

        Rubro rubro = rubroService.alta(codigo, nombre, descripcion);
        System.out.println("Rubro creado: " + rubro);
    }

    private void bajaRubro() {
        System.out.println("-- Baja de rubro --");
        Rubro rubro = rubroService.buscarPorCodigo(leerTexto("Codigo del rubro: "));

        if (confirmar("Eliminar el rubro " + rubro + "?")) {
            rubroService.baja(rubro.getCodigo());
            System.out.println("Rubro eliminado.");
        } else {
            System.out.println("Operacion cancelada.");
        }
    }

    private void modificarRubro() {
        System.out.println("-- Modificar rubro (Enter deja el valor actual) --");
        Rubro rubro = rubroService.buscarPorCodigo(leerTexto("Codigo del rubro: "));

        String nombre = leerTextoOpcional("Nombre", rubro.getNombre());
        String descripcion = leerTextoOpcional("Descripcion", rubro.getDescripcion());

        rubroService.modificar(rubro.getCodigo(), nombre, descripcion);
        System.out.println("Rubro modificado.");
    }

    private void buscarRubro() {
        System.out.println("-- Buscar rubro --");
        System.out.println(" 1. Por codigo");
        System.out.println(" 2. Por nombre");
        int opcion = leerEntero("Opcion: ");

        if (opcion == 1) {
            Rubro rubro = rubroService.buscarPorCodigo(leerTexto("Codigo: "));
            mostrarRubros(List.of(rubro));
        } else if (opcion == 2) {
            mostrarRubros(rubroService.buscarPorNombre(leerTexto("Nombre contiene: ")));
        } else {
            System.out.println("Opcion invalida.");
        }
    }

    private void mostrarRubros(List<Rubro> rubros) {
        if (rubros.isEmpty()) {
            System.out.println("No hay rubros para mostrar.");
            return;
        }
        System.out.println();
        System.out.printf("%-8s %-22s %s%n", "CODIGO", "NOMBRE", "DESCRIPCION");
        System.out.println("-".repeat(65));
        for (Rubro rubro : rubros) {
            System.out.printf("%-8s %-22s %s%n", rubro.getCodigo(), rubro.getNombre(), rubro.getDescripcion());
        }
        System.out.println("Total: " + rubros.size());
    }

    // =====================================================
    //                       ARTICULOS
    // =====================================================

    private void menuArticulos() {
        int opcion;
        do {
            System.out.println();
            System.out.println("----------- GESTION DE ARTICULOS -----------");
            System.out.println(" 1. Alta de articulo");
            System.out.println(" 2. Baja de articulo");
            System.out.println(" 3. Modificar articulo");
            System.out.println(" 4. Buscar articulo");
            System.out.println(" 5. Listar articulos");
            System.out.println(" 6. Actualizar stock");
            System.out.println(" 7. Reporte: articulos en stock minimo");
            System.out.println(" 0. Volver");
            opcion = leerEntero("Opcion: ");

            try {
                switch (opcion) {
                    case 1 -> altaArticulo();
                    case 2 -> bajaArticulo();
                    case 3 -> modificarArticulo();
                    case 4 -> buscarArticulo();
                    case 5 -> mostrarArticulos(articuloService.listar());
                    case 6 -> actualizarStock();
                    case 7 -> reporteStockMinimo();
                    case 0 -> { }
                    default -> System.out.println("Opcion invalida.");
                }
            } catch (NegocioException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    private void altaArticulo() {
        System.out.println("-- Alta de articulo --");
        String codigo = leerTexto("Codigo: ");
        String descripcion = leerTexto("Descripcion: ");

        mostrarRubros(rubroService.listar());
        String codigoRubro = leerTexto("Codigo del rubro: ");

        mostrarProveedores();
        String codigoProveedor = leerTexto("Codigo del proveedor (Enter = sin proveedor): ");

        int stockActual = leerEntero("Stock inicial: ");
        int stockMinimo = leerEntero("Stock minimo: ");
        BigDecimal precio = leerDecimal("Precio unitario: ");

        Articulo articulo = articuloService.alta(codigo, descripcion, codigoRubro, codigoProveedor,
                stockActual, stockMinimo, precio);
        System.out.println("Articulo creado: " + articulo);
    }

    private void bajaArticulo() {
        System.out.println("-- Baja de articulo --");
        Articulo articulo = articuloService.buscarPorCodigo(leerTexto("Codigo del articulo: "));

        if (confirmar("Eliminar el articulo " + articulo + "?")) {
            articuloService.baja(articulo.getCodigo());
            System.out.println("Articulo eliminado.");
        } else {
            System.out.println("Operacion cancelada.");
        }
    }

    private void modificarArticulo() {
        System.out.println("-- Modificar articulo (Enter deja el valor actual) --");
        Articulo articulo = articuloService.buscarPorCodigo(leerTexto("Codigo del articulo: "));

        String descripcion = leerTextoOpcional("Descripcion", articulo.getDescripcion());
        String codigoRubro = leerTextoOpcional("Codigo de rubro", articulo.getRubro().getCodigo());

        String proveedorActual = articulo.getProveedor() == null ? "" : articulo.getProveedor().getCodigo();
        String codigoProveedor = leerTextoOpcional("Codigo de proveedor", proveedorActual);

        int stockMinimo = leerEnteroOpcional("Stock minimo", articulo.getStockMinimo());
        BigDecimal precio = leerDecimalOpcional("Precio unitario", articulo.getPrecioUnitario());

        articuloService.modificar(articulo.getCodigo(), descripcion, codigoRubro, codigoProveedor,
                stockMinimo, precio);
        System.out.println("Articulo modificado.");
    }

    private void buscarArticulo() {
        System.out.println("-- Buscar articulo --");
        System.out.println(" 1. Por codigo");
        System.out.println(" 2. Por descripcion");
        int opcion = leerEntero("Opcion: ");

        if (opcion == 1) {
            Articulo articulo = articuloService.buscarPorCodigo(leerTexto("Codigo: "));
            mostrarArticulos(List.of(articulo));
        } else if (opcion == 2) {
            mostrarArticulos(articuloService.buscarPorDescripcion(leerTexto("Descripcion contiene: ")));
        } else {
            System.out.println("Opcion invalida.");
        }
    }

    private void actualizarStock() {
        System.out.println("-- Actualizar stock --");
        Articulo articulo = articuloService.buscarPorCodigo(leerTexto("Codigo del articulo: "));
        System.out.println(articulo.getDescripcion() + " | stock actual: " + articulo.getStockActual()
                + " | minimo: " + articulo.getStockMinimo());

        System.out.println(" 1. Ingreso de mercaderia (suma)");
        System.out.println(" 2. Egreso de mercaderia (resta)");
        System.out.println(" 3. Ajuste manual (fijar valor)");
        int opcion = leerEntero("Opcion: ");

        // Si el stock queda en el minimo, el aviso lo imprime el observador
        Articulo actualizado;
        switch (opcion) {
            case 1 -> actualizado = articuloService.aumentarStock(articulo.getCodigo(), leerEntero("Cantidad a ingresar: "));
            case 2 -> actualizado = articuloService.descontarStock(articulo.getCodigo(), leerEntero("Cantidad a retirar: "));
            case 3 -> actualizado = articuloService.ajustarStock(articulo.getCodigo(), leerEntero("Nuevo stock: "));
            default -> {
                System.out.println("Opcion invalida.");
                return;
            }
        }
        System.out.println("Stock actualizado. Nuevo stock de " + actualizado.getCodigo()
                + ": " + actualizado.getStockActual());
    }

    private void reporteStockMinimo() {
        List<Articulo> articulos = articuloService.listarConStockMinimoAlcanzado();
        System.out.println();
        System.out.println("REPORTE: ARTICULOS CON STOCK MENOR O IGUAL AL MINIMO");
        if (articulos.isEmpty()) {
            System.out.println("No hay articulos en stock minimo.");
            return;
        }
        System.out.printf("%-8s %-30s %7s %7s %8s  %s%n",
                "CODIGO", "DESCRIPCION", "STOCK", "MINIMO", "FALTAN", "PROVEEDOR");
        System.out.println("-".repeat(95));
        for (Articulo a : articulos) {
            // Unidades que faltan para volver a superar el minimo
            int faltante = a.getStockMinimo() - a.getStockActual() + 1;
            System.out.printf("%-8s %-30s %7d %7d %8d  %s%n",
                    a.getCodigo(), a.getDescripcion(), a.getStockActual(), a.getStockMinimo(),
                    faltante, nombreProveedor(a));
        }
        System.out.println("Total: " + articulos.size());
    }

    private void mostrarArticulos(List<Articulo> articulos) {
        if (articulos.isEmpty()) {
            System.out.println("No hay articulos para mostrar.");
            return;
        }
        System.out.println();
        System.out.printf("%-8s %-30s %-15s %7s %7s %13s  %s%n",
                "CODIGO", "DESCRIPCION", "RUBRO", "STOCK", "MINIMO", "PRECIO", "PROVEEDOR");
        System.out.println("-".repeat(115));
        for (Articulo a : articulos) {
            // El asterisco marca los que estan en stock minimo
            String marca = a.estaEnStockMinimo() ? " *" : "";
            System.out.printf("%-8s %-30s %-15s %7d %7d %13.2f  %s%s%n",
                    a.getCodigo(), a.getDescripcion(), a.getRubro().getNombre(),
                    a.getStockActual(), a.getStockMinimo(), a.getPrecioUnitario(),
                    nombreProveedor(a), marca);
        }
        System.out.println("Total: " + articulos.size() + "   (* = en stock minimo)");
    }

    private void mostrarProveedores() {
        List<Proveedor> proveedores = proveedorRepository.findAll();
        System.out.println();
        System.out.printf("%-8s %s%n", "CODIGO", "PROVEEDOR");
        System.out.println("-".repeat(45));
        for (Proveedor proveedor : proveedores) {
            System.out.printf("%-8s %s%n", proveedor.getCodigo(), proveedor.getRazonSocial());
        }
    }

    private String nombreProveedor(Articulo articulo) {
        return articulo.getProveedor() == null ? "(sin proveedor)" : articulo.getProveedor().getRazonSocial();
    }

    // =====================================================
    //               LECTURA DE DATOS POR TECLADO
    // =====================================================
    // Siempre se lee la linea completa con nextLine() para no tener
    // problemas con el salto de linea que deja nextInt().

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    private int leerEntero(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un numero entero valido.");
            }
        }
    }

    private BigDecimal leerDecimal(String mensaje) {
        while (true) {
            // Se acepta coma o punto como separador decimal
            String texto = leerTexto(mensaje).replace(',', '.');
            try {
                return new BigDecimal(texto);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un importe valido (ej: 1500.50).");
            }
        }
    }

    private String leerTextoOpcional(String etiqueta, String valorActual) {
        String texto = leerTexto(etiqueta + " [" + valorActual + "]: ");
        return texto.isEmpty() ? valorActual : texto;
    }

    private int leerEnteroOpcional(String etiqueta, int valorActual) {
        while (true) {
            String texto = leerTexto(etiqueta + " [" + valorActual + "]: ");
            if (texto.isEmpty()) {
                return valorActual;
            }
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un numero entero valido.");
            }
        }
    }

    private BigDecimal leerDecimalOpcional(String etiqueta, BigDecimal valorActual) {
        while (true) {
            String texto = leerTexto(etiqueta + " [" + valorActual + "]: ").replace(',', '.');
            if (texto.isEmpty()) {
                return valorActual;
            }
            try {
                return new BigDecimal(texto);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un importe valido (ej: 1500.50).");
            }
        }
    }

    private boolean confirmar(String pregunta) {
        String respuesta = leerTexto(pregunta + " (S/N): ");
        return respuesta.equalsIgnoreCase("S");
    }
}
