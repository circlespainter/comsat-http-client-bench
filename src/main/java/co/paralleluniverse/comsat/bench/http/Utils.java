package co.paralleluniverse.comsat.bench.http;

import javax.management.*;
import java.lang.management.ManagementFactory;

public final class Utils {
	private static final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	private static final Runtime r = Runtime.getRuntime();
	private static final ObjectName name;
	static {
		try {
			name = ObjectName.getInstance("java.lang:type=OperatingSystem");
		} catch (final MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
	}

	private static long samples = 0L;
	private static double maxCpu = 0.0D;
	private static double maxMem = 0.0D;
	private static double totCpu = 0.0D;
	private static double totMem = 0.0D;

	public static final class SysStats {
		public long samples;
		public double cpu, maxCpu, avgCpu;
		public double mem, maxMem, avgMem;
	}

	public static SysStats sampleSys() {
		final SysStats ret = new SysStats();
		ret.samples = ++samples;
		final AttributeList list;
		try {
			final double mem = (r.totalMemory() - r.freeMemory()) / (1_024.0D * 1_024.0D);
			totMem += mem;
			maxMem = Math.max(maxMem, mem);
			ret.mem = Math.round(mem * 100.0D) / 100.0D;
			ret.maxMem = Math.round(maxMem * 100.0D) / 100.0D;
			ret.avgMem = Math.round(totMem / samples * 100.0D) / 100.0D;

			list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});
			final Attribute att = (Attribute) list.get(0);
			final double cpu = (int) ((double) att.getValue() * 1_000.0D) / 10.0D;
			totCpu += cpu;
			maxCpu = Math.max(maxCpu, cpu);
			ret.cpu = Math.round(cpu * 100.0D) / 100.0D;
			ret.maxCpu = Math.round(maxCpu * 100.0D) / 100.0D;
			ret.avgCpu = Math.round(totCpu / samples * 100.0D) / 100.0D;
		} catch (final ReflectionException | InstanceNotFoundException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static void resetSampleSys() {
		samples = 0L;
		maxCpu = 0.0D;
		maxMem = 0.0D;
		totCpu = 0.0D;
		totMem = 0.0D;
	}
}
