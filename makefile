JVMFLAGS = -cp build
JCFLAGS = -cp src -d build
JC = javac
JVM= java 
JVMFLAGS = -cp build
PKG=mnkgame
TEST=MNKPlayerTester
MAIN=$(PKG).MNKGame 
M:=5
N:=5
K:=4
REPS:=10
TIME:=10
BEST:=S
OLD:=old

.SUFFIXES: .java .class
.PHONY: build

best: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) mnkgame.$(BEST)

a: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(K) mnkgame.$(BEST) mnkgame.$(OLD) -r $(REPS)

v: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(K) mnkgame.$(BEST) mnkgame.$(OLD) -v -t $(TIME) -r $(REPS)

pp: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) mnkgame.$(BEST) mnkgame.$(OLD)


build: 
	mkdir -p build
	$(JC) $(JCFLAGS) src/mnkgame/*.java src/mnkgame/*.java

clean:
	rm -rf build