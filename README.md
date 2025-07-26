# 💇‍♀️ MiraSalon KMP

**MiraSalon** is a Kotlin Multiplatform project built with Jetpack Compose Multiplatform and powered by a custom Ktor backend server. It targets **Android, iOS, Web, and Desktop** platforms, offering a full-featured salon management ecosystem.

It includes:
- A **Customer-facing Android app** for appointment booking and client engagement
- A **Salon Admin Dashboard** for managing appointments, staff schedules, inventory, payments, and marketing campaigns
- A **Custom Ktor Server** to handle APIs, authentication, and real-time updates

---

## ✨ Supported Features

- User Authentication
- Real-Time Appointment Booking
- Order Tracking
- Service Catalog Management
- Push Notifications
- Wishlist and Favorites
- Ratings and Reviews
- Promotions and Discounts
- Campaign Management
- Analytics and Reports Dashboard
- Responsive Design
- Customer Support
- Social Media Integration
- Booking Reminders
- Cross-device Session Sync

---

## 🏗️ Architecture

The project follows a modern **Shared Presenter Architecture** using **Slack Circuit**. This allows UI logic (Presenters) to be shared across platforms while keeping the UI (Compose) declarative.

- **Presenter (Circuit):** Handles state management using Compose-style logic.
- **UI (Compose Multiplatform):** Renders the shared state across Android, iOS, Desktop, and Web.
- **Domain Layer:** Uses UseCases and Repositories to manage business logic and data flow.
- **Data Layer:** Handles network (Ktor) and local persistence (Room for Mobile/Desktop, Exposed for Server).

---

## 🛠️ Libraries & Technologies

### 🚀 Shared / KMP
- **[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)** - Modern declarative UI shared across all platforms.
- **[Slack Circuit](https://github.com/slackhq/circuit)** - A reactive architecture for building Compose-based UIs.
- **[Koin](https://github.com/InsertKoinIO/koin)** - Lightweight dependency injection for Kotlin.
- **[Navigation 3](https://developer.android.com/jetpack/compose/navigation)** - Modern, type-safe multiplatform navigation for Compose.
- **[Ktor Client](https://github.com/ktorio/ktor)** - Multiplatform asynchronous HTTP client for networking.
- **[kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)** - Support for asynchronous programming.
- **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** - Type-safe JSON serialization.
- **[Room KMP](https://developer.android.com/kotlin/multiplatform/room)** - Local database persistence for Mobile and Desktop.
- **[Coil 3](https://github.com/coil-kt/coil)** - Image loading library for Compose Multiplatform.
- **[Kizitonwose Calendar](https://github.com/kizitonwose/Calendar)** - Highly customizable calendar library for Compose Multiplatform.
- **[Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)** - Persistent key-value storage for shared preferences.
- **[QRose](https://github.com/alexzhirkevich/qrose)** - QR code generation library for Compose Multiplatform.
- **[Molecule](https://github.com/cashapp/molecule)** - Transforms Compose-style logic into state streams.
- **[Napier](https://github.com/aakira/Napier)** - Versatile logger for Kotlin Multiplatform.
- **[Lucide Icons](https://github.com/composables/icons-lucide)** - Modern icon pack integrated for Compose.
- **[CMPToast](https://github.com/ChainTechNetwork/CMPToast)** - Customizable toast notifications for Compose Multiplatform.
- **[KSafe](https://github.com/anifantakis/KSafe)** - Type-safe, multiplatform navigation utility.
- **[SKIE](https://github.com/touchlab/SKIE)** - Enhances Swift interop for Kotlin Multiplatform.

### 🖥️ Backend (Ktor Server)
- **[Exposed](https://github.com/JetBrains/Exposed)** - Kotlin SQL framework for database access.
- **[Flyway](https://github.com/flyway/flyway)** - Database migration tool to manage schema changes.
- **PostgreSQL / H2** - Production and development database support.
- **JWT Auth** - Secure authentication using JSON Web Tokens.
- **[Micrometer](https://github.com/micrometer-metrics/micrometer)** - Metrics collection for Prometheus monitoring and observability.

### 🧪 Testing & Quality
- **[Kotest](https://github.com/kotest/kotest)** - Multiplatform testing framework.
- **[Mockative](https://github.com/mockative/mockative)** - Mocking library for KMP.
- **[Roborazzi](https://github.com/takahirom/roborazzi)** / **[Robolectric](https://github.com/robolectric/robolectric)** - Testing tools for Android.
- **[Detekt](https://github.com/detekt/detekt)** / **[Ktlint](https://github.com/pinterest/ktlint)** - Static analysis and linting for code quality.

---

## 📸 Screenshots
*(Coming Soon)*

---

## 🛠️ Getting Started

Clone the repo:

```bash
git clone https://github.com/mkaomwakuni/MiraSalon-KMP.git
```

### Running the Server
```bash
./gradlew :server:run
```
The server will log its local IP address on startup (e.g., `192.168.1.113`).

### Running the Android App on Physical Device
To connect a physical Android device to your local server, you must use your machine's LAN IP address.
Run the app with the `apiBaseUrl` property:
```bash
./gradlew :androidApp:installDebug -PapiBaseUrl=http://192.168.1.113:8080/
```
