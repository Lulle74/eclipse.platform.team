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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.OverrideAndUpdateOperation;
import org.eclipse.ui.IWorkbenchPart;

public class OverrideAndUpdateSubscriberOperation extends CVSSubscriberOperation {
	protected OverrideAndUpdateSubscriberOperation(IWorkbenchPart part, IDiffElement[] elements) {
		super(part, elements);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberOperation#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		SyncInfoSet syncSet = getSyncInfoSet();
		if(promptForOverwrite(syncSet)) {
			super.run(monitor);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberOperation#run(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void run(SyncInfoSet set, IProgressMonitor monitor) throws TeamException {
		try {
			SyncInfo[] conflicts = set.getNodes(getConflictingAdditionFilter());
			List conflictingResources = new ArrayList();
			for (int i = 0; i < conflicts.length; i++) {
				SyncInfo info = conflicts[i];
				conflictingResources.add(info.getLocal());
			}
			new OverrideAndUpdateOperation(getPart(), set.getResources(), (IResource[]) conflictingResources.toArray(new IResource[conflictingResources.size()]), null /* tag */, false /* recurse */).run(monitor);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Policy.cancelOperation();
		}
	}
	private FastSyncInfoFilter getConflictingAdditionFilter() {
		return new FastSyncInfoFilter.AndSyncInfoFilter(
			new FastSyncInfoFilter[] {
				new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] {SyncInfo.CONFLICTING}), 
				new FastSyncInfoFilter.SyncInfoChangeTypeFilter(new int[] {SyncInfo.ADDITION})
			});
	}
}