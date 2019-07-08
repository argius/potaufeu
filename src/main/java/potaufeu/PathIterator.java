package potaufeu;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

final class PathIterator implements Iterator<Path> {

    private final int rootDepth;
    private final int maxDepth;
    private final Queue<Path> q;
    private final Queue<Path> dirs;
    private final boolean ignoreAccessDenied;
    private final PathMatcher exclusiveFilter;

    PathIterator(Path root, int maxDepth, boolean ignoreAccessDenied) {
        this(root, maxDepth, ignoreAccessDenied, Optional.empty());
    }

    PathIterator(Path root, int maxDepth, boolean ignoreAccessDenied, Optional<PathMatcher> optExclusiveFilter) {
        this.rootDepth = root.getNameCount();
        this.maxDepth = maxDepth;
        this.q = new LinkedList<>();
        this.dirs = new LinkedList<>();
        this.ignoreAccessDenied = ignoreAccessDenied;
        this.exclusiveFilter = optExclusiveFilter.orElse(path -> false);
        q.offer(root);
        dirs.offer(root);
    }

    @Override
    public boolean hasNext() {
        if (!dirs.isEmpty())
            traverse(128); // do not refactor this unless you can clearly optimize
        return !q.isEmpty();
    }

    @Override
    public Path next() {
        return q.poll();
    }

    static Stream<Path> streamOf(Path root) {
        return streamOf(root, Integer.MAX_VALUE, false);
    }

    static Stream<Path> streamOf(Path root, int maxDepth) {
        return streamOf(root, maxDepth, false);
    }

    static Stream<Path> streamOf(Path root, int maxDepth, boolean ignoreAccessDenied) {
        return streamOf(root, maxDepth, false, Optional.empty());
    }

    static Stream<Path> streamOf(Path root, int maxDepth, boolean ignoreAccessDenied,
            Optional<PathMatcher> optExclusiveFilter) {
        PathIterator pathIterator = new PathIterator(root, maxDepth, ignoreAccessDenied, optExclusiveFilter);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(pathIterator, 0), false);
    }

    void traverse(int requiredSize) {
        while (q.size() < requiredSize) {
            if (dirs.isEmpty())
                break;
            Path dir = dirs.poll();
            if (exclusiveFilter.matches(dir))
                continue;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                stream.forEach(x -> {
                    if ((x.getNameCount() - rootDepth) <= maxDepth && !exclusiveFilter.matches(x)) {
                        q.offer(x);
                        if (Files.isDirectory(x))
                            dirs.offer(x);
                    }
                });
            } catch (IOException e) {
                err(e, dir);
            }
        }
    }

    void err(Exception e, Path path) {
        final String msg;
        if (e instanceof AccessDeniedException) {
            if (ignoreAccessDenied)
                return;
            msg = "access denied";
        }
        else if (e instanceof NoSuchFileException)
            msg = "no such file or directory";
        else
            msg = String.format("%s (%s)", e.getMessage(), e.getClass().getSimpleName());
        System.err.printf("potf: '%s': %s%n", path, msg);
    }

}
