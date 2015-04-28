package potaufeu;

import java.util.function.*;
import org.slf4j.*;

public final class Log {

    private final Logger log;

    private Log(Class<?> c) {
        this.log = LoggerFactory.getLogger(c);
    }

    public static Log logger(Class<?> c) {
        return new Log(c);
    }

    public void error(Supplier<String> f) {
        if (log.isErrorEnabled())
            log.error(f.get());
    }

    public void error(Supplier<String> f, Throwable th) {
        if (log.isErrorEnabled())
            log.error(f.get(), th);
    }

    public void warn(Supplier<String> f) {
        if (log.isWarnEnabled())
            log.warn(f.get());
    }

    public void warn(Supplier<String> f, Throwable th) {
        if (log.isWarnEnabled())
            log.warn(f.get(), th);
    }

    public void info(Supplier<String> f) {
        if (log.isInfoEnabled())
            log.info(f.get());
    }

    public void debug(Supplier<String> f) {
        if (log.isDebugEnabled())
            log.debug(f.get());
    }

}
