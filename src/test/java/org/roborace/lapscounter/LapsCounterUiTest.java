package org.roborace.lapscounter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Type;
import org.springframework.boot.test.context.SpringBootTest;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.roborace.lapscounter.domain.State.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LapsCounterUiTest extends LapsCounterAbstractTest {


    @BeforeEach
    void setUp() {

    }


    @AfterEach
    void tearDown() {

    }

    @Test
    void testHappyPathSimple() {

        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(RUNNING);

        sendCommandAndCheckState(FINISH);

    }

    @Test
    void testRestart() {

        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(READY);

        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(RUNNING);

        sendCommandAndCheckState(FINISH);

        sendCommandAndCheckState(READY);

    }

    @Test
    void testStateUi() {

        sendState();
        shouldReceiveState(ui, READY);

        sendCommand(STEADY);
        shouldReceiveState(ui, STEADY);

        sendState();
        shouldReceiveState(ui, STEADY);

    }

    @Test
    void testSendTime() throws InterruptedException {

        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(RUNNING);

        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getMillis(), lessThan(100L));

        Thread.sleep(TIME_SEND_INTERVAL);
        shouldReceiveType(ui, Type.TIME);
//        assertThat(ui.getLastMessage().getMillis(), equalTo(TIME_SEND_INTERVAL));

        sendCommandAndCheckState(FINISH);
        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getMillis(), greaterThan(TIME_SEND_INTERVAL));
        assertThat(ui.getLastMessage().getMillis(), lessThan(TIME_SEND_INTERVAL + 500));

    }

    @Test
    void testWrongCommand() {
        sendMessage(ui, Message.builder().type(Type.COMMAND).build());
        shouldReceiveType(ui, Type.ERROR);
    }

    @Test
    void testWrongOrder() {
        sendCommand(RUNNING);
        shouldReceiveType(ui, Type.ERROR);
        sendCommand(FINISH);
        shouldReceiveType(ui, Type.ERROR);

        sendCommandAndCheckState(STEADY);

        sendCommand(FINISH);
        shouldReceiveType(ui, Type.ERROR);

        sendCommandAndCheckState(RUNNING);

    }

    @Test
    void testLaps() {

        sendMessage(ui, buildWithType(Type.LAPS));

//        await().until(() -> ui.hasMessageWithType(Type.LAP));

    }


}