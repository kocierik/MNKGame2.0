JVMFLAGS = -cp build
JCFLAGS = -cp src -d build
JC = javac
JVM= java 
JVMFLAGS = -cp build
PKG=mnkgame
MAIN=$(PKG).MNKGame 
M:=3
N:=3
K:=3
REPS:=5
TIME:=10
BEST:=S
OLD:=old

.SUFFIXES: .java .class
.PHONY: build

best: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) mnkgame.$(BEST)

build: 
	mkdir -p build
	$(JC) $(JCFLAGS) src/mnkgame/*.java src/mnkgame/*.java

clean:
	rm -rf build