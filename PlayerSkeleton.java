
public class PlayerSkeleton {
	// ALPHA refers to the coefficient for rows cleared feature
	public static final double ALPHA = 0.5;
	// B refers to the coefficient for number of holes in each row
	public static final double B = 0.5;
	// A refers to the bonus cost for each existing dependent lines 
	public static final double A = 0.5;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		
		//for S.ROWS
		
		int bestMove = 0;
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
			
			int rowsCleared = makeMove(s.nextPiece, legalMoves[i][State.ORIENT], legalMoves[i][State.SLOT], s, field, top);
						
			// cost refers to the overall cost if we make the current move
			double cost = 0;
			
			if (rowsCleared == -1) {
				cost = Integer.MAX_VALUE;
			} else {
				cost = rowsCleared * ALPHA;
			}
			
			double[] costOfEachRow = new double[State.ROWS]; 
			int[][] dependentRows = getDependendLinesSet(field);
			
			// Calculate the cost of each row
			for (int j=State.ROWS-1; j>=0; j--) {
				costOfEachRow[j] +=  B*getNumberOfHoles(field, i);
				costOfEachRow[j] += getCostOfGap (field, i);
				
				for (int k = 1; k<= dependentRows[j][0]; k++)
					costOfEachRow[j] += costOfEachRow[dependentRows[j][k]] + A;
				cost += costOfEachRow[j];
				if (cost > bestMoveCost) break;
			}
			
			if (cost < bestMoveCost) {
				bestMoveCost = cost;
				bestMove = i;
			}
			
		}
				
		return bestMove;
	}
	
	
	// The method returns number of rows cleared. If the game fails, it returns -1.
	// The parameter field is modified;
	public int makeMove(int nextPiece, int orient, int slot, State s1, int[][] field, int[] top) {
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int[][] pHeight = State.getpHeight();
		int[][] pWidth = State.getpWidth();
		int turn = s1.getTurnNumber()+1;
		
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
