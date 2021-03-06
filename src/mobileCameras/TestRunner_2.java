package mobileCameras;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import repast.simphony.batch.BatchScenarioLoader;
import repast.simphony.engine.controller.Controller;
import repast.simphony.engine.controller.DefaultController;
import repast.simphony.engine.environment.AbstractRunner;
import repast.simphony.engine.environment.ControllerRegistry;
import repast.simphony.engine.environment.DefaultRunEnvironmentBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.SweeperProducer;
import simphony.util.messages.MessageCenter;

//https://stackoverflow.com/questions/8708342/redirect-console-output-to-string-in-java/8708357#8708357

public class TestRunner_2 extends AbstractRunner {

	private static MessageCenter msgCenter = MessageCenter.getMessageCenter(TestRunner_2.class);

	private RunEnvironmentBuilder runEnvironmentBuilder;
	protected Controller controller;
	protected boolean pause = false;
	protected Object monitor = new Object();
	protected SweeperProducer producer;
	private ISchedule schedule;
	private Parameters params = null;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();

	public TestRunner_2() {
		runEnvironmentBuilder = new DefaultRunEnvironmentBuilder(this, true);
		controller = new DefaultController(runEnvironmentBuilder);
		controller.setScheduleRunner(this);
	}
	
	public void load(String scenarioDir) throws Exception {
		//System.out.println(scenarioDir);
		load(new File(scenarioDir));
	}

	public void load(File scenarioDir) throws Exception{
		if (scenarioDir.exists()) {
			BatchScenarioLoader loader = new BatchScenarioLoader(scenarioDir);
			ControllerRegistry registry = loader.load(runEnvironmentBuilder);
			controller.setControllerRegistry(registry);
			params = loader.getParameters();
		} else {
			msgCenter.error("Scenario not found", new IllegalArgumentException(
					"Invalid scenario " + scenarioDir.getAbsolutePath()));
			return;
		}
		
		controller.batchInitialize();
		controller.runParameterSetters(params);
	}

	public void runInitialize(){
		controller.runInitialize(params);
		schedule = RunState.getInstance().getScheduleRegistry().getModelSchedule();
//		this.setOutputStream();
	}

	public void cleanUpRun(){
		controller.runCleanup();
	}
	public void cleanUpBatch(){
		controller.batchCleanup();
	}

	// returns the tick count of the next scheduled item
	public double getNextScheduledTime(){
		return ((Schedule)RunEnvironment.getInstance().getCurrentSchedule()).peekNextAction().getNextTime();
	}

	// returns the number of model actions on the schedule
	public int getModelActionCount(){
		return schedule.getModelActionCount();
	}

	// returns the number of non-model actions on the schedule
	public int getActionCount(){
		return schedule.getActionCount();
	}

	// Step the schedule
	public void step(){
		this.baos.reset();
		schedule.execute();
	}
	
	// Run to the specified time step
	public void runTo(double time) {
		if (time <= RunEnvironment.getInstance().getCurrentSchedule().getTickCount()) {
			return;
		}
		this.baos.reset();
		while (this.getActionCount() > 0){  // loop until last action is left
			if (this.getModelActionCount() == 0) {
				this.setFinishing(true);
			}
			this.step();  // execute all scheduled actions at next tick
			
			if (time <= RunEnvironment.getInstance().getCurrentSchedule().getTickCount()) {
				break;
			}
		}
	}

	// stop the schedule
	public void stop(){
		if ( schedule != null )
			schedule.executeEndActions();
	}

	public void setFinishing(boolean fin){
		schedule.setFinishing(fin);
	}

	public void execute(RunState toExecuteOn) {
		// required AbstractRunner stub.  We will control the
		//  schedule directly.
	}
	
	public void update() {
		controller.runCleanup();
		controller.batchCleanup();
	}
	
	public void setOutputStream() {
		//https://stackoverflow.com/questions/8708342/redirect-console-output-to-string-in-java/8708357#8708357	
		PrintStream ps = new PrintStream(baos);
		// IMPORTANT: Save the old System.out!
		PrintStream old = System.out;
		// Tell Java to use your special stream
		System.setOut(ps);
	}
	
	public String getLatestTrace() {
		System.out.flush();
		return this.baos.toString();
	}
	
}