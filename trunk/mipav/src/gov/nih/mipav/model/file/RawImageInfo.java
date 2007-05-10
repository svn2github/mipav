package gov.nih.mipav.model.file;

import java.util.StringTokenizer;


public class RawImageInfo {
	
	private int type;
	private int [] dims;
	private float [] res;
	private int [] units;
	private int offset;
	private boolean bigEndian;
	
	public RawImageInfo(int type, int [] dims, float [] res, int [] units, int offset, boolean bigEndian) {
		this.type = type;
		this.dims = dims;
		this.res = res;
		this.units = units;
		this.offset = offset;
		this.bigEndian = bigEndian;
	}
	
	public RawImageInfo(String consoleString) {
		StringTokenizer tokens = new StringTokenizer(consoleString, ";");
    	String typeStr = tokens.nextToken();
    	String extStr = tokens.nextToken();
    	String resStr = tokens.nextToken();
    	String unitsStr = tokens.nextToken();
    	String endianStr = tokens.nextToken();
    	String offsetStr = tokens.nextToken();
    	
    	type = Integer.parseInt(typeStr);
    	
    	//get extents
    	tokens = new StringTokenizer(extStr, ",");
    	int firstDim = Integer.parseInt(tokens.nextToken());
    	int secondDim = Integer.parseInt(tokens.nextToken());
    	int thirdDim = 1;
    	int fourthDim = 1;
    	
    	try {
    		thirdDim = Integer.parseInt(tokens.nextToken());
    		fourthDim = Integer.parseInt(tokens.nextToken());
    	} catch (Exception e) {
    		//nada
    	}
    	    	
    	if (thirdDim > 1) {
    		if (fourthDim > 1) {
    			dims = new int[4];
    			dims[3] = fourthDim;
    		} else {
    			dims = new int[3];
    		}
    		dims[2] = thirdDim;
    	} else {
    		dims = new int[2];
    	}
    	dims[0] = firstDim;
    	dims[1] = secondDim;
    	
    	//get resolutions
    	tokens = new StringTokenizer(resStr, ",");
    	
    	res = new float[dims.length];
    	
    	for (int n = 0; n < res.length; n++) {
    		res[n] = Float.parseFloat(tokens.nextToken());
    	}
    	
    	//get units of measure
    	units = new int[dims.length];
    	tokens = new StringTokenizer(unitsStr, ",");
    	for (int n = 0; n < units.length; n++) {
    		units[n] = Integer.parseInt(tokens.nextToken());
    	}
    	
    	bigEndian = Boolean.parseBoolean(endianStr);
    	
    	offset = Integer.parseInt(offsetStr);
	}
	
	public int getDataType() {
		return this.type;
	}
	
	public int[] getExtents() {
		return this.dims;
	}
	
	public int[] getUnitsOfMeasure() {
		return this.units;
	}
	
	public float [] getResolutions() {
		return this.res;
	}
	
	public boolean getEndianess() {
		return this.bigEndian;
	}
	
	public int getOffset() {
		return this.offset;
	}
}
