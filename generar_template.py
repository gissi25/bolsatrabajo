import zipfile
import os

OUT = r"C:\Users\serda\OneDrive\Escritorio\PDM\bolsa de trabajo\bolsatrabajo\template_carga_ofertas.xlsx"

empresas = [
    ("06141234560101", "Applaudo Studios"),
    ("06141234560102", "Elaniin"),
    ("06141234560103", "Creativa Consultores"),
    ("06141234560104", "TELUS International El Salvador"),
    ("06141234560105", "Accedo Technologies"),
    ("06141234560106", "INNOVATEC"),
    ("06141234560107", "Gravity 4"),
    ("06141234560108", "Sysdatec"),
    ("06141234560109", "SAGACI"),
    ("06141234560110", "Tech Americas"),
]

grados = [("1", "Bachiller"), ("2", "Tecnico Superior"), ("3", "Profesorado"),
          ("4", "Licenciatura"), ("5", "Ingenieria"), ("6", "Maestria"), ("7", "Doctorado")]

headers = ["codigo", "titulo", "nit", "grado", "experiencia_anios",
           "edad_minima", "edad_maxima", "fecha_publicacion", "fecha_caducidad",
           "descripcion_oferta", "req_codigo", "req_descripcion"]

ofertas = [
    ["OF001", "Desarrollador Android", "06141234560101", "5", "2", "22", "38", "2026-06-01", "2026-07-01", "Desarrollador Android con experiencia en Kotlin, Jetpack Compose y MVVM.", "R01", "Experiencia minima 2 anios en Kotlin"],
    ["OF001", "Desarrollador Android", "06141234560101", "5", "2", "22", "38", "2026-06-01", "2026-07-01", "Desarrollador Android con experiencia en Kotlin, Jetpack Compose y MVVM.", "R02", "Conocimiento de Jetpack Compose"],
    ["OF001", "Desarrollador Android", "06141234560101", "5", "2", "22", "38", "2026-06-01", "2026-07-01", "Desarrollador Android con experiencia en Kotlin, Jetpack Compose y MVVM.", "R03", "Manejo de APIs REST y GraphQL"],
    ["OF002", "Analista de Datos", "06141234560104", "4", "2", "22", "40", "2026-06-02", "2026-07-05", "Analista de datos para extraer, limpiar y visualizar datos.", "R01", "Manejo avanzado de SQL"],
    ["OF002", "Analista de Datos", "06141234560104", "4", "2", "22", "40", "2026-06-02", "2026-07-05", "Analista de datos para extraer, limpiar y visualizar datos.", "R02", "Conocimiento de Python y Pandas"],
    ["OF003", "Disenador UI/UX", "06141234560102", "2", "1", "20", "35", "2026-06-03", "2026-07-10", "Disenador UI/UX para crear interfaces modernas.", "R01", "Manejo de Figma o Sketch"],
    ["OF003", "Disenador UI/UX", "06141234560102", "2", "1", "20", "35", "2026-06-03", "2026-07-10", "Disenador UI/UX para crear interfaces modernas.", "R02", "Conocimientos de diseno centrado en el usuario"],
    ["OF004", "Contador General", "06141234560103", "4", "3", "25", "50", "2026-06-04", "2026-07-15", "Contador general para contabilidad completa.", "R01", "Experiencia en declaraciones de IVA e ISR"],
    ["OF004", "Contador General", "06141234560103", "4", "3", "25", "50", "2026-06-04", "2026-07-15", "Contador general para contabilidad completa.", "R02", "Manejo de software contable"],
    ["OF005", "Community Manager", "06141234560107", "1", "1", "20", "32", "2026-06-05", "2026-07-08", "Community Manager para gestionar redes sociales.", "R01", "Experiencia en manejo de redes sociales"],
    ["OF005", "Community Manager", "06141234560107", "1", "1", "20", "32", "2026-06-05", "2026-07-08", "Community Manager para gestionar redes sociales.", "R02", "Conocimiento de herramientas de diseno"],
    ["OF006", "DevOps Engineer", "06141234560105", "5", "3", "24", "42", "2026-06-06", "2026-07-20", "Ingeniero DevOps para automatizar despliegues.", "R01", "Experiencia con Docker y Kubernetes"],
    ["OF006", "DevOps Engineer", "06141234560105", "5", "3", "24", "42", "2026-06-06", "2026-07-20", "Ingeniero DevOps para automatizar despliegues.", "R02", "Manejo de AWS o Google Cloud"],
    ["OF007", "Soporte Tecnico N1", "06141234560108", "2", "1", "18", "30", "2026-06-07", "2026-07-12", "Tecnico de soporte para atender incidentes.", "R01", "Conocimientos basicos de redes y SO"],
    ["OF008", "Gerente Proyectos TI", "06141234560106", "6", "5", "28", "50", "2026-06-08", "2026-07-22", "Gerente de proyectos TI para liderar equipos.", "R01", "Certificacion PMP o equivalente"],
    ["OF008", "Gerente Proyectos TI", "06141234560106", "6", "5", "28", "50", "2026-06-08", "2026-07-22", "Gerente de proyectos TI para liderar equipos.", "R02", "Experiencia liderando equipos de software"],
    ["OF009", "Vendedor Comercial", "06141234560109", "1", "1", "20", "45", "2026-06-09", "2026-07-18", "Vendedor comercial para servicios tecnologicos.", "R01", "Experiencia en ventas y atencion al cliente"],
    ["OF010", "Programador Python Jr", "06141234560110", "5", "0", "18", "30", "2026-06-10", "2026-07-25", "Programador Python junior. Sin experiencia requerida.", "R01", "Conocimientos basicos de Python"],
    ["OF010", "Programador Python Jr", "06141234560110", "5", "0", "18", "30", "2026-06-10", "2026-07-25", "Programador Python junior. Sin experiencia requerida.", "R02", "Nociones de Git y SQL"],
]

# Build shared strings list
ss_list = []
ss_map = {}

def reg(s):
    if s not in ss_map:
        ss_map[s] = len(ss_list)
        ss_list.append(s)

# Register all strings
for h in headers: reg(h)
for row in ofertas:
    for val in row: reg(val)

# XML escape helper
def xesc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;").replace("'", "&apos;")

# Namespace
M = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
CT = "http://schemas.openxmlformats.org/package/2006/content-types"
REL = "http://schemas.openxmlformats.org/package/2006/relationships"

def xml_header():
    return '<?xml version="1.0" encoding="UTF-8"?>\n'

def make_content_types():
    return '''<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>
'''

def make_root_rels():
    return '''<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
'''

def make_wb_rels():
    return '''<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>
'''

def make_workbook():
    return '''<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Ofertas" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>
'''

def make_styles():
    return '''<?xml version="1.0" encoding="UTF-8"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"/>
'''

def make_shared_strings():
    lines = ['<?xml version="1.0" encoding="UTF-8"?>']
    cnt = len(ss_list)
    lines.append(f'<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="{cnt}" uniqueCount="{cnt}">')
    for s in ss_list:
        lines.append(f'  <si><t>{xesc(s)}</t></si>')
    lines.append('</sst>')
    return '\n'.join(lines)

def col_letter(n):
    # 1->A, 2->B, ..., 26->Z
    return chr(64 + n)

def make_sheet():
    lines = ['<?xml version="1.0" encoding="UTF-8"?>']
    lines.append(f'<worksheet xmlns="{M}">')
    
    # cols
    lines.append('  <cols>')
    widths = [10, 32, 22, 8, 14, 10, 10, 16, 16, 50, 10, 42]
    for i, w in enumerate(widths, 1):
        lines.append(f'    <col min="{i}" max="{i}" width="{w}" customWidth="1"/>')
    lines.append('  </cols>')
    
    lines.append('  <sheetData>')
    
    # Row 1: headers (all shared strings)
    lines.append('    <row r="1">')
    for ci, h in enumerate(headers, 1):
        cl = col_letter(ci)
        idx = ss_map[h]
        lines.append(f'      <c r="{cl}1" t="s"><v>{idx}</v></c>')
    lines.append('    </row>')
    
    # Data rows
    for ri, row_data in enumerate(ofertas, 2):
        lines.append(f'    <row r="{ri}">')
        for ci, val in enumerate(row_data, 1):
            cl = col_letter(ci)
            # Columns 4,5,6,7 are numeric (grado, experiencia, edad), store as number
            if ci in [4, 5, 6, 7]:
                lines.append(f'      <c r="{cl}{ri}"><v>{val}</v></c>')
            else:
                idx = ss_map[val]
                lines.append(f'      <c r="{cl}{ri}" t="s"><v>{idx}</v></c>')
        lines.append(f'    </row>')
    
    lines.append('  </sheetData>')
    lines.append('</worksheet>')
    return '\n'.join(lines)

# ─────────────────────────────────────────
# BUILD XLSX
# ─────────────────────────────────────────
if os.path.exists(OUT):
    os.remove(OUT)

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", make_content_types())
    z.writestr("_rels/.rels", make_root_rels())
    z.writestr("xl/workbook.xml", make_workbook())
    z.writestr("xl/_rels/workbook.xml.rels", make_wb_rels())
    z.writestr("xl/styles.xml", make_styles())
    z.writestr("xl/sharedStrings.xml", make_shared_strings())
    z.writestr("xl/worksheets/sheet1.xml", make_sheet())

print(f"OK - {os.path.getsize(OUT)} bytes")
