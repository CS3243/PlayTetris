
public class PlayerSkeleton {
	// ALPHA refers to the coefficient for rows cleared feature
	public static final double ALPHA = 0.5;
	// B refers to the coefficient for number of holes in each row
	public static final double B = 0.5;
	// A refers to the bonus cost for each existing dependent lines 
	public static final double A = 0.5;
	// Number of states considered when look forward
	public static final int F = 5;
	public static final double MAX= Double.MAX_VALUE;
	
	
	public static int [][][] fullLegalMoves = State.legalMoves;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {	

		int[][][] topFields = new int[F][State.ROWS][State.COLS];
		int[][] topTops = new int[F][State.COLS];
		int[] topMove = new int[F];
		double[] topCost = new double[F];
		for (int i = 0; i<5; i++) {
			topCost[i] = Integer.MAX_VALUE;
		}
		int[][] oldField = s.getField();
		int[] oldTop = s.getTop();
		
		
		double bestMoveCost = Integer.MAX_VALUE;
		for (int i= 0; i< legalMoves.length; i++) {
			
			int[] top = new int[State.COLS];
			int[][] field = new int[State.ROWS][State.COLS];
			
			for (int k = 0; k<State.COLS; k++) 
				top[k] = oldTop[k];
			
			for (int j = 0; j < State.ROWS; j++)
				for (int k = 0; k<State.COLS; k++)
					field[j][k] = oldField[j][k];
						
			double cost = computeMoveCost(s.nextPiece, legalMoves[i][State.ORIENT], legalMoves[i][State.SLOT], field, top, s.getTurnNumber()+1);
			cost += computeStateCost(field);
			
			
			int k = -1;
			for (int j= 0; j<F; j++)
				if (cost < topCost[j]) {
					if (k == -1  || topCost[j]<topCost[k]) k = j;
				}
			
			if (! (k== -1)) {
				//topCost[k] is the maximum cost in top best 5 that is smaller than current cost
				topMove[k] = i;
				topCost[k] = cost;
				topTops[k] = top;
				topFields[k] = field;
			}		
		}
		
		// Look Forward
		double bestAmortizedCost = MAX;
		int bestAmortizedMove = 0;
		
		for (int i = 0; i<F; i++) {
			if (topCost[i] != Integer.MAX_VALUE) {
				// initialize amortized cost
				double amortizedCost = 0;
				
				// Iterate over all possible pieces
				for (int nextPiece = 0; nextPiece <State.N_PIECES; nextPiece++) {
					// Iterate over all possible moves given that piece					
					bestMoveCost = Integer.MAX_VALUE;
					for (int l = 0; l <fullLegalMoves[nextPiece].length; l++ ){
						int[] top = new int[State.COLS];
						int[][] field = new int[State.ROWS][State.COLS];
						
						for (int k = 0; k<State.COLS; k++) 
							top[k] = topTops[i][k];
						
						for (int j = 0; j < State.ROWS; j++)
							for (int k = 0; k<State.COLS; k++)
								field[j][k] = topFields[i][j][k];
						
						// The current turn number is S.turnNumber + 2
						double cost = computeMoveCost(nextPiece, fullLegalMoves[nextPiece][l][State.ORIENT], fullLegalMoves[nextPiece][l][State.SLOT], field, top, s.getTurnNumber()+2);
						cost += computeStateCost(field);
						
						if (cost < bestMoveCost) {
							bestMoveCost = cost;
						}
					}
					
					amortizedCost += bestMoveCost;
				}
				
				if ((amortizedCost/State.N_PIECES + topCost[i]) < bestAmortizedCost) {
					bestAmortizedCost = (amortizedCost/State.N_PIECES + topCost[i]);
					bestAmortizedMove= i;
				}	
			}
		}
		
		return topMove[bestAmortizedMove];
	}
	
	
	public double computeMoveCost(int nextPiece, int orient, int slot, int[][] field, int[]top, int turn) {
		int rowsCleared = makeMove(nextPiece, orient, slot, field, top, turn);
		if (rowsCleared == -1) {
			return Integer.MAX_VALUE;
		} else {
			return rowsCleared * ALPHA;
		}	
	}
	
	
	public double computeStateCost(int field[][]) {
		double[] costOfEachRow = new double[State.ROWS]; 
		int[][] dependentRows = getDependendLinesSet(field);
		double cost = 0;
		
		// Calculate the cost of each row
		for (int j=State.ROWS-1; j>=0; j--) {
			costOfEachRow[j] +=  B*getNumberOfHoles(field, j);
			costOfEachRow[j] += getCostOfGap (field, j);
			for (int k = 1; k<= dependentRows[j][0]; k++)
				costOfEachRow[j] += costOfEachRow[dependentRows[j][k]] + A;
			cost += costOfEachRow[j];
		}
		return cost;
	}
	
	
	// The method returns number of rows cleared. If the game fails, it returns -1.
	// The parameter field is modified;
	public int makeMove(int nextPiece, int orient, int slot, int[][] field, int[] top, int turn) {
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int[][] pHeight = State.getpHeight();
		int[][] pWidth = State.getpWidth();
		
		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		
		//check if game ended
		if(height+pHeight[nextPiece][orient] >= State.ROWS) {
			return -1;
		}

		
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}
		
		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}
		
		int rowsCleared = 0;
		
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < State.COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				//for each column
				for(int c = 0; c < State.COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}
		return rowsCleared;
	}
	
	
	// The function returns the sum of cost of each gap detected in a specific row
	public double getCostOfGap(int[][] field, int row) {
		// TO BE IMPLEMENTED
		return 0;
	}
	
	
	// The function returns the number of holes detected in a specific row
	public int getNumberOfHoles(int[][] field, int row) {
		// TO BE IMPELMENTED
		return 0;
	}
	
	
	int[][] getDependendLinesSet(int[][] field) {
    	// TO BE IMPLEMENTED
    	
    	// result[i] stores the line number of ith row's dependent lines.
    	// result[i][0] store the number of dependent lines of the ith row.
    	int[][] result=new int[State.ROWS][1];
    	
    	return result;
    }
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			int t = p.pickMove(s,s.legalMoves());
			//System.out.println("I choose this step  "+t);
			s.makeMove(t);
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
