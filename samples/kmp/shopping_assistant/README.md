# AP2 Shopping Assistant — Kotlin Multiplatform

A Kotlin Multiplatform sample app demonstrating the AP2 protocol for agentic commerce, targeting **Android** and **iOS** from a shared codebase.

## Architecture

```
├── composeApp/      # Shared KMP module (business logic + Compose UI)
│   ├── commonMain/  # Cross-platform code
│   ├── androidMain/ # Android-specific implementations
│   └── iosMain/     # iOS-specific implementations
├── androidApp/      # Android entry point (thin)
└── iosApp/          # iOS entry point (SwiftUI wrapper)
```

### Key Packages

| Package | Description |
|---|---|
| `ap2.a2a` | A2A protocol client, message builder, and types |
| `ap2.dpc` | DPC/OpenID4VP/DCQL types and request builder |
| `ap2.model` | Shopping data models (cart, payment, intent) |
| `agent` | Business logic (Gemini AI, shopping tools, chat repository) |
| `platform` | Platform abstractions (`expect`/`actual` for logging, credentials, etc.) |
| `ui` | Shared Compose Multiplatform UI (ChatScreen, SettingsScreen, Theme) |

## Prerequisites

- Android Studio (with KMP support)
- Xcode (for iOS)
- JDK 11+

## Setup

1. Create `local.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

2. **Android**: Open in Android Studio → Run `androidApp`
3. **iOS**: Run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`, then open `iosApp/iosApp.xcodeproj` in Xcode → Run

## Technology Stack

- **Kotlin** 2.3.0 / **AGP** 9.0.0
- **Compose Multiplatform** 1.10.0
- **Ktor** 3.1.3 (HTTP client)
- **kotlinx-serialization** 1.8.1
- **multiplatform-settings** 1.3.0 (preferences)
- **Gemini REST API** (direct Ktor client, no SDK dependency)

## Notes

- **DPC on iOS**: The Android CredentialManager API has no iOS equivalent yet. The iOS implementation is a stub placeholder.
- **AP2 SDK**: The `ap2` package is structured for future extraction as a standalone SDK.
