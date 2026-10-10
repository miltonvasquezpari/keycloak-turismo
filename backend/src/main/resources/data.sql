-- Datos semilla de Fase 4.1: 5 destinos + 6 tours.
-- Se ejecuta en cada arranque (spring.sql.init.mode=always). Para que sea
-- re-ejecutable, limpia primero las tablas que gestiona y reinicia secuencias.
-- Las reservas y pagos (Fase 4.2) tendran su propia estrategia.

TRUNCATE TABLE tour, destino RESTART IDENTITY CASCADE;

INSERT INTO destino (nombre, region) VALUES
  ('Cusco',        'Cusco'),
  ('Ica',          'Ica'),
  ('Arequipa',     'Arequipa'),
  ('Puno',         'Puno'),
  ('La Libertad',  'La Libertad');

INSERT INTO tour (codigo, nombre, precio, duracion_horas, cupo_diario, activo, destino_id) VALUES
  ('T-MP-01', 'Machu Picchu Clasico',               480.00, 12, 40, true, 1),
  ('T-VS-02', 'Valle Sagrado de los Incas',         220.00, 10, 30, true, 1),
  ('T-M7-03', 'Montaña de 7 Colores',               180.00,  9, 25, true, 1),
  ('T-IB-04', 'Islas Ballestas + Huacachina',       150.00,  8, 35, true, 2),
  ('T-CO-05', 'Cañon del Colca 2D/1N',              320.00, 36, 20, true, 3),
  ('T-CC-06', 'Chan Chan y Huanchaco',              140.00,  7, 30, true, 5);
