package JFDataServer;

import Common.JForexPrinter;
import com.dukascopy.api.*;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.dukascopy.api.feed.util.RangeBarFeedDescriptor;
import com.dukascopy.api.feed.util.RenkoFeedDescriptor;
import com.dukascopy.api.feed.util.TickBarFeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jdk.nashorn.internal.ir.IfNode;
import org.apache.commons.lang3.ArrayUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.time.Instant;
import java.util.*;


@Library("jeromq-0.5.1.jar;gson-2.8.5.jar")
public class JFDataServer implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;

    private boolean stop = false;

    JFZeroMQReqListener reqListener;

    public void onStart(IContext context) throws JFException {

        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;

        reqListener = new JFZeroMQReqListener(context);

    }

    public void onAccount(IAccount account) throws JFException { }

    public void onMessage(IMessage message) throws JFException { }

    public void onStop() throws JFException {
        reqListener.stop(); // stop the thread
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException { }
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException { }


//    void zeroMQReqListener() {
//        try (ZContext zContext = new ZContext()) {
//
//            ZMQ.Socket clientSocket = zContext.createSocket(SocketType.REP);
//            clientSocket.connect("tcp://*:5555");
//        }
//    }
}

class ClientRequest {
    /*
    signifies the request id, if the id already exist then there is no no need to parse the rest of the message only the time
    Instead just find the old thread with that id and pass the time to it.
     */
    public Integer id;

    public Long time;

    public String[] instruments; // written like XXX/XXX, i.e EUR/USD TODO could be a mistake to allow multiple symbols
    public String filter; // # NO_FILTER, WEEKENDS or ALL_FLATS
    public String offerside; // BID or ASK

    public String timeBarUnit; // MIN as default, will change the unit of all req timeBars

    public Map<Integer, Integer> timeBar;
    public Map<Integer, Integer> rangeBar;
    public Map<Integer, Integer> renkoBar;

    public Map<Integer, Integer> tickBar;

    ClientRequest(){ }
}

class ClientResponse {

    public Integer id;

    public Long timestamp;
    public String time;

    public List<FeedResponse> feedResponses = new ArrayList<FeedResponse>();


    public void addFeedResponse(FeedResponse feedResponse){
        feedResponses.add(feedResponse);
    }

    public List<FeedResponse> getFeedResponses() {
        return feedResponses;
    }

    class FeedResponse {
        // One per feedDescriptor
        public String instrument;
        public String type;
        public String unit;
        public Integer period;
        public Integer range;

        public List<ITimedData> feedDatas = new ArrayList<>();

        public double[] ema5;
        public double[] ema20;

        public double[] sma5;
        public double[] sma20;

        public double[] rsi14;

        public double[][] bbands20_2;
        public double[][] bbands20_1618;
        public double[][] bbands20_2618;
        public double[][] bbands20_4236;

        public double[][] stoch5_3_3;
        public double[][] macd12_26_9;

        public double[] adx14;
        public double[] cci14;
        public double[] atr14;

        public double[][] fractal5;
        public double[][] supRes;
    }
}


//class FeedRequest {
//
//    Map<IFeedDescriptor, Integer> feedDescriptors;
//
//    FeedRequest(Map<IFeedDescriptor, Integer> feedDescriptors) {
//        this.feedDescriptors = feedDescriptors;
//    }
//}

class JFZeroMQReqListener implements Runnable, IFeedListener {
    /*
    Listens for ZeroMQ REQ requests over tcp from other applications and
    starts threads to handle those requests
     */
    Thread thrd;    // A Reference to thread is stored in thrd
    private IContext JFContext;
    private IConsole console;
    private IHistory history;

    private IIndicators indicators;

    JForexPrinter printer;

    IFeedDescriptor feedDescriptor;
    Gson gson;

    private volatile boolean exit = false;

    Map<Integer, Map<IFeedDescriptor, Integer>> feedRequests = new HashMap<>();


    JFZeroMQReqListener(IContext JFContext) {
        thrd = new Thread(this); // The thread is named when its created

        this.JFContext = JFContext;
        this.console = JFContext.getConsole();
        this.history = JFContext.getHistory();

        this.indicators = JFContext.getIndicators();

        printer = new JForexPrinter(console);

//        gson = new Gson();


        gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();


//        gson = new GsonBuilder.serializeSpecialFloatingPointValues()

        thrd.start(); // start the thread
    }

    @Override
    public void run() {

        try (ZContext zContext = new ZContext()) {

            ZMQ.Socket clientSocket = zContext.createSocket(SocketType.REP);
            clientSocket.connect("tcp://*:5555");

            while (!exit) {

                // waiting for request from client
                printer.printMe("waiting for client request");
                String clientReq = new String(clientSocket.recv(0), ZMQ.CHARSET);
                ClientRequest clientRequest = gson.fromJson(clientReq, ClientRequest.class);
                // start counting the after request received
                long startTime = System.nanoTime();

                printer.printMe(
                "Received "  + clientRequest.id +
                        " ,time "   + Instant.ofEpochMilli(clientRequest.time)
                );

                try {
                    long endTime;
                    long timeSpent;

                    Set<Instrument> instruments = new HashSet<Instrument>();

                    if (feedRequests.isEmpty() || !feedRequests.containsKey(clientRequest.id)) {
                        feedRequests.put(clientRequest.id, getFeedRequestFromRequest(clientRequest));
                        //                    clientSocket.send("req received created new "+clientRequest.id);


                        for (IFeedDescriptor feedDescriptor : feedRequests.get(clientRequest.id).keySet()) {
                            printer.printMe("Subscibing to "+feedDescriptor);

                            instruments.add(feedDescriptor.getInstrument());

                            //Subscribe to a feed, speeds up getting data
                            JFContext.subscribeToFeed(feedDescriptor, this);
                        }
                        JFContext.setSubscribedInstruments(instruments, true);

                        endTime = System.nanoTime();
                        timeSpent = endTime - startTime;

                        printer.printMe("----- NEW ----- " + timeSpent);

                    } else {
                        endTime = System.nanoTime();
                        timeSpent = endTime - startTime;

                        printer.printMe("----- OLD ----- " + timeSpent);
                        //                    clientSocket.send("req received fetched old "+clientRequest.id);
                    }

                    ClientResponse clientResponse = getFeedHistory(feedRequests.get(clientRequest.id),
                            Instant.ofEpochMilli(clientRequest.time),
                            OfferSide.fromString(clientRequest.offerside));

                    clientResponse.id = clientRequest.id;


                    String response = gson.toJson(clientResponse);

                    printer.printMe(response);

                    clientSocket.send(response);
                }

//                catch (JFException e){
//
//                    printer.printMeError("JFException when getting history");
//                    e.printStackTrace(console.getErr());
//                    printer.printMeError(e.toString());
//                    printer.printMeError(e.getMessage());
//                    printer.printMeError(e.getStackTrace());
//
//                    clientSocket.send(gson.toJson(e.getMessage()));
//                }

                catch (Exception e) {
                    e.printStackTrace(console.getErr());
                    printer.printMeError(e.toString());
                    printer.printMeError(e.getMessage());
                    printer.printMeError(e.getStackTrace());

                    clientSocket.send(gson.toJson(e.getMessage()));
                }
            }
        }
    }

    public Map<IFeedDescriptor, Integer> getFeedRequestFromRequest(ClientRequest clientRequest) {

        Map<IFeedDescriptor, Integer> returnMap = new HashMap<IFeedDescriptor, Integer>();

        Filter filter = Filter.valueOf(clientRequest.filter);
        OfferSide offerSide =  OfferSide.fromString(clientRequest.offerside);

        try {
            for (String instr : clientRequest.instruments) {

                Instrument instrument = Instrument.fromString(instr);

                ///// TIME AGGREGATION
                for (Map.Entry<Integer, Integer> timeBarReq : clientRequest.timeBar.entrySet()) {
                    /*
                    Calculates a rounded custom time period. For example 12 days gets rounded to 2 weeks and so forth.
                    Its possible to break it by asking for weird amount of hours that arent compatible with JForex.

                    If the higher period is divisible with the amount given it should work.
                    So for minutes the higher period is hourly, which is 60 Mins.
                    So 1, 2, 3, 4, 5, 6, 10, 12, 15, 30, even 40 for some reason are ok.

                    Same thing for Seconds works for all normal periods not 11 or 13 for example.
                     */
                    Integer lookBack = timeBarReq.getValue();
                    Integer timeperiodReq = timeBarReq.getKey();

                    if (timeperiodReq.equals(0)) continue; // skip if 0

                    String unitReq = clientRequest.timeBarUnit;

                    if (unitReq.equals("SEC")) { // special case for seconds
                        returnMap.put(
                                new TimePeriodAggregationFeedDescriptor(instrument, Period.createCustomPeriod(
                                        Unit.Second, timeperiodReq), offerSide, filter),
                                lookBack
                        );
                    } else { // re calculate unit to minutes

                        Integer timeperiodMins = 0; // just initalization

                        if (unitReq.equals("MIN") || unitReq.isEmpty()) timeperiodMins = timeperiodReq; //default to MIN
                        else if (unitReq.equals("HOUR")) timeperiodMins = timeperiodReq * 60;
                        else if (unitReq.equals("DAY")) timeperiodMins = timeperiodReq * 1440;
                        else if (unitReq.equals("WEEK")) timeperiodMins = timeperiodReq * 7200;
                        else if (unitReq.equals("MONTH")) timeperiodMins = timeperiodReq * 28800;

                        if (timeperiodMins < 60) { // minutes
                            returnMap.put(
                                    new TimePeriodAggregationFeedDescriptor(instrument, Period.createCustomPeriod(
                                            Unit.Minute, timeperiodMins), offerSide, filter),
                                    lookBack
                            );
                        } else if (timeperiodMins < 1440) { // hours
                            Integer timeperiodHours = timeperiodMins / 60;
                            returnMap.put(
                                    new TimePeriodAggregationFeedDescriptor(instrument, Period.createCustomPeriod(
                                            Unit.Hour, timeperiodHours), offerSide, filter),
                                    lookBack
                            );
                        } else if (timeperiodMins < 7200) { // days
                            Integer timeperiodDays = timeperiodMins / 1440;
                            returnMap.put(
                                    new TimePeriodAggregationFeedDescriptor(instrument, Period.createCustomPeriod(
                                            Unit.Day, timeperiodDays), offerSide, filter),
                                    lookBack
                            );
                        } else if (timeperiodMins <= 28800) { // weeks
                            Integer timeperiodWeeks = timeperiodMins / 7200;
                            returnMap.put(
                                    new TimePeriodAggregationFeedDescriptor(instrument, Period.createCustomPeriod(
                                            Unit.Week, timeperiodWeeks), offerSide, filter),
                                    lookBack
                            );
                        } else { // months
                            Integer timeperiodMonths = timeperiodMins / 28800;
                            returnMap.put(
                                    new TimePeriodAggregationFeedDescriptor(instrument, Period.createCustomPeriod(
                                            Unit.Month, timeperiodMonths), offerSide, filter),
                                    lookBack
                            );
                        }
                    }
                }
                ///// RANGE AGGREGATION
                for (Map.Entry<Integer, Integer> rangeBarReq : clientRequest.rangeBar.entrySet()) {

                    Integer lookBack = rangeBarReq.getValue();
                    Integer rangeReq = rangeBarReq.getKey();

                    if (rangeReq.equals(0)) continue; // skip if 0

                    returnMap.put(
                            new RangeBarFeedDescriptor(instrument, PriceRange.valueOf(rangeReq), offerSide),
                            lookBack
                    );
                }
                ///// RENKO AGGREGATION
                for (Map.Entry<Integer, Integer> renkoBarReq : clientRequest.renkoBar.entrySet()) {

                    Integer lookBack = renkoBarReq.getValue();
                    Integer renkoReq = renkoBarReq.getKey();

                    if (renkoReq.equals(0)) continue; // skip if 0

                    returnMap.put(
                            new RenkoFeedDescriptor(instrument, PriceRange.valueOf(renkoReq), offerSide),
                            lookBack
                    );
                }

                ///// TICK BARS
                for (Map.Entry<Integer, Integer> tickBarReq : clientRequest.tickBar.entrySet()) {

                    Integer lookBack = tickBarReq.getValue();
                    Integer ticksReq = tickBarReq.getKey();

                    if (ticksReq.equals(0)) continue; // skip if 0

                    returnMap.put(
                            new TickBarFeedDescriptor(instrument, TickBarSize.valueOf(ticksReq), offerSide),
                            lookBack
                    );
                }
            }



        }catch (IllegalArgumentException e){
            printer.printMeError(e.getMessage());
            printer.printMeError(e.getStackTrace());
            e.printStackTrace(console.getErr());
            printer.printMeError("Cannot create custom period, probably because its a weird period you have given");
        }
        catch (NullPointerException e) {
            printer.printMeError(e.getMessage());
            printer.printMeError(e.getStackTrace());
            e.printStackTrace(console.getErr());
            printer.printMeError("You didnt give all required attributes in the request");
        }
//            catch (Exception e) {
//            printer.printMeError(e.getMessage());
//            printer.printMeError(e.getStackTrace());
//        }

        return returnMap;
    }


    public ClientResponse getFeedHistory(Map<IFeedDescriptor, Integer> feedDescriptors, Instant time, OfferSide offerside) throws JFException {

//        Map<IFeedDescriptor, List<ITimedData>> feedHistory = new HashMap<>();

        ClientResponse clientResponse = new ClientResponse();

        for (Map.Entry<IFeedDescriptor, Integer> entry : feedDescriptors.entrySet()) {

            IFeedDescriptor feedDescriptor = entry.getKey();
            Integer numberOfFeedBarsBefore = entry.getValue();

//            printer.printMeError(feedDescriptor);

            ClientResponse.FeedResponse feedResponse = clientResponse.new FeedResponse();

            feedResponse.instrument = feedDescriptor.getInstrument().getName();
            feedResponse.type = feedDescriptor.getDataType().toString();
            feedResponse.unit = feedDescriptor.getPeriod().getUnit().getCompactDescription();
            feedResponse.period = feedDescriptor.getPeriod().getNumOfUnits();

            if(!feedDescriptor.getDataType().equals(DataType.TIME_PERIOD_AGGREGATION)) {
                feedResponse.range = feedDescriptor.getPriceRange().getPipCount();
            }

            long usedTime;
            boolean isCurrentTime = false;
            // If we have given multiple feedDescriptors the only time they have in common is the current one so we return that
            if(feedDescriptors.size() > 1 || time.equals(0)) {
                printer.printMe("Using current time");
                usedTime = history.getFeedData(feedDescriptor, 0).getTime();
                isCurrentTime = true; // we need to save this truth for i.e indicators as fractal
            }
            else    usedTime = time.toEpochMilli();

            printer.printMe("startOfCurrentBarTime "+usedTime+ "---- "+feedDescriptor);

            try {
                feedResponse.feedDatas = history.getFeedData(
                        feedDescriptor, numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.ema5 = indicators.ema(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside, 5)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.ema20 = indicators.ema(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside, 20)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.sma5 = indicators.sma(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside, 5)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.sma20 = indicators.sma(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside, 20)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.rsi14 = indicators.rsi(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside, 14)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.adx14 = indicators.adx(
                        feedDescriptor, offerside, 14)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.cci14 = indicators.cci(
                        feedDescriptor, offerside, 14)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.atr14 = indicators.atr(
                        feedDescriptor, offerside, 14)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.bbands20_2 = indicators.bbands(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside,
                        20, 2, 2, IIndicators.MaType.SMA)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.bbands20_1618 = indicators.bbands(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside,
                        20, 1.618, 1.618, IIndicators.MaType.SMA)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.bbands20_2618 = indicators.bbands(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside,
                        20, 2.618, 2.618, IIndicators.MaType.SMA)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.bbands20_4236 = indicators.bbands(
                        feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside,
                        20, 4.236, 4.236, IIndicators.MaType.SMA)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.stoch5_3_3 = indicators.stoch(feedDescriptor, offerside,
                        5, 3, IIndicators.MaType.SMA, 3, IIndicators.MaType.SMA)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                feedResponse.macd12_26_9 = indicators.macd(feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerside,
                        12, 26, 9)
                        .calculate(numberOfFeedBarsBefore, usedTime, 0);

                try{
                    if(isCurrentTime){
                        long fractalTime = history.getFeedData(feedDescriptor, 5).getTime();

                        double[][] tempFractal5 = indicators.fractal(feedDescriptor, offerside, 5)
                                .calculate(numberOfFeedBarsBefore-5, fractalTime, 0);

//                        printer.printMe("tempFractal5 "+tempFractal5.length + " "+tempFractal5[0].length);

                        double[][] fractal5 = new double[2][numberOfFeedBarsBefore];

                        if(tempFractal5[0].length == numberOfFeedBarsBefore-5) { // just a sanity check, fails if indicator cannot get enough data
                            for (int up_down = 0; up_down < 2; up_down++) {
                                for (int bar = 0; bar < numberOfFeedBarsBefore - 5; bar++) {

                                    fractal5[up_down][bar] = tempFractal5[up_down][bar];
    //                                string = String.format(
    //                                        "fractal5[%s][%s] = %s, ", up_down, bar, fractal5[up_down][bar]);
    //                                printer.printMe(string);
                                }
                            }
                        }
                        feedResponse.fractal5 = fractal5;
                    }
                    else {
                        feedResponse.fractal5 = indicators.fractal(feedDescriptor, offerside, 5)
                                .calculate(numberOfFeedBarsBefore, usedTime, 0);
                    }

                } catch (JFException e) {
                    String timeOfStart = Instant.ofEpochMilli(usedTime).toString();

                    printer.printMeError("Fractal Failed with feed: "+feedDescriptor + " ,time: "+timeOfStart);
                    e.printStackTrace(console.getErr());
                }
            }
            catch (JFException e){
                String timeOfStart = Instant.ofEpochMilli(usedTime).toString();
//                String error = "Failed with: "+feedDescriptor + " ,time: "+timeOfStart + timeOfStart;
                printer.printMeError("Failed with: "+feedDescriptor + " ,time: "+timeOfStart);
                e.printStackTrace(console.getErr());
//                throw(e);
            }

    //            feedResponse.supRes = indicators.supportResistance(feedDescriptor, offerside)
    //                    .calculate(numberOfFeedBarsBefore, usedTime, 0);

            clientResponse.timestamp = usedTime;
            clientResponse.time = Instant.ofEpochMilli(usedTime).toString();

            clientResponse.addFeedResponse(feedResponse);
        }
        return clientResponse;
    }

    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData) {}

    public void stop() {
        exit = true;
    }
}