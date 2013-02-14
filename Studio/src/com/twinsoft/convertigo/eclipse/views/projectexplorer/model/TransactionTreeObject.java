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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.util.StringEx;

public class TransactionTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {
	
	private boolean isLearning = false;
	private boolean isDreamface = false;
	
	public TransactionTreeObject(Viewer viewer, Transaction object) {
		this(viewer, object, false);
	}

	public TransactionTreeObject(Viewer viewer, Transaction object, boolean inherited) {
		super(viewer, object, inherited);
		isDefault = ((Transaction)object).isDefault;
	}

	@Override
	public Transaction getObject(){
		return (Transaction) super.getObject();
	}
	
	@Override
	public void hasBeenModified(boolean modified) {
		super.hasBeenModified(modified);
		if (modified && (getObject() instanceof HtmlTransaction)) {
			HtmlConnector htmlConnector = (HtmlConnector)((HtmlTransaction)getObject()).getConnector();
			htmlConnector.checkForStateless();
		}
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);

		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "":propertyName);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			
			// If a bean name has changed
			if ("name".equals(propertyName)) {
				handlesBeanNameChanged(treeObjectEvent);
				
			}
			else if ("sqlQuery".equals(propertyName)) {
				if (treeObject.equals(this)) {
    		    	try {
    					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
    				} catch (Exception e) {
    					ConvertigoPlugin.logWarning(e, "Could not reload in tree Transaction \""+databaseObject.getName()+"\" !");
    				}
				}
			}
		}
	}
	
	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (databaseObject instanceof ScreenClass) {
			String oldName = StringUtils.normalize((String)oldValue);
			String newName = StringUtils.normalize((String)newValue);

			Transaction transaction = getObject();
			// Modify Screenclass name in Transaction handlers
			if (!(transaction instanceof HtmlTransaction)) {
            	
				// ScreenClass and Transaction must have the same connector!
				if (transaction.getConnector().equals(databaseObject.getConnector())) {
					String oldHandlerPrefix = "on" + StringUtils.normalize(oldName);
	            	String newHandlerPrefix = "on" + StringUtils.normalize(newName);

	            	if (transaction.handlers.indexOf(oldHandlerPrefix) != -1) {
	    				StringEx sx = new StringEx(transaction.handlers);
	            		// Updating comments
	            		sx.replaceAll("handler for screen class \"" + oldName + "\"", "handler for screen class \"" + newName + "\"");
	            		// Updating functions def & calls
	            		sx.replaceAll(oldHandlerPrefix + "Entry", newHandlerPrefix + "Entry");
	            		sx.replaceAll(oldHandlerPrefix + "Exit", newHandlerPrefix + "Exit");
	            		String newHandlers = sx.toString();
	            		
	            		if (!newHandlers.equals(transaction.handlers)) {
	                		transaction.handlers = newHandlers;
	                		hasBeenModified(true);
	            		}
	            		
	    				// Updating the opened handlers editor if any
	    				IEditorPart jspart = ConvertigoPlugin.getDefault().getJscriptTransactionEditor(transaction);
	    				if ((jspart != null) && (jspart instanceof JscriptTransactionEditor)) {
	    					JscriptTransactionEditor jscriptTransactionEditor = (JscriptTransactionEditor)jspart;
	    					jscriptTransactionEditor.reload();
	    				}
	    				
	    		    	try {
	    					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
	    				} catch (Exception e) {
	    					ConvertigoPlugin.logWarning(e, "Could not reload in tree Transaction \""+databaseObject.getName()+"\" !");
	    				}
	            	}
				}
			}
		}
	}
	
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isDefault")) {
			isDefault = getObject().isDefault;
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isDefault));
		}
		if (name.equals("isLearning")) {
			isLearning = getObject().isLearning;
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isLearning));
		}
		if (name.equals("isDreamface")) {
			isDreamface = !EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_MASHUP_URL).equals("");
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isDreamface));
		}
		return super.testAttribute(target, name, value);
	}

	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Open editor
			if ((editorType == null) || ((editorType != null) && (editorType.equals("JscriptTransactionEditor"))))
				openJscriptTransactionEditor(project);
			if ((editorType != null) && (editorType.equals("XMLTransactionEditor")))
				openXMLTransactionEditor(project);
			
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}

	public void openJscriptTransactionEditor(IProject project) {
		Transaction transaction = (Transaction)this.getObject();
		
		String tempFileName = 	"_private/"+project.getName()+
								"__"+transaction.getConnector().getName()+
								"__"+transaction.getName();
		
		IFile file = project.getFile(tempFileName);

		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new JscriptTransactionEditorInput(file,transaction),
										"com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor");
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the transaction editor '" + transaction.getName() + "'");
			} 
		}
	}

	public void openXMLTransactionEditor(IProject project) {
		Transaction transaction = (Transaction)this.getObject();
		
		IFile	file = project.getFile("_private/"+transaction.getName()+".xml");
		
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new XMLTransactionEditorInput(file,transaction),
										"com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionEditor");
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the transaction editor '" + transaction.getName() + "'");
			} 
		}
	}
}