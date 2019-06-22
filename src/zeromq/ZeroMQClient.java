//package zeromq;
//
//import java.math.BigInteger;
//import java.text.SimpleDateFormat;
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//
////import JFDataServer.JForexPrinter;
//import com.dukascopy.api.Library;
////import org.zeromq.SocketType;
////import org.zeromq.ZMQ;
////import org.zeromq.ZContext;
//import com.dukascopy.api.feed.IFeedDescriptor;
//import com.dukascopy.api.feed.IFeedListener;
//
//import com.dukascopy.api.*;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonSyntaxException;
//import org.zeromq.SocketType;
//import org.zeromq.ZContext;
//import org.zeromq.ZMQ;
//
//
//@Library("jeromq-0.5.1.jar;gson-2.8.5.jar")
//public class ZeroMQClient implements IStrategy{
//    private IEngine engine;
//    private IConsole console;
//    private IHistory history;
//    private IContext context;
//    private IIndicators indicators;
//    private IUserInterface userInterface;
//
//    private boolean stop = false;
//
//    private ServerThread serverThread;
//
//    public void onStart(IContext context) throws JFException {
////        this.engine = context.getEngine();
////        this.console = context.getConsole();
////        this.history = context.getHistory();
//        this.context = context;
////        this.indicators = context.getIndicators();
////        this.userInterface = context.getUserInterface();
//
//        serverThread = new ServerThread("test", context);
//
//    }
//    public void onAccount(IAccount account) throws JFException { }
//
//    public void onMessage(IMessage message) throws JFException { }
//
//    public void onStop() throws JFException {
//        serverThread.stop(); // stop the thread
//    }
//
//    public void onTick(Instrument instrument, ITick tick) throws JFException { }
//    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException { }
//}
//
//
//class ClientRequest {
//    /*
//    signifies the request id, if the id already exist then there is no no need to parse the rest of the message only the time
//    Instead just find the old thread with that id and pass the time to it.
//     */
//    public int id;
//
//    public String[] instruments;
//
//    public Map<Integer, Integer> time;
//    public Map<Integer, Integer> range;
//    public Map<Integer, Integer> ticks;
//
//    ClientRequest(){ }
//}
//
////
////class WorkerResponse {
////
////    public String type;
////
////    public String[] instruments;
////
////
////    WorkerResponse(String type){ }
////}
//
//class HistoryWorkerThread implements Runnable, IFeedListener{
//    Thread thrd;    // A Reference to thread is stored in thrd
//
////    JForexPrinter printer;
//
//    private IContext JFContext;
//    private IConsole console;
//    private IHistory history;
//    private IFeedDescriptor feedDescriptor;
//    int pastBars;
//
//    private ZContext zContext;
//
//    private volatile boolean exit = false;
//
//    HistoryWorkerThread(String name, IFeedDescriptor feedDescriptor, int pastBars, IContext JFContext, ZContext zContext){
//        thrd = new Thread(this, name); // The thread is named when its created
//
//        this.zContext = zContext;
//        this.JFContext = JFContext;
//        this.feedDescriptor = feedDescriptor;
//        this.console = JFContext.getConsole();
//        this.history = JFContext.getHistory();
//        this.pastBars = pastBars;
//
////        printer = new JForexPrinter(console);
//
//        JFContext.setSubscribedInstruments(java.util.Collections.singleton(feedDescriptor.getInstrument()), true);
//
//        //the subscription important for enabling feed caching - hence history method performance
//        JFContext.subscribeToFeed(this.feedDescriptor, this);
//
//        thrd.start(); // start the thread
//    }
//
//    public void run(){
//
//        //  Bind to inproc: endpoint
//        ZMQ.Socket serverReqSocket = zContext.createSocket(SocketType.SUB);
//        serverReqSocket.connect("inproc://historyWorkersReq");
//        serverReqSocket.subscribe("");
//
//        ZMQ.Socket serverRepSocket = zContext.createSocket((SocketType.PUSH));
//        serverRepSocket.connect("inproc://historyWorkersRep");
//
//        // signal server that the thread is ready to receive requests
//        serverRepSocket.send(thrd.getName());
//
//        Gson gson = new Gson();
//
//        while (!exit) {
//
//            try {
//
////                printer.printMe("WH "+thrd.getName());
//
////                String serverReq = new String(serverSocket.recv(0), ZMQ.CHARSET);
//                // waiting for request from server
//                printer.printMe("waiting for request from server - "+thrd.getName());
//                String serverReq = serverReqSocket.recvStr(0).trim();
//
//                printer.printMe(serverReq);
//
//                if(serverReq.equals("kill")){
//                    printer.printMe("Killing thread "+thrd.getName());
//                    stop();
//                }
//                else {
//
////                    ITimedData lastFeedData = history.getFeedData(feedDescriptor, 0); //currently forming feed element
//
//                    BigInteger lastTime = new BigInteger("1560066300000");
//
//                    ITimedData lastFeedData = history.getFeedData(feedDescriptor, 0); //currently forming feed element
//
//                    SimpleDateFormat dateFormatUtc = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
//                    dateFormatUtc.setTimeZone(TimeZone.getTimeZone("GMT"));
//
//                    printer.printMe("---HW getting history for feed: "+feedDescriptor.toString());
//
//                    Instant lastFeedDataTime = Instant.ofEpochMilli(lastFeedData.getTime());
//
//                    printer.printMe("------------"+
//                            "feedDescriptor: "+ feedDescriptor.toString() +
//                            "--- lastFeedData: "+lastFeedDataTime+
//                                    ", time now: "+ Instant.now()
//                    );
//
//
//                    Instant timeToUse = Instant.now().minus(60, ChronoUnit.MINUTES);
//
//
////                    history.getFeedData()
//
//                    feedDescriptor.setPeriod(Period.DAILY);
//
//
//                    List<ITimedData> feedDataList = history.getFeedData(
//                            feedDescriptor, pastBars, lastFeedData.getTime(), 0);
//
////                    long time = 1560066300000;
//
////                    printer.printMe("Time "+ (long) lastFeedData.getTime());
//
//
//                    serverRepSocket.send(thrd.getName() +"--"+ gson.toJson(feedDataList));
//
////                    serverRepSocket.send("hw response");
//                }
////
////            } catch (ParseException exc) {
////                printer.printMeError(exc.getMessage());
////                printer.printMeError("Failed to parse Request");
//            } catch (JFException exc) {
//                printer.printMeError("Failed to getFeedData for feed: "+feedDescriptor.toString());
//                printer.printMeError(exc.getMessage());
//            } catch (JsonSyntaxException e) {
//                e.printStackTrace();
//            } catch (Exception exc) {
//                printer.printMeError("Failed in hw for feedDescriptor: " + feedDescriptor.toString());
//                printer.printMeError(exc.getMessage());
//            }
//        }
//    }
//    public void stop() {
//        exit = true;
//    }
//
//    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData) {}
//}
//
