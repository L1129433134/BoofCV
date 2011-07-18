/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.transform.ii.impl;

import gecv.misc.AutoTypeImage;
import gecv.misc.CodeGeneratorBase;
import gecv.misc.CodeGeneratorUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


/**
 * @author Peter Abeles
 */
public class GenerateImplIntegralImageOps extends CodeGeneratorBase {
	String className = "ImplIntegralImageOps";

	PrintStream out;

	public GenerateImplIntegralImageOps() throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(className + ".java"));
	}

	@Override
	public void generate() throws FileNotFoundException {
		printPreamble();

		printFuncs(AutoTypeImage.F32, AutoTypeImage.F32);
		printFuncs(AutoTypeImage.U8, AutoTypeImage.S32);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(CodeGeneratorUtil.copyright);
		out.print("package gecv.alg.transform.ii.impl;\n" +
				"\n" +
				"import gecv.struct.image.*;\n" +
				"\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Compute the integral image for different types of input images.\n" +
				" * </p>\n" +
				" * \n" +
				" * <p>\n" +
				" * DO NOT MODIFY: Generated by {@link GenerateImplIntegralImageOps}.\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" {\n\n");
	}

	private void printFuncs( AutoTypeImage imageIn , AutoTypeImage imageOut ) {

		String sumType = imageOut.getSumType();
		String bitWise = imageIn.getBitWise();
		String typeCast = imageOut.getTypeCastFromSum();

		out.print("\tpublic static void process( final "+imageIn.getImageName()+" input , final "+imageOut.getImageName()+" transformed )\n" +
				"\t{\n" +
				"\t\tint indexSrc = input.startIndex;\n" +
				"\t\tint indexDst = transformed.startIndex;\n" +
				"\t\tint end = indexSrc + input.width;\n" +
				"\n" +
				"\t\t"+sumType+" total = 0;\n" +
				"\t\tfor( ; indexSrc < end; indexSrc++ ) {\n" +
				"\t\t\ttransformed.data[indexDst++] = "+typeCast+"total += input.data[indexSrc]"+bitWise+";\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor( int y = 1; y < input.height; y++ ) {\n" +
				"\t\t\tindexSrc = input.startIndex + input.stride*y;\n" +
				"\t\t\tindexDst = transformed.startIndex + transformed.stride*y;\n" +
				"\t\t\tint indexPrev = indexDst - transformed.stride;\n" +
				"\n" +
				"\t\t\tend = indexSrc + input.width;\n" +
				"\n" +
				"\t\t\ttotal = 0;\n" +
				"\t\t\tfor( ; indexSrc < end; indexSrc++ ) {\n" +
				"\t\t\t\ttotal +=  input.data[indexSrc]"+bitWise+";\n" +
				"\t\t\t\ttransformed.data[indexDst++] = transformed.data[indexPrev++] + total;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateImplIntegralImageOps app = new GenerateImplIntegralImageOps();
		app.generate();
	}
}
