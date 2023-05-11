import java.io.*;
import java.net.*;
import java.net.http.*;
import java.rmi.ServerException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This approach uses the java.net.http.HttpClient classes, which
 * were introduced in Java11.
 */
public class Client {
    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    private static String host;
    private static int port;

    public static void main(String... args) throws Exception {
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);

            System.out.println(add(Integer.MAX_VALUE, Integer.MAX_VALUE)); // should get a response with error code
            System.out.println(divide(Integer.MAX_VALUE, 0)); // should get a response with error code
            System.out.println(multiply(Integer.MAX_VALUE, Integer.MAX_VALUE)); // should get a response with error code

            System.out.println(add() == 0);
            System.out.println(add(1, 2, 3, 4, 5) == 15);
            System.out.println(add(2, 4) == 6);
            System.out.println(subtract(12, 6) == 6);
            System.out.println(multiply(3, 4) == 12);
            System.out.println(multiply(1, 2, 3, 4, 5) == 120);
            System.out.println(divide(10, 5) == 2);
            System.out.println(modulo(10, 5) == 0);

        } catch (Exception ignore) {
        }

    }
    public static int add(int lhs, int rhs) throws Exception {
        return (int) sendHttpRequest("add", new Integer[]{lhs, rhs}).get(0);
    }
    public static int add(Integer... params) throws Exception {
        List<Object> results = sendHttpRequest("add", params);
        if (results == null || results.size() == 0) {
            return 0;
        }
        return (int) sendHttpRequest("add", params).get(0);
    }
    public static int subtract(int lhs, int rhs) throws Exception {
        return (int) sendHttpRequest("subtract", new Integer[]{lhs, rhs}).get(0);
    }
    public static int multiply(int lhs, int rhs) throws Exception {
        return (int) sendHttpRequest("multiply", new Integer[]{lhs, rhs}).get(0);
    }
    public static int multiply(Integer... params) throws Exception {
        List<Object> results = sendHttpRequest("multiply", params);
        if (results == null || results.size() == 0) {
            return 0;
        }
        return (int) sendHttpRequest("multiply", params).get(0);
    }
    public static int divide(int lhs, int rhs) throws Exception {
        return (int) sendHttpRequest("divide", new Integer[]{lhs, rhs}).get(0);
    }
    public static int modulo(int lhs, int rhs) throws Exception {
        return (int) sendHttpRequest("modulo", new Integer[]{lhs, rhs}).get(0);
    }

    private static List<Object> sendHttpRequest(String methodName, Integer[] args) throws URISyntaxException, IOException, InterruptedException, ParserConfigurationException, SAXException, ServerException {
        HttpClient httpClient = HttpClient.newHttpClient();
        String requestBody = buildRequestBody(methodName, args);

        String url = String.format("http://%s:%s/RPC", host, String.valueOf(port));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());

        return getAnswerFromResponse(response);
    }

    private static List<Object> getAnswerFromResponse(HttpResponse<String> response) throws IOException, ParserConfigurationException, SAXException {
        String responseBody = response.body();
        if(responseBody.contains("<fault>")) {
            return null;
        }

        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(responseBody)));

        NodeList elements = document.getElementsByTagName("i4");
        List<Object> responseResult = new ArrayList<>();
        for(int i = 0; i < elements.getLength(); i++) {

            responseResult.add(Integer.valueOf(elements.item(i).getTextContent()));
        }

        return responseResult;
    }

    private static String buildRequestBody(String methodName, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<methodCall>\n");
        sb.append(String.format("<methodName>%s</methodName>\n", methodName));
        String parameters = "<params>\n";
        for (Object param: args) {
            parameters += "<param><value><i4>" + param + "</i4></value></param>";
        }
        parameters += "</params>\n";
        sb.append(parameters);
        sb.append("</methodCall>\n");
        return sb.toString();
    }
}
