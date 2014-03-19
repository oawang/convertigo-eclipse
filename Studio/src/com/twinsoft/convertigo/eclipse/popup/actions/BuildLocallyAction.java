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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/TestCaseExecuteSelectedAction.java $
 * $Author: maximeh $
 * $Revision: 33944 $
 * $Date: 2013-04-05 18:29:40 +0200 (ven., 05 avr. 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobileDevice;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class BuildLocallyAction extends MyAbstractAction {

	static final String cordovaDir = "cordova";

	/**
	 * 
	 * @author opic
	 * This is a Fake HttpServeltRequest class implementation as the MobileResourceHelper.makeZipPackage takes a
	 * HttpServletRequest as argument.
	 * 
	 * The only Important Methods are getParameter() and getRequestUrl() used by the MakeZipPackage
	 * 	
	 */
	private class InternalRequest implements HttpServletRequest 
	{
		Map<String, String> parameters = new java.util.HashMap<String, String>();
		
		public void setParameter(String name, String value)
		{
			parameters.put(name, value);
		}
		
		@Override
		public Locale getLocale() {
			return null;
		}

		@Override
		public Enumeration<Locale> getLocales() {
			return null;
		}

		@Override
		public AsyncContext getAsyncContext() {
			return null;
		}

		@Override
		public Object getAttribute(String arg0) {
			return null;
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			return null;
		}

		@Override
		public String getCharacterEncoding() {
			return null;
		}

		@Override
		public int getContentLength() {
			return 0;
		}

		@Override
		public String getContentType() {
			return null;
		}

		@Override
		public DispatcherType getDispatcherType() {
			return null;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public String getLocalAddr() {
			return null;
		}

		@Override
		public String getLocalName() {
			return null;
		}

		@Override
		public int getLocalPort() {
			return 0;
		}

		@Override
		public String getParameter(String name) {
			return parameters.get(name);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			return null;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			return null;
		}

		@Override
		public String[] getParameterValues(String arg0) {
			return null;
		}

		@Override
		public String getProtocol() {
			return null;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			return null;
		}

		@Override
		public String getRealPath(String arg0) {
			return null;
		}

		@Override
		public String getRemoteAddr() {
			return null;
		}

		@Override
		public String getRemoteHost() {
			return null;
		}

		@Override
		public int getRemotePort() {
			return 0;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String arg0) {
			return null;
		}

		@Override
		public String getScheme() {
			return null;
		}

		@Override
		public String getServerName() {
			
			return null;
		}

		@Override
		public int getServerPort() {
			
			return 0;
		}

		@Override
		public ServletContext getServletContext() {
			
			return null;
		}

		@Override
		public boolean isAsyncStarted() {
			
			return false;
		}

		@Override
		public boolean isAsyncSupported() {
			
			return false;
		}

		@Override
		public boolean isSecure() {
			
			return false;
		}

		@Override
		public void removeAttribute(String arg0) {
			
			
		}

		@Override
		public void setAttribute(String arg0, Object arg1) {
			
			
		}

		@Override
		public void setCharacterEncoding(String arg0)
				throws UnsupportedEncodingException {
			
			
		}

		@Override
		public AsyncContext startAsync() {
			
			return null;
		}

		@Override
		public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
			
			return null;
		}

		@Override
		public boolean authenticate(HttpServletResponse arg0)
				throws IOException, ServletException {
			
			return false;
		}

		@Override
		public String getAuthType() {
			
			return null;
		}

		@Override
		public String getContextPath() {
			
			return null;
		}

		@Override
		public Cookie[] getCookies() {
			
			return null;
		}

		@Override
		public long getDateHeader(String arg0) {
			
			return 0;
		}

		@Override
		public String getHeader(String arg0) {
			
			return null;
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			
			return null;
		}

		@Override
		public Enumeration<String> getHeaders(String arg0) {
			
			return null;
		}

		@Override
		public int getIntHeader(String arg0) {
			
			return 0;
		}

		@Override
		public String getMethod() {
			
			return null;
		}

		@Override
		public Part getPart(String arg0) throws IOException,
				IllegalStateException, ServletException {
			
			return null;
		}

		@Override
		public Collection<Part> getParts() throws IOException,
				IllegalStateException, ServletException {
			
			return null;
		}

		@Override
		public String getPathInfo() {
			
			return null;
		}

		@Override
		public String getPathTranslated() {
			
			return null;
		}

		@Override
		public String getQueryString() {
			
			return null;
		}

		@Override
		public String getRemoteUser() {
			
			return null;
		}

		@Override
		public String getRequestURI() {
			
			return null;
		}

		@Override
		public StringBuffer getRequestURL() {
			// TODO : For the moment the Url is fixed with <address> This will be used for the endpoint
			// calculation.. We have to override this with the real address...
			return new StringBuffer("http://<address>/convertigo/admin/services/mobiles.GetSourcePackage");
		}

		@Override
		public String getRequestedSessionId() {
			
			return null;
		}

		@Override
		public String getServletPath() {
			
			return null;
		}

		@Override
		public HttpSession getSession() {
			
			return null;
		}

		@Override
		public HttpSession getSession(boolean arg0) {
			
			return null;
		}

		@Override
		public Principal getUserPrincipal() {
			
			return null;
		}

		@Override
		public boolean isRequestedSessionIdFromCookie() {
			
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromURL() {
			
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromUrl() {
			
			return false;
		}

		@Override
		public boolean isRequestedSessionIdValid() {
			
			return false;
		}

		@Override
		public boolean isUserInRole(String arg0) {
			
			return false;
		}

		@Override
		public void login(String arg0, String arg1) throws ServletException {
			
			
		}

		@Override
		public void logout() throws ServletException {
			
			
		}

	}
	
	public BuildLocallyAction() {
		super();
	}

	private void delete(File f)  {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
			}
		if (!f.delete())
		    Engine.logEngine.error("Failed to delete file: " + f);
	}

	private void runCordovaCommand(String Command, File projectDir) throws Throwable {
		final Process process;
		String[] envp = null;
		Map<String, String> envmap;
		envmap = System.getenv();
		envp = new String[envmap.size()];
		int i =0;
		for (Map.Entry<String, String> entry : envmap.entrySet())
			envp[i++] = entry.getKey() + "=" + entry.getValue();
		
		/**
		 * todo : handle command for Linux and MacOS (Should be cordova.sh...)
		 */
		process = Runtime.getRuntime().exec("cordova.cmd " + Command,
											envp,
											projectDir
		);
		
		InputStream is = process.getInputStream();
		final BufferedReader bis = new BufferedReader(new InputStreamReader(is));
		Thread readOutputThread = new Thread(new Runnable() {
			@Override
	        public void run() {
				try {
					String line;
					while ((line = bis.readLine()) != null) {
						Engine.logEngine.debug(line);
					}
				} catch (IOException e) {
					Engine.logEngine.error("Error while executing cordova command", e);
				}
			}
		});
		readOutputThread.start();
		process.waitFor();
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();

    			if ((databaseObject != null) && (databaseObject instanceof MobileDevice)) {
    				final MobileDevice mobileDevice = (MobileDevice)treeObject.getObject();
    				
    				// get the application name from the Mobile devices's property or if empty the project's name
    				final String applicationName = ((MobileApplication)mobileDevice.getParent()).getApplicationName().isEmpty() ?
    								ConvertigoPlugin.projectManager.currentProject.getName() :
    								((MobileApplication)mobileDevice.getParent()).getApplicationName();
    								
    				final String applicationId = ((MobileApplication)mobileDevice.getParent()).getApplicationId().isEmpty() ?
							"com.convertigo.mobile." + ConvertigoPlugin.projectManager.currentProject.getName() :
							((MobileApplication)mobileDevice.getParent()).getApplicationId();

    				// Cordova Env will be created in the _private directory
			        final File privateDir = new File(Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + "/_private");
			        // Just in case .. check that the private directory exists...
			        if (!privateDir.exists()) {
			        	ConvertigoPlugin.logInfo("Creating \"_private\" project directory");
			            try {
			                privateDir.mkdirs();
			            }
			            catch(Exception e) {
			                String message = java.text.MessageFormat.format(
			                    "Unable to create the private project directory \"{0}\"..",
			                    new Object[] { ConvertigoPlugin.projectManager.currentProject.getName() }
			                );
			                ConvertigoPlugin.logException(e, message);
			                return;
			            }
			        }
			        
			        // Test to see if the Cordova application has been created
			        String[] cordovaDirs =  privateDir.list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							if (name.equalsIgnoreCase(cordovaDir))
								return true;
							else
								return false;
						}
					});
			        if (cordovaDirs.length == 0) {
			        	// no Cordova directory has been found ask the user if he wants to create it
			        	CustomDialog customDialog = new CustomDialog(
    							shell,
    							"Create a Cordova environment",
    							"The cordova environment has not been created yet. Creating the environment\n" +
    							"must be done once by project. This project's environment will be shared by all \n" +
    							"mobile devices for local build.\n\n" +
    							"You have to install Cordova on your local machine to be able to build locally.\n" +
    							"If Cordova is not yet installed, click 'No' and download cordova from :\n" +
    							"http://cordova.apache.org . Be sure to follow all instruction on Cordova's\n" +
    							"Web site to setup your local Cordova build system. \n\n" +
    							"Do you want to create a Cordova environment now ?",
    							500, 280,
    							new ButtonSpec("Yes", true),
    							new ButtonSpec("No", false)
    					);
    					int response = customDialog.open();
    					if (response == 0) {
    						//create a local Cordova Environment
				        	File cordovaDir = new File(privateDir.getAbsolutePath() + "/" + BuildLocallyAction.cordovaDir);
    						runCordovaCommand("create " + BuildLocallyAction.cordovaDir + " " + applicationId + " " + applicationName, privateDir);
    						
    						// Add all mandatory plugins for Flash Update
    						runCordovaCommand("plugin add  org.apache.cordova.file", cordovaDir);
    						runCordovaCommand("plugin add  org.apache.cordova.file-transfer", cordovaDir);
    						runCordovaCommand("plugin add  org.apache.cordova.device", cordovaDir);
    						
    						Engine.logEngine.debug("Cordova environment is now ready.");
    					} else {
    						return;
    					}
			        } 
			        
			        // OK we are sure we have a Cordova environment.. Start the build
		        	Job buildJob = new Job("Local Cordova Build in progress...") {
						@Override
						protected IStatus run(IProgressMonitor arg0) {
							try {
					        	// Cordova environment is already created, we have to build
					        	// Step 1 : Delete everything in the Cordova's www directory
					        	File wwwDir = new File(privateDir.getAbsolutePath() + "/" + BuildLocallyAction.cordovaDir + "/www");
				        		delete(wwwDir);
					        	
					        	// Step 2 call Mobile packager to build ZIP package, simulate a fake HttpRequest
					        	InternalRequest myRequest = new InternalRequest();
					        	myRequest.setParameter("application", applicationName);
					        	File mobileArchiveFile = MobileResourceHelper.makeZipPackage(myRequest);
					        	Engine.logEngine.debug("ZIP Build package created in : " + mobileArchiveFile.getAbsolutePath());
					        	
					        	// Step 3 : Unzip in the www directory
					        	ZipUtils.expandZip(mobileArchiveFile.getAbsolutePath(),
					        			           wwwDir.getAbsolutePath());
					        	Engine.logEngine.debug("ZIP expanded in : " + wwwDir.getAbsolutePath());
					        	
					        	// Step 4: Build using Cordova the specific platform.
					        	String deviceType = mobileDevice.getClass().getName();
					        	
					        	String cordovaPlatform = "android";
					        	if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.Android"))
					        		cordovaPlatform = "android";
					        	else if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.IPad"))
					        		cordovaPlatform = "ios";
					        	else if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.IPhone3"))
					        		cordovaPlatform = "ios";
					        	else if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.IPhone4"))
					        		cordovaPlatform = "ios";
					        	else if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.BlackBerry6"))
					        		cordovaPlatform = "blackberry";
					        	else if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.WindowsPhone7"))
					        		cordovaPlatform = "wp7";
					        	else if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.WindowsPhone8"))
					        		cordovaPlatform = "wp8";
					        	else if (deviceType.equalsIgnoreCase("com.twinsoft.convertigo.beans.mobiledevices.Windows8"))
					        		cordovaPlatform = "windows8";
		
					        	File cordovaDir = new File(privateDir.getAbsolutePath() + "/" + BuildLocallyAction.cordovaDir);
					        	runCordovaCommand("platform add " + cordovaPlatform, cordovaDir);
					        	runCordovaCommand("build " + cordovaPlatform, cordovaDir);
					        	return org.eclipse.core.runtime.Status.OK_STATUS;
					        	
							} catch (Throwable e) {
					        	ConvertigoPlugin.logException(e, "Error when processing Cordova build");
					        	return org.eclipse.core.runtime.Status.CANCEL_STATUS;
							}
						}
		        	};
		        	buildJob.setUser(true);
		        	buildJob.schedule();
    			}
    		}
        }
		
		catch (IOException ee) {
        	CustomDialog customDialog = new CustomDialog(
					shell,
					"Cordova installation not found",
					"In order to use local build you must install on your workstation a valid\n" +
					"Cordova build system. You can download and install Cordova from \n" +
					"http://cordova.apache.org . Be sure to follow all instruction on Cordova\n" +
					"Web site to setup your local Cordova build system. \n\n" +
					"This message can also appear if cordova is not in your PATH.",
					500, 200,
					new ButtonSpec("Ok", true)
			);
			customDialog.open();
		}
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to build locally with Cordova");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}