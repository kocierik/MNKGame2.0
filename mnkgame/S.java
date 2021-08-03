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
import mnkgame.MNKBoard;

/**
 * Software player only a bit smarter than random.
 * <p> It can detect a single-move win or loss. In all the other cases behaves randomly.
 * </p> 
 */
public class S implements MNKPlayer {
	private static final MNKGameState OPEN = null;
	private Random rand;
	private MNKBoard B;
	private static MNKGameState myWin;
	private static MNKGameState yourWin;
	private int TIMEOUT;
	private long start;
	/**
	 * Default empty constructor
	 */
	public S() {}


	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis()); 
		B       = new MNKBoard(M,N,K);
		myWin   = first ? MNKGameState.WINP1 : MNKGameState.WINP2; 
		yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
		TIMEOUT = timeout_in_secs;	
	}

	/**
	 * Selects a position among those listed in the <code>FC</code> array.
   * <p>
   * Selects a winning cell (if any) from <code>FC</code>, otherwise
   * selects a cell (if any) that prevents the adversary to win 
   * with his next move. If both previous cases do not apply, selects
   * a random cell in <code>FC</code>.
	 * </p>
   */

	public double evaluate(MNKBoard B) {
		MNKGameState state = B.gameState();
		if(state == MNKGameState.OPEN) return 1;
		else if(state == MNKGameState.DRAW) return 0;
		else if(state == myWin) return 10;
	  else return -10;
}

public double alphabetaPruning(MNKBoard B, boolean isMaximizing, int depth, double alpha, double beta) {
	double best;
	MNKCell FC[] = B.getFreeCells();
	if (depth == 0 || B.gameState != MNKGameState.OPEN || (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
			return evaluate(B);
	} else if(isMaximizing) {
			best = 1000;
			for(MNKCell d : FC) {
				B.markCell(d.i, d.j);
				best = Math.min(best, alphabetaPruning(B,!isMaximizing,depth-1,alpha,beta));
				beta = Math.min(best, beta);
				B.unmarkCell();
				if(alpha >= beta) break;
			}
			return best;
	} else {
			best = -1000;
			for(MNKCell d : FC) {
				B.markCell(d.i, d.j);
				best = Math.max(best, alphabetaPruning(B,!isMaximizing,depth-1,alpha,beta));
				alpha = Math.max(best, alpha);
				B.unmarkCell();
				if(alpha >= beta) break;
			}
			return best;
	}
}

public MNKCell[] removeBadMoves(MNKBoard B) {
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

	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
		start = System.currentTimeMillis();
		
		if(MC.length > 0) {
			MNKCell d = MC[MC.length-1]; 
			B.markCell(d.i,d.j);         
		} 	
		if(B.M == 6 && ((B.N == 6 && B.K == 4) || (B.N == 5 && B.K == 4))) if(B.MC.size() == 0) { B.markCell(0, 0); return new MNKCell(0, 0); }
		if((B.M == 7 && B.N == 4 && B.K == 4) || (B.M == 7 && B.N == 5 && B.K == 4) || (B.M == 7 && B.N == 7 && B.K == 5)) {
			if(B.cellState(B.M/2, B.M/2) == MNKCellState.FREE) { B.markCell(B.M/2, B.M/2); return new MNKCell(B.M/2, B.M/2); }
		}
		if(B.M == 6 && (B.N != 4 && B.K != 4)) {
			if(B.cellState(B.M/2, B.M/2) == MNKCellState.FREE) { B.markCell(B.M/2, B.M/2); return new MNKCell(B.M/2, B.M/2); }
		}
		for(MNKCell d : FC) {
			if(B.markCell(d.i,d.j) == myWin) return d;  
			else B.unmarkCell();
		}
		
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
		
		int i = 1; int j = 0;
		MNKCell bestMoves = getBestMoves(B);
		MNKCell[] goodMoves = removeBadMoves(B);
		double score = 0;
		double bestScore = -10;

		if(B.M >= 20) {
			while(i < B.M) {
				MNKCell test;
				if(i>=B.M){ i = 1; j++;}
				if(B.cellState(i, j) == MNKCellState.FREE){ B.markCell(i, j); test = new MNKCell(i, j); return test; }
				i++;
			}
		}

		for(MNKCell d : goodMoves) {
			if ((System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
				break;
			} else {	
				B.markCell(d.i, d.j);		
				if(B.M <= 6) score = alphabetaPruning(B, true,6,-1000,1000);
				else if(B.M <= 10) score = alphabetaPruning(B, true,4,-1000,1000);
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