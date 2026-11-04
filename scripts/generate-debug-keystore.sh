#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Keystore de DEBUG (non secret)
# =============================================================================
# app/build.gradle.kts pointe le buildType debug vers ${rootDir}/debug.keystore
# (convention AI Studio). Sans ce fichier, `./gradlew assembleDebug` échoue.
# Ce keystore n'a AUCUNE valeur de sécurité : il est jetable et recréé à la
# demande (identifiants standards : android / androiddebugkey / android).
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f debug.keystore ]; then
  echo "✓ debug.keystore déjà présent — rien à faire."
  exit 0
fi

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
elif command -v keytool >/dev/null 2>&1; then
  KEYTOOL="$(command -v keytool)"
else
  echo "❌ keytool introuvable. Installez un JDK 17+ (ou le JBR d'Android Studio)."
  exit 1
fi

"$KEYTOOL" -genkeypair \
  -keystore debug.keystore \
  -alias androiddebugkey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug, O=Android, C=US"

chmod 600 debug.keystore
echo "✅ debug.keystore créé."
