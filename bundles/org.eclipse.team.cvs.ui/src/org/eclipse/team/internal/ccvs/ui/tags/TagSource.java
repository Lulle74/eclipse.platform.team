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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;

/**
 * A tag source provides access to a set of tags.
 */
public abstract class TagSource {
    
    public static final TagSource EMPTY = new TagSource() {
        public void commit(CVSTag[] tags, boolean replace, IProgressMonitor monitor) throws CVSException {
            // No-op
        }
        public ICVSRepositoryLocation getLocation() {
            // TODO Auto-generated method stub
            return null;
        }
        public String getShortDescription() {
            return "Empty"; //$NON-NLS-1$
        }
        public CVSTag[] getTags(int type) {
            return new CVSTag[0];
        }
        public void refresh(IProgressMonitor monitor) throws TeamException {
            // No-op
        }
        public ICVSResource[] getCVSResources() {
            return new ICVSResource[0];
        }
    };
    
    private ListenerList listeners = new ListenerList();
    
    /**
     * Simple interface for providing notification when the tags
     * for this source have changed.
     */
    public interface ITagSourceChangeListener {
        void tagsChanged(TagSource source);
    }
    
    /**
     * Create a tag source for the given folders
     * @param folders one or more folders
     * @return a tag source for the supplied folders
     */
    public static TagSource create(ICVSFolder[] folders) {
        if (folders.length == 1) {
            return new SingleFolderTagSource(folders[0]);
        } else {
            return new MultiFolderTagSource(folders);
        }
    }
    
    /**
     * Create a tag source for a list of resources
     * @param resources one or more resources
     * @return a tag source
     */
    public static TagSource create(ICVSResource[] resources) {
        if (resources.length == 1 && !resources[0].isFolder())
            return new SingleFileTagSource((ICVSFile)resources[0]);
        // TODO Should use a better tag source that uses all folders somehow
        return new SingleFolderTagSource(getFirstFolder(resources));
    }
    
    /**
     * Create a tag source for a list of resources
     * @param resources one or more resources
     * @return a tag source
     */
    public static TagSource create(IResource[] resources) {
        return create(getCVSResources(getProjects(resources)));
    }
    
    private static IResource[] getProjects(IResource[] resources) {
        Set result = new HashSet();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            result.add(resource.getProject());
        }
        return (IResource[]) result.toArray(new IResource[result.size()]);
    }

    /**
     * Return a tag source for a single remote folder
     * @param remote the remote folder
     * @return a tag source for that folder
     */
    public static TagSource create(ICVSRemoteFolder remote) {
        return new SingleFolderTagSource(remote);
    }
    
    private static ICVSResource[] getCVSResources(IResource[] resources) {
        List cvsResources = new ArrayList();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            cvsResources.add(CVSWorkspaceRoot.getCVSResourceFor(resource));
        }
        return (ICVSResource[]) cvsResources.toArray(new ICVSResource[cvsResources.size()]);
    }

    private static ICVSFolder getFirstFolder(ICVSResource[] resources) {
		if (resources[0].isFolder()) {
			return (ICVSFolder)resources[0];
		} else {
			return resources[0].getParent();
		}
	}
	
    public abstract CVSTag[] getTags(int type);
    
    public CVSTag[] getTags(int[] types) {
        if (types.length == 0) {
            return new CVSTag[0];
        }
        if (types.length == 1) {
            return getTags(types[0]);
        }
        List result = new ArrayList();
        for (int i = 0; i < types.length; i++) {
            int type = types[i];
            CVSTag[] tags = getTags(type);
            result.addAll(Arrays.asList(tags));
        }
        return (CVSTag[]) result.toArray(new CVSTag[result.size()]);
    }

    /**
     * Refresh the tags by contacting the server if appropriate
     * @param monitor a progress monitor
     */
    public abstract void refresh(IProgressMonitor monitor) throws TeamException;
    
    public abstract ICVSRepositoryLocation getLocation();

    /**
     * Return a short description of the tag source for displaying in UI.
     * @return a short description of the tag source for displaying in UI.
     */
    public abstract String getShortDescription();

    /**
     * Commit a set of tag changes to the tag cache
     * @param tags the tags that should be cached
     * @param replace whether existing tags not in the list should be removed
     * @param monitor a progress monitor
     * @throws CVSException
     */
    public abstract void commit(CVSTag[] tags, boolean replace, IProgressMonitor monitor) throws CVSException;
    
    public void addListener(ITagSourceChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ITagSourceChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners that the tags from this source may have changed
     */
    public void fireChange() {
        Object[] list = listeners.getListeners();
        for (int i = 0; i < list.length; i++) {
            final ITagSourceChangeListener listener = (ITagSourceChangeListener)list[i];
            Platform.run(new ISafeRunnable() {
                public void handleException(Throwable exception) {
                    // logged by run
                }
                public void run() throws Exception {
                    listener.tagsChanged(TagSource.this);
                }
            });
        }
    }
    
    public abstract ICVSResource[] getCVSResources();
}
