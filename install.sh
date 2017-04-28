#!/bin/sh

# Installer

set -eu

prodname=potaufeu
ver=1.0.0-beta5
owner=argius
scriptname=potf

dir=~/.$prodname
baseurl=https://github.com/$owner/$prodname
bindir=$dir/bin
libdir=$dir/lib
zipfile=${prodname}-${ver}-bin.zip
jarfile=${prodname}-${ver}.jar
scriptfile=$bindir/$scriptname

errexit() {
  printf "\033[31m[ERROR]\033[0m" ; echo " $1"
  echo "Installation incomplete."
  exit 1
}

echo "This is the installer of \"$prodname\"."
echo ""
echo "Installs version $ver into $dir"
echo "and uses /tmp/$prodname as working directory."
mkdir -p /tmp/$prodname && cd /tmp/$prodname
curl -fsSLO $baseurl/releases/download/v$ver/$zipfile || errexit "failed to download zip"
unzip -o $zipfile $jarfile || errexit "failed to unzip"

mkdir -p $libdir && cp -fp *${ver}.jar $libdir/
test -f $libdir/$jarfile || errexit "failed to copy jar file"
mkdir -p $bindir && ( echo "#!/bin/sh" ; echo "java -jar $libdir/$jarfile \$@" ) > $scriptfile
test -f $scriptfile || errexit "failed to create script"
install $scriptfile /usr/local/bin || errexit "failed to install script"

echo "\"$prodname\" was installed to /usr/local/bin and $dir/."
echo "Checking installation => `$scriptname --version`"
echo ""
echo "Installation completed."
rm -f /tmp/$prodname/${prodname}-* && rmdir /tmp/$prodname