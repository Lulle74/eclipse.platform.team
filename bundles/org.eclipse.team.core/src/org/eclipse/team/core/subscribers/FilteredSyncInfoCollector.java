/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.subscribers.SyncSetInputFromSyncSet;
import org.eclipse.team.internal.core.subscribers.WorkingSetSyncSetInput;

/**
 * Collects changes from a provided sync info set and creates another set based on 
 * the provided filters.
 * 
 * @see TeamSubscriberSyncInfoCollector
 * 
 * @since 3.0
 */
public final class FilteredSyncInfoCollector {

	private WorkingSetSyncSetInput workingSetInput;
	private SyncSetInputFromSyncSet filteredInput;
	private SyncInfoSet source;

	public FilteredSyncInfoCollector(SyncInfoSet source, IResource[] workingSet, SyncInfoFilter filter) {
		this.source = source;
		
		// TODO: optimize and don't use working set if no roots are passed in
		workingSetInput = new WorkingSetSyncSetInput(source);
		workingSetInput.setWorkingSet(workingSet);		
		filteredInput = new SyncSetInputFromSyncSet(workingSetInput.getSyncSet());
		if(filter == null) {
			setFilter(new SyncInfoFilter() {
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					return true;
				}
			}, null);
		} else {
			setFilter(filter, null);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.sets.SyncInfoSetDelegator#getSyncInfoSet()
	 */
	public SyncInfoSet getSyncInfoSet() {
		if(filteredInput != null) {
			return filteredInput.getSyncSet();
		} else {
			return workingSetInput.getSyncSet();
		}
	}
	
	public void setWorkingSet(IResource[] resources) {
		workingSetInput.setWorkingSet(resources);
	}
	
	public IResource[] getWorkingSet() {
		return workingSetInput.getWorkingSet();
	}
	
	public void setFilter(SyncInfoFilter filter, IProgressMonitor monitor) {
		filteredInput.setFilter(filter);
		try {
			filteredInput.reset(monitor);
		} catch (TeamException e) {
		}
	}
	
	public SyncInfoFilter getFilter() {
		if(filteredInput != null) {
			return filteredInput.getFilter();
		}
		return null;
	}
	
	public SyncInfoSet getWorkingSetSyncInfoSet() {
		return workingSetInput.getSyncSet();
	}
	
	public void dispose() {
		workingSetInput.disconnect();
		if(filteredInput != null) {
			filteredInput.disconnect();
		}
	}
}
