/**********************************************************************************************************************
 * garbagecat                                                                                                         *
 *                                                                                                                    *
 * Copyright (c) 2008-2016 Red Hat, Inc.                                                                              *
 *                                                                                                                    * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse *
 * Public License v1.0 which accompanies this distribution, and is available at                                       *
 * http://www.eclipse.org/legal/epl-v10.html.                                                                         *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Red Hat, Inc. - initial API and implementation                                                                  *
 *********************************************************************************************************************/
package org.eclipselabs.garbagecat.domain.jdk.unified;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipselabs.garbagecat.domain.BlockingEvent;
import org.eclipselabs.garbagecat.domain.CombinedData;
import org.eclipselabs.garbagecat.domain.TriggerData;
import org.eclipselabs.garbagecat.domain.YoungCollection;
import org.eclipselabs.garbagecat.domain.jdk.UnknownCollector;
import org.eclipselabs.garbagecat.util.jdk.JdkMath;
import org.eclipselabs.garbagecat.util.jdk.JdkRegEx;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;

/**
 * <p>
 * UNIFIED_YOUNG
 * </p>
 * 
 * <p>
 * Young collection JDK9+ with <code>-Xlog:gc:file=&lt;file&gt;</code> (no details).
 * </p>
 * 
 * <p>
 * Collector is one of the following: (1) {@link org.eclipselabs.garbagecat.domain.jdk.SerialNewEvent}, (2)
 * {@link org.eclipselabs.garbagecat.domain.jdk.ParallelScavengeEvent}, (3)
 * {@link org.eclipselabs.garbagecat.domain.jdk.ParNewEvent}.
 * </p>
 * 
 * <h3>Example Logging</h3>
 * 
 * <pre>
 * [0.053s][info][gc] GC(0) Pause Young (Allocation Failure) 0M-&gt;0M(1M) 0.914ms
 * </pre>
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
public class UnifiedYoungEvent extends UnknownCollector
        implements UnifiedLogging, BlockingEvent, YoungCollection, CombinedData, TriggerData {

    /**
     * The log entry for the event. Can be used for debugging purposes.
     */
    private String logEntry;

    /**
     * The elapsed clock time for the GC event in milliseconds (rounded).
     */
    private int duration;

    /**
     * The time when the GC event started in milliseconds after JVM startup.
     */
    private long timestamp;

    /**
     * Combined young + old generation size (kilobytes) at beginning of GC event.
     */
    private int combinedBegin;

    /**
     * Combined young + old generation size (kilobytes) at end of GC event.
     */
    private int combinedEnd;

    /**
     * Combined young + old generation allocation (kilobytes).
     */
    private int combinedAllocation;

    /**
     * The trigger for the GC event.
     */
    private String trigger;

    /**
     * Trigger(s) regular expression(s).
     */
    private static final String TRIGGER = "(" + JdkRegEx.TRIGGER_ALLOCATION_FAILURE + "|" + JdkRegEx.TRIGGER_SYSTEM_GC
            + ")";

    /**
     * Regular expression defining the logging.
     */
    private static final String REGEX = "^\\[" + JdkRegEx.TIMESTAMP + "s\\]\\[info\\]\\[gc\\] "
            + JdkRegEx.GC_EVENT_NUMBER + " Pause Young \\(" + TRIGGER + "\\) " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE
            + "\\(" + JdkRegEx.SIZE + "\\) " + JdkRegEx.DURATION_JDK9 + "[ ]*$";

    private static final Pattern pattern = Pattern.compile(REGEX);

    /**
     * 
     * @param logEntry
     *            The log entry for the event.
     */
    public UnifiedYoungEvent(String logEntry) {
        this.logEntry = logEntry;
        Matcher matcher = pattern.matcher(logEntry);
        if (matcher.find()) {
            long endTimestamp = JdkMath.convertSecsToMillis(matcher.group(1)).longValue();
            trigger = matcher.group(2);
            combinedBegin = JdkMath.calcKilobytes(Integer.parseInt(matcher.group(4)), matcher.group(6).charAt(0));
            combinedEnd = JdkMath.calcKilobytes(Integer.parseInt(matcher.group(7)), matcher.group(9).charAt(0));
            combinedAllocation = JdkMath.calcKilobytes(Integer.parseInt(matcher.group(10)),
                    matcher.group(12).charAt(0));
            duration = JdkMath.roundMillis(matcher.group(13)).intValue();
            timestamp = endTimestamp - duration;
        }
    }

    /**
     * Alternate constructor. Create serial logging event from values.
     * 
     * @param logEntry
     *            The log entry for the event.
     * @param timestamp
     *            The time when the GC event started in milliseconds after JVM startup.
     * @param duration
     *            The elapsed clock time for the GC event in milliseconds.
     */
    public UnifiedYoungEvent(String logEntry, long timestamp, int duration) {
        this.logEntry = logEntry;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public String getName() {
        return JdkUtil.LogEventType.UNIFIED_YOUNG.toString();
    }

    public String getLogEntry() {
        return logEntry;
    }

    public int getDuration() {
        return duration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCombinedOccupancyInit() {
        return combinedBegin;
    }

    public int getCombinedOccupancyEnd() {
        return combinedEnd;
    }

    public int getCombinedSpace() {
        return combinedAllocation;
    }

    public String getTrigger() {
        return trigger;
    }

    /**
     * Determine if the logLine matches the logging pattern(s) for this event.
     * 
     * @param logLine
     *            The log line to test.
     * @return true if the log line matches the event pattern, false otherwise.
     */
    public static final boolean match(String logLine) {
        return pattern.matcher(logLine).matches();
    }
}
