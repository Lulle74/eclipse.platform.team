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
package org.eclipse.team.internal.ui.history;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;


public class TypedBufferedContent extends ResourceNode {
	public TypedBufferedContent(IFile resource) {
		super(resource);
	}
	protected InputStream createStream() throws CoreException {
		return ((IFile)getResource()).getContents();
	}
	public void setContent(byte[] contents) {
		if (contents == null) contents = new byte[0];
		final InputStream is = new ByteArrayInputStream(contents);
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					IFile file = (IFile) getResource();
					if (is != null) {
						if (!file.exists()) {
							file.create(is, false, monitor);
						} else {
							file.setContents(is, false, true, monitor);
						}
					} else {
						file.delete(false, true, monitor);
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
			progressService.run(false,false, runnable);
		} catch (InvocationTargetException e) {
			//TODO: Handle
		} catch (InterruptedException e) {
			// Ignore
		}
		fireContentChanged();
	}	
	public ITypedElement replace(ITypedElement child, ITypedElement other) {
		return null;
	}
	public void fireChange() {
		fireContentChanged();
	}
}
