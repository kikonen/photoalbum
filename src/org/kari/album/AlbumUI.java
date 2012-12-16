package org.kari.album;

import org.kari.album.ui.FolderBrowserWindow;
import org.kari.log.LogUtil;
import org.kari.util.config.Config;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Photo Album UI
 *
 * @author kari
 */
public final class AlbumUI extends UI {
    static int mIndexSeed;
    public static synchronized int nextId() {
        return mIndexSeed++;
    }

    private FolderBrowserWindow mMain;
    
    @Override
    public void init(VaadinRequest pRequest) {
        LogUtil.initialize();
        Config.init(AlbumConstants.CONFIG_FILE);
        
        AlbumConstants.LOG.info("vaadin: " + AlbumConstants.VERSION);
        AlbumConstants.LOG.info("root created: " + this);
        
        if (!AlbumConstants.ALBUM_PATH.exists()) {
            throw new RuntimeException("Missing: "+ AlbumConstants.ALBUM_PATH.getKey());
        }

        startExpireDetector();

        setContent(createLayout(pRequest));
    }

    private VerticalLayout createLayout(VaadinRequest pRequest) {
        mMain = new FolderBrowserWindow(this, pRequest);
        
        mMain.setCaption("Photo Album");
        
        setContent(mMain);

        return mMain;
    }

    private void startExpireDetector() {
        Thread t = new Thread("kill detector-" + nextId()) {
            @Override
            public void run() {
                try {
//                    Application app = getApplication();
//                    boolean running;
//                    synchronized (app) {
//                        running = app.isRunning();
//                    }
//                    while (running) {
//                        Thread.sleep(60000);
//                        synchronized (app) {
//                            running = app.isRunning();
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    // immediate exit
                } finally {
                    free();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    @Override 
    public void detach() {
        super.detach();
        free();
    }
    
    public void free() {
        AlbumConstants.LOG.info("root free: " + this);
        if (mMain != null) {
            mMain.free();
        }
    }

}
