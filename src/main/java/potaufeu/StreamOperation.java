package potaufeu;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

public final class StreamOperation<T> implements Iterable<T> {

    private Stream<T> stream;

    public StreamOperation(Stream<T> stream) {
        this.stream = stream;
    }

    @Override
    public Iterator<T> iterator() {
        return stream.iterator();
    }

    public static <T> StreamOperation<T> of(Stream<T> stream) {
        return new StreamOperation<>(stream);
    }

    public Stream<T> getStream() {
        return stream;
    }

    public StreamOperation<T> peek(Consumer<? super T> action) {
        stream = stream.peek(action);
        return this;
    }

    public StreamOperation<T> sequential() {
        stream = stream.sequential();
        return this;
    }

    public StreamOperation<T> sorted(Optional<Comparator<T>> sorter) {
        sorter.ifPresent(x -> stream = stream.sorted(x));
        return this;
    }

    public StreamOperation<T> head(OptionalInt optCount) {
        optCount.ifPresent(count -> {
            AtomicInteger limitCount = new AtomicInteger(count);
            stream = stream.filter(x -> limitCount.decrementAndGet() >= 0);
        });
        return this;
    }

    public StreamOperation<T> tail(OptionalInt optCount) {
        optCount.ifPresent(count -> {
            Tail<T> q = new Tail<>(count);
            stream.forEach(q::add);
            stream = q.tailStream();
        });
        return this;
    }

    public static String pathToExtension(Path path) {
        final String s = FileAttributeFormatter.name(path);
        if (!s.contains("."))
            return "";
        else if (s.endsWith("."))
            return ".";
        return s.replaceFirst(".*\\.([^\\.]+)$", "$1");
    }

    public static final class Sampler implements Consumer<Path> {
        final Consumer<Path> action;
        final boolean isResultRecorded;
        final boolean isCounted;
        private Result result;
        private LongAdder count;

        Sampler(OptionSet opts) {
            final Consumer<Path> action;
            boolean doResultRecording = false;
            boolean doCount = false;
            if (opts.isState()) {
                action = x -> getResult().addPath(x);
                doResultRecording = true;
                result = new Result();
            }
            else if (opts.isVerbose()) {
                action = x -> getCount().increment();
                doCount = true;
                count = new LongAdder();
            }
            else
                action = x -> {
                };
            this.action = action;
            this.isResultRecorded = doResultRecording;
            this.isCounted = doCount;
        }

        @Override
        public void accept(Path t) {
            action.accept(t);
        }

        public Result getResult() {
            return result;
        }

        public LongAdder getCount() {
            return count;
        }
    }

    /**
     * This class is not thread-safe.
     */
    private static final class Tail<T> {
        final List<T> list;
        final int limit;
        final int bufferedLimit;

        Tail(int limit) {
            this.list = new ArrayList<>(8_192);
            this.limit = limit;
            this.bufferedLimit = 8_000;
        }

        void add(T o) {
            list.add(o);
            if (list.size() > bufferedLimit)
                while (list.size() > limit)
                    list.remove(0);
        }

        Stream<T> tailStream() {
            final int count = list.size();
            List<T> a = (count > limit) ? list.subList(count - limit, count) : list;
            return a.stream();
        }
    }

}
