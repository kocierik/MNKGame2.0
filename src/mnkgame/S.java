package mnkgame;

import java.util.Random;

import java.security.*;
public class S implements MNKPlayer {
	private static MNKGameState myWin;
	private static MNKGameState yourWin;
	private MNKBoard B;
  private int M,N,K, minMN;
  private static int MAX = 500_000_000, MIN = -MAX;
	private int TIMEOUT;
	private int TIMEOUT_VALUE = MAX+1;
	private long start;
	private Random rand;
	private SecureRandom random;
	private long[][][] zobristTable;
  private static int TRANSPOSITION_TABLE_LENGTH = 1024 * 1024 * 4;
  private static int TRANSPOSITION_ENTRY_NOT_FOUND = Integer.MAX_VALUE;
  private static int TRANSPOSITION_KIND_EXACT = 0,
    TRANSPOSITION_KIND_LOWER = -1, TRANSPOSITION_KIND_UPPER = 1;
        
  // transposition entry structure: [
  // first 32 bits of the hash key, 
  // last 32 bits of the hash key, 
  // searchDepth, 
  // bestMove, 
  // value, 
  // value kind (one of TRANSPOSITION_KIND_*)
  // ]
  private int[][] transposition;

	//Default empty constructor
	public S() {}

	//initializing game class
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
		TIMEOUT = timeout_in_secs;	
    currentHash = 0;
    random = new SecureRandom();
		zobristTable = new long [M][N][2];
    fillZobristHashes();
    transposition = new int[TRANSPOSITION_TABLE_LENGTH][];
	}
//--------------------------------------------------------------------------------
//evaluate board game state
  int value;
	public int evaluate() {
		MNKGameState state = B.gameState();
		if(state == MNKGameState.OPEN){
      value =  heuristic();
      return value;
    }
		else if(state == MNKGameState.DRAW) return 0;
		else if(state == myWin) return M*N*1_000;
    else return -M*N*1_000;
  }
//gives values to the series
public int seriesBonus(int n, int consecutive, int marked, Boolean open){
  int res = 0;
  if(n>=K){
    if(consecutive>=K-3){
      if(consecutive >= K-1){
          if(open) {
              res += 30_000_000;
          }
          else{
              res += 7_000_000;
          }
      }
      else if(consecutive >= K-2){
          if(open) {
              res += 3_100_000;
          }
          else{
              res += 1_400_000;
          }
      }
      else if(consecutive >= K-3){
          if(open){
              res += 600_000;
          }
          else{
              res += 250_000;
          }
      }
      //evaluate non consecutive cells density
      res += ((double)(marked-consecutive)/(double)(n-consecutive))*1_000_000;
    }
    else {
      if(marked>=K-3){
        res+=(marked)*50_000;
      }
      res += ((double)marked/(double)n)*100_000;
    }
  }
  return res;
}
//evaluate one row, column, diagonal or antidiagonal
public int depthCell(int i, int j, int dir_i, int dir_j, int maxIter){
  int value = 0;
  int lastPlayer = 0;
  int prev = -1;
  int marked = 0, series = 0, maxSeries = 0;
  int c1series = 0, c2series = 0, lastFreeSeries = 0;
  Boolean semiopenStart = false, maxOpen = false, longest = false;

  for(int z=0;z<maxIter;z++){
    //cell marked from P1
    if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P1){
      if(lastPlayer==2){
        value -= seriesBonus(c2series, maxSeries, marked, maxOpen);
        c2series = 0;
        c1series = 1 + lastFreeSeries;
        if(prev==0) semiopenStart = true;
        else semiopenStart = false;
        series = 1;
        maxSeries = series;
        longest = true;
        marked = 1;
      }
      else{
        if(prev==1){
          series++;
        }
        else{
          if(prev==0) semiopenStart = true;
          else semiopenStart = false;
          series = 1;
          if(lastPlayer==0) //only free cells are before this one
            c1series+= lastFreeSeries;
        }
        if(series>maxSeries) {
          maxSeries = series;
          longest = true;
        }
        else longest = false;
        marked++;
        c1series++;
      }
      lastFreeSeries = 0;
      lastPlayer = 1;
      prev = 1;
      if(z >= maxIter-1){
        value += seriesBonus(c1series, maxSeries, marked, maxOpen);
      }
    }
    //cell marked from P2
    else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P2){
      if(lastPlayer==1){
        value += seriesBonus(c1series, maxSeries, marked, maxOpen);
        c1series = 0;
        c2series = 1 + lastFreeSeries;
        if(prev==0) semiopenStart = true;
        else semiopenStart = false;
        series = 1;
        maxSeries = series;
        longest = true;
        marked = 1;
      }
      else{
        if(prev==2) {
          series++;
        }
        else {
          if(prev==0) semiopenStart = true;
          else semiopenStart = false;
          series = 1;
          if(lastPlayer==0) //only free cells are before this one
            c2series+= lastFreeSeries;
        }
        if(series>maxSeries) {
          maxSeries = series;
          longest = true;
        }
        else longest = false;
        marked++;
        c2series++;
      }
      lastFreeSeries = 0;
      lastPlayer = 2;
      prev = 2;
      if(z >= maxIter-1){
        value -= seriesBonus(c2series, maxSeries, marked, maxOpen);
      }
    }
    //free cell
    else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.FREE){
      if(lastPlayer==1){
        c1series++;
        if(longest){
          maxOpen = semiopenStart;
          longest = false;
        } 
      }
      if(lastPlayer==2){
        c2series++;
        if(longest){
          maxOpen = semiopenStart;
          longest = false;
        }
      }
      if(prev!=1 && prev!=2){
        lastFreeSeries++;
      }
      else lastFreeSeries = 1;

      prev = 0;
      if(z >= maxIter-1){
        if(lastPlayer == 1) value += seriesBonus(c1series, maxSeries, marked, maxOpen);
        else if(lastPlayer == 2) value -= seriesBonus(c2series, maxSeries, marked, maxOpen);
        else value = 0;
      }
    }
  }
  return value;
}
//returns one board's value
public int heuristic() {
    int i = 0, j = 0, value = 0;
    //row
    if(N >= K){
      for (i = 0; i < M; i++) {
        j = 0;
        value += depthCell(i,j,0,1,N);
      }
    }
    //column
    if(M >= K){
      for (j = 0; j < N; j++) {
        i = 0;
        value += depthCell(i,j,1,0,M);
      }
    }
    int maxLen = minMN;
    int nMaxDiag = Math.abs(M-N)+1;
    if(maxLen>=K){
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
            j = 0;
            value += depthCell(i,j,1,1,maxLen);
          }
          else if(maxLen-(i+1-nMaxDiag)>=K){
            j = 0;
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
            i = 0;
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
        for(j = N-2; j >= 0; j--) {
          if(maxLen-(N-j-1)>=K){
            i = 0;
            value += depthCell(i,j,1,-1,maxLen-(N-j-1));
          }
          else break;
        }
        for(i = 0; i < M; i++){
          if(i<nMaxDiag){
            j = N-1;
            value += depthCell(i,j,1,-1,maxLen);
          }
          else if(maxLen-(i+1-nMaxDiag)>=K){
            j = N-1;
            value += depthCell(i,j,1,-1,maxLen-(i+1-nMaxDiag));
          }
          else break;
        }
      }
      else{
        for (j = N-1; j >= 0; j--) {
          if(N-j-1 < nMaxDiag){
            i = 0;
            value += depthCell(i,j,1,-1,maxLen);
          }
          else if(maxLen-(N-j-nMaxDiag)>=K){
            i = 0;
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
    }
    //remove
    if(value<MIN || value>MAX)
      System.out.println("COMPRESO: "+(value<MIN)+" "+(value>MAX));
    if(value>MAX) return MAX-1;
    if(value<MIN) return MIN+1;
    //
    return value;
}

  private long currentHash;
  //compute zobrist hashing marking the cell
  private MNKGameState markCell(int i, int j) {
    currentHash ^= zobristTable[i][j][B.currentPlayer()];
    return B.markCell(i, j);
  }
  //compute zobrist hashing unmarking the cell
  private void unmarkCell() {
    MNKCell c = B.getMarkedCells()[B.getMarkedCells().length-1];
    currentHash ^= zobristTable[c.i][c.j][B.currentPlayer() == 1 ? 0 : 1];
    B.unmarkCell();
  }
  //time limit
  private boolean isTimeRunningOut() {
    return (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(94.0/100.0);
  }
  //returns hashing value index
  private int currentHashIndex() {
    return Math.abs((int) (currentHash % TRANSPOSITION_TABLE_LENGTH));
  }
  //stores transposition taking for granted that the search is relative to currentHash
  private void storeTransposition(int searchDepth, int bestMove, int value, int valueKind) {
    int firstHash = (int) (currentHash >> 32), secondHash = (int) currentHash;
    transposition[currentHashIndex()] = new int[]{
      firstHash, secondHash, searchDepth, bestMove, value, valueKind
    };
  }

  private int bestMove = -1;
  //returns a value if there is a cache hit retrieved from transposition table
  private int retrieveTransposition(int searchDepth, int alpha, int beta) {
    int[] entry = transposition[currentHashIndex()];
    int firstHash = (int) (currentHash >> 32), secondHash = (int) currentHash;
    if(transposition[currentHashIndex()] != null){
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
      }
    }
    return TRANSPOSITION_ENTRY_NOT_FOUND;
  }
  //saves board free cells and check if there is the best move
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
  //negamax implementation
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
  // Initialiaze zobristTable with random numbers
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
  //keeping track of both the best score and its relative cell.
  //NOTE: returns null if the time runs out before we can decide a meaningful move
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
    //remove
    System.out.println(":: depth=" + searchDepth + " best " + alpha + " in [" + alpha + "," + beta + "] -> " + bestCell);
    return bestCell;
  }
  //marked opponent cell if it can win
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
  //engine game
  public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
    start = System.currentTimeMillis();
    if(MC.length > 0) {
      MNKCell d = MC[MC.length-1]; 
      markCell(d.i,d.j);         
    }
    //always start in the middle of the board
    if(MC.length == 0 ){
      B.markCell(M/2, N/2);
      return new MNKCell(M/2, N/2);
    }
    //mark the winning cell
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
    //iterative deepining
    while(!isTimeRunningOut() && searchDepth <= maxDepth) {
      if((newCell = negamaxRoot(searchDepth++)) != null)
        bestCell = newCell;
    }
    markCell(bestCell.i, bestCell.j);
    return bestCell;
  }

  public String playerName() {
    return "Android";
  }
}