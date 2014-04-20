import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.Math;


public class PlayerSkeleton {
	public boolean toFile;
	public PrintWriter writer;
	public String fileName;
	public static double LANDINGHEIGHT = 4.500158825082766;
	public static double ROWSCLEARED = -3.4181268101392694;
	public static double ROWTRANSITIONS = 3.2178882868487753;
	public static double COLTRANSITIONS = 9.348695305445199;
	public static double HOLES = 7.899265427351652;
	public static double WELLS = 3.3855972247263626;
	
	public double[][] lowerUpperBound = new double[][] {{-5, 5}, {-5, 10}, {-5, 10},
    {-5, 10}, {-5, 10}, {-5, 10}};
	
	
	// ALPHA refers to the coefficient for rows cleared feature
	public static  double ALPHA = -1.6;
	// B refers to the coefficient for number of holes in each row
	public static  double B = 2.31;
	// A refers to the bonus cost for each existing dependent lines
	public static double A = 1;
	// C refers to the coefficient for blockage
	public static double C = 0.59;
	// D refers to the height multiplier
	public static double D = 3.78;
	// W refers to the penalty coefficient for well
	public static double W = 0.1;
	// AD refers to the coefficient multipied to each dependent line costs
	public static double AD = 0.0001;
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
        int minCostMove =0, maxHeight = 0;
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
			
            //all other terms except w(s)
			cost += computeStateCost(field,top, turn);
            
			int highestRow = 0;
			for (int j = 0; j<State.COLS; j++) {
				if (top[j] > highestRow) highestRow = top[j];
			}
			
            if (cost < minCost) {
                minCost = cost;
                minCostMove = i;
                maxHeight = highestRow;
            }
			
            
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
		
		if (maxHeight >= State.ROWS-5) 	{
			minCostMove = getLookForwardResult(topMove, topCost, topTops, topFields, s.getTurnNumber()+2);
		}
		return minCostMove;
        
	}
	
	
	public int getLookForwardResult(int[] topMove, double[] topCost, int[][] topTops, int[][][] topFields, int turnNumber) {
		// Look Forward
		double bestAmortizedCost = MAX, bestMoveCost;
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
						double cost = computeMoveCost(nextPiece, fullLegalMoves[nextPiece][l][State.ORIENT], fullLegalMoves[nextPiece][l][State.SLOT], field, top, turnNumber);
						cost += computeStateCost(field,top, turn);
						
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
			return rowsCleared * ROWSCLEARED;
		}
	}
	
	
	public double computeStateCost(int[][] field, int[] top, int turn) {
		double[] costOfEachRow = new double[State.ROWS];
		double cost = 0;
		int highestRow = 0;
		
		int pieceMaxY = 0;
		int pieceMinY = State.ROWS;
		
		for (int r = 0; r < State.ROWS; r++) {
			for (int c = 0; c < State.COLS; c++) {
				if (field[r][c] == turn) {
					pieceMaxY = Math.max(pieceMaxY, r);
					pieceMinY = Math.min(pieceMinY, r);
				}
			}
		}
		
		// Landing Height (verticdal midpoint)
        double height = 0.0;
        height = 0.5 * (double)( pieceMinY + pieceMaxY );
		cost += LANDINGHEIGHT * height;
        
        
		// Calculate the cost of each row
		for (int j = 0; j<State.COLS; j++) {
			if (top[j] > highestRow) highestRow = top[j];
			cost += COLTRANSITIONS * getTransitionCountForColumn(field, j, top[j]);
			cost += WELLS * getAllWellsForColumn(field, j);
		}
		
		//highestRow--;
		
		for (int j=highestRow; j>=0; j--) {
            double cost1 = HOLES * getNumberOfHoles(field, j);
			costOfEachRow[j] +=  cost1;
            //          System.out.println("cost of holes: "+cost1);
            
            //            double cost2 =getCostOfGap (field, top, j);
            //			costOfEachRow[j] += cost2;
            //	 System.out.println("cost of gap: "+cost2);
            
            //            for (int k = 1; k<= dependentRows[j][0]; k++){
            //				costOfEachRow[j] += 0.8*costOfEachRow[dependentRows[j][k]] + A;
            //            }
            //            System.out.println("dependent rows: "+(costOfEachRow[j]-cost1-cost2));
            //            System.out.println("sum: "+costOfEachRow[j]);
			//double enhancedCost = Math.sqrt(Math.sqrt(costOfEachRow[j]));
            //			System.out.println("Enhanced Cost is :"+enhancedCost);
			//costOfEachRow [j] = enhancedCost;
            //       //      System.out.println("cost: "+ costOfEachRow[j] +", enhancedCost: " + enhancedCost);
            //cost += enhancedCost;
			double cost2 = ROWTRANSITIONS * getTransitionCountForRow(field, j);
			costOfEachRow[j] += cost2;
			cost += costOfEachRow[j];
		}
		
        //		System.out.println("Blockage number is" + getBlockageNum(field));
		//cost += Math.sqrt(Math.sqrt(getBlockageNum(field)));
        //		for (int j = highestRow; j>=0; j--) {
        //			double dependentLinesCost = 0;
        //			for (int k = 1; k<= dependentRows[j][0]; k++)
        //				dependentLinesCost += costOfEachRow[dependentRows[j][k]];
        //			double enhancedCost = Math.sqrt(Math.sqrt(costOfEachRow[j] + 0.8*Math.sqrt(dependentLinesCost)));
        //		           System.out.println("cost: "+ costOfEachRow[j] +", dependent Cost: " + Math.sqrt(dependentLinesCost));
        //			cost += enhancedCost;
        //		}
        //
        //add panelty for deep well and multiple well
        //cost of wells
		
        //		System.out.println("temp cost is " + cost);
		//double costOfWell= 0;
        
		//costOfWell += getCostOfWellTop(1, field, top);
        //		System.out.println("cost of well is " + costOfWell);
        //cost+= costOfWell;
        //
        //        double diff = 0;
        //        for (int j= 0; j< State.COLS-1; j++) {
        //        	diff += Math.abs(top[j+1]-top[j]);
        //        }
        //        cost += diff*0.2;
        
		return cost;
	}
    
    public double getTransitionCountForColumn(int[][] field, int j, int top) {
    	int transitionCount = 0;
		int cellA, cellB;
        
        // check cell and neighbor above...
        for (int r = 0; r < top; r++) {
            cellA = field[r][j];
            cellB = field[r+1][j];
            
            // If a transition from occupied to unoccupied, or
            // from unoccupied to occupied, then it's a transition.
            if ((cellA != 0 && cellB == 0) ||
                (cellA == 0 && cellB != 0)) {
            	transitionCount++;
            }
        }
        
        // check transition between bottom-exterior and row Y=1.
        // (Note: Bottom exterior is implicitly "occupied".)
        cellA = field[0][j];
        if (cellA == 0) {
            transitionCount++;
        }
        
        // check transition between column 'mHeight' and above-exterior.
        // (Note: Sky above is implicitly UN-"occupied".)
        cellA = field[State.ROWS - 1][j];
        if (cellA == 0) {
            transitionCount++;
        }
        
        return transitionCount;
    }
    
    public int getAllWellsForColumn(int[][]field, int c) { // result range: 0..O(Height*mHeight)
        int wellValue = 0;
        int cellLeft, cellRight;
        
        for (int r = State.ROWS-1; r >= 0; r-- ) {
            if ((c - 1) >= 0) {
                cellLeft = field[r][c-1];
            }
            else {
                cellLeft = 1; // Non-empty
            }
            
            if ((c + 1) <= State.COLS-1) {
                cellRight = field[r][c+1];
            } else {
                cellRight = 1; //Non-empty
            }
            
            if (cellLeft != 0 && cellRight != 0) {
                int blanksDown = 0;
                blanksDown = this.getBlanksDownBeforeBlockedForColumn(field, c, r);
                wellValue += blanksDown;
            }
        }
        
        return wellValue;
    }
    
    public int getBlanksDownBeforeBlockedForColumn(int[][]field, int c, int topRow) { // result range: 0..topY
        int totalBlanksBeforeBlocked = 0;
        int cellValue;
        
        for (int r = topRow; r >= 0; r-- ) {
            cellValue = field[r][c];
            
            if (cellValue != 0) {
                return totalBlanksBeforeBlocked;
            } else {
                totalBlanksBeforeBlocked++;
            }
        }
        
        return totalBlanksBeforeBlocked;
    }
    
    public double getTransitionCountForRow(int[][] field, int j) {
    	int transitionCount = 0;
		int cellA, cellB;
        
        // check cell and neighbor to right...
        for (int c = 0; c < State.COLS - 1; c++ ) {
            cellA = field[j][c];
            cellB = field[j][c+1];
            
            // If a transition from occupied to unoccupied, or
            // from unoccupied to occupied, then it's a transition.
            if ((cellA != 0 && cellB == 0) ||
            	(cellA == 0 && cellB != 0)) {
                transitionCount++;
            }
        }
        
        // check transition between left-exterior and column 1.
        // (Note: Exterior is implicitly "occupied".)
        cellA = field[j][0];
        if (cellA == 0) {
            transitionCount++;
        }
        
        // check transition between column 'mWidth' and right-exterior.
        // (NOTE: Exterior is implicitly "occupied".)
        cellA = field[j][State.COLS-1];
        if (cellA == 0) {
            transitionCount++;
        }
        
        return transitionCount;
    }
	
    public double getCostOfWellTop(int width, int[][] field, int[] top) {
		double cost = 0;
		int topBottom = 0;
		for (int i = 0; i<State.COLS-width+1; i++) {
			topBottom = 0;
			for (int j=i; j<=i+width-1; j++){
				topBottom = max(topBottom, top[j]);
			}
            
			if (topBottom >=0) {
				int leftTop, rightTop;
				if (i-1 <0) leftTop = State.ROWS+1; else leftTop = top[i-1];
				if (i+width >= State.COLS) rightTop = State.ROWS+1; else rightTop = top[i+width];
				int minSideTop = min(leftTop, rightTop);
				if (minSideTop - topBottom >=3) {
					cost += ((minSideTop -topBottom) *0.88);
				}
				//System.out.println( width + "  " + i+" "+leftTop + "  "+ rightTop + "  " +topBottom +" detected!!!");
			}
		}
		return cost;
	}
	public double getCostOfWell(int[][] field, int[] top){
	    double costOfWell = 0;
	    
	    for(int c=0; c < State.COLS; c++){
	        //check width under top[c]
	    	int heightOfWell = 0;
            for(int r=0; r<top[c]-1; r++){
	            if (field[r][c] == 0){
	            	heightOfWell++;
	                if ((heightOfWell >= 3)&&(field[r+1][c]!=0)){
	                	int numRowsAbove = top[c]-r;
	                	costOfWell = costOfWell + heightOfWell;// + numRowsAbove*1.5 + (r-heightOfWell)*0.5;
	                	//System.out.println(costOfWell);
	                    //costOfWell = costOfWell + heightOfWell*(r-heightOfWell)*0.1;
	                }
	            }else{
	                heightOfWell = 0;
	            }
	        }
	    }
	    return costOfWell;
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
	
	//condition: the [row,col] is empty. thus, top[col]-1 > row.
	private int getNumofBlocksAbove(int[][] field, int[] top, int row, int col){
	    int numOfBlocks = 0;
	    
	    for (int i = row+1; i < top[col]; i++){
	        
	        if (field[i][col] != 0){
	            return top[col] - i;
	        }
	    }
	    return numOfBlocks;
	}
	private int max(int a, int b){
	    return a>b? a:b;
	}
	private int min(int a, int b){
	    return a<b? a:b;
	}
	// The function returns the sum of cost of each gap detected in a specific row
    public double getCostOfGap(int[][] field, int[] top, int row) {
		// TO BE IMPLEMENTED
	    //boolean[] possibleGaps = new boolean[State.COLS];
        int[] gapWidth = new int[State.COLS];
        int[] numOfBlocksAbove = new int[State.COLS];
        double cost = 0;
        
        int width = 0;
        // System.out.print("gap width: ");
        for(int c =State.COLS-1;c>=0;c--){
            if(field[row][c] != 0){
                width=0;
            }else{
                //every one will be remembered.
                numOfBlocksAbove[c] = getNumofBlocksAbove(field, top, row, c);
                //System.out.println("NumofBlocksAbove" + numOfBlocksAbove[c] );
                width+=1;
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
                
                if(gapWidth[col]==1){
                    cost+=getGapCostOfCell(field, top, row, col, gapWidth[col],numOfBlocksAbove[col]);
                }else{
                    cost+=getGapCostOfCell(field, top, row, col, gapWidth[col],max(numOfBlocksAbove[col],numOfBlocksAbove[col+1]));
                }
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
    
    //The function returns the cost of gap at the particular empty cell or two adjacent empty cells
    private double getGapCostOfCell(int[][] field, int[] top, int row, int col, int width, int numOfBlocksAbove){
        int gapType;
        int[] heightDifference = new int[4];
        
        for(int increment =- 2; increment < 2; increment++){
            if(((col+increment) >= 0)&&((col+increment+width) < State.COLS)){
                if(increment < 0){
                    heightDifference[increment+2]= top[col+increment]-row-numOfBlocksAbove;
                }else{
                    heightDifference[increment+2]= top[col+increment+width]-row - numOfBlocksAbove;
                }
            }else{
                //deal with the walls
                if (col==0){
                    if (increment == -2){
                        heightDifference[increment+2] = State.ROWS;
                    }else if(increment == -1){
                        heightDifference[increment+2] = State.ROWS - numOfBlocksAbove;
                    }
                }else if (col == 1){
                    if (increment == -2){
                        heightDifference[increment+2] = State.ROWS;
                    }
                }else if((col+width)>=State.COLS){
                    if (increment == 1){
                        heightDifference[increment+2] = State.ROWS;
                    }else if(increment == 0){
                        heightDifference[increment+2] = State.ROWS - numOfBlocksAbove;
                    }
                }else{
                    if (increment == 2){
                        heightDifference[increment+2] = State.ROWS;
                    }
                }
            }
        }
        
        gapType = getGapType(heightDifference[0],heightDifference[1],heightDifference[2],heightDifference[3]);
        
        return LEARNEDGAPCOST[width-1][gapType];
    }
    
    // The function returns the type of gap
	private int getGapType(int diff1, int diff2, int diff3, int diff4){
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
			System.out.println("Invalid row num: it should not be negative");
			return -1;
		}
		
		if(row >= field.length){
			System.out.println("Invalid row num: it should not exceed the maximum");
			return -1;
		}
		
		if(row == field.length - 1){
			return 0;
		}else{
			int[] spacesAtTheRow = field[row];
            
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
    	// result[i][0] stores the number of dependent lines of the ith row.
    	//The following calculates the upper rows' dependent rows first, in order to reduce the duplicate calculations.
    	
        boolean[][] dependentRows = new boolean[State.ROWS][State.ROWS];
    	for(int row = State.ROWS - 2; row>=0; row--){
    		setDependentRowsOfARow(field,row,dependentRows);
    	}
    	int[][] results = format(dependentRows);
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
		
        for(int row =0; row < dependentRows.length; row++){
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
    
    //This method returns an array of dependent rows' number of the row
	private void setDependentRowsOfARow(int field[][],int row,boolean dependentRows[][]){
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
		boolean[] dependentRowsOfTheHole = new boolean[State.ROWS];
		
		for(int r = row;r<State.ROWS;r++){
			int upperPieceIndicator = field[r][col];
			//If one of the upper piece is occupied, then we conclude that the piece we are checking is a hole
			if(upperPieceIndicator != 0){
				dependentRowsOfTheHole[r] = true;
				dependentRowsOfTheHole = combineDependentRows(dependentRowsOfTheHole,dependentRows[r]);
			}
		}
		
		return dependentRowsOfTheHole;
	}
	
	private ArrayList<Integer> getHoles(int field[][],int row){
		if(row < 0){
			System.out.println("Invalid row num: it should not be negative");
			return null;
		}
		
		if(row >= field.length){
			System.out.println("Invalid row num: it should not exceed the maximum");
			return null;
		}
		
		if(row == field.length - 1){
			return null;
		}else{
			int[] spacesAtTheRow = field[row];
			
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
		
		return -1;
	}
	
	public void playWithSpaceKey() {
		State s = new State();
		new TFrame(s);
        
		while(!s.hasLost()) {
			if (s.spacePressed) {
				int t = pickMove(s,s.legalMoves());
				s.makeMove(t);
				s.draw();
				s.drawNext(0,0);
				s.spacePressed = false;
			}
		}
        
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
	public int getBlockageNum(int[][] field){
		int totalBlockages = 0;
        
		for(int c = 0; c < State.COLS; c++){
			ArrayList<Integer> holes = getEmptySpacesOfACol(field,c);
			int baOfLastHole = 0;
			int lastRow = State.ROWS;
			for(int r=holes.size()-1;r>=0;r--){
				int blockagesOfAHole = getBlockageOfFromHoleAToHoleB(field,r,lastRow,c) + baOfLastHole;
				totalBlockages += blockagesOfAHole;
				baOfLastHole = blockagesOfAHole;
				lastRow = r;
			}
		}
        
		return totalBlockages;
	}
	
	public ArrayList<Integer> getEmptySpacesOfACol(int[][] field,int col){
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for(int r=0;r<State.ROWS;r++){
			int space = field[r][col];
			if(space == 0){
				arr.add(r);
			}
		}
		return arr;
	}
	
	public double getRandomAtPosition(int k) {
        //		switch (k) {
        //			case 0: return Math.random()*15-10; // ALPHA -10~5
        //			case 1: return Math.random()*20+10; // B 10~30
        //			case 2: return Math.random()*8-2; // A -2~6
        //			case 3: return Math.random()*5-2; // W -2~3
        //			case 4: return Math.random()*4-1; // C -1~3
        //			case 5:
        //			case 6:
        //			case 7: return Math.random()*2;
        //		};
        //		if (k == 1) {
        //			return Math.random()*10-5;
        //		} else return Math.random()*15-5;
		
		double range = lowerUpperBound[k][1] -lowerUpperBound[k][0];
		return Math.random()*range + lowerUpperBound[k][0];
	}
	
	public void runPSO() {
		int S = 63;
		int numTestCase = 10;
		double OMEGA = -0.3593, THETAP = -0.7238, THETAQ = 2.0289;
		int numOfParameters = 6;
		int optimal = 0;
		double[] optimalParameter = new double [numOfParameters];
		int[] pv = new int[S];
		double [] d = new double [numOfParameters];
		double[][] p = new double [S][numOfParameters];
		double[][] x = new double [S][numOfParameters];
		double[][] v = new double [S][numOfParameters];
        
		// initialization
		for (int i = 0; i< numOfParameters; i++) {
			d[i] = lowerUpperBound[i][1] - lowerUpperBound[i][0];
		}
		for (int i = 0; i < S; i++) {
			for (int j = 0; j<numOfParameters; j++) {
				p[i][j] = getRandomAtPosition(j);
				x[i][j] = p[i][j];
				// (-d, d) where d = | upperbound - lowerbound|
                v[i][j] = Math.random() *2*d[j] - d[j];
			}
			setParameter(p[i]);
			pv[i] = getAverageLinesCleared(numTestCase);
			if (pv[i] > optimal) {
				optimal = pv[i];
				optimalParameter = p[i];
			}
		}
		System.out.println(optimal);
		
		// Iterate
		for (int it = 0; it < 10; it++) {
			for (int i = 0; i< S; i++) {
				double r1 = Math.random();
				double r2 = Math.random();
				for (int j = 0; j< numOfParameters; j++) {
					v[i][j] = OMEGA * v[i][j] + r1*THETAP * (p[i][j] - x[i][j])
                    + r2*THETAQ * (optimalParameter[j] -x[i][j]);
					
					// Bound the velocity
					v[i][j] = getBound(v[i][j], -d[j], d[j]);
					x[i][j] += v[i][j];
					x[i][j] = getBound(x[i][j], lowerUpperBound[j][0], lowerUpperBound[j][1]);
				}
				setParameter (x[i]);
				
                int res = getAverageLinesCleared(numTestCase);
				if (res > pv[i]) {
					pv[i] = res;
					p[i] = x[i];
				}
				if (res > optimal) {
					setWriteToFile(fileName);
					optimal = res;
					System.out.println(res);
					optimalParameter = x[i];
					outputParameter();
					if (toFile) {
						writer.println(optimal);
					}
					writer.close();
				}
			}
		}
	}
	
	public double getBound (double x, double l, double u) {
		if (x > u) return u;
		if (x< l) return l;
		return x;
	}
	
	public void runGeneticAlgorithm() {
		// ALPHA refers to the coefficient for rows cleared feature
        //		public static  double ALPHA = -1;
        //		// B refers to the coefficient for number of holes in each row
        //		public static  double B = 19;
        //		// A refers to the bonus cost for each existing dependent lines
        //		public static double A = 1;
        //		// W refers to the penalty coefficient for well
        //		public static double W = 0.9;
        //		// C refers to the penalty for line difference
        //		public static  double C = 0.2;
        //		// AD refers to the coefficient multipied to each dependent line costs
        //		public static double AD = 0.8;
		int numOfParameters = 6;
		int size = 30;
		int numOfTop = 7;
		double[][] parameters = new double[size][numOfParameters];
		int[] results = new int [size];
		
		// range : -10 --- 20 , precision 0.1
		for (int i = 0; i<size; i++) {
			for (int j = 0; j<numOfParameters; j++) {
				parameters[i][j] = getRandomAtPosition(j);
			}
		}
		
		for (int i = 0; i<size; i++) {
			setParameter(parameters[i]);
			results[i] = getAverageLinesCleared(10);
		}
		
		int[] topResultsIndex,  newResults = null;
		int numOfCrossBreed = numOfTop * (numOfTop-1);
		double[][] newParameters = null;
		for (int iteration = 0; iteration <= 10; iteration ++) {
			topResultsIndex = getTopResultsIndex(numOfTop, results);
			newParameters= new double [numOfCrossBreed + numOfTop][numOfParameters];
			newResults = new int[numOfCrossBreed + numOfTop];
			// crossbreeding and mutation
			// P(mutate) = 0.1
			int index = -1;
			for (int i = 0; i< numOfTop; i++) {
				for (int j = 0; j<numOfTop; j++) {
					if (i!=j) {
                        index ++;
                        // Start to crossbreed P[topindex[i]] and P[topindex[j]]
                        for (int k = 0; k< numOfParameters; k++) {
                            double random = Math.random();
                            // Select which parent to inherit from
                            if (random <=0.5 ) {
                                newParameters[index][k] = parameters[topResultsIndex[i]][k];
                            } else {
                                newParameters[index][k] = parameters[topResultsIndex[j]][k];
                            }
                            random = Math.random();
                            // Decide if to mutate
                            if (random <= 0.1) {
                                newParameters[index][k]  = getRandomAtPosition(k);
                                //System.out.println("randomizde!");
                            }
                        }
                        setParameter(newParameters[index]);
                        double [] a = getAverageLinesClearedWithSd(10);
                        newResults[index] = (int)a[1];
                        if (a[1] >= 1000 ) {
                            //System.out.println("Hey I am outputing!");
                            outputParameter();
                            if (!toFile) {
                                System.out.println(a[0]+ "  "+newResults[index]);
                            } else {
                                writer.println(a[0]+ "  "+newResults[index]);
                            }
                            
                            if (toFile) {
                                writer.flush();
                                writer.close();
                                setWriteToFile(this.fileName);
                            }
                        }
					}
				}
			}
			
			for (int i = 0; i < numOfTop; i++) {
				newParameters [i  + numOfCrossBreed] = parameters[topResultsIndex[i]];
				newResults [i + numOfCrossBreed] = results[topResultsIndex[i]];
			}
            
			topResultsIndex = getTopResultsIndex(size, newResults);
			for (int i = 0; i<size; i++) {
				parameters[i] = newParameters[topResultsIndex[i]];
			}
            
		}
		
		int bestResultIndex = 0;
		for (int i = 1; i< numOfTop + numOfCrossBreed; i++) {
			if (newResults[i] > newResults[bestResultIndex]) {
				bestResultIndex = i;
			}
		}
		setParameter(newParameters[bestResultIndex]);
	}
	
	public int[] getTopResultsIndex (int numOfTop, int[] results) {
		int size = results.length;
		int[] ans = new int [numOfTop];
		for (int i = 0; i< numOfTop; i++) {
			ans[i] = i;
		}
		for (int i = numOfTop; i< size ; i++) {
			int k = -1;
			for (int j= 0; j<numOfTop; j++)
				if (results[i] > results[ans[j]]) {
					if (k == -1  || results[ans[j]]<results[ans[k]]) k = j;
				}
			if (k!=-1) {
				ans[k] = i;
			}
		}
		return ans;
	}
	
	public void outputParameter() {
		if (toFile) {
			writer.println("LH = " + LANDINGHEIGHT + " RC = "+ROWSCLEARED + " RT = "+ROWTRANSITIONS
                           +" CT = " +COLTRANSITIONS + " H = " + HOLES + " W = "+ WELLS);
		} else {
			System.out.println("LH = " + LANDINGHEIGHT + " RC = "+ROWSCLEARED + " RT = "+ROWTRANSITIONS
                               +" CT = " +COLTRANSITIONS + " H = " + HOLES + " W = "+ WELLS);
		}
		
	}
	public void setParameter(double[] parameter) {
        //		public static double LANDINGHEIGHT = 4.500158825082766;
        //		public static double ROWSCLEARED = -3.4181268101392694;
        //		public static double ROWTRANSITIONS = 3.2178882868487753;
        //		public static double COLTRANSITIONS = 9.348695305445199;
        //		public static double HOLES = 7.899265427351652;
        //		public static double WELLS = 3.3855972247263626;
		
		LANDINGHEIGHT = parameter[0];
		ROWSCLEARED = parameter[1];
		ROWTRANSITIONS = parameter[2];
		COLTRANSITIONS = parameter[3];
		HOLES = parameter[4];
		WELLS = parameter[5];
		
	}
	
	public int getBlockageOfFromHoleAToHoleB(int[][] field,int rowStart,int rowEnd,int col){
		int count = 0;
		for(int r=rowStart;r<rowEnd;r++){
			int space = field[r][col];
			if(space != 0){
				count ++;
			}
		}
		return count;
	}
	public void playWithVisual(int sleepAmount) {
		State s = new State();
		new TFrame(s);
		int i=1;
		while(!s.hasLost()) {
			int t = pickMove(s,s.legalMoves());
            
			s.makeMove(t);
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(sleepAmount/100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(s.getRowsCleared() > i * 5000){
				System.out.println("For now, the cost is:" + s.getRowsCleared());
				i ++;
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
	
	public int getAverageLinesCleared (int testNum) {
		double [] ans = getAverageLinesClearedWithSd(testNum);
		return (int) ans[1];
	}
	
	// It now returns an array, r[0] stores the standard deviation, r[1] stores the average
	public double[] getAverageLinesClearedWithSd (int testNum) {
		int average=0;
		int[] r = new int[testNum+1];
		for (int i=0; i<testNum; i++) {
			State s = new State();
            int j=1;
			while(!s.hasLost()) {
				int t = pickMove(s,s.legalMoves());
				s.makeMove(t);
                if(s.getRowsCleared() > j * 5000){
                    // System.out.println("For test num "+ i +". For now, the cost is:" + s.getRowsCleared());
                    j ++;
                }
			}
			
			r[i+1] = s.getRowsCleared();
			average += r[i+1];
            
		}
        r[0] = average/testNum;
        double sd = 0;
        for (int i = 1; i<=testNum; i++) {
        	sd += ((r[i]-r[0])*(r[i]-r[0]));
        }
        double[] ans = new double [] {Math.sqrt(sd/testNum), r[0]};
        return ans;
		//System.out.println("when B = " +B +" and alpha = "+ALPHA +", your average lines cleared is "+ average/testNum);
	}
	
	
	public void setWriteToFile(String fileName) {
		this.toFile = true;
		this.fileName = fileName;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
    
	public static void main(String[] args) {
		long sec = System.currentTimeMillis();
		String fileName = Long.toString(sec);
		
		PlayerSkeleton p = new PlayerSkeleton();
		p.setWriteToFile(fileName);
		//p.runGeneticAlgorithm();
		p.runPSO();
		p.writer.close();
        //		AD =0.11;
        //		W = 0.35;
        //		W = 0.5;
		//p.getAverageLinesCleared(1);
		//p.getAverageLinesCleared(2);
        //		p.playWithSpaceKey();
		//p.playWithVisual(0);
		
	}
}
