JVMFLAGS = -cp build
JCFLAGS = -cp src -d build
JC = javac
JVM= java 
JVMFLAGS = -cp build
PKG=mnkgame
TEST=MNKPlayerTester
MAIN=$(PKG).MNKGame 
M:=6
N:=4
K:=4
REPS:=2 # game to play
TIME:=10
BEST:=newPlayer
OLD:=S

.SUFFIXES: .java .class
.PHONY: build

best: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) mnkgame.$(BEST)

a: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(K) mnkgame.$(BEST) mnkgame.$(OLD) -r $(REPS)

a2: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(K) mnkgame.$(OLD) mnkgame.$(BEST) -r $(REPS)

v: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(K) mnkgame.$(BEST) mnkgame.$(OLD) -v -t $(TIME) -r $(REPS)

v2: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(K) mnkgame.$(OLD) mnkgame.$(BEST) -v -t $(TIME) -r $(REPS)

pp: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) mnkgame.$(BEST) mnkgame.$(OLD)

pp2: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) mnkgame.$(OLD) mnkgame.$(BEST) 


build: 
	mkdir -p build
	$(JC) $(JCFLAGS) src/mnkgame/*.java src/mnkgame/*.java

clean:
	rm -rf build