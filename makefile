JVMFLAGS = -cp build
JCFLAGS = -cp src -d build
JC = javac
JVM= java 
JVMFLAGS = -cp build

PKG=mnkgame
MAIN=$(PKG).MNKGame 
M:=6
N:=5
K:=4
REPS:=5
TIME:=10
BEST:=S
OLD:=old

best: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) $(PKG).$(BEST)

test1: build
	$(JVM) $(JVMFLAGS) mnkgame.MNKPlayerTester $(M) $(N) $(K) $(OLD) $(PKG).$(BEST) -v -r $(REPS)

build: $(CLASSES)
	mkdir -p build
	$(JC) $(JCFLAGS) src/mnkgame/*.java src/mnkgame/*.java

clean:
	rm -rf build