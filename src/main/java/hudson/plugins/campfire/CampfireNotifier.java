package hudson.plugins.campfire;

import hudson.tasks.Notifier;
import hudson.tasks.BuildStepMonitor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class CampfireNotifier extends Notifier {

    private transient Campfire campfire;
    private Room room;
    private String hudsonUrl;

    /**
     * Descriptor should be singleton. (Won't this just set a class constant to an instance (but not the only possible instance) of DescriptorImpl?)
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

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
        String message = build.getProject().getName() + " " + build.getDisplayName() + ": " + build.getResult().toString();
        // possible TODO: get most recent committer from log or even just show all using build.getCulprits()
        if (hudsonUrl != null && hudsonUrl.length() > 1) {
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
            this.room = campfire.findOrCreateRoomByName(room);
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
        publish(build);
        return true;
    }
}
