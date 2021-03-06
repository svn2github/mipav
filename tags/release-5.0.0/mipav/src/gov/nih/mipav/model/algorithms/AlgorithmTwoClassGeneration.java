package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.structures.jama.*;

import Jama.*;

import gov.nih.mipav.view.*;

import java.io.*;

/**
 *      This program generates type 1 and type 2 objects in different spatial patterns and tests to see if
 *      randomness if rejected for segregation or association.  The objects may be points or circles of
 *      a specified radius. 
 *
 *      Segregation occurs when members of a given class have nearest neighbors that are more frequently of the
 *      same class and less frequently of the other class than would be expected if there were randomness in the
 *      nearest neighbor structure.  Association occurs when members of a given class have nearest neighbors that
 *      are less frequently of the same class and more frequently of the other class than would be expected if 
 *      there were randomness in the nearest neighbor structure.
 *      
 *      Nearest neighbor contingency tables are constructed using the nearest neighbor frequencies of classes.
 *      Below is the nearest neighbor contingency table used for the 2 class case in this program:
 *                                                NN class                                       Sum
 *                                                Class 1                Class 2
 *      Base class            Class 1             N11                    N12                     n1
 *                            Class 2             N21                    N22                     n2
 *                            
 *                            Sum                 C1                     C2                      n
 *                            
 *      This program performs the following tests: Dixon's overall test of segregation, Dixon's cell-specific
 *      tests of segregation, Ceyhan's overall test of segregation, and Ceyhan's cell-specific tests of 
 *      segregation.  Dixon's overall test of segregation generates CD which follows a chi squared distribution
 *      with 1 degree of freedom.  Spatial randomness is rejected if CD has a value greater than the chi squared
 *      cumulative frequency value of 0.95.  In Dixon's cell-specific tests ZijD for i = 1,2 j = 1,2 are calculated
 *      from (Nij - expected value(Nij))/sqrt(variance Nij).  ZijD has a normal distribution with 0 mean and a 
 *      standard deviation of 1.  2 sided tests are used with one tail end of the curve indicating segregation and
 *      the other tail end of the curve indicating association.  Cutoff is at 0.025 at one end of the tail and at
 *      0.975 at the other end of the tail.  Ceyhan's overall test of segregation generates CN
 *      which follows a chi squared distribution with 1 degree of freedom.  Spatial randomness if rejected if CN
 *      has a value greater than the chi squared cumulative frequency value of 0.95. In Ceyhan's cell-specific
 *      tests ZijN with i = 1,2 j = 1,2 are calculated from Tij/sqrt(variance Tij).  ZijN has a normal distribution
 *      with 0 mean and a standard deviation of 1.
 *      
 *      The generalized inverse is what should be used in calculating CD and CN.  Results are:
 *                        Ceyhan             myself inverse        generalized inverse     generalized inverse
 *                                                                 Rust et al.             LAPACK dgelss  rcond = 1.0E-4
 *      2 class CD        19.67              19.66                 19.67                   19.67
 *      2 class CN        13.11              24.59                 19.67                   13.09
 *      5 class CD       275.64             279.92                275.64                  275.64
 *      5 class CN       263.10             641.28                275.64                  263.07
 *      generalized inverse                 generalized inverse
 *      LAPACK dgelss rcond = 1.0E-6        LAPACK dgelss rcond = 1.0E-7
 *      19.67                               19.67
 *      19.67                               19.67
 *      275.64                              275.64
 *      263.07                              275.64
 *      For the pinv routine using the LAPACK dgelss the singular values of the generalized inverse are stored
 *      in array s in decreasing order.  s[0] has the largest value.  rcond is used to determine the effective
 *      rank of the generalized inverse.  Singular values s[i] <= rcond * s[0] are treated as zero.
 *      So if the generalized inverse is used, CN = CD or Ceyhan's overall test of segregation produces the same
 *      result as Dixon's overall test of segregation if small singular values are not treated as zero.
 *      I have used the simple generalized inverse algorithm of  B. Rust, W. R. Burrus, and C. Schneeberger and
 *      the routine pinv to call the LAPACK dgelss routine to generate a generalized inverse.
 *      The generalized inverse algorithm of Shayle Searle in Matrix Algebra Useful for Statistics cited as a
 *      reference by Ceyhan is too vague to implement.
 *      
 *      For the 2 class example the matrix whose generalized inverse has to be obtained for Ceyhan's overall
 *      statistic is very close to being a singular matrix.  If the last decimal places are rounded off,
 *      it becomes a matrix of rank 1:
 *      sigma = 
15.822838031444995 -15.822373066537992 -15.822838031444991 15.822373066537995 
-15.822373066537992 15.822568995146952 15.822373066537994 -15.822568995146947 
-15.822838031444991 15.822373066537994 15.822838031444991 -15.822373066537992 
15.822373066537995 -15.822568995146947 -15.822373066537992 15.822568995146941 
inverse = 
-3530.592924542394 1.8764998447293003E14 -2521.8611174999132 1.8764998447393875E14 
1.8764998447293E14 -1.2509998964733105E14 1.8764998447124878E14 -1.2509998964901231E14 
-2521.8541666666665 1.8764998447124878E14 0.0 1.8764998447377066E14 
1.8764998447393878E14 -1.250999896490123E14 1.8764998447377066E14 -1.2509998964918044E14 
sigma * inverse = 
1.0 0.25 0.0 0.0 
0.5 -0.5 0.5 -0.25 
0.0 -0.75 1.5 0.5 
-0.5 0.75 -0.5 1.25 
generalized inverse = 
378.27715316760157 378.2724690177479 -378.2771531615012 -378.27246900758036 
378.27246901978145 378.2835851434381 -378.2724690136809 -378.2835851332706 
-378.27715316777284 -378.2724690179191 378.27715316167235 378.27246900775174 
-378.27246901181877 -378.2835851354754 378.2724690057183 378.28358512530787 
sigma * generalized inverse = 
0.500000000001819 1.8189894035458565E-12 -0.499999999998181 4.547473508864641E-12 
6.366462912410498E-12 0.500000000007276 -9.094947017729282E-13 -0.5000000000009095 
-0.5000000000009095 -1.8189894035458565E-12 0.4999999999972715 -5.4569682106375694E-12 
-9.094947017729282E-13 -0.500000000001819 -2.7284841053187847E-12 0.4999999999954525 
 *      Dear Dr Gandler,
thanks for bringing this issue up to my attention...
if you look at the definition of T_{ij}, the covariance expression in "Overall and pairwise segregation tests based on nearest neighbor  contingency tables:" should be the correct one, i will fix the technical report and then repost on arXiv...

have a nice evening,
E.
 

"Gandler, William (NIH/CIT) [E]" <ilb@mail.nih.gov> wrote on 22.07.2009 21:43:
> Dear Professor Elvan Ceyhan:
> 
>   In Overall and pairwise segregation tests based on nearest neighbor 
> contingency tables:
>  case 2 for overall test of segregation:
> Cov[Tii, Tkl] = Cov[Nii, Nkl] - nk*Cov[Nii, Cl]/(n - 1) - (ni - 
> 1)*Cov[Nkl, Ci]/(n-1) + (ni-1)*nk*Cov[Ci, Cl]/(n-1)**2 case 4:
> Cov[Tij, Tkl] = Cov[Nij, Nkl] - nk*Cov[Nij, Cl]/(n - 1) - ni*Cov[Nkl, 
> Cj]/(n - 1) + ni*nk*Cov[Cj, Cl]/(n - 1)**2 while in Technical Report 
> #KU-EC_08-6: New Tests of Spatial Segregation Based on Nearest 
> Nieghbor Contingency Tables:
> Case 2:
> Cov[Tij, Tkl] = Cov[Nii, Nkl] - nl*Cov[Nii, Cl]/(n - 1) - (ni - 
> 1)*Cov[Nkl, Ci]/(n - 1)  + (ni -1)*nl*Cov[Ci, Cl]/(n - 1)**2 Case 4:
> Cov[Tij, Tkl] = Cov[Nij, Nkl] - nl*Cov[Nij, Cl]/(n - 1) - ni*Cov[Nkl, 
> Cj]/(n - 1) + ni*nl*Cov[Cj, Cl]/(n - 1)**2
> 
> so in the second and fourth terms nk in the first paper becomes nl in 
> the second paper.
> 
>                                                                         
>                                         Sincerely,
> 
>                                                                         
>                                  William Gandler
 * 
 References :
 1.) "Overall and pairwise segregation tests based on nearest neighbor contigency tables" by Elvan
 Ceyhan, Computational Statistics and Data Analysis, 53, 2009, pp. 2786-2808.
 2.) Technical Report #KU-EC-08-6: New Tests of Spatial Segregation Based on Nearest Neighbor
 Contingency Tables by Elvan Ceyhan, September 18, 2008
 3.) "Nearest-neighbor contingency table analysis of spatial segregation for several species" 
 by Philip M. Dixon, Ecoscience, Vol. 9, No. 2, 2002, pp. 142-151.
 4.) "Spatial Segregation Using a Nearest-Neighbor Contingency Table" by Philip Dixon, Ecology,
 Vol. 75, No. 7, Oct., 1994, pp. 1940-1948.
 */
public class AlgorithmTwoClassGeneration extends AlgorithmBase {
    
    // numParents are generated with a uniform random distribution over the square.
    // Type 1 offspring are generated with (numOffspring1/numParents) offspring from
    // each parent in the set with a radially symmetric Gaussian distribution whose
    // standard deviation = normalized standard deviation * (xDim - 1).  Type 2
    // offspring are generated with (numOffspring2/numParents) offspring from each
    // parent in the same set used for the type 1 offspring with the same radially 
    // symmetric Gaussian distribution.  For complete spatial randomness a rejection
    // rate in the test for complete spatial randomness of 0.05 would be expected.
    // In this case the rejection rates are slightly (but significantly) higher than
    // 0.05, so the 2 classes are slightly segregated.
    // Segregation increases as normalizedStdDev decreases.
    public static final int FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS = 1;
    
    // A first set of numParents are generated with a uniform random distribution over the square.
    // Type 1 offspring are generated with (numOffspring1/numParents) offspring from
    // each parent in the first set with a radially symmetric Gaussian distribution whose
    // standard deviation = normalized standard deviation * (xDim - 1).  A second
    // set of numParents are generated with a uniform distribution over the square.
    // Type 2 offspring are generated with (numOffspring2/numParents) offspring from each
    // parent in the second set with the same radially symmetric Gaussian distribution.
    // The 2 classes are strongly segregated.
    // Segregation increases as normalizedStdDev decreases.
    public static final int FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS = 2;
    
    // numParents are generated with a uniform random distribution over the square.
    // Type 1 offspring are generated from a randomly chosen parent
    // in the set with a radially symmetric Gaussian distribution whose
    // standard deviation = normalized standard deviation * (xDim - 1).  Type 2
    // offspring are generated from a randomly chosen
    // parent in the same set used for the type 1 offspring with the same radially 
    // symmetric Gaussian distribution.
    // The 2 classes satisfy randomness in the nearest neighbor structure.
    public static final int RANDOM_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS = 3;
    
    // A first set of numParents are generated with a uniform random distribution over the square.
    // Type 1 offspring are generated from a randomly chosen
    // parent in the first set with a radially symmetric Gaussian distribution whose
    // standard deviation = normalized standard deviation * (xDim - 1).  A second
    // set of numParents are generated with a uniform distribution over the square.
    // Type 2 offspring are generated from a randomly chosen
    // parent in the second set with the same radially symmetric Gaussian distribution.
    // The 2 classes are strongly segregated.
    // Segregation increases as normalizedStdDev decreases.
    public static final int RANDOM_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS = 4;
    
    // Put 100 * parentPoissonNormalizedMean points into an area 100 times as large
    // as the actual image area.  Then each parent point is replaced with a random
    // cluster of offspring.  The number of objects inside each cluster are random with
    // a Poisson distribution whose mean = numOffspring1/parentPoissonNormalizedMean for
    // type 1 objects and whose mean = numOffspring2/parentPoissonNormalizedMean for type 2 objects.
    // The points are placed independently and uniformly inside a disc with a 
    // disc radius = normalized disc radius * (xDim - 1).  Only consider those objects
    // falling inside the actual image area.  One set of parent points is used for
    // both offspring.
    // The 2 classes satisfy randomness in the nearest neighbor structure.
    public static final int MATERN_SAME_PARENTS = 5;
    
    // Put 100 * parentPoissonNormalizedMean points into an area 100 times as large
    // as the actual image area.  Then each parent point is replaced with a random
    // cluster of type 1 offspring.  The number of type 1 objects inside each cluster are random with
    // a Poisson distribution whose mean = numOffspring1/parentPoissonNormalizedMean.
    // The type 1 offspring are placed independently and uniformly inside a disc with a 
    // disc radius = normalized disc radius * (xDim - 1).  Only consider those points
    // falling inside the actual image area.  A second set of 100 * parentPoissonNormalizedMean
    // parent points is generated for type 2 objects.  Each second set parent point is replaced with
    // a cluster of type 2 offspring.  The number of type 2 objects inside each cluster are random with
    // a Poisson distribution whose mean = numOffspring2/parentPoissonNormalizedMean.  The type 2 
    // offsprihg are placed independently and uniformly inside a disc with a disc radius =
    // normalized disc radius * (xDim - 1).  Only consider those points falling inside the
    // actual image area.
    // The classes are strongly segregated.
    // As the normalizedDiscRadius increases, the level of segregation decreases.
    public static final int MATERN_DIFFERENT_PARENTS = 6;
    
    // numPoints1 type 1 objects are generated with a uniform random distribution over the square.
    // numPoints2 type 2 objects are generated with a uniform random distribution over the square.
    // For type 1 objects the maximum value maxIntensity = sqrt(xCenter/(xDim - 1) + yCenter/(yDim - 1)) is found.
    // For each type 1 object the retention probability = sqrt(xCenter/(xDim - 1) + yCenter/(yDim - 1)) is calculated.
    // A random number between 0 and 1 is generated.  If the random number is <= retention probability, the type 1
    // object is retained.  Otherwise, the type 1 object is deleted.  If inhomogeneous = SQRT_X_PLUS_Y, then the
    // same process is used for deciding which type 2 objects to delete.  If inhomogeneous = SQRT_X_TIMES_Y, then
    // intensity = sqrt((xCenter * yCenter)/((xDim - 1) * (yDim - 1))) is used for deciding object 2 retention.
    // If inhomogeneous = ABS_X_MINUS_Y, then abs((xCenter/(xDim - 1) - yCenter/(yDim - 1)), is used for deciding
    // object 2 retention.
    // For the same or similar density functions, SQRT_X_PLUS_Y and SQRT_X_TIMES_Y, the 2 classes show randomness
    // in the nearest neighbor structure.  For very different density functions with ABS_X_MINUS_Y, moderate
    // segregation is observed between the 2 classes.
    public static final int INHOMOGENEOUS_POISSON = 7;
    
    // Type 1 objects are generated uniformly over the square (0, (1 - segregation)*(xDim - 1)) by
    // (0, (1 - segregation)*(yDim - 1)).  Type 2 objects are generated uniformly over the square
    // (segregation*(xDim - 1), xDim - 1) by (segregation*(yDim-1), yDim - 1).
    public static final int SEGREGATION_ALTERNATIVE = 8;
    
    // Type 1 objects are generated with a uniform random distribution over the square.
    // For each type 2 object a type 1 object is randomly selected, a distance ry is generated from a 
    // uniform random distribution of numbers from 0 to discRadius, and an angle is generated from a
    // uniform random distribution of angles from 0 to 2*PI.  The x center of the type 2 object is placed
    // at the x location of the parent type 1 object + ry * cosine(angle).  The y center of the type 2
    // object is placed at the y location of the parent type 1 object + ry * sine(angle).
    public static final int ASSOCIATION_ALTERNATIVE = 9;
    
    public static final int SQRT_X_PLUS_Y = 1;
    
    public static final int SQRT_X_TIMES_Y = 2;
    
    public static final int ABS_X_MINUS_Y = 3;
    

    //~ Instance fields ------------------------------------------------------------------------------------------------
    
    // Circle radius
    private int radius;
    
    private int process;
    
    // number of parents
    private int numParents;
    
    private int numOffspring1;
    
    private int numOffspring2;
    
    private double normalizedStdDev;
    
    private double parentPoissonNormalizedMean;
    
    private double normalizedDiscRadius;
    
    private int numPoints1;
    
    private int numPoints2;
    
    private int inhomogeneous;
    
    private double segregation;
    
    // Test with NNCT for Pielou's data, shown in Table 3 of "Overall and pairwise segregation tests based on nearest
    // neighbor contingency tables"  In agreement with Ceyhan except for Ceyhan's overall test of segregation for which
    // he calculates 13.11 and I calculate 19.67 for generalized inverse.  19.67 is also the
    // value I calculate for Dixon's overall test.
    private boolean selfTest1 = false;
    
    // Test with NNCT for 5 class Good and Whipple swamp tree data shown in Table 4 of "Overall and pairwise 
    // segregation tests based on nearest neighbor contingency tables".  In agreement with Ceyhan on specific tests.
    // For Dixon's overall test he calculates 275.64 and I calculate 275.64 for the generalized inverse.  
    // For Ceyhan's overall test he calculates 263.10 and I calculate 275.64 for generalized inverse. 
    private boolean selfTest2 = false;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * AlgorithmTwoClassGeneration - default constructor.
     */
    public AlgorithmTwoClassGeneration() { }

    /**
     * AlgorithmTwoClassGeneration.
     *
     * @param  srcImg   Blank source image in which circles will be drawn
     * @param  radius   Circle radius
     * @param  process 
     * @param  numParents Number of parents
     * @param  numOffspring1
     * @param  numOffspring2
     * @param  normalizedStdDev
     * @param  parentPoissonNormalizedMean
     * @param  normalizedDiscRadius
     * @param  numPoints1
     * @param  numPoints2
     * @param  inhomogeneous
     * @param  segregation
     */
    public AlgorithmTwoClassGeneration(ModelImage srcImage, int radius, int process, int numParents, 
            int numOffspring1, int numOffspring2, double normalizedStdDev,
            double parentPoissonNormalizedMean, double normalizedDiscRadius,
            int numPoints1, int numPoints2,
            int inhomogeneous, double segregation) {
        super(null, srcImage);
        this.radius = radius;
        this.process = process;
        this.numParents = numParents;
        this.numOffspring1 = numOffspring1;
        this.numOffspring2 = numOffspring2;
        this.normalizedStdDev = normalizedStdDev;
        this.parentPoissonNormalizedMean = parentPoissonNormalizedMean;
        this.normalizedDiscRadius = normalizedDiscRadius;
        this.numPoints1 = numPoints1;
        this.numPoints2 = numPoints2;
        this.inhomogeneous = inhomogeneous;
        this.segregation = segregation;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * finalize -
     */
    public void finalize() {
        super.finalize();
    }
    
    /**
     * Starts the program.
     */
    public void runAlgorithm() {
        final int SAME = 1;
        final int DIFFERENT = 2;
        int xDim;
        int yDim;
        byte mask[];
        int x;
        int y;
        int yDistSquared;
        int xDistSquared;
        int radiusSquared;
        int xMaskDim;
        int yMaskDim;
        int distSquared;
        int lowestDistSquared;
        int i;
        int j;
        int attempts;
        boolean found;
        int buffer[];
        int length;
        int xCenter = radius;
        int yCenter = radius;
        /** Reference to the random number generator. */
        RandomNumberGen randomGen;
        double stdDev;
        int maskBytesSet;
        Statistics stat;
        double degreesOfFreedom;
        double chiSquaredPercentile[] = new double[1];
        double percentile[] = new double[1];
        byte parentX[] = null;
        int xParentXLocation[];
        int xParentYLocation[];
        int xParentsPlaced;
        byte parentY[] = null;
        int yParentXLocation[] = null;
        int yParentYLocation[] = null;
        int yParentsPlaced = 0;
        int parentXLocation;
        int parentYLocation;
        int parentNumber;
        double angle;
        double distance;
        int xCircleXCenter[] = null;
        int xCircleYCenter[] = null;
        int yCircleXCenter[] = null;
        int yCircleYCenter[] = null;
        int offspring1Drawn = 0;
        int offspring2Drawn = 0;
        int offspring1PerParent;
        int offspring2PerParent;
        int discRadius;
        int expandedXDim;
        int expandedYDim;
        int expandedLength;
        int discRadiusSquared;
        int xDiscMaskDim;
        int yDiscMaskDim;
        byte discMask[];
        int xDiscMask[];
        int yDiscMask[];
        int discMaskBytesSet;
        int paddedBuffer[];
        double offspring1PoissonMean;
        double offspring2PoissonMean;
        int pointsInCluster;
        double poissonValues[];
        int events;
        double gain;
        double offset;
        int discIndex;
        int parentsPlaced;
        int originalCirclesCreated1;
        int originalCirclesCreated2;
        double intensity;
        double maxIntensity;
        int xCircleOrigXCenter[];
        int xCircleOrigYCenter[];
        int yCircleOrigXCenter[];
        int yCircleOrigYCenter[];
        int xNumber;
        double ry;
        double retentionProbability;
        double cumulativeProb;
        boolean retainCircle[];
        double NN1Distance[];
        byte NN1Type[];
        double NN2Distance[];
        byte NN2Type[];
        int NN1Neighbor[];
        int NN2Neighbor[];
        int N11;
        int N12;
        int N21;
        int N22;
        int C1;
        int C2;
        long n1;
        long n2;
        long n;
        double EN11;
        double EN12;
        double EN21;
        double EN22;
        // A (base, NN) pair (X,Y) is reflexive if (Y,X) is also a (base, NN) pair.
        // R is twice the number of reflexive pairs
        int R;
        // Q is the number of points with shared NNs, which occurs when two or more points share a NN.
        // Then Q = 2*(Q2 + 3*Q3 + 6*Q4 + 10*Q5 * 15*Q6), where Qk is the number of points that serve
        // as a NN to other points k times.
        int Q;
        int Q2;
        int Q3;
        int Q4;
        int Q5;
        int Q6;
        int Q1Array[];
        int Q2Array[];
        double p11;
        double p111;
        double p1111;
        double p12;
        double p112;
        double p1122;
        double p21;
        double p221;
        double p2211;
        double p22;
        double p222;
        double p2222;
        double varN11;
        double varN12;
        double varN21;
        double varN22;
        double covN11N22;
        double r;
        double CD1;
        double CD2;
        // Under complete spatial randomness independence, zijD asymptotically has a N(0,1) distribution 
        // conditional on Q and R;
        double z11D;
        double z12D;
        double z21D;
        double z22D;
        double T11;
        double T12;
        double T21;
        double T22;
        double p122;
        double p1112;
        double p2221;
        double covN11N12;
        double covN11N21;
        double covN21N11;
        double covN12N21;
        double covN12N22;
        double varC1;
        double varC2;
        double covN11C1;
        double covN12C2;
        double covN21C1;
        double covN22C2;
        double varT11;
        double varT12;
        double varT21;
        double varT22;
        double z11N;
        double z12N;
        double z21N;
        double z22N;
        double covN21N12;
        double covN12N11;
        double covN21N22;
        double covN22N11;
        double covN22N21;
        double covN22N12;
        double covN11C2;
        double covN12C1;
        double covN22C1;
        double covN21C2;
        double covC1C1;
        double covC1C2;
        double covC2C1;
        double covC2C2;
        double covT11T12;
        double covT11T21;
        double covT11T22;
        double covT12T11;
        double covT12T21;
        double covT12T22;
        double covT21T11;
        double covT21T12;
        double covT21T22;
        double covT22T11;
        double covT22T12;
        double covT22T21;
        double sigma[][];
        Matrix sigmaN;
        double Tp[][];
        Matrix TpM;
        double T[][];
        Matrix TM;
        double CN[][] = null;
        Matrix sigmaD;
        double ND[][];
        Matrix NDM;
        double NDp[][];
        Matrix NDpM;
        byte red[];
        byte green[];
        boolean success;
        GeneralizedInverse ge;
        double sigmaInv[][];
        if (srcImage == null) {
            displayError("Source Image is null");
            finalize();

            return;
        }

        

        fireProgressStateChanged(srcImage.getImageName(), "Two class generation ...");

        xDim = srcImage.getExtents()[0];
        yDim = srcImage.getExtents()[1];
        length = xDim * yDim;
        buffer = new int[length];
        // Create a mask for setting circles
        radiusSquared = radius * radius;
        xMaskDim = 2 * radius + 1;
        yMaskDim = xMaskDim;
        mask = new byte[xMaskDim * yMaskDim];
        maskBytesSet = 0;
        for (y = 0; y <= 2*radius; y++) {
            yDistSquared = (y - radius);
            yDistSquared = yDistSquared * yDistSquared;
            for (x = 0; x <= 2*radius; x++) {
                xDistSquared = (x - radius);
                xDistSquared = xDistSquared * xDistSquared;
                distSquared = xDistSquared + yDistSquared;
                if (distSquared <= radiusSquared) {
                    mask[x + y * xMaskDim] = 1;
                    maskBytesSet++;
                }
            }
        } // for (y = 0; y <= 2*radius; y++)
        
        switch(process) {
            case FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS:
                Preferences.debug("FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS\n");
                System.out.println("FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS");
                break;
            case FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS:
                Preferences.debug("FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS\n");
                System.out.println("FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS");
                break;
            case RANDOM_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS:
                Preferences.debug("RANDOM_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS\n");
                System.out.println("RANDOM_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS");
                break;
            case RANDOM_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS:
                Preferences.debug("RANDOM_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS\n");
                System.out.println("RANDOM_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS");
                break;
            case MATERN_SAME_PARENTS:
                Preferences.debug("MATERN_SAME_PARENTS\n");
                System.out.println("MATERN_SAME_PARENTS");
                break;
            case MATERN_DIFFERENT_PARENTS:
                Preferences.debug("MATERN_DIFFERENT_PARENTS\n");
                System.out.println("MATERN_DIFFERENT_PARENTS");
                break;
            case INHOMOGENEOUS_POISSON:
                Preferences.debug("INHOMOGENEOUS_POISSON\n");
                System.out.println("INHOMOGENEOUS_POISSON");
                switch(inhomogeneous) {
                    case SQRT_X_PLUS_Y:
                        Preferences.debug("SQRT_X_PLUS_Y\n");
                        System.out.println("SQRT_X_PLUS_Y");
                        break;
                    case SQRT_X_TIMES_Y:
                        Preferences.debug("SQRT_X_TIMES_Y\n");
                        System.out.println("SQRT_X_TIMES_Y");
                        break;
                    case ABS_X_MINUS_Y:
                        Preferences.debug("ABS_X_MINUS_Y\n");
                        System.out.println("ABS_X_MINUS_Y");
                        break;
                }
                break;
            case SEGREGATION_ALTERNATIVE:
                Preferences.debug("SEGREGATION_ALTERNATIVE\n");
                System.out.println("SEGREGATION_ALTERNATIVE");
                break;
            case ASSOCIATION_ALTERNATIVE:
                Preferences.debug("ASSOCIATION_ALTERNATIVE\n");
                System.out.println("ASSOCIATION_ALTERNATIVE");
                break;
        }
        
        randomGen = new RandomNumberGen();
        if ((process == FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS) || 
            (process == FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS) ||
            (process == RANDOM_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS) || 
            (process == RANDOM_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS)) {
            stdDev = (xDim - 1)*normalizedStdDev;
            parentX = new byte[length];
            xParentXLocation = new int[numParents];
            xParentYLocation = new int[numParents];
            xCircleXCenter = new int[numOffspring1];
            xCircleYCenter = new int[numOffspring1];
            yCircleXCenter = new int[numOffspring2];
            yCircleYCenter = new int[numOffspring2];
            offspring1PerParent = numOffspring1/numParents;
            offspring2PerParent = numOffspring2/numParents;
            for (i = 0; i < numParents; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xCenter = randomGen.genUniformRandomNum(0, xDim - 1);
                    yCenter = randomGen.genUniformRandomNum(0, yDim - 1);
                    if (parentX[xCenter + xDim * yCenter] != 0) {
                        found = false;
                        attempts++;
                    }
                    else {
                        xParentXLocation[i] = xCenter;
                        xParentYLocation[i] = yCenter;
                        parentX[xCenter + xDim * yCenter] = 1;
                    }
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
            } // for (i = 0; i < numParents; i++)
            xParentsPlaced = i;
            if (xParentsPlaced == 1) {
                if (numParents != 1) {
                    Preferences.debug("1 X parent point placed.  " + numParents + " parent points requested.\n");
                    System.out.println("1 X parent point placed. " + numParents + " parent points requested.");
                    setCompleted(false);
                    return;
                    
                }
                else {
                    Preferences.debug("1 X parent point placed.  1 parent point requested\n");
                    System.out.println("1 X parent point placed.  1 parent point requested");    
                }
            }
            else if (xParentsPlaced != numParents) {
                Preferences.debug(xParentsPlaced + " X parent points placed.  " +
                                  numParents + " parent points requested.\n");
                System.out.println(xParentsPlaced + " X parent points placed.  " +
                        numParents + " parent points requested.");
                setCompleted(false);
                return;
            }   
            else { // xParentsPlaced == numParents
                Preferences.debug(xParentsPlaced + " X parent points placed.  " +
                        numParents + " parent points requested.\n");
                System.out.println(xParentsPlaced + " X parent points placed.  " +
                        numParents + " parent points requested.");
            }
            
            if ((process == FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS) ||
                    (process == RANDOM_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS)) {
                    parentY = new byte[length];
                    yParentXLocation = new int[numParents];
                    yParentYLocation = new int[numParents];
                    for (i = 0; i < numParents; i++) {
                        found = false;
                        attempts = 0;
                        while ((!found) && (attempts <= 100)) {
                            found = true;
                            xCenter = randomGen.genUniformRandomNum(0, xDim - 1);
                            yCenter = randomGen.genUniformRandomNum(0, yDim - 1);
                            if ((parentX[xCenter + xDim * yCenter] != 0) || (parentY[xCenter + xDim * yCenter] != 0)) {
                                found = false;
                                attempts++;
                            }
                            else {
                                yParentXLocation[i] = xCenter;
                                yParentYLocation[i] = yCenter;
                                parentY[xCenter + xDim * yCenter] = 1;
                            }
                        } // while ((!found) && (attempts <= 100))
                        if (!found) {
                            break;
                        }
                    } // for (i = 0; i < numParents; i++)
                    yParentsPlaced = i;
                    if (yParentsPlaced == 1) {
                        if (numParents != 1) {
                            Preferences.debug("1 Y parent point placed.  " + numParents + " parent points requested.\n");
                            System.out.println("1 Y parent point placed. " + numParents + " parent points requested.");
                            setCompleted(false);
                            return;
                            
                        }
                        else {
                            Preferences.debug("1 Y parent point placed.  1 parent point requested\n");
                            System.out.println("1 Y parent point placed.  1 parent point requested");    
                        }
                    }
                    else if (yParentsPlaced != numParents) {
                        Preferences.debug(yParentsPlaced + " Y parent points placed.  " +
                                          numParents + " parent points requested.\n");
                        System.out.println(yParentsPlaced + " Y parent points placed.  " +
                                numParents + " parent points requested.");
                        setCompleted(false);
                        return;
                    }   
                    else { // yParentsPlaced == numParents
                        Preferences.debug(yParentsPlaced + " Y parent points placed.  " +
                                numParents + " parent points requested.\n");
                        System.out.println(yParentsPlaced + " Y parent points placed.  " +
                                numParents + " parent points requested.");
                    }
                } // if ((process == FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS) ||
            for (i = 0; i < numOffspring1; i++) {
                if ((process == FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS) ||
                   (process == FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS)) {
                    parentXLocation = xParentXLocation[i/offspring1PerParent];
                    parentYLocation = xParentYLocation[i/offspring1PerParent];
                }
                else {
                    parentNumber =  randomGen.genUniformRandomNum(0, numParents - 1);
                    parentXLocation = xParentXLocation[parentNumber];
                    parentYLocation = xParentYLocation[parentNumber];
                }
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    // radially symmetric
                    angle = randomGen.genUniformRandomNum(0.0, Math.PI);
                    distance = stdDev * randomGen.genStandardGaussian();
                    xCenter = (int)Math.round(parentXLocation + distance * Math.cos(angle));
                    if ((xCenter - radius < 0) || (xCenter + radius > xDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    yCenter = (int)Math.round(parentYLocation + distance * Math.sin(angle));
                    if ((yCenter - radius < 0) || (yCenter + radius > yDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    rloop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break rloop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                xCircleXCenter[i] = xCenter;
                xCircleYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  1;
                        }
                    }
                }
            } // for (i = 0; i < numOffspring1; i++)
            offspring1Drawn = i;
            Preferences.debug(offspring1Drawn + " offspring 1 drawn.  " + numOffspring1 + " offspring 1 requested.\n");
            System.out.println(offspring1Drawn + " offspring 1 drawn.  " + numOffspring1 + " offspring 1 requested.");
            
            for (i = 0; i < numOffspring2; i++) {
                if (process == FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS) {
                    parentXLocation = xParentXLocation[i/offspring2PerParent];
                    parentYLocation = xParentYLocation[i/offspring2PerParent];
                }
                else if (process == FIXED_OFFSPRING_ALLOCATION_POISSON_DIFFERENT_PARENTS) {
                    parentXLocation = yParentXLocation[i/offspring2PerParent];
                    parentYLocation = yParentYLocation[i/offspring2PerParent];    
                }
                else if (process == RANDOM_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS) {
                    parentNumber =  randomGen.genUniformRandomNum(0, numParents - 1);
                    parentXLocation = xParentXLocation[parentNumber];
                    parentYLocation = xParentYLocation[parentNumber];
                }
                else {
                    parentNumber =  randomGen.genUniformRandomNum(0, numParents - 1);
                    parentXLocation = yParentXLocation[parentNumber];
                    parentYLocation = yParentYLocation[parentNumber];    
                }
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    // radially symmetric
                    angle = randomGen.genUniformRandomNum(0.0, Math.PI);
                    distance = stdDev * randomGen.genStandardGaussian();
                    xCenter = (int)Math.round(parentXLocation + distance * Math.cos(angle));
                    if ((xCenter - radius < 0) || (xCenter + radius > xDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    yCenter = (int)Math.round(parentYLocation + distance * Math.sin(angle));
                    if ((yCenter - radius < 0) || (yCenter + radius > yDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    r2loop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break r2loop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                yCircleXCenter[i] = xCenter;
                yCircleYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  2;
                        }
                    }
                }
            } // for (i = 0; i < numOffspring2; i++)
            offspring2Drawn = i;
            Preferences.debug(offspring2Drawn + " offspring 2 drawn.  " + numOffspring2 + " offspring 2 requested.\n");
            System.out.println(offspring2Drawn + " offspring 2 drawn.  " + numOffspring2 + " offspring 2 requested.");
        } // if ((process == FIXED_OFFSPRING_ALLOCATION_POISSON_SAME_PARENTS) || 
        
        
        if ((process == MATERN_SAME_PARENTS) || (process == MATERN_DIFFERENT_PARENTS)) {
            // Put 100 * parentPoissonNormalizedMean points into an area 100 times as large
            // as the resulting area.
            discRadius = (int)Math.round(normalizedDiscRadius * (xDim - 1));
            expandedXDim = 10 * xDim;
            expandedYDim = 10 * yDim;
            expandedLength = expandedXDim * expandedYDim;
            // Create a mask for the disc around the Poisson parent points
            discRadiusSquared = discRadius * discRadius;
            xDiscMaskDim = 2 * discRadius + 1;
            yDiscMaskDim = xDiscMaskDim;
            discMask = new byte[xDiscMaskDim * yDiscMaskDim];
            discMaskBytesSet = 0;
            for (y = 0; y <= 2*discRadius; y++) {
                yDistSquared = y - discRadius;
                yDistSquared = yDistSquared * yDistSquared;
                for (x = 0; x <= 2 * discRadius; x++) {
                    xDistSquared = x - discRadius;
                    xDistSquared = xDistSquared * xDistSquared;
                    distSquared = xDistSquared + yDistSquared;
                    if (distSquared <= discRadiusSquared) {
                        discMask[x + y * xDiscMaskDim] = 1;
                        discMaskBytesSet++;
                    }
                }
            } // for (y = 0; y <= 2*radius; y++)
            xDiscMask = new int[discMaskBytesSet];
            yDiscMask = new int[discMaskBytesSet];
            i = 0;
            for (y = 0; y <= 2*discRadius; y++) {
                for (x = 0; x <= 2*discRadius; x++) {
                    if (discMask[x + y * xDiscMaskDim] == 1) {
                        xDiscMask[i] = x;
                        yDiscMask[i++] = y;
                    }
                }
            }
            paddedBuffer = new int[(xDim + 4 * discRadius)*(yDim + 4 * discRadius)];
            numParents = (int)Math.round(100 * parentPoissonNormalizedMean);
            parentX = new byte[expandedLength];
            xParentXLocation = new int[numParents];
            xParentYLocation = new int[numParents];
            for (i = 0, j = 0; i < numParents; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xCenter = randomGen.genUniformRandomNum(0, expandedXDim - 1);
                    yCenter = randomGen.genUniformRandomNum(0, expandedYDim - 1);
                    if (parentX[xCenter + expandedXDim * yCenter] != 0) {
                        found = false;
                        attempts++;
                    }
                    else {
                        parentX[xCenter + expandedXDim * yCenter] = 1;
                        if ((xCenter >= 4*xDim - discRadius) && (xCenter <= 5*xDim - 1 + discRadius) &&
                            (yCenter >= 4*yDim - discRadius) && (yCenter <= 5*yDim - 1 + discRadius)) {
                            // Go from discRadius to dim + 3 * discRadius -1 in paddedBuffer
                            xParentXLocation[j] = xCenter - (4 * xDim - 2*discRadius);
                            xParentYLocation[j++] = yCenter - (4 * yDim - 2*discRadius);
                        }
                    }
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }    
            } // for (i = 0; i < numParents; i++)
            xParentsPlaced = j;
            Preferences.debug(xParentsPlaced + " X parents placed in padded buffer.  Mean of " + 
                              parentPoissonNormalizedMean + " X parents requested for unpadded buffer.\n");
            System.out.println(xParentsPlaced + " X parents placed in padded buffer.  Mean of " + 
                              parentPoissonNormalizedMean + " X parents requested for unpadded buffer.");
            if (xParentsPlaced == 0) {
                setCompleted(false);
                return;
            }
            
            if (process == MATERN_DIFFERENT_PARENTS) {
                parentY = new byte[expandedLength];
                yParentXLocation = new int[numParents];
                yParentYLocation = new int[numParents];
                for (i = 0, j = 0; i < numParents; i++) {
                    found = false;
                    attempts = 0;
                    while ((!found) && (attempts <= 100)) {
                        found = true;
                        xCenter = randomGen.genUniformRandomNum(0, expandedXDim - 1);
                        yCenter = randomGen.genUniformRandomNum(0, expandedYDim - 1);
                        if ((parentX[xCenter + expandedXDim * yCenter] != 0) || 
                            (parentY[xCenter + expandedXDim * yCenter] != 0)){
                            found = false;
                            attempts++;
                        }
                        else {
                            parentY[xCenter + expandedXDim * yCenter] = 1;
                            if ((xCenter >= 4*xDim - discRadius) && (xCenter <= 5*xDim - 1 + discRadius) &&
                                (yCenter >= 4*yDim - discRadius) && (yCenter <= 5*yDim - 1 + discRadius)) {
                                // Go from discRadius to dim + 3 * discRadius -1 in paddedBuffer
                                yParentXLocation[j] = xCenter - (4 * xDim - 2*discRadius);
                                yParentYLocation[j++] = yCenter - (4 * yDim - 2*discRadius);
                            }
                        }
                    } // while ((!found) && (attempts <= 100))
                    if (!found) {
                        break;
                    }    
                } // for (i = 0; i < numParents; i++)
                yParentsPlaced = j;
                Preferences.debug(yParentsPlaced + " Y parents placed in padded buffer.  Mean of " + 
                                  parentPoissonNormalizedMean + " Y parents requested for unpadded buffer.\n");
                System.out.println(yParentsPlaced + " Y parents placed in padded buffer.  Mean of " + 
                                  parentPoissonNormalizedMean + " Y parents requested for unpadded buffer."); 
                if (yParentsPlaced == 0) {
                    setCompleted(false);
                    return;
                }
            } // if (process == MATERN_DIFFERENT_PARENTS)
            
            offspring1PoissonMean = numOffspring1/parentPoissonNormalizedMean;
            xCircleXCenter = new int[(int)Math.round(2 * xParentsPlaced * offspring1PoissonMean)];
            xCircleYCenter = new int[xCircleXCenter.length];
            offspring1Drawn = 0;
            for (i = 0; i < xParentsPlaced; i++) {
                 events = 1;
                 gain = 1.0;
                 offset = 0;
                 poissonValues = randomGen.poissDecay(events, offspring1PoissonMean, gain, offset);
                 pointsInCluster = (int)Math.round(poissonValues[0]);
                 for (j = 0; j < pointsInCluster; j++) {
                     found = false;
                     attempts = 0;
                     while ((!found) && (attempts <= 100)) {
                         found = true;
                         discIndex = randomGen.genUniformRandomNum(0, discMaskBytesSet - 1);
                         // center goes from 0 to dim + 4 * discRadius - 1 in paddedBuffer
                         xCenter = xParentXLocation[i] + xDiscMask[discIndex] - discRadius;
                         if ((xCenter - radius < 0) || (xCenter + radius > xDim + 4*discRadius - 1)) {
                             found = false;
                             attempts++;
                             continue;
                         }
                         yCenter = xParentYLocation[i] + yDiscMask[discIndex] - discRadius;
                         if ((yCenter - radius < 0) || (yCenter + radius > yDim + 4*discRadius - 1)) {
                             found = false;
                             attempts++;
                             continue;
                         }
                         r3loop:
                             for (y = 0; y <= 2*radius; y++) {
                                 for (x = 0; x <= 2*radius; x++) {
                                     if (mask[x + y * xMaskDim] == 1) {
                                         if (paddedBuffer[(xCenter + x - radius) + 
                                                          (xDim + 4 * discRadius)*(yCenter + y - radius)] != 0) {
                                             found = false;
                                             attempts++;
                                             break r3loop;
                                         }
                                     }
                                 }
                             } // for (y = 0; y <= 2*radius; y++)
                     } // while ((!found) && (attempts <= 100))
                     if (!found) {
                         break;
                     }
                     
                     for (y = 0; y <= 2*radius; y++) {
                         for (x = 0; x <= 2*radius; x++) {
                             if (mask[x + y * xMaskDim] == 1) {
                                 paddedBuffer[(xCenter + x - radius) + (xDim + 4 * discRadius) *(yCenter + y - radius)] =  1;
                             }
                         }
                     }  
                     
                     if ((xCenter - radius >= 2 * discRadius) && (xCenter + radius < xDim + 2 * discRadius) &&
                             (yCenter - radius >= 2 * discRadius) && (yCenter + radius < yDim + 2 * discRadius)) {
                         // Subtract 2*discRadius to change offset from paddedBuffer to buffer
                         xCircleXCenter[offspring1Drawn] = xCenter - 2 * discRadius;
                         xCircleYCenter[offspring1Drawn++] = yCenter - 2 * discRadius;
                     }
                 } // for (j = 0; j < pointsInCluster; j++)
            } // for (i = 0; i < xParentsPlaced; i++)
            Preferences.debug(offspring1Drawn + " offspring 1 drawn\n");
            System.out.println(offspring1Drawn + " offspring 1 drawn");
            
            offspring2PoissonMean = numOffspring2/parentPoissonNormalizedMean;
            if (process == MATERN_SAME_PARENTS) {
                parentsPlaced = xParentsPlaced;
            }
            else {
                parentsPlaced = yParentsPlaced;   
            }
            yCircleXCenter = new int[(int)Math.round(2 * parentsPlaced * offspring2PoissonMean)];
            yCircleYCenter = new int[yCircleXCenter.length];
            offspring2Drawn = 0;
            for (i = 0; i < parentsPlaced; i++) {
                 events = 1;
                 gain = 1.0;
                 offset = 0;
                 poissonValues = randomGen.poissDecay(events, offspring2PoissonMean, gain, offset);
                 pointsInCluster = (int)Math.round(poissonValues[0]);
                 for (j = 0; j < pointsInCluster; j++) {
                     found = false;
                     attempts = 0;
                     while ((!found) && (attempts <= 100)) {
                         found = true;
                         discIndex = randomGen.genUniformRandomNum(0, discMaskBytesSet - 1);
                         // center goes from 0 to dim + 4 * discRadius - 1 in paddedBuffer
                         if (process == MATERN_SAME_PARENTS) {
                             xCenter = xParentXLocation[i] + xDiscMask[discIndex] - discRadius;
                         }
                         else {
                             xCenter = yParentXLocation[i] + xDiscMask[discIndex] - discRadius;
                         }
                         if ((xCenter - radius < 0) || (xCenter + radius > xDim + 4*discRadius - 1)) {
                             found = false;
                             attempts++;
                             continue;
                         }
                         if (process == MATERN_SAME_PARENTS) {
                             yCenter = xParentYLocation[i] + yDiscMask[discIndex] - discRadius;
                         }
                         else {
                             yCenter = yParentYLocation[i] + yDiscMask[discIndex] - discRadius;    
                         }
                         if ((yCenter - radius < 0) || (yCenter + radius > yDim + 4*discRadius - 1)) {
                             found = false;
                             attempts++;
                             continue;
                         }
                         r4loop:
                             for (y = 0; y <= 2*radius; y++) {
                                 for (x = 0; x <= 2*radius; x++) {
                                     if (mask[x + y * xMaskDim] == 1) {
                                         if (paddedBuffer[(xCenter + x - radius) + 
                                                          (xDim + 4 * discRadius)*(yCenter + y - radius)] != 0) {
                                             found = false;
                                             attempts++;
                                             break r4loop;
                                         }
                                     }
                                 }
                             } // for (y = 0; y <= 2*radius; y++)
                     } // while ((!found) && (attempts <= 100))
                     if (!found) {
                         break;
                     }
                     
                     for (y = 0; y <= 2*radius; y++) {
                         for (x = 0; x <= 2*radius; x++) {
                             if (mask[x + y * xMaskDim] == 1) {
                                 paddedBuffer[(xCenter + x - radius) + (xDim + 4 * discRadius) *(yCenter + y - radius)] =  2;
                             }
                         }
                     }  
                     
                     if ((xCenter - radius >= 2 * discRadius) && (xCenter + radius < xDim + 2 * discRadius) &&
                             (yCenter - radius >= 2 * discRadius) && (yCenter + radius < yDim + 2 * discRadius)) {
                         // Subtract 2*discRadius to change offset from paddedBuffer to buffer
                         yCircleXCenter[offspring2Drawn] = xCenter - 2 * discRadius;
                         yCircleYCenter[offspring2Drawn++] = yCenter - 2 * discRadius;
                     }
                 } // for (j = 0; j < pointsInCluster; j++)
            } // for (i = 0; i < parentsPlaced; i++)
            Preferences.debug(offspring2Drawn + " offspring 2 drawn\n");
            System.out.println(offspring2Drawn + " offspring 2 drawn");
            
            for (y = 0; y < yDim; y++) {
                for (x = 0; x < xDim; x++) {
                    buffer[x + xDim * y] = paddedBuffer[(x + 2 * discRadius) + (xDim + 4 * discRadius) * (y + 2 * discRadius)];
                }
            }
        } // if ((process == MATERN_SAME_PARENTS) || (process == MATERN_DIFFERENT_PARENTS))
        
        if (process == INHOMOGENEOUS_POISSON) {
            xCircleOrigXCenter = new int[numPoints1];
            xCircleOrigYCenter = new int[numPoints1];
            yCircleOrigXCenter = new int[numPoints2];
            yCircleOrigYCenter = new int[numPoints2];
            for (i = 0; i < numPoints1; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xCenter = randomGen.genUniformRandomNum(radius, xDim - 1 - radius);
                    yCenter = randomGen.genUniformRandomNum(radius, yDim - 1 - radius);
                    r5loop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break r5loop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                xCircleOrigXCenter[i] = xCenter;
                xCircleOrigYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  1;
                        }
                    }
                }
            } // for (i = 0; i < numPoints1; i++)
            originalCirclesCreated1 = i;
            Preferences.debug(originalCirclesCreated1 + " type 1 circles orignally created.  " + 
                    numPoints1 + " type 1 circles requested.\n");
            System.out.println(originalCirclesCreated1 + " type 1 circles orignally created.  " + 
                    numPoints1 + " type 1 circles requested.\n"); 
            
            for (i = 0; i < numPoints2; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xCenter = randomGen.genUniformRandomNum(radius, xDim - 1 - radius);
                    yCenter = randomGen.genUniformRandomNum(radius, yDim - 1 - radius);
                    r6loop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break r6loop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                yCircleOrigXCenter[i] = xCenter;
                yCircleOrigYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  2;
                        }
                    }
                }
            } // for (i = 0; i < numPoints2; i++)
            originalCirclesCreated2 = i;
            Preferences.debug(originalCirclesCreated2 + " type 2 circles orignally created.  " + 
                    numPoints2 + " type 2 circles requested.\n");
            System.out.println(originalCirclesCreated2 + " type 2 circles orignally created.  " + 
                    numPoints2 + " type 2 circles requested.\n");
            
            maxIntensity = -Double.MAX_VALUE;
            for (i = 0; i < originalCirclesCreated1; i++) {
                intensity = Math.sqrt((double)xCircleOrigXCenter[i]/(xDim - 1) + (double)xCircleOrigYCenter[i]/(yDim - 1));
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                }
            }
            
            retainCircle = new boolean[originalCirclesCreated1];
            offspring1Drawn = 0;
            for (i = 0; i < originalCirclesCreated1; i++) {
                intensity = Math.sqrt((double)xCircleOrigXCenter[i]/(xDim - 1) + (double)xCircleOrigYCenter[i]/(yDim - 1));
                retentionProbability = intensity/maxIntensity;
                cumulativeProb = randomGen.genUniformRandomNum(0.0, 1.0);
                if (cumulativeProb <= retentionProbability) {
                    retainCircle[i] = true;
                    offspring1Drawn++;
                }
            }
            
            Preferences.debug(offspring1Drawn + " type 1 offspring retained.\n");
            System.out.println(offspring1Drawn + " type 1 offspring retained.");
            
            xCircleXCenter = new int[offspring1Drawn];
            xCircleYCenter = new int[offspring1Drawn];
            for (i = 0, j = 0; i < originalCirclesCreated1; i++) {
                if (retainCircle[i]) {
                    xCircleXCenter[j] = xCircleOrigXCenter[i];
                    xCircleYCenter[j++] = xCircleOrigYCenter[i]; 
                }
            }
            
            maxIntensity = -Double.MAX_VALUE;
            for (i = 0; i < originalCirclesCreated2; i++) {
                if (inhomogeneous == SQRT_X_PLUS_Y) {
                    intensity = Math.sqrt((double)yCircleOrigXCenter[i]/(xDim - 1) + (double)yCircleOrigYCenter[i]/(yDim - 1));
                }
                else if (inhomogeneous == SQRT_X_TIMES_Y) {
                    intensity = Math.sqrt((double)yCircleOrigXCenter[i] * yCircleOrigYCenter[i]/((xDim-1)*(yDim-1)));
                }
                else {
                    intensity = Math.abs((double)yCircleOrigXCenter[i]/(xDim-1) - (double)yCircleOrigYCenter[i]/(yDim-1));
                }
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                }
            }
            
            retainCircle = new boolean[originalCirclesCreated2];
            offspring2Drawn = 0;
            for (i = 0; i < originalCirclesCreated2; i++) {
                if (inhomogeneous == SQRT_X_PLUS_Y) {
                    intensity = Math.sqrt((double)yCircleOrigXCenter[i]/(xDim - 1) + (double)yCircleOrigYCenter[i]/(yDim - 1));
                }
                else if (inhomogeneous == SQRT_X_TIMES_Y) {
                    intensity = Math.sqrt((double)yCircleOrigXCenter[i] * yCircleOrigYCenter[i]/((xDim-1)*(yDim-1)));
                }
                else {
                    intensity = Math.abs((double)yCircleOrigXCenter[i]/(xDim-1) - (double)yCircleOrigYCenter[i]/(yDim-1));
                }
                retentionProbability = intensity/maxIntensity;
                cumulativeProb = randomGen.genUniformRandomNum(0.0, 1.0);
                if (cumulativeProb <= retentionProbability) {
                    retainCircle[i] = true;
                    offspring2Drawn++;
                }
            }
            
            Preferences.debug(offspring2Drawn + " type 2 offspring retained.\n");
            System.out.println(offspring2Drawn + " type 2 offspring retained.");
            
            yCircleXCenter = new int[offspring2Drawn];
            yCircleYCenter = new int[offspring2Drawn];
            for (i = 0, j = 0; i < originalCirclesCreated2; i++) {
                if (retainCircle[i]) {
                    yCircleXCenter[j] = yCircleOrigXCenter[i];
                    yCircleYCenter[j++] = yCircleOrigYCenter[i]; 
                }
            }
            
            for (i = 0; i < buffer.length; i++) {
                buffer[i] = 0;
            }
            
            for (i = 0; i < offspring1Drawn; i++) {
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCircleXCenter[i] + x - radius) + xDim*(xCircleYCenter[i] + y - radius)] =  1;
                        }
                    }
                }    
            }
            
            for (i = 0; i < offspring2Drawn; i++) {
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(yCircleXCenter[i] + x - radius) + xDim*(yCircleYCenter[i] + y - radius)] =  2;
                        }
                    }
                }    
            }
        } // if (process == INHOMOGENEOUS_POISSON)
        
        if (process == SEGREGATION_ALTERNATIVE) {
            xCircleXCenter = new int[numOffspring1];
            xCircleYCenter = new int[numOffspring1];
            yCircleXCenter = new int[numOffspring2];
            yCircleYCenter = new int[numOffspring2];
            for (i = 0; i < numOffspring1; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xCenter = randomGen.genUniformRandomNum(0, (int)Math.round((xDim - 1)*(1 - segregation)));
                    if ((xCenter - radius < 0) || (xCenter + radius > xDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    yCenter = randomGen.genUniformRandomNum(0, (int)Math.round((yDim - 1)*(1 - segregation)));
                    if ((yCenter - radius < 0) || (yCenter + radius > yDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    r7loop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break r7loop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                xCircleXCenter[i] = xCenter;
                xCircleYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  1;
                        }
                    }
                }
            } // for (i = 0; i < numOffspring1; i++)
            offspring1Drawn = i;
            Preferences.debug(offspring1Drawn + " offspring 1 drawn.  " + numOffspring1 + " offspring 1 requested.\n");
            System.out.println(offspring1Drawn + " offspring 1 drawn.  " + numOffspring1 + " offspring 1 requested.");
            
            for (i = 0; i < numOffspring2; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xCenter = randomGen.genUniformRandomNum((int)Math.round(segregation*(xDim-1)), xDim-1);
                    if ((xCenter - radius < 0) || (xCenter + radius > xDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    yCenter = randomGen.genUniformRandomNum((int)Math.round(segregation*(yDim-1)), yDim-1);
                    if ((yCenter - radius < 0) || (yCenter + radius > yDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    r8loop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break r8loop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                yCircleXCenter[i] = xCenter;
                yCircleYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  2;
                        }
                    }
                }
            } // for (i = 0; i < numOffspring2; i++)
            offspring2Drawn = i;
            Preferences.debug(offspring2Drawn + " offspring 2 drawn.  " + numOffspring2 + " offspring 2 requested.\n");
            System.out.println(offspring2Drawn + " offspring 2 drawn.  " + numOffspring2 + " offspring 2 requested.");
            
        } // if (process == SEGREGATION_ALTERNATIVE)
        
        if (process == ASSOCIATION_ALTERNATIVE) {
            discRadius = (int)Math.round(normalizedDiscRadius * (xDim - 1)); 
            xCircleXCenter = new int[numOffspring1];
            xCircleYCenter = new int[numOffspring1];
            yCircleXCenter = new int[numOffspring2];
            yCircleYCenter = new int[numOffspring2];
            for (i = 0; i < numOffspring1; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xCenter = randomGen.genUniformRandomNum(0, xDim);
                    if ((xCenter - radius < 0) || (xCenter + radius > xDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    yCenter = randomGen.genUniformRandomNum(0, yDim);
                    if ((yCenter - radius < 0) || (yCenter + radius > yDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    r9loop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break r9loop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                xCircleXCenter[i] = xCenter;
                xCircleYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  1;
                        }
                    }
                }
            } // for (i = 0; i < numOffspring1; i++)
            offspring1Drawn = i;
            Preferences.debug(offspring1Drawn + " offspring 1 drawn.  " + numOffspring1 + " offspring 1 requested.\n");
            System.out.println(offspring1Drawn + " offspring 1 drawn.  " + numOffspring1 + " offspring 1 requested.");
            
            for (i = 0; i < numOffspring2; i++) {
                found = false;
                attempts = 0;
                while ((!found) && (attempts <= 100)) {
                    found = true;
                    xNumber =  randomGen.genUniformRandomNum(0, offspring1Drawn - 1);
                    parentXLocation = xCircleXCenter[xNumber];
                    parentYLocation = xCircleYCenter[xNumber];
                    ry = randomGen.genUniformRandomNum(0.0, discRadius);
                    angle = randomGen.genUniformRandomNum(0.0, 2.0*Math.PI);
                    xCenter = (int)Math.round(parentXLocation + ry * Math.cos(angle));
                    if ((xCenter - radius < 0) || (xCenter + radius > xDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    yCenter = (int)Math.round(parentYLocation + ry * Math.sin(angle));
                    if ((yCenter - radius < 0) || (yCenter + radius > yDim - 1)) {
                        found = false;
                        attempts++;
                        continue;
                    }
                    r10loop:
                        for (y = 0; y <= 2*radius; y++) {
                            for (x = 0; x <= 2*radius; x++) {
                                if (mask[x + y * xMaskDim] == 1) {
                                    if (buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] != 0) {
                                        found = false;
                                        attempts++;
                                        break r10loop;
                                    }
                                }
                            }
                        } // for (y = 0; y <= 2*radius; y++)
                } // while ((!found) && (attempts <= 100))
                if (!found) {
                    break;
                }
                yCircleXCenter[i] = xCenter;
                yCircleYCenter[i] = yCenter;
                for (y = 0; y <= 2*radius; y++) {
                    for (x = 0; x <= 2*radius; x++) {
                        if (mask[x + y * xMaskDim] == 1) {
                            buffer[(xCenter + x - radius) + xDim*(yCenter + y - radius)] =  2;
                        }
                    }
                }
            } // for (i = 0; i < numOffspring2; i++)
            offspring2Drawn = i;
            Preferences.debug(offspring2Drawn + " offspring 2 drawn.  " + numOffspring2 + " offspring 2 requested.\n");
            System.out.println(offspring2Drawn + " offspring 2 drawn.  " + numOffspring2 + " offspring 2 requested.");
        } // if (process == ASSOCIATION_ALTERNATIVE)
        
        N11 = 0;
        N12 = 0;
        N21 = 0;
        N22 = 0;
        NN1Distance = new double[offspring1Drawn];
        NN1Type = new byte[offspring1Drawn];
        NN1Neighbor = new int[offspring1Drawn];
        for (i = 0; i < offspring1Drawn; i++) {
            lowestDistSquared = Integer.MAX_VALUE;
            for (j = 0; j < offspring1Drawn; j++) {
                if (i != j) {          
                    xDistSquared = xCircleXCenter[i] - xCircleXCenter[j];
                    xDistSquared = xDistSquared * xDistSquared;
                    yDistSquared = xCircleYCenter[i] - xCircleYCenter[j];
                    yDistSquared = yDistSquared * yDistSquared;
                    distSquared = xDistSquared + yDistSquared;
                    if (distSquared < lowestDistSquared) {
                        lowestDistSquared = distSquared;
                        NN1Distance[i] = Math.sqrt(distSquared);
                        NN1Type[i] = SAME;
                        NN1Neighbor[i] = j;
                    }  
                }
            }
            
            for (j = 0; j < offspring2Drawn; j++) {          
                xDistSquared = xCircleXCenter[i] - yCircleXCenter[j];
                xDistSquared = xDistSquared * xDistSquared;
                yDistSquared = xCircleYCenter[i] - yCircleYCenter[j];
                yDistSquared = yDistSquared * yDistSquared;
                distSquared = xDistSquared + yDistSquared;
                if (distSquared < lowestDistSquared) {
                    lowestDistSquared = distSquared;
                    NN1Distance[i] = Math.sqrt(distSquared);
                    NN1Type[i] = DIFFERENT;
                    NN1Neighbor[i] = j;
                }  
            }
        } // for (i = 0; i < offspring1Drawn; i++)
        
        for (i = 0; i < offspring1Drawn; i++) {
            if (NN1Type[i] == SAME) {
                N11++;
            }
            else {
                N12++;
            }
        }
        
        NN2Distance = new double[offspring2Drawn];
        NN2Type = new byte[offspring2Drawn];
        NN2Neighbor = new int[offspring2Drawn];
        for (i = 0; i < offspring2Drawn; i++) {
            lowestDistSquared = Integer.MAX_VALUE;
            for (j = 0; j < offspring1Drawn; j++) {         
                xDistSquared = yCircleXCenter[i] - xCircleXCenter[j];
                xDistSquared = xDistSquared * xDistSquared;
                yDistSquared = yCircleYCenter[i] - xCircleYCenter[j];
                yDistSquared = yDistSquared * yDistSquared;
                distSquared = xDistSquared + yDistSquared;
                if (distSquared < lowestDistSquared) {
                    lowestDistSquared = distSquared;
                    NN2Distance[i] = Math.sqrt(distSquared);
                    NN2Type[i] = DIFFERENT;
                    NN2Neighbor[i] = j;
                }  
            }
            
            for (j = 0; j < offspring2Drawn; j++) { 
                if (i != j) {
                    xDistSquared = yCircleXCenter[i] - yCircleXCenter[j];
                    xDistSquared = xDistSquared * xDistSquared;
                    yDistSquared = yCircleYCenter[i] - yCircleYCenter[j];
                    yDistSquared = yDistSquared * yDistSquared;
                    distSquared = xDistSquared + yDistSquared;
                    if (distSquared < lowestDistSquared) {
                        lowestDistSquared = distSquared;
                        NN2Distance[i] = Math.sqrt(distSquared);
                        NN2Type[i] = SAME;
                        NN2Neighbor[i] = j;
                    } 
                }
            }
        } // for (i = 0; i < offspring2Drawn; i++)
        
        // For real data use buffer zone correction.  The width of the buffer area should be about the
        // average nearest neighbor distance.  Larger buffer areas are wasteful with little additional
        // gain.
        
        for (i = 0; i < offspring2Drawn; i++) {
            if (NN2Type[i] == DIFFERENT) {
                N21++;
            }
            else {
                N22++;
            }
        }
        
        if (selfTest1) {
            N11 = 137;
            N12 = 23;
            N21 = 38;
            N22 = 30;
        }
        
        C1 = N11 + N21;
        C2 = N12 + N22;
        n1 = N11 + N12;
        n2 = N21 + N22;
        n = n1 + n2;
        
        // Dixon's cell-specific test of segregation
        EN11 = ((double)n1*(n1 - 1))/(n - 1);
        EN12 = ((double)n1 * n2)/(n - 1);
        EN21 = EN12;
        EN22 = ((double)n2*(n2 - 1))/(n - 1);
        
        // Find R, twice the number of reflexive pairs.
        if (selfTest1) {
            Q = 162;
            R = 134;
        }
        else { // not selfTest1
            R = 0;
            for (i = 0; i < offspring1Drawn; i++) {
                if (NN1Type[i] == SAME && NN1Type[NN1Neighbor[i]] == SAME && NN1Neighbor[NN1Neighbor[i]] == i) {
                    R++;
                }
                else if (NN1Type[i] == DIFFERENT && NN2Type[NN1Neighbor[i]] == DIFFERENT && NN2Neighbor[NN1Neighbor[i]] == i) {
                    R++;
                }
            }
            
            for (i = 0; i < offspring2Drawn; i++) {
                if (NN2Type[i] == DIFFERENT && NN1Type[NN2Neighbor[i]] == DIFFERENT && NN1Neighbor[NN2Neighbor[i]] == i) {
                    R++;
                }
                else if (NN2Type[i] == SAME && NN2Type[NN2Neighbor[i]] == SAME && NN2Neighbor[NN2Neighbor[i]] == i) {
                    R++;
                }
            }
            
            Q1Array = new int[offspring1Drawn];
            Q2Array = new int[offspring2Drawn];
            for (i = 0; i < offspring1Drawn; i++) {
                if (NN1Type[i] == SAME) {
                    Q1Array[NN1Neighbor[i]]++;
                }
                else {
                    Q2Array[NN1Neighbor[i]]++;
                }
            }
            
            for (i = 0; i < offspring2Drawn; i++) {
                if (NN2Type[i] == DIFFERENT) {
                    Q1Array[NN2Neighbor[i]]++;    
                }
                else {
                    Q2Array[NN2Neighbor[i]]++;
                }
            }
            
            Q2 = 0;
            Q3 = 0;
            Q4 = 0;
            Q5 = 0;
            Q6 = 0;
            for (i = 0; i < offspring1Drawn; i++) {
                if (Q1Array[i] == 2) {
                    Q2++;
                }
                else if (Q1Array[i] == 3) {
                    Q3++;
                }
                else if (Q1Array[i] == 4) {
                    Q4++;
                }
                else if (Q1Array[i] == 5) {
                    Q5++;
                }
                else if (Q1Array[i] == 6) {
                    Q6++;
                }
            }
            
            for (i = 0; i < offspring2Drawn; i++) {
                if (Q2Array[i] == 2) {
                    Q2++;
                }
                else if (Q2Array[i] == 3) {
                    Q3++;
                }
                else if (Q2Array[i] == 4) {
                    Q4++;
                }
                else if (Q2Array[i] == 5) {
                    Q5++;
                }
                else if (Q2Array[i] == 6) {
                    Q6++;
                }
            }
            
            Q = 2 * (Q2 + 3*Q3 + 6*Q4 + 10*Q5 + 15*Q6);
        } // else not selfTest1
        
        p11 = ((double)n1*(n1 - 1))/(n*(n - 1));
        p111 = ((double)n1*(n1 - 1)*(n1 - 2))/(n*(n - 1)*(n - 2));
        p1111 = ((double)n1*(n1 - 1)*(n1 - 2)*(n1 - 3))/(n*(n - 1)*(n - 2)*(n - 3));
        p12 = ((double)n1*n2)/(n*(n - 1));
        p112 = ((double)n1*(n1 - 1)*n2)/(n*(n - 1)*(n - 2));
        p1122 = ((double)n1*(n1 - 1)*n2*(n2 - 1))/(n*(n - 1)*(n - 2)*(n - 3));
        p21 = p12;
        p221 = ((double)n2*(n2 - 1)*n1)/(n*(n - 1)*(n - 2));
        p2211 = p1122;
        p22 = ((double)n2*(n2 - 1))/(n*(n-1));
        p222 = ((double)n2*(n2 - 1)*(n2 - 2))/(n*(n - 1)*(n - 2));
        p2222 = ((double)n2*(n2 - 1)*(n2 - 2)*(n2 - 3))/(n*(n - 1)*(n - 2)*(n - 3));
        
        varN11 = (n + R)*p11 + (2*n - 2*R + Q)*p111 + (n*n - 3*n - Q + R)*p1111 -n*n*p11*p11;
        varN12 = n*p12 + Q*p112 + (n*n - 3*n - Q + R)*p1122 - n*n*p12*p12;
        varN21 = n*p21 + Q*p221 + (n*n - 3*n - Q + R)*p2211 - n*n*p21*p21;
        varN22 = (n + R)*p22 + (2*n - 2*R + Q)*p222 + (n*n - 3*n - Q + R)*p2222 - n*n*p22*p22;
        
        z11D = (N11 - EN11)/Math.sqrt(varN11);
        z12D = (N12 - EN12)/Math.sqrt(varN12);
        Preferences.debug("z11D = " + z11D + "\n");
        if (selfTest1) {
            Preferences.debug("Expect z11D = 4.36\n");
        }
        Preferences.debug("z12D = " + z12D + "\n");
        if (selfTest1) {
            Preferences.debug("Expect z12D = -4.36\n");
        }
        Preferences.debug("Should have z11D = -z12D for 2 class case\n");
        z21D = (N21 - EN21)/Math.sqrt(varN21);
        z22D = (N22 - EN22)/Math.sqrt(varN22);
        Preferences.debug("z21D = " + z21D + "\n");
        if (selfTest1) {
            Preferences.debug("Expect z21 = -2.29\n");
        }
        Preferences.debug("z22D = " + z22D + "\n");
        if (selfTest1) {
            Preferences.debug("Expect z22 = 2.29\n");
        }
        Preferences.debug("Should have z21D = -z22D for 2 class case\n");
        
        covN11N22 = (n*n - 3*n - Q + R)*p1122 - n*n*p11*p22;
        r = covN11N22/Math.sqrt(varN11*varN22);
        CD1 = (z11D*z11D + z22D*z22D - 2*r*z11D*z22D)/(1 - r*r);
        Preferences.debug("1994 version of CD = " + CD1 + "\n");
        
        if (CD1 > 0.0) {
            // Under random labelling the chi squared statistic has degrees of freedom = 2;
            // Under complete spatial randomness the chi squared statistic has degrees of freedom = 1;
            degreesOfFreedom = 1;
            stat = new Statistics(Statistics.CHI_SQUARED_CUMULATIVE_DISTRIBUTION_FUNCTION,
                    CD1, degreesOfFreedom, chiSquaredPercentile);
            stat.run();
            Preferences.debug("chiSquared percentile for 1994 version of Dixon's overall test of segregation = " + chiSquaredPercentile[0]*100.0 + "\n");
            System.out.println("chiSquared percentile for 1994 version of Dixon's overall test of segregation = " + chiSquaredPercentile[0]*100.0);
            
            if (chiSquaredPercentile[0] > 0.950) {
                Preferences.debug("chiSquared test rejects random object distribution\n");
                System.out.println("chiSquared test rejects random object distribution"); 
            }
            else {
                Preferences.debug("chiSquared test does not reject random object distribution\n");
                System.out.println("chiSquared test does not reject random object distribution");
            }
        } // if (CD1 > 0.0)
        else {
            Preferences.debug("CD1 should be positive\n");
        }
        
        p122 = ((double)n1*n2*(n2 - 1))/(n*(n - 1)*(n - 2));
        p1112 = ((double)n1*(n1 - 1)*(n1 - 2)*n2)/(n*(n - 1)*(n - 2)*(n - 3));
        p2221 = ((double)n2*(n2 - 1)*(n2 - 2)*n1)/(n*(n - 1)*(n - 2)*(n - 3));
        covN11N12 = (n - R)*p112 + (n*n - 3*n - Q + R)*p1112 - n*n*p11*p12;
        covN11N21 = (n - R + Q)*p112 + (n*n - 3*n - Q + R)*p1112 - n*n*p11*p12;
        covN12N21 = R*p12 + (n - R)*(p112 + p122) + (n*n - 3*n - Q + R)*p1122 - n*n*p12*p21;
        covN12N22 = (n - R + Q)*p221 + (n*n - 3*n - Q + R)*p2221 - n*n*p22*p21;
        covN12N11 = covN11N12;
        covN21N12 = covN12N21;
        covN21N22 = (n - R)*p221 + (n*n - 3*n - Q + R)*p2221 - n*n*p22*p21;
        covN21N11 = covN11N21;
        covN22N11 = covN11N22;
        covN22N21 = covN21N22;
        covN22N12 = covN12N22;
        sigma = new double[4][4];
        sigma[0][0] = varN11;
        sigma[0][1] = covN11N12;
        sigma[0][2] = covN11N21;
        sigma[0][3] = covN11N22;
        sigma[1][0] = covN12N11;
        sigma[1][1] = varN12;
        sigma[1][2] = covN12N21;
        sigma[1][3] = covN12N22;
        sigma[2][0] = covN21N11;
        sigma[2][1] = covN21N12;
        sigma[2][2] = varN21;
        sigma[2][3] = covN21N22;
        sigma[3][0] = covN22N11;
        sigma[3][1] = covN22N12;
        sigma[3][2] = covN22N21;
        sigma[3][3] = varN22;
        sigmaD = new Matrix(sigma);
        NDp = new double[1][4];
        NDp[0][0] = N11 - EN11;
        NDp[0][1] = N12 - EN12;
        NDp[0][2] = N21 - EN21;
        NDp[0][3] = N22 - EN22;
        NDpM = new Matrix(NDp);
        ND = new double[4][1];
        ND[0][0] = N11 - EN11;
        ND[1][0] = N12 - EN12;
        ND[2][0] = N21 - EN21;
        ND[3][0] = N22 - EN22;
        NDM = new Matrix(ND);
        success = true;
        
        ge = new GeneralizedInverse(sigma, sigma.length, sigma[0].length);
        sigmaInv = null;
        //sigmaInv = ge.ginv();
        sigmaInv = ge.pinv();
        ge = null;
        sigmaD = new Matrix(sigmaInv);
        CD2 = ((NDpM.times(sigmaD)).times(NDM)).getArray()[0][0];
        //sigmaD = new Matrix(sigma);
        Preferences.debug("2002 version of CD calculated via matrix quadratic form for generalized inverse = " + CD2 + "\n");
        /*try {
            sigmaD = sigmaD.inverse();
        }
        catch(RuntimeException e) {
            Preferences.debug("Singular matrix on sigmaD.inverse()\n");
            Preferences.debug("Cannot calculate CD via matrix quadratic form\n");
            success = false;
        }*/
        if (success) {
            //CD2 = ((NDpM.times(sigmaD)).times(NDM)).getArray()[0][0];
            //Preferences.debug("2002 version of CD calculated via matrix quadratic form for inverse = " + CD2 + "\n");
            if (CD2 > 0.0) {
                degreesOfFreedom = 2;
                stat = new Statistics(Statistics.CHI_SQUARED_CUMULATIVE_DISTRIBUTION_FUNCTION,
                        CD2, degreesOfFreedom, chiSquaredPercentile);
                stat.run();
                Preferences.debug("chiSquared percentile for 2002 version of Dixon's overall test of segregation = " + chiSquaredPercentile[0]*100.0 + "\n");
                System.out.println("chiSquared percentile for 2002 version of Dixon's overall test of segregation = " + chiSquaredPercentile[0]*100.0);
                
                if (chiSquaredPercentile[0] > 0.950) {
                    Preferences.debug("chiSquared test rejects random object distribution\n");
                    System.out.println("chiSquared test rejects random object distribution"); 
                }
                else {
                    Preferences.debug("chiSquared test does not reject random object distribution\n");
                    System.out.println("chiSquared test does not reject random object distribution");
                }
            } // if (CD2 > 0.0)
            else {
                Preferences.debug("CD2 should be positive\n");
            }
        }
        
        if (selfTest1) {
            Preferences.debug("Should have CD = 19.67\n");
        }
        
        
        
        Preferences.debug("Dixon's cell-specific tests of segregation\n");
        System.out.println("Dixon's cell-specific tests of segregation");
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z11D, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean N11 around expected mean N11 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean N11 around expected mean N11 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of N11 indicates association\n");
            System.out.println("Low value of N11 indicates association");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of N11 indicates segregation\n");
            System.out.println("High value of N11 indicates segregation");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on N11 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on N11 value");
        }
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z12D, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean N12 around expected mean N12 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean N12 around expected mean N12 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of N12 indicates segregation\n");
            System.out.println("Low value of N12 indicates segregation");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of N12 indicates association\n");
            System.out.println("High value of N12 indicates association");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on N12 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on N12 value");
        }
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z21D, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean N21 around expected mean N21 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean N21 around expected mean N21 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of N21 indicates segregation\n");
            System.out.println("Low value of N21 indicates segregation");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of N21 indicates association\n");
            System.out.println("High value of N21 indicates association");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on N21 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on N21 value");
        }
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z22D, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean N22 around expected mean N22 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean N22 around expected mean N22 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of N22 indicates association\n");
            System.out.println("Low value of N22 indicates association");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of N22 indicates segregation\n");
            System.out.println("High value of N22 indicates segregation");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on N22 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on N22 value");
        }
        
        T11 = N11 - ((double)(n1 - 1)*C1)/(n - 1);
        T12 = N12 - ((double)n1*C2)/(n - 1);
        T21 = N21 - ((double)n2*C1)/(n - 1);
        T22 = N22 - ((double)(n2 - 1)*C2)/(n - 1);
        
        varC1 = varN11 + varN21 + 2 * covN11N21;
        varC2 = varN12 + varN22 + 2 * covN12N22;
        covN11C1 = varN11 + covN11N21;
        covN12C2 =  varN12 + covN12N22;
        covN21C1 = varN21 + covN11N21;
        covN22C2 =  varN22 + covN12N22;
        varT11 = varN11 + (n1 - 1)*(n1 - 1)*varC1/((n - 1)*(n - 1))
                  - 2 * (n1 - 1)*covN11C1/(n - 1);
        varT12 = varN12 + n1*n1*varC2/((n - 1) * (n - 1))
                 - 2 * n1*covN12C2/(n - 1);
        varT21 = varN21 + n2*n2*varC1/((n - 1)*(n - 1))
                 - 2 * n2*covN21C1/(n - 1);
        varT22 = varN22 + (n2 - 1)*(n2 - 1)*varC2/((n - 1)*(n - 1))
                 - 2 * (n2 - 1)*covN22C2/(n - 1);
        
        z11N = T11/Math.sqrt(varT11);
        z12N = T12/Math.sqrt(varT12);
        z21N = T21/Math.sqrt(varT21);
        z22N = T22/Math.sqrt(varT22);
        Preferences.debug("z11N = " + z11N + "\n");
        if (selfTest1) {
            Preferences.debug("Should have z11N = 3.63\n");
        }
        Preferences.debug("z21N = " + z21N + "\n");
        if (selfTest1) {
            Preferences.debug("Should have z21 = -3.63\n");
        }
        Preferences.debug("Should have z11N = -z21N for 2 class case\n");
        Preferences.debug("z12N = " + z12N + "\n");
        if (selfTest1) {
            Preferences.debug("Should have z12N = -3.61\n");
        }
        Preferences.debug("z22N = " + z22N + "\n");
        if (selfTest1) {
            Preferences.debug("Should have z22N = 3.61\n");
        }
        Preferences.debug("Should have z12N = -z22N for 2 class case\n");
        
        // CN = T'SigmaNInverseT
        // CN has a chiSquared distribution with 1 degree of freedom
        // T' = [T11 T12 T21 T22]
        // T = [T11]
        //     [T12]
        //     [T21]
        //     [T22]
        // SigmaN = [varT11    covT11T12  covT11T21  covT11T22]
        //          [covT12T11 varT12     covT12T21  covT12T22]
        //          [covT21T11 covT21T12  varT21     covT21T22]
        //          [covT22T11 covT22T12  covT22T21  varT22]
        
        covN11C2 = covN11N12 + covN11N22;
        covN12C1 = covN12N11 + covN12N21;
        covC1C2 = covN11N12 + covN11N22 + covN21N12 + covN21N22;
        covT11T12 = covN11N12 - n1*covN11C2/(n - 1) - (n1 - 1)*covN12C1/(n - 1)
                    + (n1 - 1)*n1*covC1C2/((n - 1)*(n - 1));
        //covT11T12 = covN11N12 - n2*covN11C2/(n - 1) - (n1 - 1)*covN12C1/(n - 1)
        //+ (n1 - 1)*n2*covC1C2/((n - 1)*(n - 1));
        covC1C1 = varN11 + covN11N21 + covN21N11 + varN21;
        covT11T21 = covN11N21 - n2*covN11C1/(n - 1) - (n1 - 1)*covN21C1/(n - 1)
                    + (n1 - 1)*n2*covC1C1/((n - 1)*(n - 1));
        //covT11T21 = covN11N21 - n1*covN11C1/(n - 1) - (n1 - 1)*covN21C1/(n - 1)
        //+ (n1 - 1)*n1*covC1C1/((n - 1)*(n - 1));
        covN22C1 = covN22N11 + covN22N21;
        covT11T22 = covN11N22 - (n2 - 1)*covN11C2/(n - 1) - (n1 - 1)*covN22C1/(n - 1)
                    + (n1 - 1)*(n2 - 1)*covC1C2/((n - 1)*(n - 1));
        covT12T11 = covT11T12;
        covC2C1 = covC1C2;
        covN21C2 = covN21N12 + covN21N22;
        covT12T21 = covN12N21 - n2*covN12C1/(n - 1) - n1*covN21C2/(n - 1)
                    + n1*n2*covC2C1/((n - 1)*(n - 1));
        //covT12T21 = covN12N21 - n1*covN12C1/(n - 1) - n1*covN21C2/(n - 1)
        //+ n1*n1*covC2C1/((n - 1)*(n - 1));
        covC2C2 = varN12 + covN12N22 + covN22N12 + varN22;
        covT12T22 = covN22N12 - n1*covN22C2/(n - 1) - (n2 - 1)*covN12C2/(n - 1)
                    + (n2 - 1)*n1*covC2C2/((n - 1)*(n - 1));
        covT21T11 = covT11T21;
        covT21T12 = covT12T21;
        covT21T22 = covN22N21 - n2*covN22C1/(n - 1) - (n2 - 1)*covN21C2/(n - 1)
                    + (n2 - 1)*n2*covC2C1/((n - 1)*(n - 1));
        covT22T11 = covT11T22;
        covT22T12 = covT12T22;
        covT22T21 = covT21T22;
        sigma[0][0] =  varT11;
        sigma[0][1] = covT11T12;
        sigma[0][2] = covT11T21;
        sigma[0][3] = covT11T22;
        sigma[1][0] = covT12T11;
        sigma[1][1] =  varT12;
        sigma[1][2] = covT12T21;
        sigma[1][3] = covT12T22;
        sigma[2][0] = covT21T11;
        sigma[2][1] = covT21T12;
        sigma[2][2] = varT21;
        sigma[2][3] = covT21T22;
        sigma[3][0] = covT22T11;
        sigma[3][1] = covT22T12;
        sigma[3][2] = covT22T21;
        sigma[3][3] = varT22;
        sigmaN = new Matrix(sigma);
        Tp = new double[1][4];
        Tp[0][0] = T11;
        Tp[0][1] = T12;
        Tp[0][2] = T21;
        Tp[0][3] = T22;
        TpM = new Matrix(Tp);
        T = new double[4][1];
        T[0][0] = T11;
        T[1][0] = T12;
        T[2][0] = T21;
        T[3][0] = T22;
        TM = new Matrix(T);
        success = true;
        ge = new GeneralizedInverse(sigma, sigma.length, sigma[0].length);
        sigmaInv = null;
        //sigmaInv = ge.ginv();
        sigmaInv = ge.pinv();
        ge = null;
        sigmaN = new Matrix(sigmaInv);
        CN = ((TpM.times(sigmaN)).times(TM)).getArray();
        Preferences.debug("CN for generalized inverse = " + CN[0][0] + "\n");
        /*sigmaN = new Matrix(sigma);
        try {
            sigmaN = sigmaN.inverse();
        }
        catch(RuntimeException e) {
            Preferences.debug("Singular matrix on sigmaN.inverse()\n");
            Preferences.debug("Cannot calculate CN\n");
            success = false;
        }*/
        if (success) {
            //CN = ((TpM.times(sigmaN)).times(TM)).getArray();
            //Preferences.debug("CN for inverse = " + CN[0][0] + "\n");
            if (selfTest1) {
                Preferences.debug("Ceyhan incorrectly has CN = 13.11\n");
                Preferences.debug("Correct value = CD = 19.67\n");
            }
            if (CN[0][0] > 0.0) {
                degreesOfFreedom = 1;
                stat = new Statistics(Statistics.CHI_SQUARED_CUMULATIVE_DISTRIBUTION_FUNCTION,
                        CN[0][0], degreesOfFreedom, chiSquaredPercentile);
                stat.run();
                Preferences.debug("chiSquared percentile for Ceyhan's overall test of segregation = " + chiSquaredPercentile[0]*100.0 + "\n");
                System.out.println("chiSquared percentile for Ceyhan's overall test of segregation = " + chiSquaredPercentile[0]*100.0);
                
                if (chiSquaredPercentile[0] > 0.950) {
                    Preferences.debug("chiSquared test rejects random object distribution\n");
                    System.out.println("chiSquared test rejects random object distribution"); 
                }
                else {
                    Preferences.debug("chiSquared test does not reject random object distribution\n");
                    System.out.println("chiSquared test does not reject random object distribution");
                }
            } // if (CN[0][0] > 0.0)
            else {
                Preferences.debug("CN should be positive\n");
            }
        } // if (success)
        
        Preferences.debug("Ceyhan's cell-specific tests of segregation\n");
        System.out.println("Ceyhan's cell-specific tests of segregation");
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z11N, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean T11 around expected mean T11 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean T11 around expected mean T11 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of T11 indicates association\n");
            System.out.println("Low value of T11 indicates association");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of T11 indicates segregation\n");
            System.out.println("High value of T11 indicates segregation");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on T11 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on T11 value");
        }
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z12N, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean T12 around expected mean T12 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean T12 around expected mean T12 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of T12 indicates segregation\n");
            System.out.println("Low value of T12 indicates segregation");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of T12 indicates association\n");
            System.out.println("High value of T12 indicates association");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on T12 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on T12 value");
        }
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z21N, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean T21 around expected mean T21 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean T21 around expected mean T21 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of T21 indicates segregation\n");
            System.out.println("Low value of T21 indicates segregation");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of T21 indicates association\n");
            System.out.println("High value of T21 indicates association");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on T21 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on T21 value");
        }
        
        stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, z22N, 0, percentile);
        stat.run();
        Preferences.debug("Percentile in Gaussian probability integral for measured mean T22 around expected mean T22 = "
                + percentile[0]*100.0 + "\n");
        System.out.println("Percentile in Gaussian probability integral for measured mean T22 around expected mean T22 = " +
                  percentile[0]*100.0);
        if (percentile[0] < 0.025) {
            Preferences.debug("Low value of T22 indicates association\n");
            System.out.println("Low value of T22 indicates association");
        }
        else if (percentile[0] > 0.975) {
            Preferences.debug("High value of T22 indicates segregation\n");
            System.out.println("High value of T22 indicates segregation");
        }
        else {
            Preferences.debug("Complete spatial randomness cannot be rejected based on T22 value\n");
            System.out.println("Complete spatial randomness cannot be rejected based on T22 value");
        }
        
        // Double check with matrix forms
        // Number of classes
        int nc;
        if (selfTest2) {
            nc = 5;
        }
        else {
            nc = 2;
        }
        long N[][] = new long[nc][nc];
        int C[] = new int[nc];
        long nn[] = new long[nc];
        double ENij[][] = new double[nc][nc];
        double p2[][] = new double[nc][nc];
        double p3[][][] = new double[nc][nc][nc];
        double p4[][][][] = new double[nc][nc][nc][nc];
        double denom;
        double covN[][][][] = new double[nc][nc][nc][nc];
        double zijD[][] = new double[nc][nc];
        double Tij[][] = new double[nc][nc];
        double covC[][] = new double[nc][nc];
        double covNC[][][] = new double[nc][nc][nc];
        double covT[][][][] = new double[nc][nc][nc][nc];
        int k;
        int m;
        double zN[][] = new double[nc][nc];
        if (selfTest2) {
            N[0][0] = 112;
            N[0][1] = 40;
            N[0][2] = 29;
            N[0][3] = 20;
            N[0][4] = 14;
            N[1][0] = 38;
            N[1][1] = 117;
            N[1][2] = 26;
            N[1][3] = 16;
            N[1][4] = 8;
            N[2][0] = 23;
            N[2][1] = 23;
            N[2][2] = 82;
            N[2][3] = 22;
            N[2][4] = 6;
            N[3][0] = 19;
            N[3][1] = 29;
            N[3][2] = 29;
            N[3][3] = 14;
            N[3][4] = 7;
            N[4][0] = 7;
            N[4][1] = 8;
            N[4][2] = 5;
            N[4][3] = 7;
            N[4][4] = 33;
            Q = 472;
            R = 454;
        } // if (selfTest2)
        else { // not selfTest2
            N[0][0] = N11;
            N[0][1] = N12;
            N[1][0] = N21;
            N[1][1] = N22;
        } // else not selfTest2
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                C[i] += N[j][i];
                nn[i] += N[i][j];
            }
        }
        
        n = 0;
        for (i = 0; i < nc; i++) {
            n += nn[i];
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                if (i == j) {
                    ENij[i][j] = ((double)nn[i]*(nn[i] - 1))/(n - 1);
                }
                else {
                    ENij[i][j] = ((double)nn[i]*nn[j])/(n - 1);
                }
            }
        }
        
        denom = n * (n - 1);
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                if (i == j) {
                    p2[i][j] = ((double)nn[i]*(nn[i] - 1))/denom;
                }
                else {
                    p2[i][j] = ((double)nn[i]*nn[j])/denom;
                }
            }
        }
        
        denom = n * (n - 1) * (n - 2);
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    if ((i == j) && (i == k)) {
                        p3[i][j][k] = ((double)nn[i]*(nn[i] - 1)*(nn[i] - 2))/denom;
                    }
                    else if (i == j) {
                        p3[i][j][k] = ((double)nn[i]*(nn[i] - 1)*nn[k])/denom;
                    }
                    else if (i == k) {
                        p3[i][j][k] = ((double)nn[i]*(nn[i] - 1)*nn[j])/denom;
                    }
                    else if (j == k) {
                        p3[i][j][k] = ((double)nn[i]*nn[j]*(nn[j] - 1))/denom;
                    }
                    else {
                        p3[i][j][k] = ((double)nn[i]*nn[j]*nn[k])/denom;
                    }
                }
            }
        }
        
        denom = n * (n - 1) * (n - 2) * (n - 3);
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    for (m = 0; m < nc; m++) {
                        if ((i == j) && (i == k) && (i == m)) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * (nn[i] - 2) * (nn[i] - 3))/denom;
                        }
                        else if ((i == j) && (i == k)) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * (nn[i] - 2) * nn[m])/denom;
                        }
                        else if ((i == j) && (i == m)) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * (nn[i] - 2) * nn[k])/denom;
                        }
                        else if ((i == k) && (i == m)) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * (nn[i] - 2) * nn[j])/denom;
                        }
                        else if ((j == k) && (j == m)) {
                            p4[i][j][k][m] = ((double)nn[i] * nn[j] * (nn[j] - 1) * (nn[j] - 2))/denom;
                        }
                        else if ((i == j) && (k == m)) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * nn[k] * (nn[k] - 1))/denom;
                        }
                        else if ((i == k) && (j == m)) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * nn[j] * (nn[j] - 1))/denom;
                        }
                        else if ((i == m) && (j == k)) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * nn[j] * (nn[j] - 1))/denom;
                        }
                        else if (i == j) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * nn[k] * nn[m])/denom;
                        }
                        else if (i == k) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * nn[j] * nn[m])/denom;
                        }
                        else if (i == m) {
                            p4[i][j][k][m] = ((double)nn[i] * (nn[i] - 1) * nn[j] * nn[k])/denom;
                        }
                        else if (j == k) {
                            p4[i][j][k][m] = ((double)nn[i] * nn[j] * (nn[j] - 1) * nn[m])/denom;
                        }
                        else if (j == m) {
                            p4[i][j][k][m] = ((double)nn[i] * nn[j] * (nn[j] - 1) * nn[k])/denom;
                        }
                        else if (k == m) {
                            p4[i][j][k][m] = ((double)nn[i] * nn[j] * nn[k] * (nn[k] - 1))/denom;
                        }
                        else {
                            p4[i][j][k][m] = ((double)nn[i] * nn[j] * nn[k] * nn[m])/denom;
                        }
                    }
                }
            }
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    for (m = 0; m < nc; m++) {
                        if ((i == j) && (i == k) && (i == m)) {
                            covN[i][j][k][m] = (n + R)*p2[i][i] + (2*n - 2*R + Q)*p3[i][i][i]
                            + (n*n - 3*n - Q + R)*p4[i][i][i][i] - n*n*p2[i][i]*p2[i][i];
                        }
                        else if ((i == k) && (j == m)) {
                            covN[i][j][k][m] = n*p2[i][j] + Q*p3[i][i][j]
                            + (n*n - 3*n - Q + R)*p4[i][i][j][j] - n*n*p2[i][j]*p2[i][j];
                        }
                        else if ((i == j) && (k == m)) {
                            covN[i][j][k][m] = (n*n - 3*n - Q + R)*p4[i][i][k][k] - n*n*p2[i][i]*p2[k][k];
                        }
                        else if ((i == j) && (i == k)) {
                            covN[i][j][k][m] = (n - R)*p3[i][i][m] + (n*n - 3*n - Q + R)*p4[i][i][i][m]
                            - n*n*p2[i][i]*p2[i][m];
                        }
                        else if ((i == k) && (i == m)) {
                            covN[i][j][k][m] = (n - R)*p3[i][i][j] + (n*n - 3*n - Q + R)*p4[i][i][i][j]
                            - n*n*p2[i][i]*p2[i][j];
                        }
                        else if ((i == j) && (i == m)) {
                            covN[i][j][k][m] = (n - R + Q)*p3[i][i][k]
                            + (n*n - 3*n - Q + R)*p4[i][i][i][k] - n*n*p2[i][i]*p2[i][k];
                        }
                        else if ((j == k) && (k == m)) {
                            covN[i][j][k][m] = (n - R + Q)*p3[j][j][i]
                            + (n*n - 3*n - Q + R)*p4[j][j][j][i] - n*n*p2[j][j]*p2[j][i];
                        }
                        else if (i == j) {
                            covN[i][j][k][m] = (n*n - 3*n - Q + R)*p4[i][i][k][m] - n*n*p2[i][i]*p2[k][m];
                        }
                        else if (k == m) {
                            covN[i][j][k][m] = (n*n - 3*n - Q + R)*p4[k][k][i][j] - n*n*p2[k][k]*p2[i][j];
                        }
                        else if (i == k) {
                            covN[i][j][k][m] = (n*n - 3*n - Q + R)*p4[i][i][j][m] - n*n*p2[i][j]*p2[i][m];
                        }
                        else if ((i == m) && (j == k)) {
                            covN[i][j][k][m] = R*p2[i][j] + (n - R)*(p3[i][i][j] + p3[i][j][j])
                            + (n*n - 3*n - Q + R)*p4[i][i][j][j] - n*n*p2[i][j]*p2[j][i];
                        }
                        else if (j == k) {
                            covN[i][j][k][m] = (n - R)*p3[i][j][m] + (n*n - 3*n - Q + R)*p4[i][j][j][m]
                            - n*n*p2[i][j]*p2[j][m];
                        }
                        else if (i == m) {
                            covN[i][j][k][m] = (n - R)*p3[i][j][k] + (n*n - 3*n - Q + R)*p4[i][i][j][k]
                            - n*n*p2[i][j]*p2[i][k];
                        }
                        else if (j == m) {
                            covN[i][j][k][m] = Q*p3[i][j][k] + (n*n - 3*n - Q + R)*p4[i][j][j][k]
                            - n*n*p2[i][j]*p2[j][k];
                        }
                        else {
                            covN[i][j][k][m] = (n*n - 3*n - Q + R)*p4[i][j][k][m] - n*n*p2[i][j]*p2[k][m];
                        }
                    }
                }
            }
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                zijD[i][j] = (N[i][j] - ENij[i][j])/Math.sqrt(covN[i][j][i][j]);
                Preferences.debug("z" + (i+1) + (j+1) + "D = " + zijD[i][j] + "\n");
            }
        }
        
        sigma = new double[nc*nc][nc*nc];
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    for (m = 0; m < nc; m++) {
                        sigma[i*nc + j][k * nc + m] = covN[i][j][k][m];
                    }
                }
            }
        }
        
        sigmaD = new Matrix(sigma);
        NDp = new double[1][nc*nc];
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                NDp[0][i*nc + j] = N[i][j] - ENij[i][j];
            }
        }
        
        NDpM = new Matrix(NDp);
        ND = new double[nc*nc][1];
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                ND[i*nc + j][0] = N[i][j] - ENij[i][j];
            }
        }
        NDM = new Matrix(ND);
        success = true;
        ge = new GeneralizedInverse(sigma, sigma.length, sigma[0].length);
        sigmaInv = null;
        //sigmaInv = ge.ginv();
        sigmaInv = ge.pinv();
        ge = null;
        sigmaD = new Matrix(sigmaInv);
        CD2 = ((NDpM.times(sigmaD)).times(NDM)).getArray()[0][0];
        Preferences.debug("CD for generalized inverse = " + CD2 + "\n");
        /*sigmaD = new Matrix(sigma);
        try {
            sigmaD = sigmaD.inverse();
        }
        catch(RuntimeException e) {
            Preferences.debug("Singular matrix on sigmaD.inverse()\n");
            Preferences.debug("Cannot calculate CD via matrix quadratic form\n");
            success = false;
        }*/
        if (success) {
            //CD2 = ((NDpM.times(sigmaD)).times(NDM)).getArray()[0][0];
            //Preferences.debug("CD for inverse = " + CD2 + "\n");
            
            if (CD2 > 0.0) {
                // Under random labelling the chi squared statistic has degrees of freedom = 6;
                degreesOfFreedom = nc*(nc - 1);
                stat = new Statistics(Statistics.CHI_SQUARED_CUMULATIVE_DISTRIBUTION_FUNCTION,
                        CD2, degreesOfFreedom, chiSquaredPercentile);
                stat.run();
                Preferences.debug("chiSquared percentile for Dixon's 2002 overall test of segregation = " + chiSquaredPercentile[0]*100.0 + "\n");
                
                if (chiSquaredPercentile[0] > 0.950) {
                    Preferences.debug("chiSquared test rejects random object distribution\n");
                }
                else {
                    Preferences.debug("chiSquared test does not reject random object distribution\n");
                }
            } // if (CD > 0.0)
            else {
                Preferences.debug("CD should be positive\n");
            }
        } // if (success)
        
        Preferences.debug("Dixon's cell-specific tests of segregation\n");
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, zijD[i][j], 0, percentile);
                stat.run();
                Preferences.debug("Percentile in Gaussian probability integral for measured mean N" + (i+1) + (j+1) + 
                                   " around expected mean N" + (i+1) + (j+1) + " = "
                        + percentile[0]*100.0 + "\n");
                if (i == j) {
                    if (percentile[0] < 0.025) {
                        Preferences.debug("Low value of N" + (i+1) + (j + 1)+ " indicates association\n");
                    }
                    else if (percentile[0] > 0.975) {
                        Preferences.debug("High value of N" + (i+1) + (j+1) + " indicates segregation\n");
                    }
                    else {
                        Preferences.debug("Complete spatial randomness cannot be rejected based on N" +
                                         (i+1) + (j+1)+ " value\n");
                    }
                } // if (i == j)
                else { // i <> j
                    if (percentile[0] < 0.025) {
                        Preferences.debug("Low value of N" + (i+1) + (j + 1)+ " indicates segregation\n");
                    }
                    else if (percentile[0] > 0.975) {
                        Preferences.debug("High value of N" + (i+1) + (j+1) + " indicates association\n");
                    }
                    else {
                        Preferences.debug("Complete spatial randomness cannot be rejected based on N" +
                                         (i+1) + (j+1)+ " value\n");
                    }    
                } // else i <> j
            } // for (j = 0; j < nc; j++)
        } // for (i = 0; i < nc; i++)
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                if (i == j) {
                    Tij[i][j] = N[i][j] - ((double)(nn[i] - 1)*C[j])/(n - 1);
                }
                else {
                    Tij[i][j] = N[i][j] - ((double)nn[i]*C[j])/(n - 1);
                }
            }
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    for (m = 0; m <nc; m++) {
                        covC[i][j] += covN[k][i][m][j];
                    }
                }
            }
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    for (m = 0; m < nc; m++) {
                        covNC[i][j][k] += covN[i][j][m][k];
                    }
                }
            }
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    for (m = 0; m < nc; m++) {
                        if ((i == j) && (i == k) && (i == m)) {
                            covT[i][j][k][m] = covN[i][j][k][m] 
                            + ((nn[i] - 1)*(nn[i] - 1)*covC[j][j])/((n - 1)*(n - 1))
                            - (2 * (nn[i] - 1) * covNC[i][j][j])/(n - 1);
                        }
                        else if ((i == k) && (j == m)) {
                            covT[i][j][k][m] = covN[i][j][k][m]
                            + (nn[i]*nn[i]*covC[j][j])/((n - 1)*(n - 1))
                            - (2 * nn[i] * covNC[i][j][j])/(n - 1);
                        }
                        else if ((i == j) && (k == m)) {
                            covT[i][j][k][m] = covN[i][i][k][k]
                            - ((nn[k] - 1)*covNC[i][i][k])/(n - 1)
                            - ((nn[i] - 1)*covNC[k][k][i])/(n - 1)
                            + ((nn[i] - 1)*(nn[k] - 1)*covC[i][k])/((n - 1)*(n - 1));
                        }
                        else if (i == j) {
                            covT[i][j][k][m] = covN[i][i][k][m]
                            - (nn[k]*covNC[i][i][m])/(n - 1)
                            - ((nn[i] - 1)*covNC[k][m][i])/(n - 1)
                            + ((nn[i] - 1)*nn[k]*covC[i][m])/((n - 1)*(n - 1));
                        }
                        else if (k == m) {
                            covT[i][j][k][m] = covN[k][k][i][j]
                            - (nn[i]*covNC[k][k][j])/(n - 1)
                            - ((nn[k] - 1)*covNC[i][j][k])/(n - 1)
                            + ((nn[k] - 1)*nn[i]*covC[k][j])/((n - 1)*(n - 1));
                        }
                        else {
                            covT[i][j][k][m] = covN[i][j][k][m]
                            - (nn[k]*covNC[i][j][m])/(n - 1)
                            - (nn[i] * covNC[k][m][j])/(n - 1)
                            + (nn[i]*nn[k]*covC[j][m])/((n - 1)*(n - 1));
                        }
                    }
                }
            }
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                zN[i][j] = Tij[i][j]/Math.sqrt(covT[i][j][i][j]);
                Preferences.debug("z" + (i+1) + (j+1) + "N = " + zN[i][j] + "\n");
            }
        }
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                for (k = 0; k < nc; k++) {
                    for (m = 0; m < nc; m++) {
                        sigma[i*nc + j][k * nc + m] = covT[i][j][k][m];
                    }
                }
            }
        }
        
        sigmaN = new Matrix(sigma);
        Tp = new double[1][nc*nc];
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                Tp[0][i*nc + j] = Tij[i][j];
            }
        }
        
        TpM = new Matrix(Tp);
        T = new double[nc*nc][1];
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                T[i*nc + j][0] = Tij[i][j];
            }
        }
        
        TM = new Matrix(T);
        success = true;
        ge = new GeneralizedInverse(sigma, sigma.length, sigma[0].length);
        sigmaInv = null;
        //sigmaInv = ge.ginv();
        sigmaInv = ge.pinv();
        ge = null;
        sigmaN = new Matrix(sigmaInv);
        CN = ((TpM.times(sigmaN)).times(TM)).getArray();
        Preferences.debug("CN for generalized inverse = " + CN[0][0] + "\n");
        /*sigmaN = new Matrix(sigma);
        try {
            sigmaN = sigmaN.inverse();
        }
        catch(RuntimeException e) {
            Preferences.debug("Singular matrix on sigmaN.inverse()\n");
            Preferences.debug("Cannot calculate CN\n");
            success = false;
        }*/
        if (success) {
            //CN = ((TpM.times(sigmaN)).times(TM)).getArray();
            //Preferences.debug("CN for inverse = " + CN[0][0] + "\n");
            if (CN[0][0] > 0.0) {
                degreesOfFreedom = (nc - 1)*(nc - 1);
                stat = new Statistics(Statistics.CHI_SQUARED_CUMULATIVE_DISTRIBUTION_FUNCTION,
                        CN[0][0], degreesOfFreedom, chiSquaredPercentile);
                stat.run();
                Preferences.debug("chiSquared percentile for Ceyhan's overall test of segregation = " + chiSquaredPercentile[0]*100.0 + "\n");
                System.out.println("chiSquared percentile for Ceyhan's overall test of segregation = " + chiSquaredPercentile[0]*100.0);
                
                if (chiSquaredPercentile[0] > 0.950) {
                    Preferences.debug("chiSquared test rejects random object distribution\n");
                    System.out.println("chiSquared test rejects random object distribution"); 
                }
                else {
                    Preferences.debug("chiSquared test does not reject random object distribution\n");
                    System.out.println("chiSquared test does not reject random object distribution");
                }
            } // if (CN[0][0] > 0.0)
            else {
                Preferences.debug("CN should be positive\n");
            }
        } // if (success)
        
        Preferences.debug("Ceyhan's cell-specific tests of segregation\n");
        
        for (i = 0; i < nc; i++) {
            for (j = 0; j < nc; j++) {
                stat = new Statistics(Statistics.GAUSSIAN_PROBABILITY_INTEGRAL, zN[i][j], 0, percentile);
                stat.run();
                Preferences.debug("Percentile in Gaussian probability integral for measured mean T" + (i+1) + (j+1) + 
                                   " around expected mean T" + (i+1) + (j+1) + " = "
                        + percentile[0]*100.0 + "\n");
                if (i == j) {
                    if (percentile[0] < 0.025) {
                        Preferences.debug("Low value of T" + (i+1) + (j + 1)+ " indicates association\n");
                    }
                    else if (percentile[0] > 0.975) {
                        Preferences.debug("High value of T" + (i+1) + (j+1) + " indicates segregation\n");
                    }
                    else {
                        Preferences.debug("Complete spatial randomness cannot be rejected based on T" +
                                         (i+1) + (j+1)+ " value\n");
                    }
                } // if (i == j)
                else { // i <> j
                    if (percentile[0] < 0.025) {
                        Preferences.debug("Low value of T" + (i+1) + (j + 1)+ " indicates segregation\n");
                    }
                    else if (percentile[0] > 0.975) {
                        Preferences.debug("High value of T" + (i+1) + (j+1) + " indicates association\n");
                    }
                    else {
                        Preferences.debug("Complete spatial randomness cannot be rejected based on T" +
                                         (i+1) + (j+1)+ " value\n");
                    }    
                } // else i <> j
            } // for (j = 0; j < nc; j++)
        } // for (i = 0; i < nc; i++)
        
        red = new byte[buffer.length];
        green = new byte[buffer.length];
        
        for (i = 0; i < buffer.length; i++) {
            if (buffer[i] == 1) {
                red[i] = (byte)255;
            }
            else if (buffer[i] == 2) {
                green[i] = (byte)255;
            }
        }
        
        try {
            srcImage.importRGBData(1, 0, red, false);
            srcImage.importRGBData(2, 0, green, true);
        }
        catch(IOException e) {
            MipavUtil.displayError("IO exception on srcImage.importRGBData");
            setCompleted(false);
            return;
        }
       
        setCompleted(true);
        return;
    }
}
