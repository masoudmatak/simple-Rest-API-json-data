package com.expertake.springboot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@RestController
public class MasoudController {
	private final static String THE_ENDPOINT = "http://latencyapi-env.eba-kqb2ph3i.eu-west-1.elasticbeanstalk.com/latencies?date=";

	@GetMapping("/")
	public String index() {
		return "you are invoking Masoud's server on Azure";
	}

	@GetMapping("/latencies")
	public String latencies(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate) {
		Map<Integer, AverageLatencies> map = new HashMap<Integer, AverageLatencies>();
		return getLatency(startDate, endDate, map);
	}

	/**
	 * loop and add for each day
	 * 
	 * @param start
	 * @param end
	 * @param map
	 * @return
	 */
	private String getLatency(String start, String end, Map<Integer, AverageLatencies> map) {
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);
		LocalDate d = startDate;
		String averageLatenciesJson = "";
		try {
			getLatencyEachDay(start, map);
			while (d.isBefore(endDate)) {
				d = d.plusDays(1);
				getLatencyEachDay(d.toString(), map);
			}
			Iterator<Integer> id = map.keySet().iterator();
			while (id.hasNext()) {
				if (averageLatenciesJson.equals("")) {
					averageLatenciesJson = map.get(id.next()).toString();
				} else {
					averageLatenciesJson = averageLatenciesJson + "," + map.get(id.next()).toString();
				}
			}

		} catch (Exception e) {
			System.out.println(e);
		}
		// I could use Gson to build Json with an Entity, but for the task it is
		// enough..
		String json = "{\"period\":[\"" + startDate + "\",\" " + endDate + "\"],";
		return json + "\"averageLatencies\": [" + averageLatenciesJson + "]}";

	}

	/**
	 * check the result for one day from the server
	 * 
	 * @param date
	 * @param map
	 * @throws Exception
	 */
	public void getLatencyEachDay(String date, Map<Integer, AverageLatencies> map) throws Exception {
		
		String urlTask = THE_ENDPOINT + date;
		Gson gson = new Gson();
		HttpRequest getReq = HttpRequest.newBuilder().uri(new URI(urlTask)).build();
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpResponse<String> getResp = httpClient.send(getReq, BodyHandlers.ofString());
		JsonElement je = gson.fromJson(getResp.body(), JsonElement.class);
		JsonArray initialData = je.getAsJsonArray();

		for (JsonElement jo : initialData) {
			InputData elem = gson.fromJson(jo, InputData.class);
			int id = elem.getServiceId();
			int latency = elem.getMilliSecondsDelay();
			AverageLatencies serviecObj;
			if (!map.containsKey(elem.getServiceId())) {
				serviecObj = new AverageLatencies();
				serviecObj.numberOfRequests = 1;
				serviecObj.averageResonseTimeMs = elem.getMilliSecondsDelay();
				serviecObj.serviceId = id;
				map.put(id, serviecObj);
			} else {
				serviecObj = map.get(id);
				serviecObj.numberOfRequests++;
				serviecObj.averageResonseTimeMs = (serviecObj.averageResonseTimeMs + latency) / 2;
				map.put(elem.getServiceId(), serviecObj);
			}
		}

	}

	public static void main(String[] args) {
		try {
			MasoudController contrloer = new MasoudController();
			Map<Integer, AverageLatencies> map = new HashMap<Integer, AverageLatencies>();
			long timeStart=System.currentTimeMillis();
			System.out.println(contrloer.getLatency("2021-01-01", "2021-12-02", map));
			long timeFinish=System.currentTimeMillis();
			System.out.println("execution time i miliSec "+(timeFinish-timeStart));
			
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}

/**
 * local classes
 * 
 * @author Masoud
 *
 */
class InputData {
	private int requestId;
	private int serviceId;
	private String date;
	private int milliSecondsDelay;

	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public int getServiceId() {
		return serviceId;
	}

	public void setServiceId(int serviceId) {
		this.serviceId = serviceId;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getMilliSecondsDelay() {
		return milliSecondsDelay;
	}

	public void setMilliSecondsDelay(int milliSecondsDelay) {
		this.milliSecondsDelay = milliSecondsDelay;
	}
}

class AverageLatencies {
	int serviceId;
	int numberOfRequests;
	int averageResonseTimeMs;

	public String toString() {
		return "{\"serviceId\":" + serviceId + ", \"numberOfRequests\":" + numberOfRequests
				+ ", \"averageResonseTimeMs\":" + averageResonseTimeMs + "}";

	}

}