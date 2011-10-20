package hudson.plugins.campfire;

import hudson.tasks.Notifier;
import hudson.tasks.BuildStepMonitor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.io.*;
import java.util.regex.Pattern;

import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class CampfireNotifier extends Notifier {

    private transient Campfire campfire;
    private Room room;
    private String hudsonUrl;
    private boolean smartNotify;

    // getter for project configuration..
    // Configured room name should be null unless different from descriptor/global room name
    public String getConfiguredRoomName() {
        if ( DESCRIPTOR.getRoom().equals(room.getName()) ) {
            return null;   
        } else {
            return room.getName();  
        }
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger LOGGER = Logger.getLogger(CampfireNotifier.class.getName());

    public CampfireNotifier() throws IOException {
        super();
        initialize();
    }

    public CampfireNotifier(String subdomain, String token, String room, String hudsonUrl, boolean ssl, boolean smartNotify) throws IOException {
        super();
        initialize(subdomain, token, room, hudsonUrl, ssl, smartNotify);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private void publish(AbstractBuild<?, ?> build) throws IOException {
        checkCampfireConnection();
        Result result = build.getResult();
        String changeString = "No changes";
        if (!build.hasChangeSetComputed()) {
            changeString = "Changes not determined";
        } else if (build.getChangeSet().iterator().hasNext()) {
            ChangeLogSet changeSet = build.getChangeSet();
            ChangeLogSet.Entry entry = build.getChangeSet().iterator().next();
            // note: iterator should return recent changes first, but GitChangeSetList currently reverses the log entries
            if (changeSet.getClass().getSimpleName().equals("GitChangeSetList")) {
                String exceptionLogMsg = "Workaround to obtain latest commit info from git plugin failed";
                try {
                    // find the sha for the first commit in the changelog file, and then grab the corresponding entry from the changeset, yikes!
                    String changeLogPath = build.getRootDir().toString() + File.separator + "changelog.xml";
                    String line;
                    String sha = "";
                    BufferedReader reader = new BufferedReader(new FileReader(changeLogPath));
                    while((line = reader.readLine()) != null) {
                        if (line.matches("^commit [a-zA-Z0-9]+$")) {
                            sha = line.replace("commit ", "");
                            break;
                        }
                    }
                    reader.close();
                    if (sha != "") {
                        Method getIdMethod = entry.getClass().getDeclaredMethod("getId");
                        for(ChangeLogSet.Entry nextEntry : build.getChangeSet()) {
                            if ( ( (String)getIdMethod.invoke(entry) ).compareTo(sha) != 0 ) entry = nextEntry;
                        }
                    }
                } catch ( IOException e ){
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( NoSuchMethodException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( IllegalAccessException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( SecurityException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( Exception e ) {
                    throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage(), e);
                }
            }
            String commitMsg = entry.getMsg().trim();
            if (commitMsg != "") {
                if (commitMsg.length() > 47) {
                    commitMsg = commitMsg.substring(0, 46)  + "...";
                }
                changeString = commitMsg + " - " + entry.getAuthor().toString();
            }
        }
        String resultString = result.toString();
        if (!smartNotify && result == Result.SUCCESS) resultString = resultString.toLowerCase();
        String message = build.getProject().getName() + " " + build.getDisplayName() + " \"" + changeString + "\": " + resultString;
        if (resultString == "FAILURE") {
          String message_sound = "/play trombone"
        } else {
          String message_sound = "/play rimshot"
        }
        String fail_message = build.getProject().getName() + " " + build.getDisplayName() + " \"" + changeString + "\": " + resultString;
        if (hudsonUrl != null && hudsonUrl.length() > 1 && (smartNotify || result != Result.SUCCESS)) {
            message = message + " (" + hudsonUrl + build.getUrl() + ")";
        }
        room.speak(message);
        room.speak(message_sound);
    }

    private void checkCampfireConnection() throws IOException {
        if (campfire == null) {
            initialize();
        }
    }

    private void initialize() throws IOException {
        initialize(DESCRIPTOR.getSubdomain(), DESCRIPTOR.getToken(), room.getName(), DESCRIPTOR.getHudsonUrl(), DESCRIPTOR.getSsl(), DESCRIPTOR.getSmartNotify());
    }

    private void initialize(String subdomain, String token, String roomName, String hudsonUrl, boolean ssl, boolean smartNotify) throws IOException {
        campfire = new Campfire(subdomain, token, ssl);
        String exceptionMsg = "Failed to initialize campfire notifier";
        try {
            this.room = campfire.findRoomByName(roomName);
            if ( this.room == null ) {
                String roomNotFoundMsg = "Room '" + roomName + "' not found";
                LOGGER.log(Level.WARNING, exceptionMsg, roomNotFoundMsg);
                throw new IOException(roomNotFoundMsg);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, exceptionMsg, e);
            throw new IOException(exceptionMsg, e);
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.WARNING, exceptionMsg, e);
            throw new IOException(exceptionMsg, e);
        } catch (XPathExpressionException e) {
            LOGGER.log(Level.WARNING, exceptionMsg, e);
            throw new IOException(exceptionMsg, e);
        } catch (SAXException e) {
            LOGGER.log(Level.WARNING, exceptionMsg, e);
            throw new IOException(exceptionMsg, e);
        }
        this.hudsonUrl = hudsonUrl;
        this.smartNotify = smartNotify;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        // If SmartNotify is enabled, only notify if:
        //  (1) there was no previous build, or
        //  (2) the current build did not succeed, or
        //  (3) the previous build failed and the current build succeeded.
        if (smartNotify) {
            AbstractBuild previousBuild = build.getPreviousBuild();
            if (previousBuild == null ||
                build.getResult() != Result.SUCCESS ||
                previousBuild.getResult() != Result.SUCCESS)
            {
                publish(build);
            }
        } else {
            publish(build);
        }
        return true;
    }
}
