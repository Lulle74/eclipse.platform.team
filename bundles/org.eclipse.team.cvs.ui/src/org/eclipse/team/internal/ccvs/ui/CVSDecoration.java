/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.*;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

/**
 * A decoration describes the annotations to a user interface element. The
 * annotations can apply to text (e.g. prefix, suffix, color, font) and to an
 * image (e.g. overlays).
 * <p>
 * This class is derived from an internal workbench class
 * <code>IDecoration</code> and is often used in conjunction with the label
 * decoration APIs. As such a client can convert between them using helpers
 * defined in this class.
 * </p>
 * TODO: 
 * profile
 * add colors and fonts to preferences instead of being hard coded
 * what to do with CVSDecorationConfiguration class?
 * preference page externalizations
 * preference page preview should update when theme changes
 * 
 * @since 3.1
 */
public class CVSDecoration {

    // Dirty state indicators
    public static final int NOT_DIRTY = 0;
    public static final int POSSIBLY_DIRTY = 1;
    public static final int DIRTY = 2;
    
	// Decorations
	private String prefix;
	private String suffix;
	private ImageDescriptor overlay;
	private Color bkgColor;
	private Color fgColor;
	private Font font;
	
	// Properties
	private int resourceType = IResource.FILE;
	private boolean watchEditEnabled = false;
	private int dirtyState = NOT_DIRTY;
	private boolean isIgnored = false;
	private boolean isAdded = false;
	private boolean isNewResource = false;
	private boolean hasRemote = false;
	private boolean readOnly = false;
	private boolean needsMerge = false;
	private boolean virtualFolder = false;
	private String tag;
	private String revision;
	private String repository;
	private ICVSRepositoryLocation location;
	private String keywordSubstitution;

	// Text formatters
	private String fileFormatter;
	private String folderFormatter;
	private String projectFormatter;
	private String resourceName;
	
	//	Images cached for better performance
	private static ImageDescriptor dirty;
	private static ImageDescriptor checkedIn;
	private static ImageDescriptor noRemoteDir;
	private static ImageDescriptor added;
	private static ImageDescriptor merged;
	private static ImageDescriptor newResource;
	private static ImageDescriptor edited;
	
	// List of preferences used to configure the decorations that
	// are applied.
	private Preferences preferences;

	/*
	 * Define a cached image descriptor which only creates the image data once
	 */
	public static class CachedImageDescriptor extends ImageDescriptor {

		ImageDescriptor descriptor;
		ImageData data;

		public CachedImageDescriptor(ImageDescriptor descriptor) {
			Assert.isNotNull(descriptor);
			this.descriptor = descriptor;
		}

		public ImageData getImageData() {
			if (data == null) {
				data = descriptor.getImageData();
			}
			return data;
		}
	}

	static {
		dirty = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR));
		checkedIn = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		added = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		merged = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_MERGED));
		newResource = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_QUESTIONABLE));
		edited = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_EDITED));
		noRemoteDir = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_NO_REMOTEDIR));
	}

	/**
	 * Default constructor uses the plug-in's preferences to determine text decoration
	 * formatters and preferences.
	 */
	public CVSDecoration(String resourceName) {
		// 	TODO: for efficiency don't look up a pref until its needed
		IPreferenceStore store = getStore();
		Preferences prefs = new Preferences();
		
		prefs.setValue(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION, store.getBoolean(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION));
		prefs.setValue(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION, store.getBoolean(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION));
		prefs.setValue(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION, store.getBoolean(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION));
		prefs.setValue(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, store.getBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION));
		prefs.setValue(ICVSUIConstants.PREF_CALCULATE_DIRTY, store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY));
		prefs.setValue(ICVSUIConstants.PREF_DIRTY_FLAG, store.getString(ICVSUIConstants.PREF_DIRTY_FLAG));
        prefs.setValue(ICVSUIConstants.PREF_POSSIBLY_DIRTY_FLAG, store.getString(ICVSUIConstants.PREF_POSSIBLY_DIRTY_FLAG));
		prefs.setValue(ICVSUIConstants.PREF_ADDED_FLAG, store.getString(ICVSUIConstants.PREF_ADDED_FLAG));
		prefs.setValue(ICVSUIConstants.PREF_USE_FONT_DECORATORS, store.getString(ICVSUIConstants.PREF_USE_FONT_DECORATORS));
		
		initialize(resourceName, prefs, store.getString(ICVSUIConstants.PREF_FILETEXT_DECORATION), store.getString(ICVSUIConstants.PREF_FOLDERTEXT_DECORATION), store.getString(ICVSUIConstants.PREF_PROJECTTEXT_DECORATION));
	}

	public CVSDecoration(String resourceName, Preferences preferences, String fileFormater, String folderFormatter, String projectFormatter) {
		initialize(resourceName, preferences, fileFormater, folderFormatter, projectFormatter);
	}

	private IPreferenceStore getStore() {
		return CVSUIPlugin.getPlugin().getPreferenceStore();
	}

	private void initialize(String resourceName, Preferences preferences, String fileFormater, String folderFormatter, String projectFormatter) {
		this.resourceName = resourceName;
		this.preferences = preferences;
		this.fileFormatter = fileFormater;
		this.folderFormatter = folderFormatter;
		this.projectFormatter = projectFormatter;
	}

	public void addPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void addSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void setForegroundColor(Color fgColor) {
		this.fgColor = fgColor;
	}

	public void setBackgroundColor(Color bkgColor) {
		this.bkgColor = bkgColor;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Color getBackgroundColor() {
		return bkgColor;
	}

	public Color getForegroundColor() {
		return fgColor;
	}

	public Font getFont() {
		return font;
	}

	public ImageDescriptor getOverlay() {
		return overlay;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}
	
	public void setResourceType(int type) {
		this.resourceType = type;
	}

	public void apply(IDecoration decoration) {
		compute();
		// apply changes
		String suffix = getSuffix();
		if(suffix != null)
			decoration.addSuffix(suffix);
		String prefix = getPrefix();
		if(prefix != null)
			decoration.addPrefix(prefix);
		ImageDescriptor overlay = getOverlay();
		if(overlay != null)
			decoration.addOverlay(getOverlay());
		Color bc = getBackgroundColor();
		if(bc != null)
			decoration.setBackgroundColor(bc);
		Color fc = getForegroundColor();
		if(fc != null)
			decoration.setForegroundColor(fc);
		Font f = getFont();
		if(f != null)
			decoration.setFont(f);
	}

	public void compute() {
		computeText();
		overlay = computeImage();
		computeColorsAndFonts();
	}

	private void computeText() {
		Map bindings = new HashMap();
		if (getDirtyState() == DIRTY) {
			bindings.put(CVSDecoratorConfiguration.DIRTY_FLAG, preferences.getString(ICVSUIConstants.PREF_DIRTY_FLAG));
		} else if (getDirtyState() == POSSIBLY_DIRTY) {
            bindings.put(CVSDecoratorConfiguration.DIRTY_FLAG, preferences.getString(ICVSUIConstants.PREF_POSSIBLY_DIRTY_FLAG));
        }
		if (isAdded()) {
			bindings.put(CVSDecoratorConfiguration.ADDED_FLAG, preferences.getString(ICVSUIConstants.PREF_ADDED_FLAG));
		} else if(isHasRemote()){
			bindings.put(CVSDecoratorConfiguration.FILE_REVISION, getRevision());
			bindings.put(CVSDecoratorConfiguration.RESOURCE_TAG, getTag());
		}	
		bindings.put(CVSDecoratorConfiguration.RESOURCE_NAME, getResourceName());
		bindings.put(CVSDecoratorConfiguration.FILE_KEYWORD, getKeywordSubstitution());
		if (resourceType != IResource.FILE) {
            if (location != null) {
    			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_HOST, location.getHost());
    			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_METHOD, location.getMethod().getName());
    			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_USER, location.getUsername());
    			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_ROOT, location.getRootDirectory());
    
                RepositoryManager repositoryManager = CVSUIPlugin.getPlugin().getRepositoryManager();
                RepositoryRoot root = repositoryManager.getRepositoryRootFor(location);
                CVSUIPlugin.getPlugin().getRepositoryManager();
                String label = root.getName();
                if (label == null) {
                  label = location.getLocation(true);
                }
                bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_LABEL, label);
            }
            if (repository != null) {
                bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_REPOSITORY, repository);
            }
        }

		CVSDecoratorConfiguration.decorate(this, getTextFormatter(), bindings);
	}

	private ImageDescriptor computeImage() {
		// show newResource icon
		if (preferences.getBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION) && isNewResource()) {
			return newResource;
		}
		// show dirty icon
		if (preferences.getBoolean(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION) && getDirtyState() == DIRTY) {
			return dirty;
		}
        // TODO: Need an icon for potentially dirty
		// show added
		if (preferences.getBoolean(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION) && isAdded()) {
			return added;
		}
		// show watch edit
		if (isWatchEditEnabled() && resourceType == IResource.FILE && !isReadOnly() && isHasRemote()) {
			return edited;
		}
		// show checked in
		if (preferences.getBoolean(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION) && isHasRemote()) {
			if (resourceType != IResource.FILE && isVirtualFolder()) {
				return noRemoteDir;
			}
			return checkedIn;
		}
		//nothing matched
		return null;
	}	
	
	private void computeColorsAndFonts() {
		if (!preferences.getBoolean(ICVSUIConstants.PREF_USE_FONT_DECORATORS))
			return;
			
		ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
		if(isIgnored()) {
			setBackgroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.IGNORED_BACKGROUND_COLOR));
			setForegroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.IGNORED_FOREGROUND_COLOR));
			setFont(current.getFontRegistry().get(CVSDecoratorConfiguration.IGNORED_FONT));
		} else if(getDirtyState() == DIRTY) {
			setBackgroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR));
			setForegroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR));
			setFont(current.getFontRegistry().get(CVSDecoratorConfiguration.OUTGOING_CHANGE_FONT));
		}
	}

	private String getResourceName() {
		return resourceName;
	}

	private String getTextFormatter() {
		switch (resourceType) {
			case IResource.FILE :
				return fileFormatter;
			case IResource.FOLDER :
				return folderFormatter;
			case IResource.PROJECT :
				return projectFormatter;
		}
		return "no format specified"; //$NON-NLS-1$
	}

	public boolean isAdded() {
		return isAdded;
	}

	public void setAdded(boolean isAdded) {
		this.isAdded = isAdded;
	}
    
    public int getDirtyState() {
        return dirtyState;
    }

	public void setDirtyState(int dirtyState) {
		this.dirtyState = dirtyState;
	}

	public boolean isIgnored() {
		return isIgnored;
	}

	public void setIgnored(boolean isIgnored) {
		this.isIgnored = isIgnored;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public boolean isWatchEditEnabled() {
		return watchEditEnabled;
	}

	public void setWatchEditEnabled(boolean watchEditEnabled) {
		this.watchEditEnabled = watchEditEnabled;
	}

	public boolean isNewResource() {
		return isNewResource;
	}

	public void setNewResource(boolean isNewResource) {
		this.isNewResource = isNewResource;
	}

	public void setLocation(ICVSRepositoryLocation location) {
		this.location = location;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getKeywordSubstitution() {
		return keywordSubstitution;
	}

	public void setKeywordSubstitution(String keywordSubstitution) {
		this.keywordSubstitution = keywordSubstitution;
	}

	public void setNeedsMerge(boolean needsMerge) {
		this.needsMerge = needsMerge;
	}

	public boolean isHasRemote() {
		return hasRemote;
	}

	public void setHasRemote(boolean hasRemote) {
		this.hasRemote = hasRemote;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isVirtualFolder() {
		return virtualFolder;
	}

	public void setVirtualFolder(boolean virtualFolder) {
		this.virtualFolder = virtualFolder;
	}
    
    public ICVSRepositoryLocation getLocation() {
        return location;
    }
    
    public void combine(CVSDecoration resourceDecoration) {
        // dirty if any are dirty
        int ds = resourceDecoration.getDirtyState();
        if (resourceDecoration.getDirtyState() == DIRTY || getDirtyState() == NOT_DIRTY)
            setDirtyState(ds);
        // ignored only if all are ignored
        if (isIgnored() && !resourceDecoration.isIgnored())
            setIgnored(false);
        // added only if all are added
        if (isAdded() && !resourceDecoration.isAdded())
            setAdded(false);
        // remote if any have a remote
        if (resourceDecoration.isHasRemote())
            setHasRemote(true);
        // Only new if all are new
        if (isNewResource() && !resourceDecoration.isNewResource())
            setNewResource(false);
        // Only watch-edit enabled if all are
        if (isWatchEditEnabled() && !resourceDecoration.isWatchEditEnabled())
            setWatchEditEnabled(false);
        // Only read-only if all are
        if (isReadOnly() && !resourceDecoration.isReadOnly())
            setReadOnly(false);
        // Only virtual if all are
        if (isVirtualFolder() && !resourceDecoration.isVirtualFolder())
            setVirtualFolder(false);
        // TODO what about needsMerge
        // Can only have a revision for a direct mapping to a single file
        if (getRevision() != null) {
            revision = null;
        }
        // Can only have a keyword substitution mode for a direct mapping to a single file
        if (getKeywordSubstitution() != null) {
            keywordSubstitution = null;
        }
        // Will only show the tag if it is the same for all
        if (getTag() != null) {
            if (resourceDecoration.getTag() == null || !getTag().equals(resourceDecoration.getTag())) {
                setTag(null);
            }
        }
        // Can only have a repository path for a single folder
        if (repository != null)
            repository = null;
        if (getLocation() != null) {
            if (resourceDecoration.getLocation() == null || !getLocation().equals(resourceDecoration.getLocation())) {
                setLocation(null);
            }
        }
        // Assume the folder type for multiple resource mappings
        resourceType = IResource.FOLDER;
        
    }
}
