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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

/**
 * Software player only a bit smarter than random.
 * <p> It can detect a single-move win or loss. In all the other cases behaves randomly.
 * </p> 
 */
public class MNKMinMax implements MNKPlayer {
	private static final MNKGameState OPEN = null;
	private Random rand;
	private MNKBoard B;
	private MNKGameState myWin;
	private MNKGameState yourWin;
	private int TIMEOUT;
	private int score;
	private MNKBoard C;

	/**
	 * Default empty constructor
	 */
	public MNKMinMax() {}


	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis()); 
		B       = new MNKBoard(M,N,K);
		C       = new MNKBoard(M,N,K);
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

	public MNKBoard unmarkAllCell(MNKBoard board, LinkedList<MNKCell> MC){
		int count = MC.size();
		for(int i = 0; i < count; i++){
			board.unmarkCell();
		}
		return board;
	} 

	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
		int bestScore = -1000;
		MNKCell bestMove = new MNKCell(0, 0);
		for(int k = 0; k < B.FC.size(); k++) {
			MNKCell d = FC[k];
			B.markCell(d.i, d.j);
			int score = minMax(B, 0, false);
			B = unmarkAllCell(B, B.MC);
			for (MNKCell cell : MC) {
				B.markCell(cell.i, cell.j);
			}
			if(score > bestScore){
				bestScore = score;
				bestMove = new MNKCell(d.i,d.j,MNKCellState.FREE);
			} 
		}
	return bestMove;
}

	private int minMax(MNKBoard board, int depth, boolean isMaximizing) {
		if(board.gameState == myWin) return 10;
		else if(board.gameState == yourWin) return -10;
		else if(board.FC.isEmpty())return 0;

		if(isMaximizing){
			int bestScore = -1000;
			for(MNKCell d : board.FC){
				board.markCell(d.i, d.j);
				bestScore = Math.max(bestScore, minMax(board, depth+1, false));
				if(board.gameState != MNKGameState.OPEN) return bestScore;
			}
			return bestScore;
		} else{
			int bestScore2 = 1000;
			for(MNKCell d : board.FC){
				board.markCell(d.i, d.j);
				bestScore2 = Math.min(bestScore2, minMax(board, depth+1, true));
				if(board.gameState != MNKGameState.OPEN) return bestScore2;
			}
			return bestScore2;
		}
	}
	
	
	public String playerName() {
		return "AIBest";
	}
}
