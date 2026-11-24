package backend.demo;

import backend.demo.controller.authController;
import backend.demo.controller.categoriaController;
import backend.demo.controller.mesaController;
import backend.demo.controller.pedidoController;
import backend.demo.controller.productoController;
import backend.demo.controller.usuarioController;
import backend.demo.dto.MesaResponse;
import backend.demo.dto.OcuparMesaRequest;
import backend.demo.dto.loginRequest;
import backend.demo.model.EstadoMesa;
import backend.demo.model.categoriaModel;
import backend.demo.model.detallePedidoModel;
import backend.demo.model.mesaModel;
import backend.demo.model.pedidoModel;
import backend.demo.model.productoModel;
import backend.demo.model.rolModel;
import backend.demo.model.usuarioModel;
import backend.demo.repository.mesaRepository;
import backend.demo.repository.pedidoRepository;
import backend.demo.repository.productoRepository;
import backend.demo.repository.usuarioRepository;
import backend.demo.services.usuarioServices;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DemoApplicationTests {

    @Autowired
    private usuarioServices usuarioService;

    @Autowired
    private authController authCtrl;

    @Autowired
    private mesaController mesaCtrl;

    @Autowired
    private productoController productoCtrl;

    @Autowired
    private pedidoController pedidoCtrl;

    @Autowired
    private categoriaController categoriaCtrl;

    @Autowired
    private usuarioController usuarioCtrl;

    @Autowired
    private usuarioRepository usuarioRepo;

    @Autowired
    private mesaRepository mesaRepo;

    @Autowired
    private productoRepository productoRepo;

    @Autowired
    private pedidoRepository pedidoRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private rolModel rolMozo;
    private categoriaModel categoriaComida;

    @BeforeEach
    void setUp() {
        pedidoRepo.deleteAll();
        productoRepo.deleteAll();
        mesaRepo.deleteAll();
        usuarioRepo.deleteAll();

        rolMozo = new rolModel();
        rolMozo.setNombre("MOZO");
        rolMozo.setDescripcion("Atención de mesas");
        entityManager.persist(rolMozo);

        categoriaComida = new categoriaModel();
        categoriaComida.setNombre("COMIDA");
        categoriaComida.setDescripcion("Platos principales");
        categoriaComida.setActiva(true);
        entityManager.persist(categoriaComida);

        usuarioModel mozoActivo = new usuarioModel();
        mozoActivo.setRol(rolMozo);
        mozoActivo.setNombreCompleto("Juan Pérez");
        mozoActivo.setNombreUsuario("mozo01");
        mozoActivo.setClaveHash(passwordEncoder.encode("Mozo12345"));
        mozoActivo.setActivo(true);
        usuarioRepo.save(mozoActivo);

        usuarioModel mozoInactivo = new usuarioModel();
        mozoInactivo.setRol(rolMozo);
        mozoInactivo.setNombreCompleto("Usuario Inactivo");
        mozoInactivo.setNombreUsuario("inactivo01");
        mozoInactivo.setClaveHash(passwordEncoder.encode("Clave12345"));
        mozoInactivo.setActivo(false);
        usuarioRepo.save(mozoInactivo);

        entityManager.flush();
    }

    @Test
    @DisplayName("1. Contexto de Spring Boot carga correctamente con base de datos en memoria")
    void contextLoads() {
        assertNotNull(usuarioService);
        assertNotNull(authCtrl);
        assertNotNull(mesaCtrl);
        assertNotNull(productoCtrl);
        assertNotNull(pedidoCtrl);
    }

    @Test
    @DisplayName("2. Auth: Login exitoso con credenciales válidas y verificación BCrypt")
    void loginExitosoConCredencialesValidas() {
        loginRequest req = new loginRequest();
        req.setNombreUsuario("mozo01");
        req.setClave("Mozo12345");

        ResponseEntity<?> resp = authCtrl.login(req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals("Inicio de sesión exitoso", body.get("mensaje"));
        assertEquals("mozo01", body.get("nombreUsuario"));
        assertEquals("MOZO", body.get("rol"));
        assertNotNull(body.get("token"));
    }

    @Test
    @DisplayName("3. Auth: Rechaza contraseña incorrecta y usuario inactivo con HTTP 401")
    void loginRechazaClaveIncorrectaYUsuarioInactivo() {
        loginRequest reqClaveMala = new loginRequest();
        reqClaveMala.setNombreUsuario("mozo01");
        reqClaveMala.setClave("Incorrecta999");

        ResponseEntity<?> resp1 = authCtrl.login(reqClaveMala);
        assertEquals(HttpStatus.UNAUTHORIZED, resp1.getStatusCode());

        loginRequest reqInactivo = new loginRequest();
        reqInactivo.setNombreUsuario("inactivo01");
        reqInactivo.setClave("Clave12345");

        ResponseEntity<?> resp2 = authCtrl.login(reqInactivo);
        assertEquals(HttpStatus.UNAUTHORIZED, resp2.getStatusCode());
    }

    @Test
    @DisplayName("4. Auth: Rechaza intentos de Inyección SQL y campos vacíos")
    void loginRechazaInyeccionSQLYCamposVacios() {
        assertTrue(usuarioService.contienePatronSQLi("' OR '1'='1"));
        assertTrue(usuarioService.contienePatronSQLi("admin'--"));
        assertTrue(usuarioService.contienePatronSQLi("1; DROP TABLE usuario"));
        assertTrue(usuarioService.contienePatronSQLi("UNION SELECT * FROM usuario"));
        assertFalse(usuarioService.contienePatronSQLi("mozo01"));

        loginRequest reqSqli = new loginRequest();
        reqSqli.setNombreUsuario("' OR '1'='1");
        reqSqli.setClave("123456");

        ResponseEntity<?> respSqli = authCtrl.login(reqSqli);
        assertEquals(HttpStatus.BAD_REQUEST, respSqli.getStatusCode());

        loginRequest reqVacio = new loginRequest();
        reqVacio.setNombreUsuario("   ");
        reqVacio.setClave("");
        assertEquals(HttpStatus.BAD_REQUEST, authCtrl.login(reqVacio).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, authCtrl.login(null).getStatusCode());
    }

    @Test
    @DisplayName("5. Mesas CRUD y Ocupación: Crear, listar, ocupar, liberar, actualizar, eliminar y validar duplicados/límites")
    void gestionCompletaDeMesasYValidaciones() {
        mesaModel nueva = new mesaModel();
        nueva.setNumero(1);
        nueva.setCapacidad(4);
        nueva.setActiva(true);

        ResponseEntity<?> respCrear = mesaCtrl.crearMesa(nueva);
        assertEquals(HttpStatus.OK, respCrear.getStatusCode());
        assertEquals(1, mesaCtrl.listarMesas().size());

        // Intento de duplicado de número de mesa -> 409 Conflict
        mesaModel duplicada = new mesaModel();
        duplicada.setNumero(1);
        duplicada.setCapacidad(6);
        assertEquals(HttpStatus.CONFLICT, mesaCtrl.crearMesa(duplicada).getStatusCode());

        // Número de mesa inválido (<= 0) o capacidad fuera de rango (> 50) -> 400 Bad Request
        mesaModel invalidaNum = new mesaModel();
        invalidaNum.setNumero(0);
        invalidaNum.setCapacidad(4);
        assertEquals(HttpStatus.BAD_REQUEST, mesaCtrl.crearMesa(invalidaNum).getStatusCode());

        mesaModel invalidaCap = new mesaModel();
        invalidaCap.setNumero(2);
        invalidaCap.setCapacidad(100);
        assertEquals(HttpStatus.BAD_REQUEST, mesaCtrl.crearMesa(invalidaCap).getStatusCode());

        mesaModel creada = (mesaModel) respCrear.getBody();
        assertNotNull(creada);

        // Ocupar y liberar mesa
        MesaResponse ocupada = mesaCtrl.ocuparMesa(creada.getIdMesa(), new OcuparMesaRequest("Familia Gómez", 3));
        assertEquals(EstadoMesa.OCUPADA, ocupada.estado());
        assertEquals(3, ocupada.numeroPersonas());

        MesaResponse liberada = mesaCtrl.liberarMesa(creada.getIdMesa());
        assertEquals(EstadoMesa.LIBRE, liberada.estado());
        assertEquals(0, liberada.numeroPersonas());

        // Actualizar y eliminar
        creada.setCapacidad(8);
        ResponseEntity<?> respAct = mesaCtrl.actualizarMesa(creada.getIdMesa(), creada);
        assertEquals(HttpStatus.OK, respAct.getStatusCode());
        assertEquals(8, ((mesaModel) respAct.getBody()).getCapacidad());

        assertEquals(HttpStatus.NO_CONTENT, mesaCtrl.eliminarMesa(creada.getIdMesa()).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, mesaCtrl.eliminarMesa(9999).getStatusCode());
    }

    @Test
    @DisplayName("8. Categorías, Usuarios y Detalle de Pedido: CRUD y persistencia de ítems")
    void gestionDeCategoriasUsuariosYDetallesDePedido() {
        // Categorías CRUD
        categoriaModel nuevaCat = new categoriaModel();
        nuevaCat.setNombre("  bebidas  ");
        nuevaCat.setDescripcion("Refrescos y jarras");
        ResponseEntity<?> respCat = categoriaCtrl.crearCategoria(nuevaCat);
        assertEquals(HttpStatus.OK, respCat.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, categoriaCtrl.crearCategoria(nuevaCat).getStatusCode());

        // Usuarios CRUD
        ResponseEntity<?> respUser = usuarioCtrl.crearUsuario(Map.of(
                "nombreCompleto", "Chef Roberto",
                "nombreUsuario", "cocina01",
                "clave", "Cocina12345",
                "rol", "COCINA"
        ));
        assertEquals(HttpStatus.OK, respUser.getStatusCode());
        assertTrue(usuarioCtrl.listarUsuarios().size() >= 3);

        // Pedido con detalles
        detallePedidoModel item = new detallePedidoModel();
        item.setIdProducto(1);
        item.setNombreProducto("CEVICHE CLÁSICO");
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("28.00"));

        pedidoModel pedidoConDetalle = new pedidoModel();
        pedidoConDetalle.setIdMesa(2);
        pedidoConDetalle.setDetalles(java.util.List.of(item));

        ResponseEntity<?> respPedido = pedidoCtrl.crearPedido(pedidoConDetalle);
        assertEquals(HttpStatus.OK, respPedido.getStatusCode());
        pedidoModel pedidoGuardado = (pedidoModel) respPedido.getBody();
        assertNotNull(pedidoGuardado);
        assertEquals(2, pedidoGuardado.getTotalArticulos());
        assertEquals(new BigDecimal("56.00"), pedidoGuardado.getTotalMonto());
        assertEquals(1, pedidoGuardado.getDetalles().size());
    }

    @Test
    @DisplayName("6. Productos CRUD: Normalización a mayúsculas, redondeo decimal y validación de precio")
    void gestionDeProductosYValidaciones() {
        productoModel prod = new productoModel();
        prod.setCategoria(categoriaComida);
        prod.setNombre("  ceviche clásico  ");
        prod.setPrecio(new BigDecimal("28.50"));
        prod.setDisponible(true);

        ResponseEntity<?> resp = productoCtrl.crearProducto(prod);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        productoModel guardado = (productoModel) resp.getBody();
        assertNotNull(guardado);
        assertEquals("CEVICHE CLÁSICO", guardado.getNombre());
        assertEquals(new BigDecimal("28.50"), guardado.getPrecio());

        // Precio inválido (<= 0) o nombre vacío -> 400 Bad Request
        productoModel precioCero = new productoModel();
        precioCero.setCategoria(categoriaComida);
        precioCero.setNombre("Plato Inválido");
        precioCero.setPrecio(BigDecimal.ZERO);
        assertEquals(HttpStatus.BAD_REQUEST, productoCtrl.crearProducto(precioCero).getStatusCode());

        productoModel sinNombre = new productoModel();
        sinNombre.setCategoria(categoriaComida);
        sinNombre.setNombre("   ");
        sinNombre.setPrecio(new BigDecimal("15.00"));
        assertEquals(HttpStatus.BAD_REQUEST, productoCtrl.crearProducto(sinNombre).getStatusCode());
    }

    @Test
    @DisplayName("7. Pedidos KDS: Creación, límite de observación (200 chars) y máquina de estados")
    void flujoDePedidosYTransicionesDeEstado() {
        pedidoModel pedido = new pedidoModel();
        pedido.setIdMesa(1);
        pedido.setNumeroMesa(1);
        pedido.setIdUsuario(1);
        pedido.setNombreMozo("Juan Pérez");
        pedido.setTotalArticulos(2);
        pedido.setTotalMonto(new BigDecimal("56.00"));
        pedido.setObservacionGeneral("X".repeat(200)); // Límite exacto permitido (200)

        ResponseEntity<?> respCrear = pedidoCtrl.crearPedido(pedido);
        assertEquals(HttpStatus.OK, respCrear.getStatusCode());
        pedidoModel creado = (pedidoModel) respCrear.getBody();
        assertNotNull(creado);
        assertEquals("ENVIADO", creado.getEstado());

        // Observación de 201 caracteres -> 400 Bad Request
        pedidoModel obsExcesiva = new pedidoModel();
        obsExcesiva.setIdMesa(1);
        obsExcesiva.setTotalArticulos(1);
        obsExcesiva.setObservacionGeneral("X".repeat(201));
        assertEquals(HttpStatus.BAD_REQUEST, pedidoCtrl.crearPedido(obsExcesiva).getStatusCode());

        // Transición inválida: ENVIADO -> DESPACHADO (saltando EN_PREPARACION) -> 400 Bad Request
        ResponseEntity<?> saltoInvalido = pedidoCtrl.cambiarEstado(
                creado.getIdPedido(), Map.of("estado", "DESPACHADO"));
        assertEquals(HttpStatus.BAD_REQUEST, saltoInvalido.getStatusCode());

        // Transición válida 1: ENVIADO -> EN_PREPARACION
        ResponseEntity<?> paso1 = pedidoCtrl.cambiarEstado(
                creado.getIdPedido(), Map.of("estado", "EN_PREPARACION"));
        assertEquals(HttpStatus.OK, paso1.getStatusCode());
        assertEquals("EN_PREPARACION", ((pedidoModel) paso1.getBody()).getEstado());

        // Transición válida 2: EN_PREPARACION -> DESPACHADO
        ResponseEntity<?> paso2 = pedidoCtrl.cambiarEstado(
                creado.getIdPedido(), Map.of("estado", "DESPACHADO"));
        assertEquals(HttpStatus.OK, paso2.getStatusCode());
        assertEquals("DESPACHADO", ((pedidoModel) paso2.getBody()).getEstado());

        // Transición inválida en reversa: DESPACHADO -> EN_PREPARACION -> 400 Bad Request
        ResponseEntity<?> reversaInvalida = pedidoCtrl.cambiarEstado(
                creado.getIdPedido(), Map.of("estado", "EN_PREPARACION"));
        assertEquals(HttpStatus.BAD_REQUEST, reversaInvalida.getStatusCode());
    }
}
