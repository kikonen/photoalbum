package org.kari.album.ui;

import java.io.File;

import org.kari.album.AlbumConstants;
import org.kari.util.concurrent.Refresher;
import org.vaadin.artur.icepush.ICEPush;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

/**
 * Folder hierarchy
 *
 * @author kari
 */
public final class FolderTreeView extends VerticalLayout 
    implements
        ValueChangeListener, 
        ClickListener, 
        Handler
{
    private File mRootDir;
    
    private final Tree mTree;
    final FolderContentView mContentView;
    
    final ICEPush mPusher;
    private transient Refresher<File> mFolderChanger;

    
    public FolderTreeView(
            ICEPush pPusher,
            FolderContentView pFolderView) 
    {
        mPusher = pPusher;
        mContentView = pFolderView;
        mTree = createTree();

        // @see http://vaadin.com/forum/-/message_boards/message/197142
        Panel panel = new Panel();
//        panel.setScrollable(true);
        panel.setContent(mTree);
        panel.setSizeFull();
        
//        mTree.setMargin(false);

        // Add panel to layout
        setMargin(false);
//        setMargin(false, false, false, false);
        setSizeFull(); 
        addComponent(panel);
    }

    @Override
    public void detach() {
        free();
        super.detach();
    }

    public void free() {
        AlbumConstants.LOG.info("tree free: " + this);
        if (mFolderChanger != null) {
            mFolderChanger.kill();
        }
    }
    
    /**
     * <p>NOTE KI height must be undefined for scrolling
     */
    private Tree createTree() {
        Tree tree = new Tree();
        tree.setItemCaptionMode(AbstractSelect.ITEM_CAPTION_MODE_PROPERTY);
        tree.setImmediate(true);
        tree.setSelectable(true);

        // Set tree to show the 'name' property as caption for items
        tree.setItemCaptionPropertyId(FolderHierarchicalContainer.PROP_NAME);
//        tree.setItemIconPropertyId(FolderHierarchicalContainer.PROP_ICON);

        tree.addListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent pEvent) {
                Object value = pEvent.getProperty().getValue();
                File dir = (File)value;
                if (dir == null) {
                    dir = mRootDir;
                }
                
                Refresher<File> changer = getFolderChanger();
                changer.start(dir);
            }
        });
        return tree;
    }
    
    /**
     * @return root dir, null if not set
     */
    public File getRootDir() {
        return mRootDir;
    }

    public void setRootDir(File pRootDir) {
        mRootDir = pRootDir;
        mTree.setContainerDataSource(createContainer());

        // Expand root
        for (Object id : mTree.rootItemIds()) {
            mTree.expandItem(id);
        }
        
//        
//        // Initially show root dir; NOT
////        mContentView.setDir(mRootDir);
    }

    public void showDir(File pDir) {
        // TODO KI select dir; which triggers display of content
        mContentView.setDir(pDir);
        mTree.select(pDir);
    }
    
    private IndexedContainer createContainer() {
//        return ProtoUtil.getHardwareContainer();
        return new FolderHierarchicalContainer(mRootDir);
    }

    @Override
    public Action[] getActions(Object pTarget, Object pSender) {
        return null;
    }

    @Override
    public void handleAction(Action pAction, Object pSender, Object pTarget) {
        Notification.show("actopm: " + pAction);
        
    }

    @Override
    public void buttonClick(ClickEvent pEvent) {
        Notification.show("click: " + pEvent);
    }

    @Override
    public void valueChange(ValueChangeEvent pEvent) {
        Notification.show("selected: " + pEvent);
    }

    Refresher<File> getFolderChanger() {
        if (mFolderChanger == null) {
            mFolderChanger = new Refresher<File>(150) {
                @Override
                protected void finish(File pData, Object pConstructed)
                    throws Exception
                {
                    if (getUI() != null) {
                        final VaadinSession session = getUI().getSession();
                        session.lock();
                        try {
                            mContentView.setDir(pData);
                        } finally {
                            session.unlock();
                        }
                        mPusher.push();
                    }
                }
            };
        }
        return mFolderChanger;
    }    
    
}
