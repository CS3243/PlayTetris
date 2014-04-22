import java.io.PrintWriter;
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
	public static int [][][] fullLegalMoves = State.legalMoves;
    
    public static final int NumRowsLookAhead = 5;
	public static final double MAX= Double.MAX_VALUE;
	public static final double gapCostForLongerWidth = 1;
	
	//Debug use
	int turn = 0;
    
	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
        //Debug use
        turn ++;
        
        //initialization of variables
		int[][][] topFields = new int[NumRowsLookAhead][State.ROWS][State.COLS];
		int[][] topTops = new int[NumRowsLookAhead][State.COLS];
		int[] topMove = new int[NumRowsLookAhead];
		double[] topCost = new double[NumRowsLookAhead];
        int[][] oldField = s.getField();
		int[] oldTop = s.getTop();
        double minCost = MAX;
        int minCostMove =0, maxHeight = 0;
        
        for (int i = 0; i<5; i++) {
			topCost[i] = Integer.MAX_VALUE;
		}
		
        for (int i= 0; i< legalMoves.length; i++) {
			int[] top = new int[State.COLS];
			int[][] field = new int[State.ROWS][State.COLS];
			
			for (int k = 0; k<State.COLS; k++)
				top[k] = oldTop[k];
			
			for (int j = 0; j < State.ROWS; j++)
				for (int k = 0; k<State.COLS; k++)
					field[j][k] = oldField[j][k];
			
            //cost for rows cleared or game ended
		 	double cost = computeMoveCost(s.nextPiece, legalMoves[i][State.ORIENT], legalMoves[i][State.SLOT], field, top, s.getTurnNumber()+1);
			
            //cost for all other factors
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
			
			int indexToReplace = -1;
			for (int j= 0; j<NumRowsLookAhead; j++)
				if (cost < topCost[j]) {
					if (indexToReplace == -1  || topCost[j]>topCost[indexToReplace]) indexToReplace = j;
				}
			
			if (! (indexToReplace== -1)) {
				topMove[indexToReplace] = i;
				topCost[indexToReplace] = cost;
				topTops[indexToReplace] = top;
				topFields[indexToReplace] = field;
			}
		}
		
		if (maxHeight >= State.ROWS-NumRowsLookAhead) 	{
			minCostMove = bestLookAheadMove(topMove, topCost, topTops, topFields, s.getTurnNumber()+2);
		}
		return minCostMove;
	}
	
	public int bestLookAheadMove(int[] topMove, double[] topCost, int[][] topTops, int[][][] topFields, int turnNumber) {
		double bestAmortizedCost = MAX, bestMoveCost;
		int bestAmortizedMove = 0;
		
		for (int i = 0; i<NumRowsLookAhead; i++) {
            if (topCost[i] != Integer.MAX_VALUE) {
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
		double stateCost = 0;
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
		
		// Landing Height
        double height = 0.0;
        height = 0.5 * (double)( pieceMinY + pieceMaxY );
		stateCost += LANDINGHEIGHT * height;
        
		// Calculate the cost of each column
		for (int j = 0; j<State.COLS; j++) {
			if (top[j] > highestRow) highestRow = top[j];
			stateCost += COLTRANSITIONS * getTransitionCountForColumn(field, j, top[j]);
			stateCost += WELLS * getAllWellsForColumn(field, j);
		}
		
		// Calculate the cost of each row
		for (int j=highestRow; j>=0; j--) {
            double cost1 = HOLES * getNumberOfHoles(field, j);
			costOfEachRow[j] +=  cost1;
			double cost2 = ROWTRANSITIONS * getTransitionCountForRow(field, j);
			costOfEachRow[j] += cost2;
			stateCost += costOfEachRow[j];
		}
        
		return stateCost;
	}
    
    public double getTransitionCountForColumn(int[][] field, int j, int top) {
    	int transitionCount = 0;
		int cellUp, cellDown;
        
        // check cell and neighbor above
        for (int r = 0; r < top; r++) {
            cellUp = field[r][j];
            cellDown = field[r+1][j];
            
            // If a transition from occupied to unoccupied, or from unoccupied to occupied, then it's a transition.
            if ((cellUp != 0 && cellDown == 0) ||
                (cellUp == 0 && cellDown != 0)) {
            	transitionCount++;
            }
        }
        
        // check transition between bottom-exterior and row Y=1.
        // Bottom exterior is implicitly "occupied"
        cellUp = field[0][j];
        if (cellUp == 0) {
            transitionCount++;
        }
        
        // check transition between column 'mHeight' and above-exterior.
        // Sky above is implicitly un-"occupied"
        cellUp = field[State.ROWS - 1][j];
        if (cellUp == 0) {
            transitionCount++;
        }
        
        return transitionCount;
    }
    
    public int getAllWellsForColumn(int[][]field, int c) {
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
    
    public int getBlanksDownBeforeBlockedForColumn(int[][]field, int c, int topRow) {
        int totalBlanksBeforeBlocked = 0;
        int cellValue;
        
        for (int row = topRow; row >= 0; row-- ) {
            cellValue = field[row][c];
            
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
		int cellLeft, cellRight;
        
        for (int c = 0; c < State.COLS - 1; c++ ) {
            cellLeft = field[j][c];
            cellRight = field[j][c+1];
            
            // If a transition from occupied to unoccupied, or
            // from unoccupied to occupied, then it's a transition.
            if ((cellLeft != 0 && cellRight == 0) ||
            	(cellLeft == 0 && cellRight != 0)) {
                transitionCount++;
            }
        }
        
        // check transition between left-exterior and column 1.
        // Note: Exterior is implicitly "occupied".
        cellLeft = field[j][0];
        if (cellLeft == 0) {
            transitionCount++;
        }
        
        // check transition between column 'mWidth' and right-exterior.
        // (NOTE: Exterior is implicitly "occupied".)
        cellLeft = field[j][State.COLS-1];
        if (cellLeft == 0) {
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
			}
		}
		return cost;
	}
    
	// The method returns number of rows cleared. If the game fails, it returns -1.
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
		
		//check whether game ended
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
	
	private int max(int a, int b){
	    return a>b? a:b;
	}
    
	private int min(int a, int b){
	    return a<b? a:b;
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
	
	public double getRandomAtPosition(int k) {
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
					//setWriteToFile(fileName);
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
		LANDINGHEIGHT = parameter[0];
		ROWSCLEARED = parameter[1];
		ROWTRANSITIONS = parameter[2];
		COLTRANSITIONS = parameter[3];
		HOLES = parameter[4];
		WELLS = parameter[5];
		
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
				System.out.println("For now, the number of rows cleared is:" + s.getRowsCleared());
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
                    System.out.println("Test num "+ i +". For now, the number of rows cleared is: " + s.getRowsCleared());
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
	}
    
	public static void main(String[] args) {
		PlayerSkeleton p = new PlayerSkeleton();
		p.playWithVisual(0);
	}
}