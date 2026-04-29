# Code Challenge — Laboratorio de Pruebas de Software

## Equipo

**Nombre del equipo:** Los de Atras

| Nombre completo | Matrícula |
|---|-------|
| Estudiante 1 | 13749 |
| Estudiante 2 | 13798 |
| Estudiante 3 | 13699 |

---

## Entrega

**Fecha:** 28 Abril 2026

**Modalidad:** GitHub

**Link del repositorio (si aplica):** https://github.com/linetteacst/codeChallenge.git

---

## Parte 1 — Módulo `users`

### ¿Qué implementaron?

<!-- Describan brevemente qué hicieron, dificultades que encontraron o decisiones que tomaron -->
Implementamos un conjunto de pruebas tanto unitarias como de integración para asegurar la calidad y la robustez del módulo de usuarios, verificando que se cumplan la gran mayoría de las reglas de negocio.

Para el primer módulo de pruebas utilizamos JUnit y Mockito para aislar las capa de servicio. Nos enfocamos principalmente en asegurar y validar la logica. Se verifico además que se enviaran las excepciones personalizadas correctas como:

  - Nombres de usuario inválidos (cortos, largos, con caracteres especiales o guiones bajos en los extremos).
  - Nombres y apellidos con números o muy cortos.
  - Límites de edad estrictos (rechazando edades menores o iguales a 12, y mayores a 120).
  - Formato de teléfonos (exactamente 10 dígitos numéricos) y validación de correos electrónicos.
  - Prevención de registros duplicados y manejo de transiciones de estado inválidas (ej. intentar suspender a un usuario ya suspendido).

Para el segundo módulo de pruebas utilizamos MockMvc y SpringBootTest para simular las llamadas HTTP a lo endpoints REST de la API. Se comprobó que el controlador procese correctamente las peticiones y retorno los código de estado HTTP adecuados y la estructura JSON esperada:

  - 201 Created para registros exitosos y 200 OK para consultas o suspensiones exitosas.
  - 400 Bad Request cuando los payloads no cumplen con las reglas de validación.
  - 404 Not Found al interactuar con IDs que no existen en la base de datos.
  - 409 Conflict al intentar registrar un username previamente guardado.


### Cobertura obtenida User Service Test

| Métrica | Resultado |
|---|---|
| Line coverage | <!-- ej. 91% --> 87% |
| Branch coverage | <!-- ej. 87% --> 85%|

---
### Cobertura obtenida User Controller Integration Test

| Métrica | Resultado |
|---|---|
| Line coverage | <!-- ej. 91% --> 93% |
| Branch coverage | <!-- ej. 87% --> 87%|

---

## Challenge Extra — Módulo `petstore`

### ¿Qué implementaron?

<!-- Describan qué lograron del challenge, si lo completaron o hasta dónde llegaron -->
Llegamos a implementar todo lo necesario del challenge, pero nos quedamos un poco cortos con el Line Coverage del Adoption Service Test, pero consideramos que con un poco mas de tiempo pudieramos haber conseguido un porcentaje.

### Cobertura obtenida Adoption Service Test

| Métrica | Resultado |
|---|---|
| Line coverage | <!-- ej. 91% --> 67% |
| Branch coverage | <!-- ej. 87% --> 100%|

---
### Cobertura obtenida Adoption Controller Integration Test

| Métrica | Resultado |
|---|---|
| Line coverage | <!-- ej. 91% --> 100% |
| Branch coverage | <!-- ej. 87% --> 95%|

---
