#!/bin/zsh
set -euo pipefail

# Nom du JAR final
JAR_NAME="FrontController.jar"

# Dossiers source et classes
SRC_DIR="src/main/java"
BUILD_DIR="build/classes"
LIB_DIR="lib"

# Nettoyage build précédent
rm -rf "$BUILD_DIR" "$JAR_NAME"
mkdir -p "$BUILD_DIR"

# Vérifie que jakarta.servlet-api.jar est présent dans lib/

CP="$LIB_DIR/jakarta.servlet-api-5.0.0.jar"

# Compilation de tous les fichiers .java récursivement
echo "Compilation des sources..."
find "$SRC_DIR" -name "*.java" -print0 | xargs -0 javac -d "$BUILD_DIR" -cp "$CP"

# Vérification que des classes ont été compilées
if [ -z "$(ls -A $BUILD_DIR)" ]; then
    echo "Erreur : aucune classe compilée. Vérifie tes fichiers .java et leur package."
    exit 1
fi

# Création du JAR
echo "Création du JAR..."
cd "$BUILD_DIR"
jar -cvf "../../$JAR_NAME" . > /dev/null
cd ../..

echo "$JAR_NAME généré avec succès ✅"
echo "Tu peux maintenant le mettre dans WEB-INF/lib/ de ton domaine de test"
