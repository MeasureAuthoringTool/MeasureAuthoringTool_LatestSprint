package mat.client;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import mat.client.clause.cqlworkspace.CQLStandaloneWorkSpacePresenter;
import mat.client.clause.cqlworkspace.CQLStandaloneWorkSpaceView;
import mat.client.event.CQLLibrarySelectedEvent;
import mat.client.shared.ContentWithHeadingWidget;
import mat.client.shared.FocusableWidget;
import mat.client.shared.MatContext;
import mat.client.shared.MatTabLayoutPanel;
import mat.client.shared.SkipListBuilder;
import mat.client.shared.WarningConfirmationMessageAlert;
import mat.shared.ConstantMessages;


public class CqlComposerPresenter implements MatPresenter, Enableable, TabObserver {
	/**
	 * The Class EnterKeyDownHandler.
	 */
	class EnterKeyDownHandler implements KeyDownHandler {
		
		/** The i. */
		private int i = 0;
		
		/**
		 * Instantiates a new enter key down handler.
		 *
		 * @param index the index
		 */
		public EnterKeyDownHandler(int index) {
			i = index;
		}
		
		/* (non-Javadoc)
		 * @see com.google.gwt.event.dom.client.KeyDownHandler#onKeyDown(com.google.gwt.event.dom.client.KeyDownEvent)
		 */
		@Override
		public void onKeyDown(KeyDownEvent event) {
			if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
				cqlComposerTabLayout.selectTab(i);
			}
		}
	}
	
	MatTabLayoutPanel targetTabLayout;
	MatPresenter targetPresenter;
	MatPresenter sourcePresenter;
	HandlerRegistration yesHandler;
	HandlerRegistration noHandler;
	
	/** The sub skip content holder. */
	private static FocusableWidget subSkipContentHolder;
	
	/**
	 * Sets the sub skip embedded link.
	 *
	 * @param name the new sub skip embedded link
	 */
	public static void setSubSkipEmbeddedLink(String name) {
		if(subSkipContentHolder == null) {
			subSkipContentHolder = new FocusableWidget(SkipListBuilder.buildSkipList("Skip to Sub Content"));
		}
		Mat.removeInputBoxFromFocusPanel(subSkipContentHolder.getElement());
		Widget w = SkipListBuilder.buildSubSkipList(name);
		subSkipContentHolder.clear();
		subSkipContentHolder.add(w);
		subSkipContentHolder.setFocus(true);
	}
	
	/** The button bar. */
	//private PreviousContinueButtonBar buttonBar = new PreviousContinueButtonBar();
	
	/** The empty widget. */
	private SimplePanel emptyWidget = new SimplePanel();
	
	/** The measure composer content. */
	private static ContentWithHeadingWidget cqlComposerContent = new ContentWithHeadingWidget();
	
	/** The cql composer tab. */
	private String cqlComposerTab;
	
	/** The cql composer tab layout. */
	private MatTabLayoutPanel cqlComposerTabLayout;
	
	/** The meta data presenter. */
	private CQLStandaloneWorkSpacePresenter cqlStandaloneWorkSpacePresenter;
	
	/**
	 * Instantiates a new measure composer presenter.
	 */
	@SuppressWarnings("unchecked")
	public CqlComposerPresenter() {
		emptyWidget.getElement().setId("emptyWidget_SimplePanel");
		cqlStandaloneWorkSpacePresenter = (CQLStandaloneWorkSpacePresenter) buildCQLWorkSpaceTab();
		cqlComposerTabLayout = new MatTabLayoutPanel(this);
		cqlComposerTabLayout.getElement().setAttribute("id", "cqlComposerTabLayout");
		cqlComposerTabLayout.add(cqlStandaloneWorkSpacePresenter.getWidget(), "CQL Library Workspace");
		
		cqlComposerTabLayout.setHeight("98%");
		cqlComposerTab = ConstantMessages.CQL_COMPOSER_TAB;
		MatContext.get().tabRegistry.put(cqlComposerTab, cqlComposerTabLayout);
		MatContext.get().enableRegistry.put(cqlComposerTab, this);
		cqlComposerTabLayout.addSelectionHandler(new SelectionHandler<Integer>() {
			@Override
			@SuppressWarnings("rawtypes")
			public void onSelection(final SelectionEvent event) {
				int index = ((SelectionEvent<Integer>) event).getSelectedItem();
				String newToken = cqlComposerTab + index;
				if (!History.getToken().equals(newToken)) {
					CQLLibrarySelectedEvent cse = MatContext.get().getCurrentLibraryInfo();
					String msg = " [CQL] " + cse.getLibraryName() + " [version] " + cse.getCqlLibraryVersion();
					String cid = cse.getCqlLibraryId();
					MatContext.get().recordTransactionEvent(cid, null, "CQL_LIBRARY_TAB_EVENT", newToken + msg, ConstantMessages.DB_LOG);
					
					History.newItem(newToken, false);
				}
			} });
		
		cqlComposerContent.setContent(emptyWidget);
		
		
		
	}
	
	/* (non-Javadoc)
	 * @see mat.client.MatPresenter#beforeClosingDisplay()
	 */
	@Override
	public void beforeClosingDisplay() {
		notifyCurrentTabOfClosing();
		cqlComposerTabLayout.updateHeaderSelection(0);
		cqlComposerTabLayout.setSelectedIndex(0);
		
		/*if (MatContext.get().getCurrentLibraryInfo() != null) {
			MatContext.get().getCurrentLibraryInfo().setCqlLibraryId("");
		}*/
		
		MatContext.get().getLibraryLockService().releaseLibraryLock();
		Command waitForUnlock = new Command() {
			@Override
			public void execute() {
				if (!MatContext.get().getLibraryLockService().isResettingLock()) {
					notifyCurrentTabOfClosing();
					cqlComposerTabLayout.updateHeaderSelection(0);
					cqlComposerTabLayout.setSelectedIndex(0);
					/*buttonBar.state = cqlComposerTabLayout.getSelectedIndex();
					buttonBar.setPageNamesOnState();*/
				} else {
					DeferredCommand.addCommand(this);
				}
			}
		};
		if (MatContext.get().getLibraryLockService().isResettingLock()) {
			waitForUnlock.execute();
			if (MatContext.get().getCurrentLibraryInfo() != null) {
				MatContext.get().getCurrentLibraryInfo().setCqlLibraryId("");
			}
		} else {
			notifyCurrentTabOfClosing();
			cqlComposerTabLayout.updateHeaderSelection(0);
			cqlComposerTabLayout.setSelectedIndex(0);
			/*buttonBar.state = measureComposerTabLayout.getSelectedIndex();
			buttonBar.setPageNamesOnState();*/
			if (MatContext.get().getCurrentLibraryInfo() != null) {
				MatContext.get().getCurrentLibraryInfo().setCqlLibraryId("");
			}
		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see mat.client.MatPresenter#beforeDisplay()
	 */
	@Override
	public void beforeDisplay() {
		String currentLibraryId = MatContext.get().getCurrentCQLLibraryId();
		
		if ((currentLibraryId != null) && !"".equals(currentLibraryId)) {
			if (MatContext.get().isCurrentLibraryEditable()) {
				MatContext.get().getLibraryLockService().setLibraryLock();
			}

			setContentHeading();
			FlowPanel fp = new FlowPanel();
			fp.getElement().setId("fp_FlowPanel");
			setSubSkipEmbeddedLink("CQLStandaloneWorkSpaceView.containerPanel");
			fp.add(subSkipContentHolder);
			fp.add(cqlComposerTabLayout);
			cqlComposerContent.setContent(fp);
			cqlComposerTabLayout.selectTab(0);
			cqlStandaloneWorkSpacePresenter.beforeDisplay();
		} else {
			cqlComposerContent.setHeading("No Library Selected", "CqlComposer");
			cqlComposerContent.setContent(emptyWidget);
		}
		Mat.focusSkipLists("CqlComposer");
	}

	/**
	 * 
	 */
	public static void setContentHeading() {
		String heading = MatContext.get().getCurrentCQLLibraryeName() + " ";
		String version = MatContext.get().getCurrentCQLLibraryVersion();
		//when a library is initialy created we need to explictly create the heading
		if (!version.startsWith("Draft") && !version.startsWith("v")) {
			version = "Draft based on v" + version;
		}
		heading = heading + version;
		cqlComposerContent.setHeading(heading, "CqlComposer");
	}
	
	/**
	 * Builds the cql work space tab.
	 *
	 * @return the mat presenter
	 */
	private MatPresenter buildCQLWorkSpaceTab(){
		CQLStandaloneWorkSpaceView cqlView = new CQLStandaloneWorkSpaceView();
		CQLStandaloneWorkSpacePresenter cqlPresenter =
				new CQLStandaloneWorkSpacePresenter(cqlView);
		cqlPresenter.getWidget();
		return cqlPresenter;
	}
	
	/* (non-Javadoc)
	 * @see mat.client.MatPresenter#getWidget()
	 */
	@Override
	public Widget getWidget() {
		return cqlComposerContent;
	}
	
	/**
	 * implementing Enableable interface
	 * set enablement for navigation links and cql composer tabs
	 * consider setting enablement for each presenter and for skip links.
	 *
	 * @param enabled the new enabled
	 */
	@Override
	public void setEnabled(boolean enabled) {
		cqlComposerTabLayout.setEnabled(enabled);
	}
	
	private void showErrorMessageAlert(WarningConfirmationMessageAlert errorMessageDisplay) {
		errorMessageDisplay.createAlert();
		errorMessageDisplay.getWarningConfirmationYesButton().setFocus(true);
	}
	
	private void handleClickEventsOnUnsavedChangesMsg(final WarningConfirmationMessageAlert saveErrorMessage, final String auditMessage) {
		removeHandlers();
		yesHandler = saveErrorMessage.getWarningConfirmationYesButton().addClickHandler(event -> onYesButtonClicked(saveErrorMessage, auditMessage));
		noHandler = saveErrorMessage.getWarningConfirmationNoButton().addClickHandler(event -> onNoButtonClicked(saveErrorMessage));
	}
	
	private void onNoButtonClicked(final WarningConfirmationMessageAlert saveErrorMessage) {
		saveErrorMessage.clearAlert();
		resetTabTargets();
	}
	
	private void onYesButtonClicked(final WarningConfirmationMessageAlert saveErrorMessage, final String auditMessage) {
		if (auditMessage != null) {
			MatContext.get().recordTransactionEvent(MatContext.get().getCurrentMeasureId(),
					null, auditMessage, auditMessage, ConstantMessages.DB_LOG);
		};
		saveErrorMessage.clearAlert();
		notifyCurrentTabOfClosing();

		if(targetPresenter == null && targetTabLayout == null) {
			targetPresenter = cqlStandaloneWorkSpacePresenter;
			targetTabLayout = cqlComposerTabLayout;
		}

		targetTabLayout.setIndexFromTargetSelection();
		MatContext.get().setAriaHidden(targetPresenter.getWidget(),  "false");
		targetPresenter.beforeDisplay();

		if(sourcePresenter != null) {
			sourcePresenter.beforeClosingDisplay();
		}

		resetTabTargets();
	}

	@Override
	public boolean isValid() {
		boolean isValid = true;
		cqlStandaloneWorkSpacePresenter.getSearchDisplay().resetMessageDisplay();
		if (cqlStandaloneWorkSpacePresenter.getSearchDisplay().getCqlLeftNavBarPanelView().getIsPageDirty()) {
			isValid = false;
		}
		return isValid;
	}

	@Override
	public void updateOnBeforeSelection() {
		cqlStandaloneWorkSpacePresenter.beforeDisplay();	
	}

	@Override
	public void showUnsavedChangesError() {
		WarningConfirmationMessageAlert saveErrorMessageAlert = null;
		String auditMessage = null;
		saveErrorMessageAlert = cqlStandaloneWorkSpacePresenter.getSearchDisplay().getCqlLeftNavBarPanelView().getGlobalWarningConfirmationMessageAlert();
		if(saveErrorMessageAlert != null) {
			showErrorMessageAlert(saveErrorMessageAlert);
		}
		
		handleClickEventsOnUnsavedChangesMsg(saveErrorMessageAlert, auditMessage);
	}

	@Override
	public void notifyCurrentTabOfClosing() {
		MatContext.get().setAriaHidden(cqlStandaloneWorkSpacePresenter.getWidget(), "true");
		cqlStandaloneWorkSpacePresenter.beforeClosingDisplay();
	}
	
	private void removeHandlers() {
		if(yesHandler!=null) {
			yesHandler.removeHandler();
		}
		if(noHandler!=null) {	
			noHandler.removeHandler();
		}
	}
	
	private void resetTabTargets() {
		targetTabLayout = null;
		targetPresenter = null;
		sourcePresenter = null;
	}
	
	public void setTabTargets(MatTabLayoutPanel targetTabLayout, MatPresenter sourcePresenter, MatPresenter targetPresenter) {
		this.targetTabLayout = targetTabLayout;
		this.targetPresenter = targetPresenter;
		this.sourcePresenter = sourcePresenter;
	}
}
