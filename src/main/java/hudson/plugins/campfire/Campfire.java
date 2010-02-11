package hudson.plugins.campfire;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import com.gargoylesoftware.htmlunit.xml.XmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlElement;

public class Campfire {
    private HttpClient client;
    private WebClient webClient;
    private String subdomain;
    private String token;

    public Campfire(String subdomain, String token) {
        super();
        this.subdomain = subdomain;
        this.token = token;
        client = new HttpClient();
        Credentials defaultcreds = new UsernamePasswordCredentials(token, "x");
        client.getState().setCredentials(new AuthScope(getHost(), 80, AuthScope.ANY_REALM), defaultcreds);
        client.getParams().setAuthenticationPreemptive(true);
        client.getParams().setParameter("http.useragent", "JTinder");
        webClient = new WebClient();
        webClient.setWebConnection(new HttpClientBackedWebConnection(webClient, client));
        webClient.setJavaScriptEnabled(false);
        webClient.setCookiesEnabled(true);
    }

    protected String getHost() {
        return this.subdomain + ".campfirenow.com";
    }

    public int post(String url, String body) throws IOException {
        PostMethod post = new PostMethod("http://" + getHost() + "/" + url);
        post.setRequestHeader("Content-Type", "application/xml");
        post.setRequestEntity(new StringRequestEntity(body, "application/xml", "UTF8"));
        try {
            return client.executeMethod(post);
        } finally {
            post.releaseConnection();
        }
    }

    public XmlPage get(String url) throws IOException {
        return (XmlPage) webClient.getPage("http://" + getHost() + "/" + url);
    }

    public boolean verify(int returnCode) {
        return (returnCode == 200 || (returnCode > 301 && returnCode < 399));
    }

    private List<Room> getRooms() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        XmlPage page = get("rooms.xml");
        List<Room> rooms = new ArrayList<Room>();
        for (XmlElement roomElement : (List<XmlElement>) page.getByXPath("//room")) {
            rooms.add(new Room(this, ((XmlElement) roomElement.getByXPath(".//name").get(0)).getTextContent().trim(), ((XmlElement) roomElement.getByXPath(".//id").get(0)).getTextContent().trim()));
        }
        return rooms;
    }

    private Room findRoomByName(String name) throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        for (Room room : getRooms()) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null;
    }

    private Room createRoom(String name) throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        verify(post("rooms.xml", "<request><room><name>" + name + "</name><topic></topic></room></request>"));
        return findRoomByName(name);
    }

    public Room findOrCreateRoomByName(String name) throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        Room room = findRoomByName(name);
        if (room != null) {
            return room;
        }
        return createRoom(name);
    }
}
