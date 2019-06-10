package zeromq;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;

import com.dukascopy.api.Library;
//import org.zeromq.SocketType;
//import org.zeromq.ZMQ;
//import org.zeromq.ZContext;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.dukascopy.api.feed.util.RangeBarFeedDescriptor;
import com.dukascopy.api.feed.util.RenkoFeedDescriptor;
import com.dukascopy.api.feed.util.TickBarFeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;

import com.dukascopy.api.*;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;


class JForexPrinter {

    IConsole console;

    JForexPrinter(IConsole console){
        this.console = console;
    }

    public void printMe(Object toPrint) {
        console.getOut().println(toPrint);
    }

    public void printMeError(Object o) {
        console.getErr().println(o);
    }
}


@Library("jeromq-0.5.1.jar;gson-2.8.5.jar")
public class ZeroMQClient implements IStrategy{
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;

    private boolean stop = false;

    private ServerThread serverThread;

    public void onStart(IContext context) throws JFException {
//        this.engine = context.getEngine();
//        this.console = context.getConsole();
//        this.history = context.getHistory();
        this.context = context;
//        this.indicators = context.getIndicators();
//        this.userInterface = context.getUserInterface();

        serverThread = new ServerThread("test", context);

    }
    public void onAccount(IAccount account) throws JFException { }

    public void onMessage(IMessage message) throws JFException { }

    public void onStop() throws JFException {
        serverThread.stop(); // stop the thread
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException { }
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException { }
}


class ServerThread implements Runnable {

    Thread thrd;    // A Reference to thread is stored in thrd

    private IContext JFContext;
    private IConsole console;
    private IHistory history;

    JForexPrinter printer;

    IFeedDescriptor feedDescriptor;

    private volatile boolean exit = false;

    private List<HistoryWorkerThread> historyWorkerThreads;

    private int historyWorkerAmount = 0;

//    private HistoryWorkerThread thread;

    ServerThread(String name, IContext JFContext) {
        thrd = new Thread(this, name); // The thread is named when its created

        this.JFContext = JFContext;
        this.console = JFContext.getConsole();
        this.history = JFContext.getHistory();

        printer = new JForexPrinter(console);

//        feedDescriptor = new TimePeriodAggregationFeedDescriptor(
//                Instrument.EURUSD, Period.DAILY, OfferSide.BID, Filter.WEEKENDS);

        thrd.start(); // start the thread
    }

    public void run() {
        try (ZContext zContext = new ZContext()) {
            // Socket to talk to client
            ZMQ.Socket clientSocket = zContext.createSocket(SocketType.REP);
            clientSocket.connect("tcp://*:5555");

            //  Bind to inproc: endpoint
            ZMQ.Socket historyWorkersReqSocket = zContext.createSocket(SocketType.PUB);
            historyWorkersReqSocket.bind("inproc://historyWorkersReq");

            ZMQ.Socket historyWorkersRepSocket = zContext.createSocket((SocketType.PULL));
//            historyWorkersRepSocket.setReceiveTimeOut(100); // only wait 100 ms to receive an update
            historyWorkersRepSocket.bind("inproc://historyWorkersRep");


            Gson gson = new Gson();

            while (!exit) {

                // waiting for request from client
                printer.printMe("waiting for cient request");
                String clientReq = new String(clientSocket.recv(0), ZMQ.CHARSET);

                // start counting the after request received
                long startTime = System.nanoTime();

                ClientRequest clientRequest = gson.fromJson(clientReq, ClientRequest.class);

//                Map clientReqJSON = gson.fromJson(clientReq, Map.class);

                printer.printMe(
                        "Received "  + clientRequest.type
                );


                if(clientRequest.type.equals("setup")){
                    printer.printMe("SETUP");

                    // kill any old workers
                    historyWorkersReqSocket.send("kill");
                    historyWorkerAmount = 0;
//                    stopWorkerThreads();

                    for (Map.Entry<Integer, Integer> entry : clientRequest.time.entrySet()) {

                        int pastBars = entry.getValue();
                        int periodInt = entry.getKey();

                        printer.printMe("TF: "+periodInt + ", pastBars: "+pastBars);

                        historyWorkerAmount++;

                        Period period;

                        if(periodInt < 60) {
                            period = Period.createCustomPeriod(Unit.Minute, entry.getKey(), JFTimeZone.UTC);
                        }
                        else if(periodInt >= 60 && periodInt < 1440){
                            period = Period.createCustomPeriod(Unit.Hour, entry.getKey()/60, JFTimeZone.UTC);
                        }
                        else if(periodInt >= 1440) {
                            period = Period.createCustomPeriod(Unit.Day, entry.getKey()/1440, JFTimeZone.UTC);
                        }
                        else {
                            printer.printMeError("Failed to find the correct Period for: "+periodInt);
                            return;
                        }

                        Instrument instrument = Instrument.fromString(clientRequest.instruments[0]);



//                        IFeedDescriptor feedDescriptor = new TimePeriodAggregationFeedDescriptor(
//                                instrument, period, OfferSide.BID, Filter.WEEKENDS);

                        IFeedDescriptor feedDescriptor = new TimePeriodAggregationFeedDescriptor(
                                instrument, period, OfferSide.BID, Filter.WEEKENDS);

                        printer.printMeError(feedDescriptor.toString());


                        String name = String.format("timebar %s %s", entry.getKey(), entry.getValue());

                        HistoryWorkerThread thread = new HistoryWorkerThread(
                                name, feedDescriptor, entry.getValue(), JFContext, zContext);

                        // wait here until historyWorker responds with their name
                        printer.printMe("waiting for setup response from worker");
                        if(historyWorkersRepSocket.recvStr(0).equals(name)){
                            printer.printMe(name + "ready");
                        }
                    }
                    printer.printMe("send setup done");
                    clientSocket.send("setup done");
                    continue;
                }
                else { // client requesting data from workers

                    historyWorkersReqSocket.send("WORKER TIME REQ");

                    // waiting for responses from workers
                    long waitForResponseStart = System.currentTimeMillis();
                    printer.printMe("waiting for data response from workers");
                    for (int i = 0; i < historyWorkerAmount; i++) {

                        String hwRep = historyWorkersRepSocket.recvStr(0);
                        String[] hwreps = hwRep.split("--");
                        printer.printMe("DATA FROM HW: " + hwreps[0] +
                                "DATA: " + hwreps[1]);
                    }

                    clientSocket.send("response");
                }


//                String historyWorkerRep = new String(historyWorkersReqSocket.recv(0), ZMQ.CHARSET);

//                printer.printMe(historyWorkerRep);
//
//                JSONObject obj = new JSONObject();
//                obj.put("Name", 2);
////                obj.put("LastTick", history.getTimeOfLastTick(Instrument.EURUSD));
////                String response = "world "+history.getTimeOfLastTick(Instrument.EURUSD);
//
//                clientSocket.send(obj.toString());
////                Thread.sleep(100); //  Do some 'work'
//
//
                long stopTime = System.nanoTime();
                printer.printMeError("TIME TAKEN: "+ (stopTime - startTime)/1000 + " us");
            }
        }
//        catch (InterruptedException exc) {
//            printer.printMeError("Exception in the Run ReplyClientThread run() method");
//        }
////        catch (JFException exc) {
////            printer.printMeError("Exception when retrieving Jforex history");
////        }
//        catch (ParseException exc) {
//            printer.printMeError(exc.getMessage());
//            printer.printMeError("Failed to parse Request");

    }

    public enum FeedType {
        TIME,
        RANGE,
        TICKS,

    }


    public void stopWorkerThreads(){
        for (HistoryWorkerThread thread : historyWorkerThreads) {
            thread.stop();
        }
    }

    public void stop() {
        exit = true;

    }
}

class ClientRequest {

    public String type;

    public String[] instruments;

    public Map<Integer, Integer> time;
    public Map<Integer, Integer> range;
    public Map<Integer, Integer> ticks;

    ClientRequest(String type){ }
}

//
//class WorkerResponse {
//
//    public String type;
//
//    public String[] instruments;
//
//
//    WorkerResponse(String type){ }
//}

class HistoryWorkerThread implements Runnable, IFeedListener{
    Thread thrd;    // A Reference to thread is stored in thrd

    JForexPrinter printer;

    private IContext JFContext;
    private IConsole console;
    private IHistory history;
    private IFeedDescriptor feedDescriptor;
    int pastBars;

    private ZContext zContext;

    private volatile boolean exit = false;

    HistoryWorkerThread(String name, IFeedDescriptor feedDescriptor, int pastBars, IContext JFContext, ZContext zContext){
        thrd = new Thread(this, name); // The thread is named when its created

        this.zContext = zContext;
        this.JFContext = JFContext;
        this.feedDescriptor = feedDescriptor;
        this.console = JFContext.getConsole();
        this.history = JFContext.getHistory();
        this.pastBars = pastBars;

        printer = new JForexPrinter(console);

        JFContext.setSubscribedInstruments(java.util.Collections.singleton(feedDescriptor.getInstrument()), true);

        //the subscription important for enabling feed caching - hence history method performance
        JFContext.subscribeToFeed(this.feedDescriptor, this);

        thrd.start(); // start the thread
    }

    public void run(){

        //  Bind to inproc: endpoint
        ZMQ.Socket serverReqSocket = zContext.createSocket(SocketType.SUB);
        serverReqSocket.connect("inproc://historyWorkersReq");
        serverReqSocket.subscribe("");

        ZMQ.Socket serverRepSocket = zContext.createSocket((SocketType.PUSH));
        serverRepSocket.connect("inproc://historyWorkersRep");

        // signal server that the thread is ready to receive requests
        serverRepSocket.send(thrd.getName());

        Gson gson = new Gson();

        while (!exit) {

            try {

//                printer.printMe("WH "+thrd.getName());

//                String serverReq = new String(serverSocket.recv(0), ZMQ.CHARSET);
                // waiting for request from server
                printer.printMe("waiting for request from server - "+thrd.getName());
                String serverReq = serverReqSocket.recvStr(0).trim();

                printer.printMe(serverReq);

                if(serverReq.equals("kill")){
                    printer.printMe("Killing thread "+thrd.getName());
                    stop();
                }
                else {

//                    ITimedData lastFeedData = history.getFeedData(feedDescriptor, 0); //currently forming feed element

                    BigInteger lastTime = new BigInteger("1560066300000");

                    ITimedData lastFeedData = history.getFeedData(feedDescriptor, 0); //currently forming feed element

                    SimpleDateFormat dateFormatUtc = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
                    dateFormatUtc.setTimeZone(TimeZone.getTimeZone("GMT"));

                    printer.printMe("---HW getting history for feed: "+feedDescriptor.toString());

                    Instant lastFeedDataTime = Instant.ofEpochMilli(lastFeedData.getTime());

                    printer.printMe("------------"+
                            "feedDescriptor: "+ feedDescriptor.toString() +
                            "--- lastFeedData: "+lastFeedDataTime+
                                    ", time now: "+ Instant.now()
                    );


                    Instant timeToUse = Instant.now().minus(60, ChronoUnit.MINUTES);


//                    history.getFeedData()

                    feedDescriptor.setPeriod(Period.DAILY);


                    List<ITimedData> feedDataList = history.getFeedData(
                            feedDescriptor, pastBars, lastFeedData.getTime(), 0);

//                    long time = 1560066300000;

//                    printer.printMe("Time "+ (long) lastFeedData.getTime());


                    serverRepSocket.send(thrd.getName() +"--"+ gson.toJson(feedDataList));

//                    serverRepSocket.send("hw response");
                }
//
//            } catch (ParseException exc) {
//                printer.printMeError(exc.getMessage());
//                printer.printMeError("Failed to parse Request");
            } catch (JFException exc) {
                printer.printMeError("Failed to getFeedData for feed: "+feedDescriptor.toString());
                printer.printMeError(exc.getMessage());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            } catch (Exception exc) {
                printer.printMeError("Failed in hw for feedDescriptor: " + feedDescriptor.toString());
                printer.printMeError(exc.getMessage());
            }
        }
    }
    public void stop() {
        exit = true;
    }

    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData) {}
}

