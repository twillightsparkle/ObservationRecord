package com.o3.server;

import com.sun.net.httpserver.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.json.JSONArray;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import com.sun.net.httpserver.Authenticator.Result;



public class Server implements HttpHandler {
    //private ArrayList<ObservationRecord> messages = new ArrayList<ObservationRecord>(); // Array to store messages
    UserAuthenticator ua;
    HttpPrincipal principal;

    private Server(UserAuthenticator ua) {
        this.ua = ua;
    }

    private Server() {
    }


    @Override
    public void handle(HttpExchange t) throws IOException {
    System.out.println("Request handled in thread " + Thread.currentThread().threadId());
    // Set CORS headers
    t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    t.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
    t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

    if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
        // Preflight request, respond with OK and exit
        System.out.println("Preflight request received");
        t.sendResponseHeaders(204, -1); // No Content
        return;
    }

    Result result = ua.authenticate(t);
    if (result instanceof Authenticator.Success) {
        principal = ((Authenticator.Success) result).getPrincipal();
        System.out.println("User authenticated: " + principal.getUsername());
    } else {
        System.out.println("Authentication failed");
        t.sendResponseHeaders(401, -1); // Unauthorized
        return;
    }
    //implement GET and POST handling 
	if (t.getRequestMethod().equalsIgnoreCase("POST")) {
		// Handle POST requests here (users send this for sending messages)
		handlePost(t);
		} else if (t.getRequestMethod().equalsIgnoreCase("GET")) {
		// Handle GET requests here (users use this to get messages)
		handleGet(t);
		} else if (t.getRequestMethod().equalsIgnoreCase("PUT")) {
        // Handle PUT requests here (users use this to update messages)
        handlePut(t);
        } else if(t.getRequestMethod().equalsIgnoreCase("DELETE")) {
        // Handle DELETE requests here (users use this to delete messages)
        handleDelete(t);
        } else {
		// Inform user here that only POST and GET functions are supported and send an error code
		//400 with a message “Not supported” (witouth the “).
        System.out.println("Unsupported method: " + t.getRequestMethod());
		handleUnsupportedMethod(t);
		}
		
    }

	private void handlePost(HttpExchange exchange) throws IOException {
        // Read request body
        System.out.println("POST request received");
        InputStream inputStream = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String requestBody = reader.lines().reduce("", (acc, line) -> acc + line);
        reader.close();
        inputStream.close();
        
        
        try{
            String query = exchange.getRequestURI().getQuery();
            // Parse query parameters
            int shift = 0;
            if(query != null){
                Map<String, String> params = parseQuery(query);
                if (params.size() > 1) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                } else if (params.size() == 1 && params.containsKey("shift")) {
                    shift = Integer.parseInt(params.get("shift"));
                }
            }


            String userNickname = (principal != null) ? MessageDatabase.getInstance().getUserNickname(principal.getUsername()) : "Unknown";
            JSONObject json = new JSONObject(requestBody);
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            String dateText = now.format(formatter);

            if(json.has("recordOwner") == false){
                json.put("recordOwner", userNickname);
            }

            String Observatory;
            String observatoryWeatherStr = null;
            if(json.has("observatory") == false){
                Observatory = null;
            }else{
                Observatory = json.getJSONArray("observatory").toString();
                JSONArray observatories = new JSONArray(Observatory);
                for (int i = 0; i < observatories.length(); i++) {
                    JSONObject observatory = observatories.getJSONObject(i);
                    if (!observatory.has("observatoryName") || !observatory.has("latitude") || !observatory.has("longitude") || observatory.getString("observatoryName").isEmpty() || observatory.isNull("latitude") || observatory.isNull("longitude")) {
                        throw new IllegalArgumentException("Each observatory must have 'observatoryName', 'latitude', and 'longitude' keys.");
                    }
                }
                if(json.has("observatoryWeather")){
                    JSONArray observatoryWeather = json.getJSONArray("observatoryWeather");
                    if(observatoryWeather.length() < observatories.length()){
                        for (int i = observatoryWeather.length(); i < observatories.length(); i++) {
                            JSONObject weatherInfo = getWeatherInfo(observatories.getJSONObject(i).getDouble("latitude"), observatories.getJSONObject(i).getDouble("longitude"));
                            observatoryWeather.put(weatherInfo);
                        }
                    }
                    observatoryWeatherStr = observatoryWeather.toString();
                }
            }

            ObservationRecord message = new ObservationRecord(
            json.getString("recordIdentifier"), 
            json.getString("recordDescription"), 
            json.getString("recordPayload"), 
            json.getString("recordRightAscension"), 
            json.getString("recordDeclination"),
            json.getString("recordOwner"), 
            dateText,
            Observatory,
            observatoryWeatherStr);
            // Store the message in the array
            
            MessageDatabase.getInstance().setMessage(message, principal.getUsername(), shift);
            

            // Send a success response
            exchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            // Send a failure response
            System.out.println("Error: " + e);
            exchange.sendResponseHeaders(400, -1);
        }
       
	}

	private void handleGet(HttpExchange exchange) throws IOException {

        System.out.println("GET request received");
        StringBuilder responseBuilder = new StringBuilder();
        try{
        JSONArray responseMessages = MessageDatabase.getInstance().getMessages(principal.getUsername());
        if (responseMessages.length() == 0) {
            responseBuilder.append("No messages");
        } else {
            responseBuilder.append(responseMessages.toString(4));
        }
        String response = responseBuilder.toString().trim(); // Remove trailing newline
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, responseBytes.length);
        
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
        } catch (Exception e) {
            System.out.println("Error: " + e);
            exchange.sendResponseHeaders(400, -1);
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        // Read request body
        System.out.println("PUT request received");
        InputStream inputStream = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String requestBody = reader.lines().reduce("", (acc, line) -> acc + line);
        reader.close();
        inputStream.close();
        try{
            String query = exchange.getRequestURI().getQuery();
            if (query == null || query.trim().isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
    
            // Parse query parameters
            Map<String, String> params = parseQuery(query);
            if (params.size() != 1 || !params.containsKey("id")) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            int recordId = Integer.parseInt(params.get("id"));
            String userNickname = (principal != null) ? MessageDatabase.getInstance().getUserNickname(principal.getUsername()) : "Unknown";
            JSONObject json = new JSONObject(requestBody);

            if(json.has("recordOwner") == false){
                json.put("recordOwner", userNickname);
            }

            String Observatory;
            if(json.has("observatory") == false){
                Observatory = null;
            }else{
                Observatory = json.getJSONArray("observatory").toString();
                JSONArray observatories = new JSONArray(Observatory);
                for (int i = 0; i < observatories.length(); i++) {
                    JSONObject observatory = observatories.getJSONObject(i);
                    if (!observatory.has("observatoryName") || !observatory.has("latitude") || !observatory.has("longitude") || observatory.getString("observatoryName").isEmpty() || observatory.isNull("latitude") || observatory.isNull("longitude")) {
                        throw new IllegalArgumentException("Each observatory must have 'observatoryName', 'latitude', and 'longitude' keys.");
                    }
                }
            }
            ObservationRecord message = new ObservationRecord(
            json.has("recordIdentifier") ? json.getString("recordIdentifier") : null, 
            json.has("recordDescription") ? json.getString("recordDescription") : null, 
            json.has("recordPayload") ? json.getString("recordPayload") : null, 
            json.has("recordRightAscension") ? json.getString("recordRightAscension") : null, 
            json.has("recordDeclination") ? json.getString("recordDeclination") : null,
            json.has("recordOwner") ? json.getString("recordOwner") : null, 
            Observatory);
            
            // Store the message in the array
            if(json.has("updateReason") == false){
                MessageDatabase.getInstance().updateMessage(recordId, principal.getUsername(),null ,message);
            }else {
            MessageDatabase.getInstance().updateMessage(recordId, principal.getUsername(), json.getString("updateReason") ,message);
            }

            // Send a success response
            exchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            exchange.sendResponseHeaders(400, -1);
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        // Read request body
        System.out.println("DELETE request received");
        try{
            String query = exchange.getRequestURI().getQuery();
            if (query == null || query.trim().isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
    
            // Parse query parameters
            Map<String, String> params = parseQuery(query);
            if (params.size() != 1 || !params.containsKey("id")) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            int recordId = Integer.parseInt(params.get("id"));
            
            // Store the message in the array
            MessageDatabase.getInstance().deleteMessage(recordId);

            // Send a success response
            exchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            exchange.sendResponseHeaders(400, -1);
        }
    }

	private void handleUnsupportedMethod(HttpExchange exchange) throws IOException {
        String response = "Not supported";
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, responseBytes.length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }


    private JSONObject getWeatherInfo(double latitude, double longitude) {
        // Implement this function to get weather information from the observatory
        try {
            // Initialize the document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse the XML file from URL
            URI uri = new URI("http://127.0.0.1:4001/wfs?latlon="+latitude+","+longitude);
            URL url = uri.toURL();
            InputStream inputStream = url.openStream();
            Document document = builder.parse(inputStream);

            // Normalize the XML structure
            document.getDocumentElement().normalize();

            // Get all wfs:member elements
            NodeList nodeList = document.getElementsByTagName("wfs:member");

            JSONObject weatherInfo = new JSONObject();
            // Iterate through each wfs:member element
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Get BsWfs:BsWfsElement element
                    Element bsWfsElement = (Element) element.getElementsByTagName("BsWfs:BsWfsElement").item(0);

                    // Extract and print details
                    String parameterName = bsWfsElement.getElementsByTagName("BsWfs:ParameterName").item(0).getTextContent();
                    String parameterValue = bsWfsElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent();

                    if (parameterName.equals("temperatureInKelvins")) {
                        weatherInfo.put("temperatureInKelvins", parameterValue);
                    } else if (parameterName.equals("cloudinessPercentance")) {
                        weatherInfo.put("cloudinessPercentance", parameterValue);
                    } else if (parameterName.equals("bagroundLightVolume")) {
                        weatherInfo.put("bagroundLightVolume", parameterValue);
                    }

                }
            }
            return weatherInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Parse query parameters
    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }



    public static void main(String[] args) throws Exception {
        try {
        MessageDatabase.getInstance().open("exampledb.db");;
        HttpServer server = HttpServer.create(new InetSocketAddress(8001),0);
        
        /* This is when use https for this server
        SSLContext sslContext = myServerSSLContext(args);
        server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
            public void configure (HttpsParameters params) {
            InetSocketAddress remote = params.getClientAddress();
            SSLContext c = getSSLContext();
            SSLParameters sslparams = c.getDefaultSSLParameters();
            params.setSSLParameters(sslparams);
            }
            });
        */

        UserAuthenticator ua = new UserAuthenticator();
        //create context that defines path for the resource, in this case a "help"
        server.createContext("/datarecord", new Server(ua));
        server.createContext("/search", new SearchHandler(ua));
        server.createContext("/view", new ViewHandler(ua));
        server.createContext("/rating", new RatingHandler(ua));
        server.createContext("/comment", new CommentHandler(ua));
        server.createContext("/toprecords", new GetTopRecordsHandler(ua));
        server.createContext("/login", new LoginHandler(ua));
        server.createContext("/registration", new RegistrationHandler());
        server.createContext("/userinfo", new GetUserinfoHandler(ua));
        // creates a default executor
        server.setExecutor(Executors.newCachedThreadPool());
        server.start(); 
		System.out.println("Server is running on http://127.0.0.1:8001/datarecord");
        } catch (FileNotFoundException e) {
        // Certificate file not found!
        System.out.println("Certificate not found!");
        e.printStackTrace();
        } catch (Exception e) {
        e.printStackTrace();
    }

    }


    private static SSLContext myServerSSLContext(String[] args) throws Exception{
        //char[] passphrase = args[1].toCharArray();
        char[] passphrase = "123456".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        //ks.load(new FileInputStream(args[0]), passphrase);
        ks.load(new FileInputStream("keystore.jks"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }
}

