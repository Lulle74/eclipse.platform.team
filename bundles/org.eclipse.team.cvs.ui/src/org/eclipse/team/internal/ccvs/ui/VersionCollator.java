package org.eclipse.team.internal.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Collator to compare two CVS revisions
 */
public class VersionCollator {
	public int compare(String revision1, String revision2) {
		if (revision1 == null && revision2 == null) return 0;
		if (revision1 == null) return -1;
		if (revision2 == null) return 1;
		int[] revision1Segments = getIntSegments(revision1);
		int[] revision2Segments = getIntSegments(revision2);
		for (int i = 0; i < revision1Segments.length && i < revision2Segments.length; i++) {
			int i1 = revision1Segments[i];
			int i2 = revision2Segments[i];
			if (i1 != i2) {
				return i1 > i2 ? 1 : -1;
			}
		}
		if (revision1Segments.length != revision2Segments.length) {
			return revision1Segments.length > revision2Segments.length ? 1 : -1;
		}
		return 0;
	}
	
	int[] getIntSegments(String string) {
		int size = string.length();
		if (size == 0) return new int[0];
		StringBuffer buffer = new StringBuffer();
		List list = new ArrayList();
		for (int i = 0; i < size; i++) {
			char ch = string.charAt(i);
			if (ch == '.') {
				list.add(new Integer(buffer.toString()));
				buffer = new StringBuffer();
			} else {
				buffer.append(ch);
			}
		}
		list.add(new Integer(buffer.toString()));
		int[] result = new int[list.size()];
		Iterator it = list.iterator();
		for (int i = 0; i < result.length; i++) {
			result[i] = ((Integer)it.next()).intValue();
		}
		return result;
	}
}

