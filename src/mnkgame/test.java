package mnkgame;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.security.*;
public class test implements MNKPlayer {
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
	public test() {}

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
	public int evaluate() {
		MNKGameState state = B.gameState();
		if(state == MNKGameState.OPEN) return heuristic();
		else if(state == MNKGameState.DRAW) return 0;
		else if(state == myWin) return M*N*1_000;
    else return -M*N*1_000;
  }
  
// mettiamo in un array le celle libere adiacenti all'ultima cella marcata da noi
public MNKCell[] getNearFreeCell() {
	 MNKCell[] arrayFC = new MNKCell[8];
	int i, j;
  i = lastMarked.i;
  j = lastMarked.j;
  //arrayFC[0] sotto
  for(int depth=1;depth<K;depth++){
    if(i+depth < B.M && B.cellState(i+depth,j) == MNKCellState.FREE){
        arrayFC[0] = new MNKCell(i+depth,j);
        times_score[0]++;
        break;
    }
    if(i+depth< B.M && B.cellState(i+depth,j) != MNKCellState.P2)
      break;
    times_score[0]++;
  }
  // arrayFC[1] destra
  //if(j+1 < B.N && B.cellState(i,j+1) != MNKCellState.P2) arrayFC[1] =new MNKCell(i,j+1);
  for(int depth=1;depth<K;depth++){
    if(j+depth < B.N && B.cellState(i,j+depth) != MNKCellState.FREE){
        arrayFC[1] = new MNKCell(i,j+depth);
        times_score[1]++;
        break;
    }
    if(j+depth < B.N && B.cellState(i,j+depth) != MNKCellState.P2)
      break;
    times_score[1]++;
  }
  // arrayFC[2] sopra
  //if(i-1 >= 0 && B.cellState(i-1,j) != MNKCellState.P2) arrayFC[2] = new MNKCell(i-1,j);
  for(int depth=1;depth<K;depth++){
    if(i-depth >= 0 && B.cellState(i-depth,j) != MNKCellState.FREE){
        arrayFC[2] = new MNKCell(i-depth,j);
        times_score[2]++;
        break;
    }
    if(i-depth >= 0 && B.cellState(i-depth,j) != MNKCellState.P2)
      break;
    times_score[2]++;
  }
  // arrayFC[3] sinistra
  //if(j-1 >= 0 && B.cellState(i,j-1) != MNKCellState.P2 ) arrayFC[3] =new MNKCell(i,j-1);
  for(int depth=1;depth<K;depth++){
    if(j-depth >= 0 && B.cellState(i,j-depth) != MNKCellState.FREE){
        arrayFC[3] = new MNKCell(i,j-depth);
        times_score[3]++;
        break;
    }
    if(j-depth >= 0 && B.cellState(i,j-depth) != MNKCellState.P2)
      break;
    times_score[3]++;
  }
  // arrayFC[4] sotto dx
  //if(i+1 < B.M && j+1 < B.N && B.cellState(i+1,j+1) != MNKCellState.P2) arrayFC[4] = new MNKCell(i+1,j+1);
  for(int depth=1;depth<K;depth++){
    if(i+depth < B.M && j+depth < B.N && B.cellState(i+depth,j+depth) != MNKCellState.FREE){
        arrayFC[4] = new MNKCell(i+depth,j+depth);
        times_score[4]++;
        break;
    }
    if(i+depth < B.M && j+depth < B.N && B.cellState(i+depth,j+depth) != MNKCellState.P2)
      break;
    times_score[4]++;
  }
  //arrayFC[5] sotto sx
  //if(i+1 < B.M && j-1 >= 0 && B.cellState(i+1,j-1) != MNKCellState.P2) arrayFC[5] = new MNKCell(i+1,j-1);
  for(int depth=1;depth<K;depth++){
    if(i+depth < B.M && j-depth >= 0 && B.cellState(i+depth,j-depth) != MNKCellState.FREE){
        arrayFC[5] = new MNKCell(i+depth,j-depth);
        times_score[5]++;
        break;
    }
    if(i+depth < B.M && j-depth >= 0 && B.cellState(i+depth,j-depth) != MNKCellState.P2)
      break;
    times_score[5]++;
  }
  //arrayFC[6] sopra dx
  // if(i-1 >= 0 && j+1 < B.N && B.cellState(i-1,j+1) != MNKCellState.P2) arrayFC[6] = new MNKCell(i-1,j+1);
  for(int depth=1;depth<K;depth++){
    if(i-depth >= 0 && j+depth < B.N && B.cellState(i-depth,j+depth) == MNKCellState.FREE){
        arrayFC[6] = new MNKCell(i-depth,j+depth);
        times_score[6]++;
        break;
    }
    if(i-depth >= 0 && j+depth < B.N && B.cellState(i+depth,j+depth) != MNKCellState.P2)
      break;
    times_score[6]++;
  }
  // //arrayFC[7] sopra sx
  // if(i-1 >= 0 && j-1 >= 0 && B.cellState(i-1,j-1) != MNKCellState.P2) arrayFC[7] = new MNKCell(i-1,j-1);
  for(int depth=1;depth<K;depth++){
    if(i-depth >= 0 && j-depth >= 0 && B.cellState(i-depth,j-depth) == MNKCellState.FREE){
        arrayFC[7] = new MNKCell(i-depth,j-depth);
        times_score[7]++;
        break;
    }
    if(i-depth >= 0 && j-depth >= 0 && B.cellState(i-depth,j-depth) != MNKCellState.P2)
      break;
    times_score[7]++;
  }

	return arrayFC;
}

public int depthCell(int i, int j, int diri, int dirj){
  int res=0;
  for(int z=1;z<K;z++){
    if(B.cellState(i+z*diri,j+z*dirj) != MNKCellState.P2)
      res++;
  }
  return res;
}


public class InnerS {
  private int i;
  private int j;
  private int score;
  public InnerS(int i, int j, int score){
    this.i = i;
    this.j = j;
    this.score = score;
  }
}


public void heuristic() {
  //ciclo che mette in un array tutte le celle da analizzare (ovvero le celle adiacenti alle celle marcate da noi)
  //getNearFreeCell(cella[][])
  HashSet<InnerS> nearFreeCell = new HashSet<InnerS>();
  InnerS v;
  //analizziamo tutte le celle adiacenti a quelle marcate da noi
  //dobbiamo ritornare tra TUTTE le celle adiacenti quella con score maggiore
  for (MNKCell cell : B.getMarkedCells()) {
    if(cell.state == MNKCellState.P1){
      //controlla possibili disposizioni vincenti nelle 4 direzioni
      int down=0,up=0,right=0,left=0,diagUR=0,diagUL=0,diagDR=0,diagDL=0;
      int [] score = new int[8];
      MNKCell[] arrayFC = getNearFreeCell();
      if(arrayFC[0]!=null)
        down = depthCell(arrayFC[0].i,arrayFC[0].j,1,0);
      if(arrayFC[1]!=null)
        right = depthCell(arrayFC[1].i,arrayFC[1].j,0,1);
      if(arrayFC[2]!=null)
        up = depthCell(arrayFC[2].i,arrayFC[2].j,-1,0);
      if(arrayFC[3]!=null)
        left = depthCell(arrayFC[3].i,arrayFC[3].j,0,-1);
      if(arrayFC[4]!=null)
        diagDR = depthCell(arrayFC[4].i,arrayFC[4].j,1,1);
      if(arrayFC[5]!=null)
        diagDL = depthCell(arrayFC[5].i,arrayFC[5].j,1,-1);
      if(arrayFC[6]!=null)
        diagUR = depthCell(arrayFC[6].i,arrayFC[6].j,-1,1);
      if(arrayFC[7]!=null)
        diagUL = depthCell(arrayFC[7].i,arrayFC[7].j,-1,-1);
      
      if(down==K-1) score[0] += 1 ;
      if(up==K-1) score[2] += 1;
      if(up+down>=K-1){
        score[0] += 1;
        score[2] += 1;
      }
      if(right==K-1) score[1] += 1 ;
      if(left==K-1) score[3] += 1;
      if(left+right>=K-1){
        score[1] += 1;
        score[3] += 1;
      }
      if(diagUL==K-1) score[7] += 1;
      if(diagUR==K-1) score[6] += 1;
      if(diagUR+diagUL>=K-1){
        score[6] += 1;
        score[7] += 1;
      }
      if(diagDR==K-1) score[4] += 1 ;
      if(diagDL==K-1) score[5] += 1;
      if(up+diagDR>=K-1){
        score[4] += 1;
        score[5] += 1;
      }
      //scorriamo hashset per verificare che la cella non ci sia gia. Se c'è gia aggiorniamo solo lo score, se no aggiungiamo la cella con il suo score
      for(int z=0;z<8;z++){
        Iterator<InnerS> it;
        it=nearFreeCell.iterator();
        Boolean found = false;
        while(it.hasNext()) {
          v = it.next();
          if(v.i==arrayFC[z].i && v.j==arrayFC[z].j){
            v.score += score[z]*times_score[z];
            found = true;
          }
        }
        if(!found){
          InnerS c = new InnerS(arrayFC[z].i,arrayFC[z].j,score[z]*times_score[z]);
          nearFreeCell.add(c);
        }
      }
    }
  }
  //TODO: scorrere HashSet e ritornare cella con valore piu alto
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
