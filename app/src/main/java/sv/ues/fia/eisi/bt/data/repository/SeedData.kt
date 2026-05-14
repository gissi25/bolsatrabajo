package sv.ues.fia.eisi.bt.data.repository

object SeedData {

    val TIPOS_DOCUMENTO = listOf(
        "DUI", "NIT", "Pasaporte"
    )

    val OFERTAS_ACADEMICAS = listOf(
        listOf("OFA01", 2, "INS002"),
        listOf("OFA02", 5, "INS001"),
        listOf("OFA03", 4, "INS001"),
        listOf("OFA04", 6, "INS002"),
        listOf("OFA05", 7, "INS001")
    )

    val DEPARTAMENTOS = listOf(
        "Ahuachapán", "Santa Ana", "Sonsonate", "Chalatenango",
        "Cuscatlán", "San Salvador", "La Libertad", "La Paz",
        "Cabañas", "San Vicente", "Usulután", "San Miguel",
        "Morazán", "La Unión"
    )

    val GENEROS = listOf(
        "Femenino", "Masculino", "No binario",
        "Prefiero no decirlo", "Otro"
    )

    val CATEGORIAS_HABILIDAD = listOf(
        "Desarrollo de Software y Lógica",
        "Infraestructura y Cloud Computing",
        "Redes y Telecomunicaciones",
        "Bases de Datos",
        "Herramientas de Inteligencia Artificial"
    )

    val GRADOS_ACADEMICOS = listOf(
        "Bachiller", "Técnico Superior", "Profesorado", "Licenciatura",
        "Ingeniería", "Maestría", "Doctorado"
    )

    val TIPOS_CERTIFICACION = listOf(
        "Certificacion Profesional", "Diplomado", "Curso", "Idioma", "Seminario"
    )

    val REDES_SOCIALES = listOf(
        "GitHub", "Steam", "LinkedIn", "Discord", "X (Twitter)"
    )

    val INSTITUCIONES = listOf(
        listOf("INS001", "Universidad de El Salvador (UES)"),
        listOf("INS002", "Escuela Nacional de Agricultura(ENA)"),
        listOf("INS003", "Fundación Gloria de Kriete"),
        listOf("INS004", "Universidad Don Bosco"),
        listOf("INS005", "Universidad José Matías Delgado"),
        listOf("INS006", "Ministerio de Educación, Ciencia y Tecnología (MINED)")
    )

    val MUNICIPIOS = listOf(
        listOf(1, 1, "Ahuachapán Norte"), listOf(1, 2, "Ahuachapán Centro"), listOf(1, 3, "Ahuachapán Sur"),
        listOf(2, 1, "Santa Ana Norte"), listOf(2, 2, "Santa Ana Centro"), listOf(2, 3, "Santa Ana Este"), listOf(2, 4, "Santa Ana Oeste"),
        listOf(3, 1, "Sonsonate Norte"), listOf(3, 2, "Sonsonate Centro"), listOf(3, 3, "Sonsonate Este"), listOf(3, 4, "Sonsonate Oeste"),
        listOf(4, 1, "Chalatenango Norte"), listOf(4, 2, "Chalatenango Centro"), listOf(4, 3, "Chalatenango Sur"),
        listOf(5, 1, "Cuscatlán Norte"), listOf(5, 2, "Cuscatlán Sur"),
        listOf(6, 1, "San Salvador Norte"), listOf(6, 2, "San Salvador Oeste"), listOf(6, 3, "San Salvador Este"), listOf(6, 4, "San Salvador Centro"), listOf(6, 5, "San Salvador Sur"),
        listOf(7, 1, "La Libertad Norte"), listOf(7, 2, "La Libertad Centro"), listOf(7, 3, "La Libertad Oeste"), listOf(7, 4, "La Libertad Este"), listOf(7, 5, "La Libertad Costa"), listOf(7, 6, "La Libertad Sur"),
        listOf(8, 1, "La Paz Oeste"), listOf(8, 2, "La Paz Centro"), listOf(8, 3, "La Paz Este"),
        listOf(9, 1, "Cabañas Este"), listOf(9, 2, "Cabañas Oeste"),
        listOf(10, 1, "San Vicente Norte"), listOf(10, 2, "San Vicente Sur"),
        listOf(11, 1, "Usulután Norte"), listOf(11, 2, "Usulután Este"), listOf(11, 3, "Usulután Oeste"),
        listOf(12, 1, "San Miguel Norte"), listOf(12, 2, "San Miguel Centro"), listOf(12, 3, "San Miguel Oeste"),
        listOf(13, 1, "Morazán Norte"), listOf(13, 2, "Morazán Sur"),
        listOf(14, 1, "La Unión Norte"), listOf(14, 2, "La Unión Sur")
    )

    val DISTRITOS = listOf(
        listOf(1, 1, 1, "Atiquizaya"), listOf(1, 1, 2, "El Refugio"), listOf(1, 1, 3, "San Lorenzo"), listOf(1, 1, 4, "Turín"),
        listOf(1, 2, 1, "Ahuachapán"), listOf(1, 2, 2, "Apaneca"), listOf(1, 2, 3, "Concepción de Ataco"), listOf(1, 2, 4, "Tacuba"),
        listOf(1, 3, 1, "Guaymango"), listOf(1, 3, 2, "Jujutla"), listOf(1, 3, 3, "San Francisco Menéndez"), listOf(1, 3, 4, "San Pedro Puxtla"),

        listOf(2, 1, 1, "Masahuat"), listOf(2, 1, 2, "Metapán"), listOf(2, 1, 3, "Santa Rosa Guachipilín"), listOf(2, 1, 4, "Texistepeque"),
        listOf(2, 2, 1, "Santa Ana"),
        listOf(2, 3, 1, "Coatepeque"), listOf(2, 3, 2, "El Congo"),
        listOf(2, 4, 1, "Candelaria de la Frontera"), listOf(2, 4, 2, "Chalchuapa"), listOf(2, 4, 3, "El Porvenir"), listOf(2, 4, 4, "San Antonio Pajonal"), listOf(2, 4, 5, "San Sebastián Salitrillo"), listOf(2, 4, 6, "Santiago de la Frontera"),

        listOf(3, 1, 1, "Juayúa"), listOf(3, 1, 2, "Nahuizalco"), listOf(3, 1, 3, "Salcoatitán"), listOf(3, 1, 4, "Santa Catarina Masahuat"),
        listOf(3, 2, 1, "Sonsonate"), listOf(3, 2, 2, "Sonzacate"), listOf(3, 2, 3, "San Antonio del Monte"), listOf(3, 2, 4, "Santo Domingo de Guzmán"), listOf(3, 2, 5, "Nahulingo"),
        listOf(3, 3, 1, "Armenia"), listOf(3, 3, 2, "Caluco"), listOf(3, 3, 3, "Cuisnahuat"), listOf(3, 3, 4, "Izalco"), listOf(3, 3, 5, "San Julián"), listOf(3, 3, 6, "Santa Isabel Ishuatán"),
        listOf(3, 4, 1, "Acajutla"),

        listOf(4, 1, 1, "Citalá"), listOf(4, 1, 2, "La Palma"), listOf(4, 1, 3, "San Ignacio"),
        listOf(4, 2, 1, "Agua Caliente"), listOf(4, 2, 2, "Dulce Nombre de María"), listOf(4, 2, 3, "El Paraíso"), listOf(4, 2, 4, "La Reina"), listOf(4, 2, 5, "Nueva Concepción"), listOf(4, 2, 6, "San Fernando"), listOf(4, 2, 7, "San Francisco Morazán"), listOf(4, 2, 8, "San Rafael"), listOf(4, 2, 9, "Santa Rita"), listOf(4, 2, 10, "Tejutla"),
        listOf(4, 3, 1, "Arcatao"), listOf(4, 3, 2, "Azacualpa"), listOf(4, 3, 3, "Cancasque"), listOf(4, 3, 4, "Chalatenango"), listOf(4, 3, 5, "Comalapa"), listOf(4, 3, 6, "Concepción Quezaltepeque"), listOf(4, 3, 7, "El Carrizal"), listOf(4, 3, 8, "La Laguna"), listOf(4, 3, 9, "Las Flores"), listOf(4, 3, 10, "Las Vueltas"), listOf(4, 3, 11, "Nombre de Jesús"), listOf(4, 3, 12, "Nueva Trinidad"), listOf(4, 3, 13, "Ojos de Agua"), listOf(4, 3, 14, "Potonico"), listOf(4, 3, 15, "San Antonio de la Cruz"), listOf(4, 3, 16, "San Antonio Los Ranchos"), listOf(4, 3, 17, "San Francisco Lempa"), listOf(4, 3, 18, "San Isidro Labrador"), listOf(4, 3, 19, "San Luis del Carmen"), listOf(4, 3, 20, "San Miguel de Mercedes"),

        listOf(5, 1, 1, "Suchitoto"), listOf(5, 1, 2, "San José Guayabal"), listOf(5, 1, 3, "Oratorio de Concepción"), listOf(5, 1, 4, "San Bartolomé Perulapía"), listOf(5, 1, 5, "San Pedro Perulapán"),
        listOf(5, 2, 1, "Cojutepeque"), listOf(5, 2, 2, "Candelaria"), listOf(5, 2, 3, "El Carmen"), listOf(5, 2, 4, "El Rosario"), listOf(5, 2, 5, "Monte San Juan"), listOf(5, 2, 6, "San Cristóbal"), listOf(5, 2, 7, "San Rafael Cedros"), listOf(5, 2, 8, "San Ramón"), listOf(5, 2, 9, "Santa Cruz Analquito"), listOf(5, 2, 10, "Santa Cruz Michapa"), listOf(5, 2, 11, "Tenancingo"),

        listOf(6, 1, 1, "Aguilares"), listOf(6, 1, 2, "El Paisnal"), listOf(6, 1, 3, "Guazapa"),
        listOf(6, 2, 1, "Apopa"), listOf(6, 2, 2, "Nejapa"),
        listOf(6, 3, 1, "Ilopango"), listOf(6, 3, 2, "San Martín"), listOf(6, 3, 3, "Soyapango"), listOf(6, 3, 4, "Tonacatepeque"),
        listOf(6, 4, 1, "Ayutuxtepeque"), listOf(6, 4, 2, "Mejicanos"), listOf(6, 4, 3, "San Salvador"), listOf(6, 4, 4, "San Marcos"), listOf(6, 4, 5, "Santo Tomás"), listOf(6, 4, 6, "Santiago Texacuangos"), listOf(6, 4, 7, "Cuscatancingo"), listOf(6, 4, 8, "Delgado"),
        listOf(6, 5, 1, "Panchimalco"), listOf(6, 5, 2, "Rosario de Mora"),

        listOf(7, 1, 1, "Quezaltepeque"), listOf(7, 1, 2, "San Matías"), listOf(7, 1, 3, "San Pablo Tacachico"),
        listOf(7, 2, 1, "San Juan Opico"), listOf(7, 2, 2, "Ciudad Arce"),
        listOf(7, 3, 1, "Colón"), listOf(7, 3, 2, "Jayaque"), listOf(7, 3, 3, "Sacacoyo"), listOf(7, 3, 4, "Tepecoyo"), listOf(7, 3, 5, "Talnique"),
        listOf(7, 4, 1, "Antiguo Cuscatlán"), listOf(7, 4, 2, "Huizúcar"), listOf(7, 4, 3, "Nuevo Cuscatlán"), listOf(7, 4, 4, "San José Villanueva"), listOf(7, 4, 5, "Zaragoza"),
        listOf(7, 5, 1, "Chiltiupán"), listOf(7, 5, 2, "Jicalapa"), listOf(7, 5, 3, "La Libertad"), listOf(7, 5, 4, "Tamanique"), listOf(7, 5, 5, "Teotepeque"),
        listOf(7, 6, 1, "Comasagua"), listOf(7, 6, 2, "Santa Tecla"),

        listOf(8, 1, 1, "Cuyultitán"), listOf(8, 1, 2, "Olocuilta"), listOf(8, 1, 3, "San Juan Talpa"), listOf(8, 1, 4, "San Luis Talpa"), listOf(8, 1, 5, "San Pedro Masahuat"), listOf(8, 1, 6, "Tapalhuaca"), listOf(8, 1, 7, "San Francisco Chinameca"),
        listOf(8, 2, 1, "El Rosario"), listOf(8, 2, 2, "Jerusalén"), listOf(8, 2, 3, "Mercedes La Ceiba"), listOf(8, 2, 4, "Paraíso de Osorio"), listOf(8, 2, 5, "San Antonio Masahuat"), listOf(8, 2, 6, "San Emigdio"), listOf(8, 2, 7, "San Juan Tepezontes"), listOf(8, 2, 8, "San Luis La Herradura"), listOf(8, 2, 9, "San Miguel Tepezontes"), listOf(8, 2, 10, "San Pedro Nonualco"), listOf(8, 2, 11, "Santa María Ostuma"), listOf(8, 2, 12, "Santiago Nonualco"),
        listOf(8, 3, 1, "San Juan Nonualco"), listOf(8, 3, 2, "San Rafael Obrajuelo"), listOf(8, 3, 3, "Zacatecoluca"),

        listOf(9, 1, 1, "Sensuntepeque"), listOf(9, 1, 2, "Victoria"), listOf(9, 1, 3, "Dolores"), listOf(9, 1, 4, "Guacotecti"), listOf(9, 1, 5, "San Isidro"),
        listOf(9, 2, 1, "Ilobasco"), listOf(9, 2, 2, "Tejutepeque"), listOf(9, 2, 3, "Jutiapa"), listOf(9, 2, 4, "Cinquera"),

        listOf(10, 1, 1, "Apastepeque"), listOf(10, 1, 2, "Santa Clara"), listOf(10, 1, 3, "San Ildefonso"), listOf(10, 1, 4, "San Esteban Catarina"), listOf(10, 1, 5, "San Sebastián"), listOf(10, 1, 6, "San Lorenzo"), listOf(10, 1, 7, "Santo Domingo"),
        listOf(10, 2, 1, "San Vicente"), listOf(10, 2, 2, "Guadalupe"), listOf(10, 2, 3, "Verapaz"), listOf(10, 2, 4, "Nuevo Tepetitán"), listOf(10, 2, 5, "Tecoluca"), listOf(10, 2, 6, "San Cayetano Istepeque"),

        listOf(11, 1, 1, "Santiago de María"), listOf(11, 1, 2, "Alegría"), listOf(11, 1, 3, "Berlín"), listOf(11, 1, 4, "Mercedes Umaña"), listOf(11, 1, 5, "Jucuapa"), listOf(11, 1, 6, "El Triunfo"), listOf(11, 1, 7, "Estanzuelas"), listOf(11, 1, 8, "San Buenaventura"), listOf(11, 1, 9, "Nueva Granada"),
        listOf(11, 2, 1, "Usulután"), listOf(11, 2, 2, "Jucuarán"), listOf(11, 2, 3, "San Dionisio"), listOf(11, 2, 4, "Concepción Batres"), listOf(11, 2, 5, "Santa María"), listOf(11, 2, 6, "Ozatlán"), listOf(11, 2, 7, "Tecapán"), listOf(11, 2, 8, "Santa Elena"), listOf(11, 2, 9, "San California"), listOf(11, 2, 10, "Ereguayquín"),
        listOf(11, 3, 1, "Jiquilisco"), listOf(11, 3, 2, "Puerto El Triunfo"), listOf(11, 3, 3, "San Agustín"), listOf(11, 3, 4, "San Francisco Javier"),

        listOf(12, 1, 1, "Ciudad Barrios"), listOf(12, 1, 2, "Sesori"), listOf(12, 1, 3, "Nuevo Edén de San Juan"), listOf(12, 1, 4, "San Gerardo"), listOf(12, 1, 5, "San Luis de la Reina"), listOf(12, 1, 6, "Carolina"), listOf(12, 1, 7, "San Antonio"), listOf(12, 1, 8, "Chapeltique"),
        listOf(12, 2, 1, "San Miguel"), listOf(12, 2, 2, "Comacarán"), listOf(12, 2, 3, "Uluazapa"), listOf(12, 2, 4, "Moncagua"), listOf(12, 2, 5, "Quelepa"), listOf(12, 2, 6, "Chirilagua"),
        listOf(12, 3, 1, "Chinameca"), listOf(12, 3, 2, "Nueva Guadalupe"), listOf(12, 3, 3, "Lolotique"), listOf(12, 3, 4, "San Jorge"), listOf(12, 3, 5, "San Rafael Oriente"), listOf(12, 3, 6, "El Tránsito"),

        listOf(13, 1, 1, "Arambala"), listOf(13, 1, 2, "Cacaopera"), listOf(13, 1, 3, "Corinto"), listOf(13, 1, 4, "El Rosario"), listOf(13, 1, 5, "Joateca"), listOf(13, 1, 6, "Jocoaitique"), listOf(13, 1, 7, "Meanguera"), listOf(13, 1, 8, "Perquín"), listOf(13, 1, 9, "San Fernando"), listOf(13, 1, 10, "San Isidro"), listOf(13, 1, 11, "Torola"),
        listOf(13, 2, 1, "Chilanga"), listOf(13, 2, 2, "Delicias de Concepción"), listOf(13, 2, 3, "El Divisadero"), listOf(13, 2, 4, "Gualococti"), listOf(13, 2, 5, "Guatajiagua"), listOf(13, 2, 6, "Jocoro"), listOf(13, 2, 7, "Lolotiquillo"), listOf(13, 2, 8, "Osicala"), listOf(13, 2, 9, "San Carlos"), listOf(13, 2, 10, "San Francisco Gotera"), listOf(13, 2, 11, "San Simón"), listOf(13, 2, 12, "Sensembra"), listOf(13, 2, 13, "Sociedad"), listOf(13, 2, 14, "Yamabal"), listOf(13, 2, 15, "Yoloaiquín"),

        listOf(14, 1, 1, "Anamorós"), listOf(14, 1, 2, "Bolívar"), listOf(14, 1, 3, "Concepción de Oriente"), listOf(14, 1, 4, "El Sauce"), listOf(14, 1, 5, "Lislique"), listOf(14, 1, 6, "Nueva Esparta"), listOf(14, 1, 7, "Pasaquina"), listOf(14, 1, 8, "Polorós"), listOf(14, 1, 9, "San José"), listOf(14, 1, 10, "Santa Rosa de Lima"),
        listOf(14, 2, 1, "Conchagua"), listOf(14, 2, 2, "El Carmen"), listOf(14, 2, 3, "Intipucá"), listOf(14, 2, 4, "La Unión"), listOf(14, 2, 5, "Meanguera del Golfo"), listOf(14, 2, 6, "San Alejo"), listOf(14, 2, 7, "Yayantique"), listOf(14, 2, 8, "Yucuaiquín")
    )

    val HABILIDADES = listOf(
        listOf(1, "H01", "Programación en Python"),
        listOf(1, "H02", "Desarrollo en C++"),
        listOf(1, "H03", "Diseño y consumo de APIs"),
        listOf(2, "H01", "Despliegue de proyectos en Google Cloud"),
        listOf(2, "H02", "Administración de sistemas Linux"),
        listOf(2, "H03", "Gestión de entornos en Vercel y Netlify"),
        listOf(3, "H01", "Configuración de topologías en Cisco Packet Tracer"),
        listOf(3, "H02", "Cálculo y diseño de Subnetting"),
        listOf(3, "H03", "Análisis de tráfico de red"),
        listOf(4, "H01", "Diseño de modelos relacionales (SQL)"),
        listOf(4, "H02", "Integración y gestión con Supabase"),
        listOf(4, "H03", "Manejo de bases de datos NoSQL"),
        listOf(5, "H01", "Integración de Gemini API y Google AI Studio"),
        listOf(5, "H02", "Ingeniería de prompts avanzados"),
        listOf(5, "H03", "Automatización de procesos con IA")
    )

    val USUARIOS = listOf(
        listOf("postulante", "12345678", "postulante"),
        listOf("empresa", "12345678", "gerente de empresa")
    )

    val EMPRESAS = listOf(
        listOf("06141234560101", 6, 4, 3, "Banco Agrícola", "2200-0001"),
        listOf("06141234560102", 6, 4, 3, "Nequi El Salvador", "2200-0002"),
        listOf("06141234560103", 6, 4, 3, "Súper Selectos Sede Central", "2200-0003"),
        listOf("06141234560104", 7, 6, 2, "Holcim El Salvador", "2200-0004"),
        listOf("06141234560105", 2, 2, 1, "AES CLESA", "2200-0005"),
        listOf("06141234560106", 12, 2, 1, "Grupo Campestre", "2200-0006"),
        listOf("06141234560107", 3, 2, 1, "Compañía Azucarera Salvadoreña - CASSA", "2200-0007"),
        listOf("06141234560108", 11, 1, 3, "La Geo Planta Geotérmica", "2200-0008"),
        listOf("06141234560109", 1, 2, 1, "Cooperativa Los Ausoles", "2200-0009"),
        listOf("06141234560110", 5, 2, 1, "Embutidos La Única", "2200-0010")
    )
}
