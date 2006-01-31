/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.ui.operations.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

public class RefreshModelParticipantJob extends RefreshParticipantJob {

	private final ResourceMapping[] mappings;

	public class ChangeDescription implements IChangeDescription, IDiffChangeListener {
		Map changes = new HashMap();
		
		public int getChangeCount() {
			return changes.size();
		}

		public void diffChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
			IDiff[] additions = event.getAdditions();
			for (int i = 0; i < additions.length; i++) {
				IDiff node = additions[i];
				changes.put(node.getPath(), node);
			}
			IDiff[] changed = event.getChanges();
			for (int i = 0; i < changed.length; i++) {
				IDiff node = changed[i];
				changes.put(node.getPath(), node);
			}
		}
		
		public void propertyChanged(int property, IPath[] paths) {
			// Do nothing
		}
	}
	
	public RefreshModelParticipantJob(ISynchronizeParticipant participant, String jobName, String taskName, ResourceMapping[] mappings, IRefreshSubscriberListener listener) {
		super(participant, jobName, taskName, listener);
		ISynchronizationContext context = ((ModelSynchronizeParticipant)getParticipant()).getContext();
		if (mappings.length == 0)
			this.mappings = context.getScope().getMappings();
		else 
			this.mappings = mappings;
	}

	protected void doRefresh(IChangeDescription changeListener,
			IProgressMonitor monitor) throws CoreException {
		ISynchronizationContext context = ((ModelSynchronizeParticipant)getParticipant()).getContext();
		try {
			context.getDiffTree().addDiffChangeListener((ChangeDescription)changeListener);
			// TODO: finer grained refresh
			context.refresh(mappings, monitor);
			// Wait for any asynchronous updating to complete
			try {
				Platform.getJobManager().join(context, monitor);
			} catch (InterruptedException e) {
				// Ignore
			}
		} finally {
			context.getDiffTree().removeDiffChangeListener((ChangeDescription)changeListener);
		}
	}

	protected int getChangeCount() {
		return ((ModelSynchronizeParticipant)getParticipant()).getContext().getDiffTree().size();
	}

	protected void handleProgressGroupSet(IProgressMonitor group) {
		// TODO Auto-generated method stub
	}

	protected IChangeDescription createChangeDescription() {
		return new ChangeDescription();
	}
	
	public boolean belongsTo(Object family) {
		if (family == ((ModelSynchronizeParticipant)getParticipant()))
			return true;
		return super.belongsTo(family);
	}

}