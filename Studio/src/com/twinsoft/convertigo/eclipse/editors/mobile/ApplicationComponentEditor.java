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

package com.twinsoft.convertigo.eclipse.editors.mobile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.events.ScriptContextAdapter;
import com.teamdev.jxbrowser.chromium.events.ScriptContextEvent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

public class ApplicationComponentEditor extends EditorPart implements ISelectionChangedListener {

	private ProjectExplorerView projectExplorerView = null;
	private ApplicationComponentEditorInput applicationEditorInput;
	
	private ScrolledComposite browserScroll;
	private GridData browserGD;
	private Menu devicesMenu;
	private Composite deviceBar;
	
	private Text deviceName;
	private Text deviceWidth;
	private Text deviceHeight;
	
	private C8oBrowser c8oBrowser;
	private Browser browser;
	private String debugUrl;
	private String baseUrl = null;
	private String pageName = null;
	private Collection<Process> processes = new LinkedList<>();
	
	private JSONArray devicesDefinition;
	private JSONArray devicesDefinitionCustom;
	
	private static Pattern pIsServerRunning = Pattern.compile(".*?server running: (http\\S*).*");
	private static Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
	
	public ApplicationComponentEditor() {
		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		
		if (projectExplorerView != null) {
			projectExplorerView.addSelectionChangedListener(this);
		}
		
		try {
			devicesDefinition = new JSONArray(IOUtils.toString(getClass().getResourceAsStream("devices.json"), "UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			devicesDefinition = new JSONArray();
		}
		
		try {
			devicesDefinitionCustom = new JSONArray(FileUtils.readFileToString(new File(Engine.USER_WORKSPACE_PATH, "studio/devices.json"), "UTF-8"));
		} catch (Exception e) {
			devicesDefinitionCustom = new JSONArray();
		}
	}

	
	@Override
	public void dispose() {
		if (projectExplorerView != null) {
			projectExplorerView.removeSelectionChangedListener(this);
		}
		
		c8oBrowser.dispose();
		
		for (Process p: processes) {
			p.destroyForcibly();
			p.destroy();
		}
		
		try {
			new ProcessBuilder("taskkill", "/F", "/IM", "node.exe").start();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		super.dispose();
	}


	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		
		applicationEditorInput = (ApplicationComponentEditorInput) input;
		setPartName(applicationEditorInput.application.getProject().getName() + " [A: " + applicationEditorInput.application.getName()+"]");
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite editor = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginBottom = gl.marginTop = gl.marginLeft = gl.marginRight
				= gl.marginHeight = gl.marginWidth
				= gl.horizontalSpacing = gl.verticalSpacing = 0;
		
		editor.setLayout(gl);
				
		devicesMenu = new Menu(parent.getShell());
		
		updateDevicesMenu();
		createToolbar(editor);
		createDeviceBar(editor);
		createBrowser(editor);
		
		devicesMenu.getItems()[0].notifyListeners(SWT.Selection, new Event());
		
		launchBuilder();
		
		getSite().getWorkbenchWindow().getActivePage().activate(this);
	}

	private void createBrowser(Composite parent) {
		browserScroll = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		browserScroll.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		browserScroll.setExpandHorizontal(true);
		browserScroll.setExpandVertical(true);
		
		Composite canvas = new Composite(browserScroll, SWT.NONE);
		browserScroll.setContent(canvas);
		
		GridLayout gl = new GridLayout(1, false);
		gl.marginBottom = gl.marginTop = gl.marginLeft = gl.marginRight
				= gl.marginHeight = gl.marginWidth
				= gl.horizontalSpacing = gl.verticalSpacing = 0;
		
		canvas.setLayout(gl);
		
		c8oBrowser = new C8oBrowser(canvas, SWT.NONE);
		browserGD = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		c8oBrowser.setLayoutData(browserGD);

		browser = c8oBrowser.getBrowser();
		debugUrl = browser.getRemoteDebuggingURL();
		browser.addScriptContextListener(new ScriptContextAdapter() {

			@Override
			public void onScriptContextCreated(ScriptContextEvent event) {
				String url = browser.getURL();
				if (baseUrl != null && url.startsWith(baseUrl)) {
					browser.executeJavaScript("sessionStorage.setItem('_c8ocafsession_storage_mode', 'session');");
				}
				super.onScriptContextCreated(event);
			}
			
		});
	}
	
	private void createDeviceBar(Composite parent) {
		deviceBar = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL, GridData.CENTER, true, false);
		deviceBar.setLayoutData(gd);
		
		RowLayout layout = new RowLayout();
		layout.center = true;
		layout.spacing = 10;
		deviceBar.setLayout(layout);
		
		FocusListener focusListener = new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				boolean doSize = false;
				int width = browserGD.widthHint;
				int height = browserGD.heightHint;
				
				if (e.widget == deviceWidth) {
					try {
						width = Integer.parseInt(deviceWidth.getText());
						doSize = true;
					} catch (Exception ex) {
						// TODO: handle exception
					}
				} else if (e.widget == deviceHeight) {
					try {
						height = Integer.parseInt(deviceHeight.getText());
						doSize = true;
					} catch (Exception ex) {
						// TODO: handle exception
					}
				}
//				doSize = false;
				if (doSize) {
					setBrowserSize(width, height);
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				deviceBar.getDisplay().asyncExec(() -> {
					((Text) e.widget).selectAll();
				});
			}
		};
		
		KeyListener keyListener = new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == 13) {
					Event ne = new Event();
					ne.widget = e.widget;
					focusListener.focusLost(new FocusEvent(ne));
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
		};
		
		VerifyListener verifyListener = e -> {
		    String oldS = ((Text) e.widget).getText();
		    String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
				if (!newS.isEmpty() && !newS.equals("-")) {
					try {
						Integer.parseInt(newS);
					} catch (Exception ex) {
						e.doit = false;
					}
				}
		};
		
		new Label(deviceBar, SWT.NONE).setText("Device name:");
		deviceName = new Text(deviceBar, SWT.NONE);
		deviceName.setFont(JFaceResources.getTextFont());
		deviceName.setLayoutData(new RowData(200, SWT.DEFAULT));
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
		new Label(deviceBar, SWT.NONE).setText("Width:");
		deviceWidth = new Text(deviceBar, SWT.NONE);
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
		new Label(deviceBar, SWT.NONE).setText("Height:");
		deviceHeight = new Text(deviceBar, SWT.NONE);
		
		for (Text t: new Text[]{deviceWidth, deviceHeight}) {
			t.setTextLimit(4);
			t.setFont(JFaceResources.getTextFont());
			t.setLayoutData(new RowData(35, SWT.DEFAULT));
			t.addFocusListener(focusListener);
			t.addVerifyListener(verifyListener);
			t.addKeyListener(keyListener);
		}
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
		ToolBar tb = new ToolBar(deviceBar, SWT.NONE);
		ToolItem button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/dbo_save.gif")));
		button.setToolTipText("Save");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String name = deviceName.getText().trim();
				
				if (name.isEmpty()) {
					Engine.logStudio.info("Device name must no be empty.");
					return;
				}
				
				if (findDevice(devicesDefinition, name) != null) {
					Engine.logStudio.info("Cannot override the default device '" + name + "'.");
					return;
				}
				
				JSONObject device = findDevice(devicesDefinitionCustom, name);
				
				try {
					if (device == null) {
						device = new JSONObject();
						device.put("name", name);
						devicesDefinitionCustom.put(device);
					}
					device.put("width", browserGD.widthHint);
					device.put("height", browserGD.heightHint);
					FileUtils.write(new File(Engine.USER_WORKSPACE_PATH, "studio/devices.json"), devicesDefinitionCustom.toString(4), "UTF-8");
					updateDevicesMenu();
				} catch (Exception ex) {
					// TODO: handle exception
				}
			}			
		});
		
		button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/project_delete.gif")));
		button.setToolTipText("Delete");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String name = deviceName.getText().trim();
				
				if (findDevice(devicesDefinition, name) != null) {
					Engine.logStudio.info("Cannot remove the default device '" + name + "'.");
					return;
				}
				
				JSONObject device = findDevice(devicesDefinitionCustom, name);
				
				try {
					if (device != null) {
						devicesDefinitionCustom.remove(device);
						FileUtils.write(new File(Engine.USER_WORKSPACE_PATH, "studio/devices.json"), devicesDefinitionCustom.toString(4), "UTF-8");
						updateDevicesMenu();
					}
				} catch (Exception ex) {
					// TODO: handle exception
				}
			}			
		});
	}

	private void createToolbar(Composite parent) {		
		ToolBar toolbar = new ToolBar(parent, SWT.VERTICAL);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, false, true);
		gd.verticalSpan = 2;
		toolbar.setLayoutData(gd);

		ToolItem item = new ToolItem(toolbar, SWT.DROP_DOWN);
		item.setToolTipText("Select device viewport");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/core/images/mobiledevice_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.ARROW) {
					ToolItem item = (ToolItem) e.widget;
					Rectangle rect = item.getBounds(); 
					Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
					devicesMenu.setLocation(pt);
					devicesMenu.setVisible(true);
				} else {
					boolean visible = !deviceBar.getVisible();
					deviceBar.setVisible(visible);
					GridData gd = (GridData) deviceBar.getLayoutData();
					gd.exclude = !visible;
					deviceBar.getParent().layout();
				}
			}
			
		});
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Change orientation");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setBrowserSize(browserGD.heightHint, browserGD.widthHint);
			}
			
		});
				
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Show debug");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/debug.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSite().getPage().activate(ConvertigoPlugin.getDefault().getMobileDebugView());
			}
			
		});
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Open in default browser");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/statements/images/ContinueWithSiteClipperStatement_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				C8oBrowser.run(() -> {
					String url = browser.getURL();
					if (url.startsWith("http")) {
						org.eclipse.swt.program.Program.launch(url);
					}
				});
			}
			
		});
	}
	
	private void updateDevicesMenu() {
		for (MenuItem m: devicesMenu.getItems()) {
			m.dispose();
		}
		
		SelectionAdapter selectionAdapter = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				deviceName.setText(((MenuItem) e.widget).getText());
				setBrowserSize((int) e.widget.getData("width"), (int) e.widget.getData("height"));
			}
			
		};
		
		for (JSONArray devices: new JSONArray[]{devicesDefinition, devicesDefinitionCustom}) {
			int len = devices.length();
			for (int i = 0; i < len; i++) {
				try {
					JSONObject json = devices.getJSONObject(i);
					MenuItem device = new MenuItem(devicesMenu, SWT.NONE);
					device.addSelectionListener(selectionAdapter);
					device.setText(json.getString("name"));
					device.setData("width", json.getInt("width"));
					device.setData("height", json.getInt("height"));
					if (json.has("desc")) {
						device.setToolTipText(json.getString("desc"));
					}
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}			
		}
	}
	
	private void setBrowserSize(int width, int height) {
		deviceWidth.setText("" + width);
		deviceHeight.setText("" + height);
		browserGD.horizontalAlignment = width < 0 ? GridData.FILL : GridData.CENTER;
		browserGD.verticalAlignment = height < 0 ? GridData.FILL : GridData.CENTER;
		browserScroll.setMinWidth(browserGD.widthHint = browserGD.minimumWidth = width);
		browserScroll.setMinHeight(browserGD.heightHint = browserGD.minimumHeight = height);
		c8oBrowser.getParent().layout();
	}
	
	@Override
	public void setFocus() {
//		c8oBrowser.getBrowserView().requestFocus();
	}

	public void refreshBrowser() {
//		if (browser != null && !browser.isDisposed()) {
//			browser.refresh();
//		}
	}
	
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
//		browser.executeScript("location.href='http://www.convertigo.com'");
//		browser.executeJavaScript("location.href='http://www.convertigo.com'");
//		if (event.getSource() instanceof ISelectionProvider) {
//			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
//			TreeObject treeObject = (TreeObject) selection.getFirstElement();
//			if (treeObject != null) {
//				if (treeObject instanceof MobileUIComponentTreeObject) {
//					TreeParent treeParent = treeObject.getParent();
//					while (treeParent != null) {
//						if (treeParent instanceof MobilePageComponentTreeObject) {
//							PageComponent page = ((MobilePageComponentTreeObject)treeParent).getObject();
//							if (pageEditorInput.is(page)) {
//								getEditorSite().getPage().bringToTop(this);
//							}
//							break;
//						}
//						treeParent = treeParent.getParent();
//					}
//				}
//				else if (treeObject instanceof MobilePageComponentTreeObject) {
//					PageComponent page = ((MobilePageComponentTreeObject)treeObject).getObject();
//					if (pageEditorInput.is(page)) {
//						getEditorSite().getPage().bringToTop(this);
//					}
//				}
//			}
//		}
	}
	
	private void appendOutput(String msg) {
		if (baseUrl == null) {
			C8oBrowser.run(() -> {
				browser.executeJavaScriptAndReturnValue("loader_log").asFunction().invokeAsync(null, msg);
			});
		}
	}
	
	private void launchBuilder() {		
		Engine.execute(() -> {
			try {
				browser.loadHTML(IOUtils.toString(getClass().getResourceAsStream("loader.html"), "UTF-8"));
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
			
			File ionicDir = new File(applicationEditorInput.application.getProject().getDirPath() + "/_private/ionic");
			if (!new File(ionicDir, "node_modules").exists()) {
				try {
					ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder("", "npm", "install", "--progress=false");
					pb.redirectErrorStream(true);
					pb.directory(ionicDir);
					Process p = pb.start();
					processes.add(p);
					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					while ((line = br.readLine()) != null) {
						line = pRemoveEchap.matcher(line).replaceAll("");
						if (StringUtils.isNotBlank(line)) {						
							Engine.logStudio.info(line);
							appendOutput(line);
						}
					}
					Engine.logStudio.info(line);
					appendOutput("\\o/");
				} catch (Exception e) {
					appendOutput(":( " + e);
				}
			}

			try {
				ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder("", "npm", "run", "ionic:serve", "--nobrowser");
				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				Process p = pb.start();
				processes.add(p);
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = br.readLine()) != null) {
					line = pRemoveEchap.matcher(line).replaceAll("");
					if (StringUtils.isNotBlank(line)) {
						Engine.logStudio.info(line);
						appendOutput(line);
						Matcher m = pIsServerRunning.matcher(line);
						if (m.matches()) {
							baseUrl = m.group(1);
							doLoad();
						}
					}
				}
				appendOutput("\\o/");
			} catch (Exception e) {
				appendOutput(":( " + e);
			}
			
		});
	}

	
	private JSONObject findDevice(JSONArray devices, String name) {
		int len = devices.length();
		for (int i = 0; i < len; i++) {
			try {
				JSONObject device = devices.getJSONObject(i);
				if (name.equals(device.getString("name"))) {
					return device;
				}
			} catch (Exception ex) {
			}
		}
		return null;
	}
	
	private void doLoad() {
		if (baseUrl != null) {
			C8oBrowser.run(() -> {
				String url = baseUrl;
				if (pageName != null) {
					url += "#/" + pageName;
				}
				
				browser.loadURL(url);
			});
		}
	}
	
	public String getDebugUrl() {
		return debugUrl;
	}
	
	public void selectPage(String pageName) {
		this.pageName = pageName;
		doLoad();
	}
}
