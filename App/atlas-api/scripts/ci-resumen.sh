#!/usr/bin/env bash
# Publica el resultado de cada herramienta como anotacion (::notice) y en el resumen del job
# ($GITHUB_STEP_SUMMARY), para revisarlo sin descargar artefactos y capturarlo para el informe.
# Uso: bash scripts/ci-resumen.sh pruebas|cucumber|mutacion|cobertura|trivy|zap|gatling|jmeter|sonar|snyk [archivo]
set -u
SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/stdout}"
export SUMMARY
TIPO="$1"
ARCHIVO="${2:-}"
export ARCHIVO
python3 - "$TIPO" <<'PY'
import csv, glob, json, os, re, sys, collections, xml.etree.ElementTree as ET
tipo = sys.argv[1]
archivo = os.environ.get("ARCHIVO", "")
summary = open(os.environ["SUMMARY"], "a", encoding="utf-8")

def publicar(titulo, detalle_md, detalle_txt=""):
    print(f"::notice title={titulo}::{detalle_txt or titulo}")
    summary.write(f"### {titulo}\n\n{detalle_md}\n\n")

def pct(a, b):
    return 100.0 * a / b if b else 0.0

if tipo in ("pruebas", "cucumber"):
    t = f = e = s = 0
    filas = []
    patron = "target/surefire-reports/TEST-*Cucumber*.xml" if tipo == "cucumber" else "target/surefire-reports/TEST-*.xml"
    for x in sorted(glob.glob(patron)):
        r = ET.parse(x).getroot()
        n, fa, er, sk = (int(r.get(k, 0)) for k in ("tests", "failures", "errors", "skipped"))
        t += n; f += fa; e += er; s += sk
        filas.append(f"| {r.get('name').split('.')[-1]} | {n} | {fa} | {er} |")
    tabla = "| Clase | Pruebas | Fallas | Errores |\n|---|---|---|---|\n" + "\n".join(filas)
    titulo = f"{'Cucumber' if tipo == 'cucumber' else 'JUnit + Mockito'}: {t} pruebas · {f} fallas · {e} errores · {s} omitidas"
    publicar(titulo, tabla)

elif tipo == "mutacion":
    r = ET.parse("target/pit-reports/mutations.xml").getroot()
    c = collections.Counter(m.get("status") for m in r.findall("mutation"))
    total = sum(c.values())
    muertos = c.get("KILLED", 0) + c.get("TIMED_OUT", 0) + c.get("MEMORY_ERROR", 0)
    cubiertos = total - c.get("NO_COVERAGE", 0)
    sobrevivientes = [f"- {m.findtext('mutatedClass').split('.')[-1]}.{m.findtext('mutatedMethod')} línea {m.findtext('lineNumber')}: {m.findtext('description')}"
                      for m in r.findall("mutation") if m.get("status") == "SURVIVED"]
    md = f"Mutantes: **{total}** · muertos: **{muertos}** · sobrevivientes: **{c.get('SURVIVED', 0)}** · sin cobertura: **{c.get('NO_COVERAGE', 0)}**\n\n" + ("Sobrevivientes:\n" + "\n".join(sobrevivientes) if sobrevivientes else "")
    publicar(f"PIT: mutation score {pct(muertos, total):.1f} % ({muertos}/{total}) · test strength {pct(muertos, cubiertos):.1f} %", md)

elif tipo == "cobertura":
    L = Lc = B = Bc = 0
    paquetes = {}
    for r in csv.DictReader(open("target/site/jacoco/jacoco.csv")):
        lm, lc = int(r["LINE_MISSED"]), int(r["LINE_COVERED"])
        bm, bc = int(r["BRANCH_MISSED"]), int(r["BRANCH_COVERED"])
        L += lm + lc; Lc += lc; B += bm + bc; Bc += bc
        p = r["PACKAGE"].replace("cl.duoc.atlas.", "").replace("cl.duoc.atlas", "(raíz)")
        a = paquetes.setdefault(p, [0, 0]); a[0] += lc; a[1] += lm + lc
    tabla = "| Paquete | Líneas cubiertas | % |\n|---|---|---|\n" + "\n".join(
        f"| {p} | {c}/{t} | {pct(c, t):.1f} % |" for p, (c, t) in sorted(paquetes.items()) if t)
    publicar(f"JaCoCo: líneas {pct(Lc, L):.1f} % ({Lc}/{L}) · ramas {pct(Bc, B):.1f} % ({Bc}/{B}) · mínimo exigido 80 %", tabla)

elif tipo == "trivy":
    data = json.load(open(archivo))
    c = collections.Counter()
    filas = []
    for res in data.get("Results", []) or []:
        for v in res.get("Vulnerabilities", []) or []:
            c[v["Severity"]] += 1
            if v["Severity"] in ("CRITICAL", "HIGH"):
                filas.append(f"| {v['VulnerabilityID']} | {v['PkgName']} | {v['Severity']} | {v.get('FixedVersion', '')} |")
    resumen = " · ".join(f"{k} {c.get(k, 0)}" for k in ("CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN"))
    os_name = (data.get("Metadata", {}).get("OS") or {})
    md = f"Imagen: `{data.get('ArtifactName')}` · SO: {os_name.get('Family', '')} {os_name.get('Name', '')}\n\n" + (
        "| CVE | Paquete | Severidad | Versión corregida |\n|---|---|---|---|\n" + "\n".join(filas) if filas else "Sin vulnerabilidades HIGH ni CRITICAL.")
    publicar(f"Trivy (todas las severidades, SO + librerías): {resumen}", md)
    por_paquete = collections.defaultdict(list)
    for res in data.get("Results", []) or []:
        for v in res.get("Vulnerabilities", []) or []:
            if v["Severity"] in ("CRITICAL", "HIGH"):
                por_paquete[f"{v['PkgName']} {v.get('InstalledVersion', '')} -> {v.get('FixedVersion', '')}"].append(f"{v['VulnerabilityID']}({v['Severity'][0]})")
    detalle = " ; ".join(f"{k}: {', '.join(sorted(set(c)))}" for k, c in sorted(por_paquete.items()))
    print(f"::notice title=Trivy detalle CRITICAL/HIGH por paquete::{detalle[:3800] or 'Sin hallazgos'}")

elif tipo == "zap":
    if not os.path.exists("report_json.json"):
        print("::warning title=OWASP ZAP::No se generó report_json.json"); sys.exit(0)
    data = json.load(open("report_json.json"))
    filas = []
    riesgos = collections.Counter()
    for site in data.get("site", []):
        for a in site.get("alerts", []):
            riesgo = a.get("riskdesc", "").split(" ")[0]
            riesgos[riesgo] += 1
            filas.append(f"| {a.get('pluginid')} | {a.get('alert')} | {a.get('riskdesc')} | {a.get('count')} |")
    md = "| Plugin | Alerta | Riesgo (confianza) | Instancias |\n|---|---|---|---|\n" + "\n".join(filas) if filas else "Sin alertas."
    resumen = " · ".join(f"{k} {riesgos.get(k, 0)}" for k in ("High", "Medium", "Low", "Informational"))
    publicar(f"OWASP ZAP baseline: {resumen}", md, "; ".join(f[2:-2] for f in filas)[:900] or "Sin alertas")
    for site in data.get("site", []):
        for a in site.get("alerts", []):
            inst = a.get("instances", [])[:3]
            det = " | ".join(f"{i.get('uri')} :: {i.get('param', '')} :: {(i.get('evidence') or '')[:200]}" for i in inst)
            otro = (a.get("otherinfo") or "")[:300]
            print(f"::notice title=ZAP {a.get('pluginid')} {a.get('alert')}::{det} || {otro}")

elif tipo == "gatling":
    if not archivo or not os.path.exists(archivo):
        print("::warning title=Gatling::No se encontró la salida de Gatling"); sys.exit(0)
    texto = open(archivo, encoding="utf-8", errors="replace").read()
    bloque = texto.split("---- Global Information")[-1].split("=====")[0]
    lineas = [l.strip("> ").rstrip() for l in bloque.splitlines() if l.startswith(">")]
    aserciones = [l.strip() for l in texto.splitlines() if re.search(r": (true|false) \(actual", l)]
    md = "```\n" + "\n".join(lineas) + "\n```\n\nAserciones:\n" + "\n".join(f"- {a}" for a in aserciones)
    ok = all(a.split(" : ")[1].startswith("true") for a in aserciones) if aserciones else False
    publicar(f"Gatling: {len(aserciones)} aserciones {'cumplidas' if ok else 'con fallas'}", md, " ; ".join(aserciones))

elif tipo == "jmeter":
    archivos = glob.glob("target/jmeter/results/*.csv")
    if not archivos:
        print("::warning title=JMeter::No se encontró el CSV de resultados"); sys.exit(0)
    filas_csv = list(csv.DictReader(open(archivos[0])))
    por = collections.defaultdict(list); errores = collections.Counter()
    for r in filas_csv:
        por[r["label"]].append(int(r["elapsed"]))
        if r["success"] != "true":
            errores[r["label"]] += 1
    filas = []
    for k, v in por.items():
        v.sort(); p95 = v[min(len(v) - 1, int(0.95 * len(v)))]
        filas.append(f"| {k} | {len(v)} | {errores[k]} | {sum(v) // len(v)} | {p95} | {v[-1]} |")
    total = len(filas_csv); err = sum(errores.values())
    tabla = "| Petición | Total | Errores | Media (ms) | p95 (ms) | Máx (ms) |\n|---|---|---|---|---|---|\n" + "\n".join(filas)
    publicar(f"JMeter: {total} peticiones · errores {pct(err, total):.2f} %", tabla, " ; ".join(f.replace("|", " ").strip() for f in filas))
elif tipo == "sonar":
    # EP3: consulta el Quality Gate y las metricas principales en SonarCloud.
    import base64, urllib.request, urllib.parse
    host = os.environ.get("SONAR_HOST_URL", "https://sonarcloud.io").rstrip("/")
    clave = os.environ.get("SONAR_PROJECT_KEY", "")
    token = os.environ.get("SONAR_TOKEN", "")
    if not (clave and token):
        print("::warning title=SonarCloud::Sin token o projectKey: no se puede consultar el resultado"); sys.exit(0)
    auth = "Basic " + base64.b64encode((token + ":").encode()).decode()
    def api(ruta, **params):
        url = host + ruta + "?" + urllib.parse.urlencode(params)
        req = urllib.request.Request(url, headers={"Authorization": auth})
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.load(r)
    try:
        gate = api("/api/qualitygates/project_status", projectKey=clave)
        metricas = "bugs,vulnerabilities,security_hotspots,code_smells,coverage,duplicated_lines_density,ncloc,reliability_rating,security_rating,sqale_rating"
        med = api("/api/measures/component", component=clave, metricKeys=metricas)
    except Exception as e:
        print(f"::warning title=SonarCloud::No se pudo consultar la API ({e})"); sys.exit(0)
    json.dump(gate, open("sonar-qualitygate.json", "w"), indent=2)
    json.dump(med, open("sonar-medidas.json", "w"), indent=2)
    estado = gate.get("projectStatus", {}).get("status", "?")
    nota = {"1.0": "A", "2.0": "B", "3.0": "C", "4.0": "D", "5.0": "E"}
    valores = {m["metric"]: m.get("value", "") for m in med.get("component", {}).get("measures", [])}
    filas = [f"| {k} | {nota.get(v, v) if k.endswith('_rating') else v} |" for k, v in sorted(valores.items())]
    cond = [f"| {c.get('metricKey')} | {c.get('status')} | {c.get('actualValue', '')} | {c.get('comparator', '')} {c.get('errorThreshold', '')} |"
            for c in gate.get("projectStatus", {}).get("conditions", [])]
    md = ("| Métrica | Valor |\n|---|---|\n" + "\n".join(filas) +
          "\n\n**Condiciones del Quality Gate**\n\n| Métrica | Estado | Valor | Umbral |\n|---|---|---|---|\n" + "\n".join(cond) +
          f"\n\nPanel: {host}/project/overview?id={urllib.parse.quote(clave)}")
    publicar(f"SonarCloud: Quality Gate {estado}", md, f"Quality Gate {estado}; " + ", ".join(f"{k}={v}" for k, v in sorted(valores.items())))

elif tipo == "snyk":
    # EP3: resumen por severidad y puerta (CRITICAL no aceptada = falla; HIGH se reporta).
    datos = json.load(open(archivo or "snyk-report.json", encoding="utf-8"))
    proyectos = datos if isinstance(datos, list) else [datos]
    vistos = {}
    ignoradas = set()
    for p in proyectos:
        for v in p.get("vulnerabilities", []):
            vistos.setdefault(v["id"], v)
        for v in (p.get("filtered", {}) or {}).get("ignore", []):
            ignoradas.add(v.get("id"))
    c = collections.Counter(v["severity"] for v in vistos.values())
    filas = [f"| {v['severity'].upper()} | {v['id']} | {v['packageName']}@{v['version']} | {v.get('title', '')} | {', '.join(v.get('fixedIn', []) or ['sin parche'])} |"
             for v in sorted(vistos.values(), key=lambda x: ["critical", "high", "medium", "low"].index(x["severity"]))]
    md = ("| Severidad | ID | Paquete | Vulnerabilidad | Corregida en |\n|---|---|---|---|---|\n" + "\n".join(filas) +
          f"\n\nAceptadas en la política .snyk (riesgo aceptado con vencimiento): {len(ignoradas)}")
    titulo = (f"Snyk: {len(vistos)} vulnerabilidades únicas · CRITICAL {c['critical']} · HIGH {c['high']} · "
              f"MEDIUM {c['medium']} · LOW {c['low']} · aceptadas {len(ignoradas)}")
    publicar(titulo, md)
    if c["high"]:
        print(f"::warning title=Snyk::{c['high']} vulnerabilidades HIGH: registrar incidente y plan de actualización")
    if c["critical"]:
        print(f"::error title=Snyk::{c['critical']} vulnerabilidades CRITICAL no aceptadas: el pipeline se detiene")
        sys.exit(1)
PY
