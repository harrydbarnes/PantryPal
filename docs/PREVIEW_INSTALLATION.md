# Install PantryPal Preview alongside PantryPal

| Build | Installed package | Launcher name |
|---|---|---|
| Existing debug/release | `com.example.pantrypal` | PantryPal |
| Preview | `com.example.pantrypal.preview` | PantryPal Preview |

Run the **Build Android APK** workflow manually with **Build a signed Preview APK** enabled, then install the preview APK from the `pantrypal-preview-apk` artifact. It installs as a second app and starts with its own pantry, preferences, permissions, widgets and sign-in. The preview launcher icon has a purple P badge. Future signed preview APKs update this preview installation.

The normal debug/release packages retain their existing identities. The preview has separate provider authorities, and its voice shortcuts target the preview package. Android emulator CI installs both packages, launches both and checks that the same private filename has independent contents in each app.

To try existing kitchen data, export a complete backup from PantryPal and restore that file in the disconnected preview app. Nothing is copied automatically. The apps use the same Firebase project: explicitly joining the same shopping household will share shopping changes between them.

Distributed preview APKs use the protected signing certificate already configured in GitHub secrets; no private signing material is committed. Firebase has a separate Android registration/configuration for the preview package. A local `./gradlew :app:assemblePreview` build falls back to the developer's debug certificate when release signing is not supplied, so its Google sign-in requires that certificate to be registered separately.
