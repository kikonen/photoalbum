//package org.kari.album.ui;
//
//import java.util.Map;
//
//import com.vaadin.terminal.PaintException;
//import com.vaadin.terminal.PaintTarget;
//import com.vaadin.terminal.Resource;
//import com.vaadin.ui.Embedded;
//
///**
// * Thumb viewer
// * 
// * @author kari
// */
//@com.vaadin.ui.ClientWidget(org.kari.album.ui.client.ui.VThumbView.class)
//public final class ThumbView
//    extends Embedded
//{
//    /**
//     * Show preview for thumb
//     */
//    public interface PreviewListener {
//        void showPreview(ThumbView pThumb);
//    }
//    
//    private Resource mPreviewSource;
//    private PreviewListener mListener;
//    
//    public ThumbView(
//        String pCaption, 
//        Resource pSource,
//        Resource pPreviewSource,
//        PreviewListener pListener)
//    {
//        super(pCaption, pSource);
//        mPreviewSource = pPreviewSource;
//        mListener = pListener;
//        
//        setWidth("120px");
//        setHeight("120px");
//    }
//
//    @Override
//    public void paintContent(PaintTarget target)
//        throws PaintException
//    {
//        super.paintContent(target);
//    }
//
//    /**
//     * Receive and handle events and other variable changes from the client.
//     * 
//     * {@inheritDoc}
//     */
//    @Override
//    public void changeVariables(Object source, Map<String, Object> variables) {
//        super.changeVariables(source, variables);
//
//        if (variables.containsKey("click")) {
////            requestRepaint();
//        } else if (variables.containsKey("mouseOver")) {
//            mListener.showPreview(this);
//        }
//    }
//
//    public Resource getPreviewSource() {
//        return mPreviewSource;
//    }
//
//}
