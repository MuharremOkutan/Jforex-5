//package zeromq;
//
////import Common.JForexPrinter;
////import JFDataServer.ClientRequest;
//import com.dukascopy.api.*;
//import com.dukascopy.api.feed.IFeedDescriptor;
//import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
//import com.google.gson.Gson;
//import org.zeromq.SocketType;
//import org.zeromq.ZContext;
//import org.zeromq.ZMQ;
//
//import java.util.List;
//import java.util.Map;
//
//public class ServerThread implements Runnable {
//
//    Thread thrd;    // A Reference to thread is stored in thrd
//
//    private IContext JFContext;
//    private IConsole console;
//    private IHistory history;
//
//    JForexPrinter printer;
//
//    IFeedDescriptor feedDescriptor;
//
//    private volatile boolean exit = false;
//
//    private List<HistoryWorkerThread> historyWorkerThreads;
//
//    private int historyWorkerAmount = 0;
//
////    private HistoryWorkerThread thread;
//
//    ServerThread(String name, IContext JFContext) {
//        thrd = new Thread(this, name); // The thread is named when its created
//
//        this.JFContext = JFContext;
//        this.console = JFContext.getConsole();
//        this.history = JFContext.getHistory();
//
//        printer = new JForexPrinter(console);
//
////        feedDescriptor = new TimePeriodAggregationFeedDescriptor(
////                Instrument.EURUSD, Period.DAILY, OfferSide.BID, Filter.WEEKENDS);
//
//        thrd.start(); // start the thread
//    }
//
//    public void run() {
//        try (ZContext zContext = new ZContext()) {
//            // Socket to talk to client
//            ZMQ.Socket clientSocket = zContext.createSocket(SocketType.REP);
//            clientSocket.connect("tcp://*:5555");
//
//            //  Bind to inproc: endpoint
//            ZMQ.Socket historyWorkersReqSocket = zContext.createSocket(SocketType.PUB);
//            historyWorkersReqSocket.bind("inproc://historyWorkersReq");
//
//            ZMQ.Socket historyWorkersRepSocket = zContext.createSocket((SocketType.PULL));
////            historyWorkersRepSocket.setReceiveTimeOut(100); // only wait 100 ms to receive an update
//            historyWorkersRepSocket.bind("inproc://historyWorkersRep");
//
//
//            Gson gson = new Gson();
//
//            while (!exit) {
//
//                // waiting for request from client
//                printer.printMe("waiting for cient request");
//                String clientReq = new String(clientSocket.recv(0), ZMQ.CHARSET);
//
//                // start counting the after request received
//                long startTime = System.nanoTime();
//
////                JFDataServer.ClientRequest clientRequest = gson.fromJson(clientReq, JFDataServer.ClientRequest.class);
//
////                Map clientReqJSON = gson.fromJson(clientReq, Map.class);
//
//                printer.printMe(
//                        "Received "  + clientRequest.type
//                );
//
//
//                if(clientRequest.type.equals("setup")){
//                    printer.printMe("SETUP");
//
//                    // kill any old workers
//                    historyWorkersReqSocket.send("kill");
//                    historyWorkerAmount = 0;
////                    stopWorkerThreads();
//
//                    for (Map.Entry<Integer, Integer> entry : clientRequest.time.entrySet()) {
//
//                        int pastBars = entry.getValue();
//                        int periodInt = entry.getKey();
//
//                        printer.printMe("TF: "+periodInt + ", pastBars: "+pastBars);
//
//                        historyWorkerAmount++;
//
//                        Period period;
//
//                        if(periodInt < 60) {
//                            period = Period.createCustomPeriod(Unit.Minute, entry.getKey(), JFTimeZone.UTC);
//                        }
//                        else if(periodInt >= 60 && periodInt < 1440){
//                            period = Period.createCustomPeriod(Unit.Hour, entry.getKey()/60, JFTimeZone.UTC);
//                        }
//                        else if(periodInt >= 1440) {
//                            period = Period.createCustomPeriod(Unit.Day, entry.getKey()/1440, JFTimeZone.UTC);
//                        }
//                        else {
//                            printer.printMeError("Failed to find the correct Period for: "+periodInt);
//                            return;
//                        }
//
//                        Instrument instrument = Instrument.fromString(clientRequest.instruments[0]);
//
//
//
////                        IFeedDescriptor feedDescriptor = new TimePeriodAggregationFeedDescriptor(
////                                instrument, period, OfferSide.BID, Filter.WEEKENDS);
//
//                        IFeedDescriptor feedDescriptor = new TimePeriodAggregationFeedDescriptor(
//                                instrument, period, OfferSide.BID, Filter.WEEKENDS);
//
//                        printer.printMeError(feedDescriptor.toString());
//
//
//                        String name = String.format("timebar %s %s", entry.getKey(), entry.getValue());
//
//                        HistoryWorkerThread thread = new HistoryWorkerThread(
//                                name, feedDescriptor, entry.getValue(), JFContext, zContext);
//
//                        // wait here until historyWorker responds with their name
//                        printer.printMe("waiting for setup response from worker");
//                        if(historyWorkersRepSocket.recvStr(0).equals(name)){
//                            printer.printMe(name + "ready");
//                        }
//                    }
//                    printer.printMe("send setup done");
//                    clientSocket.send("setup done");
//                    continue;
//                }
//                else { // client requesting data from workers
//
//                    historyWorkersReqSocket.send("WORKER TIME REQ");
//
//                    // waiting for responses from workers
//                    long waitForResponseStart = System.currentTimeMillis();
//                    printer.printMe("waiting for data response from workers");
//                    for (int i = 0; i < historyWorkerAmount; i++) {
//
//                        String hwRep = historyWorkersRepSocket.recvStr(0);
//                        String[] hwreps = hwRep.split("--");
//                        printer.printMe("DATA FROM HW: " + hwreps[0] +
//                                "DATA: " + hwreps[1]);
//                    }
//
//                    clientSocket.send("response");
//                }
//
//
////                String historyWorkerRep = new String(historyWorkersReqSocket.recv(0), ZMQ.CHARSET);
//
////                printer.printMe(historyWorkerRep);
////
////                JSONObject obj = new JSONObject();
////                obj.put("Name", 2);
//////                obj.put("LastTick", history.getTimeOfLastTick(Instrument.EURUSD));
//////                String response = "world "+history.getTimeOfLastTick(Instrument.EURUSD);
////
////                clientSocket.send(obj.toString());
//////                Thread.sleep(100); //  Do some 'work'
////
////
//                long stopTime = System.nanoTime();
//                printer.printMeError("TIME TAKEN: "+ (stopTime - startTime)/1000 + " us");
//            }
//        }
////        catch (InterruptedException exc) {
////            printer.printMeError("Exception in the Run ReplyClientThread run() method");
////        }
//////        catch (JFException exc) {
//////            printer.printMeError("Exception when retrieving Jforex history");
//////        }
////        catch (ParseException exc) {
////            printer.printMeError(exc.getMessage());
////            printer.printMeError("Failed to parse Request");
//
//    }
//
//    public enum FeedType {
//        TIME,
//        RANGE,
//        TICKS,
//
//    }
//
//
//    public void stopWorkerThreads(){
//        for (HistoryWorkerThread thread : historyWorkerThreads) {
//            thread.stop();
//        }
//    }
//
//    public void stop() {
//        exit = true;
//
//    }
//}
