package mnkgame;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

public class Solution implements MNKPlayer {
    private Random rand;
	private MNKBoard B;
	private MNKGameState myWin;
	private MNKGameState yourWin;
	private int TIMEOUT;
    private long start;

    //cose che credo mi serviranno
    int k;
	
	/**
     * Default empty constructor
     */
	public Solution() {}

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis()); 
		B       = new MNKBoard(M,N,K);
		myWin   = first ? MNKGameState.WINP1 : MNKGameState.WINP2; 
		yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
		TIMEOUT = timeout_in_secs;

        k = K-1;
	}

    public double max(double a, double b) {
        return a > b ? a : b;
    }

    public double min(double a, double b) {
        return a < b ? a : b;
    }

    public MNKCell[] removeUslessCell(MNKBoard B) {
        int i, j;
        Set<MNKCell> FC = new HashSet<MNKCell>();
        MNKCell[] MC = B.getMarkedCells();

        for(MNKCell m : MC) {
            i = m.i;
            j = m.j;
            for(int w = 1; w < k+1; w++) {
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

    public MNKCell getRandomUsefullCell(MNKBoard B) {
        int i, j;
        MNKCell[] FC = B.getFreeCells();

        for(MNKCell f : FC) {
            i = f.i;
            j = f.j;
            if (i+1 < B.M)
                if(B.cellState(i+1,j) != MNKCellState.FREE)
                    return f;
            if (i-1 >= 0)
                if(B.cellState(i-1,j) != MNKCellState.FREE)
                    return f;
            if (j+1 < B.N)
                if(B.cellState(i,j+1) != MNKCellState.FREE)
                    return f;
            if (j-1 >= 0)
                if(B.cellState(i,j-1) != MNKCellState.FREE)
                    return f;
            if (i+1 < B.M && j+1 < B.N)
                if(B.cellState(i+1,j+1) != MNKCellState.FREE)
                    return f;
            if (i+1 < B.M && j-1 >= 0)
                if(B.cellState(i+1,j-1) != MNKCellState.FREE)
                    return f;
            if (i-1 >= 0 && j+1 < B.N)
                if(B.cellState(i-1,j+1) != MNKCellState.FREE)
                    return f;
            if (i-1 >= 0 && j-1 >= 0)
                if(B.cellState(i-1,j-1) != MNKCellState.FREE)
                    return f;
        }
        return FC[0];
    }

    public double evaluate(MNKBoard B) {
        //per ora valuto solo lo stato della partita
        MNKGameState state = B.gameState();
        
        if(state == myWin)
            return 1;
        else if(state == MNKGameState.DRAW)
            return 0;
        else if(state == MNKGameState.OPEN) {
            return 0.1;
        } else
            return -1;
        //potrei valutare il numero di mie pedine di seguito e quelle avversarie per valutare meglio
    }

    public double alphabetaPruning(MNKBoard B, boolean myNode, int depth, double alpha, double beta) {
        double eval;
        MNKCell FC [] = B.getFreeCells();
        if (depth == 0 || B.gameState != MNKGameState.OPEN || (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(98.0/100.0)) {
            return evaluate(B);
        } else if(myNode) {
            eval = 10;
            for(MNKCell c : FC) {
                B.markCell(c.i, c.j);
                eval = min(eval, alphabetaPruning(B,false,depth-1,alpha,beta));
                beta = min(eval, beta);
                B.unmarkCell();
                if(beta <= alpha)
                    break;
            }
            return eval;
        } else {
            eval = -10;
            for(MNKCell c : FC) {
                B.markCell(c.i, c.j);
                eval = max(eval, alphabetaPruning(B,true,depth-1,alpha,beta));
                alpha = max(eval, alpha);
                B.unmarkCell();
                if(beta <= alpha)
                    break;
            }
            return eval;
        }
    }

    // crea n livelli di un game tree
    public Albero createGameTree(Albero T, int depth) {

        if (depth == 0) return T;

        MNKBoard B = T.getValue();
        MNKCell[] FC = removeUslessCell(B);

        for (int i = 0; i < FC.length; i++) {
            //dublico la board -> inefficiente a livello di spazio
            MNKBoard tmp = new MNKBoard(B.M,B.N,B.K);
            MNKCell[] MC = B.getMarkedCells();
            for(MNKCell m : MC)
                tmp.markCell(m.i,m.j);
            //inserisco la mossa
            tmp.markCell(FC[i].i,FC[i].j);
            Albero child = T.addChild(tmp);

            //se la partita non e' finita vado avanti nella creazione dell'albero
            if(tmp.gameState() == MNKGameState.OPEN)
                createGameTree(child,depth-1);
        }
        return T;
    }

    public void printTree(Albero T, String ramo) {
        System.out.println(ramo+T);
        MNKCell[] FC = T.getValue().getFreeCells();
        System.out.println(ramo+FC.length);
        if(!T.isLeaf()){
            for(Albero R : T.getChildren())
                printTree(R,ramo+" ");
        }
    }

    public void printArray(MNKCell [] F) {
        System.out.println("Lunghezza: " + F.length);
        for (int i = 0; i < F.length; i++)
            System.out.println(" Cella: " + F[i]);
    }

	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {

        MNKCell selected = getRandomUsefullCell(B);
        start = System.currentTimeMillis();

		if(MC.length > 0) {
			MNKCell c = MC[MC.length-1]; // Recover the last move from MC
			B.markCell(c.i,c.j);         // Save the last move in the local MNKBoard
		}

		// If there is just one possible move, return immediately
		if(FC.length == 1) return FC[0];
		
        // If board is clear, then im first, return right corner of the board
        if(MC.length == 0){
            B.markCell(0,0);
            MC = B.getMarkedCells();
            return MC[0];
        }
        
        MNKCell[] interestingFC = removeUslessCell(B);
        double value, max = -10;
        for(MNKCell A : interestingFC) {
            if ((System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
                //System.out.println("Il tempo e' finito");
                break;
            } else {
                B.markCell(A.i,A.j);
                //System.out.print("Controllo la mossa " + A);
                value = alphabetaPruning(B,true,4,-10,10); //dovrebbe essere false?
                B.unmarkCell();
                //System.out.println("    Valutata: "+ value);
                if (value > max) {
                    max = value;
                    selected = A;
                    //System.out.println("Era meglio della mossa di prima perche' vale: " + tmp);
                }
            }
        }

        B.markCell(selected.i,selected.j);
        //System.out.println("Alla fine ho selezionato " + selected);
        return selected;
	}

	public String playerName() {
		return "monkiflip";
	}
}