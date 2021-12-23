/*
 *  Copyright (C) 2021 Pietro Di Lena
 *  
 *  This file is part of the MNKGame v2.0 software developed for the
 *  students of the course "Algoritmi e Strutture di Dati" first 
 *  cycle degree/bachelor in Computer Science, University of Bologna
 *  A.Y. 2020-2021.
 *
 *  MNKGame is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this file.  If not, see <https://www.gnu.org/licenses/>.
 */

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
	public int evaluate() {
		MNKGameState state = B.gameState();
		if(state == MNKGameState.OPEN) return heuristic();
		else if(state == MNKGameState.DRAW) return 0;
		else if(state == myWin) return 10;
    else return -10;
  }
  
  public int heuristic() {
    throw new Error("heuristic not implemented yet");
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

//--------------------------------------------------------------------------------

  private int negaMax(int alpha, int beta, int depth) {
    // System.out.printf("zobrist hash: %d\n", currentHash);
    if(B.gameState() != MNKGameState.OPEN || depth == 0) return evaluate();
    for(MNKCell c : B.getFreeCells())  {
      markCell(c.i,c.j);
      int score = -negaMax(-beta, -alpha, depth - 1);
      unmarkCell();
      // System.out.printf("%d < %d < %d\n", -beta, score, -alpha);
      if(score >= beta)
        return beta;   //  fail hard beta-cutoff
      if(score > alpha)
        alpha = score; // alpha acts like max in MiniMax
    }
    return alpha;
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

// Fulcro dell'applicativo che esegue le funzioni citate sopra
public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
  start = System.currentTimeMillis();	

  // funzione che va a marcare l'ultima cella inserita dall'avversario
  // scritta dal prof necessaria per il funzionamento
  if(MC.length > 0) {
    MNKCell d = MC[MC.length-1]; 
    markCell(d.i,d.j);         
  }

  MNKCell bestCell = null;
  int beta = Integer.MAX_VALUE-1, alpha = Integer.MIN_VALUE+1;
  for(MNKCell c : B.getFreeCells()) {
    markCell(c.i, c.j);
    // turno dell'avversario -> chiamarlo con -beta -alpha
    int score = -negaMax(-beta, -alpha, 100);
    unmarkCell();
    // if(score >= beta) {
    //   bestCell = c;
    //   break;
    // }
    if(score > alpha) {
      bestCell = c;
      alpha = score;
    }
  }
  // TODO: remove
  if(bestCell == null) {
    throw new Error("bestCell is null");
  }
  System.out.println("found best cell with value (" + alpha + "," + beta + ") at " + bestCell);
  markCell(bestCell.i, bestCell.j);
  return bestCell;
}
public String playerName() {
  return "Android";
}
}
