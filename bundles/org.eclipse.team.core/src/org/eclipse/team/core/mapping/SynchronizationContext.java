/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.synchronize.ISyncInfoTree;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.diff.DiffTree;
import org.eclipse.team.internal.core.mapping.DiffCache;

/**
 * Abstract implementation of the {@link ISynchronizationContext} interface.
 * This class can be subclassed by clients.
 * 
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @see ISynchronizationContext
 * @since 3.2
 */
public abstract class SynchronizationContext implements ISynchronizationContext {

	private IResourceMappingScope input;
    private final String type;
    private final SyncInfoTree tree;
    private final DiffTree deltaTree;
    private DiffCache cache;

    /**
     * Create a synchronization context
     * @param input the input that defines the scope of the synchronization
     * @param type the type of synchronization (ONE_WAY or TWO_WAY)
     * @param tree the sync info tree that contains all out-of-sync resources
     */
    protected SynchronizationContext(IResourceMappingScope input, String type, SyncInfoTree tree, DiffTree deltaTree) {
    	this.input = input;
		this.type = type;
		this.tree = tree;
		this.deltaTree = deltaTree;
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#getInput()
	 */
	public IResourceMappingScope getScope() {
		return input;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#getSyncInfoTree()
	 */
	public ISyncInfoTree getSyncInfoTree() {
		return tree;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#getType()
	 */
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#dispose()
	 */
	public void dispose() {
		if (cache != null) {
			cache.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#getCache()
	 */
	public synchronized IDiffCache getCache() {
		if (cache == null) {
			cache = new DiffCache(this);
		}
		return cache;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#getSyncDeltaTree()
	 */
	public IDiffTree getDiffTree() {
		return deltaTree;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#getResource(org.eclipse.team.core.delta.ISyncDelta)
	 */
	public IResource getResource(IDiffNode delta) {
		IResource resource = null;
		if (delta instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) delta;
			resource = internalGetResource((IResourceDiff)twd.getLocalChange());
			if (resource == null)
				resource = internalGetResource((IResourceDiff)twd.getRemoteChange());
		} else {
			resource = internalGetResource((IResourceDiff)delta);
		}
		return resource;	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#getDeltas(org.eclipse.core.resources.mapping.ResourceTraversal[])
	 */
	public IDiffNode[] getDiffs(final ResourceTraversal[] traversals) {
		final Set result = new HashSet();
		try {
			getDiffTree().accept(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), new IDiffVisitor() {
				public boolean visit(IDiffNode delta) throws CoreException {
					for (int i = 0; i < traversals.length; i++) {
						ResourceTraversal traversal = traversals[i];
						if (traversal.contains(getResource(delta))) {
							result.add(delta);
						}
					}
					return true;
				}
			}, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			TeamPlugin.log(e);
		}
		return (IDiffNode[]) result.toArray(new IDiffNode[result.size()]);
	}

	private IResource internalGetResource(IResourceDiff localChange) {
		if (localChange == null)
			return null;
		Object before = localChange.getBeforeState();
		IResourceVariant variant = null;
		if (before instanceof IResourceVariant) {
			variant = (IResourceVariant) before;
		}
		if (variant == null) {
			Object after = localChange.getAfterState();
			if (after instanceof IResourceVariant) {
				variant = (IResourceVariant) after;
			}
		}
		if (variant != null) {
			return internalGetResource(localChange.getPath(), variant.isContainer());
		}
		return null;
	}

	private IResource internalGetResource(IPath fullPath, boolean container) {
		if (container)
			return ResourcesPlugin.getWorkspace().getRoot().getFolder(fullPath);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fullPath);
	}
	
	

}