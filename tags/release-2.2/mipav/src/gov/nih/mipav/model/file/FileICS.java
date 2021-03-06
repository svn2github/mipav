package gov.nih.mipav.model.file;

import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.view.*;

import java.awt.*;
import java.io.*;
import java.util.zip.*;

/**
*
*  Reference:
*  1. Proposed Standard for Image Cytometry Data Files by Philip Dean, Laura Mascio,
*     David Ow, Damir Sudar, and James Mullikin, Cytometry, 11, pp. 561- 569, 1990.
*
*  2. libics v.1.3 Online Documentation, copyright 2000-2002 by Cris Luengo.
*     http://www.ph.tn.tudelft.nl/~cris/libics
*       @author     William Gandler
*       @see        FileIO
*/

public class FileICS extends FileBase {



    private FileInfoICS    fileInfo;
    private File            file;
    private String          fileName;
    private String          headerFileName;
    private String          fileDir;
    private byte            fieldSeparator;
    private String          category;
    private String          subcategory;
    private int             numValues;
    private String          values[] = new String[10];
    private String          version;
    private String          dataSetFileName;
    private int             parameters = -1;
    private int             numColors = 1;
    private String          order[];
    private boolean         exchangeXY = false;
    private int             sizes[];
    private boolean         invertY = false;
    private int             significant_bits = -1;
    private String          format = new String("integer");
    private String          sign = null;
    private String          compression = new String("uncompressed");
    private int             dataType;
    private String          dataFileName = null;
    private long            dataOffset = 0L;
    private float           origin[] = null;
    private float           startLocations[];
    private float           scale[] = null;
    private float           imgResols[];
    private String          labels[] = null;
    private String          units[] = null;
    private int             unitsOfMeasure[] = null;
    private String          probe[] = null;
    private String          sensorType = null;
    private String          sensorModel = null;
    private String          channels[] = null;
    private String          pinholeRadius[] = null;
    private String          lambdaEx[] = null;
    private String          lambdaEm[] = null;
    private String          exPhotonCnt[] = null;
    private String          refrInxMedium[] = null;
    private String          numAperture[] = null;
    private String          refrInxLensMedium[] = null;
    private int             historyNumber = 0;
    private String          history[] = new String[300];
    private String          actualHistory[] = null;
    private String          captureVersion = null;
    private String          filterExposureTimeX = null;
    private String          filterFluorophoreX[] = null;
    private String          mapchannel[] = null;
    private String          plateChamberID = null;
    private String          plateVesselID = null;
    private String          specimen[] = null;
    private String          specimenSpecies[] = null;
    private String          scilType = null;
    private int             xPos = -1;
    private int             yPos = -1;
    private int             zPos = -1;
    private int             tPos = -1;
    private int             rgbPos = -1;
    private int             nDims = 0;
    private String          unitsStr[] = null;
    private static final int RGB_FIRST = 1;
    private static final int RGB_BETWEEN_SPACEANDTIME = 2;
    private static final int RGB_LAST = 3;
    private int colorSpacing = RGB_LAST;
    private int endBytes = 0;
    private int endShorts = 0;
    private int endFloats = 0;

    private int imgExtents[];


    private int              numberSlices; // zDim for 3D and zDim * tDim for 4D
    private int              numberSpaceSlices = 1; // zDim if x, y, and z are
                                                    // present; 1 otherwise
    private ModelLUT        LUT;
    private  ModelImage     image;
    private  boolean        endianess;
    private  ViewUserInterface   UI;


    private  float imgBuffer[]      = null;
    private  float imgBuffer2[]     = null;
    private  float imgBufferI[]     = null;
    private  float imgBufferI2[]    = null;
    private  long  imgLBuffer[]     = null;
    private  long  imgLBuffer2[]    = null;
    private  double imgDBuffer[]    = null;
    private  double imgDBuffer2[]   = null;
    private  double imgDBufferI[]   = null;
    private  double imgDBufferI2[]  = null;
    private  int bufferSize;

    private  boolean useGZIP = false;

    /**
    *   ICS reader constructor
    *   @param _UI              user interface reference
    *   @param fileName         file name
    *   @param fileDir          file directory
    *   @exception IOException  if there is an error making the file
    */
    public FileICS(ViewUserInterface _UI, String fileName, String fileDir) throws IOException {

        UI              = _UI;
        this.fileName   = fileName;
        this.fileDir    = fileDir;
    }


    /**
    *   getModelLUT  - returns LUT if defined
    *   @return        the LUT if defined else it is null
    */
    public ModelLUT getModelLUT(){ return LUT;}


    /**
    *  getFileInfo  - accessor that returns the file info
    *  @return        FileInfoBase containing the file info
    */
    public FileInfoBase getFileInfo() { return fileInfo;}


    /**
    *  getImageBuffer  - accessor that returns the image buffer
    *  @return           buffer of image.
    */
    public float[] getImageBuffer() { return imgBuffer;}

    /**
    *   readLine()            - reads a line of the file header
    *   separate into category, subcategory, and values
    *   @exception IOException  if there is an error reading the file
    */
    private void readLine() throws IOException {
        String  tempString;
        int     index;
        numValues = 0;

        try {
            tempString = raFile.readLine();
        }
        catch(IOException error){
            throw(error);
        }

        if (tempString == null) {
            Preferences.debug("Null header line");
            throw new IOException("Null header line");
        }

        index = tempString.indexOf(fieldSeparator);
        if (index != -1) {
            category = tempString.substring(0,index);
            if (category.equals("end")) {
              return;
            }
        }
        else if (tempString.equals("history")) {
              category = tempString;
              subcategory = null;
              return;
        }
        else {
            Preferences.debug("Field separator between category and subcategory not found\n");
            Preferences.debug("Header line = " + tempString);
            throw new IOException("Field separator between category and subcategory not found\n");
        }

        tempString = tempString.substring(index+1);
        if (category.equals("history")) {
            subcategory = tempString;
            return;
        }
        index = tempString.indexOf(fieldSeparator);
        if (index != -1) {
            subcategory = tempString.substring(0,index);
        }
        else if (category.equals("parameter")) {
            subcategory = tempString;
            return;
        }
        else {
            Preferences.debug("Field separator between subcategory and keyword not found\n");
            Preferences.debug("Header line from subcategory = " + tempString);
            throw new IOException("Field separator between subcategory and keyword not found\n");
        }

        tempString = tempString.substring(index+1);
        index = tempString.indexOf(fieldSeparator);
        while(index != -1) {
            values[numValues++] = tempString.substring(0,index);
            tempString = tempString.substring(index+1);
            index = tempString.indexOf(fieldSeparator);
        }
        values[numValues++] = tempString;
        return;
    }

     /**
    *   Reads the image header.
    */
    private void readHeader() throws IOException {
        int s;
        int index;
        String tempString;
        boolean done;
        long fileLength;
        int i;
        boolean bigEndian;
        boolean littleEndian;
        String sTemp;

        try {
            s = fileName.lastIndexOf(".");
            if (s == -1) {
                throw new IOException("ICS file name Error: . sign not found");
            }
            headerFileName = fileName.substring(0,s+1) + "ics";
            file = new File(fileDir + headerFileName);
            raFile = new RandomAccessFile(file, "r");
            fileLength = raFile.length();

            fileInfo = new FileInfoICS(headerFileName, fileDir, FileBase.ICS);
            // The first line must contains the field separator
            // character followed by the line separator character
            try {
            tempString = raFile.readLine();
            }
            catch(IOException error){
                throw(error);
            }

            if (tempString == null) {
                Preferences.debug("First line of ICS header file is null\n");
                throw new IOException("First line of ICS header file is null");
            }


            fieldSeparator = tempString.getBytes()[0];

            // The second line must contain ics_version, followed by the field
            // separator character, followed by the version number
            try {
            tempString = raFile.readLine();
            }
            catch(IOException error){
                throw(error);
            }

            if (tempString == null) {
                Preferences.debug("Second line of ICS header file is null\n");
                throw new IOException("Second line of ICS header file is null");
            }

            index = tempString.indexOf(fieldSeparator);
            if (index != -1) {
                 category = tempString.substring(0,index);
            }
            else {
                Preferences.debug("Second line of ICS header file has no field separator\n");
                Preferences.debug("Second line = " + tempString);
                throw new IOException("Second line of ICS header file has no field separator");
            }
            if ((category.compareTo("ics_version")) != 0) {
                Preferences.debug("Second line of header file is an erroneous " + tempString
                + "\n");
                throw new IOException("Second line of ICS header file lacks ics_version");
            }
            version = tempString.substring(index+1);
            fileInfo.setVersion(version);

            // The third line of the ICS header file contains filename, followed by a
            // field separator, followed by the data set filename
            try {
            tempString = raFile.readLine();
            }
            catch(IOException error){
                throw(error);
            }

            if (tempString == null) {
                Preferences.debug("Third line of ICS header file is null");
                throw new IOException("Third line of ICS header file is null");
            }

            index = tempString.indexOf(fieldSeparator);
            if (index != -1) {
                 category = tempString.substring(0,index);
            }
            else {
                Preferences.debug("Third line of ICS header file has no field separator");
                throw new IOException("Third line of ICS header file has no field separator");
            }
            if ((category.compareTo("filename")) != 0) {
                Preferences.debug("Third line of header file is an erroneous " + tempString);
                throw new IOException("Third line of ICS header file lacks filename");
            }
            dataSetFileName = tempString.substring(index+1);
            fileInfo.setDataSetFileName(dataSetFileName);


            done = false;
            while(!done) {
                if (raFile.getFilePointer() >= (fileLength - 1)) {
                    done = true;
                    break;
                }
                readLine();

                if ((category.compareTo("layout")) == 0) {
                    if ((subcategory.compareTo("parameters")) == 0) {
                        // The first parameter has the number of bits per pixel
                        parameters = Integer.valueOf(values[0]).intValue();
                    } // if ((subcategory.compareTo("parameters")) == 0)
                    else if ((subcategory.compareTo("order")) == 0) {
                        if ((values[0].compareTo("bits")) != 0) {
                            // bits must follow the order keyword
                            Preferences.debug("order[0] = " + values[0] +
                                              " instead of the required bits\n");
                            throw new IOException("order[0] = " + values[0] +
                                              " instead of the required bits");
                        }
                        order = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                            order[i] = values[i];
                            if ((order[i].compareTo("x")) == 0) {
                              xPos = i;
                              nDims++;
                            }
                            if ((order[i].compareTo("y")) == 0) {
                              yPos = i;
                              nDims++;
                            }
                            if ((order[i].compareTo("z")) == 0) {
                              zPos = i;
                              nDims++;
                            }
                            if ((order[i].compareTo("t")) == 0) {
                              tPos = i;
                              nDims++;
                            }
                            if (((order[i].compareTo("rgb")) == 0) ||
                                ((order[i].compareTo("probe")) == 0) ||
                                ((order[i].compareTo("ch")) == 0)){
                              rgbPos = i;
                            }
                        }
                        if (yPos < xPos) {
                            exchangeXY = true;
                        }
                    } // else if ((subcategory.compareTo("order")) == 0)
                    else if ((subcategory.compareTo("sizes")) == 0) {
                        sizes = new int[numValues];
                        for (i = 0; i < numValues; i++) {
                            sizes[i] = Integer.valueOf(values[i]).intValue();
                        }
                    } // else if ((subcategory.compareTo("sizes")) == 0)
                    else if ((subcategory.compareTo("coordinates")) == 0) {
                        if ((values[0].compareTo("video")) == 0) {
                            // y increases downward - the normal default
                            invertY = false;
                        }
                        else if ((values[0].compareTo("cartesian")) == 0) {
                            // y increases upward
                            invertY = true;
                        }
                        else {
                            Preferences.debug("Illegal keyword of " + values[0] +
                                              " for coordinates\n");
                            throw new IOException("Illegal keyword of " + values[0] +
                                              " for coordinates");
                        }
                    } // else if ((subcategory.compareTo("coordinates")) == 0)
                    else if ((subcategory.compareTo("significant_bits")) == 0) {
                        // The data must be in the low order bits of the word
                        significant_bits = Integer.valueOf(values[0]).intValue();
                    }
                } // if ((category.compareTo("layout")) == 0)
                else if ((category.compareTo("representation")) == 0) {
                    if ((subcategory.compareTo("byte_order")) == 0) {
                        // The Java default is big-endian.
                        // big-endian sets endianess true.
                        bigEndian = true;
                        littleEndian = false;
                        for (i = 0; i < numValues; i++) {
                            if (Integer.valueOf(values[i]).intValue() !=
                                numValues - i) {
                                 bigEndian = false;
                            }
                        }
                        if (!bigEndian) {
                            littleEndian = true;
                            for (i = 0; i < numValues; i++) {
                                if (Integer.valueOf(values[i]).intValue() !=
                                    (i+1)) {
                                    littleEndian = false;
                                }
                            }
                        } // if (!bigEndian)
                        if (bigEndian) {
                            endianess = true;
                            fileInfo.setEndianess(endianess);
                        }
                        else if (littleEndian) {
                            endianess = false;
                            fileInfo.setEndianess(endianess);
                        }
                        else {
                            Preferences.debug("Order is not big or little endian\n");
                            Preferences.debug("Cannot handle this ordering\n");
                            throw new IOException("Order is not big or little endian");
                        }
                    } // if ((subcategory.compareTo("byte_order")) == 0)
                    else if ((subcategory.compareTo("format")) == 0) {
                        // The default is integer
                        format = values[0];
                        if (((format.compareTo("integer")) != 0) &&
                            ((format.compareTo("real")) != 0) &&
                            ((format.compareTo("complex")) != 0)) {
                            Preferences.debug("Illegal keyword of " + format +
                                            " for format\n");
                            throw new IOException("Illegal keyword of " + format +
                                            " for format");
                        }
                    } // else if ((subcategory.compareTo("format")) == 0)
                   else if ((subcategory.compareTo("sign")) == 0) {
                       // The default is unsigned for integers and signed
                       // for real and complex formats
                       sign = values[0];
                       if (((sign.compareTo("signed")) != 0) &&
                           ((sign.compareTo("unsigned")) != 0)) {
                            Preferences.debug("Illegal keyword of " + sign +
                                            " for sign\n");
                            throw new IOException("Illegal keyword of " + sign +
                                            " for sign");

                       }
                   } // else if ((subcategory.compareTo("sign")) == 0)
                   else if ((subcategory.compareTo("compression")) == 0) {
                       compression = values[0];
                       if ((compression.compareTo("uncompressed")) == 0) {
                       }
                       else if ((compression.compareTo("gzip")) == 0) {
                           Preferences.debug("Must handle gzip compression\n");
                           useGZIP =true;
                       }
                       else {
                           Preferences.debug("Cannot handle compression = " +
                                              compression + "\n");
                           throw new IOException("Cannot handle compression = " +
                                                  compression + "\n");
                       }
                   } // else if ((subcategory.compareTo("compression")) == 0)
                   else if ((subcategory.compareTo("SCIL_TYPE")) == 0) {
                       scilType = values[0];
                       fileInfo.setScilType(scilType);
                   } // else if ((subcategory.compareTo("SCIL_TYPE")) == 0)
                } // else if ((category.compareTo("representation")) == 0)
                else if ((category.compareTo("source")) == 0) {
                    if ((subcategory.compareTo("file")) == 0) {
                        dataFileName = values[0];
                        Preferences.debug("Data file = " + dataFileName + "\n");
                    }
                    else if ((subcategory.compareTo("offset")) == 0) {
                        dataOffset = Long.valueOf(values[0]).longValue();
                        Preferences.debug("Data file offset = " + dataOffset + "\n");
                    }
                } // else if ((category.compareTo("source")) == 0)
                else if ((category.compareTo("parameter")) == 0) {
                    if ((subcategory.compareTo("origin")) == 0) {
                        origin = new float[numValues];
                        for (i = 0; i < numValues; i++) {
                            origin[i] = Float.valueOf(values[i]).floatValue();
                        }
                    }
                    else if ((subcategory.compareTo("scale")) == 0) {
                        scale = new float[numValues];
                        for (i = 0; i < numValues; i++) {
                            scale[i] = Float.valueOf(values[i]).floatValue();
                        }
                    }
                    else if ((subcategory.compareTo("labels")) == 0) {
                        labels = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                            labels[i] = values[i];
                        }
                    }
                    else if ((subcategory.compareTo("units")) == 0) {
                        units = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                            units[i] = values[i];
                        }
                    }
                    else if ((subcategory.compareTo("probe")) == 0) {
                        probe = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                            probe[i] = values[i];
                        }
                        fileInfo.setProbe(probe);
                    }
                    else if ((subcategory.compareTo("capture_version")) == 0) {
                        if (numValues == 1) {
                          captureVersion = values[0];
                          fileInfo.setCaptureVersion(captureVersion);
                        }
                    }
                    else if ((subcategory.compareTo("filter_exposure_timeX")) == 0) {
                        if (numValues == 1) {
                          filterExposureTimeX = values[0];
                          fileInfo.setFilterExposureTimeX(filterExposureTimeX);
                        }
                    }
                    else if ((subcategory.compareTo("filter_fluorophoreX")) == 0) {
                        filterFluorophoreX = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                          filterFluorophoreX[i] = values[i];
                        }
                        fileInfo.setFilterFluorophoreX(filterFluorophoreX);
                    }
                    else if ((subcategory.compareTo("mapchannel")) == 0) {
                        mapchannel = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                          mapchannel[i] = values[i];
                        }
                        fileInfo.setMapchannel(mapchannel);
                    }
                    else if ((subcategory.compareTo("plate_chamber_id")) == 0) {
                        if (numValues == 1) {
                          plateChamberID = values[0];
                          fileInfo.setPlateChamberID(plateChamberID);
                        }
                    }
                    else if ((subcategory.compareTo("plate_vessel_id")) == 0) {
                        if (numValues == 1) {
                          plateVesselID = values[0];
                          fileInfo.setPlateVesselID(plateVesselID);
                        }
                    }
                    else if ((subcategory.compareTo("specimen")) == 0) {
                        specimen = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                          specimen[i] = values[i];
                        }
                        fileInfo.setSpecimen(specimen);
                    }
                    else if ((subcategory.compareTo("specimen_species")) == 0) {
                        specimenSpecies = new String[numValues];
                        for (i = 0; i < numValues; i++) {
                          specimenSpecies[i] = values[i];
                        }
                        fileInfo.setSpecimenSpecies(specimenSpecies);
                    }

                } // else if ((category.compareTo("parameter")) == 0)
                else if ((category.compareTo("sensor")) == 0) {
                   if ((subcategory.compareTo("type")) == 0) {
                       sensorType = values[0];
                       fileInfo.setSensorType(sensorType);
                   }
                   else if ((subcategory.compareTo("model")) == 0) {
                       sensorModel = values[0];
                       fileInfo.setSensorModel(sensorModel);
                   }
                   else if ((subcategory.compareTo("s_params")) == 0) {
                       if ((values[0].compareTo("Channels")) == 0) {
                           channels = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               channels[i] = values[i];
                           }
                           fileInfo.setChannels(channels);
                       }
                       else if ((values[0].compareTo("PinholeRadius")) == 0) {
                           pinholeRadius = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               pinholeRadius[i] = values[i];
                           }
                           fileInfo.setPinholeRadius(pinholeRadius);
                       }
                       else if ((values[0].compareTo("LambdaEx")) == 0) {
                           lambdaEx = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               lambdaEx[i] = values[i];
                           }
                           fileInfo.setLambdaEx(lambdaEx);
                       }
                       else if ((values[0].compareTo("LambdaEm")) == 0) {
                           lambdaEm = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               lambdaEm[i] = values[i];
                           }
                           fileInfo.setLambdaEm(lambdaEm);
                       }
                       else if ((values[0].compareTo("ExPhotonCnt")) == 0) {
                           exPhotonCnt = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               exPhotonCnt[i] = values[i];
                           }
                           fileInfo.setExPhotonCnt(exPhotonCnt);
                       }
                       else if ((values[0].compareTo("RefrInxMedium")) == 0) {
                           refrInxMedium = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               refrInxMedium[i] = values[i];
                           }
                           fileInfo.setRefrInxMedium(refrInxMedium);
                       }
                       else if ((values[0].compareTo("NumAperture")) == 0) {
                           numAperture = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               numAperture[i] = values[i];
                           }
                           fileInfo.setNumAperture(numAperture);
                       }
                       else if ((values[0].compareTo("RefrInxLensMedium")) == 0) {
                           refrInxLensMedium = new String[numValues];
                           for (i = 0; i < numValues; i++) {
                               refrInxLensMedium[i] = values[i];
                           }
                           fileInfo.setRefrInxLensMedium(refrInxLensMedium);
                       }
                   } // else if ((subcategory.compareTo("s_params")) == 0)
                } // else if ((category.compareTo("sensor")) == 0)
                else if ((category.compareTo("history")) == 0) {
                    if (subcategory != null) {
                      history[historyNumber++] = subcategory;
                    }
                } // else if ((category.compareTo("history")) == 0)
                else if ((category.compareTo("end")) == 0) {
                    done = true;
                    if (dataFileName == null) {
                      dataFileName = headerFileName;
                      dataOffset = raFile.getFilePointer();
                    }
                }
            } // while(!done)

        } // try
        catch (OutOfMemoryError error) {
            System.gc();
            throw error;
        }

        if (historyNumber > 0) {
            actualHistory = new String[historyNumber];
            for (i = 0; i < historyNumber; i++) {
                actualHistory[i] = history[i];
            }
            fileInfo.setHistory(actualHistory);
        }

        if (parameters != order.length) {
            Preferences.debug("parameters = " + parameters + " but order.length = " +
                              order.length + "\n");
            throw new IOException("parameters = " + parameters + " but order.length = " +
                                   order.length);
        }

        if (parameters != sizes.length) {
            Preferences.debug("parameters = " + parameters + " but sizes.length = " +
                              sizes.length + "\n");
            throw new IOException("parameters = " + parameters + " but sizes.length = " +
                                   sizes.length);
        }

        if ((labels != null) && (parameters != labels.length)) {
            Preferences.debug("parameters = " + parameters + " but labels.length = " +
                              labels.length + "\n");
            throw new IOException("parameters = " + parameters + " but labels.length = " +
                                   labels.length);
        }

        if (rgbPos != -1) {
          numColors = sizes[rgbPos];
          if (numColors > 3) {
            nDims++;
          }
          if ((rgbPos > xPos) && (rgbPos > yPos) && (rgbPos > zPos) &&
              (rgbPos > tPos)) {
            colorSpacing = RGB_LAST;
          }
          else if ((rgbPos > xPos) && (rgbPos > yPos) && (rgbPos > zPos) &&
                   (rgbPos < tPos)) {
            colorSpacing = RGB_BETWEEN_SPACEANDTIME;
          }
          else if (((rgbPos < xPos) || (xPos == -1)) &&
                   ((rgbPos < yPos) || (yPos == -1)) &&
                   ((rgbPos < zPos) || (zPos == -1)) &&
                   ((rgbPos < tPos) || (tPos == -1))) {
            colorSpacing = RGB_FIRST;
          }
          else {
            Preferences.debug("Unexpected color parameter position\n");
            throw new IOException("Unexpected color parameter position");
          }
        }

        if (nDims > 4) {
          Preferences.debug("Cannot handle image with " + nDims + " dimensions\n");
          throw new IOException("Cannot handle image with " + nDims + " dimensions");
        }


        if ((numColors == 2) || (numColors == 3)) {
            // 2 or 3 dyes or spectra so use color
            if (((sizes[0] == 8) && ((format.compareTo("integer")) == 0)) &&
                ((sign == null) || ((sign.compareTo("unsigned")) == 0))) {
                dataType = ModelStorageBase.ARGB;
                Preferences.debug("Data type is ARGB\n");
            }
            else if (((sizes[0] == 16) && ((format.compareTo("integer")) == 0)) &&
                ((sign == null) || ((sign.compareTo("unsigned")) == 0))) {
                dataType = ModelStorageBase.ARGB_USHORT;
                Preferences.debug("Data type is ARGB_USHORT\n");
            }
            else if (((sizes[0] == 32) && ((format.compareTo("real")) == 0)) &&
                ((sign == null) || ((sign.compareTo("signed")) == 0))) {
                dataType = ModelStorageBase.ARGB_FLOAT;
                Preferences.debug("Data type is ARGB_FLOAT\n");
            }
            else {
              Preferences.debug("Cannot handle " + numColors + " color data with "
                                       + sizes[0] + " " +
                                       sign + " bits and "
                                       + format + " format\n");
                    throw new IOException("Cannot handle " + numColors +
                                          " color data with " + sizes[0] + " " +
                                       sign + " bits and "
                                       + format + " format");

            }

        } // if color
        else { // not color or more than 3 spectral color
            if (((sizes[0] == 8) && ((format.compareTo("integer")) == 0)) &&
                ((sign == null) || ((sign.compareTo("unsigned")) == 0))) {
                dataType = ModelStorageBase.UBYTE;
                Preferences.debug("Data type is UBYTE\n");
            }
            else if ((sizes[0] == 8) && ((format.compareTo("integer")) == 0) &&
                     ((sign.compareTo("signed")) == 0)) {
                dataType = ModelStorageBase.BYTE;
                Preferences.debug("Data type is BYTE\n");
            }
            else if (((sizes[0] == 16) && ((format.compareTo("integer")) == 0)) &&
                ((sign == null) || ((sign.compareTo("unsigned")) == 0))) {
                dataType = ModelStorageBase.USHORT;
                Preferences.debug("Data type is USHORT\n");
            }
            else if ((sizes[0] == 16) && ((format.compareTo("integer")) == 0) &&
                     ((sign.compareTo("signed")) == 0)) {
                dataType = ModelStorageBase.SHORT;
                Preferences.debug("Data type is SHORT\n");
            }
            else if (((sizes[0] == 32) && ((format.compareTo("integer")) == 0)) &&
                ((sign == null) || ((sign.compareTo("unsigned")) == 0))) {
                dataType = ModelStorageBase.UINTEGER;
                Preferences.debug("Data type is UINTEGER\n");
            }
            else if ((sizes[0] == 32) && ((format.compareTo("integer")) == 0) &&
                     ((sign.compareTo("signed")) == 0)) {
                dataType = ModelStorageBase.INTEGER;
                Preferences.debug("Data type is INTEGER\n");
            }
            else if ((sizes[0] == 64) && ((format.compareTo("integer")) == 0) &&
                     ((sign.compareTo("signed")) == 0)) {
                dataType = ModelStorageBase.LONG;
                Preferences.debug("Data type is LONG\n");
            }
            else if (((sizes[0] == 32) && ((format.compareTo("real")) == 0)) &&
                    ((sign == null) || ((sign.compareTo("signed")) == 0))) {
                dataType = ModelStorageBase.FLOAT;
                Preferences.debug("Data type is FLOAT\n");
            }
            else if (((sizes[0] == 64) && ((format.compareTo("real")) == 0)) &&
                    ((sign == null) || ((sign.compareTo("signed")) == 0))) {
                dataType = ModelStorageBase.DOUBLE;
                Preferences.debug("Data type is DOUBLE\n");
            }
            else if (((sizes[0] == 64) && ((format.compareTo("complex")) == 0)) &&
                    ((sign == null) || ((sign.compareTo("signed")) == 0))) {
                dataType = ModelStorageBase.COMPLEX;
                Preferences.debug("Data type is COMPLEX\n");
            }
            else if (((sizes[0] == 128) && ((format.compareTo("complex")) == 0)) &&
                    ((sign == null) || ((sign.compareTo("signed")) == 0))) {
                dataType = ModelStorageBase.DCOMPLEX;
                Preferences.debug("Data type is DCOMPLEX\n");
            }
            else {
                Preferences.debug("Cannot handle data with " + sizes[0] + " " +
                                       sign + " bits and "
                                       + format + " format\n");
                    throw new IOException("Cannot handle data with " + sizes[0] + " " +
                                       sign + " bits and "
                                       + format + " format");
            }
        } // else not color

            fileInfo.setDataType(dataType);

            imgExtents = new int[nDims];
            i = 0;
            if (xPos >= 0) {
              imgExtents[i++] = sizes[xPos];
            }
            if (yPos >= 0) {
              imgExtents[i++] = sizes[yPos];
            }
            if (zPos >= 0) {
              imgExtents[i++] = sizes[zPos];
            }
            if (tPos >= 0) {
              imgExtents[i++] = sizes[tPos];
            }
            if (numColors > 3) {
              imgExtents[i] = sizes[rgbPos];
            }

            fileInfo.setExtents(imgExtents);
            if (origin != null) {
                i = 0;
                startLocations = new float[nDims];
                if (origin.length == parameters) {
                  if (xPos >= 0) {
                    startLocations[i++] = origin[xPos];
                  }
                  if (yPos >= 0) {
                    startLocations[i++] = origin[yPos];
                  }
                  if (zPos >= 0) {
                    startLocations[i++] = origin[zPos];
                  }
                  if (tPos >= 0) {
                    startLocations[i++] = origin[tPos];
                  }
                  if (numColors > 3) {
                    startLocations[i] = 0.0f;
                  }
                }
                if ((origin.length == (parameters-1)) && (rgbPos >= 0) &&
                    (rgbPos < xPos)) {
                  // No origin for rgb parameter
                  if (xPos >= 0) {
                    startLocations[i++] = origin[xPos-1];
                  }
                  if (yPos >= 0) {
                    startLocations[i++] = origin[yPos-1];
                  }
                  if (zPos >= 0) {
                    startLocations[i++] = origin[zPos-1];
                  }
                  if (tPos >= 0) {
                    startLocations[i++] = origin[tPos-1];
                  }
                  if (numColors > 3) {
                    startLocations[i] = 0.0f;
                  }
                }

                fileInfo.setOrigin(startLocations);
            }

            if (scale != null) {
              i = 0;
              imgResols = new float[nDims];
              if (scale.length == parameters) {
                if (xPos >= 0) {
                  imgResols[i++] = scale[xPos];
                }
                if (yPos >= 0) {
                  imgResols[i++] = scale[yPos];
                }
                if (zPos >= 0) {
                  imgResols[i++] = scale[zPos];
                }
                if (tPos >= 0) {
                  imgResols[i] = scale[tPos];
                }
              }
              if ((scale.length == (parameters-1)) && (rgbPos >= 0) &&
                  (rgbPos < xPos)) {
                // No resolution  for rgb parameter
                if (xPos >= 0) {
                  imgResols[i++] = scale[xPos-1];
                }
                if (yPos >= 0) {
                  imgResols[i++] = scale[yPos-1];
                }
                if (zPos >= 0) {
                  imgResols[i++] = scale[zPos-1];
                }
                if (tPos >= 0) {
                  imgResols[i] = scale[tPos-1];
                }
              }
              fileInfo.setResolutions(imgResols);
            } // if (scale != null)


            if (labels != null) {
                if (exchangeXY) {
                    sTemp = labels[xPos];
                    labels[xPos] = labels[yPos];
                    labels[yPos] = sTemp;
                }
                fileInfo.setLabels(labels);
            }
            if (units != null) {
              i = 0;
              unitsStr = new String[nDims];
              unitsOfMeasure = new int[nDims];
              if (units.length == parameters) {
                if (xPos >= 0) {
                  unitsStr[i++] = units[xPos];
                }
                if (yPos >= 0) {
                  unitsStr[i++] = units[yPos];
                }
                if (zPos >= 0) {
                  unitsStr[i++] = units[zPos];
                }
                if (tPos >= 0) {
                  unitsStr[i++] = units[tPos];
                }
                if (numColors > 3) {
                  unitsStr[i] = units[rgbPos];
                }
              }
              if (((units.length == nDims) && (numColors <= 3)) ||
                  ((units.length == (nDims-1)) && (numColors > 3))) {
                for (i = 0; i < units.length; i++) {
                  unitsStr[i] = units[i];
                }
                if (units.length == (nDims-1)) {
                  unitsStr[nDims-1] = "undefined";
                }
              }


                for (i = 0; i < nDims; i++) {
                    if (unitsStr[i].equalsIgnoreCase("cm")) {
                        unitsOfMeasure[i] = FileInfoBase.CENTIMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("centimeters")) {
                        unitsOfMeasure[i] = FileInfoBase.CENTIMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("mm")) {
                        unitsOfMeasure[i] = FileInfoBase.MILLIMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("millimeters")) {
                        unitsOfMeasure[i] = FileInfoBase.MILLIMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("um")) {
                        unitsOfMeasure[i] = FileInfoBase.MICROMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("mic")) {
                        unitsOfMeasure[i] = FileInfoBase.MICROMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("micrometers")) {
                        unitsOfMeasure[i] = FileInfoBase.MICROMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("nm")) {
                        unitsOfMeasure[i] = FileInfoBase.NANOMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("nanometers")) {
                        unitsOfMeasure[i] = FileInfoBase.NANOMETERS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("A")) {
                        unitsOfMeasure[i] = FileInfoBase.ANGSTROMS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("angstroms")) {
                        unitsOfMeasure[i] = FileInfoBase.ANGSTROMS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("sec")) {
                        unitsOfMeasure[i] = FileInfoBase.SECONDS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("seconds")) {
                        unitsOfMeasure[i] = FileInfoBase.SECONDS;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("msec")) {
                        unitsOfMeasure[i] = FileInfoBase.MILLISEC;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("milliseconds")) {
                        unitsOfMeasure[i] = FileInfoBase.MILLISEC;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("usec")) {
                        unitsOfMeasure[i] = FileInfoBase.MICROSEC;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("microseconds")) {
                        unitsOfMeasure[i] = FileInfoBase.MICROSEC;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("nsec")) {
                        unitsOfMeasure[i] = FileInfoBase.NANOSEC;
                    }
                    else if (unitsStr[i].equalsIgnoreCase("nanoseconds")) {
                        unitsOfMeasure[i] = FileInfoBase.NANOSEC;
                    }
                    else {
                      unitsOfMeasure[i] = FileInfoBase.UNKNOWN_MEASURE;
                    }
                } // for (i = 0; i < nDims; i++)

                fileInfo.setUnitsOfMeasure(unitsOfMeasure);
            } // if (units != null)


        raFile.close();
        return;
    }


    /**
    *   readImage
    *   @return              returns the image
    *   @exception IOException if there is an error reading the file
    */
    public ModelImage readImage() throws IOException {
        int i;
        int s;
        int x,y;
        FileInputStream fis;

        progressBar = new ViewJProgressBar(ViewUserInterface.getReference().getProgressBarPrefix() + fileName,
                                           ViewUserInterface.getReference().getProgressBarPrefix() + "ICS image(s) ...", 0, 100,
                                            false, null, null);
        progressBar.setLocation( (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2,
                                 50);
        readHeader();
        s = fileName.lastIndexOf(".");
        if (dataFileName == null) {
            dataFileName = fileName.substring(0,s+1) + "ids";
        }
        file = new File(fileDir + dataFileName);
        if (useGZIP) {
            int totalBytesRead = 0;
            progressBar.setVisible(isProgressBarVisible());
            progressBar.setMessage("Uncompressing GZIP file ...");
            fis = new FileInputStream(file);
            fis.skip(dataOffset);
            GZIPInputStream gzin = new GZIPInputStream(new BufferedInputStream(fis));
            String uncompressedName = fileDir + fileName.substring(0,s) +
                                      "uncompressed.ids";
            FileOutputStream out = new FileOutputStream(uncompressedName);
            byte[] buffer = new byte[256];
            while (true) {
                int bytesRead = gzin.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                totalBytesRead += bytesRead;
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            file = new File(uncompressedName);
            progressBar.setMessage("Loading ICS image(s)...");
            dataOffset = 0L;
        } // if (useGZIP)
        raFile = new RandomAccessFile(file, "r");
        raFile.seek(dataOffset);

        image = new ModelImage(dataType, imgExtents, fileInfo.getFileName(), UI);

        if (imgExtents.length == 2) {
            numberSlices = 1;
        }
        else if (imgExtents.length == 3) {
            numberSlices = imgExtents[2];
        }
        else {
            numberSlices = imgExtents[2] * imgExtents[3];
        }

        if ((xPos >= 0) && (yPos >= 0) && (zPos >= 0)) {
          numberSpaceSlices = imgExtents[2]; // zDim;
        }

        if ((dataType != ModelStorageBase.DOUBLE) &&
            (dataType != ModelStorageBase.COMPLEX) &&
            (dataType != ModelStorageBase.DCOMPLEX) &&
            (dataType != ModelStorageBase.UINTEGER) &&
            (dataType != ModelStorageBase.LONG)) {
            if ((dataType == ModelStorageBase.ARGB) ||
                (dataType == ModelStorageBase.ARGB_USHORT) ||
                (dataType == ModelStorageBase.ARGB_FLOAT)) {
                bufferSize = 4*imgExtents[0]*imgExtents[1];
            }
            else {
                bufferSize = imgExtents[0]*imgExtents[1];
            }
            imgBuffer = new float[bufferSize];
            if (invertY || exchangeXY) {
                imgBuffer2 = new float[bufferSize];
            }
            for (i = 0; i < numberSlices; i++) {
                image.setFileInfo(fileInfo, i);
                readBuffer(i, imgBuffer);
                if (exchangeXY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgBuffer2[x + y*imgExtents[0]] =
                            imgBuffer[y + x*imgExtents[1]];
                        }
                    }
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgBuffer[x + y*imgExtents[0]] =
                            imgBuffer2[x + y*imgExtents[0]];
                        }
                    }
                } // if (exchangeXY)
                if (invertY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgBuffer2[x + y*imgExtents[0]] =
                            imgBuffer[x + (imgExtents[1] - 1 - y)*imgExtents[0]];
                        }
                    }
                    image.importData(i*bufferSize, imgBuffer2, false);
                } // if (invertY)
                else {
                    image.importData(i*bufferSize, imgBuffer, false);
                }
            }
        } // if ((dataType != ModelStorageBase.DOUBLE) &&
          //  (dataType != ModelStorageBase.COMPLEX) &&
          //  (dataType != ModelStorageBase.DCOMPLEX) &&
          //  (dataType != ModelStorageBase.UINTEGER) &&
          //  (dataType != ModelStorageBase.LONG))
        else if ((dataType == ModelStorageBase.LONG) ||
                 (dataType == ModelStorageBase.UINTEGER)) {
            bufferSize = imgExtents[0]*imgExtents[1];
            imgLBuffer = new long[bufferSize];
            if (invertY || exchangeXY) {
                imgLBuffer2 = new long[bufferSize];
            }
            for (i = 0; i < numberSlices; i++) {
                image.setFileInfo(fileInfo, i);
                readLBuffer(i,imgLBuffer);
                if (exchangeXY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgLBuffer2[x + y*imgExtents[0]] =
                            imgLBuffer[y + x*imgExtents[1]];
                        }
                    }
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgLBuffer[x + y*imgExtents[0]] =
                            imgLBuffer2[x + y*imgExtents[0]];
                        }
                    }
                } // if (exchangeXY)
                if (invertY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgLBuffer2[x + y*imgExtents[0]] =
                            imgLBuffer[x + (imgExtents[1] - 1 - y)*imgExtents[0]];
                        }
                    }
                    image.importData(i*bufferSize, imgLBuffer2, false);
                } // if (invertY)
                else {
                    image.importData(i*bufferSize, imgLBuffer, false);
                }
            }
        } // else if ((dataType == ModelStorageBase.LONG)||
          //          (dataType == ModelStorageBase.UINTEGER))
        else if (dataType == ModelStorageBase.COMPLEX) {
            bufferSize = imgExtents[0]*imgExtents[1];
            imgBuffer = new float[bufferSize];
            imgBufferI = new float[bufferSize];
            if (invertY || exchangeXY) {
                imgBuffer2 = new float[bufferSize];
                imgBufferI2 = new float[bufferSize];
            }
            for (i = 0; i < numberSlices; i++) {
                image.setFileInfo(fileInfo, i);
                readComplexBuffer(i, imgBuffer, imgBufferI);
                if (exchangeXY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgBuffer2[x + y*imgExtents[0]] =
                            imgBuffer[y + x*imgExtents[1]];
                            imgBufferI2[x + y*imgExtents[0]] =
                            imgBufferI[y + x*imgExtents[1]];
                        }
                    }
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgBuffer[x + y*imgExtents[0]] =
                            imgBuffer2[x + y*imgExtents[0]];
                            imgBufferI[x + y*imgExtents[0]] =
                            imgBufferI2[x + y*imgExtents[0]];
                        }
                    }
                } // if (exchangeXY)
                if (invertY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgBuffer2[x + y*imgExtents[0]] =
                            imgBuffer[x + (imgExtents[1] - 1 - y)*imgExtents[0]];
                            imgBufferI2[x + y*imgExtents[0]] =
                            imgBufferI[x + (imgExtents[1] - 1 - y)*imgExtents[0]];
                        }
                    }
                    image.importComplexData(2*i*bufferSize, imgBuffer2, imgBufferI2,
                                            false, true);
                } // if (invertY)
                else {
                    image.importComplexData(2*i*bufferSize, imgBuffer, imgBufferI,
                                            false, true);
                }
            }
        } // else if (dataType == ModelStorageBase.COMPLEX)
        else if (dataType == ModelStorageBase.DCOMPLEX) {
            bufferSize = imgExtents[0]*imgExtents[1];
            imgDBuffer = new double[bufferSize];
            imgDBufferI = new double[bufferSize];
            if (invertY || exchangeXY) {
                imgDBuffer2 = new double[bufferSize];
                imgDBufferI2 = new double[bufferSize];
            }
            for (i = 0; i < numberSlices; i++) {
                image.setFileInfo(fileInfo, i);
                readDComplexBuffer(i, imgDBuffer, imgDBufferI);
                if (exchangeXY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgDBuffer2[x + y*imgExtents[0]] =
                            imgDBuffer[y + x*imgExtents[1]];
                            imgDBufferI2[x + y*imgExtents[0]] =
                            imgDBufferI[y + x*imgExtents[1]];
                        }
                    }
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgDBuffer[x + y*imgExtents[0]] =
                            imgDBuffer2[x + y*imgExtents[0]];
                            imgDBufferI[x + y*imgExtents[0]] =
                            imgDBufferI2[x + y*imgExtents[0]];
                        }
                    }
                } // if (exchangeXY)
                if (invertY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgDBuffer2[x + y*imgExtents[0]] =
                            imgDBuffer[x + (imgExtents[1] - 1 - y)*imgExtents[0]];
                            imgDBufferI2[x + y*imgExtents[0]] =
                            imgDBufferI[x + (imgExtents[1] - 1 - y)*imgExtents[0]];
                        }
                    }
                    image.importDComplexData(2*i*bufferSize, imgDBuffer2, imgDBufferI2,
                                            false, true);
                } // if (invertY)
                else {
                    image.importDComplexData(2*i*bufferSize, imgDBuffer, imgDBufferI,
                                            false, true);
                }
            }
        } // else if (dataType == ModelStorageBase.DCOMPLEX)
        else { // dataType == ModelStorageBase.DOUBLE
            bufferSize = imgExtents[0]*imgExtents[1];
            imgDBuffer = new double[bufferSize];
            if (invertY || exchangeXY) {
                imgDBuffer2 = new double[bufferSize];
            }
            for (i = 0; i < numberSlices; i++) {
                image.setFileInfo(fileInfo, i);
                readDBuffer(i, imgDBuffer);
                if (exchangeXY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgDBuffer2[x + y*imgExtents[0]] =
                            imgDBuffer[y + x*imgExtents[1]];
                        }
                    }
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgDBuffer[x + y*imgExtents[0]] =
                            imgDBuffer2[x + y*imgExtents[0]];
                        }
                    }
                } // if (exchangeXY)
                if (invertY) {
                    for (x = 0; x < imgExtents[0]; x++) {
                        for (y = 0; y < imgExtents[1]; y++) {
                            imgDBuffer2[x + y*imgExtents[0]] =
                            imgDBuffer[x + (imgExtents[1] - 1 - y)*imgExtents[0]];
                        }
                    }
                    image.importData(i*bufferSize, imgDBuffer2, false);
                } // if (invertY)
                else {
                    image.importData(i*bufferSize, imgDBuffer, false);
                }
            }
        } // else dataType == ModelStorageBase.DOUBLE

        raFile.close();
        progressBar.dispose();

        return image;
    }

    /**
    *   Reads a slice of data at a time and stores the results in the buffer
    *   @param slice            offset into the file stored in the dataOffset array
    *   @param buffer           buffer where the info is stored
    *   @exception IOException  if there is an error reading the file
    */
    private void readBuffer(int slice, float buffer[]) throws IOException {
        int i = 0;
        int j;
        int nBytes;
        int nShorts;
        int nFloats;
        int b1, b2, b3, b4, b5, b6, b7, b8, b9, b10 , b11, b12;
        byte [] byteBuffer;
        int progress, progressLength, mod;
        int tmpInt, tmpInt2, tmpInt3;
        long savedPosition;
        switch (dataType) {
            case ModelStorageBase.BYTE:
              nBytes = buffer.length;
              byteBuffer = new byte[nBytes];
              raFile.read(byteBuffer, 0, nBytes);
              progress = slice * buffer.length;
              progressLength = buffer.length * numberSlices;
              mod = progressLength / 100;
              progressBar.setVisible(isProgressBarVisible());
              for (j = 0; j < nBytes; j++, i++) {
                if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                    round( (float) (i + progress) /
                          progressLength * 100), false);
                buffer[i] = byteBuffer[j];
              }
              break;
            case ModelStorageBase.UBYTE:
                nBytes = buffer.length;
                if (colorSpacing == RGB_LAST) {
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j++, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    buffer[i] = byteBuffer[j] & 0xff;
                  }
                } // if (colorSpacing == RGB_LAST)
                else if (colorSpacing == RGB_FIRST) {
                  if (nDims == 3) {
                    raFile.seek(dataOffset + slice);
                  }
                  else if ((nDims == 4) && ((slice%imgExtents[2]) == 0)) {
                    raFile.seek(dataOffset + slice/imgExtents[2]);
                    endBytes = numColors - 1 - slice/imgExtents[2];
                  }
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (i = 0; i < nBytes-1; i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) / progressLength * 100), false);
                    buffer[i] = raFile.readUnsignedByte();
                    for (j = 0; j < numColors - 1; j++) {
                      raFile.readByte();
                    }
                  } // for (i = 0; i < nBytes-1; i++)
                  if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) / progressLength * 100), false);
                  buffer[i] = raFile.readUnsignedByte();
                  if (nDims == 4) {
                    for (j = 0; j < endBytes; j++){
                      raFile.readByte();
                    }
                  }
                } // else if (colorSpacing == RGB_FIRST)
                else { // colorSpacing == RGB_BETWEEN_SPACEANDTIME
                  // have x, y, color, time
                  // imgExtents[2] = tDim
                  // imgExtents[3] = numColors
                  if ((nDims == 4) && ((slice%imgExtents[2]) == 0)) {
                    raFile.seek(dataOffset + bufferSize*slice/imgExtents[2]);
                  }
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j++, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    buffer[i] = byteBuffer[j] & 0xff;
                  }
                  savedPosition = raFile.getFilePointer();
                  raFile.seek( (numColors - 1) * nBytes + savedPosition);
                } // else colorSpacing == RGB_BETWEEN_SPACETIME
                break;
            case ModelStorageBase.SHORT:
                nBytes = 2 * buffer.length;
                byteBuffer =  new byte[nBytes];
                raFile.read(byteBuffer, 0, nBytes);
                progress = slice*buffer.length;
                progressLength = buffer.length*numberSlices;
                mod = progressLength/10;
                progressBar.setVisible(isProgressBarVisible());
                for (j = 0; j < nBytes; j+=2, i++ ) {
                    if ((i+progress)%mod==0) progressBar.updateValue( Math.round((float)(i+progress)/
                                                                progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j+1);

                    if (endianess) {
                        buffer[i] = (short)((b1 << 8) + b2);
                    }
                    else {
                        buffer[i] = (short)((b2 << 8) + b1);
                    }

                } // for (j = 0; j < nBytes; j+=2, i++ )
                break;
            case ModelStorageBase.USHORT:
                nBytes = 2 * buffer.length;
                if (colorSpacing == RGB_LAST) {
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 10;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 2, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);

                    if (endianess) {
                      buffer[i] = ( (b1 << 8) + b2);
                    }
                    else {
                      buffer[i] = ( (b2 << 8) + b1);
                    }

                  } // for (j = 0; j < nBytes; j+=2, i++ )
                } // if (colorSpacing == RGB_LAST)
                else if (colorSpacing == RGB_FIRST) {
                  nShorts = nBytes/2;
                  if (nDims == 3) {
                    raFile.seek(dataOffset + 2*slice);
                  }
                  else if ((nDims == 4) && ((slice%imgExtents[2]) == 0)) {
                    raFile.seek(dataOffset + 2*slice/imgExtents[2]);
                    endShorts = numColors - 1 - slice/imgExtents[2];
                  }
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 10;
                  progressBar.setVisible(isProgressBarVisible());
                  for (i = 0; i < nShorts-1; i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) / progressLength * 100), false);
                    buffer[i] = getUnsignedShort(endianess);
                    for (j = 0; j < numColors - 1; j++) {
                      raFile.readShort();
                    }
                  } // for (i = 0; i < nShorts-1; i++)
                  if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) / progressLength * 100), false);
                  buffer[i] = getUnsignedShort(endianess);
                  if (nDims == 4) {
                    for (j = 0; j < endShorts; j++){
                      raFile.readShort();
                    }
                  }
                } // else if (colorSpacing == RGB_FIRST)
                else { // colorSpacing == RGB_BETWEEN_SPACEANDTIME
                  // have x, y, color, time
                  // imgExtents[2] = tDim
                  // imgExtents[3] = numColors
                  if ((nDims == 4) && ((slice%imgExtents[2]) == 0)) {
                    raFile.seek(dataOffset + 2*bufferSize*slice/imgExtents[2]);
                  }
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 10;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 2, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);

                    if (endianess) {
                      buffer[i] = ( (b1 << 8) + b2);
                    }
                    else {
                      buffer[i] = ( (b2 << 8) + b1);
                    }

                  } // for (j = 0; j < nBytes; j+=2, i++ )
                  savedPosition = raFile.getFilePointer();
                  raFile.seek( (numColors - 1) * nBytes + savedPosition);
                } // else colorSpacing == RGB_BETWEEN_SPACETIME
                break;
            case ModelStorageBase.INTEGER:
                nBytes = 4 * buffer.length;
                byteBuffer =  new byte[nBytes];
                raFile.read(byteBuffer, 0, nBytes);
                progress = slice*buffer.length;
                progressLength = buffer.length*numberSlices;
                mod = progressLength/10;
                progressBar.setVisible(isProgressBarVisible());
                for (j =0; j < nBytes; j+=4, i++ ) {
                    if ((i+progress)%mod==0) progressBar.updateValue( Math.round((float)(i+progress)/
                                                                progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j+1);
                    b3 = getUnsignedByte(byteBuffer, j+2);
                    b4 = getUnsignedByte(byteBuffer, j+3);

                    if (endianess) {
                        buffer[i]=((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);  // Big Endian
                    }
                    else {
                        buffer[i]=((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);  // Little Endian
                    }
                } // for (j =0; j < nBytes; j+=4, i++ )
                break;
            case ModelStorageBase.FLOAT:
                nBytes = 4 * buffer.length;
                if (colorSpacing == RGB_LAST) {
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 10;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 4, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) / progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);
                    b3 = getUnsignedByte(byteBuffer, j + 2);
                    b4 = getUnsignedByte(byteBuffer, j + 3);

                    if (endianess) {
                      tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                    }
                    else {
                      tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                    }
                    buffer[i] = Float.intBitsToFloat(tmpInt);
                  } // for (j =0; j < nBytes; j+=4, i++ )
                } // if (colorSpacing == RGB_LAST)
                else if (colorSpacing == RGB_FIRST) {
                  nFloats = nBytes/4;
                  if (nDims == 3) {
                    raFile.seek(dataOffset + 4*slice);
                  }
                  else if ((nDims == 4) && ((slice%imgExtents[2]) == 0)) {
                    raFile.seek(dataOffset + 4*slice/imgExtents[2]);
                    endFloats = numColors - 1 - slice/imgExtents[2];
                  }
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 10;
                  progressBar.setVisible(isProgressBarVisible());
                  for (i = 0; i < nFloats-1; i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) / progressLength * 100), false);
                    buffer[i] = getFloat(endianess);
                    for (j = 0; j < numColors - 1; j++) {
                      raFile.readFloat();
                    }
                  } // for (i = 0; i < nFloats-1; i++)
                  if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) / progressLength * 100), false);
                  buffer[i] = getFloat(endianess);
                  if (nDims == 4) {
                    for (j = 0; j < endFloats; j++){
                      raFile.readFloat();
                    }
                  }
                } // else if (colorSpacing == RGB_FIRST)
                else { // colorSpacing == RGB_BETWEEN_SPACEANDTIME
                  // have x, y, color, time
                  // imgExtents[2] = tDim
                  // imgExtents[3] = numColors
                  if ((nDims == 4) && ((slice%imgExtents[2]) == 0)) {
                    raFile.seek(dataOffset + 4*bufferSize*slice/imgExtents[2]);
                  }
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * buffer.length;
                  progressLength = buffer.length * numberSlices;
                  mod = progressLength / 10;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 4, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);
                    b3 = getUnsignedByte(byteBuffer, j + 2);
                    b4 = getUnsignedByte(byteBuffer, j + 3);

                    if (endianess) {
                      tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                    }
                    else {
                      tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                    }
                    buffer[i] = Float.intBitsToFloat(tmpInt);
                  } // for (j =0; j < nBytes; j+=4, i++ )
                  savedPosition = raFile.getFilePointer();
                  raFile.seek( (numColors - 1) * nBytes + savedPosition);
                } // else colorSpacing == RGB_BETWEEN_SPACEANDTIME
                break;
            case ModelStorageBase.ARGB:
              if (colorSpacing == RGB_LAST) {
                nBytes = buffer.length / 4;
                byteBuffer = new byte[nBytes];
                raFile.read(byteBuffer, 0, nBytes);
                progress = numColors * slice * nBytes;
                progressLength = numColors * nBytes * numberSlices;
                mod = progressLength / 100;
                progressBar.setVisible(isProgressBarVisible());
                for (j = 0; j < nBytes; j++, i++) {
                  if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                      round( (float) (i + progress) /
                            progressLength * 100), false);
                  buffer[4 * i + 1] = byteBuffer[j] & 0xff;
                }
                savedPosition = raFile.getFilePointer();
                raFile.seek( (numberSlices - 1) * nBytes + savedPosition);
                raFile.read(byteBuffer, 0, nBytes);
                progress = numColors * slice * nBytes + nBytes;
                for (i = 0, j = 0; j < nBytes; j++, i++) {
                  if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                      round( (float) (i + progress) /
                            progressLength * 100), false);
                  buffer[4 * i + 2] = byteBuffer[j] & 0xff;
                }
                if (numColors == 3) {
                  raFile.seek( (2 * numberSlices - 1) * nBytes + savedPosition);
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = 3 * slice * nBytes + 2 * nBytes;
                  for (i = 0, j = 0; j < nBytes; j++, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    buffer[4 * i + 3] = byteBuffer[j] & 0xff;
                  }
                } // if (numColors == 3)
                raFile.seek(savedPosition);
              } // if (colorSpacing == RGB_LAST)
              else if (colorSpacing == RGB_FIRST) {
                if (numColors == 2) {
                  nBytes = 2 * buffer.length / 4;
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * nBytes;
                  progressLength = nBytes * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j+=2, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    buffer[4 * i + 1] = byteBuffer[j] & 0xff;
                    buffer[4 * i + 2] = byteBuffer[j+1] & 0xff;
                    buffer[4 * i + 3] = 0;
                  }
                } // if (numColors == 2)
                else { // numColors == 3
                  nBytes = 3 * buffer.length / 4;
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = slice * nBytes;
                  progressLength = nBytes * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j+=3, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    buffer[4 * i + 1] = byteBuffer[j] & 0xff;
                    buffer[4 * i + 2] = byteBuffer[j+1] & 0xff;
                    buffer[4 * i + 3] = byteBuffer[j+2] & 0xff;
                  }
                } // else numColors == 3
              } // else if (colorSpacing == RGB_FIRST)
              else { // colorSpacing == RGB_BETWEEN_SPACEANDTIME
                nBytes = buffer.length / 4;
                byteBuffer = new byte[nBytes];
                raFile.read(byteBuffer, 0, nBytes);
                progress = numColors * slice * nBytes;
                progressLength = numColors * nBytes * numberSlices;
                mod = progressLength / 100;
                progressBar.setVisible(isProgressBarVisible());
                for (j = 0; j < nBytes; j++, i++) {
                  if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                      round( (float) (i + progress) /
                            progressLength * 100), false);
                  buffer[4 * i + 1] = byteBuffer[j] & 0xff;
                }
                savedPosition = raFile.getFilePointer();
                raFile.seek( (numberSpaceSlices - 1) * nBytes + savedPosition);
                raFile.read(byteBuffer, 0, nBytes);
                progress = numColors * slice * nBytes + nBytes;
                for (i = 0, j = 0; j < nBytes; j++, i++) {
                  if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                      round( (float) (i + progress) /
                            progressLength * 100), false);
                  buffer[4 * i + 2] = byteBuffer[j] & 0xff;
                }
                if (numColors == 3) {
                  raFile.seek( (2 * numberSpaceSlices - 1) * nBytes + savedPosition);
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = 3 * slice * nBytes + 2 * nBytes;
                  for (i = 0, j = 0; j < nBytes; j++, i++) {
                    if ( (i + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (i + progress) /
                              progressLength * 100), false);
                    buffer[4 * i + 3] = byteBuffer[j] & 0xff;
                  }
                } // if (numColors == 3)
                if (((slice+1)%numberSpaceSlices) == 0) {

                }
                else {
                  raFile.seek(savedPosition);
                }
              } // else colorSpacing == RGB_BETWEEN_SPACEANDTIME
              break;
            case ModelStorageBase.ARGB_USHORT:
                if (colorSpacing == RGB_LAST) {
                  nBytes = buffer.length / 2;
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes;
                  progressLength = numColors * nBytes * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 2, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);

                    if (endianess) {
                      buffer[4 * i + 1] = ( (b1 << 8) + b2);
                    }
                    else {
                      buffer[4 * i + 1] = ( (b2 << 8) + b1);
                    }
                  }
                  savedPosition = raFile.getFilePointer();
                  raFile.seek( (numberSlices - 1) * nBytes + savedPosition);
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes + nBytes;
                  for (i = 0, j = 0; j < nBytes; j += 2, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);

                    if (endianess) {
                      buffer[4 * i + 2] = ( (b1 << 8) + b2);
                    }
                    else {
                      buffer[4 * i + 2] = ( (b2 << 8) + b1);
                    }
                  }
                  if (numColors == 3) {
                    raFile.seek( (2 * numberSlices - 1) * nBytes + savedPosition);
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = 3 * slice * nBytes + 2 * nBytes;
                    for (i = 0, j = 0; j < nBytes; j += 2, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);

                      if (endianess) {
                        buffer[4 * i + 3] = ( (b1 << 8) + b2);
                      }
                      else {
                        buffer[4 * i + 3] = ( (b2 << 8) + b1);
                      }
                    }
                  } // if (numColors == 3)
                  raFile.seek(savedPosition);
                } // if (colorSpacing == RGB_LAST)
                else if (colorSpacing == RGB_FIRST) {
                  if (numColors == 2) {
                    nBytes = 2 * buffer.length / 2;
                    byteBuffer = new byte[nBytes];
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = slice * nBytes;
                    progressLength = nBytes * numberSlices;
                    mod = progressLength / 100;
                    progressBar.setVisible(isProgressBarVisible());
                    for (j = 0; j < nBytes; j += 4, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);
                      b3 = getUnsignedByte(byteBuffer, j + 2);
                      b4 = getUnsignedByte(byteBuffer, j + 3);

                      if (endianess) {
                        buffer[4 * i + 1] = ( (b1 << 8) + b2);
                        buffer[4 * i + 2] = ( (b3 << 8) + b4);
                      }
                      else {
                        buffer[4 * i + 1] = ( (b2 << 8) + b1);
                        buffer[4 * i + 2] = ( (b4 << 8) + b3);
                      }
                    }
                  } // if (numColors == 2)
                  else { // numColors == 3
                    nBytes = 3 * buffer.length / 2;
                    byteBuffer = new byte[nBytes];
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = slice * nBytes;
                    progressLength = nBytes * numberSlices;
                    mod = progressLength / 100;
                    progressBar.setVisible(isProgressBarVisible());
                    for (j = 0; j < nBytes; j += 6, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);
                      b3 = getUnsignedByte(byteBuffer, j + 2);
                      b4 = getUnsignedByte(byteBuffer, j + 3);
                      b5 = getUnsignedByte(byteBuffer, j + 4);
                      b6 = getUnsignedByte(byteBuffer, j + 5);

                      if (endianess) {
                        buffer[4 * i + 1] = ( (b1 << 8) + b2);
                        buffer[4 * i + 2] = ( (b3 << 8) + b4);
                        buffer[4 * i + 3] = ( (b5 << 8) + b6);
                      }
                      else {
                        buffer[4 * i + 1] = ( (b2 << 8) + b1);
                        buffer[4 * i + 2] = ( (b4 << 8) + b3);
                        buffer[4 * i + 3] = ( (b6 << 8) + b5);
                      }
                    }
                  } // else numColors == 3
                } // else if (colorSpacing == RGB_FIRST)
                else { // colorSpacing == RGB_BETWEEN_SPACEANDTIME
                  nBytes = buffer.length / 2;
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes;
                  progressLength = numColors * nBytes * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 2, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);

                    if (endianess) {
                      buffer[4 * i + 1] = ( (b1 << 8) + b2);
                    }
                    else {
                      buffer[4 * i + 1] = ( (b2 << 8) + b1);
                    }
                  }
                  savedPosition = raFile.getFilePointer();
                  raFile.seek( (numberSpaceSlices - 1) * nBytes + savedPosition);
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes + nBytes;
                  for (i = 0, j = 0; j < nBytes; j += 2, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);

                    if (endianess) {
                      buffer[4 * i + 2] = ( (b1 << 8) + b2);
                    }
                    else {
                      buffer[4 * i + 2] = ( (b2 << 8) + b1);
                    }
                  }
                  if (numColors == 3) {
                    raFile.seek( (2 * numberSpaceSlices - 1) * nBytes + savedPosition);
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = 3 * slice * nBytes + 2 * nBytes;
                    for (i = 0, j = 0; j < nBytes; j += 2, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);

                      if (endianess) {
                        buffer[4 * i + 3] = ( (b1 << 8) + b2);
                      }
                      else {
                        buffer[4 * i + 3] = ( (b2 << 8) + b1);
                      }
                    }
                  } // if (numColors == 3)
                  if (((slice+1)%numberSpaceSlices) == 0) {

                  }
                  else {
                    raFile.seek(savedPosition);
                  }
                } // else colorSpacing == RGB_BETWEEN_SPACEANDTIME
                break;
            case ModelStorageBase.ARGB_FLOAT:
                if (colorSpacing == RGB_LAST) {
                  nBytes = buffer.length;
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes;
                  progressLength = numColors * nBytes * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 4, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);
                    b3 = getUnsignedByte(byteBuffer, j + 2);
                    b4 = getUnsignedByte(byteBuffer, j + 3);

                    if (endianess) {
                      tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                    }
                    else {
                      tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                    }
                    buffer[4 * i + 1] = Float.intBitsToFloat(tmpInt);
                  }
                  savedPosition = raFile.getFilePointer();
                  raFile.seek( (numberSlices - 1) * nBytes + savedPosition);
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes + nBytes;
                  for (i = 0, j = 0; j < nBytes; j += 4, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);
                    b3 = getUnsignedByte(byteBuffer, j + 2);
                    b4 = getUnsignedByte(byteBuffer, j + 3);

                    if (endianess) {
                      tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                    }
                    else {
                      tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                    }
                    buffer[4 * i + 2] = Float.intBitsToFloat(tmpInt);
                  }
                  if (numColors == 3) {
                    raFile.seek( (2 * numberSlices - 1) * nBytes + savedPosition);
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = 3 * slice * nBytes + 2 * nBytes;
                    for (i = 0, j = 0; j < nBytes; j += 4, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);
                      b3 = getUnsignedByte(byteBuffer, j + 2);
                      b4 = getUnsignedByte(byteBuffer, j + 3);

                      if (endianess) {
                        tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                      }
                      else {
                        tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                      }
                      buffer[4 * i + 3] = Float.intBitsToFloat(tmpInt);
                    }
                  } // if (numColors == 3)
                  raFile.seek(savedPosition);
                } // if (colorSpacing == RGB_LAST)
                else if (colorSpacing == RGB_FIRST) {
                  if (numColors == 2) {
                    nBytes = 2 * buffer.length;
                    byteBuffer = new byte[nBytes];
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = slice * nBytes;
                    progressLength = nBytes * numberSlices;
                    mod = progressLength / 100;
                    progressBar.setVisible(isProgressBarVisible());
                    for (j = 0; j < nBytes; j += 8, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);
                      b3 = getUnsignedByte(byteBuffer, j + 2);
                      b4 = getUnsignedByte(byteBuffer, j + 3);
                      b5 = getUnsignedByte(byteBuffer, j + 4);
                      b6 = getUnsignedByte(byteBuffer, j + 5);
                      b7 = getUnsignedByte(byteBuffer, j + 6);
                      b8 = getUnsignedByte(byteBuffer, j + 7);

                      if (endianess) {
                        tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                        tmpInt2 = ( (b5 << 24) | (b6 << 16) | (b7 << 8) | b8);
                      }
                      else {
                        tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                        tmpInt2 = ( (b8 << 24) | (b7 << 16) | (b6 << 8) | b5);
                      }
                      buffer[4 * i + 1] = Float.intBitsToFloat(tmpInt);
                      buffer[4 * i + 2] = Float.intBitsToFloat(tmpInt2);
                    }
                  } // if (numColors == 2)
                  else { // numColors == 3
                    nBytes = 3 * buffer.length;
                    byteBuffer = new byte[nBytes];
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = slice * nBytes;
                    progressLength = nBytes * numberSlices;
                    mod = progressLength / 100;
                    progressBar.setVisible(isProgressBarVisible());
                    for (j = 0; j < nBytes; j += 12, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);
                      b3 = getUnsignedByte(byteBuffer, j + 2);
                      b4 = getUnsignedByte(byteBuffer, j + 3);
                      b5 = getUnsignedByte(byteBuffer, j + 4);
                      b6 = getUnsignedByte(byteBuffer, j + 5);
                      b7 = getUnsignedByte(byteBuffer, j + 6);
                      b8 = getUnsignedByte(byteBuffer, j + 7);
                      b9 = getUnsignedByte(byteBuffer, j + 8);
                      b10 = getUnsignedByte(byteBuffer, j + 9);
                      b11 = getUnsignedByte(byteBuffer, j + 10);
                      b12 = getUnsignedByte(byteBuffer, j + 11);

                      if (endianess) {
                        tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                        tmpInt2 = ( (b5 << 24) | (b6 << 16) | (b7 << 8) | b8);
                        tmpInt3 = ( (b9 << 24) | (b10 << 16) | (b11 << 8) | b12);
                      }
                      else {
                        tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                        tmpInt2 = ( (b8 << 24) | (b7 << 16) | (b6 << 8) | b5);
                        tmpInt3 = ( (b12 << 24) | (b11 << 16) | (b10 << 8) | b9);
                      }
                      buffer[4 * i + 1] = Float.intBitsToFloat(tmpInt);
                      buffer[4 * i + 2] = Float.intBitsToFloat(tmpInt2);
                      buffer[4 * i + 3] = Float.intBitsToFloat(tmpInt3);
                    }
                  } // else numColors == 3
                } // else if (colorSpacing == RGB_FIRST)
                else { // colorSpacing == RGB_BETWEEN_SPACEANDTIME
                  nBytes = buffer.length;
                  byteBuffer = new byte[nBytes];
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes;
                  progressLength = numColors * nBytes * numberSlices;
                  mod = progressLength / 100;
                  progressBar.setVisible(isProgressBarVisible());
                  for (j = 0; j < nBytes; j += 4, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);
                    b3 = getUnsignedByte(byteBuffer, j + 2);
                    b4 = getUnsignedByte(byteBuffer, j + 3);

                    if (endianess) {
                      tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                    }
                    else {
                      tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                    }
                    buffer[4 * i + 1] = Float.intBitsToFloat(tmpInt);
                  }
                  savedPosition = raFile.getFilePointer();
                  raFile.seek( (numberSpaceSlices - 1) * nBytes + savedPosition);
                  raFile.read(byteBuffer, 0, nBytes);
                  progress = numColors * slice * nBytes + nBytes;
                  for (i = 0, j = 0; j < nBytes; j += 4, i++) {
                    if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                        round( (float) (j + progress) /
                              progressLength * 100), false);
                    b1 = getUnsignedByte(byteBuffer, j);
                    b2 = getUnsignedByte(byteBuffer, j + 1);
                    b3 = getUnsignedByte(byteBuffer, j + 2);
                    b4 = getUnsignedByte(byteBuffer, j + 3);

                    if (endianess) {
                      tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                    }
                    else {
                      tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                    }
                    buffer[4 * i + 2] = Float.intBitsToFloat(tmpInt);
                  }
                  if (numColors == 3) {
                    raFile.seek( (2 * numberSpaceSlices - 1) * nBytes + savedPosition);
                    raFile.read(byteBuffer, 0, nBytes);
                    progress = 3 * slice * nBytes + 2 * nBytes;
                    for (i = 0, j = 0; j < nBytes; j += 4, i++) {
                      if ( (j + progress) % mod == 0) progressBar.updateValue(Math.
                          round( (float) (j + progress) /
                                progressLength * 100), false);
                      b1 = getUnsignedByte(byteBuffer, j);
                      b2 = getUnsignedByte(byteBuffer, j + 1);
                      b3 = getUnsignedByte(byteBuffer, j + 2);
                      b4 = getUnsignedByte(byteBuffer, j + 3);

                      if (endianess) {
                        tmpInt = ( (b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                      }
                      else {
                        tmpInt = ( (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                      }
                      buffer[4 * i + 3] = Float.intBitsToFloat(tmpInt);
                    }
                  } // if (numColors == 3)
                  if (((slice+1)%numberSpaceSlices) == 0) {

                  }
                  else {
                    raFile.seek(savedPosition);
                  }
                } // else colorSpacing == RGB_BETWEEN_SPACETIME
                break;
        } // switch(dataType)

    }

    /**
    *   Reads a slice of data at a time and stores the results in the buffer
    *   @param slice            offset into the file stored in the dataOffset array
    *   @param bufferR           buffer where the real info is stored
    *   @param bufferI           buffer where the imaginary info is stored
    *   @exception IOException  if there is an error reading the file
    */
    private void readComplexBuffer(int slice, float bufferR[],
                                   float bufferI[]) throws IOException {
        int i = 0;
        int j;
        int nBytes;
        int b1, b2, b3, b4;
        byte [] byteBuffer;
        int progress, progressLength, mod;
        int tmpInt;

        nBytes = 8 * bufferR.length;
        byteBuffer =  new byte[nBytes];
        raFile.read(byteBuffer, 0, nBytes);
        progress = slice*bufferR.length;
        progressLength = bufferR.length*numberSlices;
        mod = progressLength/10;
        progressBar.setVisible(isProgressBarVisible());
        for (j =0; j < nBytes; j+=8, i++ ) {
            if ((i+progress)%mod==0) progressBar.updateValue( Math.round((float)(i+progress)/
                                                        progressLength * 100), false);
            b1 = getUnsignedByte(byteBuffer, j);
            b2 = getUnsignedByte(byteBuffer, j+1);
            b3 = getUnsignedByte(byteBuffer, j+2);
            b4 = getUnsignedByte(byteBuffer, j+3);

            if (endianess) {
                tmpInt=((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);  // Big Endian
            }
            else {
                tmpInt=((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);  // Little Endian
            }
            bufferR[i] = Float.intBitsToFloat(tmpInt);

            b1 = getUnsignedByte(byteBuffer, j+4);
            b2 = getUnsignedByte(byteBuffer, j+5);
            b3 = getUnsignedByte(byteBuffer, j+6);
            b4 = getUnsignedByte(byteBuffer, j+7);

            if (endianess) {
                tmpInt=((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);  // Big Endian
            }
            else {
                tmpInt=((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);  // Little Endian
            }
            bufferI[i] = Float.intBitsToFloat(tmpInt);
        } // for (j =0; j < nBytes; j+=8, i++ )
    }

    /**
    *   Reads a slice of data at a time and stores the results in the buffer
    *   @param slice            offset into the file stored in the dataOffset array
    *   @param bufferR           buffer where the real info is stored
    *   @param bufferI           buffer where the imaginary info is stored
    *   @exception IOException  if there is an error reading the file
    */
    private void readDComplexBuffer(int slice, double bufferR[],
                                   double bufferI[]) throws IOException {
        int i = 0;
        int j;
        int nBytes;
        long b1, b2, b3, b4, b5, b6, b7, b8;
        byte [] byteBuffer;
        int progress, progressLength, mod;
        long tmpLong;

        nBytes = 16 * bufferR.length;
        byteBuffer =  new byte[nBytes];
        raFile.read(byteBuffer, 0, nBytes);
        progress = slice*bufferR.length;
        progressLength = bufferR.length*numberSlices;
        mod = progressLength/10;
        progressBar.setVisible(isProgressBarVisible());
        for (j =0; j < nBytes; j+=16, i++ ) {
            if ((i+progress)%mod==0) progressBar.updateValue( Math.round((float)(i+progress)/
                                                        progressLength * 100), false);
            b1 = getUnsignedByte(byteBuffer, j);
            b2 = getUnsignedByte(byteBuffer, j+1);
            b3 = getUnsignedByte(byteBuffer, j+2);
            b4 = getUnsignedByte(byteBuffer, j+3);
            b5 = getUnsignedByte(byteBuffer, j+4);
            b6 = getUnsignedByte(byteBuffer, j+5);
            b7 = getUnsignedByte(byteBuffer, j+6);
            b8 = getUnsignedByte(byteBuffer, j+7);

            if (endianess) {
                tmpLong =((b1 << 56) | (b2 << 48) | (b3 << 40) | (b4  << 32) |
                          (b5 << 24) | (b6 << 16) | (b7 << 8) | b8);  // Big Endian
            }
            else {
                tmpLong = ((b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) |
                         (b4 << 24) | (b3 << 16) | (b2 << 8) | b1);  // Little Endian
            }
            bufferR[i] = Double.longBitsToDouble(tmpLong);

            b1 = getUnsignedByte(byteBuffer, j+8);
            b2 = getUnsignedByte(byteBuffer, j+9);
            b3 = getUnsignedByte(byteBuffer, j+10);
            b4 = getUnsignedByte(byteBuffer, j+11);
            b5 = getUnsignedByte(byteBuffer, j+12);
            b6 = getUnsignedByte(byteBuffer, j+13);
            b7 = getUnsignedByte(byteBuffer, j+14);
            b8 = getUnsignedByte(byteBuffer, j+15);

            if (endianess) {
                tmpLong =((b1 << 56) | (b2 << 48) | (b3 << 40) | (b4  << 32) |
                          (b5 << 24) | (b6 << 16) | (b7 << 8) | b8);  // Big Endian
            }
            else {
                tmpLong = ((b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) |
                         (b4 << 24) | (b3 << 16) | (b2 << 8) | b1);  // Little Endian
            }
            bufferI[i] = Double.longBitsToDouble(tmpLong);
        } // for (j =0; j < nBytes; j+=16, i++ )
    }

    /**
    *   Reads a slice of data at a time and stores the results in the buffer
    *   @param slice            offset into the file stored in the dataOffset array
    *   @param buffer           buffer where the info is stored
    *   @exception IOException  if there is an error reading the file
    */
    private void readLBuffer(int slice, long buffer[]) throws IOException {
        int i = 0;
        int j;
        int nBytes;
        long b1, b2, b3, b4, b5 , b6, b7, b8;
        byte [] byteBuffer;
        int progress, progressLength, mod;

        if (dataType == ModelStorageBase.UINTEGER) {  // reading 4 byte unsigned integers
            byteBuffer =  new byte[4 * buffer.length];
            nBytes = 4 * buffer.length;
            raFile.read(byteBuffer, 0, nBytes);
            progress = slice*buffer.length;
            progressLength = buffer.length*numberSlices;
            mod = progressLength/10;
            progressBar.setVisible(isProgressBarVisible());
            for (j =0; j < nBytes; j+=4, i++ ) {
                if ((i+progress)%mod==0) progressBar.updateValue( Math.round((float)(i+progress)/
                                                                  progressLength * 100), false);
                b1 = getUnsignedByte(byteBuffer, j);
                b2 = getUnsignedByte(byteBuffer, j+1);
                b3 = getUnsignedByte(byteBuffer, j+2);
                b4 = getUnsignedByte(byteBuffer, j+3);
                if (endianess) {
                    buffer[i] = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4) & 0xffffffffL;
                }
                else {
                    buffer[i] = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1) & 0xffffffffL;
                }
            } // for (j =0; j < nBytes; j+=4, i++ )
        } // if (type == ModelStorageBase.UINTEGER)
        else { // reading 8 byte LONGS
            byteBuffer =  new byte[8 * buffer.length];
            nBytes = 8 * buffer.length;
            raFile.read(byteBuffer, 0, nBytes);
            progress = slice*buffer.length;
            progressLength = buffer.length*numberSlices;
            mod = progressLength/10;
            progressBar.setVisible(isProgressBarVisible());
            for (j =0; j < nBytes; j+=8, i++ ) {
                if ((i+progress)%mod==0) progressBar.updateValue( Math.round((float)(i+progress)/
                                                                  progressLength * 100), false);
                b1 = getUnsignedByte(byteBuffer, j);
                b2 = getUnsignedByte(byteBuffer, j+1);
                b3 = getUnsignedByte(byteBuffer, j+2);
                b4 = getUnsignedByte(byteBuffer, j+3);
                b5 = getUnsignedByte(byteBuffer, j+4);
                b6 = getUnsignedByte(byteBuffer, j+5);
                b7 = getUnsignedByte(byteBuffer, j+6);
                b8 = getUnsignedByte(byteBuffer, j+7);

                if (endianess) {
                    buffer[i]=((b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) |
                               (b5 << 24) | (b6 << 16) | (b7 << 8) | b8);  // Big Endian
                }
                else {
                    buffer[i] = ((b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) |
                                 (b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                }
            } // for (j =0; j < nBytes; j+=8, i++ )
        } // else reading 8 byte integers
    }



    /**
    *   Reads a slice of data at a time and stores the results in the buffer
    *   @param slice            offset into the file stored in the dataOffset array
    *   @param buffer           buffer where the info is stored
    *   @exception IOException  if there is an error reading the file
    */
    private void readDBuffer(int slice, double buffer[]) throws IOException {
        int i = 0;
        int j;
        int nBytes;
        long b1, b2, b3, b4, b5 , b6, b7, b8;
        byte [] byteBuffer;
        int progress, progressLength, mod;
        long tmpLong;

        byteBuffer =  new byte[8 * buffer.length];
        nBytes = 8 * buffer.length;
        raFile.read(byteBuffer, 0, nBytes);
        progress = slice*buffer.length;
        progressLength = buffer.length*numberSlices;
        mod = progressLength/10;
        progressBar.setVisible(isProgressBarVisible());
        for (j =0; j < nBytes; j+=8, i++ ) {
            if ((i+progress)%mod==0) progressBar.updateValue( Math.round((float)(i+progress)/
                                                              progressLength * 100), false);
            b1 = getUnsignedByte(byteBuffer, j);
            b2 = getUnsignedByte(byteBuffer, j+1);
            b3 = getUnsignedByte(byteBuffer, j+2);
            b4 = getUnsignedByte(byteBuffer, j+3);
            b5 = getUnsignedByte(byteBuffer, j+4);
            b6 = getUnsignedByte(byteBuffer, j+5);
            b7 = getUnsignedByte(byteBuffer, j+6);
            b8 = getUnsignedByte(byteBuffer, j+7);

            if (endianess) {
                tmpLong=((b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) |
                         (b5 << 24) | (b6 << 16) | (b7 << 8) | b8);  // Big Endian
            }
            else {
                tmpLong=((b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) |
                         (b4 << 24) | (b3 << 16) | (b2 << 8) | b1);  // Little Endian
            }
            buffer[i] = Double.longBitsToDouble(tmpLong);

        } // for (j =0; j < nBytes; j+=8, i++ )

    }

    /**
    *   Writes an ICS format type image.
    *   @param image      Image model of data to write.
    *   @param options    options such as starting and ending slices and times
    *   @exception IOException if there is an error writing the file
    */
    public void writeImage(ModelImage image, FileWriteOptions options) throws IOException {
        String fileHeaderName;
        String fileDataName;
        int zDim;
        int bitSize;
        boolean haveRed = false;
        boolean haveGreen = false;
        boolean haveBlue = false;
        float resols[];
        float startLocation[];
        int units[];
        float origin[] = new float[3];
        String lineString;
        byte [] line;
        int i, j;
        int lastPeriod;
        int t,z;
        int sliceSize;
        byte [] byteBuffer;
        short [] shortBuffer;
        int [] intBuffer;
        long [] longBuffer;
        float [] floatBuffer;
        float [] floatBufferI;
        double [] doubleBuffer;
        double [] doubleBufferI;
        int numberSlices;
        int count;
        int zBegin, zEnd;
        int tBegin, tEnd;
        int tmpInt;
        long tmpLong;

        lastPeriod = fileName.lastIndexOf(".");
        fileHeaderName = fileName.substring(0,lastPeriod+1) + "ICS";
        fileDataName = fileName.substring(0,lastPeriod+1) + "IDS";

        progressBar = new ViewJProgressBar(fileName, "Writing ICS header file...",
                                           0, 100, true, null, null);

        progressBar.setLocation((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2,
                                50);
        progressBar.setVisible(true);

        zBegin = options.getBeginSlice();
        zEnd = options.getEndSlice();

        if (image.getNDims() == 4) {
            tBegin = options.getBeginTime();
            tEnd = options.getEndTime();
        }
        else {
            tBegin = 0;
            tEnd = 0;
        }

        file    = new File(fileDir + fileHeaderName);
        raFile  = new RandomAccessFile(file,"rw");
        // Necessary so that if this is an overwritten file there isn't any
        // junk at the end
        raFile.setLength(0);

        // Write first line with field separator character followed by
        // line separator character
        lineString = new String("\t\r\n");
        line = lineString.getBytes();
        raFile.write(line);

        // Write the second line with the version number of the ics standard
        // used to write the data file
        lineString = new String("ics_version\t1.0\r\n");
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("filename\t") + fileName.substring(0,lastPeriod) + "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        parameters = image.getNDims() + 1;
        if (image.isColorImage()) {
            parameters++;
        }
        lineString = new String("layout\tparameters\t") + Integer.toString(parameters) +
                     "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("layout\torder\tbits\tx\ty");
        if (image.getNDims() >=3) {
            lineString = lineString + "\tz";
        }
        if (image.getNDims() >=4) {
            lineString = lineString + "\tt";
        }
        if (image.isColorImage()) {
            lineString = lineString +"\tch";
        }
        lineString = lineString + "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        switch(image.getType()) {
            case ModelStorageBase.BYTE:
            case ModelStorageBase.UBYTE:
            case ModelStorageBase.ARGB:
                bitSize = 8;
                break;
            case ModelStorageBase.SHORT:
            case ModelStorageBase.USHORT:
            case ModelStorageBase.ARGB_USHORT:
                bitSize = 16;
                break;
            case ModelStorageBase.INTEGER:
            case ModelStorageBase.UINTEGER:
            case ModelStorageBase.FLOAT:
            case ModelStorageBase.ARGB_FLOAT:
                bitSize = 32;
                break;
            case ModelStorageBase.LONG:
            case ModelStorageBase.DOUBLE:
            case ModelStorageBase.COMPLEX:
                bitSize = 64;
                break;
            case ModelStorageBase.DCOMPLEX:
                bitSize = 128;
                break;
            default:
                bitSize = 8;
        }

        lineString = new String("layout\tsizes\t") + Integer.toString(bitSize) +
                     "\t" + Integer.toString(image.getExtents()[0]) + "\t" +
                     Integer.toString(image.getExtents()[1]);
        if (image.getNDims() >= 3) {
            lineString = lineString + "\t" + Integer.toString(image.getExtents()[2]);
        }
        if (image.getNDims() >= 4) {
            lineString = lineString + "\t" + Integer.toString(image.getExtents()[3]);
        }
        if (image.isColorImage()) {
            image.calcMinMax();
            if (image.getMinR() != image.getMaxR()) {
                haveRed = true;
                numColors++;
            }
            if (image.getMinG() != image.getMaxG()) {
                haveGreen = true;
                numColors++;
            }
            if (image.getMinB() != image.getMaxB()) {
                haveBlue = true;
                numColors++;
            }
            lineString = lineString + "\t" + Integer.toString(numColors);
        } // if (image.isColorImage())
        lineString = lineString + "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("layout\tcoordinates\tvideo\r\n");
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("layout\tsignificant_bits\t") + Integer.toString(bitSize) +
                     "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("representation\tbyte_order\t");
        switch(image.getType()) {
            case ModelStorageBase.BYTE:
            case ModelStorageBase.UBYTE:
            case ModelStorageBase.ARGB:
                lineString = lineString + "1\r\n";
                break;
            case ModelStorageBase.SHORT:
            case ModelStorageBase.USHORT:
            case ModelStorageBase.ARGB_USHORT:
                lineString = lineString + "2\t1\r\n";
                break;
            case ModelStorageBase.INTEGER:
            case ModelStorageBase.UINTEGER:
            case ModelStorageBase.FLOAT:
            case ModelStorageBase.ARGB_FLOAT:
            case ModelStorageBase.COMPLEX:
                lineString = lineString + "4\t3\t2\t1\r\n";
                break;
            case ModelStorageBase.LONG:
            case ModelStorageBase.DOUBLE:
            case ModelStorageBase.DCOMPLEX:
                lineString = lineString + "8\t7\t6\t5\t4\t3\t2\t1\r\n";
                break;
            default:
                lineString = lineString + "1\r\n";
        }
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("representation\tformat\t");
        switch(image.getType()) {
            case ModelStorageBase.BYTE:
            case ModelStorageBase.UBYTE:
            case ModelStorageBase.ARGB:
            case ModelStorageBase.SHORT:
            case ModelStorageBase.USHORT:
            case ModelStorageBase.ARGB_USHORT:
            case ModelStorageBase.INTEGER:
            case ModelStorageBase.UINTEGER:
            case ModelStorageBase.LONG:
                lineString = lineString + "integer\r\n";
                break;
            case ModelStorageBase.FLOAT:
            case ModelStorageBase.ARGB_FLOAT:
            case ModelStorageBase.DOUBLE:
                lineString = lineString + "real\r\n";
                break;
            case ModelStorageBase.COMPLEX:
            case ModelStorageBase.DCOMPLEX:
                lineString = lineString + "complex\r\n";
                break;
            default:
                lineString = lineString + "integer\r\n";
        }
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("representation\tsign\t");
        switch(image.getType()) {
            case ModelStorageBase.BYTE:
            case ModelStorageBase.SHORT:
            case ModelStorageBase.INTEGER:
            case ModelStorageBase.LONG:
            case ModelStorageBase.FLOAT:
            case ModelStorageBase.ARGB_FLOAT:
            case ModelStorageBase.DOUBLE:
            case ModelStorageBase.COMPLEX:
            case ModelStorageBase.DCOMPLEX:
                lineString = lineString + "signed\r\n";
                break;
            case ModelStorageBase.UBYTE:
            case ModelStorageBase.USHORT:
            case ModelStorageBase.UINTEGER:
            case ModelStorageBase.ARGB:
            case ModelStorageBase.ARGB_USHORT:
                lineString = lineString + "unsigned\r\n";
                break;
            default:
                lineString = lineString + "unsigned\r\n";
        }
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("representation\tcompression\tuncompressed\r\n");
        line = lineString.getBytes();
        raFile.write(line);

        startLocation = image.getFileInfo()[0].getOrigin();
        lineString = new String("parameter\torigin\t0.000000");
        for (i = 0; i < image.getNDims(); i++) {
            lineString = lineString + "\t" + Float.toString(startLocation[i]);
        }
        if (image.isColorImage()) {
            lineString = lineString + "\t0.000000";
        }
        lineString = lineString + "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        resols = image.getFileInfo()[0].getResolutions();
        lineString = new String("parameter\tscale\t1.0");
        for (i = 0; i < image.getNDims(); i++) {
            lineString = lineString + "\t" + Float.toString(resols[i]);
        }
        if (image.isColorImage()) {
            lineString = lineString + "\t0.000000";
        }
        lineString = lineString + "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        units = image.getFileInfo()[0].getUnitsOfMeasure();
        lineString = new String("parameter\tunits\tbits");
        for (i = 0; i < image.getNDims(); i++) {
            lineString = lineString + "\t" + FileInfoBase.getUnitsOfMeasureStr(units[i]);
        }
        if (image.isColorImage()) {
            lineString = lineString + "\tundefined";
        }
        lineString = lineString + "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        lineString = new String("parameter\tlabels\tbits\tx\ty");
        if (image.getNDims() >= 3) {
            lineString = lineString + "\tz";
        }
        if (image.getNDims() >= 4) {
            lineString = lineString + "\tt";
        }
        if (image.isColorImage()) {
            lineString = lineString +"\tch";
        }
        lineString = lineString + "\r\n";
        line = lineString.getBytes();
        raFile.write(line);

        raFile.close();

        progressBar.setMessage("Writing data file");
        file = new File(fileDir + fileDataName);
        raFile = new RandomAccessFile(file,"rw");

        sliceSize = image.getExtents()[0]*image.getExtents()[1];
        if (image.getNDims() >= 3) {
            zDim = image.getExtents()[2];
        }
        else {
            zDim = 1;
        }
        numberSlices = (tEnd - tBegin + 1) * (zEnd - zBegin + 1);
        count = 0;

        switch(image.getFileInfo()[0].getDataType()) {
            case ModelStorageBase.BYTE:
            case ModelStorageBase.UBYTE:
                byteBuffer = new byte[sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/numberSlices, options.isActiveImage());
                        image.exportSliceXY(t*zDim + z,byteBuffer);
                        raFile.write(byteBuffer);
                    }
                }
                break;
            case ModelStorageBase.SHORT:
            case ModelStorageBase.USHORT:
                shortBuffer = new short[sliceSize];
                byteBuffer = new byte[2*sliceSize];
                for (t = 0; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/numberSlices, options.isActiveImage());
                        image.exportSliceXY(t*zDim + z,shortBuffer);
                        for (j = 0; j < sliceSize; j++) {
                            byteBuffer[2*j] = (byte)(shortBuffer[j] >>> 8);
                            byteBuffer[2*j+1] = (byte)(shortBuffer[j]);
                        }
                        raFile.write(byteBuffer);
                    } // for (z = zBegin; z <= zEnd; z++)
                } // for (t = tBegin; t <= tEnd; t++)
                break;
            case ModelStorageBase.INTEGER:
            case ModelStorageBase.UINTEGER:
                intBuffer = new int[sliceSize];
                byteBuffer = new byte[4*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100 * count++)/numberSlices, options.isActiveImage());
                        image.exportSliceXY(t*zDim + z,intBuffer);
                        for (j = 0; j < sliceSize; j++) {
                            byteBuffer[4*j] = (byte)(intBuffer[j] >>> 24);
                            byteBuffer[4*j+1] = (byte)(intBuffer[j] >>> 16);
                            byteBuffer[4*j+2] = (byte)(intBuffer[j] >>> 8);
                            byteBuffer[4*j+3] = (byte)(intBuffer[j]);
                        }
                        raFile.write(byteBuffer);
                    } // for (z = zBegin; z <= zEnd; z++)
                } // for (t = tBegin; t <= tEnd; t++)
                break;
            case ModelStorageBase.LONG:
                longBuffer = new long[sliceSize];
                byteBuffer = new byte[8*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100 * count++)/numberSlices, options.isActiveImage());
                        image.exportSliceXY(t*zDim + z,longBuffer);
                        for (j = 0; j < sliceSize; j++) {
                            byteBuffer[8*j] = (byte)(longBuffer[j] >>> 56);
                            byteBuffer[8*j+1] = (byte)(longBuffer[j] >>> 48);
                            byteBuffer[8*j+2] = (byte)(longBuffer[j] >>> 40);
                            byteBuffer[8*j+3] = (byte)(longBuffer[j] >>> 32);
                            byteBuffer[8*j+4] = (byte)(longBuffer[j] >>> 24);
                            byteBuffer[8*j+5] = (byte)(longBuffer[j] >>> 16);
                            byteBuffer[8*j+6] = (byte)(longBuffer[j] >>> 8);
                            byteBuffer[8*j+7] = (byte)(longBuffer[j]);
                        }
                        raFile.write(byteBuffer);
                    } // for (z = zBegin; z <= zEnd; z++)
                } // for (t = tBegin; t <= tEnd; t++)
                break;
            case ModelStorageBase.FLOAT:
                floatBuffer = new float[sliceSize];
                byteBuffer = new byte[4*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/numberSlices, options.isActiveImage());
                        image.exportSliceXY(t*zDim + z,floatBuffer);
                        for (j = 0; j < sliceSize; j++) {
                            tmpInt = Float.floatToIntBits(floatBuffer[j]);
                            byteBuffer[4*j] = (byte)(tmpInt >>> 24);
                            byteBuffer[4*j+1] = (byte)(tmpInt >>> 16);
                            byteBuffer[4*j+2] = (byte)(tmpInt >>> 8);
                            byteBuffer[4*j+3] = (byte)(tmpInt);
                        }
                        raFile.write(byteBuffer);
                    } // for (z = zBegin; z <= zEnd; z++)
                } // for (t = tBegin; t <= tEnd; t++)
                break;
            case ModelStorageBase.DOUBLE:
                doubleBuffer = new double[sliceSize];
                byteBuffer = new byte[8*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100 * count++)/numberSlices, options.isActiveImage());
                        image.exportSliceXY(t*zDim + z,doubleBuffer);
                        for (j = 0; j < sliceSize; j++) {
                            tmpLong = Double.doubleToLongBits(doubleBuffer[j]);
                            byteBuffer[8*j] = (byte)(tmpLong >>> 56);
                            byteBuffer[8*j+1] = (byte)(tmpLong >>> 48);
                            byteBuffer[8*j+2] = (byte)(tmpLong >>> 40);
                            byteBuffer[8*j+3] = (byte)(tmpLong >>> 32);
                            byteBuffer[8*j+4] = (byte)(tmpLong >>> 24);
                            byteBuffer[8*j+5] = (byte)(tmpLong >>> 16);
                            byteBuffer[8*j+6] = (byte)(tmpLong >>> 8);
                            byteBuffer[8*j+7] = (byte)(tmpLong);
                        }
                        raFile.write(byteBuffer);
                    } // for (z = zBegin; z <= zEnd; z++)
                } // for (t = tBegin; t <= tEnd; t++)
                break;
            case ModelStorageBase.COMPLEX:
                floatBuffer = new float[sliceSize];
                floatBufferI = new float[sliceSize];
                byteBuffer = new byte[8*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/numberSlices, options.isActiveImage());
                        image.exportComplexData(2*(t*zDim + z)*sliceSize,sliceSize,
                                                  floatBuffer,floatBufferI);
                        for (j = 0; j < sliceSize; j++) {
                            tmpInt = Float.floatToIntBits(floatBuffer[j]);
                            byteBuffer[8*j] = (byte)(tmpInt >>> 24);
                            byteBuffer[8*j+1] = (byte)(tmpInt >>> 16);
                            byteBuffer[8*j+2] = (byte)(tmpInt >>> 8);
                            byteBuffer[8*j+3] = (byte)(tmpInt);
                            tmpInt = Float.floatToIntBits(floatBufferI[j]);
                            byteBuffer[8*j+4] = (byte)(tmpInt >>> 24);
                            byteBuffer[8*j+5] = (byte)(tmpInt >>> 16);
                            byteBuffer[8*j+6] = (byte)(tmpInt >>> 8);
                            byteBuffer[8*j+7] = (byte)(tmpInt);
                        }
                        raFile.write(byteBuffer);
                    } // for (z = zBegin; z <= zEnd; z++)
                } // for (t = tBegin; t <= tEnd; t++)
                break;
            case ModelStorageBase.DCOMPLEX:
                doubleBuffer = new double[sliceSize];
                doubleBufferI = new double[sliceSize];
                byteBuffer = new byte[16*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/numberSlices, options.isActiveImage());
                        image.exportDComplexData(2*(t*zDim + z)*sliceSize,sliceSize,
                                                  doubleBuffer,doubleBufferI);
                        for (j = 0; j < sliceSize; j++) {
                            tmpLong = Double.doubleToLongBits(doubleBuffer[j]);
                            byteBuffer[16*j] = (byte)(tmpLong >>> 56);
                            byteBuffer[16*j+1] = (byte)(tmpLong >>> 48);
                            byteBuffer[16*j+2] = (byte)(tmpLong >>> 40);
                            byteBuffer[16*j+3] = (byte)(tmpLong >>> 32);
                            byteBuffer[16*j+4] = (byte)(tmpLong >>> 24);
                            byteBuffer[16*j+5] = (byte)(tmpLong >>> 16);
                            byteBuffer[16*j+6] = (byte)(tmpLong >>> 8);
                            byteBuffer[16*j+7] = (byte)(tmpLong);
                            tmpLong = Double.doubleToLongBits(doubleBufferI[j]);
                            byteBuffer[16*j+8] = (byte)(tmpLong >>> 56);
                            byteBuffer[16*j+9] = (byte)(tmpLong >>> 48);
                            byteBuffer[16*j+10] = (byte)(tmpLong >>> 40);
                            byteBuffer[16*j+11] = (byte)(tmpLong >>> 32);
                            byteBuffer[16*j+12] = (byte)(tmpLong >>> 24);
                            byteBuffer[16*j+13] = (byte)(tmpLong >>> 16);
                            byteBuffer[16*j+14] = (byte)(tmpLong >>> 8);
                            byteBuffer[16*j+15] = (byte)(tmpLong);
                        }
                        raFile.write(byteBuffer);
                    } // for (z = zBegin; z <= zEnd; z++)
                } // for (t = tBegin; t <= tEnd; t++)
                break;
            case ModelStorageBase.ARGB:
                byteBuffer = new byte[sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                        if (haveRed) {
                            image.exportRGBData(1,4*(t*zDim + z)*sliceSize,sliceSize,byteBuffer);
                        }
                        else {
                            image.exportRGBData(2,4*(t*zDim + z)*sliceSize,sliceSize,byteBuffer);
                        }
                        raFile.write(byteBuffer);
                    }
                }
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                        if (haveRed && haveGreen) {
                            image.exportRGBData(2,4*(t*zDim + z)*sliceSize,sliceSize,byteBuffer);
                        }
                        else {
                            image.exportRGBData(3,4*(t*zDim + z)*sliceSize,sliceSize,byteBuffer);
                        }
                        raFile.write(byteBuffer);
                    }
                }
                if (numColors == 3) {
                    for (t = tBegin; t <= tEnd; t++) {
                        for (z = zBegin; z <= zEnd; z++) {
                            progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                            image.exportRGBData(3,4*(t*zDim + z)*sliceSize,sliceSize,byteBuffer);
                            raFile.write(byteBuffer);
                        }
                    }
                } // if (numColors == 3)
                break;
            case ModelStorageBase.ARGB_USHORT:
                shortBuffer = new short[sliceSize];
                byteBuffer = new byte[2*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                        if (haveRed) {
                            image.exportRGBData(1,4*(t*zDim + z)*sliceSize,sliceSize,
                                                shortBuffer);
                        }
                        else {
                            image.exportRGBData(2,4*(t*zDim + z)*sliceSize,sliceSize,
                                                shortBuffer);
                        }
                        for (j = 0; j < sliceSize; j++) {
                            byteBuffer[2*j] = (byte)(shortBuffer[j] >>> 8);
                            byteBuffer[2*j+1] = (byte)(shortBuffer[j]);
                        }
                        raFile.write(byteBuffer);
                    }
                }
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                        if (haveRed && haveGreen) {
                            image.exportRGBData(2,4*(t*zDim + z)*sliceSize,sliceSize,
                                                shortBuffer);
                        }
                        else {
                            image.exportRGBData(3,4*(t*zDim + z)*sliceSize,sliceSize,
                                                shortBuffer);
                        }
                        for (j = 0; j < sliceSize; j++) {
                            byteBuffer[2*j] = (byte)(shortBuffer[j] >>> 8);
                            byteBuffer[2*j+1] = (byte)(shortBuffer[j]);
                        }
                        raFile.write(byteBuffer);
                    }
                }
                if (numColors == 3) {
                    for (t = tBegin; t <= tEnd; t++) {
                        for (z = zBegin; z <= zEnd; z++) {
                            progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                            image.exportRGBData(3,4*(t*zDim + z)*sliceSize,sliceSize,
                                                shortBuffer);
                            for (j = 0; j < sliceSize; j++) {
                                byteBuffer[2*j] = (byte)(shortBuffer[j] >>> 8);
                                byteBuffer[2*j+1] = (byte)(shortBuffer[j]);
                            }
                            raFile.write(byteBuffer);
                        }
                    }
                } // if (numColors == 3)
                break;
            case ModelStorageBase.ARGB_FLOAT:
                floatBuffer = new float[sliceSize];
                byteBuffer = new byte[4*sliceSize];
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                        if (haveRed) {
                            image.exportRGBData(1,4*(t*zDim + z)*sliceSize,sliceSize,
                                                floatBuffer);
                        }
                        else {
                            image.exportRGBData(2,4*(t*zDim + z)*sliceSize,sliceSize,
                                                floatBuffer);
                        }
                        for (j = 0; j < sliceSize; j++) {
                            tmpInt = Float.floatToIntBits(floatBuffer[j]);
                            byteBuffer[4*j] = (byte)(tmpInt >>> 24);
                            byteBuffer[4*j+1] = (byte)(tmpInt >>> 16);
                            byteBuffer[4*j+2] = (byte)(tmpInt >>> 8);
                            byteBuffer[4*j+3] = (byte)(tmpInt);
                        }
                        raFile.write(byteBuffer);
                    }
                }
                for (t = tBegin; t <= tEnd; t++) {
                    for (z = zBegin; z <= zEnd; z++) {
                        progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                        if (haveRed && haveGreen) {
                            image.exportRGBData(2,4*(t*zDim + z)*sliceSize,sliceSize,
                                                floatBuffer);
                        }
                        else {
                            image.exportRGBData(3,4*(t*zDim + z)*sliceSize,sliceSize,
                                                floatBuffer);
                        }
                        for (j = 0; j < sliceSize; j++) {
                            tmpInt = Float.floatToIntBits(floatBuffer[j]);
                            byteBuffer[4*j] = (byte)(tmpInt >>> 24);
                            byteBuffer[4*j+1] = (byte)(tmpInt >>> 16);
                            byteBuffer[4*j+2] = (byte)(tmpInt >>> 8);
                            byteBuffer[4*j+3] = (byte)(tmpInt);
                        }
                        raFile.write(byteBuffer);
                    }
                }
                if (numColors == 3) {
                    for (t = tBegin; t <= tEnd; t++) {
                        for (z = zBegin; z <= zEnd; z++) {
                            progressBar.updateValue((100*count++)/(numColors*numberSlices), options.isActiveImage());
                            image.exportRGBData(3,4*(t*zDim + z)*sliceSize,sliceSize,
                                                floatBuffer);
                            for (j = 0; j < sliceSize; j++) {
                                tmpInt = Float.floatToIntBits(floatBuffer[j]);
                                byteBuffer[4*j] = (byte)(tmpInt >>> 24);
                                byteBuffer[4*j+1] = (byte)(tmpInt >>> 16);
                                byteBuffer[4*j+2] = (byte)(tmpInt >>> 8);
                                byteBuffer[4*j+3] = (byte)(tmpInt);
                            }
                            raFile.write(byteBuffer);
                        }
                    }
                } // if (numColors == 3)
                break;
        } // switch(mage.getFileInfo()[0].getDataType())

        raFile.close();
        progressBar.dispose();
    }

}
