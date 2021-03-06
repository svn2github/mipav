package gov.nih.mipav.model.structures.jama;


/**
 * Cholesky Decomposition.
 *
 * <P>For a symmetric, positive definite matrix A, the Cholesky decomposition is an lower triangular matrix L so that A
 * = L*L'.</P>
 *
 * <P>If the matrix is not symmetric or positive definite, the constructor returns a partial decomposition and sets an
 * internal flag that may be queried by the isSPD() method.</P>
 */

public class CholeskyDecomposition implements java.io.Serializable {

    /* ------------------------
       Class variables
     * ------------------------ */

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = -7156296866085202145L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /**
     * Symmetric and positive definite flag.
     *
     * @serial  is symmetric and positive definite flag.
     */
    private boolean isspd;

    /* ------------------------
       Constructor
     * ------------------------ */

    /**
     * Array for internal storage of decomposition.
     *
     * @serial  internal array storage.
     */
    private double[][] L;

    /**
     * Row and column dimension (square matrix).
     *
     * @serial  matrix dimension.
     */
    private int nrc;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Cholesky algorithm for symmetric and positive definite matrix.
     *
     * @param   Arg  Square, symmetric matrix.
     *
     * @return  Structure to access L and isspd flag.
     */

    public CholeskyDecomposition(Matrix Arg) {

        // Initialize.
        double[][] A = Arg.getArray();
        nrc = Arg.getRowDimension();
        L = new double[nrc][nrc];
        isspd = (Arg.getColumnDimension() == nrc);

        // Main loop.
        for (int j = 0; j < nrc; j++) {
            double[] Lrowj = L[j];
            double d = 0.0;

            for (int k = 0; k < j; k++) {
                double[] Lrowk = L[k];
                double s = 0.0;

                for (int i = 0; i < k; i++) {
                    s += Lrowk[i] * Lrowj[i];
                }

                Lrowj[k] = s = (A[j][k] - s) / L[k][k];
                d = d + (s * s);
                isspd = isspd & (A[k][j] == A[j][k]);
            }

            d = A[j][j] - d;
            isspd = isspd & (d > 0.0);
            L[j][j] = Math.sqrt(Math.max(d, 0.0));

            for (int k = j + 1; k < nrc; k++) {
                L[j][k] = 0.0;
            }
        }
    }

    /* ------------------------
       Temporary, experimental code.
     * ------------------------ *\
    
       \** Right Triangular Cholesky Decomposition.
       <P>
       For a symmetric, positive definite matrix A, the Right Cholesky
       decomposition is an upper triangular matrix R so that A = R'*R.
       This constructor computes R with the Fortran inspired column oriented
       algorithm used in LINPACK and MATLAB.  In Java, we suspect a row oriented,
       lower triangular decomposition is faster.  We have temporarily included
       this constructor here until timing experiments confirm this suspicion.
       *\
    
       \** Array for internal storage of right triangular decomposition. **\
       private transient double[][] R;
    
       \** Cholesky algorithm for symmetric and positive definite matrix.
       @param  A           Square, symmetric matrix.
       @param  rightflag   Actual value ignored.
       @return             Structure to access R and isspd flag.
       *\
    
       public CholeskyDecomposition (Matrix Arg, int rightflag) {
          // Initialize.
          double[][] A = Arg.getArray();
          nrc = Arg.getColumnDimension();
          R = new double[nrc][nrc];
          isspd = (Arg.getColumnDimension() == nrc);
          // Main loop.
          for (int j = 0; j < nrc; j++) {
             double d = 0.0;
             for (int k = 0; k < j; k++) {
                double s = A[k][j];
                for (int i = 0; i < k; i++) {
                   s = s - R[i][k]*R[i][j];
                }
                R[k][j] = s = s/R[k][k];
                d = d + s*s;
                isspd = isspd & (A[k][j] == A[j][k]);
             }
             d = A[j][j] - d;
             isspd = isspd & (d > 0.0);
             R[j][j] = Math.sqrt(Math.max(d,0.0));
             for (int k = j+1; k < nrc; k++) {
                R[k][j] = 0.0;
             }
          }
       }
    
       \** Return upper triangular factor.
       @return     R
       *\
    
       public Matrix getR () {
          return new Matrix(R,nrc,nrc);
       }
    
    \* ------------------------
       End of temporary code.
     * ------------------------ */

    /* ------------------------
       Public Methods
     * ------------------------ */

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Return triangular factor.
     *
     * @return  L
     */

    public Matrix getL() {
        return new Matrix(L, nrc, nrc);
    }

    /**
     * Is the matrix symmetric and positive definite?
     *
     * @return  true if A is symmetric and positive definite.
     */

    public boolean isSPD() {
        return isspd;
    }

    /**
     * Solve A*X = B.
     *
     * @param      B  A Matrix with as many rows as A and any number of columns.
     *
     * @return     X so that L*L'*X = B
     *
     * @exception  IllegalArgumentException  Matrix row dimensions must agree.
     * @exception  RuntimeException          Matrix is not symmetric positive definite.
     */

    public Matrix solve(Matrix B) {

        if (B.getRowDimension() != nrc) {
            throw new IllegalArgumentException("Matrix row dimensions must agree.");
        }

        if (!isspd) {
            throw new RuntimeException("Matrix is not symmetric positive definite.");
        }

        // Copy right hand side.
        double[][] X = B.getArrayCopy();
        int nx = B.getColumnDimension();

        // Solve L*Y = B;
        for (int k = 0; k < nrc; k++) {

            for (int i = k + 1; i < nrc; i++) {

                for (int j = 0; j < nx; j++) {
                    X[i][j] -= X[k][j] * L[i][k];
                }
            }

            for (int j = 0; j < nx; j++) {
                X[k][j] /= L[k][k];
            }
        }

        // Solve L'*X = Y;
        for (int k = nrc - 1; k >= 0; k--) {

            for (int j = 0; j < nx; j++) {
                X[k][j] /= L[k][k];
            }

            for (int i = 0; i < k; i++) {

                for (int j = 0; j < nx; j++) {
                    X[i][j] -= X[k][j] * L[k][i];
                }
            }
        }

        return new Matrix(X, nrc, nx);
    }
}
