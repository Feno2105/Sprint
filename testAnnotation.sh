#!/bin/zsh
set -euo pipefail

APP_NAME="framework_bis"
SRC_DIR="src/main/java"
LIB_DIR="lib"
WAR_FILE="${APP_NAME}.war"

# Crée lib si nécessaire

# Vérifier si jakarta.servlet-api.jar existe, sinon le télécharger

# Nettoyer ancien build
rm -rf build 
mkdir -p build/WEB-INF/classes

# Construire le classpath

# Trouver tous les fichiers .java récursivement
JAVA_FILES=($(find "$SRC_DIR" -name "*.java"))
if [ ${#JAVA_FILES[@]} -eq 0 ]; then
    echo "Aucun fichier .java trouvé dans $SRC_DIR"
    exit 1
fi

# Compilation
echo "Compilation des sources..."
javac -d build/WEB-INF/classes "${JAVA_FILES[@]}"

# Vérifier qu'on a bien des classes compilées
if [ ! -d build/WEB-INF/classes ]; then
    echo "Erreur : compilation échouée, build/WEB-INF/classes vide"
    exit 1
fi

# Créer le WAR


# Nettoyage (optionnel)
# rm -rf build
cd build/WEB-INF/classes

java com.itu.hello.Main
