package singlejartest;

import com.dukascopy.api.*;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.ITextChartObject;
import com.dukascopy.api.drawings.ITriangleChartObject;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.dukascopy.api.feed.util.RangeBarFeedDescriptor;
import com.dukascopy.api.feed.util.RenkoFeedDescriptor;
import com.dukascopy.api.feed.util.TickBarFeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import com.dukascopy.api.indicators.OutputParameterInfo.DrawingStyle;
//import com.dukascopy.dds2.greed.remotelog.AlsTranslationKeys;
class TradeData {

    public static IFeedDescriptor feedDescriptor;

    public IBar bar;

    public Timestamp barTime;
    public Timestamp actualTime;

//    public Set<IOrder> longOrders = new HashSet<IOrder>();
//    public Set<IOrder> shortOrders = new HashSet<IOrder>();

    public IOrder longOrder;
    public IOrder shortOrder;

    public Outcome outcome = Outcome.NEITHER; // NEITHER as default

    TradeData(Timestamp barTime, Timestamp actualTime, IBar bar, IOrder longOrder, IOrder shortOrder) {
        this.bar = bar;
        this.barTime = barTime;
        this.actualTime = actualTime;
        this.longOrder = longOrder;
        this.shortOrder = shortOrder;
    }

    public enum Outcome {
        LONG,
        SHORT,
        //        BOTH,
        NEITHER
    }



    public String toCSVString() {

//        String label = longOrder.getLabel();

//        String tradeNo = label.replace("DataGenLong", "");

//        if(outcome == null) return;

        String str;

//        str =   tradeNo + "," +
//                time + "," +
//                bar.getOpen() + "," +
//                bar.getHigh() + "," +
//                bar.getLow() + "," +
//                bar.getClose() + "," +
//                bar.getVolume() + "," +
//                outcome + "\r\n";

        str =   barTime.toString() + "," +
                actualTime.toString() + "," +
                bar.getOpen() + "," +
                bar.getHigh() + "," +
                bar.getLow() + "," +
                bar.getClose() + "," +
                bar.getVolume() + "," +
                outcome + "\r\n";

        return str;
    }
}

class TradeGroup {
    /*
    One Group per SL and TP distance
    Each group contains the TradeData for all trades in that group


     */

    TradeGroup(int groupNo, int stopLossPips, int takeProfitPips, double maxSpreadPips) {
        this.groupNo = groupNo;
        this.stopLossPips = stopLossPips;
        this.takeProfitPips = takeProfitPips;
        this.maxSpreadPips = maxSpreadPips;
    }

    public int groupNo;

    public int stopLossPips = 10;
    public int takeProfitPips = 10;
    public double maxSpreadPips = 1.0;

    private int tradeNo = 0;

    public Map<Integer, TradeData> tradeDatas = new HashMap<Integer, TradeData>();


    public TradeData updateTrade(Integer tradeNo, TradeData tradeData){

        return tradeDatas.replace(tradeNo, tradeData);
    }

    public TradeData addTrade(Timestamp barTime, Timestamp actualTime, IBar bar, IOrder longOrder, IOrder shortOrder) {

        TradeData tradeData = new TradeData(barTime, actualTime, bar, longOrder, shortOrder);

        TradeData res =  tradeDatas.put(tradeNo, tradeData);

        incrementTradeNo();

        return res;
    }

    public String getTradeLabelToUse(IEngine.OrderCommand orderType){
    /*
        Unique label for each trade

        not allowed to start with anything but text

        ORDERTYPE _ GROUPNUMBER _ TRADENO _  SL _ TP _ MAXSPREAD
    */

        return String.format("%s_%s_%s_%s_%s_%s",
                orderType,
                groupNo,
                tradeNo,
                stopLossPips,
                takeProfitPips,
                (int) maxSpreadPips * 10
                );

    }

    static public Map<String, Object> parseTradeLabel(String label){

        /*
        ORDERTYPE _ GROUPNUMBER _ TRADENO _  SL _ TP _ MAXSPREAD
         */

        Map<String, Object> parsedLabel = new HashMap<String, Object>();

        try {

            String[] tempArray = label.split("_");

            parsedLabel.put("tradeType", IEngine.OrderCommand.valueOf(tempArray[0]));
            parsedLabel.put("groupNo", Integer.parseInt(tempArray[1]));
            parsedLabel.put("tradeNo", Integer.parseInt(tempArray[2]));
            parsedLabel.put("SL", Integer.parseInt(tempArray[3]));
            parsedLabel.put("TP", Integer.parseInt(tempArray[4]));
            parsedLabel.put("maxSpread", Double.parseDouble(tempArray[5]) / 10);

            return parsedLabel;
        } catch (Exception e){
            return parsedLabel;
        }
    }


    public int incrementTradeNo(){
        return ++tradeNo;
    }
}


//1. Add imports:

//2. implement IFeedListener interface
public class DataGen01 implements IStrategy, IFeedListener {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IBar previousBar;
    private IOrder order;
    private IChart openedChart;
    private IChartObjectFactory factory;
    private int signals;
    private static final int PREV = 1;
    private static final int SECOND_TO_LAST = 0;
//    private int uniqueOrderCounter = 0;
    private SMATrend previousSMADirection = SMATrend.NOT_SET;
    private SMATrend currentSMADirection = SMATrend.NOT_SET;
    private Map<IOrder, Boolean> createdOrderMap = new HashMap<IOrder, Boolean>();
    private int shorLineCounter;
    private int textCounterOldSL;
    private int textCounterNewSL;

    private Writer csvWriter;
    private DateFormat dateFormat;
    private DecimalFormat priceFormat;

    private File file;

    private Map<Integer, TradeGroup> tradeGroups = new HashMap<Integer, TradeGroup>();

//    private Map<Integer, TradeData> tradeDatas = new HashMap<Integer, TradeData>();


    long startDateTime;


    @Configurable("Filter")
    public Filter filter = Filter.ALL_FLATS;
    @Configurable("Base Stop loss in pips")
    public int baseStopLossPips = 5;
    @Configurable("Step Stop loss in pips")
    public int stepStopLossPips = 5;
    @Configurable("End Stop loss in pips")
    public int endStopLossPips = 10;
    @Configurable("Base Take profit in pips")
    public int baseTakeProfitPips = 5;
    @Configurable("Step Take profit in pips")
    public int stepTakeProfitPips = 5;
    @Configurable("End Take profit in pips")
    public int endTakeProfitPips = 10;
    @Configurable("Max Spread in pips")
    public double maxSpreadPips = 0.5;

    @Configurable("Print To CSV")
    public boolean printToCSV = true;

//    @Configurable("File")
//    public File file;
    /*
     * 3. remove following parameters: 
     */
//@Configurable(value = "Instrument value")
//public Instrument myInstrument = Instrument.EURGBP;
//@Configurable(value = "Offer Side value", obligatory = true)
//public OfferSide myOfferSide;
//@Configurable(value = "Period value")
//public Period myPeriod = Period.TEN_MINS;
    //4. Add new parameter. At strategy's startup one will be able to choose between feed types.

    @Configurable("Instrument")
    public static Instrument instrument = Instrument.EURUSD;


    @Configurable("Feed type")
    public FeedType myFeed = FeedType.TIME_1_H_BID;

    private enum SMATrend {

        UP, DOWN, NOT_SET;
    }

    /*5. Declare a new enum. 
     */
    public enum FeedType {

        TICKS_5_BID(new TickBarFeedDescriptor(instrument, TickBarSize.FIVE, OfferSide.BID)),
        RENKO_2_PIPS_BID(new RenkoFeedDescriptor(instrument, PriceRange.TWO_PIPS, OfferSide.BID)),
        TIME_1_SEC_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.ONE_SEC, OfferSide.BID, Filter.WEEKENDS)),
        TIME_30_SEC_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.THIRTY_SECS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_1_MIN_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.ONE_MIN, OfferSide.BID, Filter.WEEKENDS)),
        TIME_5_MIN_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.FIVE_MINS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_15_MIN_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.FIFTEEN_MINS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_1_H_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.ONE_HOUR, OfferSide.BID, Filter.WEEKENDS)),
        TIME_4_H_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.FOUR_HOURS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_1_D_BID(new TimePeriodAggregationFeedDescriptor(instrument, Period.DAILY, OfferSide.BID, Filter.WEEKENDS)),


        RANGE_10_PIPS_BID(new RangeBarFeedDescriptor(instrument, PriceRange.valueOf(10), OfferSide.BID)),
        RANGE_5_PIPS_BID(new RangeBarFeedDescriptor(instrument, PriceRange.valueOf(5), OfferSide.BID));

        private final IFeedDescriptor feedDescriptor;

        FeedType(IFeedDescriptor feedDescriptor) {
            this.feedDescriptor = feedDescriptor;
        }

        public IFeedDescriptor getFeedDescriptor() {
            return feedDescriptor;
        }
    }

    /*
     * 6. in onStart method we are subscribing to the feed. 
     */
    public void onStart(IContext context) throws JFException {

        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();

        printMe("START");

        myFeed.getFeedDescriptor().setInstrument(instrument);
        myFeed.getFeedDescriptor().setFilter(filter);

        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(myFeed.getFeedDescriptor().getInstrument());
        context.setSubscribedInstruments(instruments, true);

        this.openedChart = context.getChart(myFeed.getFeedDescriptor().getInstrument());
        if(this.openedChart != null) this.factory = openedChart.getChartObjectFactory();

        //Subscribe to a feed:
        context.subscribeToFeed(myFeed.getFeedDescriptor(), this);

        // save the start time for the file name
        Instrument myInstrument = myFeed.feedDescriptor.getInstrument();
        startDateTime = history.getTimeOfLastTick(myInstrument);

        TradeData.feedDescriptor = myFeed.getFeedDescriptor();

        int groupNo = 0;

        for(int stopLossPips = baseStopLossPips; stopLossPips <= endStopLossPips; stopLossPips += stepStopLossPips){
            for(int takeProfitPips = baseTakeProfitPips; takeProfitPips <= endTakeProfitPips; takeProfitPips += stepTakeProfitPips){

                tradeGroups.put(groupNo, new TradeGroup(groupNo, stopLossPips, takeProfitPips, maxSpreadPips));

                groupNo++;
            }
        }


    }//end of onStart method

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
//        if (message.getOrder() != null) {
//            printMe("order: " + message.getOrder().getLabel() + " || message content: " + message);
//        }


        if(message.getReasons().contains(IMessage.Reason.ORDER_CLOSED_BY_TP) ||
                message.getReasons().contains(IMessage.Reason.ORDER_CLOSED_BY_SL)) {

            printMe("Reasons:" + message.getReasons());

            printMe("SET TP: " + message.getOrder().getTakeProfitPrice() +
                    ", SL: "+ message.getOrder().getStopLossPrice());

            printMe("Actual stop price: "+ message.getOrder().getClosePrice());

            printMe("P/L: "+ message.getOrder().getProfitLossInPips());
        }



        if(message.getReasons().contains(IMessage.Reason.ORDER_CLOSED_BY_TP)) {
            IOrder order = message.getOrder();
            String label = order.getLabel();

            Map<String, Object> parsedLabel = TradeGroup.parseTradeLabel(label);

            TradeGroup tradeGroup = tradeGroups.get((int) parsedLabel.get("groupNo"));


            for (Map.Entry<String, Object> stringObjectEntry : parsedLabel.entrySet()) {

                printMe(stringObjectEntry.getKey() + " : " + stringObjectEntry.getValue() );
            }


            printMe("tradeGroup no: "+tradeGroup.groupNo);

            printMe("Label: "+label + ", TradeNo: "+ parsedLabel.get("tradeNo"));

            TradeData tradeData = tradeGroup.tradeDatas.get((int) parsedLabel.get("tradeNo"));



            printMe("TradeData longOrder Label: "+tradeData.longOrder.getLabel());

            if (order.isLong()) {

                if (tradeData.outcome == TradeData.Outcome.NEITHER) {
                    tradeData.outcome = TradeData.Outcome.LONG;
                }
                //                if(tradeData.outcome == TradeData.Outcome.SHORT) {
                //                    tradeData.outcome = TradeData.Outcome.BOTH;
                //                }

            } else { // short
                if (tradeData.outcome == TradeData.Outcome.NEITHER) {
                    tradeData.outcome = TradeData.Outcome.SHORT;
                }
                //                if(tradeData.outcome == TradeData.Outcome.LONG) {
                //                    tradeData.outcome = TradeData.Outcome.BOTH;
                //                }
            }

            tradeGroup.updateTrade((int) parsedLabel.get("tradeNo"), tradeData);

            printMe("TradeNO" + (int) parsedLabel.get("tradeNo"));
        }

    }

    public void onStop() throws JFException {

        Instrument myInstrument = myFeed.feedDescriptor.getInstrument();
        long endDateTime = history.getTimeOfLastTick(myInstrument);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        String startTimeFormatted = sdf.format(startDateTime);
        String endTimeFormatted = sdf.format(endDateTime);

        for (Map.Entry<Integer, TradeGroup> tradeGroupEntry : tradeGroups.entrySet()) {

            TradeGroup tradeGroup = tradeGroupEntry.getValue();

            String fileName = String.format("%s - %s %s SL%s TP%s maxSpread%s.csv",
                    startTimeFormatted,
                    endTimeFormatted,
                    myFeed,
                    tradeGroup.stopLossPips,
                    tradeGroup.takeProfitPips,
                    tradeGroup.maxSpreadPips);

            String path = String.format("C:\\Programming\\jforex_data\\%s\\%s",
                    myInstrument.toString().replace("/", ""),
                    fileName);

            try{
                file = new File(path);
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (Exception e){
                console.getErr().println(e.getMessage());
                e.printStackTrace(console.getErr());
                context.stop();
            }

            try {
                csvWriter = new BufferedWriter(new FileWriter(file));
            } catch (Exception e) {
                console.getErr().println(e.getMessage());
                e.printStackTrace(console.getErr());
                context.stop();
            }

            try {
                for (Map.Entry<Integer, TradeData> entry : tradeGroup.tradeDatas.entrySet()) {

                    csvWriter.write(entry.getValue().toCSVString());

                }
            } catch (Exception e) {
                console.getErr().println(e.getMessage());
                e.printStackTrace(console.getErr());
                context.stop();
            }

            if (csvWriter != null) {
                try {
                    csvWriter.close();
                } catch (Exception e) {
                    console.getErr().println(e.getMessage());
                    e.printStackTrace(console.getErr());
                    context.stop();
                }
            }
        }
    }

    
    public void onTick(Instrument instrument, ITick tick) throws JFException {
//        if (instrument != myFeed.getFeedDescriptor().getInstrument()) {
//            return;
//        }

    }//end of onTick method

    
    @Override
    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData) {

        Instrument myInstrument = feedDescriptor.getInstrument();
        OfferSide myOfferSide = feedDescriptor.getOfferSide();

//        TradeData tradeData = new TradeData();

        printMe("New Feed Bar");


        try {
            if (!(feedData instanceof IBar)) {
                printMeError("Cannot work with tick feed data");
                return;
            }

            IBar bar = (IBar) feedData;

            Timestamp barTime = new Timestamp(feedData.getTime()); // this is the start time of the bar not the current time
            Timestamp actualTime = new Timestamp(history.getTimeOfLastTick(myInstrument));// this is the time the bar closed, so like actualTime

//            printMe("getLastTick time: "+new Timestamp(history.getLastTick(myInstrument).getTime()));
//            printMe("feedData.getTime(): "+ barTime);
//            printMe("getTick: "+ new Timestamp(history.getTick(myInstrument,0).getTime()));
//            printMe("bar.getTime(): "+ new Timestamp(bar.getTime()));
//            int candlesBefore = 2, candlesAfter = 0;
//            long completedBarTimeL = bar.getTime();


            for (Map.Entry<Integer, TradeGroup> tradeGroupEntry : tradeGroups.entrySet()) {

                TradeGroup tradeGroup = tradeGroupEntry.getValue();

                double lastTickBid = history.getLastTick(myInstrument).getBid();
                double lastTickAsk = history.getLastTick(myInstrument).getAsk();
                double stopLossValue = myInstrument.getPipValue() * tradeGroup.stopLossPips;
                double takeprofitValue = myInstrument.getPipValue() * tradeGroup.takeProfitPips;

                double stopLossPriceLong = lastTickBid - stopLossValue;
                double stopLossPriceShort = lastTickAsk + stopLossValue;

                double takeProfitPriceLong = lastTickBid + takeprofitValue;
                double takeProfitPriceShort = lastTickAsk - takeprofitValue;

                double maxSpreadValue = myInstrument.getPipValue() * tradeGroup.maxSpreadPips;
                double spreadValue = lastTickAsk - lastTickBid;


                printMe("Bid at last tick: "+lastTickBid);


//                printMe(tradeGroup.getTradeLabelToUse(IEngine.OrderCommand.BUY));

                if(spreadValue <= maxSpreadValue) {
                    IOrder longOrder = engine.submitOrder(tradeGroup.getTradeLabelToUse(IEngine.OrderCommand.BUY), myInstrument,
                            IEngine.OrderCommand.BUY, 0.001, 0, 100, stopLossPriceLong, takeProfitPriceLong);

                    IOrder shortOrder = engine.submitOrder(tradeGroup.getTradeLabelToUse(IEngine.OrderCommand.SELL), myInstrument,
                            IEngine.OrderCommand.SELL, 0.001, 0, 100, stopLossPriceShort, takeProfitPriceShort);

//                    printMe(longOrder);

                    tradeGroup.addTrade(
                            barTime,
                            actualTime,
                            bar,
                            longOrder,
                            shortOrder
                    );
                }
            }
        } catch (Exception e) {
        }
    }

    //9. We moved all logic from the onBar to onFeedData method.
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
    }

    private void printMe(Object toPrint) {
        console.getOut().println(toPrint);
    }

    private void printMeError(Object o) {
        console.getErr().println(o);
    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
    
    private void addBreakToChart(IOrder changedOrder, ITick tick, double oldSL, double newSL) throws JFException {
        if (openedChart == null) {
            return;
        }

        ITriangleChartObject orderSLTriangle = factory.createTriangle("Triangle " + shorLineCounter++,
                changedOrder.getFillTime(), changedOrder.getOpenPrice(), tick.getTime(), oldSL, tick.getTime(), newSL);

        Color lineColor = oldSL > newSL ? Color.RED : Color.GREEN;
        orderSLTriangle.setColor(lineColor);
        orderSLTriangle.setLineStyle(LineStyle.SOLID);
        orderSLTriangle.setLineWidth(1);
        orderSLTriangle.setStickToCandleTimeEnabled(false);
        openedChart.addToMainChart(orderSLTriangle);

        //drawing text
        String breakTextOldSL = String.format(" Old SL: %.5f", oldSL);
        String breakTextNewSL = String.format(" New SL: %.5f", newSL);
        double pipValue = myFeed.getFeedDescriptor().getInstrument().getPipValue();
        double textVerticalPosition = oldSL > newSL ? newSL - pipValue : newSL + pipValue;
        ITextChartObject textOldSL = factory.createText("textKey1" + textCounterOldSL++, tick.getTime(), oldSL);
        ITextChartObject textNewSL = factory.createText("textKey2" + textCounterNewSL++, tick.getTime(), newSL);
        textOldSL.setText(breakTextOldSL);
        textNewSL.setText(breakTextNewSL);
        textOldSL.setStickToCandleTimeEnabled(false);
        textNewSL.setStickToCandleTimeEnabled(false);
        openedChart.addToMainChart(textOldSL);
        openedChart.addToMainChart(textNewSL);
    }
    

    private boolean checkChart(IChart chart) {
        if (chart == null) {
            printMeError("chart for " + myFeed.getFeedDescriptor().getInstrument() + " not opened!");
            return false;
        }
        if (chart.getSelectedOfferSide() != myFeed.getFeedDescriptor().getOfferSide()) {
            printMeError("chart offer side is not " + myFeed.getFeedDescriptor().getOfferSide());
            return false;
        }

        if (chart.getFeedDescriptor().getDataType() == DataType.RENKO) {
            if (chart.getPriceRange() != myFeed.getFeedDescriptor().getPriceRange()) {
                printMeError("chart price range is not " + myFeed.getFeedDescriptor().getPriceRange());
                return false;
            }
        } else if (chart.getFeedDescriptor().getDataType() == DataType.TIME_PERIOD_AGGREGATION) {
            if (chart.getSelectedPeriod() != myFeed.getFeedDescriptor().getPeriod()) {
                printMeError("chart period is not " + myFeed.getFeedDescriptor().getPeriod());
                return false;
            }
        }

        if (chart.getFilter() != this.filter) {
            printMeError("chart filter is not " + this.filter);
            return false;
        }
        return true;
    }
}
