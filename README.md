# R FRAGANCIAS — App Android (nativa, XML + Kotlin)

App móvil nativa para Android que replica el proyecto web **rfragancias**
(catálogo público + panel de administración) y le agrega **login de
usuarios administradores**. Todas las pantallas están construidas con
**layouts XML** (no Jetpack Compose) y Activities/Fragments en Kotlin,
tal como en un proyecto clásico de Android Studio.

La app **no duplica los datos**: consume la misma API REST que ya expone
tu servidor Node/Express (`routes/perfumes.js`, `routes/clientes.js`,
`routes/ventas.js`, `routes/pedidos.js`, `routes/auth.js`,
`routes/dashboard.js`). O sea que el catálogo, los clientes, las ventas y
los pedidos que veas en la app son los mismos que ves en la web.

## 1. Abrir el proyecto

1. Descomprime `RFraganciasApp.zip`.
2. Abre **Android Studio** → `File > Open` → selecciona la carpeta
   `RFraganciasApp`.
3. Deja que Android Studio sincronice Gradle (puede pedir descargar el
   wrapper de Gradle 8.4 y el AGP 8.2.2; solo dale "OK" / "Download").

## 2. Conectar la app a tu servidor

La URL del backend está centralizada en `app/build.gradle`, en el campo
`BASE_URL`:

- **Modo debug (para probar en el emulador de Android Studio)**:
  ```
  http://10.0.2.2:3000/
  ```
  `10.0.2.2` es la forma en que el **emulador** ve a tu propio
  computador. Así que:
  1. Corre tu servidor normal: `npm start` (o el comando que uses) desde
     la carpeta `rfragancias`.
  2. Corre la app en el **emulador** de Android Studio (no en un celular
     físico, porque un celular físico no puede ver "localhost" de tu PC
     así como así).

- **Modo release (cuando publiques la app de verdad)**: cambia el valor
  en el bloque `release` de `app/build.gradle` por la URL pública de tu
  servidor, por ejemplo la de Render:
  ```groovy
  buildConfigField "String", "BASE_URL", "\"https://tu-app.onrender.com/\""
  ```

> Si quieres probar desde un celular físico contra tu servidor local,
> usa una herramienta como [ngrok](https://ngrok.com) para exponer tu
> `localhost:3000` a internet, y pon esa URL (con `https://`) en
> `BASE_URL`.

## 3. Usuario y clave de administrador

Son los mismos que ya tienes configurados en el `.env` de tu servidor
(`ADMIN_USER` y `ADMIN_PASSWORD`). Por defecto, si no configuraste nada,
son:

- Usuario: `admin`
- Clave: `admin`

## 4. Qué incluye la app

### Catálogo público (pestaña "Catálogo")
- Ver todos los perfumes activos, con foto, diseñador, notas y género.
- Buscar por nombre/diseñador/notas y filtrar por género (chips).
- Ver el detalle de un perfume.
- Botón de WhatsApp para preguntar por una fragancia (usa el mismo
  número que la web).

### Zona de administración (pestaña "Admin")
Pide inicio de sesión la primera vez; luego la sesión queda guardada en
el dispositivo (igual que una cookie de navegador) hasta que cierres
sesión.

- **Dashboard**: resumen con total de perfumes, clientes, ventas del
  mes, deuda total, pedidos pendientes y perfumes con stock bajo.
- **Catálogo (admin)**: crear, editar (con foto desde la galería),
  activar/ocultar y eliminar perfumes.
- **Clientes**: crear clientes, ver su detalle (deuda, historial de
  ventas y de abonos) y registrar abonos.
- **Ventas**: listar, registrar (cliente/perfume opcionales, cantidad,
  total, estado) y eliminar ventas.
- **Pedidos de materia prima**: listar, registrar y eliminar pedidos a
  proveedores.

## 5. Estructura del proyecto

```
RFraganciasApp/
├── app/
│   ├── build.gradle              ← aquí está BASE_URL
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/rfragancias/app/
│       │   ├── model/            ← Perfume, Cliente, Venta, Pedido...
│       │   ├── network/          ← Retrofit, OkHttp, sesión/cookies
│       │   ├── ui/catalogo/      ← pantallas públicas
│       │   ├── ui/admin/         ← login + panel de administración
│       │   └── utils/            ← formato de moneda, helpers
│       └── res/
│           ├── layout/           ← TODAS las pantallas en XML
│           ├── values/           ← colores, strings, tema (oscuro,
│           │                       terracota, igual que el logo web)
│           └── drawable/, menu/, color/, mipmap-*/
```

## 6. Notas técnicas

- **Networking**: Retrofit + OkHttp, con un `CookieJar` persistente que
  guarda la cookie de sesión (`cookie-session`) igual que lo haría un
  navegador, para que el login sobreviva a cerrar y abrir la app.
- **Imágenes**: se cargan con Glide, uniendo `BASE_URL` con la ruta que
  guarda el servidor (ej. `/uploads/foto.jpg`).
- **Subida de fotos de perfume**: se hace con `multipart/form-data`,
  igual que el formulario web.
- **Tema visual**: se tomó la paleta de `public/css/styles.css` del
  proyecto web (fondo oscuro `#0D0C0A`, acento terracota `#DE9B45`,
  texto crema `#F4EEE2`) para que la app se sienta parte de la misma
  marca.

## 7. Próximos pasos sugeridos

- Generar un ícono de la app en alta resolución (el actual es un
  placeholder simple con los colores de la marca).
- Si vas a publicar en Play Store, sube el backend a un servicio con
  HTTPS (Render, Railway, etc.) y usa esa URL en el build `release`.
- Considera agregar notificaciones push si quieres avisar a los clientes
  sobre nuevas fragancias.
