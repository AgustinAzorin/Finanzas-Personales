# CLAUDE.md — Finanzas Personales y del Hogar

## 0. Nota sobre el entorno de build en sesiones remotas

El sandbox de Claude Code on the web bloquea `dl.google.com` (política de red), que es el host
real detrás de `google()` en Gradle (Android Gradle Plugin, AndroidX, Compose, Room, Hilt, y el
propio SDK de Android). Como resultado, en ese entorno:

- `:engine` (módulo Kotlin puro, sin dependencias de Android) SÍ se puede compilar y testear:
  `./gradlew :engine:test --configure-on-demand`. `--configure-on-demand` es necesario para que
  Gradle no intente configurar `:app` (que sí depende de `google()`) al pedir sólo `:engine`.
- `:app` NO se puede compilar ni testear ahí: ni siquiera se puede resolver el plugin de
  Android. `./gradlew test` / `./gradlew assembleDebug` completos sólo funcionan en una máquina
  o CI con acceso normal a `google()`/Maven Central y el SDK de Android instalado.

Si estás retomando este proyecto en una sesión con esa misma limitación, no lo tomes como señal
de que el código de `:app` está roto: verificalo con una revisión manual cuidadosa (como se hizo
al escribir la Fase 0) y dejale al usuario la validación de build real.

Los pasos para esa validación real (fuera de este sandbox, en una máquina o CI con acceso normal
a `google()`/Maven Central) están en [`BUILD.md`](./BUILD.md).

## 1. Contexto del proyecto

Estamos construyendo una aplicación Android de finanzas personales y del hogar, orientada inicialmente a Argentina.

La aplicación debe servir para responder, de manera confiable:

1. ¿Cuánto dinero tengo realmente?
2. ¿Dónde está mi dinero?
3. ¿Cuánto debo?
4. ¿Cuánto tengo comprometido?
5. ¿Cuánto voy a tener disponible dentro de 7, 30, 60 y 90 días?
6. ¿Cuánto gasto realmente por mes?
7. ¿En qué gasto?
8. ¿Cuánto de mis ingresos ya está comprometido?
9. ¿Cómo está evolucionando mi patrimonio?
10. ¿Puedo permitirme un gasto determinado?
11. ¿Estoy mejor o peor financieramente que hace unos meses?
12. ¿Qué gastos futuros ya están comprometidos aunque todavía no hayan ocurrido?

La aplicación NO debe ser simplemente un "expense tracker". Debe funcionar como un sistema operativo financiero personal y del hogar.

## 2. Principio rector: local-first

TODOS los datos financieros viven localmente en el dispositivo. No hay:

- backend propio
- cuentas de usuario
- login
- sincronización en la nube
- analytics
- telemetría

Las únicas llamadas de red permitidas son a APIs públicas de solo lectura necesarias para:

- cotizaciones
- inflación
- índices
- información pública relacionada con comprobantes

Estas llamadas son opcionales. La aplicación completa debe funcionar en modo avión.

## 3. Alcance del hogar

La aplicación debe poder manejar:

**Finanzas personales**: dinero y obligaciones pertenecientes directamente al usuario.

**Finanzas del hogar**: gastos e ingresos compartidos por el hogar.

El usuario puede registrar:

- gastos propios
- gastos compartidos
- gastos de otro miembro del hogar
- ingresos propios
- ingresos de otros miembros
- cuentas pertenecientes al hogar
- cuentas personales
- deudas del hogar
- objetivos financieros del hogar

No existe todavía sincronización multiusuario. El concepto de "miembro del hogar" es principalmente una dimensión de atribución, no una cuenta de usuario.

Ejemplo:

```
Hogar
├── Yo
├── Persona A
└── Persona B
```

Una transacción puede pertenecer a:

- una persona
- el hogar
- una persona pero ser compartida
- otra persona pero ser pagada por el usuario

## 4. Filosofía financiera

La aplicación debe distinguir cuidadosamente entre:

- **Flujo de dinero**: dinero que efectivamente entra o sale de una cuenta.
- **Gasto**: consumo económico que representa una disminución de riqueza.
- **Ingreso**: generación de recursos.
- **Transferencia**: movimiento de dinero entre cuentas propias.
- **Deuda**: obligación futura.
- **Compromiso**: dinero que todavía no salió pero que ya está comprometido.
- **Patrimonio**: activos menos pasivos.

Estas categorías NO deben mezclarse.

## 5. Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room sobre SQLite
- Coroutines + Flow
- Hilt
- Gradle Kotlin DSL
- Version Catalog (`libs.versions.toml`)
- minSdk 29
- targetSdk: versión estable más reciente
- MVVM + Repository Pattern
- Package by Feature

## 6. Convenciones

- Código, clases, variables, paquetes y commits en inglés.
- UI en español rioplatense, usando voseo.
- Todos los textos de UI deben estar centralizados en `strings.xml`. Nunca hardcodear strings dentro de Composables.
- Nunca usar `Float` o `Double` para dinero. Usar `Long` para unidades monetarias enteras cuando sea suficiente, o `BigDecimal` cuando sea necesario.
- Las fechas deben usar `java.time`. No usar `java.util.Date`.
- Los cálculos financieros importantes deben tener tests unitarios.

## 7. Principios de integridad financiera

Estas reglas son críticas.

### Regla 1 — Transferencias

Mover dinero `Cuenta A → Cuenta B` NO es un gasto. Debe generar dos movimientos vinculados:

```
OUTFLOW Cuenta A
INFLOW Cuenta B
```

pero el movimiento económico neto del usuario es cero.

### Regla 2 — Pago de tarjeta

Comprar con tarjeta es un `Gasto`. Pagar el resumen es una `Transferencia`. Nunca contar el pago del resumen como gasto adicional.

### Regla 3 — Cuotas

Una compra en cuotas representa `1 Purchase` + `N Installments`. El gasto mensual corresponde a las cuotas imputadas al período.

### Regla 4 — Patrimonio

El patrimonio neto debe calcularse como `Activos - Pasivos`. Una transferencia entre cuentas no cambia el patrimonio. Un gasto sí. Una deuda nueva puede disminuir el patrimonio aunque todavía no haya salido dinero de una cuenta.

### Regla 5 — Saldo real vs saldo disponible

La aplicación debe distinguir:

- **Saldo real**: dinero que actualmente existe en una cuenta.
- **Saldo disponible**: dinero que puede gastarse considerando restricciones o compromisos configurados.
- **Dinero comprometido**: dinero futuro que ya tiene destino.

## 8. Modelo de datos

Diseñar el modelo completo desde el comienzo aunque algunas entidades se implementen posteriormente.

### Household

Representa el hogar. Campos conceptuales: `id`, `name`, `baseCurrency`, `createdAt`.

### HouseholdMember

Persona perteneciente al hogar. Campos: `id`, `householdId`, `name`, `type`, `isActive`.

Tipos posibles: `OWNER`, `MEMBER`, `DEPENDENT`, `OTHER`.

No crear sistema de autenticación.

## 9. Account

Representa una fuente de dinero o deuda.

Tipos: `CASH`, `BANK_ACCOUNT`, `SAVINGS_ACCOUNT`, `MERCADO_PAGO`, `CREDIT_CARD`, `INVESTMENT`, `DIGITAL_WALLET`, `OTHER_ASSET`, `LOAN`, `OTHER_LIABILITY`.

Campos: `id`, `householdId`, `ownerMemberId` (nullable), `name`, `type`, `currency`, `initialBalance`, `initialBalanceDate`, `isActive`.

Una cuenta puede ser personal o perteneciente al hogar.

## 10. Asset

Representa activos cuyo valor no necesariamente corresponde a una cuenta corriente. Ejemplos: efectivo, dólares físicos, inversiones, vehículo, inmueble, otros bienes.

No todos los activos necesitan entrar inicialmente en el MVP. El modelo debe permitirlos.

Campos conceptuales: `id`, `householdId`, `ownerMemberId`, `name`, `category`, `currency`, `currentValue`, `valuationDate`.

## 11. Liability

Representa obligaciones. Ejemplos: deuda de tarjeta, préstamo, deuda personal, deuda informal, cuota futura, otra obligación.

Campos: `id`, `householdId`, `ownerMemberId`, `name`, `type`, `principal`, `outstandingAmount`, `currency`, `dueDate`, `interestRate` (nullable).

Las tarjetas pueden tener una integración especializada con `CreditCardStatement`.

## 12. Category

Sistema jerárquico. Ejemplo:

```
Vivienda
├── Alquiler
├── Expensas
├── Luz
├── Gas
└── Internet

Alimentación
├── Supermercado
├── Delivery
└── Restaurantes

Transporte
├── SUBE
├── Combustible
├── Taxi
└── Mantenimiento

Salud
├── Medicamentos
├── Consultas
└── Obra social
```

Debe soportar categorías personalizadas.

## 13. Tag

Etiquetas libres. Ejemplos: `trabajo`, `vacaciones`, `facultad`, `cumpleaños`, `hogar`, `emergencia`.

## 14. Transaction

Entidad central.

Campos: `id`, `householdId`, `accountId`, `ownerMemberId`, `amount`, `currency`, `date`, `merchant`, `categoryId`, `type`, `source`, `note`, `reconciliationHash`, `linkedTransactionId` (nullable), `status`, `createdAt`, `updatedAt`.

Tipos: `EXPENSE`, `INCOME`, `TRANSFER`, `ADJUSTMENT`.

Fuentes: `MANUAL`, `NOTIFICATION`, `QR`, `EMAIL`, `IMPORT`, `SYSTEM`.

Estados: `CONFIRMED`, `PENDING_REVIEW`, `IGNORED`, `DUPLICATE`.

## 15. RecurringTransaction

Representa ingresos o gastos recurrentes. No limitarlo a gastos.

Debe soportar:

- **Gastos**: alquiler, expensas, internet, servicios, suscripciones.
- **Ingresos**: sueldo, beca, alquiler cobrado, ingresos recurrentes.

Campos: `id`, `type`, `name`, `estimatedAmount`, `periodicity`, `dueDay`, `categoryId`, `accountId`, `memberId`, `isActive`.

Los movimientos recurrentes generan eventos pendientes. Nunca asumir automáticamente que ocurrió un gasto real.

## 16. Installment

Representa cuotas de una compra.

Campos: `id`, `transactionId`, `installmentNumber`, `totalInstallments`, `amount`, `dueDate`, `accountingDate`, `status`.

Estados: `PENDING`, `PAID`, `CANCELLED`.

## 17. CreditCard

Debe existir una abstracción específica para tarjetas.

Campos: `accountId`, `closingDay`, `dueDay`, `creditLimit`, `availableCredit`.

Debe soportar posteriormente diferentes ciclos.

## 18. CreditCardStatement

Campos: `id`, `creditCardAccountId`, `periodStart`, `closingDate`, `dueDate`, `totalAmount`, `paidAmount`, `status`.

Estados: `OPEN`, `CLOSED`, `PARTIALLY_PAID`, `PAID`.

## 19. FinancialCommitment

Entidad para representar obligaciones futuras. Ejemplos: cuota, alquiler, servicio, resumen de tarjeta, préstamo, suscripción, gasto recurrente.

Esto es importante porque una transacción futura no necesariamente es un gasto confirmado. El sistema debe distinguir: `ACTUAL`, `COMMITTED`, `ESTIMATED`.

## 20. FinancialGoal

Objetivos financieros. Ejemplos: fondo de emergencia, comprar una notebook, vacaciones, mudanza, ahorrar USD 2.000.

Campos: `id`, `name`, `targetAmount`, `currentAmount`, `currency`, `targetDate`, `priority`, `status`.

## 21. Budget

NO implementar inicialmente como envelope budgeting. El Budget debe funcionar principalmente como objetivo/límite/referencia.

Ejemplo:

```
Alimentación
Presupuesto mensual: $300.000
Gastado: $218.000
Proyección: $287.000
```

Debe poder comparar presupuesto vs. gasto real vs. gasto proyectado.

## 22. FinancialSnapshot

Debe existir una forma de guardar snapshots periódicos de: patrimonio, activos, pasivos, ingresos, gastos, liquidez.

Esto permitirá generar evolución histórica.

## 23. Dashboard principal

El Home NO debe centrarse exclusivamente en "cuánto gastaste este mes". Debe responder: ¿Cómo estoy financieramente?

Mostrar:

- **Patrimonio neto** (`Activos - Pasivos`) con variación respecto al período anterior.
- **Dinero disponible**: total de dinero líquido actual, separando Disponible y Comprometido.
- **Próximos compromisos**: tarjetas, alquiler, servicios, cuotas, préstamos, gastos recurrentes.
- **Cash Flow proyectado**: evolución esperada de liquidez a 7, 30, 60 y 90 días.
- **Gastos del período**: gasto real, gasto proyectado, comparación con meses anteriores.
- **Alertas financieras**, ejemplos:
  - "Tu tarjeta representa el 38% de tu ingreso mensual."
  - "El próximo mes tenés $420.000 comprometidos en cuotas."
  - "Este mes gastaste 24% más en restaurantes que tu promedio."
  - "Tu liquidez proyectada cae por debajo de $200.000 el 18/09."

No generar alertas arbitrarias. Toda alerta debe tener una explicación basada en datos.

## 24. Pantalla "Dinero"

Debe mostrar dónde está el dinero. Ejemplo:

```
EFECTIVO       $80.000
MERCADO PAGO   $340.000
BANCO          $720.000
USD            US$1.200
INVERSIONES    $850.000
```

Mostrar: total en pesos, total en USD si corresponde, distribución por cuenta, evolución.

## 25. Pantalla "Comprometido"

Pantalla central. Debe responder: ¿Cuánta plata futura ya tengo comprometida?

Separar: próximos 7 días, próximos 30 días, próximos 90 días, cuotas futuras, tarjetas, gastos recurrentes, deudas.

Ejemplo:

```
Próximos 30 días

Tarjeta             $340.000
Alquiler            $450.000
Servicios           $120.000
Cuotas              $180.000
Suscripciones        $25.000
                    ---------
Total               $1.115.000
```

## 26. Pantalla "Flujo"

Debe mostrar una línea temporal financiera. Ejemplo:

```
25 AGO
Saldo actual
$1.200.000

30 AGO
Alquiler
-$450.000

01 SEP
Sueldo
+$900.000

05 SEP
Tarjeta
-$340.000

10 SEP
Servicios
-$120.000
```

Debe calcular el saldo proyectado para cada evento.

## 27. Pantalla "Patrimonio"

Mostrar patrimonio neto y su evolución:

```
1 AGO     $3.200.000
15 AGO    $3.450.000
1 SEP     $3.600.000
```

Permitir descomponer:

```
Activos
- efectivo
- cuentas
- inversiones
- otros activos

Pasivos
- tarjeta
- préstamos
- otras deudas
```

## 28. Pantalla de alta rápida

Debe seguir siendo la pantalla más rápida de toda la aplicación.

Flujo ideal: `Monto → Categoría → Guardar`. Todo lo demás es opcional.

Debe recordar: última cuenta utilizada, categorías frecuentes, comercios frecuentes. Pero nunca inventar información financiera sin confirmación.

## 29. Transacciones

Listado con filtros y búsqueda por: mes, categoría, cuenta, persona, fuente, estado, tipo.

Permitir editar cualquier movimiento.

## 30. Gastos compartidos del hogar

Una transacción puede tener un `Responsable` y `Beneficiarios`. Ejemplo:

```
Supermercado
$100.000

Pagó: Yo

Beneficiarios:
Yo 50%
Persona A 50%
```

Inicialmente no hace falta implementar un sistema completo de "split bills". Pero el modelo debe permitirlo.

## 31. Ingresos

Los ingresos deben tener la misma importancia que los gastos.

Registrar: sueldo, freelance, ventas, becas, transferencias recibidas, otros ingresos. Permitir recurrentes.

Mostrar: ingreso mensual esperado, ingreso mensual real.

## 32. Métricas financieras

La aplicación debe poder calcular:

- Tasa de ahorro: `(ingresos - gastos) / ingresos`
- Gasto promedio mensual
- Ingreso promedio mensual
- Burn rate
- Liquidez
- Patrimonio neto
- Deuda total
- Deuda / ingreso
- Gastos fijos / ingreso
- Cuotas futuras
- Meses de colchón financiero

Ejemplo:

```
Liquidez disponible: $1.200.000
Gasto esencial mensual: $400.000

Colchón: 3 meses
```

No presentar métricas como consejos médicos/financieros profesionales. Son métricas descriptivas.

## 33. "¿Puedo permitírmelo?"

Simulador (posterior). El usuario ingresa un monto de gasto y la app calcula:

```
Liquidez actual
-
Compromisos futuros
-
Colchón configurado
=
Margen disponible
```

Y responde de manera explicable, por ejemplo:

> "Después de considerar tus compromisos conocidos, este gasto reduciría tu margen disponible de $650.000 a $350.000."

No decir simplemente "Sí / No" sin mostrar el razonamiento.

## 34. Inteligencia financiera

La aplicación puede generar insights locales basados exclusivamente en los datos almacenados. Ejemplos:

- "Tus gastos de transporte aumentaron 18% respecto al promedio de los últimos 3 meses."
- "Tenés $850.000 comprometidos en cuotas futuras."
- "El próximo mes parece ser tu mes de mayor presión de liquidez."
- "Tus ingresos aumentaron pero tu tasa de ahorro disminuyó."

Los insights deben: estar basados en datos, explicar de dónde salen, poder descartarse, no inventar información, no dar recomendaciones peligrosamente deterministas.

No hace falta IA generativa inicialmente. Primero construir un motor determinístico de análisis financiero.

## 35. Motor financiero

Separar claramente:

```
UI
↓
ViewModel
↓
Use Cases
↓
Financial Engine
↓
Repositories
↓
Room
```

El Financial Engine debe ser independiente de Android/UI siempre que sea posible. Debe poder calcular: balances, transferencias, patrimonio, cash flow, cuotas, compromisos, proyecciones, métricas, presupuestos, alertas, conciliación.

Esto es una prioridad arquitectónica.

## 36. Proyección de cash flow

El motor debe tomar datos conocidos (saldo actual, ingresos recurrentes, gastos recurrentes, cuotas, tarjetas, deudas, compromisos) y producir un `ProjectedCashFlow`.

Cada evento debe indicar su nivel de certeza: `ACTUAL`, `CONFIRMED`, `COMMITTED`, `ESTIMATED`.

Nunca mezclar estimaciones con dinero real.

## 37. Captura automática

Utilizar `NotificationListenerService`. Escuchar aplicaciones configurables. No hardcodear únicamente Mercado Pago.

Cada parser debe implementar una interfaz común. Ejemplo conceptual:

```
NotificationParser
parse(notification) → ParsedTransaction?
```

Guardar siempre: `packageName`, `timestamp`, `rawText`, `parserVersion`.

Los gastos capturados automáticamente deben quedar `PENDING_REVIEW` hasta confirmación.

## 38. Conciliación

El mismo gasto puede aparecer manualmente, por notificación, por QR o por importación. Nunca hacer merge automático solamente porque coincidan monto y fecha.

Generar candidatos usando: monto, comercio normalizado, fecha, cuenta, ventana temporal. Pedir confirmación cuando exista ambigüedad.

## 39. Reglas de categorización

El usuario puede corregir, por ejemplo, `McDonald's → Restaurantes`. La aplicación puede aprender reglas locales:

```
merchantNormalized = "MCDONALDS"
→ category = RESTAURANTES
```

Las reglas deben ser transparentes y editables.

## 40. Comprobantes

Posteriormente: cámara, almacenamiento local, QR AFIP/ARCA, OCR fallback.

Los comprobantes deben vincularse a Transactions. Nunca depender exclusivamente del OCR si existe información estructurada en el QR.

## 41. Multi-moneda

Argentina requiere soporte real de ARS y USD. El sistema debe guardar siempre monto nominal original y moneda original. Nunca sobrescribir el valor histórico.

Las conversiones son una capa de presentación/análisis. Guardar también: `exchangeRateUsed`, `exchangeRateDate`, `exchangeRateSource`.

## 42. Inflación

El ajuste por inflación debe ser exclusivamente para análisis histórico. Nunca modificar el monto nominal de una transacción.

Ejemplo: gasto original $100.000, valor equivalente $145.000. Ambos valores deben permanecer diferenciados.

## 43. Seguridad

La seguridad es especialmente importante porque la aplicación contiene información financiera. Implementar progresivamente:

- SQLCipher
- Android Keystore
- clave generada localmente
- BiometricPrompt
- bloqueo automático
- ocultar contenido sensible en screenshots cuando sea razonable

Nunca: hardcodear claves, guardar secretos en SharedPreferences sin protección, enviar datos financieros a servidores.

## 44. Backup

El backup NO puede depender exclusivamente de Android Auto Backup. Implementar export/import de backup cifrado.

El archivo debe contener: base de datos, comprobantes, metadata necesaria. El usuario debe poder guardarlo manualmente en Drive, disco, etc. El backup debe permitir recuperar el estado completo de la aplicación en otro dispositivo.

## 45. Export

Exportar a `.xlsx`. Usar `ACTION_CREATE_DOCUMENT` para elegir destino. Usar `FileProvider` para compartir cuando corresponda. No utilizar `WRITE_EXTERNAL_STORAGE`.

El Excel debe incluir al menos: fecha, tipo, comercio, categoría, cuenta, persona, monto, moneda, nota.

Agregar hojas separadas cuando sea útil: Transactions, Accounts, Monthly Summary, Patrimony, Commitments.

## 46. No-goals

No implementar inicialmente:

- publicación en Play Store
- cuentas online
- login
- backend
- sincronización cloud
- multiusuario en tiempo real
- criptomonedas
- trading
- recomendaciones de inversión automatizadas
- presupuesto envelope estilo YNAB
- integración bancaria que requiera backend propio

## 47. Principio de privacidad

La aplicación debe asumir que los datos financieros pertenecen exclusivamente al usuario. Por defecto: `local data > cloud convenience`.

Nunca introducir una dependencia online simplemente porque sea más cómoda.

---

## Roadmap de fases

Trabajar una fase por sesión, en orden. Antes de implementar una fase, revisar el modelo y las decisiones de arquitectura necesarias. Si aparece una contradicción con decisiones anteriores, detenerse y explicarla antes de modificar datos existentes.

Las 9 fases (Fase 0 a Fase 8) ya tienen código escrito. Eso NO equivale a "terminado" en el
sentido de la Definition of Done: por la limitación de sandbox descripta en la sección 0, todo
`:app` se validó con revisión manual, no con una compilación real. La primera pasada de
`./gradlew test && ./gradlew assembleDebug` en una máquina real (ver [`BUILD.md`](./BUILD.md)) es
el paso que falta para cerrar el roadmap de verdad.

- **Fase 0** (implementada): MVP — cuentas, categorías, miembros del hogar, transacciones (gastos/ingresos/transferencias), recurrentes, resumen financiero básico. Antes de escribir código, proponer modelo Room, relaciones, enums, estructura de paquetes, Financial Engine, repositories, use cases, estrategia de migraciones y estrategia de testing, y esperar aprobación.
- **Fase 1** (implementada): Captura automática vía `NotificationListenerService`, parsers desacoplados, conciliación, categorización automática.
- **Fase 2** (implementada): Tarjetas y cuotas — `CreditCard`, `CreditCardStatement`, `Installment`, ciclos de cierre, vencimientos, pago de resumen.
- **Fase 3** (implementada): Finanzas del hogar — miembros, gastos compartidos, atribución, reportes personales y del hogar.
- **Fase 4** (implementada): Cash Flow y planificación — compromisos, proyección 7/30/60/90 días, alertas de liquidez, calendario financiero.
- **Fase 5** (implementada): Patrimonio — activos, pasivos, patrimonio neto, snapshots, evolución histórica.
- **Fase 6** (implementada): Multi-moneda e inflación — ARS/USD, cotizaciones, historial, conversión para reportes, comparación nominal/real.
- **Fase 7** (implementada): Comprobantes — cámara, almacenamiento local, QR AFIP/ARCA, OCR fallback.
- **Fase 8** (implementada): Seguridad y recuperación — SQLCipher, Android Keystore, BiometricPrompt, bloqueo automático, export/import cifrado, recuperación completa.

## Motor de calidad

Después de cada fase ejecutar:

```bash
./gradlew test
./gradlew assembleDebug
```

No considerar una fase terminada si falla cualquiera de los dos comandos.

### Definition of Done

Una feature está terminada cuando:

- compila
- tiene tests donde corresponda
- no rompe datos existentes
- maneja estados vacíos
- maneja errores
- funciona offline
- no introduce dependencias innecesarias
- respeta arquitectura
- respeta local-first
- tiene UI funcional
- puede sobrevivir a rotación/recreación de Activity
- las operaciones financieras críticas son determinísticas y testeadas

### Regla de oro

No optimizar para cantidad de features. Optimizar para:

```
CORRECCIÓN
>
INTEGRIDAD DE DATOS
>
RECUPERABILIDAD
>
PRIVACIDAD
>
UX
>
FEATURES
```

Una aplicación financiera incorrecta es peor que una aplicación financiera incompleta.

## Principio final del producto

La aplicación no existe para decirme "Gastaste $500.000." Existe para decirme "Esta es tu situación financiera actual." Y debe permitirme entender:

```
QUÉ TENGO
+
QUÉ DEBO
+
QUÉ ESTÁ COMPROMETIDO
+
QUÉ VA A ENTRAR
+
QUÉ VA A SALIR
+
CÓMO ESTÁ CAMBIANDO MI PATRIMONIO
=
QUÉ TAN SALUDABLE ES MI SITUACIÓN FINANCIERA
```

La aplicación debe reducir al mínimo el trabajo manual del usuario. El usuario registra las excepciones. El sistema hace el trabajo pesado.
