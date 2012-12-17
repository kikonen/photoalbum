package org.kari.album.ui;

import java.io.File;

import org.kari.album.AlbumConstants;
import org.kari.album.AlbumUI;
import org.kari.album.ui.ImageAccess.ThumbInfo;
import org.kari.util.concurrent.Refresher;
import org.vaadin.artur.icepush.ICEPush;

import com.vaadin.server.SystemError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

/**
 * Browser for folder hierarchy
 *
 * @author kari
 */
public final class FolderBrowserWindow extends VerticalLayout 
    implements
        PreviewListener
{
    private FolderTreeView mTreeView;
    private FolderContentView mContentView;
    VerticalLayout mPreviewView;
    Label mPreviewLabel;
    ICEPush mPusher = new ICEPush();
    
    private transient Refresher<File> mPreviewer;
    

    public FolderBrowserWindow(AlbumUI pUI, VaadinRequest pRequest) {
        mContentView = new FolderContentView(mPusher, this);
        mTreeView = new FolderTreeView(mPusher, mContentView);

        {
            mPreviewLabel = new Label("", Label.CONTENT_RAW);
            
            VerticalLayout layout = new VerticalLayout();
            layout.setHeight("100%");

            layout.addComponent(mPreviewLabel);
            mPreviewView = layout;
        }        
        createLayout(pRequest);
        
        mTreeView.setRootDir(AlbumConstants.DISPLAY_PATH.getPath());
        mTreeView.showDir(mTreeView.getRootDir());
        
        mPusher.extend(pUI);
    }

    private void createLayout(VaadinRequest pRequest) {
        HorizontalSplitPanel browserPanel = createBrowserPanel();

        final VerticalLayout layout = this;//new VerticalLayout();
//        layout.setMargin(false, false, false, false);
        layout.setSizeFull();
        
        layout.addComponent(createToolbar());
        layout.addComponent(browserPanel);
//        layout.addComponent(mPusher);

        // @see http://vaadin.com/forum/-/message_boards/message/197142
        layout.setExpandRatio(browserPanel, 1);
//        layout.setExpandRatio(mPusher, 0);
        
//        setContent(layout);
//        setSizeFull();
    }

    private HorizontalSplitPanel createBrowserPanel()
    {
        VerticalSplitPanel contentPanel = createContentPanel();
        
        HorizontalSplitPanel browserPanel = new HorizontalSplitPanel();
        browserPanel.setSplitPosition(30);
        browserPanel.addComponent(mTreeView);
        browserPanel.addComponent(contentPanel);

        browserPanel.setSizeFull();
        return browserPanel;
    }

    private VerticalSplitPanel createContentPanel() {
        VerticalSplitPanel contentPanel = new VerticalSplitPanel();
        contentPanel.setSplitPosition(60);
        contentPanel.addComponent(mContentView);
        contentPanel.addComponent(mPreviewView);
//        contentPanel.setMargin(true);
        return contentPanel;
    }
    
    private Layout createToolbar() {
        GridLayout grid = new GridLayout(1, 1);
        grid.setWidth(100, Unit.PERCENTAGE);
        
        Button logoutBtn = new Button("Restart");
        logoutBtn.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent pEvent) {
                getUI().close();
            }
        });
        
        grid.addComponent(logoutBtn);
        grid.setComponentAlignment(logoutBtn, Alignment.TOP_RIGHT);
        
        return grid;
    }

    @Override
    public void detach() {
        super.detach();
        free();
    }
    
    public void free() {
        AlbumConstants.LOG.info("main free: " + this);
        if (mPreviewer != null) {
            mPreviewer.kill();
        }
        mTreeView.free();
        mContentView.free();
    }

    @Override
    public void showPreview(File pFile) {
        getPreviewer().start(pFile);
    }
    
    Refresher<File> getPreviewer() {
        if (mPreviewer == null) {
            mPreviewer = new Refresher<File>(100) {
                @Override
                protected Object construct(File pData)
                    throws Exception
                {
                    return ImageAccess.createThumbInfo(
                            pData, 
                            AlbumConstants.PREVIEW_SIZE.getInt());
                }

                @Override
                protected void finish(File pData, Object pConstructed)
                    throws Exception
                {
                    if (getUI() != null) {
                        final VaadinSession session = getUI().getSession();
                        session.lock();
                        try {
                            ThumbInfo info = (ThumbInfo)pConstructed;
                            String url = info.getThumbURL();
                            String img = "<img src=\"" + url + "\"/>";
                            mPreviewLabel.setValue(img);
                        } finally {
                            session.unlock();
                        }
                        mPusher.push();
                    }
                }

                @Override
                protected void failed(Exception pError, File pData) {
                    AlbumConstants.LOG.error("refresher failed: " + pData, pError);
                    
                    if (getUI() != null) {
                        final VaadinSession session = getUI().getSession();
                        session.lock();
                        try {
                            mPreviewView.setComponentError(new SystemError(pError));
                        } finally {
                            session.unlock();
                        }
    
                        mPusher.push();
                    }
                }
            };
        }
        return mPreviewer;
    }    

}
