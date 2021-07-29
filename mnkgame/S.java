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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
	private int count;
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
		count = 0;
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
		
		if(state == myWin)
				return 1;
		else if(state == MNKGameState.DRAW)
				return 0;
		else if(state == MNKGameState.OPEN) {
				return 0.1;
		} else
				return -1;
}

public double alphabetaPruning(MNKBoard B, boolean isMaximizing, int depth, double alpha, double beta) {
	double best;
	MNKCell FC[] = B.getFreeCells();
	if (depth == 0 || B.gameState != MNKGameState.OPEN || (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
			return evaluate(B);
	} else if(isMaximizing) {
			best = 10;
			for(MNKCell d : FC) {
					B.markCell(d.i, d.j);
					best = Math.min(best, alphabetaPruning(B,false,depth-1,alpha,beta));
					beta = Math.min(best, beta);
					B.unmarkCell();
					if(alpha >= beta) break;
			}
			return best;
	} else {
			best = -10;
			for(MNKCell d : FC) {
					B.markCell(d.i, d.j);
					best = Math.max(best, alphabetaPruning(B,true,depth-1,alpha,beta));
					alpha = Math.max(best, alpha);
					B.unmarkCell();
					if(alpha >= beta) break;
			}
			return best;
	}
}

public MNKCell[] removeBadMoves(MNKBoard B) {
	Set<MNKCell> FC = new HashSet<MNKCell>();
	MNKCell[] MC = B.getMarkedCells();
	int i, j;

	for(MNKCell d : MC){
			i = d.i;
			j = d.j;
			for(int w = 1; w < B.K; w++) {
					if(i-w >= 0)
							if(B.cellState(i-w,j) == MNKCellState.FREE)
									FC.add(new MNKCell(i-w,j));
					if(j-w >= 0)
							if(B.cellState(i,j-w) == MNKCellState.FREE)
									FC.add(new MNKCell(i,j-w));
					if(i+w < B.M)
							if(B.cellState(i+w,j) == MNKCellState.FREE)
									FC.add(new MNKCell(i+w,j));
					if(j+w < B.N)
							if(B.cellState(i,j+w) == MNKCellState.FREE)
									FC.add(new MNKCell(i,j+w));
					if(i+w < B.M && j+w < B.N)
							if(B.cellState(i+w,j+w) == MNKCellState.FREE)
									FC.add(new MNKCell(i+w,j+w));
					if(i+w < B.M && j-w >= 0)
							if(B.cellState(i+w,j-w) == MNKCellState.FREE)
									FC.add(new MNKCell(i+w,j-w));
					if(i-w >= 0 && j+w < B.N)
							if(B.cellState(i-w,j+w) == MNKCellState.FREE)
									FC.add(new MNKCell(i-w,j+w));
					if(i-w >= 0 && j-w >= 0)
							if(B.cellState(i-w,j-w) == MNKCellState.FREE)
									FC.add(new MNKCell(i-w,j-w));
			}
	}
	MNKCell[] tmpFC = new MNKCell[FC.size()];
	return FC.toArray(tmpFC);
}

public MNKCell getBestMoves(MNKBoard B) {
	int i, j;
	MNKCell[] FC = B.getFreeCells();

	for(MNKCell d : FC) {
			i = d.i;
			j = d.j;
			if (i+1 < B.M)
					if(B.cellState(i+1,j) != MNKCellState.FREE)
							return d;
			if (i-1 >= 0)
					if(B.cellState(i-1,j) != MNKCellState.FREE)
							return d;
			if (j+1 < B.N)
					if(B.cellState(i,j+1) != MNKCellState.FREE)
							return d;
			if (j-1 >= 0)
					if(B.cellState(i,j-1) != MNKCellState.FREE)
							return d;
			if (i+1 < B.M && j+1 < B.N)
					if(B.cellState(i+1,j+1) != MNKCellState.FREE)
							return d;
			if (i+1 < B.M && j-1 >= 0)
					if(B.cellState(i+1,j-1) != MNKCellState.FREE)
							return d;
			if (i-1 >= 0 && j+1 < B.N)
					if(B.cellState(i-1,j+1) != MNKCellState.FREE)
							return d;
			if (i-1 >= 0 && j-1 >= 0)
					if(B.cellState(i-1,j-1) != MNKCellState.FREE)
							return d;
	}
	return FC[0];
}

	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
		start = System.currentTimeMillis();
		MNKCell bestMoves = getBestMoves(B);

		if(MC.length > 0) {
			MNKCell d = MC[MC.length-1]; 
			B.markCell(d.i,d.j);         
		}

		if(MC.length == 0){
			B.markCell(B.M/2,B.N/2);
			MC = B.getMarkedCells();
			return MC[0];
		} 


		for(MNKCell d : FC) {
			if(B.markCell(d.i,d.j) == myWin) {
				return d;  
			} else {
				B.unmarkCell();
			}
		}

		if(MC.length == 1){
			if(MNKCellState.FREE == B.cellState(B.M/2,B.N/2)){
				MNKCell d = new MNKCell(B.M/2,B.N/2);
				B.markCell(B.M/2,B.N/2);
				return d;
			} 
			else {
					MNKCell d = new MNKCell(B.M/2-1,B.N/2-1);
					B.markCell(B.M/2-1,B.N/2-1);
					return d;
				}
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

		MNKCell[] goodMoves = removeBadMoves(B);
		double score;
		double bestScore = -10;
		for(MNKCell d : goodMoves) {
			if ((System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
				break;
			} else {
				B.markCell(d.i, d.j);		
				if(B.M <= 6) score = alphabetaPruning(B, true,7,-10,10);
				// else if(B.M < 7) score = alphabetaPruning(B, true,5,-10,10);
				else if(B.M <= 10) score = alphabetaPruning(B, true,4,-10,10);
				else score = alphabetaPruning(B, true,1,-10,10);
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
			return "AIBest";
		}
}