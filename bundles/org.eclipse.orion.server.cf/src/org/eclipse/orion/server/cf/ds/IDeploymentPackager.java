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

import java.io.File;
import java.io.IOException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;

public interface IDeploymentPackager {

	public String getId();

	public File getDeploymentPackage(IFileStore contentLocation) throws IOException, CoreException;

}
