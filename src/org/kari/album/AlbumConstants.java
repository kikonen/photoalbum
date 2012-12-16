package org.kari.album;

import org.apache.log4j.Logger;
import org.kari.util.config.IntValue;
import org.kari.util.config.PathValue;
import org.kari.util.config.TextValue;
import org.kari.util.log.LogUtil;

/**
 * Shared constants for album
 * 
 * @author kari
 */
public interface AlbumConstants {
    String CONFIG_FILE = "/album.properties";
    
    TextValue ROOT_URL = new TextValue(
            "album.root.url", 
            "http://kari.dy.fi/album");
    
    /**
     * Absolute path to root of ALBUM in local filesystem
     * 
     * <p>WAS: "/home/kari/data/album"
     */
    PathValue ALBUM_PATH = new PathValue(
            "album.root.path",
            null,
            null);
    
    PathValue CACHE_PATH = new PathValue(
            "album.cache.path",
            ALBUM_PATH,
            "thumbnail");

    PathValue DISPLAY_PATH = new PathValue(
            "album.display.path",
            ALBUM_PATH,
            "Timeline");

    IntValue THUMB_SIZE = new IntValue(
            "album.thumb.size",
            64);

    IntValue PREVIEW_SIZE = new IntValue(
            "album.preview.size",
            400);

    Logger LOG = LogUtil.getLogger("album");
    
    String VERSION = "0.0.1";
}
