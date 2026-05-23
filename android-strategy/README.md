# Android Strategy Pattern — ICTU-Ex Backend Integration

## What this is

All files in this folder are ready to drop into the Android project.
They implement the **Strategy Pattern** so the app can talk to either
**Firebase** or the **Spring Boot backend** by changing one line.

---

## File Map → Where to copy each file

```
android-strategy/                          Android project destination
│
├── api/
│   ├── AuthService.kt          →   data/remote/api/AuthService.kt
│   ├── ListingService.kt       →   data/remote/api/ListingService.kt
│   └── MessagingService.kt     →   data/remote/api/MessagingService.kt
│
├── firebase/
│   ├── FirebaseAuthService.kt      →   data/remote/firebase/FirebaseAuthService.kt
│   ├── FirebaseListingService.kt   →   data/remote/firebase/FirebaseListingService.kt
│   └── FirebaseMessagingService.kt →   data/remote/firebase/FirebaseMessagingService.kt
│
├── spring/
│   ├── RetrofitClient.kt           →   data/remote/spring/RetrofitClient.kt
│   ├── IctuExApiService.kt         →   data/remote/spring/IctuExApiService.kt
│   ├── TokenStore.kt               →   data/remote/spring/TokenStore.kt
│   ├── SpringAuthService.kt        →   data/remote/spring/SpringAuthService.kt
│   ├── SpringListingService.kt     →   data/remote/spring/SpringListingService.kt
│   └── SpringMessagingService.kt   →   data/remote/spring/SpringMessagingService.kt
│
├── di/
│   └── ServiceModule.kt        →   di/ServiceModule.kt
│                                   (replace or merge with existing Hilt module)
│
├── AuthViewModel.example.kt    →   feature/auth/AuthViewModel.kt   (reference)
├── ListingViewModel.example.kt →   feature/home/ListingViewModel.kt (reference)
└── build.gradle.kts.snippet    →   merge into app/build.gradle.kts
```

---

## How to switch backends

### 1. Add to `local.properties` (never committed to git)
```properties
# true  = calls https://api.ictuex.teamnest.me  (Spring Boot)
# false = calls Firebase (original behaviour)
useSpringBackend=true
```

### 2. That's it. Rebuild the app.
`ServiceModule.kt` reads `BuildConfig.USE_SPRING_BACKEND` and injects
the right implementation everywhere automatically.

---

## Integration steps (in order)

### Step 1 — Add Retrofit + Moshi to app/build.gradle.kts
Copy the contents of `build.gradle.kts.snippet` into your app module gradle file.

### Step 2 — Copy the `api/` interfaces
These are the contracts. Every ViewModel and Repository will depend on these.

### Step 3 — Copy the `firebase/` wrappers
These wrap your existing Firebase calls. Your existing logic stays intact —
it's just moved inside these classes.

### Step 4 — Copy the `spring/` implementations
These call the live API at `https://api.ictuex.teamnest.me`.

### Step 5 — Replace your existing Hilt module
Copy `di/ServiceModule.kt` and merge it with your existing `AppModule.kt`
or `NetworkModule.kt`. Remove any direct Firebase bindings that are now
handled here.

### Step 6 — Update your ViewModels
Change any ViewModel that directly uses `FirebaseAuth` or `FirebaseFirestore`
to instead inject `AuthService`, `ListingService`, or `MessagingService`.
See the `.example.kt` files for the pattern.

---

## Architecture diagram

```
ViewModel
    │
    │  inject AuthService (interface)
    ▼
ServiceModule (Hilt)
    │
    ├── BuildConfig.USE_SPRING_BACKEND = true
    │       └── SpringAuthService  ──→  Retrofit  ──→  https://api.ictuex.teamnest.me
    │
    └── BuildConfig.USE_SPRING_BACKEND = false
            └── FirebaseAuthService  ──→  Firebase Auth + Firestore
```

---

## API base URL
```
Production:  https://api.ictuex.teamnest.me
Local dev:   http://10.0.2.2:8081   (Android emulator → localhost)
```

To use local dev, change `RetrofitClient.BASE_URL` to `http://10.0.2.2:8081/`
or make it read from `BuildConfig` as well.

---

## Notes
- `TokenStore` uses plain `SharedPreferences`. For production, replace with
  `EncryptedSharedPreferences` from `androidx.security.crypto`.
- The Firebase implementations are intentionally minimal wrappers.
  If your existing Firebase code has more logic (e.g. Room caching),
  keep it inside the Firebase implementation classes — the interface stays the same.
- `runCatching { }` wraps all calls in `Result<T>` so ViewModels never crash
  on network errors — they just receive `Result.failure(...)`.
