# Finanzas Personales y del Hogar

App Android local-first de finanzas personales y del hogar, orientada inicialmente a Argentina.
Sin backend, sin login, sin sincronización en la nube: todos los datos financieros viven en el
dispositivo (las únicas llamadas de red son opcionales, de solo lectura, para cotizaciones e
inflación).

## Documentación

- [`CLAUDE.md`](./CLAUDE.md): especificación completa del producto, modelo de datos, principios
  de integridad financiera, arquitectura y roadmap de fases.
- [`BUILD.md`](./BUILD.md): cómo compilar, testear e instalar el proyecto.

## Estado

Las 9 fases del roadmap (Fase 0 a Fase 8) tienen código implementado: MVP, captura automática,
tarjetas y cuotas, finanzas del hogar, cash flow, patrimonio, multi-moneda e inflación,
comprobantes, y seguridad y backup. El módulo `:engine` (motor financiero puro) está compilado y
testeado. El módulo `:app` todavía no pasó por una compilación real de punta a punta — ver
[`BUILD.md`](./BUILD.md) para hacerlo.

## Stack

Kotlin · Jetpack Compose · Material 3 · Room sobre SQLite (cifrada con SQLCipher) · Coroutines +
Flow · Hilt · Gradle Kotlin DSL — detalle completo en `CLAUDE.md`.
