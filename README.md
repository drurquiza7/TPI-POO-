# Sistema de Gestion de Stock - Mayorista de computacion

TP Integrador de Programacion Orientada a Objetos (UNSAdA).
Java 17 + Spring Boot 3 + Spring Data JPA + H2 en archivo. Interfaz por consola.

## Como ejecutar

Requisitos: JDK 17 o superior y Maven.

```
mvn clean package
java -jar target/gestion-stock-1.0.0.jar
```

Tambien se puede usar `mvn spring-boot:run` o ejecutar `StockApplication` desde VS Code.

La base se guarda en `./data/stockdb.mv.db`. La primera vez se cargan datos de prueba.
Para empezar de cero alcanza con borrar la carpeta `data/`.

## Estructura

```
gestion-stock/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/ar/edu/unsada/stock/
    │   │   ├── StockApplication.java
    │   │   ├── config/      DataSeeder
    │   │   ├── exception/   NegocioException
    │   │   ├── model/       Rubro, Proveedor, Articulo, Cliente,
    │   │   │                Factura, DetalleFactura, CompraProveedor, DetalleCompra
    │   │   ├── observer/    ObservadorStock, SujetoStock, AlertaStockConsola
    │   │   ├── repository/  un repositorio JpaRepository por entidad
    │   │   ├── service/     RubroService, ArticuloService
    │   │   └── ui/          MenuConsola
    │   └── resources/application.properties
    └── test/java/ar/edu/unsada/stock/service/ArticuloServiceTest.java
```

## Patron Observer

- `SujetoStock` (sujeto): lo implementa `ArticuloService`, que es el unico lugar
  por donde cambia el stock.
- `ObservadorStock` (observador): lo implementa `AlertaStockConsola`.
- Cada vez que se guarda un articulo con `stockActual <= stockMinimo`,
  el servicio llama a `notificarObservadores()` y la alerta sale por consola al instante.

## Etapas

| Etapa | Modulo | Estado |
|-------|--------|--------|
| 1 | Base del proyecto, Rubros, Articulos, Observer | Listo |
| 2 | Proveedores y Compras | Pendiente |
| 3 | Clientes y Facturacion | Pendiente |

Para las etapas 2 y 3: las entidades y repositorios ya estan creados.
Para mover stock usar siempre `articuloService.aumentarStock()` (compras) y
`articuloService.descontarStock()` (facturas), asi la alerta de stock minimo
funciona sin escribir nada extra.
