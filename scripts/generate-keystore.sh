#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Génération du keystore de signature (release / upload)
# =============================================================================
# Crée le fichier `my-upload-key.jks` utilisé par app/build.gradle.kts
# (bloc signingConfigs -> "release", alias OBLIGATOIRE : upload).
#
# Usage :
#   bash scripts/generate-keystore.sh
#   STORE_PASSWORD='MonMotDePasseSolide' bash scripts/generate-keystore.sh
#
# Variables optionnelles :
#   KEYSTORE_FILE   (défaut : my-upload-key.jks, à la racine du projet)
#   KEY_ALIAS       (défaut : upload      — NE PAS CHANGER)
#   STORE_PASSWORD  (si absente, saisie interactive)
#   DNAME           (défaut : CN=Lumina-beta, OU=Mobile, O=Lumina, L=Kandi, ST=Alibori, C=BJ)
#
# ⚠️  Le mot de passe de la clé est identique au mot de passe du store
#     (obligatoire pour un keystore PKCS12, format par défaut des JDK modernes).
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

KEYSTORE_FILE="${KEYSTORE_FILE:-my-upload-key.jks}"
KEY_ALIAS="${KEY_ALIAS:-upload}"
VALIDITY_DAYS="${VALIDITY_DAYS:-10950}" # 30 ans
KEY_SIZE="${KEY_SIZE:-4096}"
DNAME="${DNAME:-CN=Lumina-beta, OU=Mobile, O=Lumina, L=Kandi, ST=Alibori, C=BJ}"

if [ "$KEY_ALIAS" != "upload" ]; then
  echo "⚠️  Attention : app/build.gradle.kts utilise keyAlias = \"upload\"."
  echo "    Un alias différent fera échouer la signature. Alias choisi : $KEY_ALIAS"
fi

# --- Localisation de keytool -------------------------------------------------
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
elif command -v keytool >/dev/null 2>&1; then
  KEYTOOL="$(command -v keytool)"
elif [ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" ]; then
  KEYTOOL="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
else
  echo "❌ keytool introuvable."
  echo "   Installez un JDK 17+ (ex. Temurin) ou utilisez le JDK embarqué d'Android Studio,"
  echo "   puis relancez : export JAVA_HOME=/chemin/vers/jdk"
  exit 1
fi

echo "▶ keytool : $KEYTOOL"
"$KEYTOOL" -version 2>&1 | head -1

# --- Mot de passe ------------------------------------------------------------
if [ -z "${STORE_PASSWORD:-}" ]; then
  read -r -s -p "Mot de passe du keystore (min. 6 caractères) : " STORE_PASSWORD; echo
  read -r -s -p "Confirmez le mot de passe : " STORE_PASSWORD_CONFIRM; echo
  if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
    echo "❌ Les mots de passe ne correspondent pas."
    exit 1
  fi
fi

if [ "${#STORE_PASSWORD}" -lt 6 ]; then
  echo "❌ Mot de passe trop court (keytool exige au moins 6 caractères)."
  exit 1
fi

# --- Garde-fou : ne jamais écraser une clé existante -------------------------
if [ -f "$KEYSTORE_FILE" ]; then
  echo "❌ $KEYSTORE_FILE existe déjà."
  echo "   Écraser une clé de signature casse définitivement les mises à jour publiées."
  echo "   Déplacez/désarchivez l'ancien fichier, ou utilisez KEYSTORE_FILE=autre-nom.jks."
  exit 1
fi

# --- Génération --------------------------------------------------------------
echo "▶ Génération de $KEYSTORE_FILE (RSA $KEY_SIZE, validité $VALIDITY_DAYS jours)..."
"$KEYTOOL" -genkeypair \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize "$KEY_SIZE" \
  -validity "$VALIDITY_DAYS" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$STORE_PASSWORD" \
  -dname "$DNAME"

chmod 600 "$KEYSTORE_FILE"

# --- Vérification ------------------------------------------------------------
echo "▶ Vérification du keystore :"
"$KEYTOOL" -list -v -keystore "$KEYSTORE_FILE" -storepass "$STORE_PASSWORD" \
  | grep -E "Alias name|Valid from|Signature algorithm|Owner" || true

# --- Empreinte + base64 pour GitHub Secrets ----------------------------------
FINGERPRINT="$("$KEYTOOL" -list -v -keystore "$KEYSTORE_FILE" -storepass "$STORE_PASSWORD" \
  | grep -m1 "SHA256:" | sed 's/.*SHA256: //')"

ABS_KEYSTORE="$(cd "$(dirname "$KEYSTORE_FILE")" && pwd)/$(basename "$KEYSTORE_FILE")"
BASE64_FILE="$KEYSTORE_FILE.base64"
base64 -w0 "$KEYSTORE_FILE" > "$BASE64_FILE" 2>/dev/null || base64 "$KEYSTORE_FILE" | tr -d '\n' > "$BASE64_FILE"
chmod 600 "$BASE64_FILE"

cat <<EOF

=============================================================================
✅ Keystore créé : $KEYSTORE_FILE
   Alias          : $KEY_ALIAS
   SHA-256        : ${FINGERPRINT:-voir keytool -list -v}
   Base64 (secret): $BASE64_FILE
=============================================================================

⚠️  À FAIRE IMMÉDIATEMENT
 1. SAUVEGARDER $KEYSTORE_FILE + le mot de passe hors du dépôt
    (gestionnaire de mots de passe + copie chiffrée sur clé USB).
    Si vous perdez ce fichier, vous ne pourrez PLUS publier de mise à jour
    de l'application sous le même applicationId.

 2. Ne JAMAIS committer $KEYSTORE_FILE ni $BASE64_FILE (.gitignore le bloque).

 3. Enregistrer les 3 secrets GitHub (dépôt > Settings > Secrets > Actions) :

    gh secret set KEYSTORE_BASE64 < $BASE64_FILE
    gh secret set STORE_PASSWORD            # collez le mot de passe à l'invite
    gh secret set KEY_PASSWORD              # même mot de passe

 4. Build release local (APK signé) — le mot de passe n'est jamais affiché
    ni écrit sur le disque :

    export KEYSTORE_PATH="$ABS_KEYSTORE"
    read -r -s -p "Mot de passe du keystore : " STORE_PASSWORD; echo
    export KEY_PASSWORD="\$STORE_PASSWORD"
    ./gradlew assembleRelease
    # APK : app/build/outputs/apk/release/

Puis poussez un tag pour déclencher la CI de release :

    git tag v1.0.1 && git push origin v1.0.1

=============================================================================
EOF
