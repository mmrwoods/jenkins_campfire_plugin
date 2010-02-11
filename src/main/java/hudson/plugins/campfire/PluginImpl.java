package hudson.plugins.campfire;

import hudson.Plugin;

public class PluginImpl extends Plugin {

    /**
     * @see hudson.Plugin#stop()
     */
    @Override
    public void stop() throws Exception {
        CampfireNotifier.DESCRIPTOR.stop();
    }
}
