-- Datos semilla: 5 destinos + 6 tours + 4 reservas (seccion 1.8 del PLAN).
-- Se ejecuta en cada arranque (spring.sql.init.mode=always). Es
-- re-ejecutable: limpia TODAS las tablas gestionadas y reinicia secuencias.
-- Nota: cualquier reserva creada via API durante la sesion se pierde en el
-- siguiente arranque. Aceptable para una demo de seguridad.

TRUNCATE TABLE pago, reserva, tour, destino RESTART IDENTITY CASCADE;

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

-- Reservas: 2 PENDIENTE, 1 CONFIRMADA, 1 PAGADA. 2 agencias (LIM01, CUS01).
INSERT INTO reserva (id, tour_id, fecha_tour, personas, monto_total,
                     estado, cliente_username, agencia_id, creada_en) VALUES
  ('R-1001', 1, DATE '2026-11-15', 2,  960.00, 'PENDIENTE',  'carlos.premium', 'LIM01', NOW()),
  ('R-1002', 2, DATE '2026-11-20', 1,  220.00, 'PENDIENTE',  'ana.cliente',    'CUS01', NOW()),
  ('R-1003', 6, DATE '2026-10-30', 3,  420.00, 'CONFIRMADA', 'ana.cliente',    'LIM01', NOW()),
  ('R-1004', 5, DATE '2026-10-20', 2,  640.00, 'PAGADA',     'carlos.premium', 'CUS01', NOW());
