/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.cf.ds;

import java.io.InputStream;
import java.io.InputStreamReader;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.orion.server.cf.ds.objects.Plan;
import org.eclipse.orion.server.cf.manifest.v2.ManifestParseTree;
import org.eclipse.orion.server.cf.manifest.v2.utils.ManifestConstants;
import org.eclipse.orion.server.cf.manifest.v2.utils.ManifestUtils;
import org.eclipse.orion.server.core.IOUtilities;
import org.eclipse.osgi.util.NLS;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeJSDeploymentPlanner implements IDeploymentPlanner {
	protected final Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.cf"); //$NON-NLS-1$
	protected static String TYPE = "node.js"; //$NON-NLS-1$

	protected static String PACKAGE_JSON = "package.json"; //$NON-NLS-1$
	protected static String SCRIPTS = "scripts"; //$NON-NLS-1$
	protected static String START = "start"; //$NON-NLS-1$

	protected static String SERVER_JS = "server.js"; //$NON-NLS-1$
	protected static String APP_JS = "app.js"; //$NON-NLS-1$

	protected static String NODE_SERVER_JS = "node server.js"; //$NON-NLS-1$
	protected static String NODE_APP_JS = "node app.js"; //$NON-NLS-1$

	@Override
	public String getId() {
		return getClass().getSimpleName();
	}

	protected String getApplicationName(IFileStore contentLocation) {
		return contentLocation.fetchInfo().getName();
	}

	protected void set(ManifestParseTree application, String property, String defaultValue) {
		if (application.has(property))
			return;
		else
			application.put(property, defaultValue);
	}

	@Override
	public Plan getDeploymentPlan(IFileStore contentLocation, ManifestParseTree manifest) {

		/* a present package.json file determines a node.js application */
		IFileStore packageStore = contentLocation.getChild(PACKAGE_JSON);
		if (!packageStore.fetchInfo().exists())
			return null;

		/* do not support multi-aplication manifests */
		if (manifest != null && ManifestUtils.hasMultipleApplications(manifest))
			return null;

		try {

			if (manifest == null)
				manifest = ManifestUtils.createBoilerplate(getApplicationName(contentLocation));

			ManifestParseTree application = manifest.get(ManifestConstants.APPLICATIONS).get(0);
			String defaultName = getApplicationName(contentLocation);

			set(application, ManifestConstants.NAME, defaultName);
			set(application, ManifestConstants.HOST, ManifestUtils.slugify(defaultName));

			set(application, ManifestConstants.MEMORY, ManifestUtils.DEFAULT_MEMORY);
			set(application, ManifestConstants.INSTANCES, ManifestUtils.DEFAULT_INSTANCES);
			set(application, ManifestConstants.PATH, ManifestUtils.DEFAULT_PATH);

			/* node.js application require a start command */
			if (application.has(ManifestConstants.COMMAND))
				return new Plan(getId(), TYPE, manifest);

			InputStream is = null;
			try {

				is = packageStore.openInputStream(EFS.NONE, null);
				JSONObject packageJSON = new JSONObject(new JSONTokener(new InputStreamReader(is)));
				if (packageJSON.has(SCRIPTS)) {
					JSONObject scripts = packageJSON.getJSONObject(SCRIPTS);
					if (scripts.has(START)) {
						application.put(ManifestConstants.COMMAND, scripts.getString(START));
						return new Plan(getId(), TYPE, manifest);
					}
				}

			} catch (JSONException ex) {
				/* can't parse the package.json, fail */
				return null;

			} finally {
				IOUtilities.safeClose(is);
			}

			/* look for server.js or app.js files */
			IFileStore serverJS = contentLocation.getChild(SERVER_JS);
			if (serverJS.fetchInfo().exists()) {
				application.put(ManifestConstants.COMMAND, NODE_SERVER_JS);
				return new Plan(getId(), TYPE, manifest);
			}

			IFileStore appJS = contentLocation.getChild(APP_JS);
			if (appJS.fetchInfo().exists()) {
				application.put(ManifestConstants.COMMAND, NODE_APP_JS);
				return new Plan(getId(), TYPE, manifest);
			}

			/* could not deduce command, mark as required */
			Plan plan = new Plan(getId(), TYPE, manifest);
			plan.addRequired(ManifestConstants.COMMAND);
			return plan;

		} catch (Exception ex) {
			String msg = NLS.bind("Failed to handle generic deployment plan for {0}", contentLocation.toString()); //$NON-NLS-1$
			logger.error(msg, ex);
			return null;
		}
	}
}
