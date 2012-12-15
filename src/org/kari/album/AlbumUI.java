package org.kari.album;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.*;

public class AlbumUI extends UI {
    @Override
    public void init(VaadinRequest request) {
        Label label = new Label("Hello World: photos");
        setContent(label);
    }
}
