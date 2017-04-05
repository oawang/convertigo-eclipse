/*
 * Copyright (c) 2001-2016 Convertigo SA.
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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.views.mobile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.chromium.Browser;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;

public class MobileDebugView extends ViewPart implements IPartListener2 {
	
	Browser browser;
	
	public MobileDebugView() {
		
	}

	@Override
	public void dispose() {
		getSite().getPage().removePartListener(this);
		browser.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		browser = new C8oBrowser(parent, SWT.NONE).getBrowser();
		
		browser.loadHTML("<body>please select a mobile application editor</body>");
		
		
		getSite().getPage().addPartListener(this);
	}

	@Override
	public void setFocus() {
		
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if (part instanceof ApplicationComponentEditor) {
			String url = ((ApplicationComponentEditor) part).getDebugUrl();
			if (url != null && !url.equals(browser.getURL())) {
				browser.loadURL(url);
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

}
