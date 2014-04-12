
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
	//Type of gap width
	public static final int TGW = 2;
	//Type of gap state with one gap width
	public static final int TGS = 11;
	// Cost of the gap [type of gap width][gap ID]
	public static final double[][] GAPCOST = {
	    {1.17, 1.75, 1.40, 1.75, 2.33, 1.75, 3.50, 1.75, 2.33, 14.0, 15.00},
	    {1.75, 2.15, 1.75, 2.55, 2.80, 2.15, 2.80, 1.75, 2.15, 6.00, 15.00}
	};
	// Cost of gap with width larger than 2 
	public static final double gapCostForLongerWidth = 1;
	
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
			cost += computeStateCost(field,top);
			
			
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
						cost += computeStateCost(field,top);
						
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
	
	
	public double computeStateCost(int[][] field, int[] top) {
		double[] costOfEachRow = new double[State.ROWS]; 
		int[][] dependentRows = getDependendLinesSet(field);
		double cost = 0;
		
		// Calculate the cost of each row
		for (int j=State.ROWS-1; j>=0; j--) {
			costOfEachRow[j] +=  B*getNumberOfHoles(field, j);
			costOfEachRow[j] += getCostOfGap (field, top, j);
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
	public double getCostOfGap(int[][] field, int[] top, int row) {
		// TO BE IMPLEMENTED
	    int costy = 2;
	    double cost = 0;
	    for (int c = 0; c < State.COLS; c++){
    	    //if current cell is not a gap
	        if (field[row][c] != 0 || top[c] - 1 > row){
	            costy = 2;
    	    }else{
    	        if (c+1 == State.COLS){    //if at the last column
    	            cost = cost + getGapCostOfCell(field, top, row, c, costy);
    	        }else if (field[row][c+1] == 0){          //if current cell and its right neighbor is empty, continue checking without increasing the cost.
    	            costy--;
    	        }else{     //current is empty and has a occupied cell on its right, increase its cost
    	            cost = cost + getGapCostOfCell(field, top, row, c, costy);    
    	        }
    	    }
    	}
		return cost;
	}
	//The function returns the cost of gap at the particular empty cell or two cell
	private double getGapCostOfCell(int[][] field, int[] top, int row, int col, int costy){
	    int diff1, diff2, diff3, diff4, gapIndex;
	    if(costy == 1){
	        //if costy is 1, gap with width 2 
	        //gap with width 2 cannot start from 0
            assert(col!=0);
            
            //range which need to check col-3, col-2, col+1, col+2
            if (col >= 3 && col < State.COLS - 2){
                diff1 = top[col-3] -1 - row;
                diff2 = top[col-2] -1 - row;
                diff3 = top[col+1] -1 - row;
                diff4 = top[col+2] -1 - row;
                
            }else if(col == 2){
                diff1 = 21;
                diff2 = top[col-2] -1 - row;
                diff3 = top[col+1] -1 - row;
                diff4 = top[col+2] -1 - row;
                
            }else if(col == State.COLS - 2){
                diff1 = top[col-2] -1 - row;
                diff2 = top[col-1] -1 - row;
                diff3 = top[col+1] -1 - row;
                diff4 = 21;
                
            }else if(col == 1){
                diff1 = 21;
                diff2 = 2;
                diff3 = top[col+1] -1 - row;
                diff4 = top[col+2] -1 - row;
            }else{
                diff1 = top[col-3] -1 - row;
                diff2 = top[col-1] -1 - row;
                diff3 = 2;
                diff4 = 21;
            }
            gapIndex = getGapIndex(diff1, diff2, diff3, diff4);
            return GAPCOST[0][gapIndex];
	        //gap with width 1
	    }else if (costy == 2){
            //range which need to check col-2, col-1, col+1, col+2 
            if (col >= 2 && col < State.COLS - 2){
                diff1 = top[col-2] -1 - row;
                diff2 = top[col-1] -1 - row;
                diff3 = top[col+1] -1 - row;
                diff4 = top[col+2] -1 - row;
                
            }else if(col == 1){
                diff1 = 21;
                diff2 = top[col-1] -1 - row;
                diff3 = top[col+1] -1 - row;
                diff4 = top[col+2] -1 - row;
                
            }else if(col == State.COLS - 2){
                diff1 = top[col-2] -1 - row;
                diff2 = top[col-1] -1 - row;
                diff3 = top[col+1] -1 - row;
                diff4 = 21;
                
            }else if(col == 0){
                diff1 = 21;
                diff2 = 2;
                diff3 = top[col+1] -1 - row;
                diff4 = top[col+2] -1 - row;
            }else{
                diff1 = top[col-2] -1 - row;
                diff2 = top[col-1] -1 - row;
                diff3 = 2;
                diff4 = 21;
            }
            gapIndex = getGapIndex(diff1, diff2, diff3, diff4);
            return GAPCOST[0][gapIndex];
	    }else{
            return gapCostForLongerWidth;
	    }
	}
	private int getGapIndex(int diff1, int diff2, int diff3, int diff4){

       if (diff1 == diff2 && diff3 == diff4 && diff2 == diff3 && diff1 == 0){
           return 0;
       }else if ((diff1 == 0 && diff2 == 0 && diff3==1)||(diff4 == 0 && diff3 == 0 && diff2 == 1)){
           return 1;
       }else if ((diff2 == 0 && diff3 == 0)&&(diff1 == 0 || diff4 == 0)){
           return 2;
       }else if ((diff1==0 && diff2 == 0 && diff3 == 2)||(diff4==0 && diff3==0 && diff2==2)){
           return 3;
       }else if (diff2==1 && diff3==1){
           return 4;
       }else if ((diff2==1 && diff3==0 && diff4 > 0)||(diff3==1 && diff2==0 && diff1 > 0)){
           return 5;
       }else if ((diff2==1 && diff3==2)||(diff3==1 && diff2==2)){
           return 6;
       }else if (diff1 > 0 && diff2 == 0 && diff3 == 0 && diff4 > 0){
           return 7;
       }else if ((diff1>0 && diff2 == 0 && diff3 == 2)||(diff4 > 0 && diff3 == 0 && diff2 == 2)){
           return 8;
       }else if (diff2 == 2 && diff3 == 2){
           return 9;
       }else{
           return 10;
       }
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
