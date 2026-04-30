const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, LevelFormat,
  HeadingLevel, BorderStyle, WidthType, ShadingType,
  PageNumber, PageBreak, TableOfContents
} = require("docx");

// ─── HELPERS ───────────────────────────────────────────────────────────────

const border = { style: BorderStyle.SINGLE, size: 1, color: "999999" };
const borders = { top: border, bottom: border, left: border, right: border };
const cellMargins = { top: 60, bottom: 60, left: 100, right: 100 };

function headerCell(text, width) {
  return new TableCell({
    borders,
    width: { size: width, type: WidthType.DXA },
    shading: { fill: "1F3A93", type: ShadingType.CLEAR },
    margins: cellMargins,
    verticalAlign: "center",
    children: [new Paragraph({ children: [new TextRun({ text, bold: true, color: "FFFFFF", font: "Calibri", size: 20 })] })],
  });
}

function cell(text, width, opts = {}) {
  const runs = Array.isArray(text)
    ? text.map(t => typeof t === "string" ? new TextRun({ text: t, font: "Calibri", size: 20, ...opts }) : new TextRun({ font: "Calibri", size: 20, ...opts, ...t }))
    : [new TextRun({ text, font: "Calibri", size: 20, ...opts })];
  return new TableCell({
    borders,
    width: { size: width, type: WidthType.DXA },
    shading: opts.shading ? { fill: opts.shading, type: ShadingType.CLEAR } : undefined,
    margins: cellMargins,
    children: [new Paragraph({ children: runs, spacing: { before: 0, after: 0 } })],
  });
}

function boldCell(text, width, bg) {
  return cell(text, width, { bold: true, shading: bg || "F0F4FF" });
}

function heading(level, text) {
  return new Paragraph({
    heading: level,
    children: [new TextRun({ text, font: "Calibri" })],
    spacing: { before: level === HeadingLevel.HEADING_1 ? 360 : 240, after: 120 },
  });
}

function para(text, opts = {}) {
  const runs = Array.isArray(text)
    ? text.map(t => typeof t === "string" ? new TextRun({ text: t, font: "Calibri", size: 22 }) : new TextRun({ font: "Calibri", size: 22, ...t }))
    : [new TextRun({ text, font: "Calibri", size: 22 })];
  return new Paragraph({
    children: runs,
    spacing: { before: opts.before || 80, after: opts.after || 80 },
    ...opts.extra,
  });
}

function bullet(text, level = 0) {
  return new Paragraph({
    numbering: { reference: "bullets", level },
    children: [new TextRun({ text, font: "Calibri", size: 22 })],
    spacing: { before: 40, after: 40 },
  });
}

function numberedItem(text, level = 0, ref = "numbers") {
  return new Paragraph({
    numbering: { reference: ref, level },
    children: [new TextRun({ text, font: "Calibri", size: 22 })],
    spacing: { before: 40, after: 40 },
  });
}

function codeBlock(text) {
  return new Paragraph({
    spacing: { before: 80, after: 80 },
    indent: { left: 360 },
    shading: { fill: "F5F5F5", type: ShadingType.CLEAR },
    children: [new TextRun({ text, font: "Consolas", size: 18, color: "2D2D2D" })],
  });
}

function tableRow(cells) {
  return new TableRow({ children: cells });
}

// ─── COLLECTIONS ───────────────────────────────────────────────────────────

const tablesInfo = [
  ["CATEGORIA_HABILIDAD", "ID_CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA", "-"],
  ["GENERO", "ID_GENERO", "NOMBRE_GENERO", "-"],
  ["TIPO_DOCUMENTO", "ID_TIPO_DOCUMENTO", "NOMBRE_TIPO", "-"],
  ["DEPARTAMENTO", "ID_DEPARTAMENTO", "NOMBRE_DEPARTAMENTO", "-"],
  ["INSTITUCION", "ID_INSTITUCION", "NOMBRE_INSTITUCION", "-"],
  ["GRADO_ACADEMICO", "ID_GRADO_ACADEMICO", "NOMBRE_GRADO", "-"],
  ["RED_SOCIAL", "ID_RED_SOCIAL", "NOMBRE_RED, LOGO_ICONO", "-"],
  ["MUNICIPIO", "ID_MUNICIPIO", "ID_DEPARTAMENTO, NOMBRE_MUNICIPIO", "DEPARTAMENTO"],
  ["DISTRITO", "ID_DISTRITO", "ID_MUNICIPIO, NOMBRE_DISTRITO", "MUNICIPIO"],
  ["HABILIDAD", "ID_HABILIDAD", "ID_CATEGORIA_HABILIDAD, NOMBRE_HABILIDAD", "CATEGORIA_HABILIDAD"],
  ["EMPRESA", "ID_EMPRESA", "ID_DISTRITO, NOMBRE_EMPRESA, CONTACTO_DIRECTO, NIT", "DISTRITO"],
  ["OFERTA_ACADEMICA", "ID_OFERTA_ACADEMICA", "ID_INSTITUCION, ID_GRADO_ACADEMICO", "INSTITUCION, GRADO_ACADEMICO"],
  ["POSTULANTE", "ID_POSTULANTE", "ID_USUARIO, ID_GENERO, ID_DISTRITO, ID_TIPO_DOCUMENTO, NOMBRE, APELLIDO, FECHA_NACIMIENTO, NUM_DOCUMENTO, NUP, DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR, EMAIL", "USUARIO, GENERO, DISTRITO, TIPO_DOCUMENTO"],
  ["USUARIO", "ID_USUARIO", "ID_POSTULANTE, USERNAME, PASSWORD, ROL", "POSTULANTE"],
  ["CERTIFICACION", "ID_POSTULANTE+ID_CERTIFICACION", "ID_INSTITUCION, NOMBRE_CERTIFICACION, CODIGO_CERTIFICACION, FECHA_CERTIFICACION", "POSTULANTE, INSTITUCION"],
  ["OFERTA_TRABAJO", "ID_EMPRESA+ID_OFERTA", "ID_GRADO_ACADEMICO, TITULO_PUESTO, FECHA_PUBLICACION, FECHA_CADUCIDAD, EXPERIENCIA_ANIOS, EDAD_MINIMA, EDAD_MAXIMA, DESCRIPCION_OFERTA_TRABAJO", "EMPRESA, GRADO_ACADEMICO"],
  ["DETALLE_REQUISITO", "ID_DETALLE", "ID_EMPRESA, ID_OFERTA, DESCRIPCION_REQUISITO", "OFERTA_TRABAJO"],
  ["EXPERIENCIA_LABORAL", "ID_POSTULANTE+ID_EXPERIENCIA", "ID_EMPRESA, PUESTO_TRABAJO, FECHA_INICIO, FECHA_FIN, DES_EXP_LABORAL, CONTACTO_REFERENCIA", "POSTULANTE, EMPRESA"],
  ["FORMACION_ACADEMICA", "ID_FORMACION", "ID_OFERTA_ACADEMICA, ID_POSTULANTE, TITULO_OBTENIDO, FECHA_OBTENCION", "OFERTA_ACADEMICA, POSTULANTE"],
  ["HABILIDAD_POSTULANTE", "ID_HABILIDAD+ID_POSTULANTE+ID_HP", "NIVEL_DESTREZA", "HABILIDAD, POSTULANTE"],
  ["POSTULACION", "ID_EMPRESA+ID_OFERTA+ID_POSTULANTE+ID_P", "FECHA_APLICACION, ESTADO_PROCESO", "OFERTA_TRABAJO, POSTULANTE"],
  ["RED_SOCIAL_POSTULANTE", "ID_RED_POSTUALNTE", "ID_POSTULANTE, ID_RED_SOCIAL, URL_PERFIL", "RED_SOCIAL, POSTULANTE"],
];

const filesRef = [
  ["Punto de entrada app", "MainActivity.kt", "sv.ues.fia.eisi.bt/MainActivity.kt"],
  ["Application class", "BTApplication.kt", "sv.ues.fia.eisi.bt/BTApplication.kt"],
  ["DB inicialización", "ConnectionHelper.kt", "data/local/ConnectionHelper.kt"],
  ["CRUD central", "MainRepository.kt", "data/repository/MainRepository.kt"],
  ["Editor dinámico", "EditorDialogFragment.kt", "ui/crud/EditorDialogFragment.kt"],
  ["Navegación", "nav_graph.xml", "res/navigation/nav_graph.xml"],
  ["Strings de UI", "strings.xml", "res/values/strings.xml"],
  ["Colores del tema", "colors.xml", "res/values/colors.xml"],
  ["Temas dark/light", "themes.xml", "res/values/ + values-night/"],
  ["Layout login", "fragment_login.xml", "res/layout/fragment_login.xml"],
  ["Layout dashboard", "fragment_dashboard.xml", "res/layout/fragment_dashboard.xml"],
  ["Layout editor", "dialog_editor.xml", "res/layout/dialog_editor.xml"],
  ["Constantes", "Constants.kt", "utils/Constants.kt"],
  ["Password hashing", "PasswordHasher.kt", "utils/PasswordHasher.kt"],
  ["DDL base de datos", "create_db.py", "Raíz del proyecto"],
  ["Triggers SQL", "recreate_triggers.py", "Raíz del proyecto"],
  ["DB pre-poblada", "si.db", "assets/si.db"],
  ["Build config", "build.gradle.kts", "app/build.gradle.kts"],
  ["Iconos vector", "ic_*.xml", "res/drawable/"],
  ["Animaciones", "slide_*.xml", "res/anim/"],
  ["Menu toolbar", "menu_dashboard.xml", "res/menu/menu_dashboard.xml"],
];

const modifySteps = [
  ["1", "create_db.py (raíz)", "Agregar columna al CREATE TABLE"],
  ["2", "Ejecutar create_db.py", "Regenerar assets/si.db"],
  ["3", "entities/MiEntidad.kt", "Agregar campo al data class"],
  ["4", "dao/MiEntidadDao.kt", "Agregar en getAll, getById, insert, update"],
  ["5", "MainRepository.kt ~L325", "Agregar en getColumnsForTable()"],
  ["6", "EditorDialogFragment.kt ~L590", "MISMA lista duplicada — agregar columna"],
  ["7", "MainRepository validación", "Hint o validación personalizada"],
  ["8", "EditorDialogFragment validación", "Hint o validación personalizada"],
  ["9", "EditorDialogFragment input", "InputType o máscara especial"],
  ["10", "getFkReferences()", "Solo si el campo es FK"],
];

// ─── BUILD FK TABLES SECTION ───────────────────────────────────────────────

function buildFkTablesSection() {
  const tableData = [
    ["MUNICIPIO", "ID_MUNICIPIO", "ID_DEPARTAMENTO, NOMBRE_MUNICIPIO", "DEPARTAMENTO"],
    ["DISTRITO", "ID_DISTRITO", "ID_MUNICIPIO, NOMBRE_DISTRITO", "MUNICIPIO"],
    ["HABILIDAD", "ID_HABILIDAD", "ID_CATEGORIA_HABILIDAD, NOMBRE_HABILIDAD", "CATEGORIA_HABILIDAD"],
    ["EMPRESA", "ID_EMPRESA", "ID_DISTRITO, NOMBRE_EMPRESA, CONTACTO_DIRECTO, NIT", "DISTRITO"],
    ["OFERTA_ACADEMICA", "ID_OFERTA_ACADEMICA", "ID_INSTITUCION, ID_GRADO_ACADEMICO", "INSTITUCION, GRADO_ACADEMICO"],
    ["POSTULANTE", "ID_POSTULANTE", "14 campos (NOMBRE, APELLIDO, EMAIL, etc.)", "USUARIO, GENERO, DISTRITO, TIPO_DOC"],
    ["USUARIO", "ID_USUARIO", "ID_POSTULANTE, USERNAME, PASSWORD, ROL", "POSTULANTE"],
    ["CERTIFICACION", "ID_POSTULANTE+ID_CERTIFICACION", "NOMBRE_CERTIFICACION, CODIGO, FECHA", "POSTULANTE, INSTITUCION"],
    ["OFERTA_TRABAJO", "ID_EMPRESA+ID_OFERTA", "TITULO_PUESTO, FECHAS, EXPERIENCIA, EDAD", "EMPRESA, GRADO_ACADEMICO"],
    ["DETALLE_REQUISITO", "ID_DETALLE", "ID_EMPRESA, ID_OFERTA, DESCRIPCION", "OFERTA_TRABAJO (compuesta)"],
    ["EXPERIENCIA_LABORAL", "ID_POSTULANTE+ID_EXPERIENCIA", "PUESTO, FECHAS, CONTACTO", "POSTULANTE, EMPRESA"],
    ["FORMACION_ACADEMICA", "ID_FORMACION", "TITULO_OBTENIDO, FECHA_OBTENCION", "OFERTA_ACADEMICA, POSTULANTE"],
    ["HABILIDAD_POSTULANTE", "ID_HABILIDAD+ID_POSTULANTE+ID_HP", "NIVEL_DESTREZA", "HABILIDAD, POSTULANTE"],
    ["POSTULACION", "ID_EMPRESA+ID_OFERTA+ID_POSTUL+ID_P", "FECHA_APLICACION, ESTADO_PROCESO", "OFERTA_TRABAJO, POSTULANTE"],
    ["RED_SOCIAL_POSTULANTE", "ID_RED_POSTUALNTE", "ID_POSTULANTE, ID_RED_SOCIAL, URL", "RED_SOCIAL, POSTULANTE"],
  ];
  const result = [];
  tableData.forEach(function(t, i) {
    var rows = [
      tableRow([cell("Tabla", 1560, { bold: true, shading: "E8EEF8" }), cell(t[0], 2340),
                 cell("PK", 780, { bold: true, shading: "E8EEF8" }), cell(t[1], 2340),
                 cell("FK", 780, { bold: true, shading: "E8EEF8" }), cell(t[3], 1560)]),
      tableRow([cell("Campos", 780, { bold: true, shading: "F5F7FC" }), cell(t[2], 8580, { shading: "F5F7FC" })]),
    ];
    if (i === 0) {
      result.push(heading(HeadingLevel.HEADING_2, "3.3 Tablas de Negocio (15 tablas)"));
    } else {
      result.push(new Paragraph({ spacing: { before: 40, after: 40 } }));
    }
    result.push(new Table({ width: { size: 9360, type: WidthType.DXA }, columnWidths: [1560, 2340, 780, 2340, 780, 1560], rows: rows }));
  });
  return result;
}

// ─── DOCUMENT ──────────────────────────────────────────────────────────────

const doc = new Document({
  styles: {
    default: { document: { run: { font: "Calibri", size: 22 } } },
    paragraphStyles: [
      {
        id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 36, bold: true, font: "Calibri", color: "1F3A93" },
        paragraph: { spacing: { before: 360, after: 200 }, outlineLevel: 0 },
      },
      {
        id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 30, bold: true, font: "Calibri", color: "2E5090" },
        paragraph: { spacing: { before: 280, after: 160 }, outlineLevel: 1 },
      },
      {
        id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: "Calibri", color: "3A6EA5" },
        paragraph: { spacing: { before: 200, after: 120 }, outlineLevel: 2 },
      },
    ],
  },
  numbering: {
    config: [
      {
        reference: "bullets",
        levels: [
          { level: 0, format: LevelFormat.BULLET, text: "\u2022", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
          { level: 1, format: LevelFormat.BULLET, text: "\u25CB", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1440, hanging: 360 } } } },
        ],
      },
      {
        reference: "numbers",
        levels: [
          { level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        ],
      },
      {
        reference: "steps",
        levels: [
          { level: 0, format: LevelFormat.DECIMAL, text: "Paso %1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        ],
      },
    ],
  },
  sections: [
    // ── PORTADA ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      children: [
        new Paragraph({ spacing: { before: 4000 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "BOLSA DE TRABAJO", font: "Calibri", size: 64, bold: true, color: "1F3A93" })],
        }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { after: 200 },
          children: [new TextRun({ text: "Gu\u00EDa Completa de Arquitectura", font: "Calibri", size: 40, color: "2E5090" })],
        }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          border: { top: { style: BorderStyle.SINGLE, size: 6, color: "1F3A93", space: 12 } },
          spacing: { before: 200, after: 200 },
          children: [new TextRun({ text: "An\u00E1lisis profundo del c\u00F3digo fuente", font: "Calibri", size: 28, color: "666666", italics: true })],
        }),
        new Paragraph({ spacing: { before: 1200 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "Arquitectura MVVM \u2022 Kotlin \u2022 SQLite \u2022 Android", font: "Calibri", size: 24, color: "888888" })],
        }),
        new Paragraph({ spacing: { before: 600 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "Documento preparado para exposici\u00F3n t\u00E9cnica", font: "Calibri", size: 22, color: "999999" })],
        }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "Abril 2026", font: "Calibri", size: 22, color: "999999" })],
        }),
      ],
    },

    // ── TABLA DE CONTENIDO ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "\u00CDndice"),
        new TableOfContents("Tabla de Contenido", { hyperlink: true, headingStyleRange: "1-3" }),
      ],
    },

    // ── 1. VISIÓN GENERAL ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "1. Visi\u00F3n General del Proyecto"),

        para("La aplicaci\u00F3n Bolsa de Trabajo (BT) es una soluci\u00F3n Android nativa desarrollada en Kotlin bajo la arquitectura MVVM (Model-View-ViewModel). Gestiona todo el ciclo de vida de una bolsa de empleo: postulantes, empresas, ofertas laborales, postulaciones, habilidades y formaci\u00F3n acad\u00E9mica."),

        para("La base de datos es SQLite con 22 tablas pre-pobladas que se distribuyen dentro del APK como un archivo assets/si.db. No se utiliza Room ni ning\u00FAn ORM \u2014 todo el acceso a datos es mediante SQL plano a trav\u00E9s de ConnectionHelper."),

        heading(HeadingLevel.HEADING_2, "Stack Tecnol\u00F3gico"),

        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [3120, 3120, 3120],
          rows: [
            tableRow([headerCell("Componente", 3120), headerCell("Tecnolog\u00EDa", 3120), headerCell("Versi\u00F3n", 3120)]),
            tableRow([cell("Lenguaje", 3120, { bold: true }), cell("Kotlin", 3120), cell("-", 3120)]),
            tableRow([cell("Arquitectura", 3120, { bold: true }), cell("MVVM + LiveData + Coroutines", 3120), cell("-", 3120)]),
            tableRow([cell("Navegaci\u00F3n", 3120, { bold: true }), cell("Jetpack Navigation Component", 3120), cell("2.8.5", 3120)]),
            tableRow([cell("Base de datos", 3120, { bold: true }), cell("SQLite (assets pre-poblado)", 3120), cell("-", 3120)]),
            tableRow([cell("UI", 3120, { bold: true }), cell("Material 3 + RecyclerView", 3120), cell("1.13.0", 3120)]),
            tableRow([cell("Min SDK / Target", 3120, { bold: true }), cell("Android 7.0 / Android 16", 3120), cell("24 / 36", 3120)]),
            tableRow([cell("Gradle / AGP", 3120, { bold: true }), cell("Gradle + Android Gradle Plugin", 3120), cell("9.1.1", 3120)]),
          ],
        }),

        heading(HeadingLevel.HEADING_2, "Estructura de Paquetes"),

        codeBlock("sv.ues.fia.eisi.bt/"),
        codeBlock("\u251C\u2500\u2500 data/"),
        codeBlock("\u2502   \u251C\u2500\u2500 local/entities/    \u2190 22 data classes"),
        codeBlock("\u2502   \u251C\u2500\u2500 local/dao/         \u2190 22 DAOs con SQL plano"),
        codeBlock("\u2502   \u2514\u2500\u2500 repository/         \u2190 MainRepository.kt (CRUD central)"),
        codeBlock("\u251C\u2500\u2500 viewmodel/"),
        codeBlock("\u2502   \u251C\u2500\u2500 AuthViewModel.kt    \u2190 Login + Registro"),
        codeBlock("\u2502   \u251C\u2500\u2500 DashboardViewModel   \u2190 Listado de tablas"),
        codeBlock("\u2502   \u2514\u2500\u2500 CrudViewModel.kt    \u2190 CRUD gen\u00E9rico"),
        codeBlock("\u251C\u2500\u2500 ui/"),
        codeBlock("\u2502   \u251C\u2500\u2500 auth/               \u2190 LoginFragment, RegisterFragment"),
        codeBlock("\u2502   \u251C\u2500\u2500 dashboard/          \u2190 DashboardFragment, DashboardAdapter"),
        codeBlock("\u2502   \u2514\u2500\u2500 crud/               \u2190 TableDetailFragment, EditorDialogFragment"),
        codeBlock("\u2514\u2500\u2500 utils/"),
        codeBlock("    \u251C\u2500\u2500 Constants.kt"),
        codeBlock("    \u251C\u2500\u2500 PasswordHasher.kt"),
        codeBlock("    \u2514\u2500\u2500 StyledToast.kt"),

        heading(HeadingLevel.HEADING_2, "Patrones Clave"),

        bullet("Single Activity + Navigation Component: MainActivity aloja un NavHostFragment. Todas las pantallas son Fragments."),
        bullet("MVVM con LiveData: Fragments observan LiveData de ViewModels. ViewModels lanzan corrutinas en IO para DB."),
        bullet("CRUD gen\u00E9rico: Un solo TableDetailFragment y un solo EditorDialogFragment sirven para las 22 tablas."),
        bullet("MAX+1 ID: Todas las tablas usan SELECT MAX(id)+1 manual. No es thread-safe."),
        bullet("Columnas duplicadas: La metadata de columnas est\u00E1 en 3 lugares (create_db.py, MainRepository, EditorDialogFragment). Deuda t\u00E9cnica."),
        bullet("Base pre-poblada: assets/si.db se copia al internal storage en primer lanzamiento."),
        bullet("SQL plano: Sin Room ni ORM. Strings concatenados. Riesgo de SQL injection."),
      ],
    },

    // ── 2. ARQUITECTURA MVVM ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "2. Arquitectura MVVM \u2014 Flujo de Datos"),

        para("La aplicaci\u00F3n sigue el patr\u00F3n MVVM con tres capas bien definidas:"),

        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [1560, 3120, 4680],
          rows: [
            tableRow([headerCell("Capa", 1560), headerCell("Ubicaci\u00F3n", 3120), headerCell("Responsabilidad", 4680)]),
            tableRow([cell("View", 1560, { bold: true }), cell("Fragments + Layouts XML", 3120), cell("Interfaz de usuario, eventos de UI", 4680)]),
            tableRow([cell("ViewModel", 1560, { bold: true }), cell("viewmodel/*.kt", 3120), cell("Estado de UI, l\u00F3gica de presentaci\u00F3n", 4680)]),
            tableRow([cell("Model", 1560, { bold: true }), cell("data/", 3120), cell("Datos, SQLite, Repositorio", 4680)]),
          ],
        }),

        heading(HeadingLevel.HEADING_2, "Flujo Completo de una Petici\u00F3n"),
        para("Ejemplo: usuario toca la tarjeta \"Empresa\" en el Dashboard y ve los registros:"),

        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "DashboardFragment ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Usuario toca tarjeta \"Empresa\"", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "Navigation Component ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Navega a tableDetailFragment con tableName=\"EMPRESA\"", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "TableDetailFragment.onCreate() ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Extrae argumentos del Bundle", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "CrudViewModel.setTable(\"EMPRESA\") ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Establece tabla actual y llama loadItems()", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "CrudViewModel.loadItems() ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Lanza corrutina en Dispatchers.IO", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "MainRepository.searchTable(\"EMPRESA\", \"\") ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Ejecuta SELECT * FROM EMPRESA", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "ConnectionHelper ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Obtiene DB desde internal storage, ejecuta raw query", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "SQLite ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Devuelve Cursor con resultados", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "ViewModel.postValue() ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Publica en Main thread", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "TableDetailFragment observer ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Recibe datos, llama adapter.submitList()", font: "Calibri", size: 22 })],
        }),
        new Paragraph({
          numbering: { reference: "steps", level: 0 },
          children: [new TextRun({ text: "RecyclerView ", font: "Calibri", size: 22, bold: true }), new TextRun({ text: "\u2014 Renderiza filas con layout item_table_row.xml", font: "Calibri", size: 22 })],
        }),

        heading(HeadingLevel.HEADING_2, "Diagrama de Navegaci\u00F3n"),

        codeBlock("loginFragment (INICIO)"),
        codeBlock("    \u251C\u2500\u2500 action_login_to_register \u2192 registerFragment"),
        codeBlock("    \u251C\u2500\u2500 action_login_to_dashboard \u2192 dashboardFragment"),
        codeBlock("    \u2514\u2500\u2500 (login exitoso) \u2192 dashboardFragment"),
        codeBlock("              \u2502"),
        codeBlock("registerFragment"),
        codeBlock("    \u251C\u2500\u2500 action_register_to_login \u2192 loginFragment"),
        codeBlock("    \u2514\u2500\u2500 (registro exitoso) \u2192 loginFragment"),
        codeBlock("              \u2502"),
        codeBlock("dashboardFragment"),
        codeBlock("    \u251C\u2500\u2500 (tap tarjeta) \u2192 tableDetailFragment {tableName}"),
        codeBlock("    \u251C\u2500\u2500 (menu logout) \u2192 loginFragment"),
        codeBlock("    \u2514\u2500\u2500 (dark mode) \u2192 recarga actividad"),
        codeBlock("              \u2502"),
        codeBlock("tableDetailFragment"),
        codeBlock("    \u251C\u2500\u2500 (FAB +) \u2192 EditorDialogFragment (crear)"),
        codeBlock("    \u251C\u2500\u2500 (edit FAB) \u2192 EditorDialogFragment (editar)"),
        codeBlock("    \u2514\u2500\u2500 (delete FAB) \u2192 DeleteConfirmDialog"),
      ],
    },

    // ── 3. BASE DE DATOS ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "3. Base de Datos \u2014 22 Tablas"),

        para("La base de datos SQLite contiene 22 tablas organizadas en cuatro categor\u00EDas:"),

        heading(HeadingLevel.HEADING_2, "3.1 Tablas Cat\u00E1logo (7)"),
        para("Tablas simples sin dependencias for\u00E1neas:"),
        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [2340, 2340, 3120, 1560],
          rows: [
            tableRow([headerCell("Tabla", 2340), headerCell("PK", 2340), headerCell("Campos", 3120), headerCell("FK", 1560)]),
            ...["CATEGORIA_HABILIDAD|ID_CATEGORIA_HABILIDAD|NOMBRE_CATEGORIA|-",
              "GENERO|ID_GENERO|NOMBRE_GENERO|-",
              "TIPO_DOCUMENTO|ID_TIPO_DOCUMENTO|NOMBRE_TIPO|-",
              "DEPARTAMENTO|ID_DEPARTAMENTO|NOMBRE_DEPARTAMENTO|-",
              "INSTITUCION|ID_INSTITUCION|NOMBRE_INSTITUCION|-",
              "GRADO_ACADEMICO|ID_GRADO_ACADEMICO|NOMBRE_GRADO|-",
              "RED_SOCIAL|ID_RED_SOCIAL|NOMBRE_RED, LOGO_ICONO|-",
            ].map(r => { const [a,b,c,d] = r.split("|"); return tableRow([cell(a,2340),cell(b,2340),cell(c,3120),cell(d,1560)]); }),
          ],
        }),

        heading(HeadingLevel.HEADING_2, "3.2 Tablas con FK (15)"),
        ...buildFkTablesSection(),

        heading(HeadingLevel.HEADING_2, "3.3 Scripts de Base de Datos"),
        para("Estos scripts Python/SQL en la ra\u00EDz del proyecto gestionan la base de datos:"),

        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [2340, 7020],
          rows: [
            tableRow([headerCell("Script", 2340), headerCell("Prop\u00F3sito", 7020)]),
            tableRow([cell("create_db.py", 2340, { bold: true }), cell("Crea si.db con DDL de 22 tablas desde cero", 7020)]),
            tableRow([cell("seed_data.sql", 2340, { bold: true }), cell("Datos iniciales: g\u00E9neros, documentos, departamentos", 7020)]),
            tableRow([cell("recreate_triggers.py", 2340, { bold: true }), cell("18 triggers de integridad (cascade deletes, validaciones)", 7020)]),
            tableRow([cell("add_missing_triggers.py", 2340, { bold: true }), cell("3 triggers adicionales (edad, email, cascade usuario)", 7020)]),
            tableRow([cell("check_records.py", 2340, { bold: true }), cell("Conteo de registros por tabla", 7020)]),
            tableRow([cell("fix_db.py", 2340, { bold: true }), cell("Reparaci\u00F3n de tablas faltantes", 7020)]),
          ],
        }),
      ],
    },

    // ── 4. UBICACIÓN DETALLADA ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "4. Ubicaci\u00F3n Detallada de Cada Componente"),

        heading(HeadingLevel.HEADING_2, "4.1 Capa de Datos"),
        para("Entity data classes en data/local/entities/. DAOs en data/local/dao/. Repository en data/repository/MainRepository.kt"),

        heading(HeadingLevel.HEADING_2, "4.2 ViewModels"),
        para("AuthViewModel maneja login/registro. DashboardViewModel maneja el grid de tablas. CrudViewModel maneja operaciones CRUD gen\u00E9ricas."),

        heading(HeadingLevel.HEADING_2, "4.3 Fragments (Pantallas)"),
        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [2340, 3120, 3900],
          rows: [
            tableRow([headerCell("Fragmento", 2340), headerCell("Archivo", 3120), headerCell("Layout", 3900)]),
            tableRow([cell("LoginFragment", 2340), cell("ui/auth/LoginFragment.kt", 3120), cell("fragment_login.xml", 3900)]),
            tableRow([cell("RegisterFragment", 2340), cell("ui/auth/RegisterFragment.kt", 3120), cell("fragment_register.xml", 3900)]),
            tableRow([cell("DashboardFragment", 2340), cell("ui/dashboard/DashboardFragment.kt", 3120), cell("fragment_dashboard.xml", 3900)]),
            tableRow([cell("TableDetailFragment", 2340), cell("ui/crud/TableDetailFragment.kt", 3120), cell("fragment_table_detail.xml", 3900)]),
            tableRow([cell("EditorDialogFragment", 2340), cell("ui/crud/EditorDialogFragment.kt (720 l\u00EDneas)", 3120), cell("dialog_editor.xml", 3900)]),
            tableRow([cell("DeleteConfirmDialog", 2340), cell("ui/crud/DeleteConfirmDialog.kt", 3120), cell("dialog_delete_confirm.xml", 3900)]),
          ],
        }),

        heading(HeadingLevel.HEADING_2, "4.4 Recursos Clave"),
        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [2340, 2340, 4680],
          rows: [
            tableRow([headerCell("Recurso", 2340), headerCell("Archivo(s)", 2340), headerCell("Ubicaci\u00F3n", 4680)]),
            tableRow([cell("Strings", 2340), cell("strings.xml (88)", 2340), cell("res/values/strings.xml", 4680)]),
            tableRow([cell("Colores", 2340), cell("colors.xml (61)", 2340), cell("res/values/colors.xml", 4680)]),
            tableRow([cell("Tema oscuro", 2340), cell("themes.xml", 2340), cell("res/values/themes.xml", 4680)]),
            tableRow([cell("Tema claro", 2340), cell("themes.xml", 2340), cell("res/values-night/themes.xml", 4680)]),
            tableRow([cell("Navegaci\u00F3n", 2340), cell("nav_graph.xml", 2340), cell("res/navigation/nav_graph.xml", 4680)]),
            tableRow([cell("Animaciones", 2340), cell("slide_*.xml (4)", 2340), cell("res/anim/", 4680)]),
            tableRow([cell("Drawables", 2340), cell("ic_*.xml (9)", 2340), cell("res/drawable/", 4680)]),
            tableRow([cell("Men\u00FA", 2340), cell("menu_dashboard.xml", 2340), cell("res/menu/menu_dashboard.xml", 4680)]),
          ],
        }),
      ],
    },

    // ── 5. CÓMO HACER CAMBIOS ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "5. C\u00F3mo Hacer Cambios Comunes"),

        heading(HeadingLevel.HEADING_2, "5.1 Agregar un Campo a una Tabla Existente"),
        para("Ejemplo: agregar \"SALARIO\" a OFERTA_TRABAJO. Hay que modificar TODOS estos archivos:"),

        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [780, 2340, 6240],
          rows: [
            tableRow([headerCell("#", 780), headerCell("Archivo", 2340), headerCell("Cambio", 6240)]),
            ...modifySteps.map(s => tableRow([cell(s[0], 780, { bold: true }), cell(s[1], 2340), cell(s[2], 6240)])),
          ],
        }),

        para([
          { text: "ADVERTENCIA: ", bold: true, color: "CC0000" },
          { text: "Las columnas est\u00E1n DUPLICADAS en MainRepository.getColumnsForTable() y EditorDialogFragment.getColumnsForTable(). \u00A1Siempre actualizar ambos archivos!" },
        ], { before: 200 }),

        heading(HeadingLevel.HEADING_2, "5.2 Agregar una Nueva Tabla"),
        numberedItem("Crear DDL en create_db.py y ejecutarlo para regenerar assets/si.db"),
        numberedItem("Crear Entity data class en entities/"),
        numberedItem("Crear DAO en dao/"),
        numberedItem("Actualizar MainRepository: getColumnsForTable(), getIdColumn(), getAllTablesWithCount()"),
        numberedItem("Actualizar EditorDialogFragment: getColumnsForTable() duplicado"),
        numberedItem("Agregar string del nombre en strings.xml"),
        numberedItem("Recompilar: ./gradlew assembleDebug"),
        para("La nueva tabla aparece autom\u00E1ticamente en el Dashboard porque el CRUD es gen\u00E9rico.", { before: 120 }),

        heading(HeadingLevel.HEADING_2, "5.3 Agregar una Nueva Pantalla"),
        numberedItem("Crear layout XML en res/layout/fragment_mi_pantalla.xml"),
        numberedItem("Crear Fragment en ui/mipaquete/MiPantallaFragment.kt"),
        numberedItem("(Opcional) Crear ViewModel en viewmodel/"),
        numberedItem("Agregar destination en nav_graph.xml"),
        numberedItem("Agregar action desde el origen en nav_graph.xml"),
        numberedItem("Navegar: findNavController().navigate(R.id.action_X_to_Y)"),
        numberedItem("Agregar strings en strings.xml"),

        heading(HeadingLevel.HEADING_2, "5.4 Validaci\u00F3n y Formato"),
        para("Toda la validaci\u00F3n est\u00E1 en EditorDialogFragment.validateField() (~linea 450). Para agregar una nueva regla, solo a\u00F1ada un when clause:"),
        codeBlock('when {\n    columnName == "SALARIO" && value.toDoubleOrNull() == null \u2192 "Debe ser un n\u00FAmero"\n    columnName == "SALARIO" && value.toDoubleOrNull()!! < 0 \u2192 "No puede ser negativo"\n    else \u2192 null\n}'),

        heading(HeadingLevel.HEADING_2, "5.5 Modificar Tema y Colores"),
        bullet("Color primario: res/values/colors.xml \u2192 primary (#3366FF)"),
        bullet("Fondo oscuro: res/values/themes.xml \u2192 android:colorBackground"),
        bullet("Fondo claro: res/values-night/themes.xml \u2192 android:colorBackground"),
        bullet("Estilo botones: themes.xml \u2192 AestheticButton"),
        bullet("Redondez: themes.xml \u2192 AestheticCard > cornerRadius"),
      ],
    },

    // ── 6. CASOS DEL INGENIERO ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "6. Casos Pr\u00E1cticos del Ingeniero"),

        heading(HeadingLevel.HEADING_2, "Caso 1: \"Agr\u00E9gale un campo salario a OFERTA_TRABAJO\""),
        para("Archivos a modificar (10):", { bold: true }),
        numberedItem("create_db.py \u2014 Agregar salario REAL al CREATE TABLE", 0, "steps"),
        numberedItem("Ejecutar python create_db.py \u2014 Regenera assets/si.db", 0, "steps"),
        numberedItem("entities/OfertaTrabajo.kt \u2014 Agregar val salario: Double? = null", 0, "steps"),
        numberedItem("dao/OfertaTrabajoDao.kt \u2014 Agregar en getAll/getById/insert/update", 0, "steps"),
        numberedItem("MainRepository.kt ~L335 \u2014 Agregar \"SALARIO\" en getColumnsForTable()", 0, "steps"),
        numberedItem("MainRepository.kt getHintText() \u2014 Hint \"0.00\"", 0, "steps"),
        numberedItem("EditorDialogFragment.kt ~L605 \u2014 Misma lista duplicada", 0, "steps"),
        numberedItem("EditorDialogFragment.kt validateField() \u2014 Validar n\u00FAmero positivo", 0, "steps"),
        numberedItem("EditorDialogFragment.kt createTextInputField() \u2014 InputType decimal", 0, "steps"),
        numberedItem("MainRepository.kt getFkReferences() \u2014 Solo si es FK", 0, "steps"),

        heading(HeadingLevel.HEADING_2, "Caso 2: \"La pantalla POSTULANTE no carga, d\u00F3nde reviso?\""),
        bullet("Verificar DB: check_records.py \u2014 \u00BFHay registros en POSTULANTE?"),
        bullet("Verificar DAO: dao/PostulanteDao.kt \u2014 \u00BFEl SQL es correcto?"),
        bullet("Verificar adaptador: EditorDialogFragment.getColumnsForTable() \u2014 \u00BFPOSTULANTE est\u00E1 listado?"),
        bullet("Verificar logcat: adb logcat | grep BT \u2014 \u00BFHay excepciones?"),
        bullet("Verificar layout: fragment_table_detail.xml \u2014 \u00BFEl RecyclerView es visible?"),
        bullet("Verificar viewmodel: CrudViewModel.loadItems() \u2014 \u00BFSe llama correctamente?"),

        heading(HeadingLevel.HEADING_2, "Caso 3: \"Que al seleccionar Departamento solo muestre sus Municipios\""),
        para("Ya est\u00E1 implementado en EditorDialogFragment.refreshDependentDropdown() (~linea 350). La cascada existente es: DEPARTAMENTO \u2192 MUNICIPIO \u2192 DISTRITO. Para agregar una nueva cascada, modifique este mismo m\u00E9todo."),

        heading(HeadingLevel.HEADING_2, "Caso 4: \"La app debe iniciar en Dashboard, no en Login\""),
        bullet("Archivo: res/navigation/nav_graph.xml"),
        bullet("Cambiar app:startDestination=\"@id/loginFragment\" a app:startDestination=\"@id/dashboardFragment\""),
        bullet("Opcional: modificar MainActivity.kt para verificar sesi\u00F3n en SharedPreferences"),

        heading(HeadingLevel.HEADING_2, "Caso 5: \"Al borrar POSTULANTE, que se borren sus datos relacionados\""),
        para("Ya est\u00E1 implementado con triggers SQL en recreate_triggers.py. El trigger before_delete_postulante borra en cascada: HABILIDAD_POSTULANTE, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, CERTIFICACION, RED_SOCIAL_POSTULANTE y POSTULACION."),

        heading(HeadingLevel.HEADING_2, "Caso 6: \"D\u00F3nde se define el orden de las tablas en el Dashboard?\""),
        para("En MainRepository.getAllTablesWithCount(). Ah\u00ED se define la lista, el orden, los nombres mostrados y los iconos de cada tarjeta."),

        heading(HeadingLevel.HEADING_2, "Caso 7: \"Cambiar el color azul del tema\""),
        bullet("Modificar res/values/colors.xml: primary (#3366FF), primary_dark (#7BA4FF), secondary (#00BFA5)"),
        bullet("Actualizar res/values/themes.xml si usa referencias a estos colores"),

        heading(HeadingLevel.HEADING_2, "Caso 8: \"D\u00F3nde est\u00E1 la l\u00F3gica del login?\""),
        bullet("UI: LoginFragment.kt \u2014 Captura credenciales"),
        bullet("ViewModel: AuthViewModel.kt \u2014 Lanza corrutina"),
        bullet("Repository: MainRepository.login() \u2014 Busca username, verifica hash"),
        bullet("Hashing: PasswordHasher.kt \u2014 PBKDF2 con SHA-256, 65536 iteraciones"),
        bullet("Sesi\u00F3n: LoginFragment.saveSession() \u2014 SharedPreferences"),

        heading(HeadingLevel.HEADING_2, "Caso 9: \"Agregar una opci\u00F3n al dropdown de roles en Registro\""),
        para("En RegisterFragment.kt, buscar el array de roles: arrayOf(\"postulante\", \"empresa\", \"admin\") y agregar el nuevo valor. Si el rol afecta permisos, tambi\u00E9n modificar Constants.kt y MainRepository.login()."),
      ],
    },

    // ── 7. DEUDA TÉCNICA ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "7. Deuda T\u00E9cnica Conocida"),

        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [2340, 3120, 3900],
          rows: [
            tableRow([headerCell("Problema", 2340), headerCell("Ubicaci\u00F3n", 3120), headerCell("Impacto", 3900)]),
            tableRow([cell("Columnas duplicadas", 2340, { bold: true }), cell("MainRepository + EditorDialogFragment", 3120), cell("Cambiar columnas requiere tocar ambos archivos", 3900)]),
            tableRow([cell("SQL injection", 2340, { bold: true }), cell("MainRepository (concatenaci\u00F3n SQL)", 3120), cell("Posible inyecci\u00F3n en b\u00FAsquedas LIKE", 3900)]),
            tableRow([cell("Sin Room ORM", 2340, { bold: true }), cell("Toda la capa de datos", 3120), cell("Sin verificaci\u00F3n en compile-time", 3900)]),
            tableRow([cell("Sin DI (Hilt/Dagger)", 2340, { bold: true }), cell("Toda la app", 3120), cell("Construcci\u00F3n manual de dependencias", 3900)]),
            tableRow([cell("Triggers en Python", 2340, { bold: true }), cell("recreate_triggers.py", 3120), cell("Triggers no versionados en SQL", 3900)]),
            tableRow([cell("MAX+1 ID", 2340, { bold: true }), cell("MainRepository.insertRecord()", 3120), cell("No thread-safe en concurrencia", 3900)]),
            tableRow([cell("Sin pruebas unitarias", 2340, { bold: true }), cell("test/ solo tiene ejemplo", 3120), cell("Falta cobertura de tests", 3900)]),
          ],
        }),
      ],
    },

    // ── 8. REFERENCIA RÁPIDA ──
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            border: { bottom: { style: BorderStyle.SINGLE, size: 2, color: "1F3A93", space: 4 } },
            children: [new TextRun({ text: "Bolsa de Trabajo \u2014 Gu\u00EDa de Arquitectura", font: "Calibri", size: 18, color: "999999", italics: true })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            border: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC", space: 4 } },
            children: [
              new TextRun({ text: "P\u00E1gina ", font: "Calibri", size: 18, color: "999999" }),
              new TextRun({ children: [PageNumber.CURRENT], font: "Calibri", size: 18, color: "999999" }),
            ],
          })],
        }),
      },
      children: [
        heading(HeadingLevel.HEADING_1, "8. Referencia R\u00E1pida de Archivos"),

        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [2340, 2340, 4680],
          rows: [
            tableRow([headerCell("\u00BFQu\u00E9 busca?", 2340), headerCell("Archivo", 2340), headerCell("Ruta", 4680)]),
            ...filesRef.map(f => tableRow([cell(f[0], 2340, { bold: true }), cell(f[1], 2340), cell(f[2], 4680)])),
          ],
        }),

        heading(HeadingLevel.HEADING_1, "9. Checklist para Exposici\u00F3n"),
        para("Antes de presentar el proyecto, verificar:"),
        ...[
          "Recompilar con ./gradlew assembleDebug (sin errores)",
          "Verificar que assets/si.db tiene datos de prueba",
          "Probar login con credenciales de prueba",
          "Probar CRUD en 3 tablas distintas (simple, con FK, compuesta)",
          "Verificar cascada Departamento \u2192 Municipio \u2192 Distrito",
          "Probar dark mode toggle",
          "Probar b\u00FAsqueda en Dashboard y en TableDetail",
          "Verificar eliminaci\u00F3n en cascada (borrar postulante)",
          "Tener Android Studio abierto con archivos clave visibles",
          "Saber d\u00F3nde est\u00E1n los 3 archivos con columnas duplicadas",
          "Poder explicar el flujo Fragment \u2192 ViewModel \u2192 Repository \u2192 DB",
        ].map(t => bullet(t)),
      ],
    },
  ],
});

// ─── GENERATE ──────────────────────────────────────────────────────────────

const outPath = "C:\\Users\\serda\\OneDrive\\Escritorio\\PDM\\bolsa de trabajo\\bolsatrabajo\\documentos\\Guia_Completa_Arquitectura_BT_v2.docx";
Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync(outPath, buffer);
  console.log("Documento creado: " + outPath);
  console.log("Tama\u00F1o: " + (buffer.length / 1024).toFixed(1) + " KB");
});
