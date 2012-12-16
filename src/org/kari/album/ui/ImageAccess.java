package org.kari.album.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;

import org.kari.album.AlbumConstants;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.vaadin.server.ExternalResource;

/**
 * Image access utilities
 *
 * @author kari
 */
public class ImageAccess {
    /**
     * Key for metadata tag cache
     * @author kari
     */
    static class TagKey {
        public final String mDir;
        public final String mTagName;
        public TagKey(String pDir, String pTagName) {
            super();
            mDir = pDir;
            mTagName = pTagName;
        }
        @Override
        public int hashCode() {
            return mDir.hashCode() ^ mTagName.hashCode();
        }
        @Override
        public boolean equals(Object pObj) {
            TagKey b = (TagKey)pObj;
            return mDir.equals(b.mDir)
                && mTagName.equals(b.mTagName);
        }
    }
    
    /**
     * Resources for thumbnail
     *
     * @author kari
     */
    public static class ThumbInfo {
        private final File mFile;
        private final File mThumbFile;
        private final String mThumbURL;
        private Metadata mMetadata;
        private final Map<TagKey, Object> mCachedValues = new HashMap<TagKey, Object>(3);
        private final boolean mLazyMeta;
        private boolean mLoadedMeta;

        private ExternalResource mResource;

        /**
         * @param pMetadata Metadata, can be null
         * @param pLazyMeta If true metadata needs to be loaded lazily on-demand
         */
        public ThumbInfo(
            File pFile,
            File pThumbFile,
            Metadata pMetadata,
            boolean pLazyMeta)
        {
            mFile = pFile;
            mThumbFile = pThumbFile;
            mThumbURL = createURL(pThumbFile);
            mMetadata = pMetadata;
            mLazyMeta = pLazyMeta;
        }
        
        public File getThumbFile() {
            return mThumbFile;
        }
        
        public String getThumbURL() {
            return mThumbURL;
        }

        public synchronized ExternalResource getResource() {
            ExternalResource res = mResource;
            if (res == null) {
                res = new ExternalResource(mThumbURL);
                mResource = res;
            }
            return res;
        }
        
        public synchronized Metadata getMetadata() {
            if (mLazyMeta && !mLoadedMeta && mMetadata == null) {
                try {
                    mMetadata = JpegMetadataReader.readMetadata(mFile);
                } catch (Exception e) {
                    AlbumConstants.LOG.warn("Failed to read info: " + mFile, e);
                } finally {
                    mLoadedMeta = true;
                }
            }
            return mMetadata;
        }
        
        /**
         * @param pDesc If true desc instead of value
         * @return Info, null if not found
         * 
         */
        public Object getInfo(String pDir, String pTagName, boolean pDesc) {
            Object result = null;
            Metadata meta = getMetadata();
            if (meta != null) {
                TagKey key = new TagKey(pDir, pTagName);
                
                if (mCachedValues.containsKey(key)) {
                    result = mCachedValues.get(key);
                } else {
                    Directory dir = getDir(pDir);
                    if (dir != null) {
                        try {
                            // iterate through tags and print to System.out
                            Iterator<Tag> tagIter = dir.getTags().iterator();
                            while (result == null && tagIter.hasNext()) {
                                Tag tag = tagIter.next();
                                String tagName = tag.getTagName();
                                if (pTagName.equals(tagName)) {
                                    if (pDesc) {
                                        result = tag.getDescription();
                                    } else {
                                        result = dir.getObject(tag.getTagType());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            AlbumConstants.LOG.warn("Corrupted meta: " + mFile, e);
                        }
                    }
                    mCachedValues.put(key, result);
                }
            }
            return result;
        }

        /**
         * @return Dir, null if not found
         */
        public Directory getDir(String pDir) {
            Metadata meta = getMetadata();
            if (meta != null) {
                Iterator<Directory> dirIter = meta.getDirectories().iterator();
                while (dirIter.hasNext()) {
                    Directory directory = dirIter.next();
                    if (directory.getName().equals(pDir)) {
                        return directory;
                    }
                }
            }
            return null;
        }
        
        public void dumpInfo() {
            Metadata meta = getMetadata();
            if (meta != null) {
                Iterator<Directory> dirIter = meta.getDirectories().iterator();
                while (dirIter.hasNext()) {
                    Directory dir = dirIter.next();
                    // iterate through tags and print to System.out
                    Iterator<Tag> tagIter = dir.getTags().iterator();
                    while (tagIter.hasNext()) {
                        Tag tag = tagIter.next();
                        try {
                            AlbumConstants.LOG.info(
                                    "[" + dir.getName() + "] \""
                                    + tag.getTagName()
                                    + "\" ["
                                    + dir.getObject(tag.getTagType())
                                    + "] desc ["
                                    + tag.getDescription()
                                    + "]");
                        } catch (Exception e) {
                            AlbumConstants.LOG.error("Failed: " + tag, e);
                        }
                    }
                }
            }
        }
    }

    
    /**
     * @return true if file is supported
     */
    public static boolean isSupported(File pFile) {
        String fileName = pFile.getName().toLowerCase();
        return fileName.endsWith(".jpg")
            || fileName.endsWith(".png");
    }

    public static ThumbInfo createThumbInfo(
            final File pFile,
            final int pMaxSize)
        throws
            JpegProcessingException,
            MetadataException,
            IOException
    {
        Metadata metadata = null; 
        boolean lazymeta = false;
        
        File cacheFile = createCacheFile(pFile, pMaxSize);
        AlbumConstants.LOG.info("cache-file:" + cacheFile + ", exists=" + cacheFile.exists());
        if (!cacheFile.exists()) {
            metadata = JpegMetadataReader.readMetadata(pFile);
    
            // Find thumbnail
            byte[] data = null;
            
            // iterate through metadata directories
            Iterator<Directory> dirIter = metadata.getDirectories().iterator();
            while (data == null && dirIter.hasNext()) {
                Directory directory = dirIter.next();
                // iterate through tags and print to System.out
                Iterator<Tag> tagIter = directory.getTags().iterator();
                while (data == null && tagIter.hasNext()) {
                    Tag tag = tagIter.next();
                    String tagName = tag.getTagName();
                    if ("Thumbnail Data".equals(tagName)) {
                        data = directory.getByteArray(tag.getTagType());
                    }
                }
            }
            
            if (data != null) {
                makeDir(cacheFile.getParentFile());
                FileOutputStream out = new FileOutputStream(cacheFile);
                writeFile(
                        new ByteArrayInputStream(data),
                        out);
                out.close();
            } else {
                // No thumbnail; must create one
                cacheFile = createResource(
                        pFile, 
                        pMaxSize);
            }    
        } else {
            lazymeta = true;
        }
        
        return new ThumbInfo(pFile, cacheFile, metadata, lazymeta);
    }
    
    /**
     * @param pMaxSize Max width/height allowed for image
     * @return cache file
     */
    public static File createResource(
            final File pFile,
            final int pMaxSize)
        throws
            IOException
    {
        File cacheFile = createCacheFile(pFile, pMaxSize);
        if (!cacheFile.exists()) {
            BufferedImage origImage = ImageIO.read(pFile);
            int origW = origImage.getWidth();
            int origH = origImage.getHeight();
            
            if (origW > pMaxSize || origH > pMaxSize) {
                int max = origW > origH
                    ? origW
                    : origH;

                double scale = max / (double)pMaxSize;
                int destW = (int)(origW / scale);
                int destH = (int)(origH / scale);
                if (destW > origW) {
                    destW = origW;
                    destH = origH;
                }
                
                BufferedImage scaled = new BufferedImage(
                        destW, 
                        destH,
                        origImage.getType());
                Graphics2D g = scaled.createGraphics();
                
                g.setRenderingHint(
                        RenderingHints.KEY_RENDERING, 
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION, 
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                
                g.drawImage(
                        origImage, 
                        0, 0, destW, destH, 
                        0, 0, origW, origH, 
                        null);
                
                makeDir(cacheFile.getParentFile());
                FileImageOutputStream out = new FileImageOutputStream(cacheFile);
                ImageIO.write(scaled, 
                        "jpeg", 
                        out);
                out.close();
            } else {
                cacheFile = pFile;
            }
        }
        
        return cacheFile;
    }
    
    public static String createURL(final File pFile) {
        final String rootPath = AlbumConstants.ALBUM_PATH.getPath().getAbsolutePath();
        
        final String path = pFile.getParentFile().getAbsolutePath().replace('\\',  '/');
        final String relativePath = path.substring(rootPath.length() + 1);
        final String fileName = pFile.getName();
        
        String url;
        url = AlbumConstants.ROOT_URL.getText() 
            + "/"
            + relativePath 
            + "/" 
            + fileName;
        AlbumConstants.LOG.info("create-url: " + url);
        return url;
    }
    
    /**
     * @return Filename for cache file
     */
    private static File createCacheFile(
        final File pFile,
        final int pSize) 
    {
        final String rootPath = AlbumConstants.ALBUM_PATH.getPath().getAbsolutePath();
        
        final String path = pFile.getParentFile().getAbsolutePath();
        final String relativePath = path.substring(rootPath.length());
        final String fileName = pFile.getName();
        
        final int extIdx = fileName.indexOf(".");
        final String fileExt = fileName.substring(extIdx);
        final String fileBase = fileName.substring(0, extIdx);
        
        String cacheFile;
        cacheFile = AlbumConstants.CACHE_PATH.getPath() 
            + relativePath 
            + "/" 
            + fileBase 
            + "-thumb-"
            + pSize
            + fileExt;
        
        return new File(cacheFile);
    }

    
    public static void writeFile(
            InputStream pInput,
            OutputStream pOutput)
        throws IOException
    {
        byte[] buffer = new byte[4096];
        int count;
        while ( (count = pInput.read(buffer)) != -1) {
            pOutput.write(buffer, 0, count);
        }
    }
    
    /**
     * Make pDir (and missing parent directories)
     */
    public static void makeDir(
            File pDir)
        throws IOException
    {
        pDir.mkdirs();
    }
}
