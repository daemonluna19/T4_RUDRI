import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { App } from './app';
import { AuthService } from './services/auth.service';
import { ProductoService } from './services/producto.service';
import { PedidoService } from './services/pedido.service';
import { MesaService } from './services/mesa.service';
import { CategoriaService } from './services/categoria.service';
import { authGuard } from './guards/auth.guard';
import { API_CONFIG } from './services/api.config';
import { CantidadStepperComponent } from './components/cantidad-stepper/cantidad-stepper.component';
import { MesaCardComponent } from './components/mesa-card/mesa-card.component';
import { ConfirmacionComponent } from './pages/confirmacion/confirmacion.component';
import { MenuComponent } from './pages/menu/menu.component';
import { CocinaComponent } from './pages/cocina/cocina.component';
import { CrudComponent } from './pages/crud/crud.component';
import { LoginComponent } from './pages/login/login.component';

describe('RUDRI Frontend - Pruebas Unitarias', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [
        App,
        CantidadStepperComponent,
        MesaCardComponent,
        ConfirmacionComponent,
        MenuComponent,
        CocinaComponent,
        CrudComponent,
        LoginComponent
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('1. Debería instanciar la aplicación raíz RUDRI', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('2. AuthService debería rechazar intento de Inyección SQL en usuario o clave', () => {
    const authService = TestBed.inject(AuthService);
    const vectoresSQLi = [
      "' OR '1'='1",
      "admin'--",
      "1; DROP TABLE usuario",
      "' UNION SELECT * FROM usuario --"
    ];

    for (const vector of vectoresSQLi) {
      expect(authService.contienePatronSQLi(vector)).toBe(true);
      let mensajeError = '';
      authService.login({ nombreUsuario: vector, clave: '123' }).subscribe({
        error: (err: Error) => {
          mensajeError = err.message;
        }
      });
      expect(mensajeError).toContain('OWASP');
    }
  });

  it('3. AuthService debería rechazar credenciales vacías o solo con espacios en blanco', () => {
    const authService = TestBed.inject(AuthService);
    let errorVacio = '';
    authService.login({ nombreUsuario: '   ', clave: '' }).subscribe({
      error: (err: Error) => {
        errorVacio = err.message;
      }
    });
    expect(errorVacio).toContain('ingresa tu nombre de usuario y contraseña');
  });

  it('4. AuthService debería autenticar exitosamente vía HTTP Backend y calcular nombreCorto', () => {
    const authService = TestBed.inject(AuthService);
    expect(authService.estaAutenticado()).toBe(false);
    expect(authService.nombreCorto()).toBe('Invitado');

    let usuarioLogueado: unknown = null;
    authService.login({ nombreUsuario: 'mozo01', clave: 'Mozo12345' }).subscribe((res) => {
      usuarioLogueado = res;
    });

    const req = httpMock.expectOne(API_CONFIG.endpoints.authLogin);
    expect(req.request.method).toBe('POST');
    req.flush({
      idUsuario: 1,
      nombreCompleto: 'Juan Pérez',
      nombreUsuario: 'mozo01',
      rol: 'MOZO',
      mensaje: 'Inicio de sesión exitoso'
    });

    expect(usuarioLogueado).toBeTruthy();
    expect(authService.estaAutenticado()).toBe(true);
    expect(authService.nombreCorto()).toBe('Juan');

    authService.logout();
    expect(authService.estaAutenticado()).toBe(false);
    expect(authService.nombreCorto()).toBe('Invitado');
  });

  it('5. AuthService debería aplicar fallback local cuando el servidor está apagado y rechazar HTTP 401', () => {
    const authService = TestBed.inject(AuthService);

    // Caso 1: Backend devuelve 401 explícito
    let error401 = '';
    authService.login({ nombreUsuario: 'mozo01', clave: 'ClaveErronea' }).subscribe({
      error: (err: Error) => {
        error401 = err.message;
      }
    });
    const req401 = httpMock.expectOne(API_CONFIG.endpoints.authLogin);
    req401.flush({ mensaje: 'Credenciales incorrectas' }, { status: 401, statusText: 'Unauthorized' });
    expect(error401).toBe('Credenciales incorrectas');

    // Caso 2: Servidor apagado (error de red status 0) con usuario fallback válido
    authService.login({ nombreUsuario: 'cocina01', clave: 'Cocina12345' }).subscribe();
    const reqOffline = httpMock.expectOne(API_CONFIG.endpoints.authLogin);
    reqOffline.error(new ProgressEvent('error'));
    expect(authService.estaAutenticado()).toBe(true);
    expect(authService.usuarioActual()?.rol).toBe('COCINA');
  });

  it('6. authGuard debería bloquear usuarios no autenticados redirigiendo a /login y permitir autenticados', () => {
    const authService = TestBed.inject(AuthService);
    authService.logout();

    const resultadoBloqueado = TestBed.runInInjectionContext(() =>
      authGuard({} as never, {} as never)
    );
    expect(resultadoBloqueado instanceof UrlTree).toBe(true);
    expect((resultadoBloqueado as UrlTree).toString()).toBe('/login');

    // Autenticar usuario y volver a evaluar guardia
    authService.usuarioActual.set({
      idUsuario: 1,
      nombreCompleto: 'Juan Pérez',
      nombreUsuario: 'mozo01',
      rol: 'MOZO',
      mensaje: 'OK'
    });
    const resultadoPermitido = TestBed.runInInjectionContext(() =>
      authGuard({} as never, {} as never)
    );
    expect(resultadoPermitido).toBe(true);

    // Restricción por rol: un MOZO intentando entrar a una ruta exclusiva de ADMINISTRADOR (/crud) es redirigido a /mesas
    const resultadoRolRestringido = TestBed.runInInjectionContext(() =>
      authGuard({ data: { roles: ['ADMINISTRADOR'] } } as never, {} as never)
    );
    expect(resultadoRolRestringido instanceof UrlTree).toBe(true);
    expect((resultadoRolRestringido as UrlTree).toString()).toBe('/mesas');
  });

  it('7. LoginComponent debería redirigir según el rol del usuario (MOZO -> /mesas, COCINA -> /cocina, ADMIN -> /crud)', () => {
    const router = TestBed.inject(Router);
    let rutaNavegada: string[] = [];
    router.navigate = ((commands: string[]) => {
      rutaNavegada = commands;
      return Promise.resolve(true);
    }) as never;

    const fixture = TestBed.createComponent(LoginComponent);
    const comp = fixture.componentInstance;

    comp.autocompletar('admin01', 'Admin12345');
    comp.ingresar();
    const reqAdmin = httpMock.expectOne(API_CONFIG.endpoints.authLogin);
    reqAdmin.error(new ProgressEvent('error')); // Usa fallback local
    expect(rutaNavegada).toEqual(['/crud']);

    comp.autocompletar('cocina01', 'Cocina12345');
    comp.ingresar();
    const reqCocina = httpMock.expectOne(API_CONFIG.endpoints.authLogin);
    reqCocina.error(new ProgressEvent('error'));
    expect(rutaNavegada).toEqual(['/cocina']);
  });

  it('8. MesaService debería listar las 12 mesas iniciales y formatear correctamente números de 1 y 2 dígitos', () => {
    const mesaService = TestBed.inject(MesaService);
    expect(mesaService.mesas().length).toBe(12);
    expect(mesaService.mesas()[0].codigoFormateado).toBe('MESA 01');
    expect(mesaService.mesas()[9].codigoFormateado).toBe('MESA 10');
    expect(mesaService.mesas()[11].codigoFormateado).toBe('MESA 12');
  });

  it('9. MesaService debería rechazar número de mesa <= 0, capacidad fuera de rango (1..50) y números duplicados', () => {
    const mesaService = TestBed.inject(MesaService);

    let errNumero = '';
    mesaService.crearMesa({ numero: 0, capacidad: 4, activa: true }).subscribe({
      error: (e: Error) => (errNumero = e.message)
    });
    expect(errNumero).toContain('entero positivo');

    let errCapacidad = '';
    mesaService.crearMesa({ numero: 15, capacidad: 51, activa: true }).subscribe({
      error: (e: Error) => (errCapacidad = e.message)
    });
    expect(errCapacidad).toContain('entre 1 y 50');

    let errDuplicada = '';
    mesaService.crearMesa({ numero: 1, capacidad: 4, activa: true }).subscribe({
      error: (e: Error) => (errDuplicada = e.message)
    });
    expect(errDuplicada).toContain('Ya existe una mesa');
  });

  it('10. MesaCardComponent no debería emitir selección cuando la mesa está inactiva', () => {
    const fixture = TestBed.createComponent(MesaCardComponent);
    const comp = fixture.componentInstance;
    let emitida = false;
    comp.seleccionar.subscribe(() => {
      emitida = true;
    });

    comp.mesa = { idMesa: 1, numero: 1, codigoFormateado: 'MESA 01', capacidad: 4, activa: false };
    comp.alHacerClick();
    expect(emitida).toBe(false);

    comp.mesa = { idMesa: 1, numero: 1, codigoFormateado: 'MESA 01', capacidad: 4, activa: true };
    comp.alHacerClick();
    expect(emitida).toBe(true);
  });

  it('11. ProductoService debería normalizar nombres a mayúsculas, redondear precios y rechazar precios <= 0', () => {
    const productoService = TestBed.inject(ProductoService);

    let errPrecio = '';
    productoService
      .crearProducto({
        idCategoria: 1,
        nombre: 'Plato Prueba',
        precio: -5,
        disponible: true,
        imagenUrl: 'http://img'
      })
      .subscribe({ error: (e: Error) => (errPrecio = e.message) });
    expect(errPrecio).toContain('mayor a cero');

    let errNombre = '';
    productoService
      .crearProducto({
        idCategoria: 1,
        nombre: '   ',
        precio: 20,
        disponible: true,
        imagenUrl: 'http://img'
      })
      .subscribe({ error: (e: Error) => (errNombre = e.message) });
    expect(errNombre).toContain('obligatorio');

    let creadoNombre = '';
    let creadoPrecio = 0;
    productoService
      .crearProducto({
        idCategoria: 1,
        nombre: '  parihuela especial  ',
        precio: 35.556,
        disponible: true,
        imagenUrl: 'http://img'
      })
      .subscribe((prod) => {
        creadoNombre = prod.nombre;
        creadoPrecio = prod.precio;
      });

    const req = httpMock.expectOne(API_CONFIG.endpoints.productos);
    req.error(new ProgressEvent('error')); // Verifica fallback local

    expect(creadoNombre).toBe('PARIHUELA ESPECIAL');
    expect(creadoPrecio).toBe(35.56);
  });

  it('12. MenuComponent debería filtrar productos por categoría, subcategoría y texto de búsqueda', () => {
    const fixture = TestBed.createComponent(MenuComponent);
    const comp = fixture.componentInstance;

    // Por defecto: Categoría 1 (COMIDA), Subcategoría 'Mariscos'
    expect(comp.productosFiltrados().length).toBe(3);

    // Cambiar a categoría 2 (BEBIDAS) -> resetea subcategoría a 'Todos'
    comp.cambiarCategoria(2);
    expect(comp.subcategoriaActiva()).toBe('Todos');
    expect(comp.productosFiltrados().length).toBe(2);

    // Filtrar por texto de búsqueda
    comp.textoBusqueda.set('frozen');
    expect(comp.productosFiltrados().length).toBe(1);
    expect(comp.productosFiltrados()[0].nombre).toContain('LIMONADA FROZEN');
  });

  it('13. CantidadStepperComponent debería respetar los límites min (0), max (20) y estado deshabilitado', () => {
    const fixture = TestBed.createComponent(CantidadStepperComponent);
    const stepper = fixture.componentInstance;
    const valoresEmitidos: number[] = [];
    stepper.cambioCantidad.subscribe((v) => valoresEmitidos.push(v));

    // En límite inferior (0), disminuir no debe emitir
    stepper.cantidad = 0;
    stepper.min = 0;
    stepper.max = 20;
    stepper.disminuir();
    expect(valoresEmitidos.length).toBe(0);

    // En límite superior (20), aumentar no debe emitir
    stepper.cantidad = 20;
    stepper.aumentar();
    expect(valoresEmitidos.length).toBe(0);

    // Cuando está deshabilitado, no debe emitir
    stepper.cantidad = 5;
    stepper.deshabilitado = true;
    stepper.aumentar();
    stepper.disminuir();
    expect(valoresEmitidos.length).toBe(0);

    stepper.deshabilitado = false;
    stepper.aumentar();
    stepper.disminuir();
    expect(valoresEmitidos).toEqual([6, 4]);
  });

  it('14. PedidoService debería agregar, actualizar, eliminar con cantidad 0 y validar límites (1..20) y disponibilidad', () => {
    const productoService = TestBed.inject(ProductoService);
    const pedidoService = TestBed.inject(PedidoService);
    const ceviche = productoService.productos()[0];
    const sopaNoDisponible = productoService.productos()[2];

    const res1 = pedidoService.actualizarCantidadProducto(ceviche, 2);
    expect(res1.exito).toBe(true);
    expect(pedidoService.totalArticulosCarrito()).toBe(2);
    expect(pedidoService.totalMontoCarrito()).toBe(56.0);

    expect(pedidoService.actualizarCantidadProducto(ceviche, 20).exito).toBe(true);
    expect(pedidoService.totalArticulosCarrito()).toBe(20);

    expect(pedidoService.actualizarCantidadProducto(ceviche, 21).exito).toBe(false);
    expect(pedidoService.actualizarCantidadProducto(ceviche, -1).exito).toBe(false);
    expect(pedidoService.actualizarCantidadProducto(ceviche, 2.5).exito).toBe(false);

    const resNoDisp = pedidoService.actualizarCantidadProducto(sopaNoDisponible, 1);
    expect(resNoDisp.exito).toBe(false);
    expect(resNoDisp.mensaje).toContain('no está disponible');

    expect(pedidoService.actualizarCantidadProducto(ceviche, 0).exito).toBe(true);
    expect(pedidoService.totalArticulosCarrito()).toBe(0);
    expect(pedidoService.itemsEnCarrito().length).toBe(0);
  });

  it('15. PedidoService debería validar mesa seleccionada, carrito no vacío y límite de 200 caracteres en observación', () => {
    const productoService = TestBed.inject(ProductoService);
    const pedidoService = TestBed.inject(PedidoService);
    const mesaService = TestBed.inject(MesaService);
    const ceviche = productoService.productos()[0];

    // Sin mesa seleccionada -> error
    let errSinMesa = '';
    pedidoService.enviarPedidoACocina().subscribe({
      error: (e: Error) => (errSinMesa = e.message)
    });
    expect(errSinMesa).toContain('seleccionar una mesa');

    // Con mesa seleccionada pero carrito vacío -> error
    pedidoService.seleccionarMesa(mesaService.mesas()[11]); // Mesa 12
    let errCarritoVacio = '';
    pedidoService.enviarPedidoACocina().subscribe({
      error: (e: Error) => (errCarritoVacio = e.message)
    });
    expect(errCarritoVacio).toContain('al menos un plato');

    // Con plato en carrito pero observación de 201 caracteres -> error
    pedidoService.actualizarCantidadProducto(ceviche, 1);
    pedidoService.observacionGeneral.set('A'.repeat(201));
    let errObs = '';
    pedidoService.enviarPedidoACocina().subscribe({
      error: (e: Error) => (errObs = e.message)
    });
    expect(errObs).toContain('200 caracteres');

    // Con observación exacta de 200 caracteres -> éxito y formatea Mesa 12 sin cero extra en ConfirmacionComponent
    pedidoService.observacionGeneral.set('A'.repeat(200));
    const fixtureConf = TestBed.createComponent(ConfirmacionComponent);
    const compConf = fixtureConf.componentInstance;
    compConf.enviarPedido();

    const req = httpMock.expectOne(API_CONFIG.endpoints.pedidos);
    req.error(new ProgressEvent('error')); // Usa fallback local

    expect(compConf.mensajeExito()).toContain('(Mesa 12)');
    expect(pedidoService.totalArticulosCarrito()).toBe(0);
  });

  it('16. PedidoService y CocinaComponent deberían hacer cumplir la máquina de estados KDS (ENVIADO -> EN_PREPARACION -> DESPACHADO)', () => {
    const pedidoService = TestBed.inject(PedidoService);
    const pedidoInicial = pedidoService.pedidos()[0];
    expect(pedidoInicial.estado).toBe('ENVIADO');

    // Salto inválido directo de ENVIADO a DESPACHADO -> rechazado
    let errSalto = '';
    pedidoService.cambiarEstadoPedido(pedidoInicial.idPedido, 'DESPACHADO').subscribe({
      error: (e: Error) => (errSalto = e.message)
    });
    expect(errSalto).toContain('Transición no permitida');

    // Transición válida 1: ENVIADO -> EN_PREPARACION
    pedidoService.cambiarEstadoPedido(pedidoInicial.idPedido, 'EN_PREPARACION').subscribe();
    const req1 = httpMock.expectOne(`${API_CONFIG.endpoints.pedidos}/${pedidoInicial.idPedido}/estado`);
    req1.error(new ProgressEvent('error'));
    expect(pedidoService.pedidos()[0].estado).toBe('EN_PREPARACION');

    // Transición válida 2: EN_PREPARACION -> DESPACHADO
    pedidoService.cambiarEstadoPedido(pedidoInicial.idPedido, 'DESPACHADO').subscribe();
    const req2 = httpMock.expectOne(`${API_CONFIG.endpoints.pedidos}/${pedidoInicial.idPedido}/estado`);
    req2.error(new ProgressEvent('error'));
    expect(pedidoService.pedidos()[0].estado).toBe('DESPACHADO');
    // Un pedido DESPACHADO ya no debe aparecer en pendientes de cocina
    expect(
      pedidoService.pedidosPendientesCocina().some((p) => p.idPedido === pedidoInicial.idPedido)
    ).toBe(false);

    let errReversa = '';
    pedidoService.cambiarEstadoPedido(pedidoInicial.idPedido, 'EN_PREPARACION').subscribe({
      error: (e: Error) => (errReversa = e.message)
    });
    expect(errReversa).toContain('Transición no permitida');
  });

  it('17. LoginComponent debe asociar labels explícitos, autocomplete y aria-label en visor de clave', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const labelUsuario = el.querySelector('label[for="input-usuario"]');
    const inputUsuario = el.querySelector('#input-usuario') as HTMLInputElement;
    const labelClave = el.querySelector('label[for="input-clave"]');
    const inputClave = el.querySelector('#input-clave') as HTMLInputElement;
    const btnEye = el.querySelector('.btn-eye');

    expect(labelUsuario).toBeTruthy();
    expect(inputUsuario?.getAttribute('autocomplete')).toBe('username');
    expect(labelClave).toBeTruthy();
    expect(inputClave?.getAttribute('autocomplete')).toBe('current-password');
    expect(btnEye?.getAttribute('aria-label')).toContain('Mostrar u ocultar contraseña');
  });

  it('18. MesaCardComponent debe exponer aria-pressed y aria-label descriptivo para lectores de pantalla', () => {
    const fixture = TestBed.createComponent(MesaCardComponent);
    const comp = fixture.componentInstance;
    comp.mesa = { idMesa: 3, numero: 3, codigoFormateado: 'MESA 03', capacidad: 6, activa: true };
    comp.seleccionada = true;
    fixture.detectChanges();

    const btn = fixture.nativeElement.querySelector('button.mesa-tile') as HTMLButtonElement;
    expect(btn.getAttribute('aria-pressed')).toBe('true');
    expect(btn.getAttribute('aria-label')).toContain('MESA 03');
    expect(btn.getAttribute('aria-label')).toContain('6 personas');
  });

  it('19. MenuComponent y CantidadStepperComponent deben incluir aria-label en controles interactivos', () => {
    const fixtureMenu = TestBed.createComponent(MenuComponent);
    fixtureMenu.detectChanges();
    httpMock.match(() => true).forEach((req) => req.error(new ProgressEvent('error')));
    const menuEl: HTMLElement = fixtureMenu.nativeElement;

    const selectSubcat = menuEl.querySelector('select.subcat-select');
    const inputSearch = menuEl.querySelector('#input-buscar-plato');
    expect(selectSubcat?.getAttribute('aria-label')).toBe('Filtrar por subcategoría');
    expect(inputSearch?.getAttribute('aria-label')).toContain('Buscar plato');

    const fixtureStep = TestBed.createComponent(CantidadStepperComponent);
    fixtureStep.detectChanges();
    const stepBtns = fixtureStep.nativeElement.querySelectorAll('button.step-btn');
    expect(stepBtns[0]?.getAttribute('aria-label')).toBe('Disminuir cantidad');
    expect(stepBtns[1]?.getAttribute('aria-label')).toBe('Aumentar cantidad');
  });

  it('20. CrudComponent debe exponer role="tablist", aria-selected y labels asociados por id', () => {
    const fixtureCrud = TestBed.createComponent(CrudComponent);
    fixtureCrud.detectChanges();
    httpMock.match(() => true).forEach((req) => req.error(new ProgressEvent('error')));
    const crudEl: HTMLElement = fixtureCrud.nativeElement;

    const tablist = crudEl.querySelector('[role="tablist"]');
    const tabs = crudEl.querySelectorAll('[role="tab"]');
    expect(tablist).toBeTruthy();
    expect(tabs.length).toBe(3);
    expect(tabs[0].getAttribute('aria-selected')).toBe('true');
    expect(crudEl.querySelector('label[for="crud-prod-nombre"]')).toBeTruthy();
    expect(crudEl.querySelector('#crud-prod-nombre')).toBeTruthy();
  });
});
