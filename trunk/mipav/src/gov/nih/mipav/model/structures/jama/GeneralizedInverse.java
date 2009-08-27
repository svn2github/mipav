package gov.nih.mipav.model.structures.jama;


import gov.nih.mipav.view.*;

public class GeneralizedInverse {
    private double A[][];
    
    // Number of rows in A
    private int NR;
    // Number of columns in A
    private int NC;
    
    // Double precision routine variables found in routine dlamch
    private double base;
    private double emax;
    private double emin;
    private double eps;
    private boolean first = true;
    private double prec;
    private double rmax;
    private double rmin;
    private double rnd;
    private double sfmin;
    private double t;
    
    public GeneralizedInverse() {
        
    }
    
    public GeneralizedInverse(double A1[][], int NR, int NC) {
        int i;
        int j;
        this.NR = NR;
        this.NC = NC;
        A = new double[NR][NC];
        for (i = 0; i < NR; i++) {
            for (j = 0; j < NC; j++) {
                A[i][j] = A1[i][j];
            }
        }
    }
    
    // This is a port of the FORTRAN suborutine GINV2, A Simple Algorithm for Computing the Generalized Inverse
    // of a Matrix by B. Rust, W. R. Burrus, and C. Schneeberger, CACM 9(5): 381-387 (May, 1966)
    public double[][] ginv() {
        // Note there were 2 errors in file http://ftp.aset.psu.edu/pub/ger/fortran/hdk/ginv.for
        // Line 55   DO 60 I = I, JM1 should be 55    DO 60 I = 1, JM1
        // Line DO 65 K = I, JM1 should be DO 65 K = 1, JM1
        int i;
        double Ainv[][] = new double[NC][NR];
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
        
        // Dependent column tolerance for n bit floating point fraction
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
                    for (k = 1; k <= jm1; k++) {
                        A[i-1][j-1] = A[i-1][j-1] - A[i-1][k-1]*atemp[k-1]*aflag[k-1];    
                    } // for (k = 1; k <= jm1; k++)
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
    
    // This is a port of algorithm 645, a program for testing generalized inverse subroutines.
    // ALGORITHM 645 COLLECTED ALGORITHMS FROM ACM.
    // ALGORITHM APPEARED IN ACM-TRANS. MATH. SOFTWARE, VOL. 12, NO. 3, SEPT., 1986, P. 274
    // ORIGINAL CODE BY J. C. NASH 1979, J. C. NASH AND C. E. GRATTON 1982
    // J. C. NASH 1983, 1984
    // J. C. NASH AND R. L. C. WANG 1985, 1986
    public void ginvTest() {
        int m[] = new int[1];
        int n[] = new int[1];
        int ma;
        int na;
        int nb;
        int k[] = new int[1];
        int mopt;
        int iseed[] = new int[1];
        double alpha[] = new double[1];
        double Ag[][] = new double[30][30];
        double X[][] = new double[30][30];
        double C[][] = new double[30][30];
        double B[][] = new double[30][30];
        double G[][] = new double[30][30];
        double ta[] = new double[4];
        double tm[] = new double[4];
        boolean fail[] = new boolean[1];
        int i;
        int j;
        int test;
        int nTests = 22;
        String message[] = new String[]{"Testing invalid option A9+", "Testing iseed[0] < 0 in gmatx",
                "Testing m[0] < 1 in gmatx", "Testing alpha[0] < 0.0 in gmatx",
                "Testing k[0] < 0 in gmatx", "Testing k > min(m[0],n[0]) in gmatx",
                "Testing n[0] > m[0] in gmatx, ginvse not called",
                "Test no error data set which generates identity matrix",
                "Test 1 by 5 row vector", "Test 5 by 1 column vector",
                "Test 1 by 1 trivial case", "Test 5 by 10 matrix with power of 2 singular values",
                "Test 10 by 5 generated matrix with rank 2",
                "Test 5 by 10 generated matrix -- power of 0.1, rank 3",
                "Test Zielke A1", "Test Zielke A2", "Test Zielke A3",
                "Test Zielke / Rutishauser A4", "Test Zielke inverse A1",
                "Test Zielke inverse A2", "Test Zielke inverse A3",
                "Test Zielke / Rutishauser A4 inverse"};
        
        int mVar[] = new int[]{0,3,-1,4,5,5,3,4,1,5,1,10,10,5,5,5,5,5,5,5,5,5};
        int nVar[] = new int[]{0,3,1,4,5,3,4,4,5,1,1,5,5,10,5,5,5,5,5,5,5,5};
        int kVar[] = new int[]{0,2,1,4,-1,8,1,4,1,1,1,5,2,3,5,5,5,5,5,5,5,5};
        int moptVar[] = new int[]{-9,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,3,4,-1,-2,-3,-4};
        int iseedVar[] = new int[]{0,-1,34,56,98,1,0,453,12345,12345,12345,12345,54321,
                                   34521,0,0,0,0,0,0,0,0};
        double alphaVar[] = new double[]{2.0,0.2,0.1,-1.8,0.09,1.0,1.0,1.0,0.5,0.5,0.5,
                                         0.5,0.5,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1};
        // From Chapter 8 Generalized Inverses of Matrix Algebra Useful for Statistics by
        // Shayle R. Searle:
        //  Given any matrix A, there is a unique matrix M such that (1):
        //  (i) AMA = A
        //  (ii) MAM = M
        //  (iii) AM is symmetric
        //  (iv) MA is symmetric
        // The matrix M defined by the four Penrose conditions in (1) is unique for a given A.
        // But there are many matrices G which satisfy just the first Penrose condition:
        // AGA = A (5)
        // Nevertheless, they are of such importance in solving linear equations that we direct
        // most attention to those matrices G rather than to the Moore-Penorose inverse M.
        
        //   Any matrix G satisfying (5) is called a generalized inverse of A; and, by (5), when A
        // is p by q then G is q by p.  Although the name generalized inverse has not been adopted
        // universally, it is widely used.  Notice that G is "a" generalized inverse of A and not
        // "the" generalized inverse, because for any given A there are generally many matrices G
        // satisfying (5).  The exception is when A is nonsingular, in which case there is only one
        // G satisfying (5) and it is the regular inverse G = (A)-1 = M.  A useful alternative symbol
        // for G satisfying AGA = A is A with a superscript -.
        
        // Test ginvse routine that comes with the tester package.
        // Note.  ginvse is not designed to produce a 'good' generalized inverse.
        // It is meant only to furnish test data for routines gmatx, zielke, and ptst.
        // All 4 Penrose tests are failed.
        boolean doginvse = false;
        // Test ginv routine. 
        // All 4 Penrose tests passed
        boolean doginv = true;
        double tol = Math.pow(16.0, -5.0);
        
        // The following sequence of tests is used to test error traps in ginvse, ptst, and zielke
        // This section can be omitted withot affecting computations
        Preferences.debug("Test traps for calling sequence errors\n");
        Preferences.debug("Provoke 11 errors reports for dimensions and invalid arguments\n");
        
        // Test trap for matrix row dimension less than 1
        // Testing ma < 1
        ma = 0;
        na = 10;
        m[0] = 10;
        n[0] = 10;
        ptst(m, n, Ag, ma, X, na, C, ma, ta, tm, fail);
        // Ought to fail, stop if it has not failed
        if (!fail[0]) {
            return;
        }
        ginvse(G, ma, na, X, ma, m, n, tol, C, ma, fail);
        if (!fail[0]) {
            return;
        }
        
        // Test trap for matrix column dimension less than 1
        // Testing na < 1
        ma = 11;
        na = 0;
        m[0] = 11;
        n[0] = 11;
        ptst(m, n, Ag, ma, X, na, C, ma, ta, tm, fail);
        // Ought to fail, stop if it has not failed
        if (!fail[0]) {
            return;
        }
        ginvse(G, ma, na, X, ma, m, n, tol, C, ma, fail);
        if (!fail[0]) {
            return;
        }
        
        // Test trap for matrix row order less than 1
        // Testing m[0] < 1
        ma = 12;
        na = 12;
        m[0] = 0;
        n[0] = 12;
        ptst(m, n, Ag, ma, X, na, C, ma, ta, tm, fail);
       // Ought to fail, stop if it has not failed
        if (!fail[0]) {
            return;
        }
        ginvse(G, ma, na, X, ma, m, n, tol, C, ma, fail);
        if (!fail[0]) {
            return;
        }
        
        // Test trap for matrix column order less than 1
        // Testing n[0] < 1
        ma = 13;
        na = 13;
        m[0] = 13;
        n[0] = 0;
        ptst(m, n, Ag, ma, X, na, C, ma, ta, tm, fail);
       // Ought to fail, stop if it has not failed
        if (!fail[0]) {
            return;
        }
        ginvse(G, ma, na, X, ma, m, n, tol, C, ma, fail);
        if (!fail[0]) {
            return;
        }
        
        // Test trap for number of rows greater than dimension
        // Testing m[0] > ma
        ma = 5;
        na = 5;
        m[0] = 6;
        n[0] = 5;
        ginvse(G, ma, na, X, ma, m, n, tol, C, ma, fail);
        // Ought to fail, stop if it has not failed
        if (!fail[0]) {
            return;
        }
        
        // Test trap fornumber of columns greater than dimension
        // Testing n[0] > na
        ma = 5;
        na = 5;
        m[0] = 5;
        n[0] = 7;
        ginvse(G, ma, na, X, ma, m, n, tol, C, ma, fail);
        // Ought to fail, stop if it has not failed
        if (!fail[0]) {
            return;
        }
        
        // Testing ma and na forbidden by zielke
        ma = 3;
        na = 3;
        m[0] = 3;
        n[0] = 3;
        mopt = 2;
        zielke(m, n, Ag, ma, na, mopt, alpha, fail);
        // Ought to fail, stop if it has not failed
        if (!fail[0]) {
            return;
        }
        
        // End of sequence of calls to test error traps
        Preferences.debug("End of calling sequence for error tests\n");
        
        // The following tests are designed to ensure that the routine ptst is performing correctly.
        // First set up the matrix and a supposed inverse.
        m[0] = 3;
        n[0] = 2;
        Ag[0][0] = 1.0;
        Ag[0][1] = 2.0;
        Ag[1][0] = 2.0;
        Ag[1][1] = -3.0;
        Ag[2][0] = 0.5;
        Ag[2][1] = 0.0;
        
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
        Preferences.debug("Ag = " + "\n");
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 2; j++) {
                Preferences.debug(Ag[i][j] + " ");    
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
        ptst(m, n, Ag, ma, X, na, C, ma, ta, tm, fail);
        
        // Approximate results
        // Test PTST with 3 by 2 matrices to verify matrix multiplications
        //                 Ag                       X
        //              1.0   2.0           0.1   0.2   -0.2
        //              2.0  -3.0           0.3  -0.3    0.9
        //              0.5   0.0
        //  Test Penrose Conditions
        //  Ag is the input matrix, X is the inverse of Ag
        //  Input matrix norm     = 4.27200100E00
        //  Inverse matrix norm =   1.03923000E00
        //                       Average Deviation       Maximum Deviation
        // AgXAg=TEST=Ag ACTUAL        0.891666                 2.3
        //            NORMALIZED       0.2087232                0.5383886
        // XAgX=TEST=X ACTUAL          0.1875                   0.42
        //            NORMALIZED       0.1804217                0.4041449
        // (AgX)T=TEST=AgX ACTUAL      0.1683332                0.32
        //               NORMALIZED    0.3791636                0.7207864
        // (XAg)T=TEST=XAg ACTUAL      0.55                     0.55
        //               NORMALIZED    0.1238851                0.1238851
        
        // Reverse call to test row size < column size case
        Preferences.debug("Same test on ptst with Ag and X interchanged\n");
        ptst(n, m, X, na, Ag, ma, C, ma, ta, tm, fail);
        
        // Test row and column vector problems
        // Matrix (column vector) Ag
        Ag[0][0] = 3.0;
        Ag[1][0] = 4.0;
        
        // Matrix (row vector) X
        X[0][0] = 3.0/25.0;
        X[0][1] = 4.0/25.0;
        
        m[0] = 2;
        n[0] = 1;
        Preferences.debug("Test ptst with row and column vectors\n");
        Preferences.debug("Ag = " + "\n");
        Preferences.debug(Ag[0][0] + "\n");
        Preferences.debug(Ag[1][0] + "\n");
        Preferences.debug("X = " + "\n");
        Preferences.debug(X[0][0] + " " + X[0][1] + "\n");
        ptst(m, n, Ag, ma, X, na, C, ma, ta, tm, fail);
        
        // Reverse call to test row size < column size case
        Preferences.debug("Same vector test on ptst with Ag and X interchanged\n");
        ptst(n, m, X, na, Ag, ma, C, ma, ta, tm, fail);
        
        // Test trivial case (1 by 1)
        Ag[0][0] = 2.0;
        X[0][0] = 0.5;
        m[0] = 1;
        n[0] = 1;
        Preferences.debug("Test ptst with trivial 1 by 1 matrix Ag = 2.0 and matrix X = 0.5\n");
        ptst(n, m, X, na, Ag, ma, C, ma, ta, tm, fail);
        
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
            
            // m[0] and n[0] give the size of the matrix to be generated by gmatx
            // k[0] gives its rank
            // mopt = 0 if gmatx is to be called
            // mopt = 1, 2, 3, 4 for Zielke matrices
            //      = -1, -2, -3, -4 for their Moore-Penrose inverses
            // iseed[0] = an integer seed for use by the random number generator called by gmatx
            // alpha[0] = a parameter used to adjust the singular values of the matrix generated by gmatx
            
            // For details of the controls, see the comments in the routines gmatx and zielke
            Preferences.debug("Test number = " + (test + 1) + "\n");
            Preferences.debug(message[test] + "\n");
            // For each test set m, n, k, mopt, iseed, alpha
            m[0] = mVar[test];
            n[0] = nVar[test];
            k[0] = kVar[test];
            mopt = moptVar[test];
            iseed[0] = iseedVar[test];
            alpha[0] = alphaVar[test];
            
            // initialize error flag to imply correct execution
            fail[0] = false;
            
            Preferences.debug("m[0] = " + m[0] + "\n");
            Preferences.debug("n[0] = " + n[0] + "\n");
            Preferences.debug("k[0] = " + k[0] + "\n");
            Preferences.debug("mopt = " + mopt + "\n");
            Preferences.debug("iseed[0] = " + iseed[0] + "\n");
            Preferences.debug("alpha[0] = " + alpha[0] + "\n");
            
            // Other tests for valid inputs are made in the routines
            
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
                // Note that m[0] and n[0] are replaced by appropriate values.
                // mopt is the Zielke matrix selected
                // alpha is needed in zielke as a parameter in formulas for the matrix elements generated.
                zielke(m, n, Ag, ma, na, mopt, alpha, fail);
                if (fail[0]) {
                    continue;
                }
            } // if (mopt != 0)
            else { // mopt == 0
                // Using generated matrix
                // We suggest values of alpha to lie within 0.10 and 10.0, but other values of alpha
                // will be tolerated.  Note that alpha cannot be equal to zero, and if so will be set
                // to one in gmatx
                gmatx(m, n, Ag, ma, na, B, nb, alpha, k, iseed, fail);
                if (fail[0]) {
                    continue;
                }
            } // else mopt == 0
            
            // Add a different call for each generalized inverse routine tested
            
            if (doginv) {
                NR = m[0];
                NC = n[0];
                A = new double[NR][NC];
                for (i = 0; i < NR; i++) {
                    for (j = 0; j < NC; j++) {
                        A[i][j] = Ag[i][j];
                    }
                } 
                
                A = ginv();
                for (i = 0; i < NC; i++) {
                    for (j = 0; j < NR; j++) {
                        X[i][j] = A[i][j];
                    }
                } 
            } // if (doginv)
            
            if (doginvse) {
                // ginvse is not designed to handle more columns than rows 
                // that is, n[0] > m[0], so avoid call in such cases
                
                if (n[0] > m[0]) {
                    Preferences.debug("ginvse not called because n[0] = " + n[0] + " > m[0] = " + m[0] + "\n");
                    continue;
                }
                
                // ginvse is also unable to handle column vectors, that is an m[0] by 1 matrix Ag
                if (n[0] < 2) {
                    continue;
                }
                
                // Note additional statements to get conformity with test program
                // Copy matrix to avoid overwriting it
                for (i = 0; i < m[0]; i++) {
                    for (j = 0; j < n[0]; j++) {
                        G[i][j] = Ag[i][j];    
                    }
                } // for (i = 0; i < m[0]; i++)
                
                // IBM single precision
                tol = Math.pow(16.0, -5.0);
                ginvse(G, ma, na, X, ma, m, n, tol, C, ma, fail);
                
                if (fail[0]) {
                    continue;
                }
            } // if (doginvse)
            
            // Test penrose conditions
            ptst(m, n, Ag, ma, X, ma, C, ma, ta, tm, fail);
        } // for (test = 0; test < nTests; test++)
    } // ginvTest
    
    private void ptst(int m[], int n[], double A[][], int ma, double X[][], int nx, double C[][], 
                      int nc, double ta[], double tm[], boolean fail[]) {
        // This subroutine is designed to test a proposed generalized inverse of a matrix labelled A,
        // which is m by n.  X is the n by m matrix containing the supposed inverse.
        // Program originally for square, symmetric matrices written by Richard L. C. Wang, 1977
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
        // norm = SQRT(SUM(A[i-1][j-1]**2), for i = 1, m[0], j = 1, n[0])
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
        if (m[0]<= 0) {
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
        // norm = SQRT(SUM(A[i-1][j-1]**2), for i = 1, m[0], j = 1, n[0])
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
    
    private void zielke(int m[], int n[], double A[][], int ma, int na, int mopt, double alpha[], boolean fail[]) {
        // Computes generalized inverse test matrices due to Zielke, Signum Newsletter, Vol. 13, #4, Dec. 78,
        // pages 10 - 12 and Zielke, G., Signum Newsletter, Vol. 16, #3, Sept. 81, pages 7-8.
        // Note corrections in Signum Newsletter, Vol. 16, #4, Dec. 81, page 6.
        // These matrices are labelled A1, A2, A3, A4 or their generalized inverses A1+, A2+, A3+, A4+.
        // m[0] = number of rows in matrix A produced
        // This is set (changed) by this subroutine
        // n[0] = number of columns in matrix A produced
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
                A[0][0] = -(9.0*alpha[0]+63.0)/168.0;
                A[0][1] = -(6.0*alpha[0]-46.0)/168.0;
                A[0][2] = -(3.0*alpha[0]-29.0)/168.0;
                A[0][3] = -12.0/168.0;
                A[0][4] = (3.0*alpha[0]+5.0)/168.0;
                A[0][5] = (6.0*alpha[0]+22.0)/168.0;
                A[0][6] = (9.0*alpha[0]+39.0)/168.0;
                A[1][0] = -6.0/168.0;
                A[1][1] = -4.0/168.0;
                A[1][2] = -2.0/168.0;
                A[1][3] = 0.0;
                A[1][4] = 2.0/168.0;
                A[1][5] = 4.0/168.0;
                A[1][6] = 6.0/168.0;
                A[2][0] = (9.0*alpha[0]+51.0)/168.0;
                A[2][1] = (6.0*alpha[0]+38.0)/168.0;
                A[2][2] = (3.0*alpha[0]+25.0)/168.0;
                A[2][3] = 12.0/168.0;
                A[2][4] = -(3.0*alpha[0]+1.0)/168.0;
                A[2][5] = -(6.0*alpha[0]+14.0)/168.0;
                A[2][6] = -(9.0*alpha[0]+27.0)/168.0;
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
                A[1][1] = (2.0*alpha[0]+13.0)/8.0;
                A[1][2] = (-8.0*alpha[0]-28.0)/8.0;
                A[1][3] = (6.0*alpha[0]+17.0)/8.0;
                A[1][4] = (-2.0*alpha[0]-3.0)/8.0;
                A[1][5] = (2.0*alpha[0]+1.0)/8.0;
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
                A[0][0] = (12.0*alpha[0]+44.0)/60.0;
                A[0][1] = 1.0/3.0;
                A[0][2] = (-12.0*alpha[0]-4.0)/60.0;
                A[0][3] = (-6.0*alpha[0]-27.0)/60.0;
                A[0][4] = (6.0*alpha[0]-3.0)/60.0;
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
                A[3][0] = alpha[0]/5.0;
                A[3][1] = 0.0;
                A[3][2] = -alpha[0]/5.0;
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
                A[0][0] = alpha[0]/2.0;
                A[0][1] = 0.5;
                A[0][2] = alpha[0]/2.0;
                A[0][3] = 0.5;
                A[0][4] = -alpha[0];
                A[1][0] = 0.0;
                A[1][1] = -0.25;
                A[1][2] = 0.0;
                A[1][3] = -0.25;
                A[1][4] = 0.5;
                A[2][0] = -2.0*(alpha[0]+1.0)/4.0;
                A[2][1] = 0.0;
                A[2][2] = A[2][0];
                A[2][3] = 0.0;
                A[2][4] = alpha[0];
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
                A[0][0] = alpha[0];
                A[0][1] = alpha[0];
                A[0][2] = alpha[0] - 1.0;
                A[0][3] = alpha[0];
                A[1][0] = alpha[0] + 1.0;
                A[1][1] = alpha[0];
                A[1][2] = alpha[0];
                A[1][3] = alpha[0];
                A[2][0] = alpha[0];
                A[2][1] = alpha[0];
                A[2][2] = alpha[0] - 1.0;
                A[2][3] = alpha[0];
                A[3][0] = alpha[0] + 1.0;
                A[3][1] = alpha[0];
                A[3][2] = alpha[0];
                A[3][3] = alpha[0];
                A[4][0] = alpha[0] + 1.0;
                A[4][1] = alpha[0] + 1.0;
                A[4][2] = alpha[0];
                A[4][3] = alpha[0] + 1.0;
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
                A[0][0] = alpha[0] + 1.0;
                A[0][1] = alpha[0];
                A[0][2] = alpha[0];
                A[0][3] = alpha[0] + 1.0;
                for (i = 0; i < 4; i++) {
                    A[1][i] = A[0][i] + 1.0;
                }
                for (i = 0; i < 4; i++) {
                    A[2][i] = A[1][i] + 1.0;
                }
                A[3][0] = alpha[0] + 1.0;
                A[3][1] = alpha[0] + 1.0;
                A[3][2] = alpha[0];
                A[3][3] = alpha[0] + 2.0;
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
                A[0][0] = alpha[0];
                A[0][1] = alpha[0] + 1.0;
                A[0][2] = alpha[0] + 2.0;
                A[0][3] = alpha[0] + 3.0;
                A[0][4] = alpha[0];
                A[1][0] = alpha[0];
                A[1][1] = alpha[0] + 2.0;
                A[1][2] = alpha[0] + 3.0;
                A[1][3] = alpha[0] + 5.0;
                A[1][4] = alpha[0] + 1.0;
                for (i = 1; i <= 4; i++) {
                    A[2][i-1] = alpha[0] + i;
                }
                A[2][4] = alpha[0] + 2.0;
                for (i = 0; i < 5; i++) {
                    A[3][i] = A[2][i] + 1.0;    
                }
                for (i = 0; i < 4; i++) {
                    A[4][i] = A[3][i] + 1.0;
                }
                A[4][4] = alpha[0] + 5.0;
                A[5][0] = alpha[0] + 5.0;
                A[5][1] = alpha[0] + 5.0;
                A[5][2] = alpha[0] + 6.0;
                A[5][3] = alpha[0] + 6.0;
                A[5][4] = alpha[0] + 7.0;
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
                        A[i-1][j-1] = alpha[0] + i + j - 1.0;
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
    
    private void gmatx(int m[], int n[], double A[][], int ma, int na, double B[][], int nb,
                       double alpha[], int k[], int iseed[], boolean fail[]) {
        // February 9, 1984, May 27, 1984, July 18, 1985
        // by J. C. Nash and R. L. C. Wang
        // Computes an m[0] by n[0] matrix - A - for use in testing programs purporting to calculate
        // the generalized inverse of a matrix
        
        // The matrix A, dimension ma by na but size m[0] by n[0], is to be calculated by a sequence
        // of pseudo-random Jacobi rotations applied to a 'diagonal' matrix whose elements are
        // determined by the parameters k[0] and alpha[0] as follows.  A[i-1][i-1] is set to 
        // alpha**(1 - i) for for i = 1,2,...,k[0] and to zero for i > k[0], where k[0] is a positive integer
        // no larger than the minimum value of m[0] and n[0]
        
        // m[0] = number of rows in matrix A
        // Normally unchanged by this routine
        // Altered during the execution of the routine if n[0] > m[0].
        // Then reset before return to the calling program.
        // m[0] is reset to ma if m[0] > ma on entry
        // n[0] = number of columns in matrix A
        // Normally unchanged by this routine
        // Altered during the execution of the routine if n[0] > m[0]
        // Then reset before return to the calling program.
        // No check is made to verfiy n[0] <= na
        // A = The matrix to be created in this routine
        // A must be declared with first dimension = ma and 
        // second dimension at least n[0] in the calling program.
        // ma = first or row dimension of A
        // ma should correspond to the first dimension of matrix A in the calling program.
        // Unchanged by this routine.
        // na = second or column dimension of matrix A
        // Should not be altered
        // B = working matrix in which test matrix is built
        // nb = first dimension of matrix B
        // Should be at least as large as largest dimension of matrix A
        // Not altered by this routine
        // alpha[0] = Factor used to generate singular values of matrix A.
        // These are generated according to the formula alpha**(1-i) for for = 1,2,...,k[0]
        // where k[0] is the rank (see below)
        // alpha[0] should lie in the interval (0.1, 10.0) to generate 'reasonable' singular
        // values, but other positive values are accepted without change.
        // If alpha[0] <= 0.0, it will be set to 1.0.
        // k[0] = the rank of the matrix to be generated
        // Must be positive and no larger than the minimum of m[0] and n[0].
        // If k[0] > min(m[0],n[0]), it will be set to min(m[0],n[0]) and a warning message
        // displayed.
        // Otherwise, k[0] is unaltered by this routine.
        // iseed[0] = seed for the pseudo-random number generator
        // Only positive values of iseed are allowed
        // If iseed[0] <= 0 it will be set to 1
        // iseed is converted to a double precision variable dseed for used in drand, the
        // generator routine.
        // dseed is altered by drand, and dseed must be supplied in every
        // call to drand.
        // Reference: Schrage, L., A more portable fortran random number generator, 
        // ACM Transactions on Mathematical Software, Vol. 5, No. 2, June, 1979, pages 132-138.
        // fail[0] = error flag
        // fail[0] = true if gmatx has failed to create matrix A
        // fail[0] = false if gmatx has created matrix A
        // Most failures are associated with the use of incorrect arguments for this routine.
        
        double dseed;
        int i;
        int im;
        int imax;
        int j;
        int j1;
        int L;
        int mn;
        int m1;
        int ipass;
        int jpass;
        double c;
        double s;
        double t;
        
        // Set flag initially to false (program O.K.)
        fail[0] = false;
        if (iseed[0] <= 0) {
            System.out.println(iseed[0] + " was an improper choice for iseed[0]");
            Preferences.debug(iseed[0] + " was an improper choice for iseed[0]\n");
            iseed[0] = 1;
            System.out.println("iseed[0] was set to 1 in gmatx");
            Preferences.debug("iseed[0] was set to 1 in gmatx\n");
        } // if (iseed[0] <= 0)
        
        // Tests on m[0], n[0], and ma
        
        // Test for first dimension too small
        if (ma < 1) {
            System.out.println("ma = " + ma + " is incorrectly less than 1");
            Preferences.debug("ma = " + ma + " is incorrectly less than 1\n");
            fail[0] = true;
            return;
        }
        // Test for invalid row or column size
        if (m[0] < 1) {
            System.out.println("m[0] = " + m[0] + " is incorrectly less than 1");
            Preferences.debug("m[0] = " + m[0] + " is incorrectly less than 1\n");
            fail[0] = true;
            return;    
        }
        if (n[0] < 1) {
            System.out.println("n[0] = " + n[0] + " is incorrectly less than 1");
            Preferences.debug("n[0] = " + n[0] + " is incorrectly less than 1\n");
            fail[0] = true;
            return;       
        }
        // Test for valid row size
        if (m[0] > ma) {
            System.out.println("m[0] = " + m[0] + " exceeded allowable value of ma = " + ma);
            Preferences.debug("m[0] = " + m[0] + " exceeded allowable value of ma = " + ma + "\n");
            m[0] = ma;
            System.out.println("m[0] was set to ma = " + ma + " in gmatx");
            Preferences.debug("m[0] was set to ma = " + ma + " in gmatx\n");
        }
        
        Preferences.debug("Generating matrix by pseudo-random Jacobi rotations\n");
        Preferences.debug("Order = " + m[0]+ " by " + n[0] + "\n");
        Preferences.debug("Pseudo-random number seed = " + iseed[0] + "\n");
        
        // Determine if A or A-transpose is to be generated by the Jacobi rotations
        // imax = 1 if A is generated
        // imax = 0 if A-transpose is generated
        imax = 1;
        mn = n[0];
        if (m[0] < n[0]) {
            imax = 0;
            im = m[0];
            m[0] = n[0];
            n[0] = im;
            mn = m[0];
        } // if (m[0] < n[0])
        
        for (i = 0; i < m[0]; i++) {
            for (j = 0; j < n[0]; j++) {
                B[i][j] = 0.0;
            }
        }
        
        // Replace (alpha[0] <= 0.0) by  1.0
        if (alpha[0] <= 0.0) {
            System.out.println("alpha[0] = " + alpha[0] + " is incorrectly <= 0.0");
            Preferences.debug("alpha[0] = " + alpha[0] + " is incorrectly <= 0.0\n");
            alpha[0] = 1.0;
            System.out.println("alpha[0] was set to 1.0 in gmatx");
            Preferences.debug("alpha[0] was set to 1.0 in gmatx\n");
        }
        
        // Test k[0] and loop, k[0] being the rank
        if (k[0] < 1) {
            System.out.println("k[0] = " + k[0] + " is an invalid value.  Terminating gmatx");
            Preferences.debug("k[0] = " + k[0] + " is an invalid value.  Terminating gmatx\n");
            fail[0] = true;
            return;
        }
        if (k[0] > mn) {
            System.out.println("k[0] = " + k[0] + " exceeds the allowable value = mn for k[0]");
            Preferences.debug("k[0] = " + k[0] + " exceeds the allowable value = mn for k[0]\n");
            k[0] = mn;
            System.out.println("k[0] has been set equal to mn = " + mn);
            Preferences.debug("k[0] has been set equal to mn = " + mn + "\n");
        }
        for (i = 1; i <= k[0]; i++) {
            B[i-1][i-1] = Math.pow(alpha[0], 1-i);
        }
        Preferences.debug("The initial diagonal elements for i = 1 to " + k[0] + " are:\n");
        for (i = 0; i < k[0]; i++) {
            Preferences.debug(B[i][i] + "\n");
        }
        m1 = m[0] - 1;
        
        // Set random number generator seed
        dseed = (double)iseed[0];
        
        // Perform Jacobi rotations (one sweep only)
        jpass = 1;
        
        // More sweeps can be performed as follows
        // jpass = 3;
        // For three sweeps
        for (ipass = 1; ipass <= jpass; ipass++) {
            // Avoid loop when trivial 1 by 1 matrix is to be generated
            if (m1 < 1) {
                continue;
            }
            for (i = 1; i <= m1; i++) {
                j1 = i + 1;
                for (j = j1; j <= m[0]; j++) {
                    // Get sine of angle of rotation from pseudo random number generator
                    s = drand(dseed);
                    // Compute cosine
                    c = Math.sqrt(1.0-s*s);
                    // rows
                    for (L = 1; L <= n[0]; L++) {
                        t = B[i-1][L-1];
                        B[i-1][L-1] = t*c + B[j-1][L-1]*s;
                        B[j-1][L-1] = -t*s + B[j-1][L-1]*c;
                    } // for (L = 1; L <= n[0]; L++)
                    
                    // Column transformations - these are omitted if the indices are too large
                    // (that is, if j > n[0])
                    // Since max(i) = j - 1, there is no test on i
                    if (j > n[0]) {
                        continue;
                    }
                    
                    for (L = 1; L <= m[0]; L++) {
                        t = B[L-1][i-1];
                        B[L-1][i-1] = t*c + B[L-1][j-1]*s;
                        B[L-1][j-1] = -t*s + B[L-1][j-1]*c;
                    } // for (L = 1; L <= m[0]; L++)
                } // for (j = j1; j <= m[0]; j++)
            } // for (i = 1; i <= m1; i++)
        } // for (ipass = 1; ipass <= jpass; ipass++)
        
        // Reset row and column sizes
        if (imax != 1) {
            im = m[0];
            m[0] = n[0];
            n[0] = im;
        } // if (imax != 1)
        
        // Move B or B-transpose to A
        for (i = 0; i < m[0]; i++) {
            for (j = 0; j < n[0]; j++) {
                if (imax == 0) {
                    A[i][j] = B[j][i];
                }
                if (imax == 1) {
                    A[i][j] = B[i][j];
                }
            } // for (j = 0; j < n[0]; j++)
        } // for (i = 0; i < m[0]; i++)
        
        // Output the matrix which has been generated
        Preferences.debug("Matrix A generated by gmatx:\n");
        for (i = 0; i < m[0]; i++) {
            Preferences.debug("Row " + i + " ");
            for (j = 0; j < n[0]; j++) {
                Preferences.debug(A[i][j] + " ");
            }
            Preferences.debug("\n");
        } // for (i = 0; i < m[0]; i++)
        return;
    } // gmatx
    
    private double drand(double dx) {
        // Portable random number generator using the recursion:
        // dx = dx * A mod P
        // From Linus Shrage, ACM Transactions on Mathematical Software, Vol. 5, No. 2, p.134 FF,
        // June, 1979
        // dx = seed for next member of pseudo-random sequence
        // dx should not be altered between calls to drand
        // dx is altered on each call to drand
        // ouput dpprn = double precision pseudo random number in the unit interval which is produced
        // by this routine
        
        double a = 16807.0;
        // 2**31 - 1
        double p = 2147483647.0;
        // 2**15
        double b15 = 32768.0;
        // 2**16
        double b16 = 65536.0;
        double xhi;
        double xalo;
        double leftlo;
        double fhi;
        double k;
        double dpprn;
        
        // Get 15 high order bits of dx
        xhi = dx/b16;
        xhi = Math.floor(xhi);
        
        // Get 16 lo bits of dx and form lo product
        xalo = (dx - xhi*b16)*a;
        
        // Get 15 hi order bits of lo product
        leftlo = xalo/b16;
        leftlo = Math.floor(leftlo);
        
        // Form the 31 highest bits of full product
        fhi = xhi*a + leftlo;
        
        // Get overflow past 31st bit of full product
        k = fhi/b15;
        k = Math.floor(k);
        
        // Assemble all the parts and presubtract p
        // The parentheses are essential
        dx = (((xalo-leftlo*b16)-p)+(fhi-k*b15)*b16) + k;
        
        // Add p back in if necessary
        if (dx < 0.0) {
            dx = dx + p;
        }
        
        // Multiply by 1/(2**31-1)
        dpprn = dx * 4.656612875E-10;
        return dpprn;
    } // drand
    
    private void ginvse(double A[][], int ma, int na, double AIN[][], int nain, int m[], int n[],
                        double tol, double V[][], int nv, boolean fail[]) {
        // Inverse of A = V * inverse of S * transpose of U
        // via singular value decomposition using algorithms 1 and 2 of
        // Nash, J. C., Compact Numerical Methods for Computers, 1979,
        // Adam Hilger, Bristol or Halsted Press, N.Y.
        
        // A = Matrix of size m[0] by n[0] to be 'inverted'
        // A is destroyed by this routine and becomes the 
        // U matrix of the singular value decomposition
        // ma = row dimension of array A
        // unchanged by this routine
        // na = column dimension of array A
        // unchanged by this routine
        // AIN = computed 'inverse' (n[0] rows by m[0] cols.)
        // nain = row dimension of array AIN
        //        (Column dimension assumed to be at least m[0])
        // unchanged by this routine
        // m[0] = number of rows in matrix A and the number of columns in matrix AIN
        // n[0] = number of columns in matrix A the number of rows in matrix AIN
        // unchanged by this routine
        // tol = machine precision for computing environment
        // unchanged by this routine
        // V = work matrix which becomes the V matrix (n[0] by n[0])
        // of the singular value decomposition
        // nv = dimensions of V (Both rows and columns)
        // unchanged by this routine
        // fail[0] = error flag - true implies failure of ginvse
        
        // Note.  This routine is not designed to produce a 'good' generalized inverse.
        // It is meant only to furnish test data for routines gmatx, zielke, and ptst.
        
        int icount;
        int nm1;
        int i;
        int j;
        int jp1;
        int k;
        int sweep;
        int limit;
        double p;
        double q;
        double r;
        double vv = 0.0;
        double c;
        double s;
        double temp;
        
        // Initially set fail[0] = false to imply correct execution
        fail[0] = false;
        
        // Tests on validity of dimensions
        if (m[0] < 2) {
            System.out.println("ginvse failed because m[0] = " + m[0] + " is less than 2");
            Preferences.debug("ginvse failed because m[0] = " + m[0] + " is less than 2\n");
            fail[0] = true;
            return;
        }
        if (ma < 2) {
            System.out.println("ginvse failed because ma = " + ma + " is less than 2");
            Preferences.debug("ginvse failed because ma = " + ma + " is less than 2\n");
            fail[0] = true;
            return;        
        }
        if (n[0] < 2) {
            System.out.println("ginvse failed because n[0] = " + n[0] + " is less than 2");
            Preferences.debug("ginvse failed because n[0] = " + n[0] + " is less than 2\n");
            fail[0] = true;
            return;    
        }
        if (na < 2) {
            System.out.println("ginvse failed because na = " + na + " is less than 2");
            Preferences.debug("ginvse failed because na = " + na + " is less than 2\n");
            fail[0] = true;
            return;        
        }
        if (n[0] > na) {
            System.out.println("ginvse failed because n[0] = " + n[0] + " exceeds na = " + na);
            Preferences.debug("ginvse failed because n[0] = " + n[0] + " exceeds na = " + na + "\n");
            fail[0] = true;
            return;        
        }
        if (m[0] > ma) {
            System.out.println("ginvse failed because m[0] = " + m[0] + " exceeds ma = " + ma);
            Preferences.debug("ginvse failed because m[0] = " + m[0] + " exceeds ma = " + ma + "\n");
            fail[0] = true;
            return;        
        }
        
        // n[0] must not exceed m[0], otherwise ginvse will fail
        if (n[0] > m[0]) {
            System.out.println("ginvse failed because n[0] = " + n[0] + " exceeds m[0] = " + m[0]);
            Preferences.debug("ginvse failed because n[0] = " + n[0] + " exceeds m[0] = " + m[0] + "\n");
            fail[0] = true;
            return;            
        }
        
        // sweep counter initialized to zero
        sweep = 0;
        // set sweep limit
        // max(n[0], 6) was chosen from experience
        limit = Math.max(n[0], 6);
        
        // V[i][j] initially n by n identity
        for (i = 0; i < n[0]; i++) {
            for (j = 0; j < n[0]; j++) {
                V[i][j] = 0.0;
            }
            V[i][i] = 1.0;
        }
        
        do {
            // Initialize rotation counter (counts down to 0)
            icount = n[0]*(n[0] - 1)/2;
            
            // count sweep
            sweep = sweep + 1;
            nm1 = n[0] - 1;
            for (j = 1; j <= nm1; j++) {
                jp1 = j + 1;
                for (k = jp1; k <= n[0]; k++) {
                    p = 0.0;
                    q = 0.0;
                    r = 0.0;
                    for (i = 1; i <= m[0]; i++) {
                        // Test for and avoid underflow
                        // Not needed for machines which underflow to zero without error message
                        if (Math.abs(A[i-1][j-1]) >= tol) {
                            q = q + A[i-1][j-1]*A[i-1][j-1];
                        }
                        if (Math.abs(A[i-1][k-1]) >= tol) {
                            r = r + A[i-1][k-1]*A[i-1][k-1];    
                        }
                        if (Math.abs(A[i-1][j-1]/tol)*Math.abs(A[i-1][k-1]/tol) >= 1.0) {
                            p = p + A[i-1][j-1]*A[i-1][k-1];
                        }
                    } // for (i = 1; i <= m[0]; i++)
                    if (q < r) {
                        c = 0.0;
                        s = 1.0;
                    } // if (q < r)
                    else { // q >= r
                        if ((Math.abs(q) < tol) && (Math.abs(r) < tol)) {
                            icount = icount - 1;
                            continue;
                        }
                        if (r == 0.0) {
                            icount = icount - 1;
                            continue;
                        }
                        if ((p/q)*(p/r) < tol) {
                            icount = icount - 1;
                            continue;
                        }
                        
                        // Calculate the sine and cosine of the angle of rotation
                        q = q - r;
                        vv = Math.sqrt(4.0*p*p + q*q);
                        c = Math.sqrt((vv+q)/(2.0*vv));
                        s = p/(vv*c);
                    } // else q >= r
                    
                    // Apply the rotation to A
                    for (i = 1; i <= m[0]; i++) {
                        r = A[i-1][j-1];
                        A[i-1][j-1] = r*c + A[i-1][k-1]*s;
                        A[i-1][k-1] = -r*s + A[i-1][k-1]*c;
                    } // for (i = 1; i <= m[0]; i++)
                    
                    // Apply the rotation to V
                    for (i = 1; i <= n[0]; i++) {
                        r = V[i-1][j-1];
                        V[i-1][j-1] = r*c + V[i-1][k-1]*s;
                        V[i-1][k-1] = -r*s + V[i-1][k-1]*c;
                    } // for (i = 1; i <= n[0]; i++)
                } // for (k = jp1; k <= n[0]; k++)
            } // for (j = 1; j <= nm1; j++)
            
            // Output the number of sweeps and rotations
            Preferences.debug("Sweep = " + sweep + "\n");
            Preferences.debug("Jacobi rotations performed = " + icount + "\n");
            
            // Check number of sweeps and rotations (termination test)
        } while ((icount > 0) && (sweep < limit));
        
        if (sweep >= limit) {
            Preferences.debug("Sweep limit reached\n");
        }
        
        for (j = 1; j <= n[0]; j++) {
            q = 0.0;
            for (i = 1; i <= m[0]; i++) {
                q = q + A[i-1][j-1]*A[i-1][j-1];
            }
            
            // Arbitrary rank decision
            if (j == 1) {
                vv = 1.0E-3*Math.sqrt(q);
            }
            if (Math.sqrt(q) > vv) {
                for (i = 1; i <= m[0]; i++) {
                    A[i-1][j-1] = A[i-1][j-1]/q;
                }
            } // if (Math.sqrt(q) > vv)
            else { // Math.sqrt(q) <= vv
                for (i = 1; i <= m[0]; i++) {
                    A[i-1][j-1] = 0.0;
                }
            } // else Math.sqrt(q) <= vv
        } // for (j = 1; j <= n[0]; j++)
        
        for (i = 0; i < n[0]; i++) {
            for (j = 0; j < m[0]; j++) {
                temp = 0.0;
                for (k = 0; k < n[0]; k++) {
                    temp = temp + V[i][k]*A[j][k];
                } // for (k = 0; k < n[0]; k++)
                AIN[i][j] = temp;
            } // for (j = 0; j < m[0]; j++)
        } // for (i = 0; i < n[0]; i++)
        return;
    } // ginvse
    
    /* This is a port of dgelss, LAPACK driver routine from version 3.2
    *
    *  -- LAPACK driver routine (version 3.2) --
    *  -- LAPACK is a software package provided by Univ. of Tennessee,    --
    *  -- Univ. of California Berkeley, Univ. of Colorado Denver and NAG Ltd..--
    *     November 2006
    *
    *     .. Scalar Arguments ..
          INTEGER            INFO, LDA, LDB, LWORK, M, N, NRHS, RANK
          DOUBLE PRECISION   RCOND
    *     ..
    *     .. Array Arguments ..
          DOUBLE PRECISION   A( LDA, * ), B( LDB, * ), S( * ), WORK( * )
    *     ..
    *
    *  Purpose
    *  =======
    *
    *  DGELSS computes the minimum norm solution to a real linear least
    *  squares problem:
    *
    *  Minimize 2-norm(| b - A*x |).
    *
    *  using the singular value decomposition (SVD) of A. A is an M-by-N
    *  matrix which may be rank-deficient.
    *
    *  Several right hand side vectors b and solution vectors x can be
    *  handled in a single call; they are stored as the columns of the
    *  M-by-NRHS right hand side matrix B and the N-by-NRHS solution matrix
    *  X.
    *
    *  The effective rank of A is determined by treating as zero those
    *  singular values which are less than RCOND times the largest singular
    *  value.
    *
    
    */
    private void dgelss(int m, int n, int nrhs, double A[][], int lda, double B[][], int ldb, double s[],
                        double rcond, int rank[], double work[], int lwork, int info[]) {
        /*  Arguments
        *  =========
        *
        *  m       (input) INTEGER
        *          The number of rows of the matrix A. m >= 0.
        *
        *  n       (input) INTEGER
        *          The number of columns of the matrix A. n >= 0.
        *
        *  nrhs    (input) INTEGER
        *          The number of right hand sides, i.e., the number of columns
        *          of the matrices B and X. nrhs >= 0.
        *
        *  A       (input/output) DOUBLE PRECISION array, dimension (lda,n)
        *          On entry, the m-by-n matrix A.
        *          On exit, the first min(m,n) rows of A are overwritten with
        *          its right singular vectors, stored rowwise.
        *
        *  lda     (input) INTEGER
        *          The leading dimension of the array A.  lda >= max(1,m).
        *
        *  B       (input/output) DOUBLE PRECISION array, dimension (ldb,nrhs)
        *          On entry, the m-by-nrhs right hand side matrix B.
        *          On exit, B is overwritten by the n-by-nrhs solution
        *          matrix X.  If m >= n and RANK = n, the residual
        *          sum-of-squares for the solution in the i-th column is given
        *          by the sum of squares of elements n+1:m in that column.
        *
        *  ldb     (input) INTEGER
        *          The leading dimension of the array B. ldb >= max(1,max(m,n)).
        *
        *  s       (output) DOUBLE PRECISION array, dimension (min(m,n))
        *          The singular values of A in decreasing order.
        *          The condition number of A in the 2-norm = S(1)/S(min(m,n)).
        *
        *  rcond   (input) DOUBLE PRECISION
        *          rcond is used to determine the effective rank of A.
        *          Singular values s(i) <= RCOND*s[0] are treated as zero.
        *          If  rcond < 0, machine precision is used instead.
        *
        *  rank    (output) INTEGER
        *          The effective rank of A, i.e., the number of singular values
        *          which are greater than rcond*s[0].
        *
        *  work    (workspace/output) DOUBLE PRECISION array, dimension (MAX(1,lwork))
        *          On exit, if info[0] = 0, work[0] returns the optimal lwork.
        *
        *  lwork   (input) INTEGER
        *          The dimension of the array work. lwork >= 1, and also:
        *          lwork >= 3*min(m,n) + max( 2*min(m,n), max(m,n), nrhs )
        *          For good performance, LWORK should generally be larger.
        *
        *          If lwork = -1, then a workspace query is assumed; the routine
        *          only calculates the optimal size of the work array, returns
        *          this value as the first entry of the work array, and no error
        *          message related to lwork is issued by XERBLA.
        *
        *  info    (output) INTEGER
        *          = 0:  successful exit
        *          < 0:  if info[0] = -i, the i-th argument had an illegal value.
        *          > 0:  the algorithm for computing the SVD failed to converge;
        *                if info[0] = i, i off-diagonal elements of an intermediate
        *                bidiagonal form did not converge to zero.
        */
        int minmn;
        int maxmn;
        boolean lquery;
        int minwrk;
        int maxwrk = 0;
        int mm;
        int mnthr = 0;
        String name;
        String opts;
        int bdspac;
        double[] smlnum = new double[1];
        double[] bignum = new double[1];
        double anrm;
        int iascl;
        int i;
        double bnrm;
        int ibscl;
        int itau;
        int iwork;
        
        // Test the input arguments
        info[0] = 0;
        minmn = Math.min(m, n);
        maxmn = Math.max(m, n);
        lquery = (lwork == -1);
        if (m < 0) {
            info[0] = -1;
        }
        else if (n < 0) {
            info[0] = -2;
        }
        else if (nrhs < 0) {
            info[0] = -3;
        }
        else if (lda < Math.max(1, m)) {
            info[0] = -5;
        }
        else if (ldb < Math.max(1, maxmn)) {
            info[0] = -7;
        }
        
        // Compute workspace
        // (Note: Comments in the code beginning "Workspace:" describe the minimal amount of workspace needed at
        // that point in the code, as well as the preferred amount for good performance.  nb refers to the
        // optimal block size for the immediately following subroutine, as returned by ilaenv.)
        
        if (info[0] == 0) {
            minwrk = 1;
            maxwrk = 1;
            if (minmn > 0) {
                mm = m;
                name = new String("DGELSS");
                opts = new String(" ");
                mnthr = ilaenv(6, name, opts, m, n, nrhs, -1);
                if ((m >= n) && (m >= mnthr)) {
                    // Path 1a - overdetermined, with many more rows than columns
                    mm = n;
                    name = new String("DGEQRF");
                    opts = new String(" ");
                    maxwrk = Math.max(maxwrk, n + n*ilaenv(1, name, opts, m, n, -1, -1));
                    name = new String("DORMQR");
                    opts = new String("LT");
                    maxwrk = Math.max(maxwrk, n + nrhs*ilaenv(1, name, opts, m, nrhs, n, -1));
                } // if ((m >= n) && (m >= mnthr))
                if (m >= n) {
                    // Path 1 - overdetermined or exactly determined
                    
                    // Compute workspace needed for dbdsqr
                    bdspac = Math.max(1, 5*n);
                    name = new String("DGEBRD");
                    opts = new String(" ");
                    maxwrk = Math.max(maxwrk, 3*n + (mm + n)*ilaenv(1, name, opts, mm, n, -1, -1));
                    name = new String("DORMBR");
                    opts = new String("QLT");
                    maxwrk = Math.max(maxwrk, 3*n + nrhs*ilaenv(1, name, opts, mm, nrhs, n, -1));
                    name = new String("DORGBR");
                    opts = new String("P");
                    maxwrk = Math.max(maxwrk, 3*n + (n - 1)*ilaenv(1, name, opts, n, n, n, -1));
                    maxwrk = Math.max(maxwrk, bdspac);
                    maxwrk = Math.max(maxwrk, n*nrhs);
                    minwrk = Math.max(3*n + mm, Math.max(3*n + nrhs, bdspac));
                    maxwrk = Math.max(minwrk, maxwrk);
                } // if (m >= n)
                if (n > m) {
                    // Compute workspace needed for dbdsqr
                    
                    bdspac = Math.max(1, 5*m);
                    minwrk = Math.max(3*m + nrhs, Math.max(3*m + n, bdspac));
                    if (n >= mnthr) {
                        // Path 2a - undetermined, with many more columns than rows
                        name = new String("DGELQF");
                        opts = new String(" ");
                        maxwrk = m + m*ilaenv(1, name, opts, m, n, -1, -1);
                        name = new String("DGEBRD");
                        opts = new String(" ");
                        maxwrk = Math.max(maxwrk, m*m + 4*m + 2*m*ilaenv(1, name, opts, m, m, -1, -1));
                        name = new String("DORMBR");
                        opts = new String("QLT");
                        maxwrk = Math.max(maxwrk, m*m + 4*m + nrhs*ilaenv(1, name, opts, m, nrhs, m, -1));
                        name = new String("DORGBR");
                        opts = new String("P");
                        maxwrk = Math.max(maxwrk, m*m + 4*m + (m - 1)*ilaenv(1, name, opts, m, m, m, -1));
                        maxwrk = Math.max(maxwrk, m*m + m + bdspac);
                        if (nrhs > 1) {
                            maxwrk = Math.max(maxwrk, m*m + m + m*nrhs);    
                        } // if (nrhs > 1)
                        else {
                            maxwrk = Math.max(maxwrk, m*m + 2*m);
                        }
                        name = new String("DORMLQ");
                        opts = new String("LT");
                        maxwrk = Math.max(maxwrk, m + nrhs*ilaenv(1, name, opts, n, nrhs, m, -1));
                    } // if (n >= mnthr)
                    else { // n < mnthr
                        // Path 2 - underdetermined
                        name = new String("DGEBRD");
                        opts = new String(" ");
                        maxwrk = 3*m + (n + m)*ilaenv(1, name, opts, m, n, -1, -1);
                        name = new String("DORMBR");
                        opts = new String("QLT");
                        maxwrk = Math.max(maxwrk, 3*m + nrhs*ilaenv(1, name, opts, m, nrhs, m, -1));
                        name = new String("DORGBR");
                        opts = new String("P");
                        maxwrk = Math.max(maxwrk, 3*m + m*ilaenv(1, name, opts, m, n, m, -1));
                        maxwrk = Math.max(maxwrk, bdspac);
                        maxwrk = Math.max(maxwrk, n*nrhs);
                    } // else n < mnthr
                } // if (n > m)
                maxwrk = Math.max(minwrk, maxwrk);
            } // if (minmn > 0)
            work[0] = maxwrk;
            
            if ((lwork < minwrk) && (!lquery)) {
                info[0] = -12;
            }
        } // if (info[0] == 0)
        
        if (info[0] != 0) {
            MipavUtil.displayError("DGELSS exits with error info[0] = " + info[0]);
            Preferences.debug("DGELSS exits with error info[0] = " + info[0] + "\n");
            return;
        }
        else if (lquery) {
            return;
        }
        
        // Quick return if possible
        if ((m == 0) || (n == 0)) {
            rank[0] = 0;
            return;
        }
        
        // Get machine parameters
        eps = dlamch('P');
        sfmin = dlamch('S');
        smlnum[0] = sfmin/eps;
        bignum[0] = 1.0 / smlnum[0];
        dlabad(smlnum, bignum);
        
        // Scale A if max element outside range [smlnum[0], bignum[0]]
        anrm = dlange('M', m, n, A, lda, work);
        iascl = 0;
        if ((anrm > 0.0) && (anrm < smlnum[0])) {
            // Scale matrix norm up to smlnum[0]
            dlascl('G', 0, 0, anrm, smlnum[0], m, n, A, lda, info);
            iascl = 1;
        } // if ((anrm > 0.0) && (anrm < smlnum[0]))
        else if (anrm > bignum[0]) {
            // Scale matrix norm down to bignum[0]
            dlascl('G', 0, 0, anrm, bignum[0], m, n, A, lda, info);
            iascl = 2;
        } // else if (anrm > bignum[0])
        else if (anrm == 0.0) {
            // Matrix all zero.  Return zero solution.
            dlaset('F', Math.max(m,n), nrhs, 0.0, 0.0, B, ldb);
            for (i = 0; i < minmn; i++) {
                s[i] = 0.0;
            }
            rank[0] = 0;
            work[0] = maxwrk;
            return;
        } // else if (anrm == 0.0)
        
        // Scale B if max element outside range [smlnum[0], bignum[0]]
        bnrm = dlange('M', m, nrhs, B, ldb, work);
        ibscl = 0;
        if ((bnrm > 0.0) && (bnrm < smlnum[0])) {
            // Scale matrix norm up to smlnum[0]
            dlascl('G', 0, 0, bnrm, smlnum[0], m, nrhs, B, ldb, info);
            ibscl = 1;
        }
        else if (bnrm > bignum[0]) {
            // Scale matrix down to bignum[0]
            dlascl('G', 0, 0, bnrm, bignum[0], m, nrhs, B, ldb, info);
            ibscl = 2;
        }
        
        // Overdetermined case
        if (m >= n) {
            // Path 1 - overdetermined or exactly determined
            mm = m;
            if (m >= mnthr) {
                // Path 1a - overdetermined, with many more rows than columns
                mm = n;
                itau = 1;
                iwork = itau + n;
                
                // Compute A = Q*R
                // (Workspace: need 2*n, prefer n + n*nb)
            }  // if (m >= mnthr)
        } // if (m >= n)
    } // dgelss
    
    /**
     * ilaenv is ported from the version 3.2.1 LAPACK auxiliary routine Original ILAENV created by Univ. of Tennessee,
     * Univ. of California Berkeley, Univ. of Colorado Denver and NAG Ltd., April, 2009
     *
     * <p>ilaenv is called from the LAPACK routines to choose problem-dependent parameters for the local environment.
     * See ispec for a description of the parameters.</p>
     *
     * <p>This version provides a set of parameters which should give good, but not optimal, performance on many of the
     * currently available computers. Users are encouraged to modify this subroutine to set the tuning parameters for
     * their particular machine using the option and problem size information in the arguments.</p>
     *
     * <p>This routine will not function correctly if it is converted to all lower case. Converting it to all upper case
     * is allowed.</p>
     *
     * @param   ispec  input integer Specifies the parameter to be returned as the value of ilaenv. 
     *                 = 1: the optimal blocksize; if this value is 1,
     *                      an unblocked algorithm will give the best performance. 
     *                 = 2: the minimum block size for which the block routine should be used;
     *                      if the usable block size is less than this value,
     *                      an unblocked routine should be used 
     *                 = 3: the crossover point ( in a block routine, for n less than this value,
     *                      an unblocked routine should be used) 
     *                 = 4: the number of shifts, used in the nonsymmetric eigenvalue routines (deprecated)
     *                 = 5: the minimum column dimension for blocking to be used; 
     *                      rectangular blocks must have dimension at least k by m,
     *                      where k is given by ilaenv(2,...) and m by ilaenv(5,...) 
     *                 = 6: the crossover point for the SVD (when reducing an m by n
     *                      matrix to bidiagonal form, if max(m,n)/min(m,n) exceeds this value,
     *                      a QR factorization is used first to reduce the matrix to a triangular form.)
     *                 = 7: the number of processors 
     *                 = 8: the crossover point for the multishift QR method for nonsymmetric
     *                      eigenvalue problems (deprecated) 
     *                 = 9: maximum size of the subproblems at the bottom of the computation tree in the
     *                      divide-and-conquer algorithm (used by xgelsd and xgesdd) 
     *                = 10: ieee NaN arithmetic can be trusted not to trap 
     *                = 11: infinity can be trusted not to trap
     *                12 <= ispec <= 16:
     *                      xhseqr or one of its subroutines
     *                      see iparmq for detailed explanation
     * @param   name   input String The name of the calling subroutine, in either upper case or lower case.
     * @param   opts   input String The character options to the subroutine name, concatenated into a single character
     *                 string. For example, uplo = 'U', trans = 'T', and diag = 'N' for a triangular routine would be
     *                 specified as opts = 'UTN'. opts has all the character options to subroutine name, in the same
     *                 order that they appear in the argument list for name, even if they are not used in determining
     *                 the value of the parameter specified by ispec.
     * @param   n1     input integer
     * @param   n2     input integer
     * @param   n3     input integer
     * @param   n4     input integer n1 to n4 have problem dimensions for the subroutine name; these may not all be
     *                 required. The problem dimensions n1, n2, n3, and n4 are specified in the order that they appear
     *                 in the argument list for name. n1 is used first, n2 second, and so on, and unused problem
     *                 dimensions are passed a value of -1.
     *
     * @return  answer output integer 
     *                 >= 0; the value of the parameter specified by ispec 
     *                 < 0: il answer = -k, the k-th parameter had an illegal value 
     * The parameter value returned by ilaenv is checked for validity in the calling routine.
     */
    private int ilaenv(int ispec, String name, String opts, int n1, int n2, int n3, int n4) {
        String subnam;
        int answer;
        char first;
        String c1, c2, c3, c4;
        boolean sname;
        boolean cname;
        int nb;
        int nbmin;
        int nx;

        if ((ispec < 1) || (ispec > 16)) {

            // Invalid value for ispec
            return -1;
        }

        if ((ispec == 1) || (ispec == 2) || (ispec == 3)) {

            // Copy name to subnam
            // Make subnam upper case is the first character of name is lower case
            subnam = new String(name);
            first = name.charAt(0);

            if (Character.isLowerCase(first)) {
                subnam = subnam.toUpperCase();
            }

            c1 = subnam.substring(0, 1);

            if ((c1.equals("S")) || (c1.equals("D"))) {
                sname = true;
            } else {
                sname = false;
            }

            if ((c1.equals("C")) || (c1.equals("Z"))) {
                cname = true;
            } else {
                cname = false;
            }

            if (!(cname || sname)) {
                return 1;
            }

            c2 = subnam.substring(1, 3);
            c3 = subnam.substring(3, 6);
            c4 = c3.substring(1, 3);

            if (ispec == 1) {

                // block size
                // In these examples, separate code is provided for setting nb for
                // real and complex.  We assume that nb will take the same value in
                // single or double precision.
                nb = 1;

                if (c2.equals("GE")) {

                    if (c3.equals("TRF")) {
                        nb = 64;
                    } // if (c3.equals("TRF"))
                    else if ((c3.equals("QRF")) || (c3.equals("RQF")) || (c3.equals("LQF")) || (c3.equals("QLF"))) {
                        nb = 32;
                    } // else if ((c3.equals("QRF")) || (c3.equals("RQF")) ||
                    else if (c3.equals("HRD")) {
                        nb = 32;
                    } // else if (c3.equals("HRD"))
                    else if (c3.equals("BRD")) {
                        nb = 32;
                    } // else if (c3.equals("BRD"))
                    else if (c3.equals("TRI")) {
                        nb = 64;
                    } // else if (c3.equals("TRI"))
                } // if (c2.equals("GE"))
                else if (c2.equals("PO")) {

                    if (c3.equals("TRF")) {
                        nb = 64;
                    } // if (c3.equals("TRF"))
                } // else if (c2.equals("PO"))
                else if (c2.equals("SY")) {

                    if (c3.equals("TRF")) {
                        nb = 64;
                    } // if (c3.equals("TRF"))
                    else if (sname && (c3.equals("TRD"))) {
                        nb = 32;
                    } // else if (sname && (c3.equals("TRD")))
                    else if (sname && (c3.equals("GST"))) {
                        nb = 64;
                    } // else if (sname && (c3.equals("GST")))
                } // else if (c2.equals("SY"))
                else if (cname && (c2.equals("HE"))) {

                    if (c3.equals("TRF")) {
                        nb = 64;
                    } // if (c3.equals("TRF"))
                    else if (c3.equals("TRD")) {
                        nb = 32;
                    } // else if (c3.equals("TRD"))
                    else if (c3.equals("GST")) {
                        nb = 64;
                    } // else if (c3.equals("GST"))
                } // else if (cname && (c2.equals("HE")))
                else if (sname && (c2.equals("OR"))) {

                    if ((c3.substring(0, 1).equals("G")) || (c3.substring(0, 1).equals("M"))) {

                        if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) || (c4.equals("QL")) ||
                                (c4.equals("HR")) || (c4.equals("TR")) || (c4.equals("BR"))) {
                            nb = 32;
                        }
                    } // if (c3.substring(0,1).equals("G")) ||
                } // else if (sname && (c2.equals("OR")))
                else if (cname && (c2.equals("UN"))) {

                    if ((c3.substring(0, 1).equals("G")) || (c3.substring(0, 1).equals("M"))) {

                        if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) || (c4.equals("QL")) ||
                                (c4.equals("HR")) || (c4.equals("TR")) || (c4.equals("BR"))) {
                            nb = 32;
                        }
                    } // if (c3.substring(0,1).equals("G")) ||
                } // else if (cname && (c2.equals("UN")))
                else if (c2.equals("GB")) {

                    if (c3.equals("TRF")) {

                        if (n4 <= 64) {
                            nb = 1;
                        } else {
                            nb = 32;
                        }
                    } // if (c3.equals("TRF"))
                } // else if (c2.equals("GB"))
                else if (c2.equals("PB")) {

                    if (c3.equals("TRF")) {

                        if (n2 <= 64) {
                            nb = 1;
                        } else {
                            nb = 32;
                        }
                    } // if (c3.equals("TRF"))
                } // else if (c2.equals("PB"))
                else if (c2.equals("TR")) {

                    if (c3.equals("TRI")) {
                        nb = 64;
                    } // if (c3.equals("TRI"))
                } // else if (C2.equals("TR"))
                else if (c2.equals("LA")) {

                    if (c3.equals("UUM")) {
                        nb = 64;
                    } // if (c3.equals("UUM"))
                } // else if (c2.equals("LA"))
                else if (sname && (c2.equals("ST"))) {

                    if (c3.equals("EBZ")) {
                        nb = 1;
                    } // if (c3.equals("EBZ"))
                } // else if (sname && (c2.equals("ST")))

                return nb;
            } // if (ispec == 1)
            else if (ispec == 2) {
                // minimum block size

                nbmin = 2;

                if (c2.equals("GE")) {

                    if ((c3.equals("QRF")) || (c3.equals("RQF")) || (c3.equals("LQF")) || (c3.equals("QLF")) ||
                            (c3.equals("HRD")) || (c3.equals("BRD")) || (c3.equals("TRI"))) {
                        nbmin = 2;
                    } // if ((c3.equals("QRF")) || (c3.equals("RQF")) || (c3.equals("LQF")) ||
                } // if (c2.equals("GE"))
                else if (c2.equals("SY")) {

                    if (c3.equals("TRF")) {
                        nbmin = 8;
                    } // if (c3.equals("TRF"))
                    else if (sname && (c3.equals("TRD"))) {
                        nbmin = 2;
                    } // else if (sname && (c3.equals("TRD")))
                } // else if (c2.equals("SY"))
                else if (cname && (c2.equals("HE"))) {

                    if (c3.equals("TRD")) {
                        nbmin = 2;
                    } // if (c3.equals("TRD"))
                } // else if (cname && (c2.equals("HE")))
                else if (sname && (c2.equals("OR"))) {

                    if ((c3.substring(0, 1).equals("G")) || (c3.substring(0, 1).equals("M"))) {

                        if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) || (c4.equals("QL")) ||
                                (c4.equals("HR")) || (c4.equals("TR")) || (c4.equals("BR"))) {
                            nbmin = 2;
                        } // if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) ||
                    } // if ((c3.substring(0,1).equals("G")) ||
                } // else if (sname && (c2.equals("OR")))
                else if (cname && (c2.equals("UN"))) {

                    if ((c3.substring(0, 1).equals("G")) || (c3.substring(0, 1).equals("M"))) {

                        if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) || (c4.equals("QL")) ||
                                (c4.equals("HR")) || (c4.equals("TR")) || (c4.equals("BR"))) {
                            nbmin = 2;
                        } // if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) ||
                    } // if ((c3.substring(0,1).equals("G")) ||
                } // else if (cname && (c2.equals("UN")))

                return nbmin;
            } // else if (ispec == 2)
            else { // ispec == 3

                // crossover point

                nx = 0;

                if (c2.equals("GE")) {

                    if ((c3.equals("QRF")) || (c3.equals("RQF")) || (c3.equals("LQF")) || (c3.equals("QLF")) ||
                            (c3.equals("HRD")) || (c3.equals("BRD"))) {
                        nx = 128;
                    } // if ((c3.equals("QRF")) || (c3.equals("RQF")) || (c3.equals("LQF")) ||
                } // if (c2.equals("GE"))
                else if (c2.equals("SY")) {

                    if (sname && (c3.equals("TRD"))) {
                        nx = 32;
                    } // if (sname && (c3.equals("TRD")))
                } // else if (c2.equals("SY"))
                else if (cname && (c2.equals("HE"))) {

                    if (c3.equals("TRD")) {
                        nx = 32;
                    } // if (c3.equals("TRD"))
                } // else if (cname && (c2.equals("HE")))
                else if (sname && (c2.equals("OR"))) {

                    if (c3.substring(0, 1).equals("G")) {

                        if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) || (c4.equals("QL")) ||
                                (c4.equals("HR")) || (c4.equals("TR")) || (c4.equals("BR"))) {
                            nx = 128;
                        } // if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) ||
                    } // if (c3.substring(0,1).equals("G"))
                } // else if (sname && (c2.equals("OR")))
                else if (cname && (c2.equals("UN"))) {

                    if (c3.substring(0, 1).equals("G")) {

                        if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) || (c4.equals("QL")) ||
                                (c4.equals("HR")) || (c4.equals("TR")) || (c4.equals("BR"))) {
                            nx = 128;
                        } // if ((c4.equals("QR")) || (c4.equals("RQ")) || (c4.equals("LQ")) ||
                    } // if (c3.substring(0,1).equals("G"))
                } // else if (cname && (c2.equals("UN")))

                return nx;
            } // else ispec == 3
        } // if ((ispec == 1) || (ispec == 2) || (ispec == 3))
        else if (ispec == 4) {

            // number of shifts (used by xhseqr)
            return 6;
        } // else if (ispec == 4)
        else if (ispec == 5) {

            // minimum column dimension (not used)
            return 2;
        } // else if (ispec == 5)
        else if (ispec == 6) {

            // crossover point for SVD (used by xgelss and xgesvd)
            return (int) (1.6 * Math.min(n1, n2));
        } // else if (ispec == 6)
        else if (ispec == 7) {

            // number of processors (not used)
            return 1;
        } // else if (ispec == 7)
        else if (ispec == 8) {

            // crossover point for multishift (used by xhseqr)
            return 50;
        } // else if (ispec == 8)
        else if (ispec == 9) {

            // maximum size of the subproblems at the bottom of the computation
            // tree in divide-and-conquer algorithm (used by xgelsd and xgesdd)
            return 25;
        } // else if (ispec == 9)
        else if (ispec == 10) {

            // ieee NaN arithmetic can be trusted not to trap
            answer = ieeeck(1, 0.0, 1.0);

            return answer;
        } // else if (ispec == 10)
        else if (ispec == 11){

            // infinity arithmetic can be trusted not to trap
            answer = ieeeck(0, 0.0, 1.0);

            return answer;
        } // else ispec == 11
        else { // 12 <= ispec <= 16
            // answer = iparmq(ispec, name, opts, n1, n2, n3, n4);
            
            return -1;
        }
    } // ilaenv
    
    /**
     * Version 3.2 auxiliary routine ported form LAPACK Original IEEECK created by Univ. of Tennessee, Univ. of
     * California Berkeley, University of Colorado Denver, and NAG Ltd., November, 2006
     * ieeeck is called form the ilaenv routine to verify that infinity and possibly NaN arithmetic is safe
     * (i.e. will not trap)
     *
     * @param   ispec  input int Specifies whether to test just for infinity arithmetic or whether to test for infinity
     *                 and NaN arithmetic 
     *                 = 0: Verify infinity arithmetic only. 
     *                 = 1: Verify infinity and NaN aritmetic
     * @param   zero   input double Must contain the value 0.0. This is passed to prevent the compiler from optimizing away
     *                 this code
     * @param   one    input double Must contain the value 1.0. This is passed to prevent the compiler from optimizing away
     *                 this code.
     *
     * @return  int    = 0: Arithmetic failed to produce the correct answers 
     *                 = 1: Arithmetic produced the correct answers
     */
    private int ieeeck(int ispec, double zero, double one) {
        double posinf;
        double neginf;
        double negzro;
        double newzro;
        double nan1;
        double nan2;
        double nan3;
        double nan4;
        double nan5;
        double nan6;

        posinf = one / zero;

        if (posinf <= one) {
            return 0;
        }

        neginf = -one / zero;

        if (neginf >= zero) {
            return 0;
        }

        negzro = one / (neginf + one);

        if (negzro != zero) {
            return 0;
        }

        neginf = one / negzro;

        if (neginf >= zero) {
            return 0;
        }

        newzro = negzro + zero;

        if (newzro != zero) {
            return 0;
        }

        posinf = one / newzro;

        if (posinf <= one) {
            return 0;
        }

        neginf = neginf * posinf;

        if (neginf >= zero) {
            return 0;
        }

        posinf = posinf * posinf;

        if (posinf <= one) {
            return 0;
        }

        // Return if we were only asked to check infinity arithmetic
        if (ispec == 0) {
            return 1;
        }

        nan1 = posinf + neginf;

        nan2 = posinf / neginf;

        nan3 = posinf / posinf;

        nan4 = posinf * zero;

        nan5 = neginf * negzro;

        nan6 = nan5 * 0.0;

        if (nan1 == nan1) {
            return 0;
        }

        if (nan2 == nan2) {
            return 0;
        }

        if (nan3 == nan3) {
            return 0;
        }

        if (nan4 == nan4) {
            return 0;
        }

        if (nan5 == nan5) {
            return 0;
        }

        if (nan6 == nan6) {
            return 0;
        }

        return 1;
    } // ieeeck
    
    /**
     * This is a port of the version 3.2 LAPACK auxiliary routine DLAMCH Original DLAMCH created by Univ. of Tennessee,
     * Univ. of California Berkeley, and NAG Ltd., November, 2006
     * dlamch determines double precision machine parameters.
     *
     * @param   cmach  input char Specifies the value to be returned by dlamch
     *                 = 'E' or 'e', returns eps, relative machine precision 
     *                 = 'S' or 's', returns sfmin, safe minimum, such that 1/sfmin does not overflow
     *                 = 'B' or 'b', returns base, base of the machine
     *                 = 'P' or 'p', returns prec = eps*base
     *                 = 'N' or 'n', returns t, number of (base) digits in the mantissa
     *                 = 'R' or 'r', returns rnd = 1.0 when rounding occurs in addition, 0.0 otherwise 
     *                 = 'M' or 'm', returns emin, minimum exponent before (gradual) underflow 
     *                 = 'U' or 'u', returns rmin, underflow threshold = base**(emin-1)
     *                 = 'L' or 'l', emax, largest exponent before overflow 
     *                 = 'O' or 'o', rmax, overflow threshold = (base**emax)*(1-eps)
     *
     * @return  double
     */
    private double dlamch(char cmach) {

        boolean[] lrnd = new boolean[1];
        int[] beta = new int[1];
        int[] imax = new int[1];
        int[] imin = new int[1];
        int[] it = new int[1];
        double rmach = 0.0;
        double small;

        if (first) {
            first = false;
            dlamc2(beta, it, lrnd, imin, imax);
            base = beta[0];
            t = it[0];

            if (lrnd[0]) {
                rnd = 1.0;
                eps = Math.pow(base, (1 - it[0])) / 2.0;
            } else {
                rnd = 0.0;
                eps = Math.pow(base, (1 - it[0]));
            }

            prec = eps * base;
            emin = imin[0];
            emax = imax[0];
            sfmin = rmin;
            small = 1.0 / rmax;

            if (small >= sfmin) {

                // Use small plus a bit, to avoid the possibility of rounding causing
                // overflow when computing 1/sfmin.
                sfmin = small * (1.0 + eps);
            }
        } // if (first)

        if ((cmach == 'E') || (cmach == 'e')) {
            rmach = eps;
        } else if ((cmach == 'S') || (cmach == 's')) {
            rmach = sfmin;
        } else if ((cmach == 'B') || (cmach == 'b')) {
            rmach = base;
        } else if ((cmach == 'P') || (cmach == 'p')) {
            rmach = prec;
        } else if ((cmach == 'N') || (cmach == 'N')) {
            rmach = t;
        } else if ((cmach == 'R') || (cmach == 'r')) {
            rmach = rnd;
        } else if ((cmach == 'M') || (cmach == 'm')) {
            rmach = emin;
        } else if ((cmach == 'U') || (cmach == 'u')) {
            rmach = rmin;
        } else if ((cmach == 'L') || (cmach == 'l')) {
            rmach = emax;
        } else if ((cmach == 'O') || (cmach == 'o')) {
            rmach = rmax;
        }

        return rmach;
    } // dlamch
    
    /**
     * Port of version 3.2 LAPACK auxiliary routine DLAMC1 Original DLAMC1 created by Univ. of Tennessee, Univ. of
     * California Berkeley, and NAG Ltd., November, 2006
     * dlamc1 determines the machine parameters given by beta, t, rnd, and ieee1.
     *
     * @param  beta   output int[] The base of the machine.
     * @param  t      output int[] The number of (beta) digits in the mantissa
     * @param  rnd    output boolean[] Specifies whether proper rounding (rnd = true) or chopping (rnd = false) occurs in
     *                addition. This may not be a reliable guide to the way in which the machine performs its
     *                arithmetic.
     * @param  ieee1  output boolean[] Specifies whether rounding appears to be done in the IEEE 'round to nearest'
     *                style.
     * This routine is based on the routine ENVRON by Malcolm and incorporates suggestions by Gentleman and Marovich. See
     * Malcolm, M. A. (1972) Algorithms to reveal properties of floating-point arithmetic.  Comms. of the ACM, 15,
     * pp. 949-951.
     * Gentleman, W. M. and Marovich S. B. (1974) More on algorithms that reveal properties of floating point
     * arithmetic units.  Comms. of the ACM, 17, pp. 276-277.
     */
    private void dlamc1(int[] beta, int[] t, boolean[] rnd, boolean[] ieee1) {
        boolean lieee1;
        boolean lrnd;
        int lbeta;
        int lt;
        double a;
        double b;
        double c;
        double f;
        double one;
        double qtr;
        double savec;
        double t1;
        double t2;

        one = 1;

        // lbeta, lieee1, lt, and lrnd are the local values of beta, ieee1, t, and
        // rnd.

        // Throughout this routine we use the function dlamc3 to ensure that
        // relevant values are stored and not held in registers, or are not
        // affected by optimizers.

        // Compute a = 2.0**m with the smallest positive integer m such that
        // computed value(a + 1.0) = a.
        a = 1;
        c = 1;

        while (c == one) {
            a = 2 * a;
            c = dlamc3(a, one);
            c = dlamc3(c, -a);
        } // while (c == one)

        // Now compute b = 2.0**m with the smallest positive integer m such that
        // computed value(a + b) > a
        b = 1;
        c = dlamc3(a, b);

        while (c == a) {
            b = 2 * b;
            c = dlamc3(a, b);
        } // while (c == a)

        // Now compute the base.  a and c are neighboring floating point numbers
        // in the interval (beta**t, beta**(t+1)) and so their difference is beta.
        // Adding 0.25 to c is to ensure that it is truncated to beta and not
        // (beta - 1).

        qtr = one / 4;
        savec = c;
        c = dlamc3(c, -a);
        lbeta = (int) (c + qtr);

        // Now determine whether rounding or chopping occurs, by adding a
        // bit less than beta/2 and a bit more than beta/2 to a.

        b = lbeta;
        f = dlamc3(b / 2, -b / 100);
        c = dlamc3(f, a);

        if (c == a) {
            lrnd = true;
        } else {
            lrnd = false;
        }

        f = dlamc3(b / 2, b / 100);
        c = dlamc3(f, a);

        if ((lrnd) && (c == a)) {
            lrnd = false;
        }

        // Try and decide whether rounding is done in the IEEE 'round to nearest'
        // style. b/2 is half a unit in the last place of the two numbers a and
        // savec.  Furthermore, a is even, i.e. has last bit zero, and savec is
        // odd. Thus adding b/2 to a should not change a, but adding b/2 to savec
        // should change savec.

        t1 = dlamc3(b / 2, a);
        t2 = dlamc3(b / 2, savec);
        lieee1 = (t1 == a) && (t2 > savec) && lrnd;

        // Now find the mantissa, t.  It should be the integer part of log to the
        // base beta of a, however it is safer to determine t by powering.  So we
        // find t as the smallest positive integer for which
        // computed value(beta**t + 1.0) = 1.0.

        lt = 0;
        a = 1;
        c = 1;

        while (c == one) {
            lt = lt + 1;
            a = a * lbeta;
            c = dlamc3(a, one);
            c = dlamc3(c, -a);
        } // while (c == one)

        beta[0] = lbeta;
        t[0] = lt;
        rnd[0] = lrnd;
        ieee1[0] = lieee1;

        return;
    } // dlamc1
    
    /**
     * Port of LAPACK version 3.2 auxiliary routine DLAMC2 Original DLAMC2 created by Univ. of Tennessee, Univ. of
     * California Berkeley, nad NAG Ltd., November, 2006
     * Determines machine parameters 3 globals are determined: 1.) eps double The smallest positive number such that
     * computed value(1.0 - eps) < 1.0 2.) rmin double The smallest normalized number for the machine, given by
     * base**(emin - 1), where base is the floating point value of beta. 3.) rmax double The largest positive number for
     * the machine, given by base**emax*(1-eps), where base is the floating point value of beta.
     *
     * @param  beta  output int[] The base of the machine.
     * @param  t     output int[] The number of (beta) digits in the mantissa.
     * @param  rnd   ouptut boolean Specifies whether proper rounding (rnd == true) or chopping (rnd == false) occurs in
     *               addition. This may not be a reliable guide to the way in which the machine performs its arithmetic
     * @param  emin  output int[] The minimum exponent before (gradual) underflow occurs
     * @param  emax  output int[] The maximum exponent before overflow occurs
     * The computation of EPS is based on a routine PARANOIA by W. Kahan of the University of California at Berkeley.
     */
    private void dlamc2(int[] beta, int[] t, boolean[] rnd, int[] emin, int[] emax) {
        boolean ieee;
        boolean iwarn = false;
        boolean[] lieee1 = new boolean[1];
        boolean[] lrnd = new boolean[1];
        int[] gnmin = new int[1];
        int[] gpmin = new int[1];
        int i;
        int[] lbeta = new int[1];
        int[] lemax = new int[1];
        int lemin;
        int[] lt = new int[1];
        int[] ngnmin = new int[1];
        int[] ngpmin = new int[1];
        double a;
        double b;
        double c;
        double half;
        double leps;
        double[] lrmax = new double[1];
        double lrmin;
        double one;
        double rbase;
        double sixth;
        double small;
        double third;
        double two;
        double zero;

        zero = 0;
        one = 1;
        two = 2;

        // lbeta, lt, lrnd, leps, lemin, and lrmin are the local values of beta, t,
        // rnd, eps, emin, and rmin.

        // Throughout this routine we use the function dlamc3 to ensure that
        // relevant values are stored and not held in registers, or are not
        // affected by optimizers.

        // dlamc1 returns the parameters lbeta, lt, lrnd, and lieee1.
        dlamc1(lbeta, lt, lrnd, lieee1);

        // Start to find eps

        b = lbeta[0];
        a = Math.pow(b, -lt[0]);
        leps = a;

        // Try some tricks to see whether or not this is the correct eps.
        b = two / 3;
        half = one / 2;
        sixth = dlamc3(b, -half);
        third = dlamc3(sixth, sixth);
        b = dlamc3(third, -half);
        b = dlamc3(b, sixth);
        b = Math.abs(b);

        if (b < leps) {
            b = leps;
        }

        leps = 1;

        while ((leps > b) && (b > zero)) {
            leps = b;
            c = dlamc3(half * leps, Math.pow(two, 5.0) * (leps * leps));
            c = dlamc3(half, -c);
            b = dlamc3(half, c);
            c = dlamc3(half, -b);
            b = dlamc3(half, c);
        } // while ((leps > b) && (b > zero))

        if (a < leps) {
            leps = a;
        }

        // Computation of eps complete.

        // Now find emin.  let a = + or - 1, and + or - (1 + base**(-3)).
        // Keep dividing a by beta until (gradual) underflow occurs. This
        // is detected when we cannot recover the previous a.

        rbase = one / lbeta[0];
        small = one;

        for (i = 1; i <= 3; i++) {
            small = dlamc3(small * rbase, zero);
        }

        a = dlamc3(one, small);
        dlamc4(ngpmin, one, lbeta[0]);
        dlamc4(ngnmin, -one, lbeta[0]);
        dlamc4(gpmin, a, lbeta[0]);
        dlamc4(gnmin, -a, lbeta[0]);
        ieee = false;

        if ((ngpmin[0] == ngnmin[0]) && (gpmin[0] == gnmin[0])) {

            if (ngpmin[0] == gpmin[0]) {
                lemin = ngpmin[0];
                // Non twos-complement machnines, no gradual underflow; e.g., VAX
            } else if ((gpmin[0] - ngpmin[0]) == 3) {
                lemin = ngpmin[0] - 1 + lt[0];
                ieee = true;
                // Non twos-complement machines, with gradual underflow; e.g, IEEE
                // standard followers
            } else {
                lemin = Math.min(ngpmin[0], gpmin[0]);

                // A guess; no known machine
                iwarn = true;
            }
        } // if ((ngpmin[0] == ngnmin[0]) && (gpmin[0] == gnmin[0]))
        else if ((ngpmin[0] == gpmin[0]) && (ngnmin[0] == gnmin[0])) {

            if (Math.abs(ngpmin[0] - ngnmin[0]) == 1) {
                lemin = Math.max(ngpmin[0], ngnmin[0]);
                // Twos-complement machines, no gradual underflow, e.g., CYBER 205
            } else {
                lemin = Math.min(ngpmin[0], ngnmin[0]);

                // A guess; no known machine
                iwarn = true;
            }
        } // else if ((ngpmin[0] == gpmin[0]) && (ngnmin[0] == gnmin[0]))
        else if ((Math.abs(ngpmin[0] - ngnmin[0]) == 1) && (gpmin[0] == gnmin[0])) {

            if ((gpmin[0] - Math.min(ngpmin[0], ngnmin[0])) == 3) {
                lemin = Math.max(ngpmin[0], ngnmin[0]) - 1 + lt[0];
                // Twos-complement machines with gradual underflow; no known machine
            } else {
                lemin = Math.min(ngpmin[0], ngnmin[0]);

                // A guess; no known machine
                iwarn = true;
            }
        } // else if ((Math.abs(ngpmin[0] - ngnmin[0]) == 1) && (gpmin[0] == gnmin[0]))
        else {
            lemin = Math.min(ngpmin[0], Math.min(ngnmin[0], Math.min(gpmin[0], gnmin[0])));

            // A guess; no known machine
            iwarn = true;
        }

        if (iwarn) {
            Preferences.debug("iwarn is true in dlamc2 emin = " + lemin + "\n");
            Preferences.debug("The emin value may be incorrect\n");
        }

        // Assume IEEE arithmetic if we found denormalized numbers above, or if
        // arithmetic seems to round in the IEEE style, determined in routine
        // dlamc1.  A true IEEE machine should have both things true; however,
        // faulty macines may have one or the other.
        ieee = ieee || lieee1[0];

        // Compute rmin by successive division by beta.  We could compute rmin as
        // base**(emin-1), but some machines underflow during this computation.

        lrmin = 1;

        for (i = 1; i <= (1 - lemin); i++) {
            lrmin = dlamc3(lrmin * rbase, zero);
        }

        // Finally, call dlamc5 to compute emax and rmax
        dlamc5(lbeta[0], lt[0], lemin, ieee, lemax, lrmax);

        beta[0] = lbeta[0];
        t[0] = lt[0];
        rnd[0] = lrnd[0];
        eps = leps;
        emin[0] = lemin;
        rmin = lrmin;
        emax[0] = lemax[0];
        rmax = lrmax[0];

        return;
    } // dlamc2
    
    /**
     * This is a port of the LAPACK version 3.2 auxiliary routine DLAMC3 Original DLAMC3 created by Univ. of Tennessee,
     * Univ. of California Berkeley, and NAG Ltd., November, 2006
     * dlamc3 is intended to force a and b to be stored prior to doing the addition of a and b, for use in
     * situations where optimizers might hold one of these in a register
     *
     * @param   a  double
     * @param   b  double
     *
     * @return  double
     */
    private double dlamc3(double a, double b) {
        double answer = a + b;

        return answer;
    }
    
    /**
     * This is a port of version 3.2 LAPACK auxiliary routine DLAMC4 Original DLAMC4 created by Univ. of Tennessee, Univ.
     * of California Berkeley, and NAG Ltd., November, 2006
     * dlamc4 is a service routine for dlamc2
     *
     * @param  emin   output int[] The minimum exponent before (gradual) underflow, computed by setting a = start and
     *                dividing by base until the previous a cannot be recovered
     * @param  start  input double The starting point for determining emin.
     * @param  base   input int The base of the machine.
     */
    private void dlamc4(int[] emin, double start, int base) {
        int i;
        double a;
        double b1;
        double b2;
        double c1;
        double c2;
        double d1;
        double d2;
        double one;
        double rbase;
        double zero;

        a = start;
        one = 1;
        rbase = one / base;
        zero = 0;
        emin[0] = 1;
        b1 = dlamc3(a * rbase, zero);
        c1 = a;
        c2 = a;
        d1 = a;
        d2 = a;

        while ((c1 == a) && (c2 == a) && (d1 == a) && (d2 == a)) {
            emin[0] = emin[0] - 1;
            a = b1;
            b1 = dlamc3(a / base, zero);
            c1 = dlamc3(b1 * base, zero);
            d1 = zero;

            for (i = 1; i <= base; i++) {
                d1 = d1 + b1;
            }

            b2 = dlamc3(a * rbase, zero);
            c2 = dlamc3(b2 / rbase, zero);
            d2 = zero;

            for (i = 1; i <= base; i++) {
                d2 = d2 + b2;
            }
        } // while ((c1 == a) && (c2 == a) && (d1 == a) && (d2 == a))

        return;
    } // dlamc4
    
    /**
     * This is a port of the version 3.2 LAPACK auxiliary routine DLAMC5 Original DLAMC5 created by Univ. of Tennessee,
     * Univ. of California Berkeley, and NAG Ltd., November, 2006
     * dlamc5 attempts to compute rmax, the largest machine floating-point number, without overflow. It assumes
     * that emax + abs(emin) sum approximately to a power of 2. It will fail on machines where this assumption does not
     * hold, for example, the Cyber 205 (emin = -28625, emax = 28718). It will also fail if the value supplied for emin
     * is too large (i.e. too close to zero), probably with overflow
     *
     * @param  beta  input int The base of floating-point arithmetic.
     * @param  p     input int The number of base beta digits in the mantissa of a floating-point value.
     * @param  emin  input int The minimum exponent before (gradual) underflow.
     * @param  ieee  input boolean A logical flag specifying whether or not the arithmetic system is thought to comply
     *               with the IEEE standard.
     * @param  emax  output int[] The largest exponent before overflow.
     * @param  rmax  output double[] The largest machine floating-point number.
     */
    private void dlamc5(int beta, int p, int emin, boolean ieee, int[] emax, double[] rmax) {
        int exbits;
        int expsum;
        int i;
        int lexp;
        int nbits;
        int trya;
        int uexp;
        double oldy = 0.0;
        double recbas;
        double y;
        double z;

        // First compute lexp and uexp, two powers of 2 that bound abs(emin).  We
        // then assume that emax + abs(emin) will sum approximately to the bound
        // that is closest to abs(emin).  (emax is the exponent of the required
        // number rmax).

        lexp = 1;
        exbits = 1;
        trya = lexp * 2;

        while (trya <= (-emin)) {
            lexp = trya;
            exbits = exbits + 1;
            trya = lexp * 2;
        } // while (trya <= (-emin))

        if (lexp == -emin) {
            uexp = lexp;
        } else {
            uexp = trya;
            exbits = exbits + 1;
        }

        // Now -lexp is less than or equal to emin, and -uexp is greater than or
        // equal to emin.  exbits is the number of bits needed to store the
        // exponent.

        if ((uexp + emin) > (-lexp - emin)) {
            expsum = 2 * lexp;
        } else {
            expsum = 2 * uexp;
        }

        // expsum is the exponent range, approximately equal to emax - emin + 1
        emax[0] = expsum + emin - 1;
        nbits = 1 + exbits + p;

        // nbits is the total number of bits needed to store a floating-point
        // number.

        if (((nbits % 2) == 1) && (beta == 2)) {

            // Either there are an odd number of bits used to store a floating-point
            // number, which is unlikely, or some bits are not used in the
            // representation of numbers, which is possible, (e.g Cray machines) or
            // the mantissa has an implicit bit, (e.g. IEEE machines, Dec VAX
            // machines), which is perhaps the most likely.  We have to assume the
            // last alternative.  If this is true, then we need to reduce emax by
            // one because there must be some way of representing zero in an
            // implicit-bit system.  On machines like the Cray, we are reducing
            // emax by one unnecessarily.
            emax[0] = emax[0] - 1;
        }

        if (ieee) {

            // Assume we are on an IEEE machine which reserves one exponent for
            // infinity and NaN
            emax[0] = emax[0] - 1;
        }

        // Now create rmax, the largest machine number, which should be equal to
        // (1.0 - beta**(-p))* beta**emax.

        // First compute 1.0 - beta**(-p), being careful that the result is less
        // than 1.0.

        recbas = 1.0 / beta;
        z = beta - 1.0;
        y = 0.0;

        for (i = 1; i <= p; i++) {
            z = z * recbas;

            if (y < 1.0) {
                oldy = y;
            }

            y = dlamc3(y, z);
        } // for (i = 1; i <= p; i++)

        if (y >= 1.0) {
            y = oldy;
        }

        // Now multiply by beta**emax to get rmax

        for (i = 1; i <= emax[0]; i++) {
            y = dlamc3(y * beta, 0.0);
        }

        rmax[0] = y;

        return;
    } // dlamc5
    
    /**
     * This is a port of the version 3.2 LAPACK auxiliary routine DLABAD Original DLABAD created by Univ. of Tennessee,
     * Univ. of California Berkeley,  Univ. of Colorado Denver, and NAG Ltd., November, 2006
     * dlabad takes as input the values computed by dlamch for underflow and overflow, and returns the square root
     * of each of these values if the log of large is sufficiently big. This routine is intended to identify machines
     * with a large exponent range, such as the Crays, and redefine the underflow and overflow limits to be the square
     * roots fo the values computed by dlamch. This subroutine is needed because dlamch does not compensate for poor
     * arithmetic in the upper half of the exponent range, as is found on a Cray.
     *
     * @param  small  input/ouptut double[] On entry, the underflow threshold as computed by dlamch. On exit, if
     *                log10(large) is sufficiently large, the square root of small, otherwise unchanged.
     * @param  large  input/output double[] On entry, the overflow threshold as computed by dlamch. On exit, if
     *                log10(large) is sufficiently large, the square root of large, otherwise unchanged.
     */
    private void dlabad(double[] small, double[] large) {

        // If it looks like we're on a Cray, take the square root of small and
        // large to avoid overflow and underflow problems.
        if ((0.4342944819 * Math.log(large[0])) > 2000.0) {
            small[0] = Math.sqrt(small[0]);
            large[0] = Math.sqrt(large[0]);
        }

        return;
    } // dlabad
    
    /**
     * This is a port of the version 3.2 LAPACK auxiliary routine DLANGE Original DLANGE created by Univ. of Tennessee,
     * Univ. of California Berkeley, Univ. of Colorado Denver, and NAG Ltd., November, 2006
     * dlange returns the value of the one norm, or the Frobenius norm, or the infinity norm, or the element of the
     * largest absolute value of a real matrix A.
     *
     * @param   norm  input char Specifies the value to be returned from dlange as:
     *                = 'M' or 'm' returns max(abs(A[i][j])). Note that this is not a matrix norm. 
     *                = '1', 'O' or 'o' returns norm1(A), where norm1 denotes the one norm of a matrix
     *                                 (maximum column sum) 
     *                = 'I' or 'i' returns normI(A), where normI denotes the infinity norm of a matrix (maximum row sum)
     *                = 'F', 'f', 'E', or 'e' returns normF(A), where normF denotes the Frobenius norm of a matrix
     *                                       (square root of sum of squares).
     * @param   m     input int The number of rows of the matrix A. m >= 0. When m = 0, dlange returns zero.
     * @param   n     input int The number of columns of the matrix A. n >= 0. When n = 0, dlange returns zero.
     * @param   A     input double[][] array of dimension (lda,n). Contains the m by n matrix A.
     * @param   lda   input int The leading dimension of the array A. lda >= max(1,m).
     * @param   work  workspace double[] of dimension max(1, lwork), where lwork >= m when norm = 'I';
     *                otherwise, work is not referenced.
     *
     * @return  double
     */
    private double dlange(char norm, int m, int n, double[][] A, int lda, double[] work) {
        int i;
        int j;
        double[] scale = new double[1];
        double[] sum = new double[1];
        double value = 0.0;
        double[] x;

        if (Math.min(m, n) == 0) {
            value = 0.0;
        } else if ((norm == 'M') || (norm == 'm')) {
            // Find max(abs(A[i][j]))

            value = 0.0;

            for (j = 0; j < n; j++) {

                for (i = 0; i < m; i++) {
                    value = Math.max(value, Math.abs(A[i][j]));
                }
            }
        } // else if ((norm == 'M') || (norm == 'm'))
        else if ((norm == 'O') || (norm == 'o') || (norm == '1')) {

            // Find norm1(A)
            value = 0.0;

            for (j = 0; j < n; j++) {
                sum[0] = 0.0;

                for (i = 0; i < m; i++) {
                    sum[0] = sum[0] + Math.abs(A[i][j]);
                }

                value = Math.max(value, sum[0]);
            } // for (j = 0; j < n; j++)
        } // else if ((norm == 'O') || (norm == 'o') || (norm == '1'))
        else if ((norm == 'I') || (norm == 'i')) {

            // Find normI(A)
            for (i = 0; i < m; i++) {
                work[i] = 0.0;
            }

            for (j = 0; j < n; j++) {

                for (i = 0; i < m; i++) {
                    work[i] = work[i] + Math.abs(A[i][j]);
                }
            } // for (j = 0; j < n; j++)

            value = 0.0;

            for (i = 0; i < m; i++) {
                value = Math.max(value, work[i]);
            }
        } // else if ((norm == 'I') || (norm == 'i'))
        else if ((norm == 'F') || (norm == 'f') || (norm == 'E') || (norm == 'e')) {

            // Find normF(A)
            scale[0] = 0.0;
            sum[0] = 1.0;
            x = new double[m];

            for (j = 0; j < n; j++) {

                for (i = 0; i < m; i++) {
                    x[i] = A[i][j];
                }

                dlassq(m, x, 1, scale, sum);
            } // for (j = 0; j < n; j++)

            value = scale[0] * Math.sqrt(sum[0]);
        } // else if ((norm == 'F') || (norm == 'f') || (norm == 'E') ||

        return value;
    } // dlange
    
    /**
     * This is a port of version 3.2 LAPACK auxiliary routine DLASSQ Original DLASSQ created by Univ. of Tennessee, Univ.
     * of California Berkeley, Univ. of Colorado Denver, and NAG Ltd., November, 2006
     * dlassq returns the values scl and smsq such that
     *  (scl**2)*smsq = x[0]**2 + x[incx]**2 + ... + x[(n-1)*incx]**2 + (scale**2)*sumsq 
     * The value of sumsq is assumed to be non-negative and scl returns the value 
     *  scl =  max(scale,abs(x[i])). 
     * scale and sumsq refer to the original supplied values in scale[] and sumsq[]. scl and smsq
     * are the returned values in scale[] and sumsq[] that overwrite the orginal values. 
     * This routine makes only one pass through the vector x.
     *
     * @param  n      input int The number of elements to be used from the vector x
     * @param  x      input double[] The vector for which a scaled sum of squares is computed, using x[0], x[incx], ...,
     *                x[(n-1)*incx]
     * @param  incx   input int The increment between successive values of the vector x. incx > 0.
     * @param  scale  input/output double[] On entry, the value scale in the equation above. On exit, scale is
     *                overwritten with scl, the scaling factor for the sum of squares
     * @param  sumsq  input/output double[] On entry, the value sumsq in the equation above. On exit, sumsq is
     *                overwritten with smsq, the basic sum of squares from which scl has been factored out.
     */
    private void dlassq(int n, double[] x, int incx, double[] scale, double[] sumsq) {
        int ix;
        double absxi;
        double ratio;

        if (n > 0) {

            for (ix = 0; ix <= ((n - 1) * incx); ix += incx) {

                if (x[ix] != 0.0) {
                    absxi = Math.abs(x[ix]);

                    if (scale[0] < absxi) {
                        ratio = scale[0] / absxi;
                        sumsq[0] = 1 + (sumsq[0] * ratio * ratio);
                        scale[0] = absxi;
                    } // if (scale[0] < absxi)
                    else { // scale[0] >= absxi
                        ratio = absxi / scale[0];
                        sumsq[0] = sumsq[0] + (ratio * ratio);
                    } // else scale[0] >= absxi
                } // if (x[ix] != 0.0)
            } // for (ix = 0; ix <= (n-1)*incx; ix += incx)
        } // if (n > 0)

        return;
    } // dlassq
    
    /**
     * This is a port of the version 3.2 LAPACK auxiliary routine DLASCL Original DLASCL created by Univ. of Tennessee,
     * Univ. of California Berkeley, Univ. of Colorado Denver, and NAG Ltd., November, 2006
     * dlascl multiplies the m by n real matrix A by the real scalar cto/cfrom. This is done without
     * over/underflow as long as the final result cto*A[i][j]/cfrom does not over/underflow. type specifies that A may
     * be full, upper triangular, lower triangular, upper Hessenberg, or banded.
     *
     * @param  type   input char type indicates the storage type of the input matrix. 
     *                = 'G': A is a full matrix. 
     *                = 'L': A is a lower triangular matrix. 
     *                = 'U': A is an upper triangular matrix. 
     *                = 'H': A is an upper Hessenberg matrix. 
     *                = 'B': A is a symmetric band matrix with lower bandwidth kL and upper bandwidth
     *                       ku and with only the lower half stored. 
     *                = 'Q': A is a symmetric band matrix with lower bandwidth kL and upper bandwidth
     *                       ku and with only the upper half stored. 
     *                = 'Z': A is a band matrix with lower bandwith kL and upper bandwidth ku
     * @param  kL     input int The lower bandwidth of A. Referenced only if type = 'B', 'Q', or 'Z'.
     * @param  ku     input int The upper bandwidth of A. Referenced only if type = 'B', 'Q', or 'Z'.
     * @param  cfrom  input double
     * @param  cto    input double The matrix A is multiplied by cto/cfrom. A[i][j] is computed without over/underflow
     *                if the final result cto*A[i][j]/cfrom can be represented without over/underflow. cfrom must be
     *                nonzero.
     * @param  m      input int The number of rows of the matrix A. m >= 0.
     * @param  n      input int The number of columns of the matrix A. n >= 0.
     * @param  A      input/output double[][] of dimension lda by n. The matrix to be multiplied by cto/cfrom.
     * @param  lda    input int The leading dimension of the array A. lda >= max(1,m).
     * @param  info   output int[] 
     *                = 0: successful exit 
     *                < 0: If info = -i, the i-th argument had an illegal value
     */
    private void dlascl(char type, int kL, int ku, double cfrom, double cto, int m, int n, double[][] A, int lda,
                        int[] info) {
        boolean done;
        int i;
        int itype;
        int j;
        int k1;
        int k2;
        int k3;
        int k4;
        double bignum;
        double cfrom1;
        double cfromc;
        double cto1;
        double ctoc;
        double mul;
        double smlnum;

        // Test the input arguments
        info[0] = 0;

        if ((type == 'G') || (type == 'g')) {
            itype = 0;
        } else if ((type == 'L') || (type == 'l')) {
            itype = 1;
        } else if ((type == 'U') || (type == 'u')) {
            itype = 2;
        } else if ((type == 'H') || (type == 'h')) {
            itype = 3;
        } else if ((type == 'B') || (type == 'b')) {
            itype = 4;
        } else if ((type == 'Q') || (type == 'q')) {
            itype = 5;
        } else if ((type == 'Z') || (type == 'z')) {
            itype = 6;
        } else {
            itype = -1;
        }

        if (itype == -1) {
            info[0] = -1;
        } else if ((cfrom == 0.0) || (Double.isNaN(cfrom))) {
            info[0] = -4;
        } else if (Double.isNaN(cto)) {
            info[0] = -5;
        } else if (m < 0) {
            info[0] = -6;
        } else if ((n < 0) || ((itype == 4) && (n != m)) || ((itype == 5) && (n != m))) {
            info[0] = -7;
        } else if ((itype <= 3) && (lda < Math.max(1, m))) {
            MipavUtil.displayError("itype = " + itype + " m = " + m + " lda = " + lda);
            info[0] = -9;
        } else if (itype >= 4) {

            if ((kL < 0) || (kL > Math.max(m - 1, 0))) {
                info[0] = -2;
            } else if ((ku < 0) || (ku > Math.max(n - 1, 0)) || (((itype == 4) || (itype == 5)) && (kL != ku))) {
                info[0] = -3;
            } else if (((itype == 4) && (lda < (kL + 1))) || ((itype == 5) && (lda < (ku + 1))) ||
                           ((itype == 6) && (lda < ((2 * kL) + ku + 1)))) {
                info[0] = -9;
            }
        } // else if (itype >= 4)

        if (info[0] != 0) {
            MipavUtil.displayError("Error dlascl had info = " + info[0]);
            Preferences.debug("Error dlascl had info = " + info[0] + "\n");
            return;
        }

        // Quick return if possible
        if ((n == 0) || (m == 0)) {
            return;
        }

        // Get machine parameters
        smlnum = dlamch('S');
        bignum = 1.0 / smlnum;

        cfromc = cfrom;
        ctoc = cto;

        do {
            cfrom1 = cfromc * smlnum;
            if (cfrom1 == cfromc) {
                // cfromc is an infinity.  Multiply by a correctly signed zero for finite ctoc,
                // or a NaN if ctoc is infinite
                mul = ctoc/cfromc;
                done = true;
                cto1 = ctoc;
            } // if (cfrom1 == cfromc)
            else {
                cto1 = ctoc / bignum;
                if (cto1 == ctoc) {
                    // ctoc is either 0 or an infinity.  In both cases, ctoc itself
                    // serves as the correct multiplication factor
                    mul = ctoc;
                    done = true;
                    cfromc = 1.0;
                }
                else if ((Math.abs(cfrom1) > Math.abs(ctoc)) && (ctoc != 0.0)) {
                    mul = smlnum;
                    done = false;
                    cfromc = cfrom1;
                } else if (Math.abs(cto1) > Math.abs(cfromc)) {
                    mul = bignum;
                    done = false;
                    ctoc = cto1;
                } else {
                    mul = ctoc / cfromc;
                    done = true;
                }
            }

            if (itype == 0) {

                // Full matrix
                for (j = 0; j < n; j++) {

                    for (i = 0; i < m; i++) {
                        A[i][j] = A[i][j] * mul;
                    }
                }
            } // if (itype == 0)
            else if (itype == 1) {

                // Lower triangular matrix
                for (j = 0; j < n; j++) {

                    for (i = j; i < m; i++) {
                        A[i][j] = A[i][j] * mul;
                    }
                }
            } // else if (itype == 1)
            else if (itype == 2) {

                // Upper triangular matrix
                for (j = 0; j < n; j++) {

                    for (i = 0; i <= Math.min(j, m - 1); i++) {
                        A[i][j] = A[i][j] * mul;
                    }
                }
            } // else if (itype == 2)
            else if (itype == 3) {

                // Upper Hessenberg matrix
                for (j = 0; j < n; j++) {

                    for (i = 0; i <= Math.min(j + 1, m - 1); i++) {
                        A[i][j] = A[i][j] * mul;
                    }
                }
            } // else if (itype == 3)
            else if (itype == 4) {

                // Lower half of a symmetric band matrix
                k3 = kL + 1;
                k4 = n + 1;

                for (j = 0; j < n; j++) {

                    for (i = 0; i <= Math.min(k3 - 1, k4 - j - 2); i++) {
                        A[i][j] = A[i][j] * mul;
                    }
                }
            } // else if (itype == 4)
            else if (itype == 5) {

                // upper half of a symmetric band matrix
                k1 = ku + 2;
                k3 = ku + 1;

                for (j = 0; j < n; j++) {

                    for (i = Math.max(k1 - j - 2, 0); i <= (k3 - 1); i++) {
                        A[i][j] = A[i][j] * mul;
                    }
                }
            } // else if (itype == 5)
            else if (itype == 6) {

                // Band matrix
                k1 = kL + ku + 2;
                k2 = kL + 1;
                k3 = (2 * kL) + ku + 1;
                k4 = kL + ku + 1 + m;

                for (j = 0; j < n; j++) {

                    for (i = Math.max(k1 - j - 2, k2 - 1); i <= Math.min(k3 - 1, k4 - j - 2); i++) {
                        A[i][j] = A[i][j] * mul;
                    }
                }
            } // else if (itype == 6)
        } while (!done);

        return;
    } // dlascl
    
    /**
     * This is a port of version 3.2 auxiliary routine DLASET. Original DLASET created by Univ. of Tennessee, Univ. of
     * California Berkeley, Univ. of Colorado Denver, and NAG Ltd., November, 2006
     * dlaset initializes an m-by-n matrix A to beta on the diagonal and alpha on the offdiagonals.
     *
     * @param  uplo   input char Specifies the part of the matrix to be set. 
     *                = 'U': Upper triangular part is set; the strictly lower triangular part of A is not changed. 
     *                = 'L': Lower triangular part is set; the strictly upper triangular part of A is not changed.
     *                Otherwise: All of the matrix A is set.
     * @param  m      input int The number of rows of the matrix A. m >= 0.
     * @param  n      input int The number of columns of the matrix A. n >= 0.
     * @param  alpha  input double The constant to which the offdiagonal elements are to be set.
     * @param  beta   input double The constant to which the diagonal elements are to be set.
     * @param  A      input/output double[][] of dimension lda by n. On exit, the leading m-by-n submatrix of A is set
     *                as follows: 
     *                If uplo = 'U', A(i,j) = alpha, 0 <= i <= j-1, 0 <= j <= n-1,
     *                If uplo = 'L', A(i,j) = alpha, j+1 <= i <= m-1, 0 <= j <= n-1, 
     *                Otherwise, A(i,j) = alpha, 0 <= i <= m-1, 0 <= j <= n-1, i!= j
     *                and, for all uplo, A(i,i) = beta, 0 <= i <= min(m-1,n-1).
     * @param  lda    input int The leading dimension of the array A. lda >= max(1,m).
     */
    private void dlaset(char uplo, int m, int n, double alpha, double beta, double[][] A, int lda) {
        int i;
        int j;

        if ((uplo == 'U') || (uplo == 'u')) {

            // Set the srictly upper triangular or trapezoidal part of the array to
            // alpha.
            for (j = 1; j < n; j++) {

                for (i = 0; i <= Math.min(j - 1, m - 1); i++) {
                    A[i][j] = alpha;
                }
            }
        } // if ((uplo == 'U') || (uplo == 'u'))
        else if ((uplo == 'L') || (uplo == 'l')) {

            // Set the strictly lower triangular or trapezoidal part of the array to
            // alpha.
            for (j = 0; j <= Math.min(m - 1, n - 1); j++) {

                for (i = j + 1; i <= (m - 1); i++) {
                    A[i][j] = alpha;
                }
            }
        } // else if ((uplo == 'L') || (uplo == 'l'))
        else {

            // Set the leading m-by-n submatrix to alpha
            for (j = 0; j < n; j++) {

                for (i = 0; i < m; i++) {
                    A[i][j] = alpha;
                }
            }
        } // else

        // Set the first min(m,n) diagonal elements to beta
        for (i = 0; i <= Math.min(m - 1, n - 1); i++) {
            A[i][i] = beta;
        }

        return;
    } // dlaset

}