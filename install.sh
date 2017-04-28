#!/bin/sh

# Installer

set -eu

prodname=potaufeu
ver=1.0.0-beta5
owner=argius
execname=potf
execdir=/usr/local/bin

dir=~/.$prodname
baseurl=https://github.com/$owner/$prodname
bindir=$dir/bin
libdir=$dir/lib
zipfile=${prodname}-${ver}-bin.zip
jarfile=${prodname}-${ver}.jar
jarpath=$libdir/$jarfile
binfile=$bindir/$execname
execfile=$execdir/$execname

errexit() {
  printf "\033[31m[ERROR]\033[0m" ; echo " $1"
  echo "Installation incomplete."
  exit 1
}

onexit() {
  if [ -n "$tmpdir" ]; then
    cd $tmpdir/..
    test -f $tmpdir/$jarfile && rm $tmpdir/$jarfile
    test -f $tmpdir/$zipfile && rm $tmpdir/$zipfile
    rmdir $tmpdir
  fi
}

tmpdir=`mktemp -d /tmp/${prodname}-XXXXXX`
trap onexit EXIT
trap "trap - EXIT; onexit; exit -1" 1 2 15 # SIGHUP SIGINT SIGTERM

echo "This is the installer of \"$prodname\"."
echo ""

# OS adjustment
case "`uname -a`" in
  CYGWIN* )
    echo "adjusting for Cygwin"
    jarpath=`cygpath -m $jarpath`
    echo ""
    ;;
  *ubuntu* )
    echo "adjusting for Ubuntu"
    if [ -d ~/.local/bin ] && [ -w ~/.local/bin ]; then
      execdir=~/.local/bin
    elif [ -d ~/bin ] && [ -w ~/bin ]; then
      execdir=~/bin
    elif [ -d /usr/local/bin ] && [ -w /usr/local/bin ]; then
      execdir=/usr/local/bin
    else
      errexit "cannot detect writable exec dir"
    fi
    execfile=$execdir/$execname
    echo ""
esac

echo "Installs version $ver into $execdir and $dir,"
echo "and uses $tmpdir as a working directory."
cd $tmpdir || errexit "failed to change directory"
echo "downloading: $zipfile"
curl -fsSLO $baseurl/releases/download/v$ver/$zipfile || errexit "failed to download zip"
unzip -o $zipfile $jarfile || errexit "failed to unzip"

mkdir -p $libdir && cp -fp $jarfile $libdir/
test -f $libdir/$jarfile || errexit "failed to copy jar file"
mkdir -p $bindir && ( echo "#!/bin/sh" ; echo "java -jar $jarpath \$@" ) > $binfile
test -f $binfile || errexit "failed to create $binfile"
chmod +x $binfile || errexit "failed to change a permission"
ln -sf $binfile $execfile || errexit "failed to create a symlink of $binfile"

echo "\"$prodname\" has been installed to $execdir and $dir/ ."
echo "Checking installation => `$execname --version`"
echo ""
echo "Installation completed."
