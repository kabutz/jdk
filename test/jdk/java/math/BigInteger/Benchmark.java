import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class Benchmark {
  private static final ThreadMXBean tmbean =
      ManagementFactory.getThreadMXBean();
  private static final long CONVERSION = 1000 * 1000;

  private long cpuTime;
  private long elapsedTime;
  private long userTime;
  private final ByteWatcherSingleThread byteWatcher =
      new ByteWatcherSingleThread();
  private long bytes;

  public void start() {
    cpuTime = getCurrentThreadCPUTime();
    userTime = getCurrentThreadUserTime();
    elapsedTime = getCurrentElapsedTime();
    byteWatcher.reset();
  }

  public void stop() {
    bytes = byteWatcher.calculateAllocations();
    cpuTime = getCurrentThreadCPUTime() - cpuTime;
    userTime = getCurrentThreadUserTime() - userTime;
    elapsedTime = getCurrentElapsedTime() - elapsedTime;
  }

  private static long getCurrentElapsedTime() {
    return System.currentTimeMillis();
  }

  private static long getCurrentThreadUserTime() {
    return tmbean.getCurrentThreadUserTime() / CONVERSION;
  }

  private static long getCurrentThreadCPUTime() {
    return tmbean.getCurrentThreadCpuTime() / CONVERSION;
  }

  public long getCpuTime() {
    return cpuTime;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public long getUserTime() {
    return userTime;
  }

  public String toString() {
    return "cpu=%d,user=%d,elapsed=%d,memory=%s,bytes=%d".formatted(
            cpuTime, userTime, elapsedTime, formatMemory(), bytes);
  }

  private String formatMemory() {
    return Memory.format(bytes, Memory.BYTES, 1);
  }
}
