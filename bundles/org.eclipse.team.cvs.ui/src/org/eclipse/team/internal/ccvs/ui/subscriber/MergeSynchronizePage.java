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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.jface.action.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.actions.DirectionFilterActionGroup;
import org.eclipse.team.ui.synchronize.actions.RemoveSynchronizeParticipantAction;
import org.eclipse.ui.IActionBars;


public class MergeSynchronizePage extends CVSSynchronizeViewPage {

	private RemoveSynchronizeParticipantAction removeAction;
	private DirectionFilterActionGroup modes;
	private Action updateAdapter;
	
	public MergeSynchronizePage(TeamSubscriberParticipant participant, ISynchronizeView view, ITeamSubscriberSyncInfoSets input) {
		super(participant, view, input);		
		removeAction = new RemoveSynchronizeParticipantAction(getParticipant());
		modes = new DirectionFilterActionGroup(getParticipant(), TeamSubscriberParticipant.INCOMING_MODE | TeamSubscriberParticipant.CONFLICTING_MODE);
		updateAdapter = new CVSActionDelegate(new MergeUpdateAction());
		
		Utils.initAction(updateAdapter, "action.SynchronizeViewUpdate.", Policy.getBundle()); //$NON-NLS-1$
		getParticipant().setMode(TeamSubscriberParticipant.INCOMING_MODE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {		
		super.setActionBars(actionBars);
		if(actionBars != null) {
			IToolBarManager toolbar = actionBars.getToolBarManager();
			toolbar.add(new Separator());
			modes.fillToolBar(toolbar);
			toolbar.add(new Separator());
			toolbar.add(updateAdapter);
			toolbar.add(removeAction);
		}		
	}	
}