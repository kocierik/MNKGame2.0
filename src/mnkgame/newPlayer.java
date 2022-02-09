package mnkgame;
import java.security.SecureRandom;

public class newPlayer implements MNKPlayer {
	private static MNKGameState myWin;
	private static MNKGameState yourWin;
	private MNKBoard B;
  private int M,N,K, minMN;
  private static int MAX = 1_000_000_000, MIN = -MAX;
	private int TIMEOUT;
	private int TIMEOUT_VALUE = MAX+1;
	private long start;
	private SecureRandom random;
	private long[][][] zobristTable;
  // 6 * 4 (int) * TRANSPOSITION_TABLE_LENGTH = 
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
	public newPlayer() {}

	//initializing game class
	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		B      = new MNKBoard(M,N,K);
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
		else if(state == myWin) return MAX-1;
    else return MIN+1;
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
              res += 9_000_000;
            }
        }
        else if(consecutive >= K-2){
            if(open) {
              res += 4_000_000;
            }
            else{
              res += 1_900_000;
            }
        }
        else if(consecutive >= K-3){
            if(open){
              res += 900_000;
            }
            else{
              res += 400_000;
            }
        }
        //evaluate non consecutive cells density
        res += ((double)(marked-consecutive)/(double)(n-consecutive))*100_000;
      }
      else {
        if(marked>=K-3){
          res+=(marked)*50_000;
        }
        //marked cells density
        res += ((double)marked/(double)n)*100_000;
      }
    }
    return res;
  }

  //evaluate one row, column, diagonal or antidiagonal
  public int depthCell(int i, int j, int dir_i, int dir_j, int maxIter){
    int value = 0;
    //last player is 1 or 2
    int lastPlayer = 0;
    // prev is 0, 1 or 2
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
    if(B.getMarkedCells().length > 0) {
      int len = B.getMarkedCells().length-1;
      MNKCell[] markedCell = B.getMarkedCells();
      MNKCell c = markedCell[len];

      int res = 0;
      int rightSide = Math.min(K,N-c.j-1);
      int leftSide = Math.min(K,c.j);
      int belowSide = Math.min(K,M-c.i-1);
      int aboveSide = Math.min(K,c.i);
      int rightUpSide = Math.min(K,Math.min(rightSide,aboveSide));
      int leftUpSide = Math.min(K,Math.min(leftSide,aboveSide));
      int rightDownSide = Math.min(K,Math.min(rightSide,belowSide));
      int leftDownSide = Math.min(K,Math.min(leftSide,belowSide));
      //start cell coordinates
      int i = 0,j = 0;

      //row evaluation
      i = c.i;
      j = c.j - leftSide;
      res += depthCell(i, j, 0, 1, leftSide+rightSide+1);

      //column evaluation
      i = c.i - aboveSide;
      j = c.j;
      res += depthCell(i, j, 1, 0, aboveSide+belowSide+1);

      //diagonal evaluation
      i = c.i - leftUpSide;
      j = c.j - leftUpSide;
      res += depthCell(i, j, 1, 1, leftUpSide+rightDownSide+1);

      //antidiagonal evaluation
      i = c.i - rightUpSide;
      j = c.j + rightUpSide;
      res += depthCell(i, j, 1, -1, rightUpSide+leftDownSide+1);

      //this shouldn't happen, but we check it just in case
      if(res>MAX) return MAX-1; 
      if(res<MIN) return MIN+1;
      return res;
    }
    return 0;
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
    return (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(90.0/100.0);
  }
  
  //returns hashing value index
  private int currentHashIndex() {
    return Math.abs((int) (currentHash % TRANSPOSITION_TABLE_LENGTH));
  }

  // these function take for granted that the search is relative to currentHash
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
    if(transposition[currentHashIndex()] != null)
      //avoid collisions (verify the hashes match)
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
      for (int i = 0; i < M; i++) {
        for (int j = 0; j < N; j++) {
          zobristTable[i][j][0] = random.nextLong();
          zobristTable[i][j][1] = random.nextLong();
        }
      }
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
    return bestCell;
  }
  
  //marked opponent cell if it can win
  MNKCell isLosingCell(MNKCell[] FC, int randCell){
    MNKCell c = FC[randCell]; // random move
		markCell(c.i,c.j); // mark the random position	
		for(int k = 0; k < FC.length; k++) {
      if(k != randCell) {     
				MNKCell d = FC[k];
				if(markCell(d.i,d.j) == yourWin) {
					unmarkCell();        
					unmarkCell();	       
					markCell(d.i,d.j);   
					return d;							 
				} else {
					unmarkCell();	       
				}	
			}
		}
		//No win or loss, return the randomly selected move
    unmarkCell();
    
		return null;
  }

  //start in the middle of the board
  public MNKCell centerCell(MNKCell MC[]){
    if(MC.length == 0 ){
      markCell(M/2, N/2);
      return new MNKCell(M/2, N/2);
    }
    return null;
  }

  //engine game
  public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
    start = System.currentTimeMillis();
    if(MC.length > 0) {
      MNKCell d = MC[MC.length-1]; 
      markCell(d.i,d.j);         
    }
    
    //mark the winning cell
    for(MNKCell d : FC) {
			if(markCell(d.i,d.j) == myWin) return d; 
			else unmarkCell();
		}

    MNKCell bestCell = null, newCell;
    int searchDepth = 1, maxDepth = B.getFreeCells().length;
    //iterative deepining
    while(!isTimeRunningOut() && searchDepth <= maxDepth) {
      if((newCell = negamaxRoot(searchDepth++)) != null)
        bestCell = newCell;
    }
    MNKCell b = centerCell(MC);
    if(b != null) return b;
    if(FC.length>1){
      MNKCell a = isLosingCell(FC,1);
      if(a != null) return a;
      MNKCell c = isLosingCell(FC,0);
      if(c != null) return c;
    }

    markCell(bestCell.i, bestCell.j);
    return bestCell;
  }
  public String playerName() {
    return "newPlayer";
  }
}
