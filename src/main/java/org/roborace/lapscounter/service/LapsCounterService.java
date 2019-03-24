package org.roborace.lapscounter.service;

import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Robot;
import org.roborace.lapscounter.domain.State;
import org.roborace.lapscounter.domain.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
public class LapsCounterService {

    private static final Logger LOG = LoggerFactory.getLogger(LapsCounterService.class);

    private State state = State.STEADY;
    private Stopwatch stopwatch = new Stopwatch();

    private List<Robot> robots = new ArrayList<>();

    @Autowired
    RoboraceWebSocketHandler webSocketHandler;

    public void handleMessage(Message message) {

        switch (message.getType()) {
            case COMMAND:
                command(message);
                break;
            case STATE:
                webSocketHandler.broadcast(getState());
                break;
            case ROBOT_INIT:
                robotInit(message);
                break;
            case ROBOT_EDIT:
                robotEdit(message);
                break;
            case TIME:
                break;
            case LAPS:
                for (Robot robot : robots) {
                    webSocketHandler.broadcast(getLap(robot));
                }
                break;
            case LAP:
                break;
            case FRAME:
                frame(message);
                break;
            case ERROR:
                break;
        }

    }

    private void command(Message message) {
        State parsedState = message.getState();
        if (parsedState == null) {
            webSocketHandler.broadcast(Message.builder().type(Type.ERROR).build());
            return;
        }
        if ((parsedState == State.STEADY && state != State.READY)
                || (parsedState == State.RUNNING && state != State.STEADY)
                || (parsedState == State.FINISH && state != State.RUNNING)) {
            Message error = Message.builder()
                    .type(Type.ERROR)
                    .message("Wrong current state to apply command")
                    .build();
            webSocketHandler.broadcast(error);
            return;
        }
        if (parsedState != this.state) {
            this.state = parsedState;
            webSocketHandler.broadcast(getState());
            if (this.state == State.RUNNING) {
                stopwatch.start();
                webSocketHandler.broadcast(getTime());
            } else if (this.state == State.FINISH) {
                stopwatch.finish();
                webSocketHandler.broadcast(getTime());
            }
        }
    }

    private void robotInit(Message message) {
        Optional<Robot> existing = getRobot(message.getSerial());
        Robot robot;
        if (existing.isPresent()) {
            LOG.info("Reconnect robot {}", existing.get());
            robot = existing.get();
        } else {
            robot = new Robot();
            robot.setSerial(message.getSerial());
            robots.add(robot);
            LOG.info("Connect new robot {}", robot);
        }
        LOG.info("Connected robots: {}", robots);
        webSocketHandler.broadcast(getLap(robot));
    }

    private void robotEdit(Message message) {
        Robot robot = getRobotOrElseThrow(message.getSerial());
        LOG.info("Reconnect robot {}", robot);
        robot.setName(message.getName());
        webSocketHandler.broadcast(getLap(robot));
    }

    private void frame(Message message) {
        Robot robot = getRobotOrElseThrow(message.getSerial());
        robot.incLaps();
        webSocketHandler.broadcast(getLap(robot));
    }

    public Message getState() {
        Message message = new Message();
        message.setType(Type.STATE);
        message.setState(state);
        return message;
    }

    public Message getTime() {
        Message message = new Message();
        message.setType(Type.TIME);
        message.setMillis(stopwatch.getTime());
        return message;
    }

    private Message getLap(Robot robot) {
        Message message = new Message();
        message.setType(Type.LAP);
        message.setSerial(robot.getSerial());
        message.setName(robot.getName());
        message.setLaps(robot.getLaps());
        message.setMillis(stopwatch.getTime());
        return message;
    }

    private Robot getRobotOrElseThrow(Integer serial) {
        return Optional.ofNullable(serial)
                .flatMap(this::getRobot)
                .orElseThrow(() -> new RuntimeException("Cannot find robot by serial"));
    }

    private Optional<Robot> getRobot(Integer serial) {
        return robots.stream().filter(r -> r.getSerial() == serial).findAny();
    }
}
