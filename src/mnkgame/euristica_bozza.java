
public int longSeriesBonus(int n, int consecutive, int marked){
    if(n>=K){
      if(consecutive == K-1) return 10_000_000; //10M
      if(consecutive == K-2) return 100_000;    //100k
      if(consecutive == K-3) return 1_000;      //1k
      else return (marked/n) * 1_000;
    }
    return 0;
  }
  /*for(int z=0;z<maxIter;z++){
      if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P1){
        if(last==2){
          value -= longSeriesBonus(c2series);
          c2series = 0;
        }
        c1series += 1 + freeSeries;
        last = 1;
      }
      else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P2){
        if(last==1){
          value += longSeriesBonus(c1series);
          c1series = 0;
        }
        c2series += 1 + freeSeries;
        last=2;
      }
      else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.FREE){
        if(last==1){
          c1series++;
        }
        if(last==2){
          c2series++;
        }
        cFree++;
      }
  
    }
    return value;*/
  
  //sequenza finale, aggiungere marked longSeriesBonus
  public int depthCell(int i, int j, int dir_i, int dir_j, int maxIter){
    int value = 0;
    //last_player è 1 o 2
    int lastPlayer = 0;
    int prev = -1;
    int marked = 0, series = 0, maxSeries = 0;
    int c1series = 0, c2series = 0, lastFreeSeries = 0;
    for(int z=0;z<maxIter;z++){
      if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P1){
        if(lastPlayer==2){
          value -= longSeriesBonus(c2series, maxSeries,marked);
          maxSeries = 0;
          c2series = 0;
          c1series = 1 + lastFreeSeries;
          marked = 1;
        }
        else{
          if(prev==1) series++;
          else{
            if(series>maxSeries) maxSeries = series;
            series = 1;
          }
          marked++;
          c1series++;
        }
        lastFreeSeries = 0;
        lastPlayer = 1;
        prev = 1;
      }
      else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.P2){
        if(lastPlayer==1){
          value += longSeriesBonus(c1series, maxSeries, marked);
          maxSeries = 0;
          c1series = 0;
          c2series = 1 + lastFreeSeries;
          marked = 1;
        }
        else{
          if(prev==2) series++;
          else {
            if(series>maxSeries) maxSeries = series;
            series = 1;
          }
          marked++;
          c2series++;
        }
        lastFreeSeries = 0;
        lastPlayer = 2;
        prev = 2;
      }
      else if(B.cellState(i+z*dir_i,j+z*dir_j) == MNKCellState.FREE){
        if(lastPlayer==1){
          c1series++;
        }
        if(lastPlayer==2){
          c2series++;
        }
        if(prev!=1 && prev!=2){
          lastFreeSeries++;
        }
        else lastFreeSeries = 1;
      }
    }
    return value;
  }
  
  public int heuristic() {
    int i,j,value = 0;
    //row
    for (i = 0; i < M; i++) {
      j = 0;
      value += depthCell(i,j,0,1,N);
    }
    //column
    for (j = 0; j < N; j++) {
      i = 0;
      value += depthCell(i,j,1,0,M);
    }
    
    int maxLen = minMN;
    int nMaxDiag = Math.abs(M-N)+1;
    //diagonal
    if(M>=N){
      for (j = 1; j < N; j++) {
        if(maxLen-j>=K){
          i = 0;
          value += depthCell(i,j,1,1,maxLen-j);
        }
        else break;
      }
      for(i = 0; i < M; i++){
        if(i<nMaxDiag){
          j=0;
          value += depthCell(i,j,1,1,maxLen);
        }
        else if(maxLen-(i+1-nMaxDiag)>=K){
          value += depthCell(i,j,1,1,maxLen-(i+1-nMaxDiag));
        }
        else break;
      }
    }
    else{
      for (j = 0; j < N; j++) {
        if(j<nMaxDiag){
          i = 0;
          value += depthCell(i,j,1,1,maxLen);
        }
        else if(maxLen-(j+1-nMaxDiag)>=K){
          value += depthCell(i,j,1,1,maxLen-(j+1-nMaxDiag));
        }
        else break;
      }
      for(i = 1; i < M; i++){
        if(maxLen-i>=K){
          j = 0;
          value += depthCell(i,j,1,1,maxLen-i);
        }
        else break;
      }
    }
    
    //antidiagonal
    if(M>=N){
      for (j = N-2; j >= 0; j--) {
        if(maxLen-(N-j-1)>=K){
          i = 0;
          value += depthCell(i,j,1,-1,maxLen-(N-j-1));
        }
        else break;
      }
      for(i = 0; i < M; i++){
        if(i<nMaxDiag){
          j=N-1;
          value += depthCell(i,j,1,-1,maxLen);
        }
        else if(maxLen-(i+1-nMaxDiag)>=K){
          value += depthCell(i,j,1,-1,maxLen-(i+1-nMaxDiag));
        }
        else break;
      }
    }
    else{
      for (j = N-1; j >=0; j--) {
        if(N-j<nMaxDiag){
          i = 0;
          value += depthCell(i,j,1,-1,maxLen);
        }
        else if(maxLen-(N-j-nMaxDiag)>=K){
          value += depthCell(i,j,1,-1,maxLen-(N-j-nMaxDiag));
        }
        else break;
      }
      for(i = 1; i < M; i++){
        if(maxLen-i>=K){
          j = N-1;
          value += depthCell(i,j,1,-1,maxLen-i);
        }
        else break;
      }
    }
    return value;
  }