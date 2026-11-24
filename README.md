# RUDRI - Sistema de Gestión de Pedidos y Cocina (KDS)

Sistema web full-stack para la gestión de salón (mesas), carta digital, toma de pedidos, monitor de cocina (KDS) y administración (CRUD) para el restaurante **RUDRI**.

## Arquitectura del Proyecto

- **`Backend/`**: API REST con **Java 17**, **Spring Boot 4**, **Spring Data JPA**, **BCrypt (`spring-security-crypto`)**, **Bean Validation** y **MySQL / H2 (tests)**.
- **`frontend/`**: Aplicación SPA con **Angular 21**, Signals, **Vitest** para pruebas unitarias y sincronización en tiempo real mediante `BroadcastChannel`.

---

## Requisitos Previos

- **Java JDK 17+** y Maven (o usar el wrapper `./mvnw` incluido)
- **Node.js 20+** y `npm`
- **MySQL 8+** (para ejecución en entorno local de desarrollo/producción)

---

## Configuración de Base de Datos (MySQL)

1. Crea la base de datos en MySQL:
   ```sql
   CREATE DATABASE IF NOT EXISTS db_rudri CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Define la variable de entorno `DB_PASSWORD` con la contraseña de tu usuario `root` de MySQL (configurada en `Backend/src/main/resources/application.properties`):
   ```powershell
   $env:DB_PASSWORD="tu_password_mysql"
   ```

---

## Ejecución del Proyecto

### 1. Levantar el Backend (`http://localhost:8080`)

```powershell
cd Backend
.\mvnw.cmd spring-boot:run
```

Principales endpoints disponibles:
- `POST /api/auth/login` — Autenticación con validación anti-SQLi (OWASP), verificación BCrypt y emisión de token de sesión.
- `GET | POST | PUT | DELETE /api/mesas` y `PUT /api/mesas/{id}/ocupar` / `PUT /api/mesas/{id}/liberar` — Gestión y estado de mesas.
- `GET | POST | PUT | DELETE /api/categorias` — Gestión de categorías de la carta.
- `GET | POST | PUT | DELETE /api/productos` — Gestión de productos con normalización y redondeo decimal.
- `GET | POST /api/pedidos` y `PATCH /api/pedidos/{id}/estado` — Creación de pedidos con detalle de ítems y máquina de estados KDS (`ENVIADO -> EN_PREPARACION -> DESPACHADO`).
- `GET | POST | DELETE /api/usuarios` y `PATCH /api/usuarios/{id}/estado` — Administración de usuarios y roles.

### 2. Levantar el Frontend (`http://localhost:4200`)

```powershell
cd frontend
npm install
npm start
```

---

## Ejecución de Pruebas Automatizadas

### Backend (JUnit 5 + H2 en memoria)
```powershell
cd Backend
.\mvnw.cmd test
```

### Frontend (Vitest + Angular TestBed)
```powershell
cd frontend
npm test -- --watch=false
```
