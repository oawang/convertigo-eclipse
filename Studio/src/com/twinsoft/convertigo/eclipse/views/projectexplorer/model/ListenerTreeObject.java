/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/model/DocumentTreeObject.java $
 * $Author: nathalieh $
 * $Revision: 39108 $
 * $Date: 2015-02-04 15:23:39 +0100 (mer., 04 févr. 2015) $
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Listener;

public class ListenerTreeObject extends DatabaseObjectTreeObject {

	public ListenerTreeObject(Viewer viewer, DatabaseObject object) {
		super(viewer, object);
	}

	public ListenerTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
		setEnabled(getObject().isEnabled());
	}

	@Override
	public Listener getObject() {
		return (Listener)super.getObject();
	}
	
	@Override
    public boolean isEnabled() {
		setEnabled(getObject().isEnabled());
    	return super.isEnabled();
    }
	
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isEnable")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		return super.testAttribute(target, name, value);
	}
}
