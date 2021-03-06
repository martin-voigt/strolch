package li.strolch.execution;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import li.strolch.execution.service.StartActivityExecutionService;
import li.strolch.model.Locator;
import li.strolch.model.State;
import li.strolch.model.Tags;
import li.strolch.model.activity.Action;
import li.strolch.model.activity.Activity;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.service.LocatorArgument;
import li.strolch.testbase.runtime.RuntimeMock;

public class StartActivityExecutionServiceTest extends RuntimeMock {

	@Before
	public void before() {
		mockRuntime(new File("target/" + StartActivityExecutionServiceTest.class.getName()),
				new File("src/test/resources/executiontest"));
		startContainer();
	}

	@After
	public void after() {
		destroyRuntime();
	}

	@Test
	public void shouldExecuteSingleActionActivity() throws InterruptedException {

		Locator activityLoc = Locator.valueOf(Tags.ACTIVITY, "ToStock", "produceBicycle");

		Certificate cert = loginTest();

		StartActivityExecutionService svc = new StartActivityExecutionService();
		LocatorArgument arg = new LocatorArgument();
		arg.realm = "execution";
		arg.locator = activityLoc;

		doServiceAssertResult(cert, svc, arg);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action = tx.findElement(activityLoc.append("produce"));
			assertEquals(State.EXECUTION, action.getState());
		}

		Thread.sleep(100L);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action = tx.findElement(activityLoc.append("produce"));
			assertEquals(State.EXECUTED, action.getState());
		}
	}

	@Test
	public void shouldExecuteSeriesActivity() throws InterruptedException {

		Locator activityLoc = Locator.valueOf(Tags.ACTIVITY, "ToStock", "conveyors");

		Certificate cert = loginTest();

		StartActivityExecutionService svc = new StartActivityExecutionService();
		LocatorArgument arg = new LocatorArgument();
		arg.realm = "execution";
		arg.locator = activityLoc;

		doServiceAssertResult(cert, svc, arg);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action;

			action = tx.findElement(activityLoc.append("action_1"));
			assertEquals(State.EXECUTION, action.getState());

			action = tx.findElement(activityLoc.append("action_2"));
			assertEquals(State.CREATED, action.getState());
			action = tx.findElement(activityLoc.append("action_3"));
			assertEquals(State.CREATED, action.getState());
		}

		Thread.sleep(450L);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action;

			action = tx.findElement(activityLoc.append("action_1"));
			assertEquals(State.EXECUTED, action.getState());

			action = tx.findElement(activityLoc.append("action_2"));
			assertEquals(State.EXECUTION, action.getState());

			action = tx.findElement(activityLoc.append("action_3"));
			assertEquals(State.CREATED, action.getState());
		}

		Thread.sleep(300L);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action;

			action = tx.findElement(activityLoc.append("action_1"));
			assertEquals(State.EXECUTED, action.getState());

			action = tx.findElement(activityLoc.append("action_2"));
			assertEquals(State.EXECUTED, action.getState());

			action = tx.findElement(activityLoc.append("action_3"));
			assertEquals(State.EXECUTION, action.getState());
		}

		Thread.sleep(300L);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action;

			action = tx.findElement(activityLoc.append("action_1"));
			assertEquals(State.EXECUTED, action.getState());

			action = tx.findElement(activityLoc.append("action_2"));
			assertEquals(State.EXECUTED, action.getState());

			action = tx.findElement(activityLoc.append("action_3"));
			assertEquals(State.EXECUTED, action.getState());

			Activity activity = (Activity) tx.findElement(activityLoc.append("action_3")).getRootElement();
			assertEquals(State.EXECUTED, activity.getState());

		}
	}

	@Test
	public void shouldExecuteParallelActivity() throws InterruptedException {

		Locator activityLoc = Locator.valueOf(Tags.ACTIVITY, "ToStock", "parallel");

		Certificate cert = loginTest();

		StartActivityExecutionService svc = new StartActivityExecutionService();
		LocatorArgument arg = new LocatorArgument();
		arg.realm = "execution";
		arg.locator = activityLoc;

		doServiceAssertResult(cert, svc, arg);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action;

			action = tx.findElement(activityLoc.append("action_1"));
			assertEquals(State.EXECUTION, action.getState());
			action = tx.findElement(activityLoc.append("action_2"));
			assertEquals(State.EXECUTION, action.getState());
			action = tx.findElement(activityLoc.append("action_3"));
			assertEquals(State.EXECUTION, action.getState());
		}

		Thread.sleep(150L);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Action action;

			action = tx.findElement(activityLoc.append("action_1"));
			assertEquals(State.EXECUTED, action.getState());
			action = tx.findElement(activityLoc.append("action_2"));
			assertEquals(State.EXECUTED, action.getState());
			action = tx.findElement(activityLoc.append("action_3"));
			assertEquals(State.EXECUTED, action.getState());
		}
	}

	@Test
	public void shouldExecuteDeepActivity() throws InterruptedException {

		Locator activityLoc = Locator.valueOf(Tags.ACTIVITY, "ToStock", "deep");

		Certificate cert = loginTest();

		StartActivityExecutionService svc = new StartActivityExecutionService();
		LocatorArgument arg = new LocatorArgument();
		arg.realm = "execution";
		arg.locator = activityLoc;

		doServiceAssertResult(cert, svc, arg);

		Thread.sleep(1000L);

		try (StrolchTransaction tx = getRealm("execution").openTx(cert, StartActivityExecutionServiceTest.class)) {
			Activity activity = tx.findElement(activityLoc);
			assertEquals(State.EXECUTED, activity.getState());
		}
	}
}
