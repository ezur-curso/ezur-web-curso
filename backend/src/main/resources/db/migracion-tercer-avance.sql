-- Migración para bases PostgreSQL que ya existían antes del tercer avance
-- (con spring.jpa.hibernate.ddl-auto=validate, p. ej. perfil "prod", Hibernate no altera las tablas).
-- Ejecutar UNA vez, antes de desplegar. Con ddl-auto=update no hace falta.

CREATE TABLE IF NOT EXISTS tipos_servicio (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS dni       VARCHAR(8);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS nombres   VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS apellidos VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS usuario   VARCHAR(30);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS perfil    VARCHAR(20);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado    VARCHAR(20);

-- Los administradores que ya existen pasan a ser ADMINISTRADOR y ACTIVO.
-- Ajusta dni (8 dígitos únicos), nombres y apellidos de cada uno después.
UPDATE usuarios
SET usuario   = COALESCE(usuario, split_part(email, '@', 1)),
    dni       = COALESCE(dni, lpad(id::text, 8, '0')),
    nombres   = COALESCE(nombres, 'Administrador'),
    apellidos = COALESCE(apellidos, 'Ezur'),
    perfil    = 'ADMINISTRADOR',
    estado    = 'ACTIVO';

ALTER TABLE usuarios ALTER COLUMN dni       SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN nombres   SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN apellidos SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN usuario   SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN perfil    SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN estado    SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN email     DROP NOT NULL;

-- Solo se crean si no existe ya una restricción única sobre esas columnas
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_dni     ON usuarios (dni);
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_usuario ON usuarios (usuario);

-- La columna rol ya no se usa
ALTER TABLE usuarios DROP COLUMN IF EXISTS rol;

-- Espacio de cliente: solicitudes ligadas a la cuenta y fecha de alta
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS usuario_id BIGINT;
ALTER TABLE usuarios     ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP;
