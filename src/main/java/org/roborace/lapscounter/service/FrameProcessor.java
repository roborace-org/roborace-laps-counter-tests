package org.roborace.lapscounter.service;

import org.roborace.lapscounter.domain.Robot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FrameProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FrameProcessor.class);

    private final Map<Integer, Long> lastFrameTimeBySerialMap = new HashMap<>();

    private final long safeInterval;

    public FrameProcessor(@Value("${laps.safe-interval}") long safeInterval) {
        this.safeInterval = safeInterval;
    }

    public boolean checkFrame(Robot robot, Integer frame, long raceTime) {

        int serial = robot.getSerial();
        Long lastLapTime = lastFrameTimeBySerialMap.getOrDefault(serial, 0L);
        if (raceTime >= lastLapTime + safeInterval) {
            robot.incLaps();
            lastFrameTimeBySerialMap.put(serial, raceTime);
            robot.setTime(raceTime);
            LOG.info("Frame is counted: {}", robot);
            return true;
        } else {
            LOG.warn("Frame is not counted (too quick): {}", robot);
        }
        return false;
    }

    public void reset() {
        lastFrameTimeBySerialMap.clear();
    }
}
