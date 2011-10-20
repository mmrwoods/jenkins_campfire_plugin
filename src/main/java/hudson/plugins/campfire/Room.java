package hudson.plugins.campfire;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class Room {
    private transient Campfire campfire;
    private String name;
    private String id;

    public Room(Campfire cf, String name, String id) {
        super();
        this.campfire = cf;
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public void speak(String message) throws IOException {
        campfire.post("room/" + id + "/speak.xml", "<message><type>TextMessage</type><body>" + message + "</body></message>");
    }

    public void play(String sound) throws IOException {
        campfire.post("room/" + id + "/speak.xml", "<message><type>SoundMessage</type><body>" + sound + "</body></message>");
    }

}
