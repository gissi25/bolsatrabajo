-- ============================================================
-- SEED DATA - BOLSA DE TRABAJO
-- Datos iniciales para probar el Servicio 1
-- Ejecutar DESPUES de esquema_tablas.sql
-- ============================================================
-- INSTRUCCIONES: phpMyAdmin > SQL > Delimitador: ;
-- ============================================================

-- 1. CATEGORIA_HABILIDAD (AUTO_INCREMENT)
INSERT INTO CATEGORIA_HABILIDAD (ID_CATEGORIA_HABILIDAD, NOMBRE_CATEGORIA) VALUES
(1, 'Desarrollo de Software y Logica'),
(2, 'Infraestructura y Cloud Computing'),
(3, 'Redes y Telecomunicaciones'),
(4, 'Bases de Datos'),
(5, 'Herramientas de Inteligencia Artificial'),
(6, 'Idiomas');

-- 2. GENERO (AUTO_INCREMENT)
INSERT INTO GENERO (ID_GENERO, NOMBRE_GENERO) VALUES
(1, 'Femenino'),
(2, 'Masculino'),
(3, 'No binario'),
(4, 'Prefiero no decirlo'),
(5, 'Otro');

-- 3. TIPO_DOCUMENTO (AUTO_INCREMENT)
INSERT INTO TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO, NOMBRE_TIPO) VALUES
(1, 'DUI'),
(2, 'NIT'),
(3, 'Pasaporte');

-- 4. DEPARTAMENTO (AUTO_INCREMENT)
INSERT INTO DEPARTAMENTO (ID_DEPARTAMENTO, NOMBRE_DEPARTAMENTO) VALUES
(1, 'Ahuachapán'),
(2, 'Santa Ana'),
(3, 'Sonsonate'),
(4, 'Chalatenango'),
(5, 'Cuscatlán'),
(6, 'San Salvador'),
(7, 'La Libertad'),
(8, 'La Paz'),
(9, 'Cabañas'),
(10, 'San Vicente'),
(11, 'Usulután'),
(12, 'San Miguel'),
(13, 'Morazán'),
(14, 'La Unión');

-- 5. GRADO_ACADEMICO (AUTO_INCREMENT)
INSERT INTO GRADO_ACADEMICO (ID_GRADO_ACADEMICO, NOMBRE_GRADO) VALUES
(1, 'Bachiller'),
(2, 'Técnico Superior'),
(3, 'Profesorado'),
(4, 'Licenciatura'),
(5, 'Ingeniería'),
(6, 'Maestría'),
(7, 'Doctorado');

-- 6. RED_SOCIAL (AUTO_INCREMENT)
INSERT INTO RED_SOCIAL (ID_RED_SOCIAL, NOMBRE_RED) VALUES
(1, 'GitHub'),
(2, 'Facebook'),
(3, 'LinkedIn'),
(4, 'Discord'),
(5, 'X (Twitter)'),
(6, 'Instagram');

-- 7. TIPO_CERTIFICACION (AUTO_INCREMENT)
INSERT INTO TIPO_CERTIFICACION (ID_TIPO_CERTIFICACION, NOMBRE_TIPO) VALUES
(1, 'Certificacion Profesional'),
(2, 'Diplomado'),
(3, 'Curso'),
(4, 'Idioma'),
(5, 'Seminario');

-- 8. INSTITUCION (PK manual)
INSERT INTO INSTITUCION (ID_INSTITUCION, NOMBRE_INSTITUCION) VALUES
('INS001', 'Universidad de El Salvador (UES)'),
('INS002', 'ITCA'),
('INS003', 'Fundacion Gloria de Kriete'),
('INS004', 'Universidad Don Bosco'),
('INS005', 'Universidad Jose Matias Delgado'),
('INS006', 'CASATIC');

-- 9. MUNICIPIO (PK compuesta: ID_DEPARTAMENTO, ID_MUNICIPIO)
INSERT INTO MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_MUNICIPIO) VALUES
(1, 1, 'Ahuachapán Norte'),
(1, 2, 'Ahuachapán Centro'),
(1, 3, 'Ahuachapán Sur'),
(2, 1, 'Santa Ana Norte'),
(2, 2, 'Santa Ana Centro'),
(2, 3, 'Santa Ana Este'),
(2, 4, 'Santa Ana Oeste'),
(3, 1, 'Sonsonate Norte'),
(3, 2, 'Sonsonate Centro'),
(3, 3, 'Sonsonate Este'),
(3, 4, 'Sonsonate Oeste'),
(4, 1, 'Chalatenango Norte'),
(4, 2, 'Chalatenango Centro'),
(4, 3, 'Chalatenango Sur'),
(5, 1, 'Cuscatlán Norte'),
(5, 2, 'Cuscatlán Sur'),
(6, 1, 'San Salvador Norte'),
(6, 2, 'San Salvador Oeste'),
(6, 3, 'San Salvador Este'),
(6, 4, 'San Salvador Centro'),
(6, 5, 'San Salvador Sur'),
(7, 1, 'La Libertad Norte'),
(7, 2, 'La Libertad Centro'),
(7, 3, 'La Libertad Oeste'),
(7, 4, 'La Libertad Este'),
(7, 5, 'La Libertad Costa'),
(7, 6, 'La Libertad Sur'),
(8, 1, 'La Paz Oeste'),
(8, 2, 'La Paz Centro'),
(8, 3, 'La Paz Este'),
(9, 1, 'Cabañas Este'),
(9, 2, 'Cabañas Oeste'),
(10, 1, 'San Vicente Norte'),
(10, 2, 'San Vicente Sur'),
(11, 1, 'Usulután Norte'),
(11, 2, 'Usulután Este'),
(11, 3, 'Usulután Oeste'),
(12, 1, 'San Miguel Norte'),
(12, 2, 'San Miguel Centro'),
(12, 3, 'San Miguel Oeste'),
(13, 1, 'Morazán Norte'),
(13, 2, 'Morazán Sur'),
(14, 1, 'La Unión Norte'),
(14, 2, 'La Unión Sur');

-- 10. DISTRITO (PK compuesta: ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
-- Solo algunos distritos clave para que funcionen las FK de EMPRESA
INSERT INTO DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO, NOMBRE_DISTRITO) VALUES
(1, 1, 1, 'Atiquizaya'),
(1, 1, 2, 'El Refugio'),
(1, 2, 1, 'Ahuachapán'),
(1, 3, 1, 'Guaymango'),
(2, 1, 1, 'Masahuat'),
(2, 2, 1, 'Santa Ana'),
(2, 3, 1, 'Coatepeque'),
(2, 4, 1, 'Candelaria de la Frontera'),
(3, 1, 1, 'Juayúa'),
(3, 2, 1, 'Sonsonate'),
(3, 3, 1, 'Armenia'),
(3, 4, 1, 'Acajutla'),
(4, 1, 1, 'Citalá'),
(4, 2, 1, 'Agua Caliente'),
(4, 3, 1, 'Arcatao'),
(5, 1, 1, 'Suchitoto'),
(5, 2, 1, 'Cojutepeque'),
(6, 1, 1, 'Aguilares'),
(6, 2, 1, 'Apopa'),
(6, 3, 1, 'Ilopango'),
(6, 3, 3, 'Soyapango'),
(6, 4, 5, 'San Salvador'),
(6, 4, 1, 'Ayutuxtepeque'),
(6, 4, 3, 'Cuscatancingo'),
(6, 5, 1, 'San Marcos'),
(7, 1, 1, 'Quezaltepeque'),
(7, 2, 1, 'San Juan Opico'),
(7, 3, 1, 'Colón'),
(7, 4, 1, 'Antiguo Cuscatlán'),
(7, 5, 1, 'Chiltiupán'),
(7, 6, 1, 'Comasagua'),
(7, 6, 2, 'Santa Tecla'),
(8, 1, 1, 'Cuyultitán'),
(8, 2, 1, 'El Rosario'),
(8, 3, 1, 'San Juan Nonualco'),
(9, 1, 1, 'Sensuntepeque'),
(9, 2, 1, 'Ilobasco'),
(10, 1, 1, 'Apastepeque'),
(10, 2, 1, 'San Vicente'),
(11, 1, 1, 'Santiago de María'),
(11, 2, 1, 'Usulután'),
(11, 3, 1, 'Jiquilisco'),
(12, 1, 1, 'Ciudad Barrios'),
(12, 2, 1, 'San Miguel'),
(12, 3, 1, 'Chinameca'),
(13, 1, 1, 'Arambala'),
(13, 2, 1, 'Chilanga'),
(14, 1, 1, 'Anamorós'),
(14, 2, 1, 'Conchagua'),
(14, 2, 4, 'La Unión');

-- 11. HABILIDAD (PK compuesta: ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)
INSERT INTO HABILIDAD (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, NOMBRE_HABILIDAD) VALUES
(1, 'H01', 'Programación en Python'),
(1, 'H02', 'Desarrollo en C++'),
(1, 'H03', 'Diseño y consumo de APIs'),
(2, 'H01', 'Despliegue de proyectos en Google Cloud'),
(2, 'H02', 'Administración de sistemas Linux'),
(2, 'H03', 'Gestión de entornos en Vercel y Netlify'),
(3, 'H01', 'Configuración de topologías en Cisco Packet Tracer'),
(3, 'H02', 'Cálculo y diseño de Subnetting'),
(3, 'H03', 'Análisis de tráfico de red'),
(4, 'H01', 'Diseño de modelos relacionales (SQL)'),
(4, 'H02', 'Integración y gestión con Supabase'),
(4, 'H03', 'Manejo de bases de datos NoSQL'),
(5, 'H01', 'Integracion de Gemini API y Google AI Studio'),
(5, 'H02', 'Ingenieria de prompts avanzados'),
(5, 'H03', 'Automatizacion de procesos con IA'),
(6, 'H01', 'Ingles'),
(6, 'H02', 'Frances'),
(6, 'H03', 'Portugues');

-- 12. EMPRESA (PK manual: NIT)
INSERT INTO EMPRESA (NIT, ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID, NOMBRE_EMPRESA, CONTACTO_DIRECTO) VALUES
('06141234560101', 7, 4, 1, 'Applaudo Studios', '2200-0001'),
('06141234560102', 7, 6, 2, 'Elaniin', '2200-0002'),
('06141234560103', 6, 4, 5, 'Creativa Consultores', '2200-0003'),
('06141234560104', 6, 4, 1, 'TELUS International El Salvador', '2200-0004'),
('06141234560105', 6, 4, 3, 'Accedo Technologies', '2200-0005'),
('06141234560106', 14, 2, 4, 'INNOVATEC', '2200-0006'),
('06141234560107', 6, 3, 3, 'Gravity 4', '2200-0007'),
('06141234560108', 6, 5, 1, 'Sysdatec', '2200-0008'),
('06141234560109', 6, 2, 1, 'SAGACI', '2200-0009'),
('06141234560110', 2, 2, 1, 'Tech Americas', '2200-0010');

-- 13. OFERTA_ACADEMICA (PK manual)
INSERT INTO OFERTA_ACADEMICA (ID_OFERTA_ACADEMICA, ID_GRADO_ACADEMICO, ID_INSTITUCION) VALUES
('OFA01', 2, 'INS002'),
('OFA02', 5, 'INS001'),
('OFA03', 4, 'INS001'),
('OFA04', 6, 'INS002'),
('OFA05', 7, 'INS001');

-- 14. USUARIO (AUTO_INCREMENT)
INSERT INTO USUARIO (ID_USUARIO, USERNAME, PASSWORD, ROL) VALUES
(1, 'postulante', '12345678', 'postulante'),
(2, 'empresa', '12345678', 'gerente de empresa');

-- ============================================================
-- FIN: 14 tablas con datos iniciales
-- ============================================================
