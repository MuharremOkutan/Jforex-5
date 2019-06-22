package DataGen2;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.*;
import com.dukascopy.api.feed.util.KagiFeedDescriptor;
import com.dukascopy.api.feed.util.PointAndFigureFeedDescriptor;
import com.dukascopy.api.feed.util.LineBreakFeedDescriptor;
import com.dukascopy.api.feed.util.RangeBarFeedDescriptor;
import com.dukascopy.api.feed.util.RenkoFeedDescriptor;
import com.dukascopy.api.feed.util.TickBarFeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import com.dukascopy.api.indicators.IIndicator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class DataGen2  implements IStrategy, IFeedListener {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;

    private Writer csvWriter;

    JForexPrinter printer;

    Instant startDateTime;

    Set<RowData> rowDatas = new HashSet<RowData>();

    @Configurable("Instrument")
    public static Instrument mainInstrument = Instrument.EURUSD;
    @Configurable("Feed type")
    public FeedType mainFeed = FeedType.TIME_1_H_BID;

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
    @Configurable("Min profit in pips")
    public double minProfitPips = 1;
    @Configurable("Allow BOTH")
    public boolean isAllowBoth = false;
    @Configurable("Make Outcome NEITHER if above max pips")
    public boolean isNeitherIfAboveMaxPips = false;

    @Configurable("Print To CSV")
    public boolean printToCSV = true;

    public enum FeedType {

        // TODO Replace this with a way to just choose the instrument, feed type and amount

        /// TIME BARS
        TIME_1_SEC_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.ONE_SEC, OfferSide.BID, Filter.WEEKENDS)),
        TIME_15_SEC_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.createCustomPeriod(Unit.Second, 15), OfferSide.BID, Filter.WEEKENDS)),
        TIME_30_SEC_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.THIRTY_SECS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_1_MIN_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.ONE_MIN, OfferSide.BID, Filter.WEEKENDS)),
        TIME_5_MIN_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.FIVE_MINS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_15_MIN_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.FIFTEEN_MINS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_1_H_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.ONE_HOUR, OfferSide.BID, Filter.WEEKENDS)),
        TIME_4_H_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.FOUR_HOURS, OfferSide.BID, Filter.WEEKENDS)),
        TIME_1_D_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.DAILY, OfferSide.BID, Filter.WEEKENDS)),
        TIME_1_W_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.WEEKLY, OfferSide.BID, Filter.WEEKENDS)),

        //// TICK BARS
        TICKS_5_BID(new TickBarFeedDescriptor(mainInstrument, TickBarSize.FIVE, OfferSide.BID)),
        TICKS_10_BID(new TickBarFeedDescriptor(mainInstrument, TickBarSize.valueOf(10), OfferSide.BID)),
        TICKS_20_BID(new TickBarFeedDescriptor(mainInstrument, TickBarSize.valueOf(20), OfferSide.BID)),
        TICKS_50_BID(new TickBarFeedDescriptor(mainInstrument, TickBarSize.valueOf(50), OfferSide.BID)),
        TICKS_100_BID(new TickBarFeedDescriptor(mainInstrument, TickBarSize.valueOf(100), OfferSide.BID)),
        TICKS_200_BID(new TickBarFeedDescriptor(mainInstrument, TickBarSize.valueOf(200), OfferSide.BID)),

        /// RANGE BARS
        RANGE_1_PIP_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(1), OfferSide.BID)),
        RANGE_5_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(5), OfferSide.BID)),
        RANGE_10_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(10), OfferSide.BID)),
        RANGE_20_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(20), OfferSide.BID)),
        RANGE_50_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(50), OfferSide.BID)),
        RANGE_100_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(100), OfferSide.BID)),
        RANGE_200_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(200), OfferSide.BID)),

        //// RENKO BARS
        RENKO_1_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.ONE_PIP, OfferSide.BID)),
        RENKO_5_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.valueOf(5), OfferSide.BID)),
        RENKO_10_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.valueOf(10), OfferSide.BID)),
        RENKO_20_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.valueOf(20), OfferSide.BID)),
        RENKO_50_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.valueOf(50), OfferSide.BID)),
        RENKO_100_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.valueOf(100), OfferSide.BID)),
        RENKO_200_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.valueOf(200), OfferSide.BID)),

        //// KAGI BARS
        KAGI_1_PIP_BID(new KagiFeedDescriptor(mainInstrument, OfferSide.BID, PriceRange.valueOf(1))),
        KAGI_5_PIP_BID(new KagiFeedDescriptor(mainInstrument, OfferSide.BID, PriceRange.valueOf(5))),
        KAGI_10_PIP_BID(new KagiFeedDescriptor(mainInstrument, OfferSide.BID, PriceRange.valueOf(10))),
        KAGI_20_PIP_BID(new KagiFeedDescriptor(mainInstrument, OfferSide.BID, PriceRange.valueOf(20))),
        KAGI_50_PIP_BID(new KagiFeedDescriptor(mainInstrument, OfferSide.BID, PriceRange.valueOf(50))),

        // TODO read up on P&F more for better settings
        //// POINT & FIGURE BARS
        PF_1_PIP_X_3_BID(new PointAndFigureFeedDescriptor(mainInstrument, PriceRange.valueOf(1), ReversalAmount.THREE, OfferSide.BID)),
        PF_3_PIP_X_3_BID(new PointAndFigureFeedDescriptor(mainInstrument, PriceRange.valueOf(3), ReversalAmount.THREE, OfferSide.BID)),

        //// LINEBREAK BARS
        LINEBREAK_15_SEC_BID(new LineBreakFeedDescriptor(mainInstrument, OfferSide.BID, Period.createCustomPeriod(Unit.Second, 5))),
        LINEBREAK_1_MIN_BID(new LineBreakFeedDescriptor(mainInstrument, OfferSide.BID, Period.createCustomPeriod(Unit.Minute, 1))),
        LINEBREAK_5_MIN_BID(new LineBreakFeedDescriptor(mainInstrument, OfferSide.BID, Period.createCustomPeriod(Unit.Minute, 5))),
        LINEBREAK_15_MIN_BID(new LineBreakFeedDescriptor(mainInstrument, OfferSide.BID, Period.createCustomPeriod(Unit.Minute, 15))),
        LINEBREAK_1_H_BID(new LineBreakFeedDescriptor(mainInstrument, OfferSide.BID, Period.createCustomPeriod(Unit.Hour, 1))),
        LINEBREAK_4_H_BID(new LineBreakFeedDescriptor(mainInstrument, OfferSide.BID, Period.createCustomPeriod(Unit.Hour, 4))),
        LINEBREAK_1_D_BID(new LineBreakFeedDescriptor(mainInstrument, OfferSide.BID, Period.createCustomPeriod(Unit.Day, 1)));

        private final IFeedDescriptor feedDescriptor;

        FeedType(IFeedDescriptor feedDescriptor) {
            this.feedDescriptor = feedDescriptor;
        }

        public IFeedDescriptor getFeedDescriptor() {
            return feedDescriptor;
        }
    }

    public void onStart(IContext context) throws JFException {

        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();

        printer = new JForexPrinter(console);

        mainFeed.getFeedDescriptor().setInstrument(mainInstrument);
        mainFeed.getFeedDescriptor().setFilter(filter);

        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(mainFeed.getFeedDescriptor().getInstrument());
        context.setSubscribedInstruments(instruments, true);

        //Subscribe to a feed:
        context.subscribeToFeed(mainFeed.getFeedDescriptor(), this);

        // save the start time for the file name
        Instrument mainInstrument = mainFeed.feedDescriptor.getInstrument();
        startDateTime = Instant.ofEpochMilli(history.getTimeOfLastTick(mainInstrument));
    }

    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData) {

        if (!(feedData instanceof IBar)) {
            printer.printMeError("Cannot work with tick feed data");
            return;
        }

        try {

            Instrument instrument = feedDescriptor.getInstrument();

            Instant barOpenedTime = Instant.ofEpochMilli(feedData.getTime()); // this is the start time of the bar not the current time
            Instant actualTime = Instant.ofEpochMilli(history.getTimeOfLastTick(instrument));// this is the time the bar closed, so like actualTime

            RowData rowData = new RowData(barOpenedTime, actualTime);

            // TODO here would also add all other feeds to the rowData

            rowData.feedBars.put(feedDescriptor, feedData);

            double lastTickBid = history.getLastTick(instrument).getBid();
            double lastTickAsk = history.getLastTick(instrument).getAsk();

            int orderNo = 0;

            // OPEN ALL THE ORDER PAIRS BY LOOPING THROUGH THE SL AND TP
            for(int stopLossPips = baseStopLossPips; stopLossPips <= endStopLossPips; stopLossPips += stepStopLossPips) {
                for (int takeProfitPips = baseTakeProfitPips; takeProfitPips <= endTakeProfitPips; takeProfitPips += stepTakeProfitPips) {

                    // if we dont allow the outcome to be BOTH we cannot trade with a SL > TP
                    if(!isAllowBoth && stopLossPips > takeProfitPips) continue;

                    double stopLossValue = instrument.getPipValue() * stopLossPips;
                    double takeprofitValue = instrument.getPipValue() * takeProfitPips;

                    double stopLossPriceLong = lastTickBid - stopLossValue;
                    double stopLossPriceShort = lastTickAsk + stopLossValue;

                    double takeProfitPriceLong = lastTickBid + takeprofitValue;
                    double takeProfitPriceShort = lastTickAsk - takeprofitValue;

                    double maxSpreadValue = instrument.getPipValue() * maxSpreadPips;
                    double spreadValue = lastTickAsk - lastTickBid;

                    if(spreadValue <= maxSpreadValue) {

                        String label = String.format("TP%s_SL%s_%s_%d",
                                takeProfitPips, stopLossPips, actualTime.getEpochSecond(), orderNo++);

                        printer.printMe("LABEL: "+("long_".concat(label)));

                        IOrder longOrder = engine.submitOrder("LONG_".concat(label), instrument,
                                IEngine.OrderCommand.BUY, 0.001, 0, 100, stopLossPriceLong, takeProfitPriceLong);

                        IOrder shortOrder = engine.submitOrder("SHORT_".concat(label), instrument,
                                IEngine.OrderCommand.SELL, 0.001, 0, 100, stopLossPriceShort, takeProfitPriceShort);

                        OrderPair orderPair = new OrderPair(longOrder, shortOrder, stopLossPips, takeProfitPips);
                        rowData.orderPairs.put(orderPair, false); // add order pair to rowData, false because no outcome set
                        rowDatas.add(rowData); // add rowData to rowDatas set

                    }
                    else if(isNeitherIfAboveMaxPips) { // we still need to create a OrderPair even without Orders
                        OrderPair orderPair = new OrderPair(stopLossPips, takeProfitPips);
                        rowData.orderPairs.put(orderPair, true); // add order pair to rowData, true because no need to check outcome since no real trades
                        rowDatas.add(rowData); // add rowData to rowDatas set
                    }


                }
            }
        } catch (JFException e) {
            printer.printMeError("Failed with: "+feedDescriptor.toString());
            printer.printMeError(e.getMessage());
        }
    }

    public void onAccount(IAccount account) throws JFException { }
    public void onMessage(IMessage message) throws JFException { }
    public void onTick(Instrument instrument, ITick tick) throws JFException { }
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException { }


    public void onStop() throws JFException {

        long endDateTime = history.getTimeOfLastTick(mainInstrument);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        String startTimeFormatted = sdf.format(startDateTime.toEpochMilli());
        String endTimeFormatted = sdf.format(endDateTime);

        String fileName = String.format("%s - %s %s maxSpread%s allowBoth=%s.csv",
                startTimeFormatted,
                endTimeFormatted,
                mainFeed,
                maxSpreadPips,
                isAllowBoth);

        String path = String.format("C:\\Programming\\jforex_data\\%s\\%s",
                mainInstrument.toString().replace("/", ""),
                fileName);


        // Create file and any dirs
        File file = new File(path);
        try{
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

        // write to file


        boolean firstRow = true;
        try {
            for(RowData rowData :rowDatas) {
                rowData.updateOrderPairOutcome(minProfitPips, isAllowBoth);

                if (firstRow) csvWriter.write(rowData.getCSVHeader());
                firstRow = false;

                csvWriter.write(rowData.getCSVLine());
//                printer.printMe("-- CSV LINE: " + rowData.getCSVHeader() + "     " + rowData.getCSVLine());
            }
        } catch (Exception e) {
            console.getErr().println(e.getMessage());
            e.printStackTrace(console.getErr());
            context.stop();
        }

        // close csvWriter
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

class RowData {
    // one RowData object per row on the CSV, holds all data for that row
    Instant mainFeedTimeOpened;
    Instant actualTime;


    // contains the bar data for each of feed at actualTime, this means most bars will be unfinished,
    Map<IFeedDescriptor, ITimedData> feedBars = new LinkedHashMap <IFeedDescriptor, ITimedData>(); // linked to preserve order

    // TODO add indicators

    Map<OrderPair, Boolean> orderPairs = new LinkedHashMap <OrderPair, Boolean>(); // map of orders for this row <OrderPair, isOutcomeSet> // linked to preserve order

    RowData(Instant mainFeedTimeOpened, Instant actualTime) {
        this.mainFeedTimeOpened = mainFeedTimeOpened;
        this.actualTime = actualTime;
    }

    public boolean getIsAllOutcomesSet(){
        return(!this.orderPairs.containsValue(false)); // if any value is false we do not yet know all outcomes
    }

    public void updateOrderPairOutcome(double minProfitPips, boolean isAllowBoth){

        if(getIsAllOutcomesSet()) { return; } // if we already know all outcomes then skip

        for (Map.Entry<OrderPair, Boolean> entry : this.orderPairs.entrySet()) {

            if(entry.getValue().equals(true)) { continue; } //  skip because its already been updated

//            entry.setValue(true); // set to true before checking, gets set back to false if no outcome yet

            OrderPair pair = entry.getKey();

            if (pair.longOrder.getState().equals(IOrder.State.CLOSED) &&
                    pair.longOrder.getProfitLossInPips() >= minProfitPips) {
                pair.outcome = OrderPair.Outcome.LONG; // order is closed and above minProfitPips, update outcome
                entry.setValue(true); // set to true because outcome is known
            }
            else if (pair.shortOrder.getState().equals(IOrder.State.CLOSED) &&
                    pair.shortOrder.getProfitLossInPips() >= minProfitPips) {
                pair.outcome = OrderPair.Outcome.SHORT; // order is closed and above minProfitPips, update outcome

                if(!isAllowBoth) entry.setValue(true); // set to true because outcome is known
            }
            else if (pair.longOrder.getState().equals(IOrder.State.CLOSED) &&
                    pair.shortOrder.getState().equals(IOrder.State.CLOSED)) {
                pair.outcome = OrderPair.Outcome.NEITHER; // neither trade went in to profit

                if(!isAllowBoth) entry.setValue(true); // set to true because outcome is known
            } else {
                entry.setValue(false); // outcome unknown still
            }

            if(isAllowBoth) {
                // we allow both long and short to be profitable, i.e the SL is greater than the TP

                 if (pair.outcome.equals(OrderPair.Outcome.SHORT)) { // short already gone to profit

                    if (pair.longOrder.getState().equals(IOrder.State.CLOSED) &&
                            pair.longOrder.getProfitLossInPips() >= minProfitPips) { /// long order in profit so both

                        pair.outcome = OrderPair.Outcome.BOTH; // order is closed and above minProfitPips, update outcome
                        entry.setValue(true); // set to true because outcome is known for both
                    }
                 }
                 else if (pair.outcome.equals(OrderPair.Outcome.LONG)) { // long already gone to profit

                    if (pair.shortOrder.getState().equals(IOrder.State.CLOSED) &&
                            pair.shortOrder.getProfitLossInPips() >= minProfitPips) { /// short order in profit so both

                        pair.outcome = OrderPair.Outcome.BOTH; // order is closed and above minProfitPips, update outcome
                        entry.setValue(true); // set to true because outcome is known for both
                    }
                }
            }
        }
    }

    public String getCSVHeader(){
        /*
            ### MAIN
            # Time MainFeedBar Opened
            # Time Bar Closed and Time of Trade
            ### FEED INFO (for each feed)
            # Time of FeedBar Opened
            # OHLC
            # Volume
            ### INDICATOR (for each indicator)
            # Indicator values
            ### OUTCOME (for each order pair)
            # Outcome
         */

        String header = "TimeOpened, Time, ";

        for (IFeedDescriptor feedDescriptor : feedBars.keySet()) {

            String feedName = "";

            if(feedDescriptor.getDataType().equals(DataType.TIME_PERIOD_AGGREGATION) ||
                    feedDescriptor.getDataType().equals(DataType.LINE_BREAK)) {
                feedName = String.format("%s %s %s",
                        feedDescriptor.getInstrument(), feedDescriptor.getDataType(), feedDescriptor.getPeriod());
            }


//            if(feedDescriptor.getDataType().equals(DataType.TIME_PERIOD_AGGREGATION) ||
//                    feedDescriptor.getDataType().equals(DataType.LINE_BREAK)) {
//                feedName = String.format("%s %s %s",
//                        feedDescriptor.getInstrument(), feedDescriptor.getDataType(), feedDescriptor.getPeriod());
//            }
//            else if(feedDescriptor.getDataType().equals(DataType.TICK_BAR)) {
//                feedName = String.format("%s %s %s",
//                        feedDescriptor.getInstrument(), feedDescriptor.getDataType(), feedDescriptor.getTickBarSize());
//            }
//            else if(feedDescriptor.getDataType().equals(DataType.PRICE_RANGE_AGGREGATION) ||
//                    feedDescriptor.getDataType().equals(DataType.RENKO) ||
//                    feedDescriptor.getDataType().equals(DataType.KAGI)) {
//
//                feedName = String.format("%s %s %s",
//                        feedDescriptor.getInstrument(), feedDescriptor.getDataType(), feedDescriptor.getPriceRange());
//            }
//            else if(feedDescriptor.getDataType().equals(DataType.POINT_AND_FIGURE)) {
//
//                feedName = String.format("%s %s %s %s",
//                        feedDescriptor.getInstrument(), feedDescriptor.getDataType(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount());
//            }

            String feedStr =
                    feedName + " TimeOpened" + "," +
                    feedName + " Open" + "," +
                    feedName + " High" + "," +
                    feedName + " Low" + "," +
                    feedName + " Close" + "," +
                    feedName + " Volume" + ",";


            // ADDITIONAL DATA DEPENDANT ON DATA TYPE
            if (feedDescriptor.getDataType().equals(DataType.PRICE_RANGE_AGGREGATION) ||
                    feedDescriptor.getDataType().equals(DataType.TICK_BAR)) {

                String additionalData =
                        feedName + " TimeEnd" + "," +
                        feedName + " TickCount" + ",";

                feedStr = feedStr.concat(additionalData);

            } else if (feedDescriptor.getDataType().equals(DataType.RENKO)) {

                String additionalData =
                        feedName + " TimeEnd" + "," +
                        feedName + " TickCount" + "," +
                        feedName + " WickPrice" + ",";

                feedStr = feedStr.concat(additionalData);

            } else if (feedDescriptor.getDataType().equals(DataType.POINT_AND_FIGURE)) {

                String additionalData =
                        feedName + " TimeEnd" + "," +
                        feedName + " TickCount" + "," +
                        feedName + " isRising" + ",";

                feedStr = feedStr.concat(additionalData);

            } else if (feedDescriptor.getDataType().equals(DataType.LINE_BREAK)) {

                String additionalData =
                        feedName + " TimeEnd" + "," +
                        feedName + " TickCount" + "," +
                        feedName + " WickPrice" + "," +
                        feedName + " isRising" + "," +
                        feedName + " TurnAroundPrice" + ",";

                feedStr = feedStr.concat(additionalData);

            } else if (feedDescriptor.getDataType().equals(DataType.KAGI)) {

                String additionalData =
                        feedName + " TimeEnd" + "," +
                        feedName + " TickCount" + "," +
                        feedName + " isRising" + "," +
                        feedName + " TurnAroundPrice" + "," +
                        feedName + " YinSpan1" + "," +
                        feedName + " YinSpan2" + "," +
                        feedName + " YinSpan1" + "," +
                        feedName + " YangSpan2" + ",";

                feedStr = feedStr.concat(additionalData);
            }

            header = header.concat(feedStr);
        }

        // TODO <--- add indicators here

        for (OrderPair orderPair : orderPairs.keySet()) {

            String outcomeStr =
                    "SL"+orderPair.stoploss + " TP"+orderPair.takeprofit + ",";

            header = header.concat(outcomeStr);
        }

        header = header.concat(("\r\n")); // end with new line


        return(header);
    }

    public String getCSVLine(){

        /*
            ### MAIN
            # Time MainFeedBar Opened
            # Time Bar Closed and Time of Trade
            ### FEED INFO (for each feed)
            # Time of FeedBar Opened
            # OHLC
            # Volume
            ### INDICATOR (for each indicator)
            # Indicator values
            ### OUTCOME (for each order pair)
            # Outcome
         */

        String csvLine =
                mainFeedTimeOpened + "," +
                actualTime + ",";


        for (Map.Entry<IFeedDescriptor, ITimedData> entry : feedBars.entrySet()) {

            IFeedDescriptor feedDescriptor = entry.getKey();
            ITimedData feedData = entry.getValue();

            // TODO could be cleaned out by checking if the datatype inherits the different interfaces and then just adding that data to the string

            if (feedDescriptor.getDataType().equals(DataType.TIME_PERIOD_AGGREGATION)) {
                IBar bar = (IBar) feedData;

                String feedStr =
                        Instant.ofEpochMilli(bar.getTime())+ "," +
                        bar.getOpen() + "," +
                        bar.getHigh() + "," +
                        bar.getLow() + "," +
                        bar.getClose() + "," +
                        bar.getVolume() + ",";

                csvLine = csvLine.concat(feedStr);

            } else if (feedDescriptor.getDataType().equals(DataType.PRICE_RANGE_AGGREGATION)) {
                IRangeBar bar = (IRangeBar) feedData;

                String feedStr =
                        Instant.ofEpochMilli(bar.getTime())+ "," +
                        bar.getOpen() + "," +
                        bar.getHigh() + "," +
                        bar.getLow() + "," +
                        bar.getClose() + "," +
                        bar.getVolume() + "," +
                        Instant.ofEpochMilli(bar.getEndTime()) + "," +
                        bar.getFormedElementsCount() + ",";

                csvLine = csvLine.concat(feedStr);

            } else if (feedDescriptor.getDataType().equals(DataType.TICK_BAR)) {
                ITickBar bar = (ITickBar) feedData;

                String feedStr =
                        Instant.ofEpochMilli(bar.getTime())+ "," +
                        bar.getOpen() + "," +
                        bar.getHigh() + "," +
                        bar.getLow() + "," +
                        bar.getClose() + "," +
                        bar.getVolume() + "," +
                        Instant.ofEpochMilli(bar.getEndTime()) + "," +
                        bar.getFormedElementsCount() + ",";

                csvLine = csvLine.concat(feedStr);

            } else if (feedDescriptor.getDataType().equals(DataType.RENKO)) {
                IRenkoBar bar = (IRenkoBar) feedData;

                String feedStr =
                        Instant.ofEpochMilli(bar.getTime())+ "," +
                        bar.getOpen() + "," +
                        bar.getHigh() + "," +
                        bar.getLow() + "," +
                        bar.getClose() + "," +
                        bar.getVolume() + "," +
                        Instant.ofEpochMilli(bar.getEndTime()) + "," +
                        bar.getFormedElementsCount() + "," +
                        bar.getWickPrice() + ",";

                csvLine = csvLine.concat(feedStr);

            } else if (feedDescriptor.getDataType().equals(DataType.POINT_AND_FIGURE)) {
                IPointAndFigure bar = (IPointAndFigure) feedData;

                String feedStr =
                        Instant.ofEpochMilli(bar.getTime())+ "," +
                        bar.getOpen() + "," +
                        bar.getHigh() + "," +
                        bar.getLow() + "," +
                        bar.getClose() + "," +
                        bar.getVolume() + "," +
                        Instant.ofEpochMilli(bar.getEndTime()) + "," +
                        bar.getFormedElementsCount() + "," +
                        bar.isRising() + ",";

                csvLine = csvLine.concat(feedStr);

            } else if (feedDescriptor.getDataType().equals(DataType.LINE_BREAK)) {
                ILineBreak bar = (ILineBreak) feedData;

                String feedStr =
                        Instant.ofEpochMilli(bar.getTime())+ "," +
                        bar.getOpen() + "," +
                        bar.getHigh() + "," +
                        bar.getLow() + "," +
                        bar.getClose() + "," +
                        bar.getVolume() + "," +
                        Instant.ofEpochMilli(bar.getEndTime()) + "," +
                        bar.getFormedElementsCount() + "," +
                        bar.getWickPrice() + "," +
                        bar.isRising() + "," +
                        bar.getTurnaroundPrice() + ",";

                csvLine = csvLine.concat(feedStr);

            } else if (feedDescriptor.getDataType().equals(DataType.KAGI)) {
                IKagi bar = (IKagi) feedData;

                String feedStr =
                        Instant.ofEpochMilli(bar.getTime()) + "," +
                        bar.getOpen() + "," +
                        bar.getHigh() + "," +
                        bar.getLow() + "," +
                        bar.getClose() + "," +
                        bar.getVolume() + "," +
                        Instant.ofEpochMilli(bar.getEndTime()) + "," +
                        bar.getFormedElementsCount() + "," +
                        bar.isRising() + "," +
                        bar.getTurnaroundPrice() + "," +
                        bar.getYinSpan()[0] + "," +
                        bar.getYinSpan()[1] + "," +
                        bar.getYangSpan()[0] + "," +
                        bar.getYangSpan()[1] + ",";

                csvLine = csvLine.concat(feedStr);
            }
        }

        // TODO <--- add indicators here

        for (OrderPair orderPair : orderPairs.keySet()) {

            csvLine = csvLine.concat(orderPair.outcome.toString() + ",");
        }

        csvLine = csvLine.concat(("\r\n")); // end with new line

        return(csvLine);
    }
}

class Feeds {

    // TODO change this to FeedType after

    static enum FeedType {

    }
}

class OrderPair{

    OrderPair(IOrder longOrder, IOrder shortOrder, double stoploss, double takeprofit) {
        this.longOrder = longOrder;
        this.shortOrder = shortOrder;
        this.stoploss = stoploss;
        this.takeprofit = takeprofit;
    }

    OrderPair(double stoploss, double takeprofit) {
        this.stoploss = stoploss;
        this.takeprofit = takeprofit;
    }

    public enum Outcome {
        LONG,
        SHORT,
        BOTH,
        NEITHER
    }

    double stoploss;    //  the given value
    double takeprofit;  //  the given value

    Outcome outcome = Outcome.NEITHER; // in case the order never closes then it still considered a loss

    IOrder longOrder;
    IOrder shortOrder;
}



//public enum FeedType {
//
//    // TODO Replace this with a way to just choose the instrument, feed type and amount
//
//    TICKS_5_BID(new TickBarFeedDescriptor(mainInstrument, TickBarSize.FIVE, OfferSide.BID)),
//    RENKO_2_PIPS_BID(new RenkoFeedDescriptor(mainInstrument, PriceRange.TWO_PIPS, OfferSide.BID)),
//    TIME_1_SEC_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.ONE_SEC, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_30_SEC_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.THIRTY_SECS, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_1_MIN_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.ONE_MIN, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_5_MIN_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.FIVE_MINS, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_15_MIN_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.FIFTEEN_MINS, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_1_H_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.ONE_HOUR, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_4_H_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.FOUR_HOURS, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_1_D_BID(new TimePeriodAggregationFeedDescriptor(mainInstrument, Period.DAILY, OfferSide.BID, Filter.WEEKENDS)),
//
//
//    RANGE_10_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(10), OfferSide.BID)),
//    RANGE_5_PIPS_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(5), OfferSide.BID)),
//    RANGE_1_PIP_BID(new RangeBarFeedDescriptor(mainInstrument, PriceRange.valueOf(1), OfferSide.BID));
//
//    private final IFeedDescriptor feedDescriptor;
//
//    FeedType(IFeedDescriptor feedDescriptor) {
//        this.feedDescriptor = feedDescriptor;
//    }
//
//    public IFeedDescriptor getFeedDescriptor() {
//        return feedDescriptor;
//    }
//}