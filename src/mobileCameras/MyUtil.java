package mobileCameras;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.space.Dimensions;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;

public class MyUtil {
	
	public static int returnBounceAngle(ContinuousSpace<Object> space, NdPoint here, int angleDeg, double speed) {
		Dimensions spaceDims = space.getDimensions();
		assert spaceDims.size() == 2 : "space dimensions must == 2. now the Human class can only handle 2D space";
		//assert spaceDims.getOrigin(0) == 0 && spaceDims.getOrigin(1) == 0: "space origin must be [0,0]";
		
		double deltaX = Math.cos(Math.toRadians(angleDeg)) * speed;
		double deltaY = Math.sin(Math.toRadians(angleDeg)) * speed;
		double newX = here.getX() + deltaX;
		double newY = here.getY() + deltaY;;
		
		double height = spaceDims.getHeight();
		double width = spaceDims.getWidth();
		double minY = spaceDims.getOrigin(1);
		double maxY = minY + height;
		double minX = spaceDims.getOrigin(0);
		double maxX = minX + width;

		int bounceCountX = 0; // bouncing on x axis
		int bounceCountY = 0; // bouncing on y axis
		
	
		if(newX >= maxX) {
			bounceCountY += (int) Math.ceil((newX - maxX) / width);
		}
		if(newX <= minX) {
			bounceCountY += (int) Math.ceil((minX - newX) / width);
		}
		if(newY >= maxY) {
			bounceCountX += (int) Math.ceil((newY - maxY) / height);
		}
		if(newY <= minY) {
			bounceCountX += (int) Math.ceil((minY - newY) / height);
		}
		
		
		int tmp = angleDeg * (int) Math.pow(-1,(bounceCountX % 2)); // reflect by bouncing on x axis
		tmp = 180 * (bounceCountY % 2) + (int) Math.pow(-1, (bounceCountY % 2)) * tmp; // 180 - tmp, reflect by bouncing on y axis
		
		return Math.floorMod(tmp, 360);
		
	}
	public static Document parseScenarioXML(String filePath)  {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(new File(filePath));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return doc;
	}
	
	public static HashMap<Integer, Element> parseCoveredHumans(Document scenario) {
		HashMap<Integer, Element> objMap = new HashMap<>();

		NodeList cameraList = scenario.getElementsByTagName("camera");
		for (int i = 0; i < cameraList.getLength(); i++) {
			NodeList objectList = ((Element) cameraList.item(i)).getElementsByTagName("object");
			for (int j = 0; j < objectList.getLength(); j++) {
				Element obj = (Element) objectList.item(j);
				int id = Integer.parseInt(obj.getAttribute("id"));
				objMap.putIfAbsent(id, obj);
			}
		}
		return objMap;
	}

	public static HashMap<Integer, Element> parseUncoveredHumans(Document scenario) {
		HashMap<Integer, Element> objMap = new HashMap<>();

		Element listNode = (Element) scenario.getElementsByTagName("uncovered_objects").item(0);
		NodeList objectList = listNode.getElementsByTagName("object");
		for (int j = 0; j < objectList.getLength(); j++) {
			Element obj = (Element) objectList.item(j);
			int id = Integer.parseInt(obj.getAttribute("id"));
			objMap.putIfAbsent(id, obj);
		}
		return objMap;
	}
	
	
	public static void scheduling(Context<Object> context, int startTime) {
		int startTime2 = startTime + (6 - (startTime % 5)) % 5;
		
		List<Object> camList = context.getObjectsAsStream(Camera.class).collect(Collectors.toList());
		List<Object> humList = context.getObjectsAsStream(Human.class).collect(Collectors.toList());
		
		Schedule schedule = (Schedule) RunEnvironment.getInstance().getCurrentSchedule();
		
		ScheduleParameters sp101 = ScheduleParameters.createRepeating(startTime, 1, 101);
		ScheduleParameters sp100 = ScheduleParameters.createRepeating(startTime, 1, 100);
		ScheduleParameters sp1Every5 = ScheduleParameters.createRepeating(startTime2, 5, 1); // this one is different
		ScheduleParameters spSecondLast = ScheduleParameters.createRepeating(startTime-1 , 1, ScheduleParameters.LAST_PRIORITY + 1);
		ScheduleParameters spFirst = ScheduleParameters.createRepeating(startTime, 1, ScheduleParameters.FIRST_PRIORITY);
		ScheduleParameters sp3 = ScheduleParameters.createRepeating(startTime, 1, 3);
		ScheduleParameters sp2 = ScheduleParameters.createRepeating(startTime, 1, 2);
		ScheduleParameters spLast = ScheduleParameters.createRepeating(startTime-1, 1, ScheduleParameters.LAST_PRIORITY);
		
		for (Object cam : camList) {
			schedule.schedule(sp101, cam, "sense");
			schedule.schedule(sp100, cam, "thinkAndAct");
//			schedule.schedule(sp1Every5, cam, "clearMsg");
			schedule.schedule(spSecondLast, cam, "printTrace");
		}
		
		for (Object hum : humList) {
			schedule.schedule(spFirst, hum, "run");
		}
		
		schedule.schedule(sp3, context, "evaporateNetwork");
		schedule.schedule(sp2, context, "strengthenNetwork");
		schedule.schedule(spLast, context, "collectTraceForEnv");
		
	}
	
	
	public static double[] updateByBoundaryCheck(double x, double y, double maxX, double maxY) {
		double newX = x;
		double newY = y;
		// check world boundary
		if (x < 0) {
			newX = 0;
		}
		if (x >= maxX) {
			newX = maxX - 0.01;
		}
		if (y < 0) {
			newY = 0;
		}
		if (y >= maxY) {
			newY = maxY - 0.01;
		}
		return new double[] {newX, newY};
	}
	
	
	public static double nextChaoticValue(double x) {
		if (x < 0) {
			x = Math.abs(x);
		}
		if (x > 1) {
			x = x - Math.floor(x);
		}
		return 4 * x * (1-x);
	}
	
}
