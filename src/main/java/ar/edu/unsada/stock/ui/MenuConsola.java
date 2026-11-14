package ar.edu.unsada.stock.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ar.edu.unsada.stock.exception.NegocioException;
import ar.edu.unsada.stock.model.Articulo;
import ar.edu.unsada.stock.model.CompraProveedor;
import ar.edu.unsada.stock.model.DetalleCompra;
import ar.edu.unsada.stock.model.Proveedor;
import ar.edu.unsada.stock.model.Rubro;
import ar.edu.unsada.stock.service.ArticuloService;
import ar.edu.unsada.stock.service.CompraService;
import ar.edu.unsada.stock.service.ProveedorService;
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
    private final ProveedorService proveedorService;
    private final CompraService compraService;

    private final Scanner scanner = new Scanner(System.in);

    public MenuConsola(RubroService rubroService, ArticuloService articuloService,
                       ProveedorService proveedorService, CompraService compraService) {
        this.rubroService = rubroService;
        this.articuloService = articuloService;
        this.proveedorService = proveedorService;
        this.compraService = compraService;
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
                case 3 -> menuProveedoresYCompras();
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

        mostrarProveedores(proveedorService.listar());
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

    private String nombreProveedor(Articulo articulo) {
        return articulo.getProveedor() == null ? "(sin proveedor)" : articulo.getProveedor().getRazonSocial();
    }

    // =====================================================
    //                 PROVEEDORES Y COMPRAS
    // =====================================================

    private void menuProveedoresYCompras() {
        int opcion;
        do {
            System.out.println();
            System.out.println("------ GESTION DE PROVEEDORES Y COMPRAS ------");
            System.out.println(" 1. Alta de proveedor");
            System.out.println(" 2. Baja de proveedor");
            System.out.println(" 3. Modificar proveedor");
            System.out.println(" 4. Buscar proveedor");
            System.out.println(" 5. Listar proveedores");
            System.out.println(" 6. Registrar compra a proveedor");
            System.out.println(" 7. Listar compras");
            System.out.println(" 8. Listar compras por rango de fechas");
            System.out.println(" 0. Volver");
            opcion = leerEntero("Opcion: ");

            try {
                switch (opcion) {
                    case 1 -> altaProveedor();
                    case 2 -> bajaProveedor();
                    case 3 -> modificarProveedor();
                    case 4 -> buscarProveedor();
                    case 5 -> mostrarProveedores(proveedorService.listar());
                    case 6 -> registrarCompra();
                    case 7 -> mostrarCompras(compraService.listar());
                    case 8 -> listarComprasPorFechas();
                    case 0 -> { }
                    default -> System.out.println("Opcion invalida.");
                }
            } catch (NegocioException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    // ---------------- Proveedores ----------------

    private void altaProveedor() {
        System.out.println("-- Alta de proveedor --");
        String codigo = leerTexto("Codigo: ");
        String cuit = leerTexto("CUIT: ");
        String razonSocial = leerTexto("Razon social: ");
        String direccion = leerTexto("Direccion: ");
        String telefono = leerTexto("Telefono: ");
        String email = leerTexto("Email: ");

        Proveedor proveedor = proveedorService.alta(codigo, cuit, razonSocial, direccion, telefono, email);
        System.out.println("Proveedor creado: " + proveedor);
    }

    private void bajaProveedor() {
        System.out.println("-- Baja de proveedor --");
        Proveedor proveedor = proveedorService.buscarPorCodigo(leerTexto("Codigo del proveedor: "));

        if (confirmar("Eliminar el proveedor " + proveedor + "?")) {
            proveedorService.baja(proveedor.getCodigo());
            System.out.println("Proveedor eliminado.");
        } else {
            System.out.println("Operacion cancelada.");
        }
    }

    private void modificarProveedor() {
        System.out.println("-- Modificar proveedor (Enter deja el valor actual) --");
        Proveedor proveedor = proveedorService.buscarPorCodigo(leerTexto("Codigo del proveedor: "));

        String cuit = leerTextoOpcional("CUIT", proveedor.getCuit());
        String razonSocial = leerTextoOpcional("Razon social", proveedor.getRazonSocial());
        String direccion = leerTextoOpcional("Direccion", proveedor.getDireccion());
        String telefono = leerTextoOpcional("Telefono", proveedor.getTelefono());
        String email = leerTextoOpcional("Email", proveedor.getEmail());

        proveedorService.modificar(proveedor.getCodigo(), cuit, razonSocial, direccion, telefono, email);
        System.out.println("Proveedor modificado.");
    }

    private void buscarProveedor() {
        System.out.println("-- Buscar proveedor --");
        System.out.println(" 1. Por codigo");
        System.out.println(" 2. Por razon social");
        int opcion = leerEntero("Opcion: ");

        if (opcion == 1) {
            Proveedor proveedor = proveedorService.buscarPorCodigo(leerTexto("Codigo: "));
            mostrarProveedores(List.of(proveedor));
        } else if (opcion == 2) {
            mostrarProveedores(proveedorService.buscarPorRazonSocial(leerTexto("Razon social contiene: ")));
        } else {
            System.out.println("Opcion invalida.");
        }
    }

    private void mostrarProveedores(List<Proveedor> proveedores) {
        if (proveedores.isEmpty()) {
            System.out.println("No hay proveedores para mostrar.");
            return;
        }
        System.out.println();
        System.out.printf("%-8s %-30s %-14s %-16s %s%n", "CODIGO", "RAZON SOCIAL", "CUIT", "TELEFONO", "EMAIL");
        System.out.println("-".repeat(100));
        for (Proveedor p : proveedores) {
            System.out.printf("%-8s %-30s %-14s %-16s %s%n",
                    p.getCodigo(), p.getRazonSocial(), p.getCuit(), p.getTelefono(), p.getEmail());
        }
        System.out.println("Total: " + proveedores.size());
    }

    // ---------------- Compras ----------------

    private void registrarCompra() {
        System.out.println("-- Registrar compra a proveedor --");

        // El codigo y el proveedor se validan antes de cargar renglones,
        // para no hacerle tipear todo al usuario y que falle al final
        String codigo = compraService.validarCodigoDisponible(leerTexto("Codigo de la compra: "));

        mostrarProveedores(proveedorService.listar());
        Proveedor proveedor = proveedorService.buscarPorCodigo(leerTexto("Codigo del proveedor: "));

        LocalDate fecha = leerFechaOpcional("Fecha de la compra (AAAA-MM-DD)", LocalDate.now());

        List<DetalleCompra> renglones = cargarRenglonesDeCompra();
        if (renglones.isEmpty()) {
            System.out.println("No se cargaron articulos. Compra cancelada.");
            return;
        }

        System.out.println();
        System.out.println("RESUMEN DE LA COMPRA " + codigo);
        System.out.println("Proveedor: " + proveedor + "   Fecha: " + fecha);
        mostrarRenglonesDeCompra(renglones);
        System.out.printf("TOTAL: %.2f%n", calcularTotal(renglones));

        if (!confirmar("Confirma la compra?")) {
            System.out.println("Compra cancelada. No se modifico el stock.");
            return;
        }

        CompraProveedor compra = compraService.registrar(codigo, proveedor.getCodigo(), fecha, renglones);
        System.out.printf("Compra %s registrada. Total: %.2f. Stock actualizado.%n",
                compra.getCodigo(), compra.getTotal());
    }

    // Pide articulos en un bucle hasta que el usuario deja el codigo vacio
    private List<DetalleCompra> cargarRenglonesDeCompra() {
        List<DetalleCompra> renglones = new ArrayList<>();
        System.out.println("Carga de articulos (Enter en el codigo para terminar)");

        while (true) {
            String codigoArticulo = leerTexto("Codigo del articulo: ");
            if (codigoArticulo.isEmpty()) {
                break;
            }

            // Un error en un renglon no cancela toda la compra: se avisa y se pide otro
            try {
                Articulo articulo = articuloService.buscarPorCodigo(codigoArticulo);
                if (yaEstaCargado(renglones, articulo)) {
                    System.out.println("Ese articulo ya esta en la compra.");
                    continue;
                }
                System.out.println("  " + articulo.getDescripcion() + " | stock actual: " + articulo.getStockActual());

                int cantidad = leerEntero("  Cantidad: ");
                BigDecimal costo = leerDecimal("  Costo unitario: ");
                if (cantidad <= 0 || costo.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("La cantidad y el costo deben ser mayores a cero. Renglon descartado.");
                    continue;
                }

                DetalleCompra renglon = new DetalleCompra(articulo, cantidad, costo);
                renglones.add(renglon);
                System.out.printf("  Renglon agregado. Subtotal: %.2f | Total parcial: %.2f%n",
                        renglon.getSubtotal(), calcularTotal(renglones));
            } catch (NegocioException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
        return renglones;
    }

    private boolean yaEstaCargado(List<DetalleCompra> renglones, Articulo articulo) {
        for (DetalleCompra renglon : renglones) {
            if (renglon.getArticulo().getCodigo().equals(articulo.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal calcularTotal(List<DetalleCompra> renglones) {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCompra renglon : renglones) {
            total = total.add(renglon.getSubtotal());
        }
        return total;
    }

    private void listarComprasPorFechas() {
        System.out.println("-- Compras por rango de fechas --");
        LocalDate fechaDesde = leerFecha("Fecha desde (AAAA-MM-DD): ");
        LocalDate fechaHasta = leerFecha("Fecha hasta (AAAA-MM-DD): ");

        mostrarCompras(compraService.listarPorFechas(fechaDesde, fechaHasta));
    }

    // Muestra cada compra con su cabecera y sus renglones
    private void mostrarCompras(List<CompraProveedor> compras) {
        if (compras.isEmpty()) {
            System.out.println("No hay compras para mostrar.");
            return;
        }
        BigDecimal totalGeneral = BigDecimal.ZERO;
        for (CompraProveedor compra : compras) {
            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("COMPRA " + compra.getCodigo() + "   Fecha: " + compra.getFechaCompra()
                    + "   Proveedor: " + compra.getProveedor());
            mostrarRenglonesDeCompra(compra.getDetalles());
            System.out.printf("%65s %13.2f%n", "TOTAL:", compra.getTotal());
            totalGeneral = totalGeneral.add(compra.getTotal());
        }
        System.out.println();
        System.out.printf("Compras: %d   Importe total: %.2f%n", compras.size(), totalGeneral);
    }

    private void mostrarRenglonesDeCompra(List<DetalleCompra> renglones) {
        System.out.printf("%-8s %-30s %9s %15s %13s%n",
                "CODIGO", "DESCRIPCION", "CANTIDAD", "COSTO UNITARIO", "SUBTOTAL");
        System.out.println("-".repeat(80));
        for (DetalleCompra renglon : renglones) {
            System.out.printf("%-8s %-30s %9d %15.2f %13.2f%n",
                    renglon.getArticulo().getCodigo(), renglon.getArticulo().getDescripcion(),
                    renglon.getCantidad(), renglon.getCostoUnitario(), renglon.getSubtotal());
        }
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

    private LocalDate leerFecha(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                System.out.println("Fecha invalida. Use el formato AAAA-MM-DD (ej: 2026-11-13).");
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

    private LocalDate leerFechaOpcional(String etiqueta, LocalDate valorActual) {
        while (true) {
            String texto = leerTexto(etiqueta + " [" + valorActual + "]: ");
            if (texto.isEmpty()) {
                return valorActual;
            }
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                System.out.println("Fecha invalida. Use el formato AAAA-MM-DD (ej: 2026-11-13).");
            }
        }
    }

    private boolean confirmar(String pregunta) {
        String respuesta = leerTexto(pregunta + " (S/N): ");
        return respuesta.equalsIgnoreCase("S");
    }
}
