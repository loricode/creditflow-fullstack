// Graba videos narrados de CrediFlow usando la APLICACIÓN REAL (docker compose up -d --build).
// Uso: node grabar.js [modulo ...] [--prueba] [--sin-voz]
//   modulo: camino-feliz            (sin módulos = todos)
//   --prueba: no graba video; deja una captura por escena en ./capturas (para revisar el guion)
//   --sin-voz: solo subtítulos, aunque edge-tts esté instalado
// Técnica: Playwright abre Chrome, los clics y la escritura son reales, el screencast de Chrome (CDP)
// captura los cuadros y ffmpeg arma el MP4 y mezcla la narración (edge-tts) en su momento exacto.
const fs = require('fs'), path = require('path'), os = require('os'), crypto = require('crypto');
const { execFileSync, spawnSync } = require('child_process');

// playwright-core y ffmpeg-static: de esta carpeta (npm install) o de la carpeta de herramientas compartida.
const TOOLS = process.env.VIDEO_TOOLS || path.join(os.homedir(), 'OneDrive', 'Documentos', 'video');
const req = m => { try { return require(m); } catch (e) { return require(path.join(TOOLS, 'node_modules', m)); } };
const { chromium } = req('playwright-core');
const FFMPEG = req('ffmpeg-static');
const VOZ = require('./voz.js');

const args = process.argv.slice(2);
const PRUEBA = args.includes('--prueba');
const SIN_VOZ = args.includes('--sin-voz');
const VOICE = process.env.VOZ_TTS || 'es-CO-SalomeNeural';
const BASE = process.env.CREDIFLOW_URL || 'http://localhost:3100';
const W = 1440, H = 900;
const RAIZ = path.join(__dirname, '..', '..', '..');
const SALIDA = path.join(RAIZ, 'docs', 'presentacion', 'videos');
const CAPTURAS = path.join(__dirname, 'capturas');
const sleep = ms => new Promise(r => setTimeout(r, ms));

// ---------- Voz (opcional) ----------
function buscarEdge() {
  if (SIN_VOZ) return null;
  if (process.env.EDGE_TTS) return [process.env.EDGE_TTS, []];
  const venv = path.join(RAIZ, '.venv', 'Scripts', 'edge-tts.exe');
  if (fs.existsSync(venv)) return [venv, []];
  for (const [c, a] of [['edge-tts', []], ['python', ['-m', 'edge_tts']]]) {
    if (spawnSync(c, [...a, '--help'], { stdio: 'ignore' }).status === 0) return [c, a];
  }
  return null;
}
const EDGE = buscarEdge();
const DIRVOZ = path.join(__dirname, 'voz');
// El servicio de voz a veces falla con frases cortas al cambiar la velocidad (NoAudioReceived):
// se reintenta y, como último recurso, se genera a velocidad normal.
function generarVoz(texto, f) {
  const intentos = [['--rate=+4%'], ['--rate=+4%'], []];
  let ultimo;
  for (const [i, extra] of intentos.entries()) {
    const r = spawnSync(EDGE[0], [...EDGE[1], '--voice', VOICE, ...extra, '--text', texto, '--write-media', f], { encoding: 'utf8' });
    if (r.status === 0 && fs.existsSync(f) && fs.statSync(f).size > 0) return;
    ultimo = (r.stderr || '').trim().split(/\r?\n/).pop();
    if (i === 0) spawnSync(process.execPath, ['-e', 'setTimeout(()=>{},1000)']);
  }
  fs.rmSync(f, { force: true });
  throw new Error(`edge-tts no pudo narrar "${texto}": ${ultimo}`);
}
function audio(texto) {
  fs.mkdirSync(DIRVOZ, { recursive: true });
  const f = path.join(DIRVOZ, crypto.createHash('sha1').update(VOICE + texto).digest('hex').slice(0, 16) + '.mp3');
  if (!fs.existsSync(f) || fs.statSync(f).size === 0) generarVoz(texto, f);
  const r = spawnSync(FFMPEG, ['-i', f], { encoding: 'utf8' });
  const m = (r.stderr || '').match(/Duration: (\d+):(\d+):([\d.]+)/);
  return { f, dur: m ? (+m[1]) * 3600 + (+m[2]) * 60 + parseFloat(m[3]) : 3 };
}
const lectura = txt => Math.max(3200, String(txt).split(/\s+/).length * 330);

async function verificarSistema() {
  const r = await fetch(`${BASE}/api/v1/estadisticas`).catch(() => null);
  if (!r || r.status !== 200) throw new Error(`la API no responde en ${BASE}/api/v1/estadisticas: ¿está levantado docker compose?`);
}

// ---------- Capa de presentación: subtítulo, número de escena, rol, cursor y portada ----------
const OVERLAY = `(() => {
  if (document.getElementById('vx')) return;
  const css = document.createElement('style');
  css.textContent = \`
  #vx-cap{position:fixed;left:32px;right:32px;bottom:22px;z-index:99998;background:rgba(8,51,68,.95);color:#fff;border-radius:18px;
    padding:14px 20px 14px 16px;display:flex;gap:16px;align-items:center;box-shadow:0 14px 34px rgba(8,51,68,.4);
    font-family:'Plus Jakarta Sans',system-ui,sans-serif;transition:opacity .35s, transform .35s;pointer-events:none;border:1px solid rgba(103,232,249,.25)}
  #vx-cap.off{opacity:0;transform:translateY(12px)}
  #vx-n{flex:none;width:46px;height:46px;border-radius:50%;background:linear-gradient(135deg,#06b6d4,#2dd4bf);color:#083344;display:grid;place-items:center;font:800 19px 'Plus Jakarta Sans',sans-serif}
  #vx-t{font:800 20px/1.25 'Plus Jakarta Sans',sans-serif}
  #vx-d{font:500 16px/1.4 'Plus Jakarta Sans',sans-serif;color:rgba(255,255,255,.86);margin-top:2px}
  #vx-role{margin-left:auto;flex:none;font:700 13px 'Plus Jakarta Sans',sans-serif;background:#5eead4;color:#083344;border-radius:999px;padding:4px 12px}
  #vx-cur{position:fixed;left:0;top:0;z-index:99999;width:26px;height:26px;pointer-events:none;transition:left .65s cubic-bezier(.3,.7,.3,1), top .65s cubic-bezier(.3,.7,.3,1);filter:drop-shadow(0 2px 3px rgba(0,0,0,.4))}
  .vx-rip{position:fixed;z-index:99998;width:44px;height:44px;margin:-22px 0 0 -22px;border-radius:50%;border:3px solid #06b6d4;pointer-events:none;animation:vxr .55s ease-out forwards}
  @keyframes vxr{from{transform:scale(.3);opacity:1}to{transform:scale(1.4);opacity:0}}
  #vx-card{position:fixed;inset:0;z-index:100000;display:grid;place-items:center;color:#fff;text-align:center;font-family:'Plus Jakarta Sans',sans-serif;transition:opacity .6s;
    background:radial-gradient(700px 360px at 85% 10%,rgba(45,212,191,.35),transparent 60%),linear-gradient(135deg,#164e63 0%,#0e7490 45%,#06b6d4 100%)}
  #vx-card.off{opacity:0;pointer-events:none}
  #vx-card h1{font:800 62px/1.1 'Plus Jakarta Sans',sans-serif;margin:0 0 14px;letter-spacing:-1.5px}
  #vx-card p{font:500 24px/1.5 'Plus Jakarta Sans',sans-serif;margin:0 auto;color:rgba(255,255,255,.9);max-width:900px}
  #vx-card small{display:inline-block;margin-top:30px;font:700 15px 'Plus Jakarta Sans',sans-serif;letter-spacing:.12em;text-transform:uppercase;background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.3);border-radius:999px;padding:7px 18px}
  main{padding-bottom:150px!important}\`;
  document.head.appendChild(css);
  const root = document.createElement('div'); root.id = 'vx';
  root.innerHTML = '<div id="vx-cap" class="off"><div id="vx-n"></div><div><div id="vx-t"></div><div id="vx-d"></div></div><div id="vx-role"></div></div>'
    + '<svg id="vx-cur" viewBox="0 0 24 24"><path d="M4 2l16 9-7 2-3 7z" fill="#fff" stroke="#083344" stroke-width="1.6" stroke-linejoin="round"/></svg>'
    + '<div id="vx-card" class="off"><div><h1 id="vx-ch"></h1><p id="vx-cp"></p><small id="vx-cs"></small></div></div>';
  document.body.appendChild(root);
  const cur = document.getElementById('vx-cur'); cur.style.left = '720px'; cur.style.top = '450px';
})()`;

// ---------- Guiones ----------
const CERRAR_MODAL = 'mat-dialog-container button[mat-dialog-close]';

const MODULOS = [
  {
    id: 'camino-feliz', n: 1, ruta: '/panel', titulo: 'CrediFlow', sub: 'Créditos evaluados en segundos, con una explicación clara',
    cierre: ['Decidido en segundos', 'Java · Spring Boot · PL/SQL · Angular · AWS · Terraform · REST y SOAP · IA'],
    run: async v => {
      await v.cap(1, 'Panel de originación', 'Indicadores en vivo, actualizados cada 5 segundos.', 'Analista');
      await v.apuntar('[data-tour="kpis"]'); await v.hold(1500);

      await v.cap(2, 'El recorrido de cada solicitud', 'API REST → Outbox → SNS → SQS → Worker: PL/SQL · S3 · IA · SOAP', 'Arquitectura');
      await v.apuntar('[data-tour="flujo"]'); await v.hold(1500);

      await v.cap(3, 'Actividad de la semana', 'Últimos 7 días, desglose por producto y actividad reciente.', 'Analista');
      await v.apuntar('[data-tour="grafico"]'); await v.hold(1300);
      await v.apuntar('[data-tour="recientes"]'); await v.hold(1000);

      await v.cap(4, 'Simulador de cuota', 'Calculado en Oracle con PKG_CREDITO.CALCULAR_CUOTA.', 'Solicitante');
      await v.clic('[data-tour="nav-Simulador"]', 1300);
      await v.apuntar('[data-tour="sim-resultado"]'); await v.hold(1200);

      await v.cap(5, 'Cambiar de producto', 'La tasa y la cuota se recalculan al instante.', 'Solicitante');
      await v.clic('[data-tour="sim-VIVIENDA"]', 1700);
      await v.clic('[data-tour="sim-VEHICULO"]', 1700);

      await v.cap(6, 'Solicitar el crédito', 'Abre el asistente de 3 pasos.', 'Solicitante');
      await v.clic('[data-tour="sim-solicitar"]', 1300);

      await v.cap(7, 'Paso 1 · Tus datos', 'Nombre, documento y correo, validados en vivo.', 'Solicitante');
      await v.escribir('input[formcontrolname="nombreCompleto"]', 'Laura Gómez Rincón', 300, 45);
      await v.escribir('input[formcontrolname="numeroDocumento"]', '1032456789', 300, 60);
      await v.escribir('input[formcontrolname="email"]', 'laura.gomez@correo.co', 600, 35);
      await v.clic('[data-tour="paso1-continuar"]', 1000);

      await v.cap(8, 'Paso 2 · Tus finanzas', 'Capacidad de pago y endeudamiento calculados en vivo.', 'Solicitante');
      await v.escribir('input[formcontrolname="ingresosMensuales"]', '8500000', 300, 70);
      await v.escribir('input[formcontrolname="gastosMensuales"]', '2800000', 600, 70);
      await v.apuntar('[data-tour="capacidad"]'); await v.hold(1400);
      await v.clic('[data-tour="paso2-continuar"]', 1000);

      await v.cap(9, 'Paso 3 · Tu crédito', 'Cuota estimada y regla del 40 % de la capacidad de pago.', 'Solicitante');
      await v.clic('[data-tour="producto-VEHICULO"]', 1300);
      await v.apuntar('[data-tour="simulacion"]'); await v.hold(1800);

      await v.cap(10, 'Enviar la solicitud', 'La API responde al instante (201) y es idempotente por externalId.', 'Solicitante');
      await v.clic('[data-tour="enviar"]', 300);
      await v.esperar('[data-tour="exito"]', 15000);

      await v.cap(11, 'Solicitud recibida', 'Ya tiene número y la evaluación empezó.', 'Sistema');
      await v.apuntar('[data-tour="exito"] h3'); await v.hold(1000);
      await v.clic('[data-tour="ver-solicitud"]', 600);

      await v.cap(12, 'Evaluación en vivo', 'SNS → SQS → Worker: original en S3, score en PL/SQL, explicación y core SOAP.', 'Worker');
      await v.apuntar('[data-tour="recorrido"]');
      await v.esperar('[data-tour="cabecera"].aprobada', 60000);
      await v.hold(1500);

      await v.cap(13, 'Resultado de la evaluación', 'Score, riesgo, tasa por riesgo, cuota y referencia del core bancario.', 'PL/SQL');
      await v.apuntar('[data-tour="resultado"]'); await v.hold(2000);

      await v.cap(14, 'Explicación en lenguaje claro', 'Claude (Anthropic) sin datos personales; si no está disponible, reglas.', 'IA');
      await v.apuntar('[data-tour="ia"]'); await v.hold(2000);

      await v.cap(15, 'Historial de estados', 'Lo registra el trigger TRG_SOLICITUD_HISTORIAL en Oracle.', 'Oracle');
      await v.apuntar('[data-tour="historial"]'); await v.hold(1500);

      await v.cap(16, 'Intentos del worker', 'Cada intento con su resultado y duración.', 'Worker');
      await v.apuntar('[data-tour="intentos"]'); await v.hold(1300);

      await v.cap(17, 'Tabla de amortización', 'Sistema francés, generada por PL/SQL con FORALL.', 'Solicitante');
      await v.clic('[data-tour="btn-amortizacion"]', 1300);
      await v.apuntar('[data-tour="amortizacion-resumen"]'); await v.hold(1200);
      await v.rueda('mat-dialog-container .mat-mdc-dialog-content', 700); await v.hold(1200);
      await v.clic(CERRAR_MODAL, 900);

      await v.cap(18, 'Original en Amazon S3', 'Cifrado AES-256 y particionado por año, mes y producto.', 'Worker');
      await v.clic('[data-tour="btn-original"]', 1300);
      await v.apuntar('[data-tour="original-json"]'); await v.hold(2000);
      await v.clic(CERRAR_MODAL, 900);

      await v.cap(19, 'Buscar solicitudes', 'Búsqueda, filtros, orden y paginación en el servidor.', 'Analista');
      await v.clic('[data-tour="nav-Solicitudes"]', 1300);
      await v.escribir('[data-tour="buscar"]', 'Laura Gómez', 1600, 60);

      await v.cap(20, 'Encontrada', 'Aprobada, con su score.', 'Analista');
      await v.apuntar('tr.mat-mdc-row:has-text("Laura Gómez Rincón") >> nth=0'); await v.hold(1600);

      await v.cap(21, 'De vuelta al panel', 'Los indicadores ya incluyen la solicitud nueva.', 'Analista');
      await v.clic('[data-tour="nav-Panel"]', 1600);
      await v.apuntar('[data-tour="recientes"] li >> nth=0'); await v.hold(1300);
      await v.apuntar('[data-tour="kpis"]'); await v.hold(1500);
    },
  },
];

// ---------- Grabación de un módulo ----------
async function grabar(mod, browser) {
  const TMP = path.join(__dirname, 'frames', mod.id);
  fs.rmSync(TMP, { recursive: true, force: true }); fs.mkdirSync(TMP, { recursive: true });
  const conVoz = !!EDGE && !PRUEBA;
  const narracion = []; let vozFin = 0, wall0 = 0;
  const esperarVoz = async () => { if (PRUEBA) return; const ms = vozFin - Date.now(); if (ms > 0) await sleep(ms); };
  const hablar = (t, d) => {
    if (PRUEBA) return;
    const texto = VOZ[t];
    if (conVoz && texto) { const a = audio(texto); narracion.push({ f: a.f, t: Date.now() }); vozFin = Date.now() + a.dur * 1000 + 450; }
    else vozFin = Date.now() + lectura(t + ' ' + (d || ''));
  };

  const context = await browser.newContext({ viewport: { width: W, height: H }, deviceScaleFactor: 1, colorScheme: 'light', locale: 'es-CO' });
  const page = await context.newPage();
  const errores = [];
  page.on('pageerror', e => errores.push(e.message));
  await page.goto(BASE + mod.ruta);
  await page.waitForSelector('[data-tour="kpis"]', { timeout: 30000 });
  await page.evaluate(() => document.fonts.ready);
  await page.evaluate(OVERLAY);
  await sleep(1500); // que terminen los contadores animados del panel

  const frames = []; let cdp = null;
  if (!PRUEBA) {
    cdp = await context.newCDPSession(page);
    cdp.on('Page.screencastFrame', async f => {
      const file = path.join(TMP, `f${String(frames.length).padStart(6, '0')}.jpg`);
      fs.writeFileSync(file, Buffer.from(f.data, 'base64'));
      if (!frames.length) wall0 = Date.now();
      frames.push({ file, t: f.metadata.timestamp, w: Date.now() });
      cdp.send('Page.screencastFrameAck', { sessionId: f.sessionId }).catch(() => {});
    });
    await cdp.send('Page.startScreencast', { format: 'jpeg', quality: 88, maxWidth: W, maxHeight: H, everyNthFrame: 1 });
  }

  let shot = 0;
  fs.mkdirSync(CAPTURAS, { recursive: true });
  const snap = async n => { if (PRUEBA) await page.screenshot({ path: path.join(CAPTURAS, `${String(mod.n).padStart(2, '0')}-${mod.id}-${String(++shot).padStart(2, '0')}-${n}.png`) }); };
  const hold = ms => sleep(PRUEBA ? Math.min(ms, 400) : ms);
  // La navegación de Angular reemplaza la vista pero no el body: el overlay sobrevive.
  const asegurarOverlay = () => page.evaluate(OVERLAY);

  const v = { page, hold };
  v.cap = async (n, t, d, role) => {
    await esperarVoz(); await asegurarOverlay();
    await page.evaluate(([n, t, d, role]) => {
      document.getElementById('vx-cap').classList.remove('off');
      document.getElementById('vx-n').textContent = n; document.getElementById('vx-t').textContent = t;
      document.getElementById('vx-d').textContent = d; const r = document.getElementById('vx-role'); r.textContent = role || ''; r.style.display = role ? '' : 'none';
    }, [n, t, d, role]);
    hablar(t, d);
    await snap(t.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-zA-Z0-9]+/g, '-').toLowerCase());
  };
  const card = async (h, p, s) => {
    await esperarVoz(); await asegurarOverlay();
    await page.evaluate(([h, p, s]) => {
      document.getElementById('vx-ch').textContent = h; document.getElementById('vx-cp').textContent = p; document.getElementById('vx-cs').textContent = s || '';
      document.getElementById('vx-cap').classList.add('off');
      document.getElementById('vx-card').classList.remove('off');
    }, [h, p, s]);
    hablar(h, p);
  };
  const cardOff = async () => { await esperarVoz(); await page.evaluate(() => document.getElementById('vx-card').classList.add('off')); };

  v.apuntar = async sel => {
    const loc = page.locator(sel).first();
    await loc.waitFor({ state: 'visible', timeout: 15000 });
    let b = null;
    // Centrar el elemento: así nunca queda debajo del subtítulo.
    try { await loc.evaluate(el => el.scrollIntoView({ block: 'center', behavior: 'smooth' })); await sleep(PRUEBA ? 150 : 500); } catch (e) { /* sigue */ }
    for (let k = 0; k < 6 && !b; k++) { try { await loc.scrollIntoViewIfNeeded({ timeout: 2000 }); b = await loc.boundingBox(); } catch (e) { await sleep(250); } }
    if (!b) throw new Error('no pude ubicar ' + sel);
    const x = b.x + Math.min(b.width / 2, 60), y = b.y + Math.min(b.height / 2, 40);
    await page.evaluate(([x, y]) => { const c = document.getElementById('vx-cur'); c.style.left = (x - 4) + 'px'; c.style.top = (y - 2) + 'px'; }, [x, y]);
    await hold(700);
    return { loc, x, y };
  };
  const ripple = (x, y) => page.evaluate(([x, y]) => { const r = document.createElement('div'); r.className = 'vx-rip'; r.style.left = x + 'px'; r.style.top = y + 'px'; document.body.appendChild(r); setTimeout(() => r.remove(), 700); }, [x, y]);
  v.clic = async (sel, pausa = 700) => { const { loc, x, y } = await v.apuntar(sel); await ripple(x, y); await loc.click(); await hold(pausa); };
  // focus() y no click(): en Angular Material la etiqueta flotante intercepta el clic sobre el input
  v.escribir = async (sel, txt, pausa = 500, delay = 50) => { const { loc, x, y } = await v.apuntar(sel); await ripple(x, y); await loc.focus(); await loc.fill(''); await loc.pressSequentially(txt, { delay: PRUEBA ? 0 : delay }); await hold(pausa); };
  v.esperar = async (sel, timeout = 10000) => { await page.locator(sel).first().waitFor({ state: 'visible', timeout }); };
  /** Desplaza suavemente un contenedor con scroll (p. ej. la tabla dentro de un modal). */
  v.rueda = async (sel, dy) => {
    const loc = page.locator(sel).first();
    await loc.waitFor({ state: 'visible', timeout: 10000 });
    await loc.evaluate((el, dy) => el.scrollBy({ top: dy, behavior: 'smooth' }), dy);
    await hold(900);
  };

  await card(mod.titulo, mod.sub, `Video ${mod.n} de ${MODULOS.length} · CrediFlow`);
  await hold(2600); await snap('portada'); await cardOff(); await hold(700);
  await mod.run(v);
  await card(mod.cierre[0], mod.cierre[1], 'CrediFlow');
  await hold(1800); await snap('cierre'); await esperarVoz(); await hold(800);

  const wallFin = Date.now();
  if (cdp) await cdp.send('Page.stopScreencast');
  await context.close();
  if (errores.length) console.log(`  errores de la página (${mod.id}):`, errores);
  if (PRUEBA) return null;

  fs.mkdirSync(SALIDA, { recursive: true });
  const base = `${String(mod.n).padStart(2, '0')}-${mod.id}`;
  const OUT = path.join(SALIDA, `${base}.mp4`);
  const lista = frames.map((f, i) => { const d = i + 1 < frames.length ? frames[i + 1].t - f.t : Math.max(0.5, (wallFin - f.w) / 1000); return `file '${f.file.replace(/\\/g, '/')}'\nduration ${Math.max(d, 0.001).toFixed(4)}`; });
  lista.push(`file '${frames[frames.length - 1].file.replace(/\\/g, '/')}'`);
  fs.writeFileSync(path.join(TMP, 'lista.txt'), lista.join('\n'));
  const video = path.join(TMP, 'video.mp4');
  execFileSync(FFMPEG, ['-y', '-f', 'concat', '-safe', '0', '-i', path.join(TMP, 'lista.txt'),
    '-vf', `fps=30,scale=${W}:${H}:flags=lanczos,format=yuv420p`, '-c:v', 'libx264', '-preset', 'slow', '-crf', '20', '-movflags', '+faststart', video], { stdio: 'ignore' });
  let mp3 = null;
  if (narracion.length) {
    const a = ['-y', '-i', video];
    narracion.forEach(n => a.push('-i', n.f));
    const filtros = narracion.map((n, i) => `[${i + 1}:a]adelay=${Math.max(0, Math.round(n.t - wall0))}:all=1[a${i}]`);
    filtros.push(narracion.map((_, i) => `[a${i}]`).join('') + `amix=inputs=${narracion.length}:normalize=0:dropout_transition=0,loudnorm=I=-16:TP=-1.5[a]`);
    a.push('-filter_complex', filtros.join(';'), '-map', '0:v', '-map', '[a]', '-c:v', 'copy', '-c:a', 'aac', '-ar', '48000', '-b:a', '160k', '-movflags', '+faststart', OUT);
    execFileSync(FFMPEG, a, { stdio: 'ignore' });
    // La narración también como audio independiente (para podcast, presentación o revisión)
    mp3 = path.join(SALIDA, `${base}-narracion.mp3`);
    execFileSync(FFMPEG, ['-y', '-i', OUT, '-vn', '-c:a', 'libmp3lame', '-q:a', '2', mp3], { stdio: 'ignore' });
  } else {
    fs.copyFileSync(video, OUT);
  }
  fs.rmSync(TMP, { recursive: true, force: true });
  return { OUT, mp3, seg: Math.round(frames[frames.length - 1].t - frames[0].t), voz: narracion.length };
}

(async () => {
  const pedidos = args.filter(a => !a.startsWith('--'));
  const lista = pedidos.length ? MODULOS.filter(m => pedidos.includes(m.id)) : MODULOS;
  if (!lista.length) { console.error('Módulos válidos:', MODULOS.map(m => m.id).join(', ')); process.exit(1); }
  console.log(PRUEBA ? 'Modo prueba (capturas, sin video)' : `Narración: ${EDGE ? 'sí (' + VOICE + ')' : 'no, solo subtítulos'}`);

  await verificarSistema();
  // Toda la narración se genera ANTES de grabar: sin esperas de red entre escenas,
  // y un fallo de voz aparece al inicio, no a mitad del video.
  if (EDGE && !PRUEBA) {
    const textos = Object.values(VOZ);
    process.stdout.write(`Generando narración (${textos.length} frases)… `);
    textos.forEach(audio);
    console.log('lista');
  }
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    for (const m of lista) {
      process.stdout.write(`Video ${m.n} · ${m.titulo}… `);
      const r = await grabar(m, browser);
      console.log(r ? `${path.basename(r.OUT)} · ${r.seg} s${r.voz ? ' · con voz' : ''}${r.mp3 ? ' · ' + path.basename(r.mp3) : ''}` : 'capturas listas');
    }
  } finally {
    await browser.close();
  }
})().catch(e => { console.error('FALLO:', e.message); process.exit(1); });
