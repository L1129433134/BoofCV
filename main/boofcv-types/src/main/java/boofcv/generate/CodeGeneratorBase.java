/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.generate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


/**
 * <p>Base class for code generators.</p>
 *
 * @author Peter Abeles
 */
public abstract class CodeGeneratorBase {

	protected PrintStream out;
	protected String className;
	/**
	 * If true the output will be in the source directory, overwriting existing code
	 */
	protected boolean overwrite = true;

	public CodeGeneratorBase(boolean useDefaultName) {
		if( useDefaultName ) {
			autoSelectName();
		}
	}

	public CodeGeneratorBase() {
		autoSelectName();
	}

	public void parseArguments( String []args ) {
		// todo fill this in later
	}

	public void autoSelectName() {
		className = getClass().getSimpleName();
		if( className.startsWith("Generate") ) {
			int l = "Generate".length();
			className = className.substring(l);
			try {
				initFile();
			} catch( FileNotFoundException e ) {
				throw new RuntimeException(e);
			}
		} else {
			System.out.println("Class name doesn't start with Generate");
		}
	}

	protected void printParallel(String var, String lower, String upper , String body ) {
		out.println();
		out.printf("\t\t//CONCURRENT_BELOW BoofConcurrency.range(%s, %s, %s -> {\n",lower,upper,var);
		out.printf("\t\tfor( int %s = %s; %s < %s; %s++ ) {\n",var,lower,var,upper,var);
		out.print(body);
		out.print("\t\t}\n");
		out.print("\t\t//CONCURRENT_ABOVE });\n");

	}
	protected void printParallelBlock(String var0 , String var1, String lower, String upper , String minBlock , String body ) {
		out.println();

		out.printf("\t\t//CONCURRENT_BELOW BoofConcurrency.blocks(%s, %s, %s,(%s,%s)->{\n",lower,upper,minBlock,var0,var1);
		out.printf("\t\tfinal int %s = %s, %s = %s;\n",var0,lower,var1,upper);
		out.print(body);
		out.print("\t\t//CONCURRENT_INLINE });\n");
	}

	/**
	 * Creates 
	 *
	 * @throws FileNotFoundException
	 */
	public abstract void generate() throws FileNotFoundException;

	public void initFile() throws FileNotFoundException {
		File file = new File(className + ".java");
		if( overwrite ) {
			file = new File(packageToPath(getClass()),file.getName());
			if( !file.getParentFile().exists() ) {
				if( !file.getParentFile().mkdirs() ) {
					throw new RuntimeException("Failed to create path "+file.getParentFile().getPath());
				}
			}
		}

		System.out.println(file.getAbsolutePath());

		out = new PrintStream(new FileOutputStream(file));
		out.print(CodeGeneratorUtil.copyright);
		out.println();
		out.println("package " + getPackage() + ";");
		out.println();
	}

	public File packageToPath( Class c ) {
		String name = c.getCanonicalName();

		String words[] = name.split("\\.");
		String path = "src/main/java/";
		for (int i = 0; i < words.length-1; i++) {
			path += words[i] + "/";
		}
		return new File(path);
	}

	public void setOutputFile( String className ) throws FileNotFoundException {
		if( this.className != null )
			throw new IllegalArgumentException("ClassName already set.  Out of date code?");
		this.className = className;
		initFile();
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public String generateDocString() {
		return  " * <p>\n" +
				" * DO NOT MODIFY. This code was automatically generated by "+getClass().getSimpleName()+".\n"+
				" * <p>\n";
	}

	public String generatedAnnotation() {
		return "@Generated(\""+getClass().getName()+"\")\n";
	}

	public String getPackage() {
		return getClass().getPackage().getName();
	}
}
