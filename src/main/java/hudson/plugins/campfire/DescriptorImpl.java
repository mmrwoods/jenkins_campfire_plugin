package hudson.plugins.campfire;

import hudson.tasks.Publisher;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.AbstractProject;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.xml.sax.SAXException;
import net.sf.json.JSONObject;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    private transient Campfire campfire;
    private boolean enabled = false;
    private String subdomain;
    private String token;
    private String room;

    public DescriptorImpl() {
        super(CampfireNotifier.class);
        load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public String getToken() {
        return token;
    }

    public String getRoom() {
        return room;
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    /**
     * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest)
     */
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        CampfireNotifier result = new CampfireNotifier();
        try {
            initCampfire();
            result.setRoom(campfire.findOrCreateRoomByName(room));
            result.getRoom().join();
        } catch (IOException e) {
            throw new FormException(e, "Cannot join room");
        } catch (ParserConfigurationException e) {
            throw new FormException(e, "Cannot join room");
        } catch (XPathExpressionException e) {
            throw new FormException(e, "Cannot join room");
        } catch (SAXException e) {
            throw new FormException(e, "Cannot join room");
        }
        return result;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
      subdomain = req.getParameter("subdomain");
      token = req.getParameter("token");
      room = req.getParameter("room");
      save();
      return super.configure(req, json);
    }

    protected Campfire initCampfire() throws IOException {
        if (campfire == null) {
            campfire = new Campfire(subdomain, token);
        }
        return campfire;
    }

    public void stop() throws IOException {
        if (campfire == null) {
            return;
        }
        campfire = null;
    }

    /**
     * @see hudson.model.Descriptor#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "Campfire Notification";
    }

    /**
     * @see hudson.model.Descriptor#getHelpFile()
     */
    @Override
    public String getHelpFile() {
        return "/plugin/campfire/help.html";
    }
}
