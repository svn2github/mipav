package gov.nih.mipav.model.scripting.parameters;


import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.structures.ModelImage;

import java.util.Enumeration;
import java.util.Hashtable;


/**
 * A lookup table containing Parameters keyed by a label/name.  Can be filled by the parser with Parameters for a script action execution, or filled by a scriptable dialog and used to record the parameters in a script.
 */
public class ParameterTable {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** The parameter table.  The keys are parameter labels/names, the values are various Parameter subclasses. */
    Hashtable paramTable = new Hashtable();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Removes all parameters from the table.
     */
    public void clear() {
        paramTable.clear();
    }

    /**
     * Checks to see if there is an entry in the parameter table for a particular label.
     *
     * @param   paramLabel  The label/name of the variable parameter to check for.
     *
     * @return  <code>True</code> if there is an entry in the table with the given label, <code>false</code> otherwise.
     */
    public boolean containsParameter(String paramLabel) {
        return paramTable.containsKey(paramLabel);
    }

    /**
     * Converts all of the parameters in the table into a comma delimited list of parameters, suitable for inclusion in a script.  No command line overriding is performed.
     *
     * @return  The information on the parameters in the table in string form. 
     */
    public String convertToString() {
        String str = new String();

        Parameter[] params = getParameters();

        for (int i = 0; i < params.length; i++) {

            if (i > 0) {
                str += ", ";
            }

            str += params[i].convertToString();
        }

        return str;
    }

    /**
     * Return one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the parameter to retrieve.
     *
     * @return  The requested parameter.
     */
    public Parameter get(String paramLabel) {
        try {
            Parameter param = (Parameter) paramTable.get(paramLabel);
            
            if (!(param instanceof ParameterImage) && !(param instanceof ParameterList)) {
                VariableTable varTable = VariableTable.getReference();
                if (varTable.isVariableSet(paramLabel)) {
                    try {
                        String overrideValue = varTable.interpolate(paramLabel);
                        param = ParameterFactory.newNonListParameter(paramLabel, param.getType(), overrideValue);
                    } catch (ParserException pe) {
                        throw new ParameterException(paramLabel, "Overriding of parameter value failed: " + pe.getLocalizedMessage());
                    }
                }
            }
            
            return param;
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the boolean value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the boolean parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public boolean getBoolean(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterBoolean) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the double value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the double parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public double getDouble(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterDouble) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the float value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the float parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public float getFloat(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterFloat) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the model image assigned to one of the image parameters from the table.
     *
     * @param   paramLabel  The label/name of the image variable parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public ModelImage getImage(String paramLabel) {
        try {
            return ((ParameterImage) get(paramLabel)).getImage();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the integer value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the integer parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public int getInt(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterInt) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the one of the parameters from the table (cast as a ParameterList).
     *
     * @param   paramLabel  The label/name of the list parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public ParameterList getList(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable?
            return (ParameterList) get(paramLabel);
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the long value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the long parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public long getLong(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterLong) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Get an array containing all of the parameters in the parameter table.  No command line overriding is performed.
     *
     * @return  The parameters in the table, in no particular order.
     */
    public Parameter[] getParameters() {
        Parameter[] paramsArray = new Parameter[size()];

        int i = 0;
        Enumeration paramsEnum = paramTable.elements();

        while (paramsEnum.hasMoreElements()) {
            paramsArray[i] = (Parameter) paramsEnum.nextElement();
            i++;
        }

        return paramsArray;
    }

    /**
     * Return the signed short value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the signed short parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public short getShort(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterShort) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the string value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the string parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public String getString(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterString) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the unsigned short value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the unsigned short parameter to retrieve.
     *
     * @return  The requested parameter's value.
     */
    public short getUShort(String paramLabel) {
        try {
            // TODO: allow overriding the value from the VariableTable
            return ((ParameterUShort) get(paramLabel)).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Add a parameter to a table.
     *
     * @param  param  The parameter to add.
     */
    public void put(Parameter param) {
        put(param.getLabel(), param);
    }

    /**
     * Add a parameter to the table.
     *
     * @param  paramLabel  The label/name of the parameter to add (does not have to be <code>param.getLabel()</code>.
     * @param  param       The parameter to add.
     */
    public void put(String paramLabel, Parameter param) {
        paramTable.put(paramLabel, param);
    }

    /**
     * Remove a parameter from the table.
     *
     * @param  paramLabel  The label/name of the parameter to remove.
     */
    public void remove(String paramLabel) {
        paramTable.remove(paramLabel);
    }

    /**
     * Returns the number of parameters contained in the table.
     *
     * @return  The number of parameters in the table.
     */
    public int size() {
        return paramTable.size();
    }
}
