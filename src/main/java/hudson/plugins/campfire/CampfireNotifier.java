package hudson.plugins.campfire;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CampfireNotifier extends Notifier {

    private transient Campfire campfire;
    private Room room;
    private String hudsonUrl;
    private boolean smartNotify;
    private boolean sound;

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

    public CampfireNotifier() {
        super();
        initialize();
    }

    public CampfireNotifier(String subdomain, String token, String room, String hudsonUrl, boolean ssl, boolean smartNotify, boolean sound) {
        super();
        initialize(subdomain, token, room, hudsonUrl, ssl, smartNotify, sound);
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
                    String sha = getCommitHash(changeLogPath);
                    if (!"".equals(sha)) {
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
            if (!"".equals(commitMsg)) {
                if (commitMsg.length() > 47) {
                    commitMsg = commitMsg.substring(0, 46)  + "...";
                }
                changeString = commitMsg + " - " + entry.getAuthor().toString();
            }
        }
        String resultString = result.toString();
        if (!smartNotify && result == Result.SUCCESS) resultString = resultString.toLowerCase();
        String message = build.getProject().getName() + " " + build.getDisplayName() + " \"" + changeString + "\": " + resultString;
        if (hudsonUrl != null && hudsonUrl.length() > 1 && (smartNotify || result != Result.SUCCESS)) {
            message = message + " (" + hudsonUrl + build.getUrl() + ")";
        }
        room.speak(message);
        if (sound) {
          String message_sound;
          if ("FAILURE".equals(resultString)) {
            message_sound = "trombone";
          } else {
            message_sound = "rimshot";
          }
          room.play(message_sound);
        }
    }

    private String getCommitHash(String changeLogPath) throws IOException {
        String sha = "";
        BufferedReader reader = new BufferedReader(new FileReader(changeLogPath));
        String line;
        while((line = reader.readLine()) != null) {
            if (line.matches("^commit [a-zA-Z0-9]+$")) {
                sha = line.replace("commit ", "");
                break;
            }
        }
        reader.close();
        return sha;
    }

    private void checkCampfireConnection() {
        if (campfire == null) {
            initialize();
        }
    }

    private void initialize()  {
        initialize(DESCRIPTOR.getSubdomain(), DESCRIPTOR.getToken(), room.getName(), DESCRIPTOR.getHudsonUrl(), DESCRIPTOR.getSsl(), DESCRIPTOR.getSmartNotify(), DESCRIPTOR.getSound());
    }

    private void initialize(String subdomain, String token, String roomName, String hudsonUrl, boolean ssl, boolean smartNotify, boolean sound) {
        campfire = new Campfire(subdomain, token, ssl);
        this.room = campfire.findRoomByName(roomName);
        if ( this.room == null ) {
            throw new RuntimeException("Room '" + roomName + "' not found - verify name and room permissions");
        }
        this.hudsonUrl = hudsonUrl;
        this.smartNotify = smartNotify;
        this.sound = sound;
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
