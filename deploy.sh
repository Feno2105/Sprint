#!/bin/zsh
set -euo pipefail

APP_NAME="framework"
SRC_DIR="src/main/java"
LIB_DIR="lib"
WAR_FILE="${APP_NAME}.war"

# Crée lib si nécessaire
mkdir -p "$LIB_DIR"

# Vérifier si jakarta.servlet-api.jar existe, sinon le télécharger

# Nettoyer ancien build
rm -rf build "$WAR_FILE"
mkdir -p build/WEB-INF/classes

# Construire le classpath
CP="$LIB_DIR/jakarta.servlet-api-5.0.0.jar"

# Trouver tous les fichiers .java récursivement
JAVA_FILES=($(find "$SRC_DIR" -name "*.java"))
if [ ${#JAVA_FILES[@]} -eq 0 ]; then
    echo "Aucun fichier .java trouvé dans $SRC_DIR"
    exit 1
fi

# Compilation
echo "Compilation des sources..."
javac -d build/WEB-INF/classes -cp "$CP" "${JAVA_FILES[@]}"

# Vérifier qu'on a bien des classes compilées
if [ ! -d build/WEB-INF/classes ]; then
    echo "Erreur : compilation échouée, build/WEB-INF/classes vide"
    exit 1
fi

# Créer le WAR
echo "Création du WAR..."
cd build
jar -cvf "../$WAR_FILE" . > /dev/null
cd ..

# Nettoyage (optionnel)
# rm -rf build

echo "$WAR_FILE généré avec succès ✅"
echo "Tu peux maintenant le déployer dans tomcat/webapps/"
