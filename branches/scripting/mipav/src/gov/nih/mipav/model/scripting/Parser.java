package gov.nih.mipav.model.scripting;


import gov.nih.mipav.model.scripting.actions.*;
import gov.nih.mipav.model.scripting.parameters.Parameter;

import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewUserInterface;

import gov.nih.mipav.view.dialogs.AlgorithmParameters;

import java.util.Vector;


/**
 * This is the main class used to parse and run scripts.  It may also be used to retrieve the image placeholder
 * variables (e.g., $image1) used in a script and the number of VOIs required for a particular image placeholder.
 */
public class Parser {

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns an array of image placeholder variables used in a given script.
     *
     * @param   scriptFile  The full path to the script file on disk which should be parsed.
     *
     * @return  An array of the image placeholder variables (e.g. $image1) used in <code>scriptFile</code>.
     *
     * @throws  ParserException  If a problem is encountered while parsing the script.
     */
    public static final String[] getImageVarsUsedInScript(String scriptFile) throws ParserException {
        Vector imageVarList = new Vector();
        int numImages = 0;
        ParserEngine parser = new ParserEngine(scriptFile, true);

        while (parser.hasMoreLinesToParse()) {
            ParsedActionLine parsedLine = parser.parseNextLine();

            if (parsedLine != null) {
                Parameter[] params = parsedLine.getParameterTable().getParameters();

                for (int i = 0; i < params.length; i++) {

                    if (params[i].getType() == Parameter.PARAM_EXTERNAL_IMAGE) {

                        if (!imageVarList.contains(params[i].getValueString())) {
                            imageVarList.add(params[i].getValueString());
                            numImages++;
                        }
                    }
                }
            }
        }

        String[] imageVars = new String[imageVarList.size()];

        for (int i = 0; i < imageVars.length; i++) {
            imageVars[i] = (String) imageVarList.elementAt(i);
        }

        Preferences.debug("parser:\tFound " + numImages + " unique images used in script " + scriptFile + "\n",
                          Preferences.DEBUG_SCRIPTING);

        return imageVars;
    }

    /**
     * Returns the number of VOIs which will be opened for a given image (<code>imageVarName</code>) when a particular script is run.
     *
     * @param   scriptFile    The full path to the script file on disk which should be parsed.
     * @param   imageVarName  The image placeholder variable (e.g. $image1) for which to count required VOIs.
     *
     * @return  The number of VOIs required for the given <code>imageVarName</code>.
     *
     * @throws  ParserException  If a problem is encountered while parsing the script.
     */
    public static final int getNumberOfVOIsRequiredForImageVar(String scriptFile, String imageVarName)
            throws ParserException {
        int numVOIs = 0;
        ParserEngine parser = new ParserEngine(scriptFile, true);

        while (parser.hasMoreLinesToParse()) {
            ParsedActionLine parsedLine = parser.parseNextLine();

            if (parsedLine != null) {

                if (Parser.isOpenVOIAction(parsedLine.getAction())) {

                    if (imageVarName.equalsIgnoreCase(Parser.getVOIParentImage(parsedLine))) {
                        numVOIs++;
                    }
                }
            }
        }

        Preferences.debug("parser:\tFound " + numVOIs + " unique VOIs used in script " + scriptFile + "attached to image " + imageVarName + "\n", Preferences.DEBUG_SCRIPTING);

        return numVOIs;
    }

    /**
     * Testing main method.
     * TODO: remove this method once testing is completed.
     *
     * @param  args  path to a script to parse and run.
     */
    public static final void main(String[] args) {

        try {
            String[] imageVarNames = Parser.getImageVarsUsedInScript(args[0]);
            for (int i = 0; i < imageVarNames.length; i++) {
                int numVOIs = Parser.getNumberOfVOIsRequiredForImageVar(args[0], imageVarNames[i]);
                System.out.println(imageVarNames[i] + " ===> " + numVOIs);
            }
            Parser.runScript(args[0]);
        } catch (ParserException pe) {
            MipavUtil.displayError("Fatal error encountered running script:\n" + pe);
        }
    }

    /**
     * Parses and runs the commands contained within a given script file.
     *
     * @param   scriptFile  The full path to the script file on disk which should be parsed.
     *
     * @throws  ParserException  If a problem is encountered while parsing the script.
     */
    public static final void runScript(String scriptFile) throws ParserException {
        // check that all externally-specified (not generated by the script actions) images have been setup 
        String[] requiredImages = Parser.getImageVarsUsedInScript(scriptFile);
        for (int i = 0; i < requiredImages.length; i++) {
            if (!ScriptRunner.getReference().getImageTable().isImageVariableSet(requiredImages[i])) {
                throw new ParserException(scriptFile, 0, "Required image not found: " + requiredImages[i]);
            }
        }
        
        // if the -hide argument was specified, we require an Exit command somewhere in the script.  otherwise mipav will appear to "hang"
        if (!ViewUserInterface.getReference().isAppFrameVisible() && !hasExitAction(scriptFile)) {
            // TODO: maybe automatically add the exit command to the end of the script...
            throw new ParserException(scriptFile, 0, "Scripts run with the -hide option must close MIPAV with an Exit command.");
        }
        
        ParserEngine parser = new ParserEngine(scriptFile, true);

        while (parser.hasMoreLinesToParse()) {
            ParsedActionLine parsedLine = parser.parseNextLine();

            if (parsedLine != null) {
                Preferences.debug("parser:\t\t" + parsedLine.convertToString() + "\n", Preferences.DEBUG_SCRIPTING);

                ScriptableActionInterface scriptAction = ScriptableActionLoader.getScriptableAction(parsedLine.getAction());
                
                Preferences.debug("parser:\tRunning action:\t" + scriptAction.getClass().getName() + "\n", Preferences.DEBUG_SCRIPTING);
                
                try {
                    scriptAction.scriptRun(parsedLine.getParameterTable());
                } catch (Exception e) {
                    String message = "\n\n" + e.getClass().getName() + "\n";
                    for (int i = 0; i < e.getStackTrace().length; i++) {
                        message += "\t" + e.getStackTrace()[i] + "\n";
                    }
                    ParserException exception = new ParserException(scriptFile, parser.getCurrentLineNumber(), message);
                    exception.initCause(e);
                    throw exception;
                }
            }
        }
    }

    /**
     * Returns the image placeholder variable referenced by an 'OpenVOI' action.
     *
     * @param   parsedLine  A parsed line from a script (should be an 'OpenVOI' action).
     *
     * @return  The image placeholder variable for the image the VOI will be loaded into. 
     */
    private static final String getVOIParentImage(ParsedActionLine parsedLine) {
        return parsedLine.getParameterTable().get(AlgorithmParameters.getInputImageLabel(1)).getValueString();
    }
    
    /**
     * Search the script file for an Exit action (required when running in hidden GUI mode from the command line).
     * 
     * @param   scriptFile  The full path to the script file on disk which should be parsed.
     * 
     * @return  <code>True</code> if the script contains an Exit command, <code>false</code> otherwise.
     * 
     * @throws  ParserException  If a problem is encountered while parsing the script.
     */
    private static final boolean hasExitAction(String scriptFile) throws ParserException {
        ParserEngine parser = new ParserEngine(scriptFile, true);
        
        while (parser.hasMoreLinesToParse()) {
            ParsedActionLine parsedLine = parser.parseNextLine();
            
            if (parsedLine != null) {
                if (parsedLine.getAction().equals(ActionBase.getActionName(ActionExit.class))) {
                    // found an Exit action
                    return true;
                }
            }
        }
        
        // no Exit action
        return false;
    }

    /**
     * Returns whether a action string would result in the opening of a VOI.
     *
     * @param   action  The action string to check.
     *
     * @return  Whether the given action will open a VOI.
     */
    private static final boolean isOpenVOIAction(String action) {

        if (action.equalsIgnoreCase(ActionBase.getActionName(ActionOpenVOI.class))) {
            return true;
        }

        return false;
    }
}
