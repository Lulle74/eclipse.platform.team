/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.subscriber.MergeSynchronizeParticipant;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;

/**
 * SyncInfoSource that obtains SyncInfo from the SynchronizeView's SyncSet.
 */
public class SynchronizeViewTestAdapter extends SyncInfoSource {

	public SynchronizeViewTestAdapter() {
		TeamUI.getSynchronizeManager().showSynchronizeViewInActivePage(null);
	}
	
	public SyncInfo getSyncInfo(TeamSubscriber subscriber, IResource resource) throws TeamException {
		SyncInfoSet set = getCollector(subscriber).getSyncInfoSet();
		SyncInfo info = set.getSyncInfo(resource);
		if (info == null) {
			info = subscriber.getSyncInfo(resource);
			if ((info != null && info.getKind() != SyncInfo.IN_SYNC)) {
				throw new AssertionFailedError();
			}
		}
		return info;
	}
	
	private TeamSubscriberParticipant getParticipant(TeamSubscriber subscriber) {
		// show the sync view
		ISynchronizeParticipant[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			if(participant instanceof TeamSubscriberParticipant) {
				if(((TeamSubscriberParticipant)participant).getSubscriber() == subscriber) {
					return (TeamSubscriberParticipant)participant;
				}
			}
		}
		return null;
	}
	
	private TeamSubscriberSyncInfoCollector getCollector(TeamSubscriber subscriber) {
		TeamSubscriberParticipant participant = getParticipant(subscriber);
		if (participant == null) return null;
		TeamSubscriberSyncInfoCollector syncInfoCollector = participant.getSyncInfoCollector();
		EclipseTest.waitForSubscriberInputHandling(syncInfoCollector);
		return syncInfoCollector;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#assertProjectRemoved(org.eclipse.team.core.subscribers.TeamSubscriber, org.eclipse.core.resources.IProject)
	 */
	protected void assertProjectRemoved(TeamSubscriber subscriber, IProject project) throws TeamException {		
		super.assertProjectRemoved(subscriber, project);
		SyncInfoSet set = getParticipant(subscriber).getSyncInfoCollector().getSyncInfoSet();
		if (set.getOutOfSyncDescendants(project).length != 0) {
			throw new AssertionFailedError("The sync set still contains resources from the deleted project " + project.getName());	
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#createMergeSubscriber(org.eclipse.core.resources.IProject, org.eclipse.team.internal.ccvs.core.CVSTag, org.eclipse.team.internal.ccvs.core.CVSTag)
	 */
	public CVSMergeSubscriber createMergeSubscriber(IProject project, CVSTag root, CVSTag branch) {
		CVSMergeSubscriber mergeSubscriber = super.createMergeSubscriber(project, root, branch);
		ISynchronizeManager synchronizeManager = TeamUI.getSynchronizeManager();
		ISynchronizeParticipant participant = new MergeSynchronizeParticipant(mergeSubscriber);
		synchronizeManager.addSynchronizeParticipants(
				new ISynchronizeParticipant[] {participant});		
		ISynchronizeView view = synchronizeManager.showSynchronizeViewInActivePage(null);
		view.display(participant);
		return mergeSubscriber;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#tearDown()
	 */
	public void tearDown() {
		ISynchronizeParticipant[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			if(participant.getId().equals(CVSMergeSubscriber.QUALIFIED_NAME)) {
				TeamUI.getSynchronizeManager().removeSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			}
		}
		// Process all async events that may have been generated above
		while (Display.getCurrent().readAndDispatch()) {};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#refresh(org.eclipse.team.core.subscribers.TeamSubscriber, org.eclipse.core.resources.IResource)
	 */
	public void refresh(TeamSubscriber subscriber, IResource resource) throws TeamException {
		super.refresh(subscriber, resource);
		EclipseTest.waitForSubscriberInputHandling(getCollector(subscriber));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#reset()
	 */
	public void reset(TeamSubscriber subscriber) throws TeamException {
		super.reset(subscriber);
		getCollector(subscriber).reset(DEFAULT_MONITOR);
	}
}
