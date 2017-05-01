package potaufeu;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

final class Result implements Serializable {

    private static final long serialVersionUID = -4212253217941704388L;

    private Set<Path> pathList;
    private String name;
    private Map<Path, List<FileLine>> grepped;

    public Result() {
        this.pathList = Collections.synchronizedSet(new HashSet<>());
        this.name = "";
        this.grepped = new HashMap<>();
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

    public int getLineCount() {
        return this.grepped.size();
    }

    public Result mergeOr(Result an) {
        Result r = new Result();
        r.pathList.addAll(this.pathList);
        r.pathList.addAll(an.pathList);
        return r;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeObject(pathList.stream().map(x -> x.toFile()).toArray());
        oos.writeObject(name);
        oos.writeObject(grepped);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Object o1 = ois.readObject();
        Object o2 = ois.readObject();
        Object o3 = ois.readObject();
        this.pathList = Stream.of((Object[]) o1).map(x -> ((File) x).toPath()).collect(Collectors.toSet());
        this.name = (String) o2;
        this.grepped = (Map<Path, List<FileLine>>) o3;
    }

}
