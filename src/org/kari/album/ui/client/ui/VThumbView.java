//package org.kari.album.ui.client.ui;
//
//import com.google.gwt.event.dom.client.ClickEvent;
//import com.google.gwt.event.dom.client.ClickHandler;
//import com.google.gwt.event.dom.client.MouseOverEvent;
//import com.google.gwt.event.dom.client.MouseOverHandler;
//import com.google.gwt.user.client.Event;
//import com.vaadin.terminal.gwt.client.ApplicationConnection;
//import com.vaadin.terminal.gwt.client.UIDL;
//import com.vaadin.terminal.gwt.client.ui.VEmbedded;
//
///**
// * Client side widget which communicates with the server. Messages from the
// * server are shown as HTML and mouse clicks are sent to the server.
// */
//public final class VThumbView
//    extends VEmbedded
//    implements
//        ClickHandler,
//        MouseOverHandler
//{
//	/** The client side widget identifier */
//    private String mPaintableId;
//
//	/** Reference to the server connection object. */
//    private ApplicationConnection mClient;
//
//	/**
//	 * The constructor should first call super() to initialize the component and
//	 * then handle any initialization relevant to Vaadin.
//	 */
//	public VThumbView() {
//		// Tell GWT we are interested in receiving click events
//		sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER);
//		
//		// Add a handler for the click events (this is similar to FocusWidget.addClickHandler())
//		addDomHandler(this, ClickEvent.getType());
//        addDomHandler(this, MouseOverEvent.getType());
//	}
//
//    /**
//     * Called whenever an update is received from the server 
//     */
//	@Override
//	public void updateFromUIDL(UIDL uidl, ApplicationConnection pClient) {
//	    super.updateFromUIDL(uidl, pClient);
//	    
//		// This call should be made first. 
//		// It handles sizes, captions, tooltips, etc. automatically.
//		if (pClient.updateComponent(this, uidl, true)) {
//		    // If client.updateComponent returns true there has been no changes and we
//		    // do not need to update anything.
//			return;
//		}
//
//		// Save reference to server connection object to be able to send
//		// user interaction later
//		mClient = pClient;
//
//		// Save the client side identifier (paintable id) for the widget
//		mPaintableId = uidl.getId();
//	}
//
//	@Override
//	public void onClick(ClickEvent event) {
//		mClient.updateVariable(mPaintableId, CLICK_EVENT_IDENTIFIER, "thumb", true);
//	}
//
//    @Override
//    public void onMouseOver(MouseOverEvent pEvent) {
//        mClient.updateVariable(mPaintableId, "mouseOver", true, true);
//    }
//     
//}
