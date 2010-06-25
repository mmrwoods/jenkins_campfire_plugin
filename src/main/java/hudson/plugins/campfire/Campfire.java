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

import com.gargoylesoftware.htmlunit.WebClient;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Campfire {
    private HttpClient client;
    private WebClient webClient;
    private String subdomain;
    private String token;
    private boolean ssl;

    public Campfire(String subdomain, String token, boolean ssl) {
        super();
        this.subdomain = subdomain;
        this.token = token;
        this.ssl = ssl;
        client = new HttpClient();
        Credentials defaultcreds = new UsernamePasswordCredentials(token, "x");
        client.getState().setCredentials(new AuthScope(getHost(), -1, AuthScope.ANY_REALM), defaultcreds);
        client.getParams().setAuthenticationPreemptive(true);
        client.getParams().setParameter("http.useragent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16");
        webClient = new WebClient();
        webClient.setWebConnection(new HttpClientBackedWebConnection(webClient, client));
        webClient.setJavaScriptEnabled(true);
        webClient.setCookiesEnabled(true);
    }

    protected String getHost() {
      return this.subdomain + ".campfirenow.com";
    }

    protected String getProtocol() {
      if (this.ssl) { return "https://"; }
      return "http://";
    }

    public int post(String url, String body) throws IOException {
        PostMethod post = new PostMethod(getProtocol() + getHost() + "/" + url);
        post.setRequestHeader("Content-Type", "application/xml");
        post.setRequestEntity(new StringRequestEntity(body, "application/xml", "UTF8"));
        try {
            return client.executeMethod(post);
        } finally {
            post.releaseConnection();
        }
    }

    public String get(String url) throws IOException {
        GetMethod get = new GetMethod(getProtocol() + getHost() + "/" + url);
        get.setRequestHeader("Content-Type", "application/xml");
        try {
            client.executeMethod(get);
            return get.getResponseBodyAsString();
        } finally {
            get.releaseConnection();
        }
    }

    public boolean verify(int returnCode) {
        return (returnCode == 200 || (returnCode > 301 && returnCode < 399));
    }

    private List<Room> getRooms() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        String body = get("rooms.xml");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        StringReader reader = new StringReader(body);
        InputSource inputSource = new InputSource( reader );
        Document doc = builder.parse(inputSource);

        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression roomExpr = xpath.compile("//room");
        XPathExpression nameExpr = xpath.compile(".//name");
        XPathExpression idExpr = xpath.compile(".//id");

        NodeList roomNodeList = (NodeList) roomExpr.evaluate(doc, XPathConstants.NODESET);
        List<Room> rooms = new ArrayList<Room>();
        for (int i = 0; i < roomNodeList.getLength(); i++) {
            Node roomNode = roomNodeList.item(i);
            String name = ((NodeList) nameExpr.evaluate(roomNode, XPathConstants.NODESET)).item(0).getFirstChild().getNodeValue();
            String id = ((NodeList) idExpr.evaluate(roomNode, XPathConstants.NODESET)).item(0).getFirstChild().getNodeValue();
            rooms.add(new Room(this, name.trim(), id.trim()));
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
