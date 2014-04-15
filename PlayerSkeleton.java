import java.util.ArrayList;
import java.lang.Math;

public class PlayerSkeleton {
	// ALPHA refers to the coefficient for rows cleared feature
	public static  double ALPHA = -10;
	// B refers to the coefficient for number of holes in each row
	public static final double B = 30;
	// A refers to the bonus cost for each existing dependent lines 
	public static double A = 1;
	// Number of states considered when look forward
	public static final int F = 5;
	public static final double MAX= Double.MAX_VALUE;
	//Type of gap width
	public static final int TGW = 2;
	//Type of gap state with one gap width
	public static final int TGS = 11;
	// Cost of the gap [type of gap width][gap ID]
	public static final double[][] GAPCOST = {
	    {1.17, 1.75, 1.40, 1.75, 2.33, 1.75, 3.50, 1.75, 2.33, 14.0, 50.00, 40.0},
	    {1.75, 2.15, 1.75, 2.55, 2.80, 2.15, 2.80, 1.75, 2.15, 6.00, 35.00, 10.0}
	};
    
    public static final double[][] LEARNEDGAPCOST = {
    {1.55, 2.01, 1.34, 2.55, 2.69, 2.01, 4.38, 1.55, 2.57, 15,  0.5, 0.5},
    {1.63, 2.15, 1.31, 1.79, 2.72, 2.74, 2.18, 1.77, 2.09, 5.26,0.5, 0.5}
	};

	// Cost of gap with width larger than 2 
	public static final double gapCostForLongerWidth = 1;
	
	public static int [][][] fullLegalMoves = State.legalMoves;
	
	//Debug use
	int turn = 0;
//	int nextPiece = -1;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {	
		//System.out.println("Current turn is:" + turn);
		turn ++;

        //initialization of variables
		int[][][] topFields = new int[F][State.ROWS][State.COLS];
		int[][] topTops = new int[F][State.COLS];
		int[] topMove = new int[F];
		double[] topCost = new double[F];
		for (int i = 0; i<5; i++) {
			topCost[i] = Integer.MAX_VALUE;
		}
		int[][] oldField = s.getField();
		int[] oldTop = s.getTop();
		
        double minCost = MAX;
        int mincostMove =0;
		double bestMoveCost = Integer.MAX_VALUE;
		for (int i= 0; i< legalMoves.length; i++) {
			
			int[] top = new int[State.COLS];
			int[][] field = new int[State.ROWS][State.COLS];
			
			for (int k = 0; k<State.COLS; k++) 
				top[k] = oldTop[k];
			
			for (int j = 0; j < State.ROWS; j++)
				for (int k = 0; k<State.COLS; k++)
					field[j][k] = oldField[j][k];
			
//			//For debug purpose
//		    nextPiece = s.nextPiece;
            //w(s)
		 	double cost = computeMoveCost(s.nextPiece, legalMoves[i][State.ORIENT], legalMoves[i][State.SLOT], field, top, s.getTurnNumber()+1);
   //         System.out.println("~~~~~~~~~~~~~~~w(s) = " +cost);
			
            //all other terms except w(s)
			cost += computeStateCost(field,top);
//            System.out.println("~~~~~~~~~~~~~~~cost = "+cost+"\n");
            
//            if (cost < minCost) {
//                minCost = cost;
//                mincostMove = i;
//            }
			
//                    return mincostMove;
            
			int k = -1;
			for (int j= 0; j<F; j++)
				if (cost < topCost[j]) {
                    //change "<" to ">"
					if (k == -1  || topCost[j]>topCost[k]) k = j;
				}
			
			if (! (k== -1)) {
                //topCost[k] is the maximum cost in top best 5 that is larger than cost
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
	      //   System.out.println("rows cleared: "+rowsCleared);
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
		//TODO: shall we start from the top most row instead of the entire rows?
		for (int j=State.ROWS-1; j>=0; j--) {
      //      System.out.println("row: "+j);
            double cost1 = B*getNumberOfHoles(field, j);
			costOfEachRow[j] +=  cost1;
     //       System.out.println("cost of holes: "+cost1);
            
            double cost2 =getCostOfGap (field, top, j);
			costOfEachRow[j] += cost2;
	//		 System.out.println("cost of gap: "+cost2);
            
            //Possible bug
            for (int k = 1; k<= dependentRows[j][0]; k++)
				costOfEachRow[j] += costOfEachRow[dependentRows[j][k]] + A;
//            
//            System.out.println("dependent rows: "+(costOfEachRow[j]-cost1-cost2));
//            System.out.println("sum: "+costOfEachRow[j]);
            
			double enhancedCost = Math.sqrt(Math.sqrt(costOfEachRow[j]));
       //      System.out.println("cost: "+ costOfEachRow[j] +", enhancedCost: " + enhancedCost);
            cost += enhancedCost;
		}
        
        //add panelty for deep well and multiple well
//        double costOfWell = 0;
//        for(int i =1;i<3;i++){
//            costOfWell+=getCostOfWell(i,field,top);
//        }
//        
//
//        cost+=costOfWell;

       // cost+=getCostOfWell(1,field,top);
		return cost;
	}
    
    
    public double getCostOfWell(int width, int[][] field, int[] top) {
		double cost = 0, unit = 5;
		for (int i = 0; i<State.COLS-width+1; i++) {
			boolean isWell = true;
			for (int j=i; j<=i+width-1; j++){
				if (top[i] != top[j]) {
					isWell = false;
					break;
				}
			}
			if (isWell) {
				// If its left side exists, it cannot be shorter than the ith column
				if (i-1 >0 && top[i-1]<=top[i]) isWell = false;
				// Same apply for right side
				if (i+width <State.COLS && top[i+width] <= top[i]) isWell = false;
			}
			if (isWell) {
				boolean l = false, r = false;
				// Either its left side is wall,
				// or the left side is 3 units taller than the ith column
				if (i-1 >=0 && top[i-1]-top[i]>=3) l = true;
				// same apply for its right side
				if (i+width < State.COLS && top[i+width]-top[i]>=3) r = true;
				isWell = r||l;
			}
			if (isWell) {
				cost = cost + unit;
			}
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
//	public double getCostOfGap(int[][] field, int[] top, int row) {
//		// TO BE IMPLEMENTED
//	    int gap = 3;
//	    double cost = 0;
//	    for (int c = 0; c < State.COLS; c++){
//    	    //if current cell is not a gap
//	        if (field[row][c] != 0 || top[c] - 1 > row){
//	            gap = 3;
//    	    }else{
//    	        if (c+1 == State.COLS){    //if at the last column
//    	            cost = cost + getGapCostOfCell(field, top, row, c, gap);
//    	        }else if (field[row][c+1] == 0){          //if current cell and its right neighbor is empty, continue checking without increasing the cost.
//    	            gap--;
//    	        }else{     //current is empty and has a occupied cell on its right, increase its cost
//    	            cost = cost + getGapCostOfCell(field, top, row, c, gap);
//    	        }
//    	    }
//    	}
//		return cost;
//	}

	
    public double getCostOfGap(int[][] field, int[] top, int row) {
		// TO BE IMPLEMENTED
	    boolean[] possibleGaps = new boolean[State.COLS];
        int[] gapWidth = new int[State.COLS];
        double cost = 0;
        
        //If it is not empty or something above it, it is not a gap.
	    for (int c = 0; c < State.COLS; c++){
	        if (field[row][c] != 0 || top[c] - 1 > row){
	            possibleGaps[c] = false;
            }else{
                possibleGaps[c] = true;
            }
        }
        
        int width = 0;
       // System.out.print("gap width: ");
        for(int c =State.COLS-1;c>=0;c--){
            if(possibleGaps[c]){
                width+=1;
            }else{
                width=0;
            }
            //if it is the left most col or its left col is not empty. remember its width
            if (c == 0 || field[row][c-1]!=0){
                gapWidth[c] = width;
            }else{
                gapWidth[c] = 0;
            }
            //System.out.print(width+" ,");
        }
      //  System.out.println();
        int col=0;
        while(col<State.COLS){
            //gap with width 1 and 2
            if(gapWidth[col]>0 && gapWidth[col]<3){
                //System.out.println("!"+gapWidth[col]);
                cost+=getGapCostOfCell(field, top, row, col, gapWidth[col]);
                col+=gapWidth[col];
            }else if(gapWidth[col] >= 3){
                cost+=gapCostForLongerWidth;
                col+=gapWidth[col];
            }else{
                col++;
            }
        }
        return cost;
    }
    
    //The function returns the cost of gap at the particular empty cell or two cell
    private double getGapCostOfCell(int[][] field, int[] top, int row, int col, int width){
        int gapType;
        
        int[] difference = new int[4];
        
        for(int increment=-2;increment<2;increment++){
            if(((col+increment)>=0)&&((col+increment+width)<State.COLS)){
                if(increment<0){
                    difference[increment+2]= top[col+increment]-row;
                }
                else{
                    difference[increment+2]= top[col+increment+width]-row;
                }
            }else{
                if (col==0){
                    if (increment == -2){
                        difference[increment+2] = State.ROWS; 
                    }else if(increment == -1){
                        difference[increment+2] = 2;
                    }
                }else if (col == 1){
                    if (increment == -2){
                        difference[increment+2] = State.ROWS; 
                    }
                }else if((col+width)>=State.COLS){
                    if (increment == 1){
                        difference[increment+2] = State.ROWS; 
                    }else if(increment == 0){
                        difference[increment+2] = 2;
                    }
                }else{
                    if (increment == 2){
                        difference[increment+2] = State.ROWS; 
                    }
                }
                
                    
                //how to deal with the wall
                //can be modify corresponds to gap detection
               
            }
        }
        
        gapType = getGapIndex(difference[0],difference[1],difference[2],difference[3]);
        
    //   System.out.println("width: "+ (width-1)+", type: "+gapType);
        //return gapType*LEARNEDGAPCOST[width-1][gapType];
        return LEARNEDGAPCOST[width-1][gapType];
    }
    
//    private int getGapIndex(int diff1, int diff2, int diff3, int diff4){
        
        
//        if(diff2>3 && diff3>3){
//        
//            switch(Math.max(diff2,diff3)){
//                case 4:
//                    return 10;
//                case 5:
//                    return 11;
//                case 6:
//                    return 12;
//                case 7:
//                    return 13;
//                case 8:
//                    return 14;
//                default:
//                    return 15;
//            }
//        
//        
//        }
//        else
//            if (diff2 >=3 && diff3>= 3){
//            return 9;
//        }else if ((diff1==2 && diff2 <=1 && diff3 == 3)||(diff2 ==3 && diff3 ==1 && diff4==2)){
//            return 3;
//        }else if ((diff2 ==1 && diff3 == 3)||(diff2 ==3 && diff3 ==1)){
//            return 8;
//        }else if (diff1==2 && diff2 ==1 && diff3 == 1 && diff4==2){
//                return 7;
//        }  else if ((diff1 <= 1 && diff2 == 1 && diff3==1&&diff4==2)||(diff1 == 2 && diff2 == 1 && diff3==1&&diff4<=1)){
//                return 2;
//        }else if (diff2==1 && diff3== 1){
//            return 0;
//        }else if ((diff2 ==2 && diff3 == 1 && diff4==2)||(diff1==2 && diff2 ==1 && diff3 ==2)){
//            return 5;
//        }else if ((diff2 == 1 && diff3==2)||(diff2 == 2 && diff3==1)){
//            return 1;
//        }else if (diff2 ==2 && diff3 == 2){
//            return 4;
//        }else if ((diff2 ==2 && diff3 == 3)||(diff2 ==3 && diff3 ==2)){
//            return 6;
//        }else {
//           // System.out.println("difference: "+diff1+", "+diff2+", "+diff3+", "+diff4);
//          return 10;
//        }
//	}
    
    


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
           //deep well and gap
       }else if (diff2 > 2 && diff3 > 2){
           return 10;
       }else{
           return 11;
       }
	}
	
	// The function returns the number of holes detected in a specific row
	public int getNumberOfHoles(int[][] field, int row) {
		if(row < 0){
			System.out.println("row num should not be negative");
			return -1;//Exceptions?
		}
		
		if(row >= field.length){
			System.out.println("row num should not exceed the maximum");
			return -1; //Exceptions?
		}
		
		if(row == field.length - 1){
			return 0;//Cause the top row will not have any holes by definition
		}else{
			int[] spacesAtTheRow = field[row];
			
			//Start counting
			int holeNum = 0;
			for(int colNum = 0; colNum < spacesAtTheRow.length; colNum ++){				
				if(isHole(field,row,colNum)){
					holeNum ++;
				}
			}
			
			return holeNum;
		}
	}
	
	
	public int[][] getDependendLinesSet(int[][] field) {
    	// TO BE IMPLEMENTED
    	
    	// result[i] stores the line number of ith row's dependent lines.
    	// result[i][0] store the number of dependent lines of the ith row.
    	
    	//The following are calculating the upper rows' dependent rows first, in order to reduce the duplicate calculations.
    	boolean[][] dependentRows = new boolean[State.ROWS][State.ROWS];
    	for(int row = State.ROWS - 2; row>=0; row--){
    		setDependentRowsOfARow(field,row,dependentRows);
    	}
    	int[][] results = format(dependentRows);
//    	if(nextPiece == 0){
//        	printResults(results);
//        	System.out.println("");
//    	}
    	return results;
    }
	
	private void printDependentRows(boolean dependentRows[][]){
		System.out.print("\n");
		for(boolean[] dependentRow:dependentRows){
			for(int i=0; i<dependentRow.length;i++){
				System.out.print(" " + dependentRow[i]+" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");

	}
	
	private void printResults(int results[][]){
		System.out.print("\nHey, this is the dependence results with turn " + turn + ":\n");
		for(int i=0; i<results.length;i++){
			int[] result = results[i];
			System.out.print("Row " +i + ": ");
			for(int j=1; j<result.length;j++){
				System.out.print(" " + result[j]+" ");
			}
			System.out.print("\n");
		}

		System.out.print("\n");

	}
	
	private int[][] format(boolean dependentRows[][]){
		int[][] results = new int[State.ROWS][State.ROWS];
		int outIndex = 0;
		for(int row =0;row < dependentRows.length; row++){
			int count = countNumberOfDependentRows(dependentRows,row);	
			
			boolean[] dependentRowOfTheRow = dependentRows[row];
			int index = 0;
			int result[] = new int[count + 1];
			result[0] = count;
			for(int r=0;r<dependentRowOfTheRow.length;r++){
				if(dependentRowOfTheRow[r]){
					index ++;
					result[index] = r;
				}
			}
			
			results[outIndex] = result;
			outIndex ++;
		}
		
		return results;
	}
	private int countNumberOfDependentRows(boolean dependentRows[][],int row){
		boolean[] dependentRowsOfTheRow = dependentRows[row];
		
		int count = 0;
		for(boolean isDependentRow:dependentRowsOfTheRow){
			if(isDependentRow){
				count++;
			}
		}
		
		return count;
	}
	private void setDependentRowsOfARow(int field[][],int row,boolean dependentRows[][]){
		//This method will return an array of dependent rows' number of the row we choose
		ArrayList<Integer> holes = getHoles(field,row);
		boolean currentDependentRows[] = new boolean[State.ROWS];
		for(int col: holes){
			boolean[] dependentRowsOfTheHole = getDependentRowsOfAHole(field,row,col,dependentRows);
			currentDependentRows = combineDependentRows(currentDependentRows,dependentRowsOfTheHole);
		}
		
		dependentRows[row] = currentDependentRows;
	}
	
	private boolean[] getDependentRowsOfAHole(int field[][],int row,int col,boolean dependentRows[][]){
		int rowAboveTheHole = getTheRowDirectlyAboveAHole(field,row,col);
		
		//Add the rowAboveTheHole as the dependent row of the row the hole is at
		boolean[] dependentRowsOfTheHole = new boolean[State.ROWS];
		dependentRowsOfTheHole[rowAboveTheHole] = true;
		
		dependentRowsOfTheHole = combineDependentRows(dependentRowsOfTheHole,dependentRows[rowAboveTheHole]);
		
		return dependentRowsOfTheHole;
	}
	
	private ArrayList<Integer> getHoles(int field[][],int row){
		if(row < 0){
			System.out.println("row num should not be negative");
			return null;//Exceptions?
		}
		
		if(row >= field.length){
			System.out.println("row num should not exceed the maximum");
			return null; //Exceptions?
		}
		
		if(row == field.length - 1){
			return null;//Cause the top row will not have any holes by definition
		}else{
			int[] spacesAtTheRow = field[row];
			
			//Start counting
			ArrayList<Integer> holes = new ArrayList<Integer>();
			for(int colNum = 0; colNum < spacesAtTheRow.length; colNum ++){				
				if(isHole(field,row,colNum)){
					holes.add(colNum);
				}
			}
			
			return holes;
		}
	}
	
	private Boolean isHole(int field[][],int row,int col){
		int pieceIndicator = field[row][col];
		
		//Check if the piece is occupied or not
		if(pieceIndicator != 0){
			return false;
		}
		
		for(int r = row; r<State.ROWS; r++){
			int upperPieceIndicator = field[r][col];
			
			//If one of the upper piece is occupied, then we conclude that the piece we are checking is a hole
			if(upperPieceIndicator != 0){
				return true;
			}
		}
		
		return false;
		
	}
	
	private boolean[] combineDependentRows(boolean dependentRow1[], boolean dependentRow2[]){		
		boolean combinedDependentRows[] = new boolean[State.ROWS];
		for(int r = 0; r<State.ROWS; r++){
			combinedDependentRows[r] = dependentRow1[r] || dependentRow2[r];
		}
		return combinedDependentRows;
	}
	
	private int getTheRowDirectlyAboveAHole(int field[][],int row,int col){
		for(int r = row;r<State.ROWS;r++){
			int upperPieceIndicator = field[r][col];
			
			//If one of the upper piece is occupied, then we conclude that the piece we are checking is a hole
			if(upperPieceIndicator != 0){
				return r;
			}
		}
		
		return -1; //Exceptions?
	}
	
	public void playWithVisual(int sleepAmount) {
		State s = new State();
		new TFrame(s);
		while(!s.hasLost()) {
			int t = pickMove(s,s.legalMoves());
			//System.out.println("I choose this step  "+t);
			s.makeMove(t);
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(sleepAmount);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
	public void getAverageLinesCleared (int testNum) {
		int average=0;
		for (int i=0; i<testNum; i++) {
			State s = new State();
			while(!s.hasLost()) {
				int t = pickMove(s,s.legalMoves());
				s.makeMove(t);	
			}
			System.out.println(s.getRowsCleared());
			average += s.getRowsCleared();
		}
        
		System.out.println("For "+testNum +" sessions, your average lines cleared is "+ average/testNum);
		  // return average/testNum;
        //return average/testNum;
	}
	
	public static void main(String[] args) {
		PlayerSkeleton p = new PlayerSkeleton();
		//p.playWithVisual(50);
		p.getAverageLinesCleared(10);
        
//        int max = 0;
//		double maxA = 0, maxAlpha = 0;
//		for (double a=1; a<=10; a++) {
//			A=a;
//			for (double alpha = -5; alpha <=5; alpha++ ) {
//				ALPHA = alpha;
//				int current = p.getAverageLinesCleared(10);
//				if (current>max) {
//					max = current;
//					maxA = a;
//					maxAlpha = alpha;
//				}
//			}
//		}
//		System.out.println("a = " + maxA + "  maxAlpha = "+maxAlpha);
        
	}
	
}
