# Videos narrados de CrediFlow

Graba la **aplicación real** (no una maqueta) en Chrome, con subtítulos, cursor animado y narración, y deja
el MP4 y su narración en MP3 en [`docs/presentacion/videos/`](../../presentacion/videos/).

| # | Guion | Archivos | Qué muestra |
|---|---|---|---|
| 1 | `camino-feliz` | `01-camino-feliz.mp4` · `01-camino-feliz-narracion.mp3` | Panel → simulador → asistente de 3 pasos → evaluación en vivo → resultado, explicación con IA, historial, intentos, amortización y original en S3 → búsqueda → panel actualizado |

## Requisitos

- El sistema levantado: `docker compose up -d --build` (el grabador usa http://localhost:3100; cámbialo con `CREDIFLOW_URL`).
- Para un camino feliz sin reintentos, el core bancario sin fallos simulados:
  `$env:CORE_TASA_FALLO=0; docker compose up -d core-bancario`. Al terminar: `Remove-Item env:CORE_TASA_FALLO; docker compose up -d core-bancario`.
- Node 18+ y Google Chrome.
- `playwright-core` y `ffmpeg-static`: de esta carpeta (`npm install playwright-core ffmpeg-static`) o, si no están,
  de `%USERPROFILE%\OneDrive\Documentos\video` (cámbiala con `VIDEO_TOOLS`).
- Narración (opcional): `edge-tts` (`pip install edge-tts`). Se busca en `.venv` del proyecto, en el PATH o en la ruta
  de la variable `EDGE_TTS`. Sin él, el video queda solo con subtítulos. Voz por defecto `es-CO-SalomeNeural`
  (cámbiala con `VOZ_TTS`). Requiere internet: edge-tts envía el texto de [`voz.js`](voz.js) al servicio de voz de Microsoft.

## Uso

```powershell
node grabar.js --prueba          # sin video: una captura por escena en .\capturas (revisar el guion)
node grabar.js                   # todos los videos
node grabar.js camino-feliz      # solo uno
node grabar.js --sin-voz         # solo subtítulos

# edge-tts instalado en otro entorno virtual:
$env:EDGE_TTS="C:\ruta\.venv\Scripts\edge-tts.exe"; node grabar.js
```

## Cómo funciona

1. Comprueba que la API responde y genera toda la narración antes de grabar, para que no haya esperas de red
   entre escenas. Los audios quedan en caché en `voz/`.
2. Abre la app con Playwright en 1440×900 y le inyecta una capa con subtítulo, número de escena, rol y cursor.
3. Cada escena (`v.cap`) muestra su subtítulo y narra el texto de `voz.js` que tiene la misma clave; la escena
   siguiente espera a que termine la voz.
4. Los clics y la escritura son reales: la solicitud se registra de verdad y el worker la evalúa en vivo.
5. Captura los cuadros con el *screencast* de Chrome (CDP); ffmpeg arma el MP4 respetando los tiempos reales,
   mezcla cada audio en su instante exacto y exporta la narración completa en MP3.

Para cambiar el guion, edita `MODULOS` en `grabar.js` y su narración en `voz.js` (la clave es el título de la
escena). Los selectores usan atributos `data-tour` de la interfaz, así no dependen de textos ni estilos.
El servicio de voz falla con frases muy cortas (`NoAudioReceived`): escribe frases de al menos 8 o 10 palabras.
