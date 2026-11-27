import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import javax.tools.JavaCompiler;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Reads Java source files for the documentation generator. Collects class and method details along with their comments without running the tests.
 */
public class MethodExtractor {

    /**
     * Encodes a potentially multiline field for tab-separated output.
     *
     * @param value source path, signature component, or Javadoc text
     * @return UTF-8 input encoded as standard Base64
     */
    static String b64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses all Java source files under the provided directory and writes one tab-separated Base64-encoded details record per method or constructor. Records include the source path, owner, name, return type, parameters, source offsets, and source-attached Javadoc text.
     *
     * @param args one argument: project source root to inspect
     * @throws Exception if Java sources cannot be enumerated or parsed
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected one source directory");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK is required to generate source documentation");

        List<File> inputs = new ArrayList<>();
        try (var paths = Files.walk(Path.of(args[0]))) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(path -> inputs.add(path.toFile()));
        }
        Collections.sort(inputs);

        try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> files = manager.getJavaFileObjectsFromFiles(inputs);
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavacTask task = (JavacTask) compiler.getTask(null, manager,
                    diagnostics, List.of("--release", "17", "-proc:none", "-Xlint:none"), null, files);
            Trees trees = Trees.instance(task);
            DocTrees docs = DocTrees.instance(task);
            for (CompilationUnitTree unit : task.parse()) {
                String fileName = Path.of(unit.getSourceFile().toUri()).toString();
                SourcePositions positions = trees.getSourcePositions();

                new TreePathScanner<Void, Void>() {
                    private final Deque<String> enclosingClasses = new ArrayDeque<>();

                    /**
                     * Tracks the containing type while traversing nested Java classes and records.
                     *
                     * @param type class declaration being visited
                     * @param unused scanner context, unused by this extractor
                     * @return {@code null}; traversal continues through the type's members
                     */
                    @Override public Void visitClass(ClassTree type, Void unused) {
                        enclosingClasses.addLast(type.getSimpleName().toString());
                        super.visitClass(type, unused);
                        enclosingClasses.removeLast();
                        return null;
                    }

                    /**
                     * Emits the details for a declared Java method or constructor.
                     *
                     * @param method method declaration encountered by the syntax tree scanner
                     * @param unused scanner context, unused by this extractor
                     * @return the default visitor result after the record is emitted
                     */
                    @Override public Void visitMethod(MethodTree method, Void unused) {
                        int start = (int) positions.getStartPosition(unit, method);
                        int end = (int) positions.getEndPosition(unit, method);
                        int body = method.getBody() == null ? -1
                                : (int) positions.getStartPosition(unit, method.getBody());
                        var commentTree = docs.getDocCommentTree(getCurrentPath());
                        String comment = commentTree == null ? "" : commentTree.toString();
                        String parameters = String.join(";", method.getParameters().stream()
                                .map(p -> p.getType() + " " + p.getName()).toList());
                        String[] fields = {
                            b64(fileName), b64(String.join(".", enclosingClasses)),
                            b64(method.getName().toString()),
                            b64(method.getReturnType() == null ? "" : method.getReturnType().toString()),
                            b64(parameters), String.valueOf(start), String.valueOf(end),
                            String.valueOf(body), b64(comment)
                        };
                        System.out.println(String.join("\t", fields));
                        return super.visitMethod(method, unused);
                    }
                }.scan(unit, null);
            }
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    System.err.println(diagnostic);
                }
            }
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR))
                throw new IllegalStateException("Java source contains syntax errors");
        }
    }
}
