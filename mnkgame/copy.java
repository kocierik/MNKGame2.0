package mnkgame;

import java.util.HashSet;
import java.util.Random;
import java.security.*;
public class copy implements MNKPlayer {
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
	public copy() {}

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

// analizza solo le celle vicine a quelle nostro o a quelle dell'avversario a distanza K
// Utile per non prendere in considerazione celle troppo lontane
public MNKCell[] getNearFreeCell(MNKBoard B) {
	HashSet<MNKCell> FC = new HashSet<MNKCell>();
	int i, j;
	for(MNKCell d : B.getMarkedCells()){
		i = d.i;
		j = d.j;
		for(int k = 1; k < B.K; k++) {
			if(i+k < B.M && B.cellState(i+k,j) == MNKCellState.FREE) FC.add(new MNKCell(i+k,j));
			if(j+k < B.N && B.cellState(i,j+k) == MNKCellState.FREE) FC.add(new MNKCell(i,j+k));
			if(i-k >= 0 && B.cellState(i-k,j) == MNKCellState.FREE) FC.add(new MNKCell(i-k,j));
			if(j-k >= 0 && B.cellState(i,j-k) == MNKCellState.FREE) FC.add(new MNKCell(i,j-k));
			if(i+k < B.M && j+k < B.N && B.cellState(i+k,j+k) == MNKCellState.FREE) FC.add(new MNKCell(i+k,j+k));
			if(i+k < B.M && j-k >= 0 && B.cellState(i+k,j-k) == MNKCellState.FREE) FC.add(new MNKCell(i+k,j-k));
			if(i-k >= 0 && j+k < B.N && B.cellState(i-k,j+k) == MNKCellState.FREE) FC.add(new MNKCell(i-k,j+k));
			if(i-k >= 0 && j-k >= 0 && B.cellState(i-k,j-k) == MNKCellState.FREE) FC.add(new MNKCell(i-k,j-k));
		}
	}
	MNKCell[] arrayFC = new MNKCell[FC.size()];
	return FC.toArray(arrayFC);
}
//--------------------------------------------------------------------------------


// Permette di analizzare solo le celle libere vicine alle nostre mosse
// In questo modo giocheremo sempre su un certo "lato" di gioco. 
// Evitando così mosse inutili dove non sono presenti mosse nostre o dell'avversario
public MNKCell getBestMoves(MNKBoard B) {
	int i, j;
	HashSet<MNKCell> cellBest = new HashSet<MNKCell>();
	for(MNKCell d : B.getFreeCells()) {
		i = d.i;
		j = d.j;
		if (i+1 < B.M && B.cellState(i+1,j) != MNKCellState.FREE) cellBest.add(d);
		if (j+1 < B.N && B.cellState(i,j+1) != MNKCellState.FREE) cellBest.add(d);
		if (i-1 >= 0 && B.cellState(i-1,j) != MNKCellState.FREE) cellBest.add(d);
		if (i+1 < B.M && j+1 < B.N && B.cellState(i+1,j+1) != MNKCellState.FREE) cellBest.add(d);
		if (i+1 < B.M && j-1 >= 0 && B.cellState(i+1,j-1) != MNKCellState.FREE) cellBest.add(d);
		if (i-1 >= 0 && j+1 < B.N && B.cellState(i-1,j+1) != MNKCellState.FREE) cellBest.add(d);
		if (i-1 >= 0 && j-1 >= 0 && B.cellState(i-1,j-1) != MNKCellState.FREE) cellBest.add(d);
		if (j-1 >= 0 && B.cellState(i,j-1) != MNKCellState.FREE) cellBest.add(d);
	}
	if(cellBest.size() != 0){
		MNKCell[] cells = new MNKCell[cellBest.size()];
		return cellBest.toArray(cells)[rand.nextInt(cells.length)];
	} else { 
		return B.getFreeCells()[0];
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

	// Ritorna un valore hash in output utile per la zobristTable

//--------------------------------------------------------------------------------

	// Fulcro dell'applicativo che esegue le funzioni citate sopra
	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
		start = System.currentTimeMillis();	

		// funzione che va a marcare l'ultima cella inserita dall'avversario
		// scritta dal prof necessaria per il funzionamento
		if(MC.length > 0) {
			MNKCell d = MC[MC.length-1]; 
			B.markCell(d.i,d.j);         
		}
//--------------------------------------------------------------------------------

		// Se c'è una cella che ci permette di vincere la marca
		for(MNKCell d : FC) {
			if(B.markCell(d.i,d.j) == myWin) return d;  
			else B.unmarkCell();
		}
	//--------------------------------------------------------------------------------	
	
	// Se l'avversario può vincere con una singola mossa, il bot
		// marcherà quella cella vincente
		int pos = rand.nextInt(FC.length); 
		MNKCell p = FC[pos]; 
		B.markCell(p.i,p.j); 
		for(int k = 0; k < FC.length; k++) {
			if((System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
				return p;
			} else if(k != pos) { 
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
//--------------------------------------------------------------------------------		


		int i = 1; int j = 0;
		MNKCell bestMoves = getBestMoves(B);
		MNKCell[] goodMoves = getNearFreeCell(B);
		double score = 0;
		double bestScore = -10;


		// Condizione temporanea per tabelle di gioco molto grandi
		if(B.M >= 20) {
			while(i < B.M) {
				MNKCell test;
				if(i>=B.M){ i = 1; j++;}
				if(B.cellState(i, j) == MNKCellState.FREE){ B.markCell(i, j); test = new MNKCell(i, j); return test; }
				i++;
			}
		}
		//----------------------------------------------------------------
		
		// Esegue alphaBetaPruning e tutte le euristiche applicate
		for(MNKCell d : goodMoves) {
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