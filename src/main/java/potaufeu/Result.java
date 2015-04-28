package potaufeu;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

final class Result {

    private Set<Path> pathList;
    private String name;

    final Map<Path, List<FileLine>> grepped = new HashMap<>();

    public Result() {
        this.pathList = Collections.synchronizedSet(new HashSet<>());
        this.name = "";
    }

    public void addPath(Path path) {
        pathList.add(path);
    }

    public int matchedCount() {
        return pathList.size();
    }

    public Stream<Path> pathStream() {
        return pathList.stream();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Result mergeOr(Result an) {
        Result r = new Result();
        r.pathList.addAll(this.pathList);
        r.pathList.addAll(an.pathList);
        return r;
    }

}
