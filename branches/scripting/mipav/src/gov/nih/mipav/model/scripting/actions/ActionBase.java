package gov.nih.mipav.model.scripting.actions;


import gov.nih.mipav.model.scripting.ScriptableActionInterface;
import gov.nih.mipav.model.scripting.parameters.ParameterTable;

import gov.nih.mipav.view.Preferences;


/**
 * A base class for all non-algorithmic (not JDialog*) script actions.
 */
public abstract class ActionBase implements ScriptableActionInterface {
    /**
     * {@inheritDoc}
     */
    public abstract void insertScriptLine();
    
    /**
     * {@inheritDoc}
     */
    public abstract void scriptRun(ParameterTable parameters);
    
    /**
     * Returns the script command string for this action.
     * 
     * @return  The script command which should be used for this action class (e.g., Clone for ActionClone).
     */
    protected String getActionName() {
        String name = getClass().getName();
        
        int index = name.lastIndexOf("Action");
        
        if (index == -1) {
            // TODO: may be an fatal error..
            Preferences.debug("action base: No script Action prefix found.  Returning " + name + "\n", Preferences.DEBUG_SCRIPTING);
            return name;
        } else {
            Preferences.debug("action base: Extracting script action command.  Returning " + name.substring(index + 6) + "\n", Preferences.DEBUG_SCRIPTING);
            return name.substring(index + 6);
        }
    }
}
