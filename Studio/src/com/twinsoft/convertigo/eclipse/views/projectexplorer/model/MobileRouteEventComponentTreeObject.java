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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;

public class MobileRouteEventComponentTreeObject extends MobileComponentTreeObject {

	public MobileRouteEventComponentTreeObject(Viewer viewer, RouteEventComponent object) {
		super(viewer, object);
	}

	public MobileRouteEventComponentTreeObject(Viewer viewer, RouteEventComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
	}

	@Override
	public RouteEventComponent getObject() {
		return (RouteEventComponent) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}

	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
		if (bModified && !isInherited) {
			markTemplateAsDirty();
		}
	}
	
	protected void markTemplateAsDirty() {
		;
	}

}