package gov.nih.mipav.model.structures.jama;


import gov.nih.mipav.view.*;

public class GeneralizedInverse {
    // This is a port of the FORTRAN suborutine GINV2, A Simple Algorithm for Computing the Generalized Inverse
    // of a Matrix by B. Rust, W. R. Burrus, and C. Schneeberger, CACM 9(5): 381-387 (May, 1966)
    private double A[][];
    
    // First dimension of A
    private int MR;
    // Number of rows in A
    private int NR;
    // Number of columns in A
    private int NC;
    
    public GeneralizedInverse() {
        
    }
    
    public GeneralizedInverse(double A1[][], int NR) {
        int i;
        int j;
        this.NR = NR;
        MR = A1.length;
        NC = A1[0].length;
        A = new double[NR][NC];
        for (i = 0; i < NR; i++) {
            for (j = 0; j < NC; j++) {
                A[i][j] = A1[i][j];
            }
        }
    }
    
    public double[][] ginv() {
      int i;
      double Ainv[][] = new double[NR][NC];
      double U[][] = new double[NC][NC];
      double aflag[] = new double[NC];
      double atemp[] = new double[NC];
      double fac;
      int n;
      double tol;
      int j;
      double dot1;
      int jm1;
      int L;
      int k;
      double dot2;
      
      for (i = 0; i < NC; i++) {
          U[i][i] = 1.0;
      }
      fac = dot(A, NR, 1, 1);
      fac = 1.0/Math.sqrt(fac);
      for (i = 0; i < NR; i++) {
          A[i][0] = A[i][0] * fac;
      }
      for (i = 0; i < NC; i++) {
          U[i][0] = U[i][0] * fac;
      }
      aflag[0] = 1.0;
      
      // Dependent column tolerance for n bit floating point precision
      n = 27;
      tol = 10.0 * Math.pow(0.5, n);
      tol = tol * tol;
      for (j = 2; j <= NC; j++) {
          dot1 = dot(A, NR, j, j);
          jm1 = j - 1;
          for (L = 1; L <= 2; L++) {
              for (k = 1; k <= jm1; k++) {
                  atemp[k-1] = dot(A, NR, j, k);    
              } // for (k = 1; k <= jm1; k++)
              for (k = 1; k <= jm1; k++) {
                  for (i = 1; i <= NR; i++) {
                      A[i-1][j-1] = A[i-1][j-1] - atemp[k-1]*A[i-1][k-1]*aflag[k-1];    
                  } // for (i = 1; i <= NR; i++)
                  for (i = 1; i <= NC; i++) {
                      U[i-1][j-1] = U[i-1][j-1] - atemp[k-1]*U[i-1][k-1];
                  } // for (i = 1; i <= NC; i++)
              } // for (k = 1; k <= jm1; k++)
          } // for (L = 1; L <= 2; L++)
          dot2 = dot(A, NR, j, j);
          if (((dot2/dot1) - tol) <= 0.0) {
              for (i = 1; i <= jm1; i++) {
                  atemp[i-1] = 0.0;
                  for (k = 1; k <= i; k++) {
                      atemp[i - 1] = atemp[i - 1] + U[k-1][i-1]*U[k-1][j-1];
                  } // for (k = 1; k <= i; k++)
              } // for (i = 1; i <= jm1; i++)
              for (i = 1; i <= NR; i++) {
                  A[i-1][j-1] = 0.0;
                  for (k = i; k <= jm1; k++) {
                      A[i-1][j-1] = A[i-1][j-1] - A[i-1][k-1]*atemp[k-1]*aflag[k-1];    
                  } // for (k = i; k <= jm1; k++)
              } // for (i = 1; i <= NR; i++)
              aflag[j-1] = 0.0;
              fac = dot(U, NC, j, j);
              fac = 1.0/Math.sqrt(fac);
          } // if (((dot2/dot1) - tol) <= 0.0)
          else { // ((dot2/dot1) - tol) > 0.0
              aflag[j-1] = 1.0;
              fac = 1.0/Math.sqrt(dot2);
          } // else ((dot2/dot1) - tol) > 0.0)
          for (i = 1; i <= NR; i++) {
              A[i-1][j-1] = A[i-1][j-1]*fac;
          } // for (i = 1; i <= NR; i++)
          for (i = 1; i <= NC; i++) {
              U[i-1][j-1] = U[i-1][j-1]*fac;
          } // for (i = 1; i <= NC; i++)
      } // for (j = 2; j <= NC; j++)
      for (j = 1; j <= NC; j++) {
          for (i = 1; i <= NR; i++) {
              fac = 0.0;
              for (k = j; k <= NC; k++) {
                  fac = fac + A[i-1][k-1]*U[j-1][k-1];
              } // for (k = j; k <= NC; k++)
              A[i-1][j-1] = fac;
          } // for (i = 1; i <= NR; i++)
      } // for (j = 1; j <= NC; j++)
      for (i = 1; i <= NC; i++) {
          for (j = 1; j <= NR; j++) {
              Ainv[i-1][j-1] = A[j-1][i-1];
          }
      }
      return Ainv;
    }
    
    // Computes the inner product of columns JC and KC
    private double dot(double A[][], int NR, int JC, int KC) {
        double dot = 0.0;
        int i;
        for (i = 0; i < NR; i++) {
            dot += A[i][JC-1]*A[i][KC-1];
        }
        return dot;
    }
    
    // This is a port of part of algorithm 645, a program for testing generalized inverse subroutines.
    // ALGORITHM 645 COLLECTED ALGORITHMS FROM ACM.
    // ALGORITHM APPEARED IN ACM-TRANS. MATH. SOFTWARE, VOL. 12, NO. 3, SEPT., 1986, P. 274
    // ORIGINAL CODE BY J. C. NASH 1979, J. C. NASH AND C. E. GRATTON 1982
    // J. C. NASH 1983, 1984
    // J. C. NASH AND R. L. C. WANG 1985, 1986
    public void ginvTest() {
        // The following tests are designed to ensure that the routine ptst is performing correctly.
        // First set up the matrix and a supposed inverse.
        int m[] = new int[1];
        int n[] = new int[1];
        int ma;
        int na;
        int nb;
        int k;
        int mopt;
        int iseed;
        int alpha;
        double A[][] = new double[30][30];
        double X[][] = new double[30][30];
        double C[][] = new double[30][30];
        double ta[] = new double[4];
        double tm[] = new double[4];
        boolean fail[] = new boolean[1];
        int i;
        int j;
        int test;
        int nTests = 1;
        String message[] = new String[nTests];
        int mVar[] = new int[nTests];
        int nVar[] = new int[nTests];
        int kVar[] = new int[nTests];
        int moptVar[] = new int[nTests];
        int iseedVar[] = new int[nTests];
        int alphaVar[] = new int[nTests];
        m[0] = 3;
        n[0] = 2;
        A[0][0] = 1.0;
        A[0][1] = 2.0;
        A[1][0] = 2.0;
        A[1][1] = -3.0;
        A[2][0] = 0.5;
        A[2][1] = 0.0;
        
        // Supposed inverse
        X[0][0] = 0.1;
        X[0][1] = 0.2;
        X[0][2] = -0.2;
        X[1][0] = 0.3;
        X[1][1] = -0.3;
        X[1][2] = 0.9;
        ma = 30;
        na = 30;
        Preferences.debug("Test ptst with 3 by 2 matrices to verify matrix multiplications\n");
        Preferences.debug("A = " + "\n");
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 2; j++) {
                Preferences.debug(A[i][j] + " ");    
            }
            Preferences.debug("\n");
        }
        Preferences.debug("X = " + "\n");
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 3; j++) {
                Preferences.debug(X[i][j] + " ");    
            }
            Preferences.debug("\n");
        }
        ptst(m, n, A, ma, X, na, C, ma, ta, tm, fail);
        
        // Approximate results
        // Test PTST with 3 by 2 matrices to verify matrix multiplications
        //                 A                       X
        //              1.0   2.0           0.1   0.2   -0.2
        //              2.0  -3.0           0.3  -0.3    0.9
        //              0.5   0.0
        //  Test Penrose Conditions
        //  A is the input matrix, X is the inverse of A
        //  Input matrix norm     = 4.27200100E00
        //  Inverse matrix norm =   1.03923000E00
        //                       Average Deviation       Maximum Deviation
        // AXA=TEST=A ACTUAL        0.891666                 2.3
        //            NORMALIZED    0.2087232                0.5383886
        // XAX=TEST=X ACTUAL        0.1875                   0.42
        //            NORMALIZED    0.1804217                0.4041449
        // (AX)T=TEST=AX ACTUAL     0.1683332                0.32
        //               NORMALIZED 0.3791636                0.7207864
        // (XA)T=TEST=XA ACTUAL     0.55                     0.55
        //               NORMALIZED 0.1238851                0.1238851
        
        // Reverse call to test row size < column size case
        Preferences.debug("Same test on ptst with A and X interchanged\n");
        ptst(n, m, X, na, A, ma, C, ma, ta, tm, fail);
        
        // Test row and column vector problems
        // Matrix (column vector) A
        A[0][0] = 3.0;
        A[1][0] = 4.0;
        
        // Matrix (row vector) X
        X[0][0] = 3.0/25.0;
        X[0][1] = 4.0/25.0;
        
        m[0] = 2;
        n[0] = 1;
        Preferences.debug("Test ptst with row and column vectors\n");
        Preferences.debug("A = " + "\n");
        Preferences.debug(A[0][0] + "\n");
        Preferences.debug(A[1][0] + "\n");
        Preferences.debug("X = " + "\n");
        Preferences.debug(X[0][0] + " " + X[0][1] + "\n");
        ptst(m, n, A, ma, X, na, C, ma, ta, tm, fail);
        
        // Reverse call to test row size < column size case
        Preferences.debug("Same vector test on ptst with A and X interchanged\n");
        ptst(n, m, X, na, A, ma, C, ma, ta, tm, fail);
        
        // Test trivial case (1 by 1)
        A[0][0] = 2.0;
        X[0][0] = 0.5;
        m[0] = 1;
        n[0] = 1;
        Preferences.debug("Test ptst with trivial 1 by 1 matrix A = 2.0 and matrix X = 0.5\n");
        ptst(n, m, X, na, A, ma, C, ma, ta, tm, fail);
        
        // Test main driver code for generalized inverse testing
        
        // ma is the maximum first dimension of working arrays
        // na is the maximum second dimension of working arrays
        // nb is max(ma, na) and the first and second dimension of the working array B.
        
        // Size of the input matrix should not exceed 30 by 30
        
        for (test = 0; test < nTests; test++) {
            // top of cycle
            ma = 30;
            na = 30;
            nb = 30;
            
            // m and n give the size of the matrix to be generated by gmatx
            // k gives its rank
            // mopt = 0 if gmatx is to be called
            // mopt = 1, 2, 3, 4 for zielike matrices
            //      = -1, -2, -3, -4 for their Moore-Penrose inverses
            // iseed = an integer seed for use by the random number generator called by gmatx
            // alpha = a parameter used to adjust the singular values of the matrix generated by gmatx
            
            // For details of the controls, see the comments in the routines gmatx and zielike
            Preferences.debug("Test number = " + (test + 1) + "\n");
            Preferences.debug(message[test] + "\n");
            // For each test set m, n, k, mopt, iseed, alpha
            m[0] = mVar[test];
            n[0] = nVar[test];
            k = kVar[test];
            mopt = moptVar[test];
            iseed = iseedVar[test];
            alpha = alphaVar[test];
            
            // initialize error flag to imply correct execution
            fail[0] = false;
            
            Preferences.debug("m = " + m + "\n");
            Preferences.debug("n = " + n + "\n");
            Preferences.debug("k = " + k + "\n");
            Preferences.debug("mopt = " + mopt + "\n");
            Preferences.debug("iseed = " + iseed + "\n");
            Preferences.debug("alpha = " + alpha + "\n");
            
            // Other tests for valid inputs are made the routines
            
            // Check for dimensions exceeded
            if (m[0] >= ma) {
                Preferences.debug("m[0] >= ma dimension exceeded input ignored\n");
                continue;
            }
            if (n[0] >= na) {
                Preferences.debug("n[0] >= na dimension exceeded input ignored\n");
                continue;    
            }
            
            if (mopt != 0) {
                // zielke routine call
                // Note that m and n are replaced by appropriate values.
                // mopt is the zielike matrix selected
                // alpha is needed in zielike as a parameter in formulas for the matrix elements generated.
                zielke(m, n, A, ma, na, mopt, alpha, fail);
                if (fail[0]) {
                    continue;
                }
            } // if (mopt != 0)
        } // for (test = 0; test < nTests; test++)
    }
    
    private void ptst(int m[], int n[], double A[][], int ma, double X[][], int nx, double C[][], 
                      int nc, double ta[], double tm[], boolean fail[]) {
        // This subroutine is designed to test a proposed generalized inverse of a matrix labelled A,
        // which is m by n.  X is the n by m matrix containing the supposed inverse.
        // Program originally for square, symmetric matrices written by Richard L. C. Wnag, 1977
        // Modified and adapted by J. C. Nash 1979, 1982
        // Note that A and X may be interchanged -- tester does not object.
        // Average and maximum absolute deviations are caclulated for the matrices
        // AXA - A         ... Penrose condition 1
        // XAX - X         ... Penrose condition 2
        // (AX)T - AX      ... Penrose condition 3
        // (XA)T - XA      ... Penrose condition 4
        // m[0] = number of rows in the 'original' matrix A
        //   = number of columns in purported inverse
        // unchanged by this routine
        // n[0] = number of columns in the 'original' matrix A
        //   = number of rows in purported inverse
        // unchanged by this routine
        // A = 'original' matrix of which a generalized inverse has supposedly been computed
        // unchanged by this program
        // ma = first or row dimension of A
        // unchanged by this program
        // X = purported generalized inverse of matrix A
        // unchanged by this routine
        // nx = first or row dimension of X
        // unchanged by this routine
        // C = double precision working array
        // should be dimensioned at least ma by ma
        // nc = first or row dimension of C
        // unchanged by this routine
        // should be at least as large as ma
        // ta = vector (1 dimensional array) of 4 elements to store average absolute
        // deviations from each of the four penrose conditions
        // tm = vector (1 dimensional array) of 4 elements to store maximum absolute
        // deviations from each of the four penrose conditions
        // fail = failure flag set true for failure
        // false otherwise
        
        // This routine uses double precision accumulation of inner products to limit rounding error.
        // reference Wilkinson, J. H., The Algebraic Eigenvalue Problem, Oxford, 1965
        // ptst calls double anorm(m, n, A, ma)
        // anorm computes the square norm of a matrix A
        // norm = SQRT(SUM(A[i][j]**2), for i = 1, m, j = 1, n)
        // This norm is used for simplicity.  Other norms are acceptable
        double s;
        double v;
        int i;
        int j;
        int j1;
        int L;
        int n1;
        int m1;
        double ama;
        double anx;
        double t1;
        double t2;
        
        // Dimensions must be at least one
        // No tests are made to see if dimensions are at least 1
        
        // Initialize failure flag to indicate a successful operation
        fail[0] = false;
        if (ma <= 0) {
            Preferences.debug("ptst failed because ma = " + ma + " is less than 1\n");
            System.out.println("ptst failed because ma = " + ma + " is less than 1");
            fail[0] = true;
            return;
        }
        if (n[0] <= 0) {
            Preferences.debug("ptst failed because n[0] = " + n[0] + " is less than 1\n");
            System.out.println("ptst failed because n[0] = " + n[0] + " is less than 1");
            fail[0] = true;
            return;    
        }
        if (nx <= 0) {
            Preferences.debug("ptst failed because nx = " + nx + " is less than 1\n");
            System.out.println("ptst failed because nx = " + nx + " is less than 1");
            fail[0] = true;
            return;
        }
        if (m [0]<= 0) {
            Preferences.debug("ptst failed because m[0] = " + m[0] + " is less than 1\n");
            System.out.println("ptst failed because m[0] = " + m[0] + " is less than 1");
            fail[0] = true;
            return;
        }
        if (nc <= 0) {
            Preferences.debug("ptst failed because nc = " + nc + " is less than 1\n");
            System.out.println("ptst failed because nc = " + nc + " is less than 1");
            fail[0] = true;
            return;
        }
        
        // Zero test values
        for (i = 0; i < 4; i++) {
            ta[i] = 0.0;
            tm[i] = 0.0;
        }
        
        // Compute AX
        for (i = 0; i < m[0]; i++) {
            for (j = 0; j < m[0]; j++) {
                s = 0.0;
                for (L = 0; L < n[0]; L++) {
                    s = s + A[i][L] * X[L][j];
                } // for (L = 0; L < n[0]; L++) 
                C[i][j] = s;
            } // for (j = 0; j < m[0]; j++)
        } // for (i = 0; i < m[0]; i++)
        
        // Compute AXA, AXA - A = (m[0] by n[0])
        for (i = 0; i < m[0]; i++) {
            for (j = 0; j < n[0]; j++) {
                s = 0.0;
                for (L = 0; L < m[0]; L++) {
                    s = s + C[i][L] * A[L][j];
                } // for (L = 0; L < m[0]; L++)
                t1 = Math.abs(A[i][j] - s);
                if (t1 > tm[0]) {
                    tm[0] = t1;
                }
                ta[0] = ta[0] + t1;
            } // for (j = 0; j < n[0]; j++)
        } // for (i = 0; i < m[0]; i++)
        ta[0] = ta[0]/(m[0] * n[0]);
        
        // Compute XAX, XAX - X = (n[0] by m[0])
        for (i = 0; i < n[0]; i++) {
            for (j = 0; j < m[0]; j++) {
                s = 0.0;
                for (L = 0; L < m[0]; L++) {
                    s = s + X[i][L]*C[L][j];    
                } // for (L = 0; L < m[0]; L++)
                t1 = Math.abs(X[i][j] - s);
                if (t1 > tm[1]) {
                    tm[1] = t1;
                }
                ta[1] = ta[1] + t1;
            } // for (j = 0; j < m[0]; j++)
        } // for (i = 0; i < n[0]; i++)
        ta[1] = ta[1]/(m[0] * n[0]);
        
        // Asymmetry of AX (m[0] by n[0])
        // Test for trivial case
        // Note that normalization unnecessary when m[0] = 1 since AX will be 1 by 1
        if (m[0] > 1) {
            m1 = m[0] - 1;
            for (i = 1; i <= m1; i++) {
                j1 = i + 1;
                for (j = j1; j <= m[0]; j++) {
                    t1 = Math.abs(C[i-1][j-1] - C[j-1][i-1]);
                    if (t1 > tm[2]) {
                        tm[2] = t1;
                    }
                    ta[2] = ta[2] + t1;
                } // for (j = j1; j <= m[0]; j++)
            } // for (i = 1; i <= m1; i++)
            ta[2] = ta[2]/(m[0]*(m[0]-1)/2);
        } // if (m[0] > 1)
        
        // Asymmetry of XA (n[0] by n[0])
        // Test for trivial case
        // Noate that normalization unnecessary when n[0] = 1 since XA will be 1 by 1
        if (n[0] > 1) {
            n1 = n[0] - 1;
            for (i = 1; i <= n1; i++) {
                j1 = i + 1;
                for (j = j1; j <= n[0]; j++) {
                    s = 0.0;
                    v = 0.0;
                    for (L = 1; L <= m[0]; L++) {
                        s = s + X[i-1][L-1]*A[L-1][j-1];
                        v = v + X[j-1][L-1]*A[L-1][i-1];
                    } // for (L = 1; L <= m[0]; L++)
                    t1 = Math.abs(s-v);
                    if (t1 > tm[3]) {
                        tm[3] = t1;
                    }
                    ta[3] = ta[3] + t1;
                } // for (j = j1; j <= n[0]; j++)
            } // for (i = 1; i <= n1; i++)
            ta[3] = ta[3]/(n[0]*(n[0]-1)/2);
        } // if (n > 1)
        
        Preferences.debug("Test of penrose conditions\n");
        Preferences.debug("A is the input matrix, X is the inverse of A\n");
        
        // Compute norms in order to gauge relative error sizes
        ama = anorm(m, n, A, ma);
        Preferences.debug("Input matrix norm = " + ama + "\n");
        anx = anorm(n, m, X, ma);
        Preferences.debug("Inverse matrix norm = " + anx + "\n");
        
        Preferences.debug("Average Deviation       Maximum Deviation\n");
        t1 = ta[0]/ama;
        Preferences.debug("AXA=TEST=A Average deviation actual = " + ta[0] + "\n");
        Preferences.debug("AXA=TEST=A Average deviation normalized = " + t1 + "\n");
        t2 = tm[0]/ama;
        Preferences.debug("AXA=TEST=A Maximum deviation actual = " + tm[0] + "\n");
        Preferences.debug("AXA=TEST=A Maximum deviation normalized = " + t2 + "\n");
        t1 = ta[1]/anx;
        Preferences.debug("XAX=TEST=X Average deviation actual = " + ta[1] + "\n");
        Preferences.debug("XAX=TEST=X Average deviation normalized = " + t1 + "\n");
        t2 = tm[1]/anx;
        Preferences.debug("XAX=TEST=X Maximum deviation actual = " + tm[1] + "\n");
        Preferences.debug("XAX=TEST=X Maximum deviation normalized = " + t2 + "\n");
        t1 = ta[2]/(ama*anx);
        Preferences.debug("(AX)T=TEST=AX Average deviation actual = " + ta[2] + "\n");
        Preferences.debug("(AX)T=TEST=AX Average deviation normalized = " + t1 + "\n");
        t2 = tm[2]/(ama*anx);
        Preferences.debug("(AX)T=TEST=AX Maximum deviation actual = " + tm[2] + "\n");
        Preferences.debug("(AX)T=TEST=AX Maximum deviation normalized = " + t2 + "\n");
        t1 = ta[3]/(ama*anx);
        Preferences.debug("(XA)T=TEST=XA Average deviation actual = " + ta[3] + "\n");
        Preferences.debug("(XA)T=TEST=XA Average deviation normalized = " + t1 + "\n");
        t2 = tm[3]/(ama*anx);
        Preferences.debug("(XA)T=TEST=XA Maximum deviation actual = " + tm[3] + "\n");
        Preferences.debug("(XA)T=TEST=XA Maximum deviation normalized = " + t2 + "\n");
        return;
    } // ptst
    
    private double anorm(int m[], int n[], double A[][], int ma) {
        // Compute square (euclidean) norm of matrix A
        // norm = SQRT(SUM(A[i][j]**2), for i = 1, m[0], j = 1, n[0])
        // This norm is used for simplicity - others acceptable
        // m[0] = number of rows in matrix A
        // n[0] = number of columns in matrix A
        // A = subject matrix which is m[0] by n[0]
        // ma = first dimension of A
        // None of these arguments are altered by this routine
        int i;
        int j;
        double dnorm;
        
        dnorm = 0.0;
        for (i = 0; i < m[0]; i++) {
            for (j = 0; j < n[0]; j++) {
                dnorm = dnorm + A[i][j]*A[i][j];
            } // for (j = 0; j < n[0]; j++)
        } // for (i = 0; i < m[0]; i++)
        dnorm = Math.sqrt(dnorm);
        return dnorm;
    } // anorm
    
    private void zielke(int m[], int n[], double A[][], int ma, int na, int mopt, double alpha, boolean fail[]) {
        // Computes generalized inverse test matrices due to Zielke, Signum Newsletter, Vol. 13, #4, Dec. 78,
        // pages 10 - 12 and Zielke, G., Signum Newsletter, Vol. 16, #3, Sept. 81, pages 7-8.
        // Note corrections in Signum Newsletter, Vol. 16, #4, Dec. 81, page 6.
        // These matrices are labelled A1, A2, A3, A4 or their generalized inverses A1+, A2+, A3+, A4+.
        // m[0] = number of rows in matrix A produced
        // This is set (changed) by this subroutine
        // n[0] = number of columns in matrix A produces
        // This is set (changed) by this subroutine
        // A - The matrix which is generated
        // ma - The first or row dimension of A in the calling program
        // Unchanged by this routine
        // na - The second or column dimension of A in the calling program.=
        // Unchanged by this routine
        // mopt - An integer used to select the matrix to be generated
        // Unchanged by this routine
        // The following table determines the possible matrices which may be generated.
        // mopt = - 4 yields Zielke/RutiShauser A4+ matrix (The Moore-Penrose inverse of A4)
        // mopt = -3 yields Zielke A3+ matrix
        // mopt = -2 yields Zielke A2+ matrix
        // mopt = -1 yields Zielke A1+ matrix
        // mopt = 1 yields Zielke A1 matrix
        // mopt = 2 yields Zielke A2 matrix
        // mopt = 3 yields Zielke A3 matrix
        // mopt = 4 yields Zielke/RutiShauser A4 matrix
        // alpha = parameter used in generating the Zielke matrices
        // Unchanged by this routine
        // fail[0] = failure flag set true on failure.
        // false otherwise
        
        // The declared dimensions must be large enough to hold the resuling matrices.
        // The following table is useful:
        //                     mopt                min. ma            min. na
        //                       1                    5                  4
        //                       2                    5                  4
        //                       3                    5                  4
        //                       4                    7                  3
        //                      -1                    4                  5
        //                      -2                    4                  5
        //                      -3                    4                  5
        //                      -4                    3                  7
        int i;
        int j;
        int nopt;
        
        // Initially failure flag indicates successful operation
        fail[0] = false;
        
        // Use the value of mopt to determine which input matrix is desired
        nopt = mopt + 5;
        
        // Safety check
        if ((nopt < 1) || (nopt > 9) || (nopt == 5)) {
            System.out.println("Error mopt = " + mopt + " is not available for zielke");
            Preferences.debug("Error mopt = " + mopt + " is not available for zielke\n");
            fail[0] = true;
            return;
        } // if ((nopt < 1) || (nopt > 9) || (nopt == 5))
        switch (nopt) {
            case 1:
                // Compute Zielke Rutishauser matrix A4+
                if (ma < 3) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 7) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                A[0][0] = -(9.0*alpha+63.0)/168.0;
                A[0][1] = -(6.0*alpha-46.0)/168.0;
                A[0][2] = -(3.0*alpha-29.0)/168.0;
                A[0][3] = -12.0/168.0;
                A[0][4] = (3.0*alpha+5.0)/168.0;
                A[0][5] = (6.0*alpha+22.0)/168.0;
                A[0][6] = (9.0*alpha+39.0)/168.0;
                A[1][0] = -6.0/168.0;
                A[1][1] = -4.0/168.0;
                A[1][2] = -2.0/168.0;
                A[1][3] = 0.0;
                A[1][4] = 2.0/168.0;
                A[1][5] = 4.0/168.0;
                A[1][6] = 6.0/168.0;
                A[2][0] = (9.0*alpha+51.0)/168.0;
                A[2][1] = (6.0*alpha+38.0)/168.0;
                A[2][2] = (3.0*alpha+25.0)/168.0;
                A[2][3] = 12.0/168.0;
                A[2][4] = -(3.0*alpha+1.0)/168.0;
                A[2][5] = -(6.0*alpha+14.0)/168.0;
                A[2][6] = -(9.0*alpha+27.0)/168.0;
                m[0] = 3;
                n[0] = 7;
                Preferences.debug("Zielke/Rutishauser matrix A4+\n");
                break;
            case 2:
                // Compute Zielke matrix A3+
                if (ma < 4) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 5) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                A[0][0] = 0.5;
                A[0][1] = -0.125;
                A[0][2] = -1.0;
                A[0][3] = 0.875;
                A[0][4] = -0.625;
                A[0][5] = 0.375;
                A[1][0] = -1.0;
                A[1][1] = (2.0*alpha+13.0)/8.0;
                A[1][2] = (-8.0*alpha-28.0)/8.0;
                A[1][3] = (6.0*alpha+17.0)/8.0;
                A[1][4] = (-2.0*alpha-3.0)/8.0;
                A[1][5] = (2.0*alpha+1.0)/8.0;
                A[2][0] = 1.25;
                A[2][1] = -A[1][1] + 0.25;
                A[2][2] = -A[1][2] - 1.25;
                A[2][3] = -A[1][3] + 1.0;
                A[2][4] = -A[1][4] - 0.5;
                A[2][5] = -A[1][5] + 0.25;
                A[3][0] = -0.25;
                A[3][1] = 0.375;
                A[3][2] = -0.25;
                A[3][3] = 0.125;
                A[3][4] = 0.125;
                A[3][5] = -0.125;
                A[4][0] = -0.5;
                A[4][1] = -0.25;
                A[4][2] = 1.5;
                A[4][3] = -1.25;
                A[4][4] = 0.75;
                A[4][5] = -0.25;
                m[0] = 5;
                n[0] = 6;
                Preferences.debug("Zielke matrix A3+\n");
                break;
            case 3:
                // Compute Zielke matrix A2+
                if (ma < 4) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 5) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                A[0][0] = (12.0*alpha+44.0)/60.0;
                A[0][1] = 1.0/3.0;
                A[0][2] = (-12.0*alpha-4.0)/60.0;
                A[0][3] = (-6.0*alpha-27.0)/60.0;
                A[0][4] = (6.0*alpha-3.0)/60.0;
                A[1][0] = -A[0][0] - 0.2;
                A[1][1] = -A[0][1];
                A[1][2] = -A[0][2] + 0.2;
                A[1][3] = -A[0][3] + 0.1;
                A[1][4] = -A[0][4] - 0.1;
                A[2][0] = A[1][0] + 11.0/15.0;
                A[2][1] = 0.0;
                A[2][2] = A[1][2] - 1.0/15.0;
                A[2][3] = A[1][3] - 0.7;
                A[2][4] = A[1][4] - 0.3;
                A[3][0] = alpha/5.0;
                A[3][1] = 0.0;
                A[3][2] = -alpha/5.0;
                A[3][3] = -A[2][3] + 0.1;
                A[3][4] = -A[2][4] - 0.1;
                m[0] = 4;
                n[0] = 5;
                Preferences.debug("Zielke matrix A2+\n");
                break;
            case 4:
                // Compute Zielke matrix A1+
                if (ma < 4) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 5) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                A[0][0] = alpha/2.0;
                A[0][1] = 0.5;
                A[0][2] = alpha/2.0;
                A[0][3] = 0.5;
                A[0][4] = -alpha;
                A[1][0] = 0.0;
                A[1][1] = -0.25;
                A[1][2] = 0.0;
                A[1][3] = -0.25;
                A[1][4] = 0.5;
                A[2][0] = -2.0*(alpha+1.0)/4.0;
                A[2][1] = 0.0;
                A[2][2] = A[2][0];
                A[2][3] = 0.0;
                A[2][4] = alpha;
                A[3][0] = 0.0;
                A[3][1] = -0.25;
                A[3][2] = 0.0;
                A[3][3] = -0.25;
                A[3][4] = 0.5;
                m[0] = 4;
                n[0] = 5;
                Preferences.debug("Zielke matrix A1+\n");
                break;
            case 6:
                // Compute Zielke matrix A1
                if (ma < 5) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 4) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                A[0][0] = alpha;
                A[0][1] = alpha;
                A[0][2] = alpha - 1.0;
                A[0][3] = alpha;
                A[1][0] = alpha + 1.0;
                A[1][1] = alpha;
                A[1][2] = alpha;
                A[1][3] = alpha;
                A[2][0] = alpha;
                A[2][1] = alpha;
                A[2][2] = alpha - 1.0;
                A[2][3] = alpha;
                A[3][0] = alpha + 1.0;
                A[3][1] = alpha;
                A[3][2] = alpha;
                A[3][3] = alpha;
                A[4][0] = alpha + 1.0;
                A[4][1] = alpha + 1.0;
                A[4][2] = alpha;
                A[4][3] = alpha + 1.0;
                m[0] = 5;
                n[0] = 4;
                Preferences.debug("Zielke matrix A1\n");
                break;
            case 7:
                // Compute Zielke matrix A2
                if (ma < 5) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 4) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                A[0][0] = alpha + 1.0;
                A[0][1] = alpha;
                A[0][2] = alpha;
                A[0][3] = alpha + 1.0;
                for (i = 0; i < 4; i++) {
                    A[1][i] = A[0][i] + 1.0;
                }
                for (i = 0; i < 4; i++) {
                    A[2][i] = A[1][i] + 1.0;
                }
                A[3][0] = alpha + 1.0;
                A[3][1] = alpha + 1.0;
                A[3][2] = alpha;
                A[3][3] = alpha + 2.0;
                for (i = 0; i < 4; i++) {
                    A[4][i] = A[3][i] - 1.0;
                }
                m[0] = 5;
                n[0] = 4;
                Preferences.debug("Zielke matrix A2\n");
                break;
            case 8:
                // Compute Zielke matrix A3
                if (ma < 6) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 5) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                A[0][0] = alpha;
                A[0][1] = alpha + 1.0;
                A[0][2] = alpha + 2.0;
                A[0][3] = alpha + 3.0;
                A[0][4] = alpha;
                A[1][0] = alpha;
                A[1][1] = alpha + 2.0;
                A[1][2] = alpha + 3.0;
                A[1][3] = alpha + 5.0;
                A[1][4] = alpha + 1.0;
                for (i = 1; i <= 4; i++) {
                    A[2][i-1] = alpha + i;
                }
                A[2][4] = alpha + 2.0;
                for (i = 0; i < 5; i++) {
                    A[3][i] = A[2][i] + 1.0;    
                }
                for (i = 0; i < 4; i++) {
                    A[4][i] = A[3][i] + 1.0;
                }
                A[4][4] = alpha + 5.0;
                A[5][0] = alpha + 5.0;
                A[5][1] = alpha + 5.0;
                A[5][2] = alpha + 6.0;
                A[5][3] = alpha + 6.0;
                A[5][4] = alpha + 7.0;
                m[0] = 6;
                n[0] = 5;
                Preferences.debug("Zielke matrix A3\n");
                break;
            case 9:
                // Compute Zielke/Rutishauser matrix A4
                if (ma < 7) {
                    System.out.println("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids ma = " + ma + " in zielke\n");
                    fail[0] = true;
                    return;    
                }
                if (na < 3) {
                    System.out.println("Error mopt = " + mopt + " forbids na = " + na + " in zielke");
                    Preferences.debug("Error mopt = " + mopt + " forbids na = " + na + " in zielke\n");
                    fail[0] = true;
                    return;      
                }
                for (i = 1; i <= 7; i++) {
                    for (j = 1; j <= 3; j++) {
                        A[i-1][j-1] = alpha + i + j - 1.0;
                    }
                }
                m[0] = 7;
                n[0] = 3;
                Preferences.debug("Zielke/Rutishauser matrix A4\n");
                break;
        } // switch (nopt)
        Preferences.debug("A =\n");
        for (i = 0; i < m[0]; i++) {
            Preferences.debug("Row = " + i + " ");
            for (j = 0; j < n[0]; j ++) {
                Preferences.debug(A[i][j] + " ");
            }
            Preferences.debug("\n");
        }
        return;
    } // zielke
}