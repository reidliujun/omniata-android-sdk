#!/bin/sh

if [ ! -n "$1" ]; then
    echo "Give the version as a parameter"
    exit -1
fi

VERSION=$1

ECLIPSE_RELEASE_JAR=bin/omniata-android-sdk.jar
RELEASE_JAR=omniata-android-sdk.jar
if [ ! -f "$ECLIPSE_RELEASE_JAR" ]; then
    echo "Release jar-file missing: $ECLIPSE_RELEASE_JAR. It should be automatically build be Eclipse"
    exit -1
fi

PAGES_REPOSITY_DIR=../Omniata.github.io
if [ ! -d "$PAGES_REPOSITY_DIR" ]; then
    echo "No Github pages clone in $PAGES_REPOSITY_DIR. Clone that first with 'git clone git@github.com:Omniata/Omniata.github.io.git'"
    exit -1
fi

cp $ECLIPSE_RELEASE_JAR $RELEASE_JAR
git commit -m "Latest release jar" $RELEASE_JAR

git tag -a v$VERSION -m "v${VERSION}"
git push -u origin master

# Clean and run doxygen
echo "Creating APIdoc"
rm -rf javadoc
mkdir javadoc
javadoc -classpath src -d javadoc -public com.omniata.android.sdk

# Deploy docs and binary
echo "Copying to Omniata repository"
DIR=`pwd`

PAGES_DIR_RELATIVE=docs/sdks/android/$VERSION
PAGES_DIR=../Omniata.github.io/$PAGES_DIR_RELATIVE

rm -rf $PAGES_DIR
mkdir $PAGES_DIR
mkdir $PAGES_DIR/apidoc
cp -r javadoc/* $PAGES_DIR/apidoc/
cp $RELEASE_JAR $PAGES_DIR/

echo "Commiting and pushing"
cd $PAGES_REPOSITY_DIR
git pull
git add $PAGES_DIR_RELATIVE
git commit -m "iOS SDK ${VERSION}" $PAGES_DIR_RELATIVE
git push -u origin master

echo "Ready version $VERSION"

