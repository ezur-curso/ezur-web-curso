# Ezur: arquitectura en tres capas

```
Navegador ──► frontend (8080) ──► backend / API REST (8081) ──► PostgreSQL (5432)
              Thymeleaf + sesión     Spring Boot + JWT            base de datos
```

| Carpeta     | Qué es                                                              | Puerto |
|-------------|---------------------------------------------------------------------|--------|
| `frontend/` | Sitio público y paneles (Thymeleaf). No tiene base de datos.        | 8080   |
| `backend/`  | API REST: lógica, validaciones, correos, seguridad y acceso a datos. | 8081   |
| base de datos | PostgreSQL (o H2 en memoria en las pruebas).                      | 5432   |

El navegador solo habla con el frontend. El frontend llama a la API con el token del usuario;
ese token vive en la sesión del servidor y nunca llega al navegador.

## Cómo se autentica

1. El formulario de `/login` (cliente) o `/admin-login` (administrador) llega al frontend.
2. El frontend pide `POST /api/auth/login` a la API. La API valida la clave, el estado de la cuenta
   y que la cuenta pertenezca a ese acceso, y responde con un token JWT (HS256).
3. El administrador recibe un token "a medias" (`ROLE_PRE_2FA`): solo sirve para `/api/auth/2fa`.
   Al verificar el código TOTP, la API entrega el token completo (`ROLE_ADMIN`).
4. Cada petición del frontend a la API lleva `Authorization: Bearer <token>` y `X-Forwarded-For` con
   la IP real del visitante (el límite de intentos se aplica por persona, no por contenedor).

## API (resumen)

| Zona      | Rutas                                                                                   |
|-----------|-----------------------------------------------------------------------------------------|
| Pública   | `GET /api/publico/testimonios`, `GET /api/publico/tipos-servicio`, `POST /api/presupuestos`, `POST /api/contacto` |
| Acceso    | `POST /api/auth/login`, `/registro`, `/olvide`, `/restablecer`, `GET/POST /api/auth/2fa` |
| Cliente   | `/api/cliente/perfil`, `/solicitudes`, `/solicitudes/{id}/cancelar`, `/clave`          |
| Admin     | `/api/admin/presupuestos`, `/mensajes`, `/testimonios`, `/usuarios` (CRUD y estados)     |

Errores: `400 {"errores": {campo: mensaje}}` (validación), `401` sin token válido, `403` sin permiso,
`404`, `409` conflicto, `429` demasiados intentos.

## Variables de entorno

| Variable        | Quién la usa | Para qué                                                       |
|-----------------|--------------|----------------------------------------------------------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | backend | Conexión a PostgreSQL (por defecto `localhost:5432/ezur_db`, usuario `postgres`) |
| `JWT_SECRETO`   | backend      | Firma de los tokens, mínimo 32 caracteres (obligatoria con el perfil `prod`) |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_DESTINO` | backend | Correos de notificación y de recuperación de clave |
| `APP_BASE_URL`  | ambos        | URL pública del frontend (enlaces de los correos, cookie `secure`) |
| `API_URL`       | frontend     | Dónde está la API (`http://localhost:8081`; en contenedores `http://backend:8081`) |

Copia `.env.example` como `.env` y complétalo.

## Levantar cada capa (sin contenedores)

```bash
# 1) API  (necesita PostgreSQL y DB_PASSWORD)
cd backend && ./mvnw spring-boot:run

# 2) Sitio público
cd frontend && ./mvnw spring-boot:run
```

Con contenedores, `docker compose up --build` en la raíz levanta `db`, `backend` y `frontend`
(`docker-compose.yml` es un ejemplo; cada carpeta tiene su propio `Dockerfile`).

## Pruebas

```bash
cd backend && ./mvnw test    # API: acceso, 2FA, roles, registro, presupuestos, espacio del cliente
cd frontend && ./mvnw test   # sitio: páginas, login contra la API (doble), errores y redirecciones
```

## Estilos del frontend

`frontend/src/main/resources/static/css/output.css` se genera con Tailwind (ya está incluido):

```bash
cd frontend && npm install && npx tailwindcss -i ./tailwind-src/input.css -o ./src/main/resources/static/css/output.css
```

## Despliegue

Ahora son dos servicios web (frontend y API) más la base de datos. La API debe tener `JWT_SECRETO`,
`DB_*` y `SPRING_PROFILES_ACTIVE=prod` (con `ddl-auto=validate`: ejecuta antes
`backend/src/main/resources/db/migracion-tercer-avance.sql`). El frontend necesita `API_URL` apuntando a la API.
