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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import mnkgame.ReadingReturnValueTask;
import mnkgame.MNKBoard;

/**
 * Software player only a bit smarter than random.
 * <p> It can detect a single-move win or loss. In all the other cases behaves randomly.
 * </p> 
 */
public class Cop extends Thread implements MNKPlayer  {
	private static final MNKGameState OPEN = null;
	private Random rand;
	private MNKBoard B;
	private static MNKGameState myWin;
	private static MNKGameState yourWin;
	private int TIMEOUT;

	/**
	 * Default empty constructor
	 */
	public Cop() {}


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

	public double evaluate(MNKBoard B, int depth) {
		MNKGameState state = B.gameState();
		if(state == myWin) return 10;
		else if(state == MNKGameState.DRAW) return 0;
		else if(state == yourWin) return -10;
		return 0;
}

public double alphabetaPruning(MNKBoard B, boolean myNode, int depth, double alpha, double beta) {
	double eval;
	MNKCell FC [] = B.getFreeCells();
	if (depth == 0 || B.gameState != MNKGameState.OPEN) {
			return evaluate(B,depth);
	} else if(myNode) {
			eval = 10;
			for(MNKCell c : FC) {
				B.markCell(c.i, c.j);
				eval = Math.min(eval, alphabetaPruning(B,false,depth-1,alpha,beta));
				beta = Math.min(eval, beta);
				B.unmarkCell();
				if(beta <= alpha)
						break;
			}
			return eval;
	} else {
			eval = -10;
			for(MNKCell c : FC) {
					B.markCell(c.i, c.j);
					eval = Math.max(eval, alphabetaPruning(B,true,depth-1,alpha,beta));
					alpha = Math.max(eval, alpha);
					B.unmarkCell();
					if(beta <= alpha)
							break;
			}
			return eval;
	}
}
 

// public void run(){  
// 	System.out.println("thread is running " + Thread.currentThread().getName());  	
// }

	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC){
	System.out.println("-----------------------------------------------");
	if(MC.length > 0) {
		MNKCell c = MC[MC.length-1]; 
		B.markCell(c.i,c.j);         
	}
	if(MC.length == 0){
		B.markCell(1,1);
		MC = B.getMarkedCells();
		return MC[0];
	} 

		ReadingReturnValueTask readTask1 = new ReadingReturnValueTask(B, true,3,-10,10);
		Thread runnableThread1 = new Thread(readTask1);
		runnableThread1.setName("readTask1");
		runnableThread1.start();
		try {
		System.out.println(runnableThread1.getName() + " : have no of lines " + readTask1.numberLines());
	} catch (Exception e) {}

	
	double bestScore = -1000;
	double score;
	MNKCell bestMove = new MNKCell(1,1);
	for(MNKCell d : FC) {
		B.markCell(d.i, d.j);														
		score = alphabetaPruning(B, true,4,-10,10);
		B.unmarkCell();
		if (score > bestScore){
			bestScore = score;
			bestMove = d;
		}
	}
	B.markCell(bestMove.i, bestMove.j);
	System.out.println("SELECTCELL --> riga = " + bestMove.i + ", colonna = " + bestMove.j + "  | MOVEVAL ---> " + bestScore );
	return bestMove;
}

	public String playerName() {
		return "AIBest";
	}

}



