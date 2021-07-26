package mnkgame;
import java.sql.Time;

public class ReadingReturnValueTask implements Runnable{
  private double eval;
  private volatile boolean finish;
  private MNKBoard B;
  boolean myNode;
  int depth;
  double alpha;
  double beta;
  public ReadingReturnValueTask(MNKBoard B, boolean myNode, int depth, double alpha, double beta) {
    this.B = B;
    this.myNode = myNode;
    this.depth = depth;
    this.alpha = alpha;
    this.beta = beta;
  }

	public double evaluate(MNKBoard B, int depth) {
		MNKGameState state = B.gameState();
		
		if(state == MNKGameState.WINP1)
				return 10;
		else if(state ==  MNKGameState.WINP2)
				return 0;
		else if(state == MNKGameState.OPEN) {
				return 0.1;
		} else
				return -10;
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

  public void run(){
    MNKCell FC [] = B.getFreeCells();
    if (depth == 0 || B.gameState != MNKGameState.OPEN) {
        eval = evaluate(B,depth);
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
        finish = true;
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
        finish = true;
    } 

    synchronized(this){
      this.notify();
    }
  }
  
  public double numberLines() throws InterruptedException{
    synchronized(this){
      if(!finish)
        this.wait();
    }
    return eval;
  }

}