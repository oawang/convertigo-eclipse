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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import com.teamdev.jxbrowser.chromium.ContextMenuHandler;
import com.teamdev.jxbrowser.chromium.ContextMenuParams;
import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMDocument;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMNode;
import com.teamdev.jxbrowser.chromium.dom.DOMNodeAtPoint;
import com.teamdev.jxbrowser.chromium.events.ScriptContextAdapter;
import com.teamdev.jxbrowser.chromium.events.ScriptContextEvent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.DatabaseObjectFoundException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

public class ApplicationComponentEditor extends EditorPart implements ISelectionChangedListener {

	private ProjectExplorerView projectExplorerView = null;
	private ApplicationComponentEditorInput applicationEditorInput;
	
	private ScrolledComposite browserScroll;
	private GridData browserGD;
	private Menu devicesMenu;
	private Composite deviceBar;
	
	private ToolItem deviceOsToolItem;
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
	
	private DeviceOS deviceOS = DeviceOS.android;
	private ZoomFactor zoomFactor = ZoomFactor.z100;
	private double dpiFactorX = 1;
	private double dpiFactorY = 1;
	
	private static Pattern pIsServerRunning = Pattern.compile(".*?server running: (http\\S*).*");
	private static Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
	private static Pattern pPriority = Pattern.compile("class(\\d+)");
	
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

	private void saveDeviceBar() {
		try {
			if (!deviceBar.getParent().isVisible()) {
				return;
			}
			JSONObject device = new JSONObject();
			device.put("visible", deviceBar.isVisible());
			device.put("name", deviceName.getText());
			device.put("width", NumberUtils.toInt(deviceWidth.getText(), -1));
			device.put("height", NumberUtils.toInt(deviceHeight.getText(), -1));
			device.put("zoom", zoomFactor.percent());
			device.put("os", deviceOS.name());
			FileUtils.write(new File(Engine.USER_WORKSPACE_PATH, "studio/device.json"), device.toString(4), "UTF-8");
		} catch (Exception e) {
			// TODO: handle exception
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
		saveDeviceBar();
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		{
			Point dpi = parent.getDisplay().getDPI();
			dpiFactorX = dpi.x / 96f;
			dpiFactorY = dpi.y / 96f; 
		}
		
		DeviceOS.init(parent.getDisplay());
		
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
		
		try {
			JSONObject device = new JSONObject(FileUtils.readFileToString(new File(Engine.USER_WORKSPACE_PATH, "studio/device.json"), "UTF-8"));
			deviceName.setText(device.getString("name"));
			deviceWidth.setText("" + device.getInt("width"));
			deviceHeight.setText("" + device.getInt("height"));
			zoomFactor = ZoomFactor.get(device.getInt("zoom"));
			deviceOS = DeviceOS.valueOf(device.getString("os"));
			setDeviceBarVisible(device.getBoolean("visible"));			
			updateBrowserSize();
		} catch (Exception e) {
			devicesMenu.getItems()[0].notifyListeners(SWT.Selection, new Event());
		}
		
		launchBuilder(false);
		
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
					try {
						browser.executeJavaScript(
							"sessionStorage.setItem('_c8ocafsession_storage_mode', 'local');\n"
							+ "navigator.__defineGetter__('userAgent', function(){ return '" + deviceOS.agent() + "'});\n"
							+ IOUtils.toString(getClass().getResourceAsStream("inject.js"), "UTF-8")
						);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				browser.setZoomLevel(zoomFactor.zoomLevel());
				super.onScriptContextCreated(event);
			}
			
		});
		
		browser.setContextMenuHandler(new ContextMenuHandler() {
			
			@Override
			public void showContextMenu(ContextMenuParams ctx) {
				java.awt.Point location = ctx.getLocation();
				location.setLocation(location.getX() * dpiFactorX, location.getY() * dpiFactorY);
				DOMNodeAtPoint nodeAP = browser.getNodeAtPoint(location);
				DOMNode node = nodeAP.getNode();
				while (!(node == null || node instanceof DOMElement)) {
					node = node.getParent();
				}
				while (node != null) {
					DOMElement element = (DOMElement) node;
					String classes = element.getAttribute("class");
					Matcher mPriority = pPriority.matcher(classes);
					if (mPriority.find()) {
						try {
							node = null;
							long priority = Long.parseLong(mPriority.group(1));
							new WalkHelper() {

								@Override
								protected void walk(DatabaseObject databaseObject) throws Exception {
									if (databaseObject.priority == priority) {
										throw new DatabaseObjectFoundException(databaseObject);
									}
									super.walk(databaseObject);
								}
								
							}.init(applicationEditorInput.application);
						} catch (DatabaseObjectFoundException e) {
							DatabaseObject databaseObject = e.getDatabaseObject();

							c8oBrowser.getDisplay().asyncExec(() -> ConvertigoPlugin.getDefault().getProjectExplorerView().objectSelected(new CompositeEvent(databaseObject)));
							
							if (databaseObject instanceof MobileComponent) {
								highlightComponent((MobileComponent) databaseObject);
							}
						} catch (Exception e) {
							e.printStackTrace();							
						}
					} else {
						node = node.getParent();
					}
				}
			}
		});
	}
	
	private void createDeviceBar(Composite parent) {
		deviceBar = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL, GridData.CENTER, true, false);
		deviceBar.setLayoutData(gd);
		
		gd.exclude = true;
		deviceBar.setVisible(false);
		
		RowLayout layout = new RowLayout();
		layout.center = true;
		layout.spacing = 10;
		deviceBar.setLayout(layout);
		
		FocusListener focusListener = new FocusAdapter() {
			
			@Override
			public void focusLost(FocusEvent e) {
				updateBrowserSize();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				deviceBar.getDisplay().asyncExec(() -> ((Text) e.widget).selectAll());
			}
			
		};
		
		KeyListener keyListener = new KeyAdapter() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == 13) {
					updateBrowserSize();
				}
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
		
		ToolBar tb = new ToolBar(deviceBar, SWT.NONE);
		deviceOsToolItem = new ToolItem(tb, SWT.DROP_DOWN);
		final Menu mOS = new Menu(tb);
		SelectionListener selectionListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDeviceOS((DeviceOS) e.widget.getData());
			}
			
		};
		
		for (DeviceOS device: DeviceOS.values()) {
			MenuItem menuItem = new MenuItem(mOS, SWT.NONE);
			menuItem.setData(device);
			menuItem.setImage(device.image());
			menuItem.setText(device.displayName());
			menuItem.addSelectionListener(selectionListener);			
		}
		
		deviceOsToolItem.setToolTipText("Select device OS.");
		deviceOsToolItem.setImage(deviceOS.image());
		deviceOsToolItem.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ToolItem item = (ToolItem) e.widget;
				Rectangle rect = item.getBounds(); 
				Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
				mOS.setLocation(pt);
				mOS.setVisible(true);
			}
			
		});
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
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
		
		tb = new ToolBar(deviceBar, SWT.NONE);
		ToolItem button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/zoom_out.png")));
		button.setToolTipText("Zoom out");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				zoomFactor = zoomFactor.out();
				updateBrowserSize();
			}
		});
		
		button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/zoom_reset.png")));
		button.setToolTipText("Zoom reset");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				zoomFactor = ZoomFactor.z100;
				updateBrowserSize();
			}
		});
		
		button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/zoom_in.png")));
		button.setToolTipText("Zoom in");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				zoomFactor = zoomFactor.in();
				updateBrowserSize();
			}
		});
		
		new ToolItem(tb, SWT.SEPARATOR);
		
		button = new ToolItem(tb, SWT.PUSH);
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
				
				C8oBrowser.run(() -> {
					JSONObject device = findDevice(devicesDefinitionCustom, name);
					
					try {
						if (device == null) {
							device = new JSONObject();
							device.put("name", name);
							devicesDefinitionCustom.put(device);
						}
						device.put("width", NumberUtils.toInt(deviceWidth.getText(), -1));
						device.put("height", NumberUtils.toInt(deviceHeight.getText(), -1));
						device.put("zoom", zoomFactor.percent());
						device.put("os", deviceOS.name());
						FileUtils.write(new File(Engine.USER_WORKSPACE_PATH, "studio/devices.json"), devicesDefinitionCustom.toString(4), "UTF-8");
						
						parent.getDisplay().asyncExec(() -> updateDevicesMenu());
					} catch (Exception ex) {
						// TODO: handle exception
					}
				});
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
		item.setToolTipText("Select device viewport. Click to toggle the custom device bar.");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/core/images/mobiledevice_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.ARROW) {
					ToolItem item = (ToolItem) e.widget;
					Rectangle rect = item.getBounds(); 
					Point pt = item.getParent().toDisplay(new Point(rect.x + 8, rect.y + 8));
					devicesMenu.setLocation(pt);
					devicesMenu.setVisible(true);
				} else {
					setDeviceBarVisible(!deviceBar.getVisible());
				}
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Change orientation");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				String width = deviceWidth.getText();
				deviceWidth.setText(deviceHeight.getText());
				deviceHeight.setText(width);
				updateBrowserSize();
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
				
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Refresh");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/refresh.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				doReload();
			}
			
		});
				
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Back");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/undo.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				C8oBrowser.run(() -> {
					int index = browser.getCurrentNavigationEntryIndex();
					if (index > 2) {
						browser.goBack();
					}
				});
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
				
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Remove highlight");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/write_wait_zone.d.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				C8oBrowser.run(() -> browser.executeJavaScript("_c8o_remove_all_overlay()"));
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
				
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
		
		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Run npm install");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/show_blocks.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				MessageDialog dialog = new MessageDialog(
					null, "Run npm install ?",
					null, "Do you want to perform a 'npm install' that can take time.",
					MessageDialog.QUESTION,
					new String[] {"Update", "Re-install", "Cancel"}, 0
				);
				int result = dialog.open();
				if (result == 1) {
					File nodeModules = new File(applicationEditorInput.application.getProject().getDirPath() + "/_private/ionic/node_modules");
					FileUtils.deleteQuietly(nodeModules);
				}
				if (result < 2) {
					launchBuilder(true);
				}
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
				deviceName.setText(((MenuItem) e.widget).getText().substring(2));
				deviceWidth.setText("" + e.widget.getData("width"));
				deviceHeight.setText("" + e.widget.getData("height"));
				zoomFactor = (ZoomFactor) e.widget.getData("zoom");
				setDeviceOS((DeviceOS) e.widget.getData("os"));
				
				updateBrowserSize();
			}
			
		};
		
		for (JSONArray devices: new JSONArray[]{devicesDefinition, devicesDefinitionCustom}) {
			int len = devices.length();
			for (int i = 0; i < len; i++) {
				MenuItem device = new MenuItem(devicesMenu, SWT.NONE);
				try {
					JSONObject json = devices.getJSONObject(i);
					
					DeviceOS os = DeviceOS.android;
					try {
						os = DeviceOS.valueOf(json.getString("os"));
					} catch (Exception e) {	}
					
					device.addSelectionListener(selectionAdapter);
					device.setText((devices == devicesDefinition ? "🔒 " : "👤 ") + json.getString("name"));
					device.setImage(os.image());
					device.setData("width", json.getInt("width"));
					device.setData("height", json.getInt("height"));
					device.setData("zoom", ZoomFactor.get(json.has("zoom") ? json.getInt("zoom") : 100));
					device.setData("os", os);
					
					if (json.has("desc")) {
						device.setToolTipText(json.getString("desc"));
					}
				} catch (JSONException e1) {
					device.dispose();
				}
			}			
		}
	}
	
	private void updateBrowserSize() {
		int width = NumberUtils.toInt(deviceWidth.getText(), -1);
		int height = NumberUtils.toInt(deviceHeight.getText(), -1);
		
		width = zoomFactor.swt(width, dpiFactorX);
		height = zoomFactor.swt(height, dpiFactorY);
		browserGD.horizontalAlignment = width < 0 ? GridData.FILL : GridData.CENTER;
		browserGD.verticalAlignment = height < 0 ? GridData.FILL : GridData.CENTER;
		browserScroll.setMinWidth(browserGD.widthHint = browserGD.minimumWidth = width);
		browserScroll.setMinHeight(browserGD.heightHint = browserGD.minimumHeight = height);
		c8oBrowser.getParent().layout();
		
		C8oBrowser.run(() -> {
			browser.executeJavaScript("try {_c8o_remove_all_overlay()} catch(e){}");
			browser.setZoomLevel(zoomFactor.zoomLevel());
		});
	}
	
	@Override
	public void setFocus() {
		c8oBrowser.setFocus();
	}
	
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		
	}
	
	private void appendOutput(String msg) {
		C8oBrowser.run(() -> {
			if (browser.getURL().equals("about:blank")) {
				browser.executeJavaScriptAndReturnValue("loader_log").asFunction().invokeAsync(null, msg);
			}
		});
	}
	
	private void launchBuilder(boolean forceInstall) {				
		Engine.execute(() -> {
			try {
				browser.loadHTML(IOUtils.toString(getClass().getResourceAsStream("loader.html"), "UTF-8"));
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
			
			File ionicDir = new File(applicationEditorInput.application.getProject().getDirPath() + "/_private/ionic");
			File nodeModules = new File(ionicDir, "node_modules");
			if (forceInstall || !nodeModules.exists()) {
				boolean[] running = {true};
				try {
					ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder("", "npm", "install");//, "--progress=false");
					pb.redirectErrorStream(true);
					pb.directory(ionicDir);
					Process p = pb.start();
					Engine.execute(() -> {
						try {
							while (running[0] && !nodeModules.exists()) {
								appendOutput("Waiting for node_modules creation");
								Thread.sleep(1000);
							}
							while (running[0]) {
								appendOutput("node_modules: " + FileUtils.byteCountToDisplaySize(FileUtils.sizeOfAsBigInteger(nodeModules)));
								Thread.sleep(1000);
							} 
						} catch (Exception e) {
							appendOutput("Something wrong during the install: " + e);
						}
					});
					processes.add(p);
					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					while ((line = br.readLine()) != null) {
						line = pRemoveEchap.matcher(line).replaceAll("");
						if (StringUtils.isNotBlank(line)) {
							running[0] = false;				
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
	
	private void setDeviceBarVisible(boolean visible) {
		deviceBar.setVisible(visible);
		GridData gd = (GridData) deviceBar.getLayoutData();
		gd.exclude = !visible;
		deviceBar.getParent().layout();
	}
	
	private void doLoad() {
		if (baseUrl != null) {
			C8oBrowser.run(() -> {
				String url = baseUrl;
				if (pageName != null) {
					url += "#/" + pageName;
				}
				if (!browser.getURL().equals(url)) {
					browser.loadURL(url);
				}
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

	public void highlightComponent(MobileComponent mobileComponent) {
		C8oBrowser.run(() -> {
			if (mobileComponent instanceof UIComponent) {
				selectPage(((UIComponent) mobileComponent).getPage().getName());
			}
			DOMDocument doc = browser.getDocument();
			MobileComponent mc = mobileComponent;
			while (doc.findElements(By.className("class" + mc.priority)).isEmpty()) {
				DatabaseObject parent = mc.getParent();
				if (parent instanceof MobileComponent) {
					mc = (MobileComponent) parent;
				} else {
					return;
				}
			}
			browser.executeJavaScript("_c8o_highlight_class('class" + mc.priority + "');");
		});
	}
	
	private void doReload() {
		C8oBrowser.run(() -> {
			if (!browser.getURL().equals("about:blank")) {
				browser.reload();
			}
		});
	}
	
	private void setDeviceOS(DeviceOS deviceOS) {
		if (!this.deviceOS.equals(deviceOS)) {
			this.deviceOS = deviceOS;
			deviceOsToolItem.setImage(deviceOS.image());
			doReload();
		}
	}
}
