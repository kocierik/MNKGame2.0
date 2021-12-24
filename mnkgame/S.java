package mnkgame;

import java.util.HashSet;
import java.util.Random;
import java.security.*;
public class S implements MNKPlayer {
	private static final MNKGameState OPEN = null;
	private Random rand;
	private static MNKGameState myWin;
	private static MNKGameState yourWin;
	private int TIMEOUT;
	private long start;
	private SecureRandom random;
	private long[][][] zobristTable;
	private MNKBoard B;
	private long currentHash;
	private int M,N,K;
	
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
		myWin   = first ? MNKGameState.WINP1 : MNKGameState.WINP2; 
		yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
		TIMEOUT = timeout_in_secs;	
		currentHash = 0;
		zobristTable = new long [M][N][2];
		zobristTable();
	}
//--------------------------------------------------------------------------------

	/* Valutazione dello stato di gioco:
		1 = gioco aperto
		0 = pareggio
		10 = vittoria
		-10 = sconfitta
	*/
	public double evaluate(MNKBoard B) {
		MNKGameState state = B.gameState();
		if(state == MNKGameState.OPEN) return 1;
		else if(state == MNKGameState.DRAW) return 0;
		else if(state == myWin) return 10;
	  else return -10;
}

public int heuristic(){
	throw new Error("Heuristic not implemented yet");
}


private MNKGameState markCell(int i, int j) {
	currentHash ^= zobristTable[i][j][B.currentPlayer()];
	return B.markCell(i, j);
}
private void unmarkCell() {
	MNKCell c = B.getMarkedCells()[B.getMarkedCells().length-1];
	currentHash ^= zobristTable[c.i][c.j][B.currentPlayer() == 1 ? 0 : 1];
	B.unmarkCell();
}

//--------------------------------------------------------------------------------

	// Applicazione dell'alphabetaPruning
public double alphabetaPruning(boolean isMaximizing, int depth, double alpha, double beta) {
	double best;
	MNKCell FC[] = B.getFreeCells();
	if (depth == 0 || B.gameState != MNKGameState.OPEN || (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
			return evaluate(B);
	} else if(isMaximizing) {
			best = 1000;
			for(MNKCell d : FC) {
				B.markCell(d.i, d.j);
				best = Math.min(best, alphabetaPruning(!isMaximizing,depth-1,alpha,beta));
				beta = Math.min(best, beta);
				B.unmarkCell();
				if(alpha >= beta) break;
			}
			return best;
	} else {
			best = -1000;
			for(MNKCell d : FC) {
				B.markCell(d.i, d.j);
				best = Math.max(best, alphabetaPruning(!isMaximizing,depth-1,alpha,beta));
				alpha = Math.max(best, alpha);
				B.unmarkCell();
				if(alpha >= beta) break;
			}
			return best;
	}
}
//--------------------------------------------------------------------------------

	// Prende un valore random
	public long random64(){
		random = new SecureRandom();
		return random.nextLong();
	}

//--------------------------------------------------------------------------------
	// Inizializzazione tabella di zobristTable
	public void zobristTable(){
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				zobristTable[i][j][0] = random64();
				zobristTable[i][j][1] = random64();
			}
		}
	}
//--------------------------------------------------------------------------------

	// Fulcro dell'applicativo che esegue le funzioni citate sopra
	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
		start = System.currentTimeMillis();	

		if(MC.length > 0) {
			MNKCell d = MC[MC.length-1]; 
			B.markCell(d.i,d.j);         
		}

		for(MNKCell d : FC) {
			if(B.markCell(d.i,d.j) == myWin) return d;  
			else B.unmarkCell();
		}

		int i = 1; int j = 0;
		MNKCell bestMoves = null;
		double score = 0;
		double bestScore = -10;

		//----------------------------------------------------------------
		
		// Esegue alphaBetaPruning e tutte le euristiche applicate
		for(MNKCell d : FC) {
			if ((System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
				break;
			} else {	
				B.markCell(d.i, d.j);	
				if(B.M <= 6) score = alphabetaPruning(true,6,-1000,1000);
				else if(B.M <= 10) score = alphabetaPruning(true,4,-1000,1000);
				B.unmarkCell();
				if (score > bestScore){
					bestScore = score;
					bestMoves = d;
				} 
			}
		} 
		B.markCell(bestMoves.i, bestMoves.j);
		return bestMoves;
	}

		public String playerName() {
			return "Android";
		}
}