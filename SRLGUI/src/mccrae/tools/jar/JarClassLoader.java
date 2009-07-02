/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mccrae.tools.jar;

import java.io.IOException;

/**
 *
 * @author john
 */
public class JarClassLoader extends MultiClassLoader
    {
    private JarResources	jarResources;

    public JarClassLoader (String jarName) throws IOException
	{
	// Create the JarResource and suck in the .jar file.
	jarResources = new JarResources (jarName);
	}

    protected byte[] loadClassBytes (String className)
	{
	// Support the MultiClassLoader's class name munging facility.
	className = formatClassName (className);

	// Attempt to get the class data from the JarResource.
	return (jarResources.getResource (className));
	}


}
