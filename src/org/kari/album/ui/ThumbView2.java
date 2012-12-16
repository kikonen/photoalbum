package org.kari.album.ui;

import com.vaadin.server.Resource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Embedded;

/**
 * Thumbnail view (no mouseover)
 * 
 * @author kari
 */
public class ThumbView2 extends CustomComponent {
    /**
     * Show preview for thumb
     */
    public interface PreviewListener2 {
        void showPreview(ThumbView2 pThumb);
    }

    private Embedded mThumb;
    private Resource mPreviewSource;
    
    public ThumbView2(
        String pCaption, 
        Resource pSource,
        Resource pPreviewSource)
    {
        mPreviewSource = pPreviewSource;

        mThumb = new Embedded(pCaption, pSource);
        mThumb.setWidth("120px");
        mThumb.setHeight("120px");

        setSizeUndefined();
        setCompositionRoot(mThumb);
    }

    public Resource getPreviewSource() {
        return mPreviewSource;
    }

}
