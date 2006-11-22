package gov.nih.mipav.model.scripting.parameters;


import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.util.*;


/**
 * A lookup table containing Parameters keyed by a label/name. Can be filled by the parser with Parameters for a script
 * action execution, or filled by a scriptable dialog and used to record the parameters in a script.
 */
public class ParameterTable {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** The parameter table. The keys are parameter labels/names, the values are various Parameter subclasses. */
    Map paramTable = new LinkedHashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Removes all parameters from the table.
     */
    public void clear() {
        paramTable.clear();
    }

    /**
     * Checks to see if there is an entry in the parameter table for a particular label. It also checks to see if a
     * value for the parameter has been set in the global VariableTable.
     *
     * @param   paramLabel  The label/name of the variable parameter to check for.
     *
     * @return  <code>True</code> if there is an entry in the table with the given label, <code>false</code> otherwise.
     */
    public boolean containsParameter(String paramLabel) {
        return paramTable.containsKey(paramLabel) || VariableTable.getReference().isVariableSet(paramLabel);
    }

    /**
     * Converts all of the parameters in the table into a comma delimited list of parameters, suitable for inclusion in
     * a script. No command line overriding is performed.
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
     * Return the boolean value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the boolean parameter to retrieve.
     *
     * @return  The requested parameter's value.
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public boolean getBoolean(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_BOOLEAN);

            return ((ParameterBoolean) param).getValue();
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
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public double getDouble(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_DOUBLE);

            return ((ParameterDouble) param).getValue();
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
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public float getFloat(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_FLOAT);

            return ((ParameterFloat) param).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return the model image assigned to one of the image parameters from the table. No overriding from the
     * VariableTable is allowed.
     *
     * @param   paramLabel  The label/name of the image variable parameter to retrieve.
     *
     * @return  The requested parameter's value.
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public ModelImage getImage(String paramLabel) {

        try {
            return getImageParameter(paramLabel).getImage();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Return one of the image parameters from the table. No overriding from the VariableTable is allowed.
     *
     * @param   paramLabel  The label/name of the image variable parameter to retrieve.
     *
     * @return  The requested parameter.
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public ParameterImage getImageParameter(String paramLabel) {

        try {
            return (ParameterImage) paramTable.get(paramLabel);
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
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public int getInt(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_INT);

            return ((ParameterInt) param).getValue();
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
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public ParameterList getList(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_LIST);

            return (ParameterList) param;
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
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public long getLong(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_LONG);

            return ((ParameterLong) param).getValue();
        } catch (NullPointerException npe) {
            throw new ParameterException(paramLabel, npe.getLocalizedMessage());
        } catch (ClassCastException cce) {
            throw new ParameterException(paramLabel, cce.getLocalizedMessage());
        }
    }

    /**
     * Get an array containing all of the parameters in the parameter table. No command line overriding is performed.
     *
     * @return  The parameters in the table, in no particular order.
     */
    public Parameter[] getParameters() {
        Parameter[] paramArray = new Parameter[size()];
        paramTable.values().toArray(paramArray);

        return paramArray;
    }

    /**
     * Return the signed short value of one of the parameters from the table.
     *
     * @param   paramLabel  The label/name of the signed short parameter to retrieve.
     *
     * @return  The requested parameter's value.
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public short getShort(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_SHORT);

            return ((ParameterShort) param).getValue();
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
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public String getString(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_STRING);

            return ((ParameterString) param).getValue();
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
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    public short getUShort(String paramLabel) {

        try {
            Parameter param = getWithOverride(paramLabel, Parameter.PARAM_USHORT);

            return ((ParameterUShort) param).getValue();
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

    /**
     * Returns a parameter from the table, possibly overriding it with a value stored in the global VariableTable.
     *
     * @param   paramLabel  The label of the parameter to return.
     * @param   paramType   The type of the parameter we want returned.
     *
     * @return  The requested parameter from the table.
     *
     * @throws  ParameterException  DOCUMENT ME!
     */
    protected Parameter getWithOverride(String paramLabel, int paramType) {
        Parameter param = (Parameter) paramTable.get(paramLabel);

        if (!(param instanceof ParameterImage)) {
            VariableTable varTable = VariableTable.getReference();

            if (varTable.isVariableSet(paramLabel)) {

                try {
                    String overrideValue = varTable.interpolate(paramLabel);
                    Preferences.debug("param table:\t Overriding parameter (" + paramLabel + ") with value:\t" +
                                      overrideValue + "\n", Preferences.DEBUG_SCRIPTING);
                    param = ParameterFactory.newNonListParameter(paramLabel, paramType, overrideValue);
                } catch (ParserException pe) {
                    throw new ParameterException(paramLabel,
                                                 "Overriding of parameter value failed: " + pe.getLocalizedMessage());
                }
            }
        }

        return param;
    }
}
