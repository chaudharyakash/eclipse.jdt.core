/*******************************************************************************
 * Copyright (c) 2014 GK Software AG, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.Test;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.tests.util.Util;
import org.osgi.framework.Bundle;

public class ExternalAnnotations18Test extends ModifyingResourceTests {

	private IJavaProject project;
	private IPackageFragmentRoot root;
	private String ANNOTATION_LIB;

	private static final String MY_MAP_CONTENT = 
			"package libs;\n" + 
			"\n" + 
			"public interface MyMap<K,V> {\n" + 
			"	V get(Object key);\n" + 
			"	V put(K key, V val);\n" + 
			"	V remove(Object key);\n" + 
			"}\n";

	public ExternalAnnotations18Test(String name) {
		super(name);
	}
	
// Use this static initializer to specify subset for tests
// All specified tests which do not belong to the class are skipped...
	static {
		// Names of tests to run: can be "testBugXXXX" or "BugXXXX")
//		TESTS_PREFIX = "testLibsWithTypeParameters";
//		TESTS_NAMES = new String[] {"test3"};
//		TESTS_NUMBERS = new int[] { 23, 28, 38 };
//		TESTS_RANGE = new int[] { 21, 38 };
	}
	public static Test suite() {
		return buildModelTestSuite(ExternalAnnotations18Test.class, BYTECODE_DECLARATION_ORDER);
	}

	/**
	 * @deprecated indirectly uses deprecated class PackageAdmin
	 */
	public void setUpSuite() throws Exception {
		super.setUpSuite();
		
		Bundle[] bundles = org.eclipse.jdt.core.tests.Activator.getPackageAdmin().getBundles("org.eclipse.jdt.annotation", "[2.0.0,3.0.0)");
		File bundleFile = FileLocator.getBundleFile(bundles[0]);
		this.ANNOTATION_LIB = bundleFile.isDirectory() ? bundleFile.getPath()+"/bin" : bundleFile.getPath();
	}
	
	public String getSourceWorkspacePath() {
		// we read individual projects from within this folder:
		return super.getSourceWorkspacePath()+"/ExternalAnnotations18";
	}

	void setupJavaProject(String name) throws CoreException, IOException {
		this.project = setUpJavaProject(name, "1.8"); //$NON-NLS-1$
		addLibraryEntry(this.project, this.ANNOTATION_LIB, false);
		Map options = this.project.getOptions(true);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		this.project.setOptions(options);

		IPackageFragmentRoot[] roots = this.project.getAllPackageFragmentRoots();
		int count = 0;
		for (int i = 0, max = roots.length; i < max; i++) {
			final IPackageFragmentRoot packageFragmentRoot = roots[i];
			switch(packageFragmentRoot.getKind()) {
				case IPackageFragmentRoot.K_SOURCE :
					count++;
					if (this.root == null) {
						this.root = packageFragmentRoot;
					}
			}
		}
		assertEquals("Wrong value", 1, count); //$NON-NLS-1$
		assertNotNull("Should not be null", this.root); //$NON-NLS-1$
	}
	
	void myCreateJavaProject(String name) throws CoreException {
		this.project = createJavaProject(name, new String[]{"src"}, new String[]{"JCL18_LIB"}, null, null, "bin", null, null, null, "1.8");
		addLibraryEntry(this.project, this.ANNOTATION_LIB, false);
		Map options = this.project.getOptions(true);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		this.project.setOptions(options);

		IPackageFragmentRoot[] roots = this.project.getAllPackageFragmentRoots();
		int count = 0;
		for (int i = 0, max = roots.length; i < max; i++) {
			final IPackageFragmentRoot packageFragmentRoot = roots[i];
			switch(packageFragmentRoot.getKind()) {
				case IPackageFragmentRoot.K_SOURCE :
					count++;
					if (this.root == null) {
						this.root = packageFragmentRoot;
					}
			}
		}
		assertEquals("Wrong value", 1, count); //$NON-NLS-1$
		assertNotNull("Should not be null", this.root); //$NON-NLS-1$
	}
	
	protected void tearDown() throws Exception {
		this.project.getProject().delete(true, true, null);
		this.project = null;
		this.root = null;
		super.tearDown();
	}
	
	// TODO: using this copy from AttachedJavadocTests test also programmatically setting the external annotation location:
	private void setExternalAnnotationsAttribute(String folderName) throws JavaModelException {
		IClasspathEntry[] entries = this.project.getRawClasspath();
		IResource resource = this.project.getProject().findMember("/"+folderName+"/"); //$NON-NLS-1$
		assertNotNull("annotations folder cannot be null", resource); //$NON-NLS-1$
		URI locationURI = resource.getLocationURI();
		assertNotNull("annotations folder cannot be null", locationURI); //$NON-NLS-1$
		URL annotationsUrl = null;
		try {
			annotationsUrl = locationURI.toURL();
		} catch (MalformedURLException e) {
			assertTrue("Should not happen", false); //$NON-NLS-1$
		} catch(IllegalArgumentException e) {
			assertTrue("Should not happen", false); //$NON-NLS-1$
		}
		IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, annotationsUrl.toExternalForm());
		for (int i = 0, max = entries.length; i < max; i++) {
			final IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
					&& entry.getContentKind() == IPackageFragmentRoot.K_BINARY
					&& "/AttachedJavadocProject/lib/test6.jar".equals(entry.getPath().toString())) { //$NON-NLS-1$
				entries[i] = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), new IClasspathAttribute[] { attribute}, entry.isExported());
			}
		}
		this.project.setRawClasspath(entries, null);
	}

	protected void addLibraryWithExternalAnnotations(
			IJavaProject javaProject,
			String jarName,
			String externalAnnotationPath,
			String[] pathAndContents,
			String compliance,
			Map options) throws CoreException, IOException
	{
		createLibrary(javaProject, jarName, null, pathAndContents, null, compliance, options);
		String jarPath = '/' + javaProject.getProject().getName() + '/' + jarName;
		IClasspathEntry entry = JavaCore.newLibraryEntry(
				new Path(jarPath),
				null/*src attach*/,
				null/*src attach root*/,
				new Path(externalAnnotationPath),
				null/*access rules*/,
				null/*extraAttributes*/,
				false/*exported*/);
		addClasspathEntry(this.project, entry);
	}

	private void assertNoMarkers(IMarker[] markers) throws CoreException {
		for (int i = 0; i < markers.length; i++)
			System.err.println("Unexpected marker: "+markers[i].getAttributes().entrySet());
		assertEquals("Number of markers", 0, markers.length);
	}
	
	private void assertNoProblems(IProblem[] problems) throws CoreException {
		for (int i = 0; i < problems.length; i++)
			System.err.println("Unexpected marker: "+problems[i]);
		assertEquals("Number of markers", 0, problems.length);
	}

	private void assertProblems(IProblem[] problems, String[] messages, int[] lines) throws CoreException {
		int nMatch = 0;
		for (int i = 0; i < problems.length; i++) {
			for (int j = 0; j < messages.length; j++) {
				if (messages[j] == null) continue;
				if (problems[i].toString().equals(messages[j])
						&& problems[i].getSourceLineNumber() == lines[j]) {
					messages[j] = null;
					problems[i] = null;
					nMatch++;
					break;
				}
			}
		}
		for (int i = 0; i < problems.length; i++) {
			if (problems[i] != null)
				fail("Unexpected problem "+problems[i]+" at "+problems[i].getSourceLineNumber());
		}
		for (int i = 0; i < messages.length; i++) {
			if (messages[i] != null)
				System.err.println("Unmatched problem "+messages[i]);
		}
		assertEquals("Number of problems", messages.length, nMatch);
	}

	/** Perform full build. */
	public void test1FullBuild() throws Exception {
		setupJavaProject("Test1");
		addLibraryWithExternalAnnotations(this.project, "lib1.jar", "annots", new String[] {
				"/UnannotatedLib/libs/MyMap.java",
				MY_MAP_CONTENT
			}, "1.8", null);
		this.project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		IMarker[] markers = this.project.getProject().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
		assertNoMarkers(markers);
	}

	/** Reconcile an individual CU. */
	public void test1Reconcile() throws Exception {
		setupJavaProject("Test1");
		addLibraryWithExternalAnnotations(this.project, "lib1.jar", "annots", new String[] {
				"/UnannotatedLib/libs/MyMap.java",
				MY_MAP_CONTENT
			}, "1.8", null);
		IPackageFragment fragment = this.root.getPackageFragment("test1");
		ICompilationUnit unit = fragment.getCompilationUnit("Test1.java").getWorkingCopy(new NullProgressMonitor());
		CompilationUnit reconciled = unit.reconcile(AST.JLS8, true, null, new NullProgressMonitor());
		IProblem[] problems = reconciled.getProblems();
		assertNoProblems(problems);
	}
	
	public void testLibs1() throws Exception {
		myCreateJavaProject("TestLibs");
		addLibraryWithExternalAnnotations(this.project, "lib1.jar", "annots", new String[] {
				"/UnannotatedLib/libs/Lib1.java",
				"package libs;\n" + 
				"\n" + 
				"import java.util.Collection;\n" + 
				"import java.util.Iterator;\n" + 
				"\n" + 
				"public interface Lib1 {\n" + 
				"	<T> Iterator<T> unconstrainedTypeArguments1(Collection<T> in);\n" + 
				"	Iterator<String> unconstrainedTypeArguments2(Collection<String> in);\n" + 
				"	<T> Iterator<? extends T> constrainedWildcards(Collection<? extends T> in);\n" + 
				"	<T extends Collection<?>> T constrainedTypeParameter(T in);\n" + 
				"}\n"
			}, "1.8", null);
		new File(this.project.getProject().getLocation().toString()+"/annots/libs/").mkdirs();
		Util.createFile(this.project.getProject().getLocation().toString()+"/annots/libs/Lib1.eea", 
				"interface libs/Lib1\n" + 
				"\n" + 
				"unconstrainedTypeArguments1\n" + 
				" <T:Ljava/lang/Object;>(Ljava/util/Collection<TT;>;)Ljava/util/Iterator<TT;>;\n" + 
				" <T:Ljava/lang/Object;>(Ljava/util/Collection<T0T;>;)Ljava/util/Iterator<TT;>;\n" + 
				"\n" + 
				"unconstrainedTypeArguments2\n" + 
				" (Ljava/util/Collection<Ljava/lang/String;>;)Ljava/util/Iterator<Ljava/lang/String;>;\n" + 
				" (Ljava/util/Collection<Ljava/lang/String;>;)Ljava/util/Iterator<L1java/lang/String;>;\n" + 
				"constrainedWildcards\n" +
				" <T:Ljava/lang/Object;>(Ljava/util/Collection<+TT;>;)Ljava/util/Iterator<+TT;>;\n" +
				" <T:Ljava/lang/Object;>(Ljava/util/Collection<+T0T;>;)Ljava/util/Iterator<+T1T;>;\n" +
				"constrainedTypeParameter\n" +
				" <T::Ljava/util/Collection<*>;>(TT;)TT;\n" +
				" <T::Ljava/util/Collection<*>;>(T0T;)T1T;\n");
		IPackageFragment fragment = this.project.getPackageFragmentRoots()[0].createPackageFragment("tests", true, null);
		ICompilationUnit unit = fragment.createCompilationUnit("Test1.java", 
				"package tests;\n" + 
				"import org.eclipse.jdt.annotation.*;\n" + 
				"\n" + 
				"import java.util.Collection;\n" + 
				"import java.util.Iterator;\n" + 
				"\n" + 
				"import libs.Lib1;\n" + 
				"\n" + 
				"public class Test1 {\n" + 
				"	Iterator<@NonNull String> test1(Lib1 lib, Collection<@Nullable String> coll) {\n" + 
				"		return lib.unconstrainedTypeArguments1(coll);\n" + 
				"	}\n" + 
				"	Iterator<@NonNull String> test2(Lib1 lib, Collection<@Nullable String> coll) {\n" + 
				"		return lib.unconstrainedTypeArguments2(coll);\n" + 
				"	}\n" +
				"	Iterator<? extends @NonNull String> test3(Lib1 lib, Collection<String> coll) {\n" +
				"		return lib.constrainedWildcards(coll);\n" +
				"	}\n" +
				"	@NonNull Collection<String> test4(Lib1 lib, @Nullable Collection<String> in) {\n" +
				"		return lib.constrainedTypeParameter(in);\n" +
				"	}\n" + 
				"}\n",
				true, new NullProgressMonitor()).getWorkingCopy(new NullProgressMonitor());
		CompilationUnit reconciled = unit.reconcile(AST.JLS8, true, null, new NullProgressMonitor());
		IProblem[] problems = reconciled.getProblems();
		assertNoProblems(problems);
	}
	
	public void testLibsWithArrays() throws Exception {
		myCreateJavaProject("TestLibs");
		addLibraryWithExternalAnnotations(this.project, "lib1.jar", "annots", new String[] {
				"/UnannotatedLib/libs/Lib1.java",
				"package libs;\n" + 
				"\n" + 
				"import java.util.Collection;\n" + 
				"import java.util.Iterator;\n" + 
				"\n" + 
				"public interface Lib1 {\n" + 
				"	String[] constraintArrayTop(String[] in);\n" + 
				"	String[] constraintArrayFull(String[] in);\n" + 
				"	String[][] constraintDeep(String[][] in);\n" + 
				"}\n"
			}, "1.8", null);
		new File(this.project.getProject().getLocation().toString()+"/annots/libs/").mkdirs();
		Util.createFile(this.project.getProject().getLocation().toString()+"/annots/libs/Lib1.eea", 
				"interface libs/Lib1\n" + 
				"\n" + 
				"constraintArrayTop\n" + 
				" ([Ljava/lang/String;)[Ljava/lang/String;\n" + 
				" ([0Ljava/lang/String;)[1Ljava/lang/String;\n" + 
				"\n" + 
				"constraintArrayFull\n" + 
				" ([Ljava/lang/String;)[Ljava/lang/String;\n" +
				" ([0L0java/lang/String;)[1L1java/lang/String;\n" +
				"\n" +
				"constraintDeep\n" +
				" ([[Ljava/lang/String;)[[Ljava/lang/String;\n" +
				" ([0[1L0java/lang/String;)[1[0L1java/lang/String;\n");
		IPackageFragment fragment = this.project.getPackageFragmentRoots()[0].createPackageFragment("tests", true, null);
		ICompilationUnit unit = fragment.createCompilationUnit("Test1.java", 
				"package tests;\n" + 
				"import org.eclipse.jdt.annotation.*;\n" + 
				"\n" + 
				"import libs.Lib1;\n" + 
				"\n" + 
				"public class Test1 {\n" + 
				"	String @NonNull[] test1(Lib1 lib, String @Nullable[] ok, String[] nok) {\n" + 
				"		lib.constraintArrayTop(nok);\n" + 
				"		return lib.constraintArrayTop(ok);\n" + 
				"	}\n" +
				"	@NonNull String @NonNull[] test2(Lib1 lib, @Nullable String @Nullable[] ok, String[] nok) {\n" + 
				"		lib.constraintArrayFull(nok);\n" + 
				"		return lib.constraintArrayFull(ok);\n" + 
				"	}\n" + 
				"	@NonNull String @NonNull[] @Nullable[] test3(Lib1 lib, @Nullable String @Nullable[] @NonNull[] ok, String[][] nok) {\n" + 
				"		lib.constraintDeep(nok);\n" + 
				"		return lib.constraintDeep(ok);\n" + 
				"	}\n" + 
				"}\n",
				true, new NullProgressMonitor()).getWorkingCopy(new NullProgressMonitor());
		CompilationUnit reconciled = unit.reconcile(AST.JLS8, true, null, new NullProgressMonitor());
		IProblem[] problems = reconciled.getProblems();
		assertProblems(problems, new String[] {
			"Pb(955) Null type safety (type annotations): The expression of type 'String[]' needs unchecked conversion to conform to '@Nullable String @Nullable[]'",
			"Pb(955) Null type safety (type annotations): The expression of type 'String[][]' needs unchecked conversion to conform to '@Nullable String @Nullable[] @NonNull[]'",
		}, new int[] { 12, 16 });
	}

	public void testLibsWithFields() throws Exception {
		myCreateJavaProject("TestLibs");
		addLibraryWithExternalAnnotations(this.project, "lib1.jar", "annots", new String[] {
				"/UnannotatedLib/libs/Lib1.java",
				"package libs;\n" + 
				"\n" +
				"public interface Lib1 {\n" + 
				"	String one = \"1\";\n" + 
				"	String none = null;\n" + 
				"}\n"
			}, "1.8", null);
		new File(this.project.getProject().getLocation().toString()+"/annots/libs/").mkdirs();
		Util.createFile(this.project.getProject().getLocation().toString()+"/annots/libs/Lib1.eea", 
				"interface libs/Lib1\n" +
				"\n" + 
				"one\n" + 
				" Ljava/lang/String;\n" + 
				" L1java/lang/String;\n" + 
				"\n" + 
				"none\n" + 
				" Ljava/lang/String;\n" +
				" L0java/lang/String;\n" +
				"\n");
		IPackageFragment fragment = this.project.getPackageFragmentRoots()[0].createPackageFragment("tests", true, null);
		ICompilationUnit unit = fragment.createCompilationUnit("Test1.java", 
				"package tests;\n" + 
				"import org.eclipse.jdt.annotation.*;\n" + 
				"\n" + 
				"import libs.Lib1;\n" + 
				"\n" + 
				"public class Test1 {\n" + 
				"	@NonNull String test0() {\n" + 
				"		return Lib1.none;\n" + 
				"	}\n" +
				"	@NonNull String test1() {\n" + 
				"		return Lib1.one;\n" + 
				"	}\n" +
				"}\n",
				true, new NullProgressMonitor()).getWorkingCopy(new NullProgressMonitor());
		CompilationUnit reconciled = unit.reconcile(AST.JLS8, true, null, new NullProgressMonitor());
		IProblem[] problems = reconciled.getProblems();
		assertProblems(problems, new String[] {
			"Pb(953) Null type mismatch (type annotations): required '@NonNull String' but this expression has type '@Nullable String'",
		}, new int[] { 8 });
	}

	public void testLibsWithTypeParameters() throws Exception {
		myCreateJavaProject("TestLibs");
		addLibraryWithExternalAnnotations(this.project, "lib1.jar", "annots", new String[] {
				"/UnannotatedLib/libs/Lib1.java",
				"package libs;\n" + 
				"\n" +
				"public interface Lib1<U,V,W extends U> {\n" + 
				"	U getU();\n" + 
				"	V getV();\n" +
				"	W getW();\n" +
				"	<X,Y extends CharSequence> Y fun(X x);\n" + 
				"}\n"
			}, "1.8", null);
		new File(this.project.getProject().getLocation().toString()+"/annots/libs/").mkdirs();
		Util.createFile(this.project.getProject().getLocation().toString()+"/annots/libs/Lib1.eea", 
				"interface libs/Lib1\n" +
				" <U:Ljava/lang/Object;V:Ljava/lang/Object;W:TU;>\n" +
				" <0U:Ljava/lang/Object;1V:Ljava/lang/Object;W:T1U;>\n" +
				"\n" +
				"fun\n" +
				" <X:Ljava/lang/Object;Y::Ljava/lang/CharSequence;>(TX;)TY;\n" +
				" <1X:Ljava/lang/Object;Y::L1java/lang/CharSequence;>(TX;)TY;\n" +
				"\n");
		IPackageFragment fragment = this.project.getPackageFragmentRoots()[0].createPackageFragment("tests", true, null);
		ICompilationUnit unit = fragment.createCompilationUnit("Test1.java", 
				"package tests;\n" + 
				"import org.eclipse.jdt.annotation.*;\n" + 
				"\n" + 
				"import libs.Lib1;\n" + 
				"\n" + 
				"public class Test1 {\n" + 
				"	@NonNull String test0(Lib1<@Nullable String,@NonNull String,@NonNull String> l) {\n" + 
				"		return l.getU();\n" + // mismatch: U is nullable 
				"	}\n" +
				"	@NonNull String test1(Lib1<@Nullable String,@NonNull String,@NonNull String> l) {\n" + 
				"		return l.getV();\n" + // OK: V is nonnull
				"	}\n" +
				"	@NonNull String test2(Lib1<@Nullable String,@NonNull String,@NonNull String> l) {\n" + 
				"		return l.getW();\n" + // OK: V is nonnull
				"	}\n" +
				"	Lib1<@NonNull String, @NonNull String, @NonNull String> f1;\n" + // mismatch at U
				"	Lib1<@Nullable String, String, @NonNull String> f2;\n" + // mismatch at V
				"	Lib1<@Nullable String, @NonNull String, @Nullable String> f3;\n" + // mismatch at W
				"	@Nullable String test3(Lib1<@Nullable String,@NonNull String,@NonNull String> l) {\n" +
				"		return l.<@Nullable String,@Nullable String>fun(\"\");\n" + // mismatches at X and Y
				"	}\n" +
				"}\n",
				true, new NullProgressMonitor()).getWorkingCopy(new NullProgressMonitor());
		CompilationUnit reconciled = unit.reconcile(AST.JLS8, true, null, new NullProgressMonitor());
		IProblem[] problems = reconciled.getProblems();
		assertProblems(problems, new String[] {
			"Pb(953) Null type mismatch (type annotations): required '@NonNull String' but this expression has type '@Nullable String'",
			"Pb(964) Null constraint mismatch: The type '@NonNull String' is not a valid substitute for the type parameter '@Nullable U extends Object'",
			"Pb(964) Null constraint mismatch: The type 'String' is not a valid substitute for the type parameter '@NonNull V extends Object'",
			"Pb(964) Null constraint mismatch: The type '@Nullable String' is not a valid substitute for the type parameter '@NonNull W extends @NonNull U extends Object'", // FIXME(stephan): @NonNull before W is bogus, see https://bugs.eclipse.org/456532
			"Pb(964) Null constraint mismatch: The type '@Nullable String' is not a valid substitute for the type parameter '@NonNull X extends Object'",
			"Pb(964) Null constraint mismatch: The type '@Nullable String' is not a valid substitute for the type parameter '@NonNull Y extends @NonNull CharSequence'", // FIXME(see above)
		}, new int[] { 8, 16, 17, 18, 20, 20 });
	}

	/** Project with real JRE8. */
	public void test2() throws Exception {
		Hashtable options = JavaCore.getOptions();
		try {
			setupJavaProject("Test2");
			this.project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
			IMarker[] markers = this.project.getProject().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			assertNoMarkers(markers);
		} finally {
			// project using a full JRE container initializes global options to 1.8 -- must reset now:
			JavaCore.setOptions(options);
		}
	}

	/** Project with real JRE8.
	 * More interesting work with generics
	 */
	public void test3() throws Exception {
		Hashtable options = JavaCore.getOptions();
		try {
			setupJavaProject("Test3");
			this.project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
			IMarker[] markers = this.project.getProject().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			assertNoMarkers(markers);
		} finally {
			// project using a full JRE container initializes global options to 1.8 -- must reset now:
			JavaCore.setOptions(options);
		}
	}

}
