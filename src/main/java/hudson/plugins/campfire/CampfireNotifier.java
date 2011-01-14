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

import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class CampfireNotifier extends Notifier {

    private transient Campfire campfire;
    private Room room;
    private String hudsonUrl;
    private boolean smartNotify;

    /**
     * Descriptor should be singleton. (Won't this just set a class constant to an instance (but not the only possible instance) of DescriptorImpl?)
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger LOGGER = Logger.getLogger(CampfireNotifier.class.getName());

    public CampfireNotifier() throws IOException {
        super();
        initialize();
    }

    public CampfireNotifier(String subdomain, String token, String room, String hudsonUrl, boolean ssl) throws IOException {
        super();
        initialize(subdomain, token, room, hudsonUrl, ssl);
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
            // note: iterator should return recent changes first, but GitChangeSetList doesn't (at the moment)
            if (changeSet.getClass().getSimpleName().equals("GitChangeSetList")) {
                String log_warn_prefix = "Workaround to obtain latest commit info from git plugin failed: ";
                try {
                    Method getDateMethod = entry.getClass().getDeclaredMethod("getDate");
                    for(ChangeLogSet.Entry nextEntry : build.getChangeSet()) {
                        if ( ( (String)getDateMethod.invoke(entry) ).compareTo( (String)getDateMethod.invoke(nextEntry) ) < 0 ) entry = nextEntry;
                    }
                } catch ( NoSuchMethodException e ) {
                    LOGGER.log(Level.WARNING, log_warn_prefix + e.getMessage());
                } catch ( IllegalAccessException e ) {
                    LOGGER.log(Level.WARNING, log_warn_prefix + e.getMessage());
                } catch ( SecurityException e ) {
                    LOGGER.log(Level.WARNING, log_warn_prefix + e.getMessage());
                } catch ( Exception e ) {
                    throw new RuntimeException(e.getMessage(), e);
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
        if (!DESCRIPTOR.getSmartNotify() && result == Result.SUCCESS) resultString = resultString.toLowerCase();
        String message = build.getProject().getName() + " " + build.getDisplayName() + " \"" + changeString + "\": " + resultString;
        if (hudsonUrl != null && hudsonUrl.length() > 1 && (DESCRIPTOR.getSmartNotify() || result != Result.SUCCESS)) {
            message = message + " (" + hudsonUrl + build.getUrl() + ")";
        }
        room.speak(message);
    }

    private void checkCampfireConnection() throws IOException {
        if (campfire == null) {
            initialize();
        }
    }

    private void initialize() throws IOException {
        initialize(DESCRIPTOR.getSubdomain(), DESCRIPTOR.getToken(), DESCRIPTOR.getRoom(), DESCRIPTOR.getHudsonUrl(), DESCRIPTOR.getSsl());
    }

    private void initialize(String subdomain, String token, String room, String hudsonUrl, boolean ssl) throws IOException {
        campfire = new Campfire(subdomain, token, ssl);
        try {
            this.room = campfire.findRoomByName(room);
            if ( this.room == null ) {
              throw new IOException("Room '" + room + "' not found");
            }
        } catch (IOException e) {
            throw new IOException("Cannot join room: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new IOException("Cannot join room: " + e.getMessage());
        } catch (XPathExpressionException e) {
            throw new IOException("Cannot join room: " + e.getMessage());
        } catch (SAXException e) {
            throw new IOException("Cannot join room: " + e.getMessage());
        }
        this.hudsonUrl = hudsonUrl;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        // If SmartNotify is enabled, only notify if:
        //  (1) there was no previous build, or
        //  (2) the current build did not succeed, or
        //  (3) the previous build failed and the current build succeeded.
        if (DESCRIPTOR.getSmartNotify()) {
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
