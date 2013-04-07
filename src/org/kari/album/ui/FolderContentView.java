package org.kari.album.ui;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kari.album.AlbumConstants;
import org.kari.album.ui.ImageAccess.ThumbInfo;
import org.kari.util.concurrent.Refresher;
import org.vaadin.artur.icepush.ICEPush;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

/**
 * Window for displaying folder view
 * 
 * @author kari
 */
public final class FolderContentView extends VerticalLayout {
    /**
     * Cache fill for first screen full in worker thread
     */
    private static final int SCREEN_SIZE = 50;
    
    private static final String COL_IMAGE = "Image";
    private static final String COL_DETAILS = "Details";

    /**
     * Render File instances in table  
     */
    class ValueColumnGenerator
        implements
            Table.ColumnGenerator
    {
        @Override
        public Component generateCell(
            Table pSource,
            Object pElem,
            Object pColumnId)
        {
            Component result = null;
            if (pElem instanceof File) {
                File file = (File)pElem;
                ThumbInfo info = getThumbInfo(file);

                if (info != null) {
                    if (COL_IMAGE.equals(pColumnId)) {
                        try {
                            if (false) {
                                Embedded thumb = new Embedded(
                                        file.getName(),
                                        info.getResource());
                                thumb.setHeight(AlbumConstants.THUMB_SIZE.getInt() +"px");
                                result = thumb;
                            } else {
                                int size = AlbumConstants.THUMB_SIZE.getInt();
                                String url = info.getThumbURL();
                                result = new Label(
                                        "<img src=\"" + url + "\" height=\"" + size + "\"/>",
                                        Label.CONTENT_RAW);
                            }
                        } catch (Exception e) {
                            AlbumConstants.LOG.error("Failed to render: " + file, e);
                        }
                    } else if (COL_DETAILS.equals(pColumnId)) {
                        // TODO KI exif info
                        Object dt = info.getInfo("Exif", "Date/Time", true); 
                        Object w = info.getInfo("Exif", "Exif Image Width", false);
                        Object h = info.getInfo("Exif", "Exif Image Height", false);
                        Object iso = info.getInfo("Exif", "ISO Speed Ratings", true);
                        Object flash = info.getInfo("Exif", "Flash", true);
                        Object aperture = info.getInfo("Exif", "Aperture Value", true);
                        Object exposure = info.getInfo("Exif", "Exposure Time", true);
                        Object exposureBias = info.getInfo("Exif", "Exposure Bias Value", true);
                        
                        if (dt == null) {
                            dt = new Date(file.lastModified());
                        }

                        String url = ImageAccess.createURL(file);

                        
                        StringBuilder sb = new StringBuilder(100);
                        
                        // Line 1
                        sb.append("<b><a href=\"");
                        sb.append(url);
                        sb.append("\" ");
                        sb.append("target=\"_blank\"");
                        sb.append(">");
                        sb.append(file.getName());
                        sb.append("</a></b><br>");

                        // Line 2
                        if (dt != null) {
                            sb.append(dt);
                            sb.append("<br>");
                        }
                        
                        // Line 3
                        if (w != null && h != null) {
                            sb.append(w);
                            sb.append(" x ");
                            sb.append(h);
                            
                            sb.append(" (");
                            // Line 6
                            sb.append(file.length() / 1000.0);
                            sb.append(" KB");
                            sb.append(")");
                            sb.append("<br>");
                        }

                        // Line 4
                        {
                            if (iso != null) {
                                sb.append("ISO ");
                                sb.append(iso);
                            }
    
                            if (aperture != null) {
                                sb.append(" <b>");
                                sb.append(aperture);
                                sb.append("</b>");
                            }

                            if (exposure != null) {
                                sb.append(" ");
                                sb.append(exposure);
                            }

                            if (exposureBias != null) {
                                sb.append(" ");
                                sb.append(exposureBias);
                            }

                            // Line 5
                            boolean showFlash = flash != null && flash.toString().indexOf("Flash fired") != -1;
                            if (showFlash) {
                                sb.append(" (");
                                sb.append(flash);
                                sb.append(")");
                            }
//
//                            if (iso != null || aperture != null || exposure != null || showFlash) {
//                                sb.append("<br>");
//                            }
                        }
                        

                        result = new Label(
                                sb.toString(),
                                Label.CONTENT_RAW);
                    }
                } else {
                    if (COL_IMAGE.equals(pColumnId)) {
                        result = new Label("error");
                    }
                }
            } else if (pElem != null) {
                if (COL_IMAGE.equals(pColumnId)) {
                    result = new Label(pElem.toString());
                }
            }
            
            return result;
        }
    }

    /**
     * Dir for which folder view is shown
     */
    private File mDir;
    Table mTable;
    Label mInfo;
    PreviewListener mPreviewListener;
    
    static final Map<File, SoftReference<ThumbInfo>> mGlobalCache = new HashMap<File, SoftReference<ThumbInfo>>();
    static int mGlobalHits;
    static int mGlobalMisses;
    static int mGlobalFlushes;
    
    Map<File, ThumbInfo> mThumbnails = new HashMap<File, ThumbInfo>(); 

    
    final ICEPush mPusher;
    /**
     * Async retrieval of folder contents
     */
    private transient Refresher<File> mFolderChanger;
    
    public FolderContentView(
        ICEPush pPusher,
        PreviewListener pPreviewListener)
    {
        mInfo = new Label();
        mTable = createTable();
        mPreviewListener = pPreviewListener;
        mPusher = pPusher;
        
        setSizeFull();

        addComponent(mTable);
        addComponent(mInfo);

        // @see http://vaadin.com/forum/-/message_boards/message/197142
        setExpandRatio(mTable, 1);
        
        setSizeFull();
    }
    
    @Override
    public void detach() {
        free();
        super.detach();
    }

    public void free() {
        AlbumConstants.LOG.info("content free: " + this);
        if (mFolderChanger != null) {
            mFolderChanger.kill();
        }
    }

    private Table createTable() {
        Table table = new Table();
        table.setWidth("100%");
        table.setHeight("100%");
        table.setPageLength(10);
        
        table.setSelectable(true);
        table.setImmediate(true);
        table.setCacheRate(1.5);
        
        table.addContainerProperty(COL_IMAGE, String.class, null);
        table.addContainerProperty("Detail", String.class, null);
        
        table.setColumnHeaders(new String[] {
            COL_IMAGE,
            COL_DETAILS
        });
        
        table.setColumnWidth(COL_IMAGE, 100);
        
        ValueColumnGenerator generator = new ValueColumnGenerator();
        table.addGeneratedColumn(COL_IMAGE, generator);
        table.addGeneratedColumn(COL_DETAILS, generator);
        
        table.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent pEvent) {
                Object value = pEvent.getProperty().getValue();
                if (value != null) {
                    mPreviewListener.showPreview((File)value);
                }
            }
        });
        
        return table;
    }

    /**
     * @return path to folder, null if not set
     */
    public File getDir() {
        return mDir;
    }

    public void setDir(File pDir) {
        if (!pDir.equals(mDir)) { 
            mDir = pDir;
            
            AlbumConstants.LOG.info("Loading dir: " + mDir);

            String size = "?";
            if (containsFiles(mDir)) {
                getFolderChanger().start(mDir);
                
                IndexedContainer ds = new IndexedContainer();
                ds.addItem("wait...");
                mTable.setContainerDataSource(ds);
            } else {
                size = "0";
                
                IndexedContainer ds = new IndexedContainer();
                ds.addItem("<Empty Folder>");
                mTable.setContainerDataSource(ds);
            }
            
            mInfo.setValue(mDir.getName() +": <b>" + size +"</b> photos");
            mInfo.setContentMode(Label.CONTENT_RAW);
        }
    }

    /**
     * Async folder filling
     * 
     * @param pCache cache prefilled if pFillCache is used
     */
    IndexedContainer createContainer(
        File pDir,
        final Map<File, ThumbInfo> pCache,
        boolean pFillCache) 
    {
        List data;
        if (pDir != null) {
            File[] files = pDir.listFiles();
            if (files != null) {
                data = new ArrayList();
                for (File file : files) {
                    if (!file.isDirectory() && ImageAccess.isSupported(file)) {
                        data.add(file);
                    }
                }
                
                Collections.sort(data);
            } else {
                data = Collections.singletonList("<Empty Folder>");
            }
        } else {
            data = Collections.singletonList("<Folder Missing>");
        }
        
        // prefill first items (for initial display)
        if (pFillCache) {
            for (int i = 0; i < SCREEN_SIZE && i < data.size(); i++) {
                Object elem = data.get(i);
                if (elem instanceof File) {
                    getThumbInfo(pCache, (File)elem);
                }
            }
        }
        return new IndexedContainer(data);
    }
    
    private boolean containsFiles(File pDir) {
        boolean result = false;
        if (pDir != null) {
            File[] files = pDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isDirectory() && ImageAccess.isSupported(file)) {
                        result = true;
                        break;
                    }
                }
            }
        }        
        return result;
    }
    
    /**
     * <p>NOTE KI valid only from VAADIN thread
     */
    public ThumbInfo getThumbInfo(File pFile) {
        return getThumbInfo(mThumbnails, pFile);
    }
    
    /**
     * @param pCache Cached used
     * 
     * @return null if no info available
     */
    public ThumbInfo getThumbInfo(final Map<File, ThumbInfo> pCache, File pFile) {
        ThumbInfo info = null;

        synchronized (mGlobalCache) {
            SoftReference<ThumbInfo> cached = mGlobalCache.get(pFile);
            if (cached != null) {
                info = cached.get();
                if (info != null) {
                    mGlobalHits++;
                } else {
                    mGlobalFlushes++;
                }
            } else {
                mGlobalMisses++;
            }
        }
        
        if (info == null) {
            if (pCache.containsKey(pFile)) {
                info = pCache.get(pFile);
            } else {
                try {
                    info = ImageAccess.createThumbInfo(
                        pFile,
                        AlbumConstants.THUMB_SIZE.getInt());
                } catch (Exception e) {
                    AlbumConstants.LOG.error("Failed to render: " + pFile, e);
                }
                pCache.put(pFile, info);
            }
            
            synchronized (mGlobalCache) {
                mGlobalCache.put(pFile, new SoftReference<ThumbInfo>(info));
            }
        } else {
            pCache.put(pFile, info);
        }
        
        return info;
    }
    
    
    
    Refresher<File> getFolderChanger() {
        if (mFolderChanger == null) {
            mFolderChanger = new Refresher<File>(100) {
                @Override
                protected void finish(File pData, Object pConstructed)
                    throws Exception
                {
                    AlbumConstants.LOG.info("changing content: " + pData
                            + ", GLOBAL: "
                            + ", hits=" + mGlobalHits
                            + ", flushes=" + mGlobalFlushes
                            + ", misses=" + mGlobalMisses);
                    try {
                        final Map<File, ThumbInfo> cache = new HashMap<File, ThumbInfo>();
                        IndexedContainer ds = createContainer(pData, cache, true);

                        if (getUI() != null) {
                            final VaadinSession session = getUI().getSession();
                            session.lock();
                            try {
                                mThumbnails = cache;
                                mInfo.setValue(pData.getName() +": <b>" + ds.size() + "</b> photos");
                                mInfo.setContentMode(Label.CONTENT_RAW);
                                mTable.setContainerDataSource(ds);
                            } finally {
                                session.unlock();
                            }
                        }
                    } catch (Throwable e) {
                        AlbumConstants.LOG.error("Failed: " + pData, e);
                    }
                    
                    mPusher.push();
                }
            };
        }
        return mFolderChanger;
    }    

}
