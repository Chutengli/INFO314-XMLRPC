package edu.uw.info314.xmlrpc.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import spark.Request;
import spark.Response;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;

import static spark.Spark.*;

class Call {
    public String name;
    public List<Object> args = new ArrayList<>();
}

public class App {
    public static final Logger LOG = Logger.getLogger(App.class.getCanonicalName());

    private static final int PORT = 8080;

    public static void main(String[] args) {
        LOG.info("Starting up on port " + PORT);
        App serverApp = new App();

        port(PORT);

        before("/RPC", (request, response) -> {
            if(!request.requestMethod().equalsIgnoreCase("post")) {
                halt(405, "Bad Request");
            }
        });


        // This is the mapping for POST requests to "/RPC";
        // this is where you will want to handle incoming XML-RPC requests
        post("/RPC", serverApp::handlePost);

        notFound((request, response) -> {
            response.status(404);
            return "Route Not Found";
        });

        // Each of the verbs has a similar format: get() for GET,
        // put() for PUT, delete() for DELETE. There's also an exception()
        // for dealing with exceptions thrown from handlers.
        // All of this is documented on the SparkJava website (https://sparkjava.com/).
    }

    public static Call extractXMLRPCCall(String xml) throws IOException, ParserConfigurationException, SAXException, IllegalArgumentException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Document document = documentBuilder.parse(input);

        List<Object> args = new ArrayList<>();

        String methodName = document.getElementsByTagName("methodName").item(0).getTextContent();

        NodeList i4s = document.getElementsByTagName("i4");
        if (i4s != null) {
            for(int i = 0; i < i4s.getLength(); i++) {
                Node param = i4s.item(i);
                args.add(Integer.valueOf(param.getTextContent()));
            }
        }

        Call newCall = new Call();
        newCall.name = methodName;
        newCall.args = args;
        return newCall;
    }

    private String handlePost(Request request, Response response) throws ParserConfigurationException {
        try {
            Call methodCall = extractXMLRPCCall(request.body());
            Calc calc = new Calc();

            switch (methodCall.name) {
                case "add" -> {
                    int[] numbers = new int[methodCall.args.size()];
                    for(int i = 0; i < methodCall.args.size(); i++) {
                        numbers[i] = (int) methodCall.args.get(i);
                    }
                    return buildSuccessResponse(calc.add(numbers), response);
                }
                case "subtract" -> {
                    return buildSuccessResponse(calc.subtract((int) methodCall.args.get(0), (int) methodCall.args.get(1)), response);
                }
                case "multiply" -> {
                    int[] numbers = new int[methodCall.args.size()];
                    for(int i = 0; i < methodCall.args.size(); i++) {
                        numbers[i] = (int) methodCall.args.get(i);
                        LOG.info("current Number: " + numbers[i]);
                    }
                    LOG.info(String.valueOf(calc.multiply(numbers)));
                    return buildSuccessResponse(calc.multiply(numbers), response);
                }
                case "divide" -> {
                    if ((int) methodCall.args.get(1) == 0) {
                        throw new ArithmeticException("Divide by zero!");
                    }
                    return buildSuccessResponse(calc.divide((int) methodCall.args.get(0), (int) methodCall.args.get(1)), response);
                }
                case "modulo" -> {
                    if ((int) methodCall.args.get(1) == 0) {
                        throw new ArithmeticException("Divide by zero!");
                    }
                    return buildSuccessResponse(calc.modulo((int) methodCall.args.get(0), (int) methodCall.args.get(1)), response);
                }
                default -> throw new IllegalArgumentException("We have not support that method yet");
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            LOG.warning("Sending back client fault response");
            response.status(400);
            return buildRequestFaultResponse("illegal argument type: " + e.getMessage());
        } catch (Exception e) {
            LOG.warning("Sending back server fault response");
            response.status(500);
            return buildServerFaultResponse(e.getMessage());
        }
    }

    private String buildSuccessResponse(int value, Response response) {
        response.status(200);
        response.header("Host", "localhost:" + PORT);

        return "<?xml version=\"1.0\"?>\n" +
                "<methodResponse>\n" +
                "<params>\n" +
                "<param>\n" +
                String.format("<value><i4>%d</i4></value>\n", value) +
                "</param>\n" +
                "</params>\n" +
                "</methodResponse>\n";
    }

    private String buildServerFaultResponse(String message) {
        return "<?xml version=\"1.0\"?>\n" +
                "<methodResponse>\n" +
                "<fault>\n" +
                "<value>\n" +
                "<struct>\n" +
                "<member>\n" +
                "<name>faultCode</name>\n" +
                String.format("<value><i4>%d</i4></value>\n", 500) +
                "</member>\n" +
                "<member>\n" +
                "<name>faultString</name>\n" +
                String.format("<value><int>%s</int></value>\n", message) +
                "</member>\n" +
                "</struct>\n" +
                "</value>\n" +
                "</fault>\n" +
                "</methodResponse>\n";
    }

    private String buildRequestFaultResponse (String message) {
        return "<?xml version=\"1.0\"?>\n" +
                "<methodResponse>\n" +
                "<fault>\n" +
                "<value>\n" +
                "<struct>\n" +
                "<member>\n" +
                "<name>faultCode</name>\n" +
                String.format("<value><i4>%d</i4></value>\n", 3) +
                "</member>\n" +
                "<member>\n" +
                "<name>faultString</name>\n" +
                String.format("<value><int>%s</int></value>\n", message) +
                "</member>\n" +
                "</struct>\n" +
                "</value>\n" +
                "</fault>\n" +
                "</methodResponse>\n";
    }

    private String getStringFromDocument(Document document) {
        try {
            DOMSource domSource = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
