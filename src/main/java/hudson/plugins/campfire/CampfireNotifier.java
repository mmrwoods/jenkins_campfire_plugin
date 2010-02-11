package hudson.plugins.campfire;

import hudson.tasks.Notifier;
import hudson.tasks.BuildStepMonitor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;

public class CampfireNotifier extends Notifier {

    private Room room;

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public void setRoom(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return this.room;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private void publish(AbstractBuild<?, ?> build) throws IOException {
        checkCampfireConnection();
        room.speak(build.getProject().getName() + " " + build.getDisplayName() + " completed:" + build.getResult().toString());
    }

    private void checkCampfireConnection() throws IOException {
        DESCRIPTOR.initCampfire();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        publish(build);
        return true;
    }
}
