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
    private boolean enabled = false;
    private String subdomain;
    private String token;
    private String room;
    private String hudsonUrl;
    private boolean ssl;

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

    public String getHudsonUrl() {
        return hudsonUrl;
    }

    public boolean getSsl() {
        return ssl;
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    /**
     * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest)
     */
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        try {
            return new CampfireNotifier(subdomain, token, room, hudsonUrl, ssl);
        } catch (Exception e) {
            throw new FormException("Failed to initialize campfire notifier - check your global campfire notifier configuration settings", e, "");
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        subdomain = req.getParameter("campfireSubdomain");
        token = req.getParameter("campfireToken");
        room = req.getParameter("campfireRoom");
        hudsonUrl = req.getParameter("campfireHudsonUrl");
        if ( hudsonUrl != null && !hudsonUrl.endsWith("/") ) {
            hudsonUrl = hudsonUrl + "/";
        }
        ssl = req.getParameter("campfireSsl") != null;
        save();
        return super.configure(req, json);
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
