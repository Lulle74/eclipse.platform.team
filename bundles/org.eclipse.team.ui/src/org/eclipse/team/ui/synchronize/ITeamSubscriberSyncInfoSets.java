package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSet;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;
import org.eclipse.ui.IWorkingSet;


public interface ITeamSubscriberSyncInfoSets {
	public abstract TeamSubscriberParticipant getParticipant();
	public abstract TeamSubscriber getSubscriber();
	public void reset() throws TeamException;
	public abstract SyncSet getFilteredSyncSet();
	public abstract SyncSet getSubscriberSyncSet();
	public abstract SyncSet getWorkingSetSyncSet();
	public abstract void setFilter(SyncInfoFilter filter, IProgressMonitor monitor) throws TeamException;
	public abstract void setWorkingSet(IWorkingSet set);
	public abstract IWorkingSet getWorkingSet();
	public abstract IResource[] workingSetRoots();
	public abstract IResource[] subscriberRoots();
	public abstract void registerListeners(ISyncSetChangedListener listener);
	public abstract void deregisterListeners(ISyncSetChangedListener listener);
}