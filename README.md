# Android Bluetooth Trackpad

Cette application transforme votre téléphone Android en un trackpad Bluetooth pour votre tablette ou tout autre appareil supportant les souris Bluetooth. Elle utilise le profil Bluetooth HID (Human Interface Device) pour simuler une souris standard sans avoir besoin d'installer d'application tierce sur l'appareil cible.

## Fonctionnalités

- **Mouvement relatif du curseur** : Glissez un doigt pour déplacer le curseur.
- **Clic gauche** : Touchez l'écran avec un doigt.
- **Clic droit** : Touchez l'écran avec deux doigts simultanément.
- **Défilement (Scroll)** : Glissez deux doigts verticalement.
- **Appairage facile** : Bouton pour rendre le téléphone détectable directement depuis l'application.

## Prérequis

- **Téléphone (Émetteur)** : Android 9 (API 28) ou version ultérieure. Le matériel doit supporter le profil Bluetooth HID Device (certains constructeurs désactivent cette fonctionnalité).
- **Tablette/Cible (Récepteur)** : Tout appareil supportant les souris Bluetooth (Android, iPadOS, Windows, macOS, Linux).

## Installation et Utilisation

### Compilation depuis les sources

1. Clonez ce dépôt ou copiez les fichiers.
2. Ouvrez le projet dans **Android Studio** (Hedgehog ou plus récent recommandé).
3. Connectez votre téléphone Android avec le débogage USB activé.
4. Cliquez sur **Run 'app'**.

### Utilisation

1. Lancez l'application sur votre téléphone.
2. Cliquez sur le bouton **"Appairer"**. Votre téléphone devient détectable par les autres appareils Bluetooth.
3. Sur votre tablette, allez dans les paramètres Bluetooth et recherchez **"BTTrackpad"**.
4. Appairez les deux appareils.
5. Une fois connecté, la zone grise sur votre téléphone devient votre trackpad !

## Structure du Projet

- `MainActivity.kt` : Gère l'interface utilisateur Jetpack Compose, la détection des gestes et les permissions.
- `BluetoothHidService.kt` : Gère l'enregistrement du profil HID et l'envoi des rapports de souris.
- `HidDescriptor.kt` : Définit le descripteur de rapport HID pour une souris standard.

## Limitations techniques

Certains smartphones Android ne supportent pas le mode "HID Device" même s'ils sont sous Android 9+. L'application affichera un message d'avertissement en rouge si elle détecte que le profil n'est pas disponible.

---
Développé avec ❤️ pour un usage pratique et mobile.
