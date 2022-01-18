package mnkgame;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.security.*;
public class S implements MNKPlayer {
	private static MNKGameState myWin;
	private static MNKGameState yourWin;
  private static MNKCellState myCell;
	private MNKBoard B;
  private int M,N,K, minMN;
  private static int MAX = 10_000_000, MIN = -MAX;
  private static MNKCell lastMarked;
	private int TIMEOUT;
	private int TIMEOUT_VALUE = MAX+1;
	private long start;
	private Random rand;
	private SecureRandom random;
	private long[][][] zobristTable;
  // 6 * 4 (int) * TRANSPOSITION_TABLE_LENGTH = 
  private static int TRANSPOSITION_TABLE_LENGTH = 1024 * 1024 * 4;
  private static int TRANSPOSITION_ENTRY_LENGTH = 6;
  private static int TRANSPOSITION_ENTRY_NOT_FOUND = Integer.MAX_VALUE;
  private static int TRANSPOSITION_KIND_EXACT = 0,
    TRANSPOSITION_KIND_LOWER = -1, TRANSPOSITION_KIND_UPPER = 1;
  private static int[] times_score = new int[8];
        
  // transposition entry structure: [
  // first 32 bits of the hash key, 
  // last 32 bits of the hash key, 
  // searchDepth, 
  // bestMove, 
  // value, 
  // value kind (one of TRANSPOSITION_KIND_*)
  // ]
  private int[][] transposition;

  // TODO: remove, debug only
  private int tre = 0, tro = 0, trh = 0, trm = 0;

	/**
	 * Default empty constructor
	 */
	public S() {}

	// Classe di inizializzazione del gioco
	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis()); 
		B       = new MNKBoard(M,N,K);
    this.M = M;
    this.N = N;
    this.K = K;
    this.minMN = Math.min(M,N);
		myWin   = first ? MNKGameState.WINP1 : MNKGameState.WINP2; 
		yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
		myCell   = first ? MNKCellState.P1 : MNKCellState.P2;
		TIMEOUT = timeout_in_secs;	
    currentHash = 0;
    random = new SecureRandom();
		zobristTable = new long [M][N][2];
    fillZobristHashes();
    transposition = new int[TRANSPOSITION_TABLE_LENGTH][];
    tre = tro = trh = trm = 0;
	}
//--------------------------------------------------------------------------------

	/* Valutazione dello stato di gioco:
		1 = gioco aperto
		0 = pareggio
		10 = vittoria
		-10 = sconfitta
	*/
  int value;
	public int evaluate() {
		MNKGameState state = B.gameState();
		if(state == MNKGameState.OPEN){
      value =  heuristic();
      System.out.println("valore dell'euristica = " + value);
      return value;
    }
		else if(state == MNKGameState.DRAW) return 0;
		else if(state == myWin) return M*N*1_000;
    else return -M*N*1_000;
  }

public int longSeriesBonus(int n, int consecutive, int marked){
  if(n>=K){
    if(consecutive == K-1) return 1_000; //1k
    if(consecutive == K-2) return 100;   //100
    if(consecutive == K-3) return 10;    //10
    else return (marked/n) * 100;
  }
  return 0;
}

//player 1 = io, player 2 = nemico
public int depthCell(int i, int j, int dir_i, int dir_j, int maxIter){
  int value = 0;
  //last_player è 1 o 2
  int lastPlayer = 0;
  int prev = -1;
  int marked = 0, series = 0, maxSeries = 0;
  int c1series = 0, c2series = 0, lastFreeSeries = 0;
  for(int z=0;z<maxIter;z++){
    if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P1){
      if(lastPlayer==2){
        value -= longSeriesBonus(c2series, maxSeries,marked);
        maxSeries = 0;
        c2series = 0;
        c1series = 1 + lastFreeSeries;
        marked = 1;
      }
      else{
        if(prev==1) series++;
        else{
          if(series>maxSeries) maxSeries = series;
          series = 1;
        }
        marked++;
        c1series++;
      }
      lastFreeSeries = 0;
      lastPlayer = 1;
      prev = 1;
    }
    else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P2){
      if(lastPlayer==1){
        value += longSeriesBonus(c1series, maxSeries, marked);
        maxSeries = 0;
        c1series = 0;
        c2series = 1 + lastFreeSeries;
        marked = 1;
      }
      else{
        if(prev==2) series++;
        else {
          if(series>maxSeries) maxSeries = series;
          series = 1;
        }
        marked++;
        c2series++;
      }
      lastFreeSeries = 0;
      lastPlayer = 2;
      prev = 2;
    }
    else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.FREE){
      if(lastPlayer==1){
        c1series++;
      }
      if(lastPlayer==2){
        c2series++;
      }
      if(prev!=1 && prev!=2){
        lastFreeSeries++;
      }
      else lastFreeSeries = 1;
    }
  }
  return value;
}

public int heuristic() {
  int i,j,value = 0;
  //row
  for (i = 0; i < M; i++) {
    j = 0;
    value += depthCell(i,j,0,1,N);
  }
  //column
  for (j = 0; j < N; j++) {
    i = 0;
    value += depthCell(i,j,1,0,M);
  }
  
  int maxLen = minMN;
  int nMaxDiag = Math.abs(M-N)+1;
  //diagonal
  if(M>=N){
    for (j = 1; j < N; j++) {
      if(maxLen-j>=K){
        i = 0;
        value += depthCell(i,j,1,1,maxLen-j);
      }
      else break;
    }
    for(i = 0; i < M; i++){
      if(i<nMaxDiag){
        j=0;
        value += depthCell(i,j,1,1,maxLen);
      }
      else if(maxLen-(i+1-nMaxDiag)>=K){
        value += depthCell(i,j,1,1,maxLen-(i+1-nMaxDiag));
      }
      else break;
    }
  }
  else{
    for (j = 0; j < N; j++) {
      if(j<nMaxDiag){
        i = 0;
        value += depthCell(i,j,1,1,maxLen);
      }
      else if(maxLen-(j+1-nMaxDiag)>=K){
        value += depthCell(i,j,1,1,maxLen-(j+1-nMaxDiag));
      }
      else break;
    }
    for(i = 1; i < M; i++){
      if(maxLen-i>=K){
        j = 0;
        value += depthCell(i,j,1,1,maxLen-i);
      }
      else break;
    }
  }
  
  //antidiagonal
  if(M>=N){
    for (j = N-2; j >= 0; j--) {
      if(maxLen-(N-j-1)>=K){
        i = 0;
        value += depthCell(i,j,1,-1,maxLen-(N-j-1));
      }
      else break;
    }
    for(i = 0; i < M; i++){
      if(i<nMaxDiag){
        j=N-1;
        value += depthCell(i,j,1,-1,maxLen);
      }
      else if(maxLen-(i+1-nMaxDiag)>=K){
        value += depthCell(i,j,1,-1,maxLen-(i+1-nMaxDiag));
      }
      else break;
    }
  }
  else{
    for (j = N-1; j >=0; j--) {
      if(N-j<nMaxDiag){
        i = 0;
        value += depthCell(i,j,1,-1,maxLen);
      }
      else if(maxLen-(N-j-nMaxDiag)>=K){
        value += depthCell(i,j,1,-1,maxLen-(N-j-nMaxDiag));
      }
      else break;
    }
    for(i = 1; i < M; i++){
      if(maxLen-i>=K){
        j = N-1;
        value += depthCell(i,j,1,-1,maxLen-i);
      }
      else break;
    }
  }
  return value;
}

  private long currentHash;
  private MNKGameState markCell(int i, int j) {
    currentHash ^= zobristTable[i][j][B.currentPlayer()];
    return B.markCell(i, j);
  }
  private void unmarkCell() {
    MNKCell c = B.getMarkedCells()[B.getMarkedCells().length-1];
    currentHash ^= zobristTable[c.i][c.j][B.currentPlayer() == 1 ? 0 : 1];
    B.unmarkCell();
  }

  private boolean isTimeRunningOut() {
    return (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(94.0/100.0);
  }

  private int currentHashIndex() {
    return Math.abs((int) (currentHash % TRANSPOSITION_TABLE_LENGTH));
  }

  // these function take for granted that the search is relative to currentHash
  private void storeTransposition(int searchDepth, int bestMove, int value, int valueKind) {
    int firstHash = (int) (currentHash >> 32), secondHash = (int) currentHash;
    if(transposition[currentHashIndex()] != null)
      tro++;
    else
      tre++;
    
    transposition[currentHashIndex()] = new int[]{
      firstHash, secondHash, searchDepth, bestMove, value, valueKind
    };
  }
  private int bestMove = -1;
  private int retrieveTransposition(int searchDepth, int alpha, int beta) {
    int[] entry = transposition[currentHashIndex()];
    int firstHash = (int) (currentHash >> 32), secondHash = (int) currentHash;
    if(transposition[currentHashIndex()] != null)
    // avoid collisions (verify the hashes match)
    if(entry != null && firstHash == entry[0] && secondHash == entry[1]) {
      if (entry[2] >= searchDepth) {
        if (entry[5] == TRANSPOSITION_KIND_EXACT)
          return entry[4];
        if (entry[5] == TRANSPOSITION_KIND_LOWER && entry[4] <= alpha)
          return alpha;
        if (entry[5] == TRANSPOSITION_KIND_UPPER && entry[4] >= beta)
          return beta;
      }
      bestMove = entry[3];
      trh++;
    } else
      trm++;
    return TRANSPOSITION_ENTRY_NOT_FOUND;
  }

  private int[] getFreeCells() {
    int[] res = new int[B.getFreeCells().length];
    int i = 0;
    boolean doWeHaveTheBestMove = false;
    for(MNKCell c : B.getFreeCells()) {
      res[i++] = c.i*minMN + c.j;
      if(res[i-1] == bestMove)
        doWeHaveTheBestMove = true;
    }

    // take into account any possible bestMove
    if(bestMove != -1 && doWeHaveTheBestMove) {
      int tmp = res[0];
      res[0] = bestMove;
      res[res.length-1] = tmp;
    }

    return res;
  }

//--------------------------------------------------------------------------------

  public int negamax(int searchDepth, int alpha, int beta, int color) {
    int value, localBestMove = -1, valueKind = TRANSPOSITION_KIND_LOWER;
    if((value = retrieveTransposition(searchDepth, alpha, beta)) != TRANSPOSITION_ENTRY_NOT_FOUND)
      return value;
    if(isTimeRunningOut())
      return TIMEOUT_VALUE;
    if (searchDepth == 0 || B.gameState != MNKGameState.OPEN) {
      value = color * evaluate() / B.getMarkedCells().length;
      storeTransposition(searchDepth, bestMove, value, TRANSPOSITION_KIND_EXACT);
      return value;
    }

    for(int c : getFreeCells()) {
      markCell(c/minMN, c%minMN); 
      value = -negamax(searchDepth-1, -beta, -alpha, -color);
      unmarkCell();
      if(value == -TIMEOUT_VALUE)
        // deliberately don't store anything in the transposition table
        return TIMEOUT_VALUE;

      if(value >= beta) {
        storeTransposition(searchDepth, c, beta, TRANSPOSITION_KIND_UPPER);
        return beta;
      }
      if(value > alpha) {
        valueKind = TRANSPOSITION_KIND_EXACT;
        localBestMove = c;
        alpha = value;
      }
    }
    storeTransposition(searchDepth, localBestMove == -1 ? bestMove : localBestMove, alpha, valueKind);
    return alpha;
  }

//--------------------------------------------------------------------------------
// Inizializzazione tabella di zobristTable
// public long random64(){
//   random = new SecureRandom();
//   return random.nextLong();
// }

public void fillZobristHashes(){
  new Thread(() ->{
    for (int i = 0; i < M; i++) {
      for (int j = 0; j < N; j++) {
        zobristTable[i][j][0] = random.nextLong();
        zobristTable[i][j][1] = random.nextLong();
      }
    }
  }).start();
}

  // NOTE: returns null if the time runs out before we can decide a meaningful move
  private MNKCell negamaxRoot(int searchDepth) {
    MNKCell bestCell = null;
    int alpha = MIN, beta = MAX, score;
    for(int c : getFreeCells()) {
      markCell(c/minMN, c%minMN);
      score = -negamax(searchDepth-1, -beta, -alpha, -1);
      unmarkCell();
      if(score > alpha && score != -TIMEOUT_VALUE) {
        alpha = score;
        bestCell = new MNKCell(c/minMN, c%minMN);
      }
    }
    System.out.println(":: depth=" + searchDepth + " best " + alpha + " in [" + alpha + "," + beta + "] -> " + bestCell);
    return bestCell;
  }

  MNKCell isLosingCell(MNKCell[] FC){
		int pos = rand.nextInt(FC.length); 
		MNKCell p = FC[pos]; 
		B.markCell(p.i,p.j); 
		for(int k = 0; k < FC.length; k++) {
      if(k != pos) { 
				MNKCell q = FC[k];
				if(B.markCell(q.i,q.j) == yourWin) {
					B.unmarkCell();
					B.unmarkCell();	       
					B.markCell(q.i,q.j);   
					return q;							 
				} else {
					B.unmarkCell();	       
				}	
			}	
		}
		B.unmarkCell();
    return null;	
  }

  // Fulcro dell'applicativo che esegue le funzioni citate sopra
  public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
    trm = trh = 0;
    start = System.currentTimeMillis();	
    System.out.println("computation started");

    // funzione che va a marcare l'ultima cella inserita dall'avversario
    // scritta dal prof necessaria per il funzionamento
    if(MC.length > 0) {
      MNKCell d = MC[MC.length-1]; 
      markCell(d.i,d.j);         
    }

    // Inizia sempre al centro
    if(MC.length == 0 ){
      B.markCell(M/2, N/2);
      return new MNKCell(M/2, N/2);
    }

    for(MNKCell d : FC) {
			if(B.markCell(d.i,d.j) == myWin) return d; 
			else B.unmarkCell();
		}
    
    MNKCell a = isLosingCell(FC);
    if(a != null){
       return a;
      }

    MNKCell bestCell = null, newCell;
    int searchDepth = 1, maxDepth = B.getFreeCells().length;
    System.out.println("[---------------------------------------------------------]");
    while(!isTimeRunningOut() && searchDepth <= maxDepth) {
      if((newCell = negamaxRoot(searchDepth++)) != null)
        bestCell = newCell;
    }
    lastMarked = bestCell;

    // TODO: remove
    if(bestCell == null) {
      throw new Error("bestCell is null");
    }
    System.out.println("marked " + bestCell);
    System.out.println("transposition table usage: \t" +tre+"\t" + ((float)tre/TRANSPOSITION_TABLE_LENGTH)*100 + "%\ntransposition table overwrite: \t" + tro +"\t" + ((float)tro/(tre+1))*100 + "%\ntransposition table hits: \t"+trh+"\t" + ((float)trh/(trh+trm+1))*100 + "%\ntransposition table overwrite:\t"+tro+"\t" + ((float)trm/(trh+trm+1))*100 + "%");
    System.out.println("[---------------------------------------------------------]");
    markCell(bestCell.i, bestCell.j);
    return bestCell;
  }
  public String playerName() {
    return "Android";
  }



}
