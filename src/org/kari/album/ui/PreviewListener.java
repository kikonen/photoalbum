package org.kari.album.ui;

import java.io.File;

/**
 * Preview listener for image selection
 *
 * @author kari
 */
public interface PreviewListener {
    /**
     * @param pFile File for which preview is required
     */
    void showPreview(File pFile);
}
