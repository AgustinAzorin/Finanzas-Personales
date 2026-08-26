# Build — Finanzas Personales y del Hogar

Guía para compilar, testear e instalar el proyecto en una máquina real (no en el sandbox de
Claude Code on the web, que bloquea `dl.google.com` y no puede resolver ni siquiera el plugin de
Android — ver `CLAUDE.md`, sección 0).

## Estado del proyecto

Las 9 fases del roadmap (Fase 0 a Fase 8, ver `CLAUDE.md`) tienen código escrito y revisado a
mano. El módulo `:engine` (motor financiero, Kotlin puro) está compilado y testeado en cada
sesión. El módulo `:app` (Android completo: UI, Room, Hilt, ML Kit, SQLCipher, etc.) **todavía no
pasó por una compilación real de punta a punta** — este documento es exactamente para eso: la
primera vez que corras estos pasos es de esperar encontrar algún error puntual de compilación
(ver punto 6 más abajo).

## Requisitos previos

- JDK 17.
- Android Studio (recomendado — trae el SDK) o Android SDK Command-line Tools instalado a mano.
- Android SDK con `compileSdk 35` / `targetSdk 35` (Build-Tools y Platform 35) y `minSdk 29`
  soportado.
- Conexión a internet sin restricciones hacia `google()` (Maven de Google) y Maven Central — la
  primera sincronización de Gradle descarga el Android Gradle Plugin, Compose, Room, Hilt, ML Kit
  y SQLCipher.
- (Opcional, para probar cámara/QR/biometría) un dispositivo Android físico con depuración USB
  habilitada, o un emulador (AVD) con Google Play Services.

## 1. Clonar y abrir el proyecto

```bash
git clone <url-del-repo>
cd Finanzas-Personales
```

Se puede abrir la carpeta directamente en Android Studio (File → Open) y dejar que sincronice
Gradle solo, o trabajar por línea de comandos con `./gradlew`.

## 2. Primera sincronización de Gradle

```bash
./gradlew --version
```

Alcanza con este comando para forzar la primera descarga de dependencias. Puede tardar varios
minutos la primera vez.

## 3. Correr los tests

Motor financiero (`:engine`, Kotlin puro — ya se corre en cada sesión de desarrollo):

```bash
./gradlew :engine:test
```

Suite completa (`:engine` + `:app`, incluye Room, Hilt, Robolectric — recién ahora se puede
correr):

```bash
./gradlew test
```

Estos dos comandos son el "Motor de calidad" que define `CLAUDE.md`: ninguna fase se considera
terminada si alguno falla.

## 4. Compilar el APK de debug

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## 5. Instalar en un dispositivo o emulador

```bash
./gradlew installDebug
```

o arrastrar el APK generado a un emulador ya corriendo.

## 6. Si aparecen errores de compilación

Es esperable en esta primera pasada: todo el código de `:app` (Fases 0 a 8) se escribió sin poder
compilarlo nunca — se validó con revisión manual archivo por archivo, no con un compilador real
(ver `CLAUDE.md`, sección 0). Lo más probable es algún import faltante, alguna firma de API que
cambió de versión, o algún binding de Hilt puntual.

Ante un error:

1. Copiar el mensaje completo de Gradle (clase, archivo, línea).
2. Ubicar el archivo señalado — la mayoría de los archivos de `:app` tienen comentarios que citan
   la sección de `CLAUDE.md` que implementan, lo que ayuda a entender la intención original antes
   de tocar el código.
3. Corregir y volver a correr `./gradlew test && ./gradlew assembleDebug` completo antes de dar
   por cerrado el arreglo.

## 7. Habilitar la captura automática (Fase 1)

`NotificationListenerService` requiere que el usuario habilite el permiso a mano desde el
dispositivo (Android no permite otorgarlo automáticamente):

```
Ajustes → Apps y notificaciones → Acceso especial → Acceso a notificaciones → Finanzas
```

También se puede llegar a este mismo ajuste desde la pantalla "Captura automática" dentro de la
app.

## 8. Notas para la primera prueba de Fase 8 (seguridad)

- La base de datos se cifra con SQLCipher desde el primer arranque de la app; no existe una
  versión sin cifrar previa que migrar.
- Si activás el bloqueo con biometría en un emulador sin huella/rostro configurados, la app avisa
  que no se puede activar en vez de dejarte bloqueado sin salida. En el emulador: Extended
  controls → Fingerprint, para simular una huella.
- El backup exportado queda cifrado con la contraseña que elijas al exportarlo — no hay forma de
  recuperar el archivo sin ella, ni siquiera reinstalando la app.

## Lo que NO hace falta

- Ninguna cuenta, backend, ni credenciales: toda la app es local-first (`CLAUDE.md`, secciones 2
  y 47).
- Las únicas llamadas de red son opcionales y sólo se disparan si el usuario toca "Actualizar" en
  la pantalla de Cotizaciones (dólar/inflación).
