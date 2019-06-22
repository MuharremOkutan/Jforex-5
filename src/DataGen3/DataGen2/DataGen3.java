package DataGen3.DataGen2;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.*;
import com.dukascopy.api.feed.util.*;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

enum FeedType {

    /// TIME BARS
//    TIME_2_SEC_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.createCustomPeriod(Unit.Second, 2), OfferSide.BID, Filter.WEEKENDS)),
//    TIME_15_SEC_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.createCustomPeriod(Unit.Second, 15), OfferSide.BID, Filter.WEEKENDS)),
//    TIME_30_SEC_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.THIRTY_SECS, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_1_MIN_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_MIN, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_5_MIN_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.FIVE_MINS, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_15_MIN_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.FIFTEEN_MINS, OfferSide.BID, Filter.WEEKENDS)),
    TIME_1_H_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_HOUR, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_4_H_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.FOUR_HOURS, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_1_D_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.DAILY, OfferSide.BID, Filter.WEEKENDS)),
//    TIME_1_W_BID(new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.WEEKLY, OfferSide.BID, Filter.WEEKENDS)),


    //// TICK BARS
//    TICKS_5_BID(new TickBarFeedDescriptor(Instrument.EURUSD, TickBarSize.FIVE, OfferSide.BID)),
//    TICKS_10_BID(new TickBarFeedDescriptor(Instrument.EURUSD, TickBarSize.valueOf(10), OfferSide.BID)),
//    TICKS_20_BID(new TickBarFeedDescriptor(Instrument.EURUSD, TickBarSize.valueOf(20), OfferSide.BID)),
//    TICKS_50_BID(new TickBarFeedDescriptor(Instrument.EURUSD, TickBarSize.valueOf(50), OfferSide.BID)),
//    TICKS_100_BID(new TickBarFeedDescriptor(Instrument.EURUSD, TickBarSize.valueOf(100), OfferSide.BID)),
//    TICKS_200_BID(new TickBarFeedDescriptor(Instrument.EURUSD, TickBarSize.valueOf(200), OfferSide.BID)),
//    TICKS_500_BID(new TickBarFeedDescriptor(Instrument.EURUSD, TickBarSize.valueOf(500), OfferSide.BID)),

    /// RANGE BARS
//    RANGE_1_PIP_BID(new RangeBarFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(1), OfferSide.BID)),
//    RANGE_5_PIPS_BID(new RangeBarFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(5), OfferSide.BID)),
//    RANGE_10_PIPS_BID(new RangeBarFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(10), OfferSide.BID)),
//    RANGE_20_PIPS_BID(new RangeBarFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(20), OfferSide.BID)),
//    RANGE_50_PIPS_BID(new RangeBarFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(50), OfferSide.BID)),
//    RANGE_100_PIPS_BID(new RangeBarFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(100), OfferSide.BID)),
//    RANGE_200_PIPS_BID(new RangeBarFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(200), OfferSide.BID, Period.TICK)),

    //// RENKO BARS
//    RENKO_1_PIPS_BID(new RenkoFeedDescriptor(Instrument.EURUSD, PriceRange.ONE_PIP, OfferSide.BID)),
//    RENKO_5_PIPS_BID(new RenkoFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(5), OfferSide.BID)),
//    RENKO_10_PIPS_BID(new RenkoFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(10), OfferSide.BID)),
//    RENKO_20_PIPS_BID(new RenkoFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(20), OfferSide.BID)),
//    RENKO_50_PIPS_BID(new RenkoFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(50), OfferSide.BID)),
//    RENKO_100_PIPS_BID(new RenkoFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(100), OfferSide.BID)),
//    RENKO_200_PIPS_BID(new RenkoFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(200), OfferSide.BID)),

    //// KAGI BARS - doesnt really work
//    KAGI_1_PIP_BID(new KagiFeedDescriptor(Instrument.EURUSD, OfferSide.BID, PriceRange.valueOf(1))),
//    KAGI_5_PIP_BID(new KagiFeedDescriptor(Instrument.EURUSD, OfferSide.BID, PriceRange.valueOf(5))),
//    KAGI_10_PIP_BID(new KagiFeedDescriptor(Instrument.EURUSD, OfferSide.BID, PriceRange.valueOf(10))),
//    KAGI_20_PIP_BID(new KagiFeedDescriptor(Instrument.EURUSD, OfferSide.BID, PriceRange.valueOf(20))),
//    KAGI_50_PIP_BID(new KagiFeedDescriptor(Instrument.EURUSD, OfferSide.BID, PriceRange.valueOf(50))),
//
    //// POINT & FIGURE BARS
//    PF_1_PIP_X_3_BID(new PointAndFigureFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(1), ReversalAmount.THREE, OfferSide.BID)),
//    PF_3_PIP_X_3_BID(new PointAndFigureFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(3), ReversalAmount.THREE, OfferSide.BID)),
//    PF_10_PIP_X_3_BID(new PointAndFigureFeedDescriptor(Instrument.EURUSD, PriceRange.valueOf(10), ReversalAmount.THREE, OfferSide.BID)),
//
//    //// LINEBREAK BARS
//    LINEBREAK_15_SEC_BID(new LineBreakFeedDescriptor(Instrument.EURUSD, OfferSide.BID, Period.createCustomPeriod(Unit.Second, 5))),
//    LINEBREAK_1_MIN_BID(new LineBreakFeedDescriptor(Instrument.EURUSD, OfferSide.BID, Period.createCustomPeriod(Unit.Minute, 1))),
//    LINEBREAK_5_MIN_BID(new LineBreakFeedDescriptor(Instrument.EURUSD, OfferSide.BID, Period.createCustomPeriod(Unit.Minute, 5))),
//    LINEBREAK_15_MIN_BID(new LineBreakFeedDescriptor(Instrument.EURUSD, OfferSide.BID, Period.createCustomPeriod(Unit.Minute, 15))),
//    LINEBREAK_1_H_BID(new LineBreakFeedDescriptor(Instrument.EURUSD, OfferSide.BID, Period.createCustomPeriod(Unit.Hour, 1))),
//    LINEBREAK_4_H_BID(new LineBreakFeedDescriptor(Instrument.EURUSD, OfferSide.BID, Period.createCustomPeriod(Unit.Hour, 4))),
//    LINEBREAK_1_D_BID(new LineBreakFeedDescriptor(Instrument.EURUSD, OfferSide.BID, Period.createCustomPeriod(Unit.Day, 1)))

    ;

    private final IFeedDescriptor feedDescriptor;

    FeedType(IFeedDescriptor feedDescriptor) {
        this.feedDescriptor = feedDescriptor;
    }

    public IFeedDescriptor getFeedDescriptor() {
        return feedDescriptor;
    }
}

@Library("common-lang3.jar")
public class DataGen3  implements IStrategy, IFeedListener {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;

    private Writer csvWriter;

    JForexPrinter printer;

    Instant startDateTime;

    Set<RowData> rowDatas = new HashSet<RowData>();

    int globalTradeNo = 0;

//    Set<FeedType> feeds = new HashSet<FeedType>();

    @Configurable("Instrument")
    public static Instrument mainInstrument = Instrument.EURUSD;
    @Configurable("Feed type")
    public FeedType mainFeed;

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


    public void onStart(IContext context) throws JFException {

        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();

        printer = new JForexPrinter(console);

        RowData.console = console; // so that we can write errors in rowdata

        mainFeed.getFeedDescriptor().setInstrument(mainInstrument);
        mainFeed.getFeedDescriptor().setFilter(filter);

        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(mainFeed.getFeedDescriptor().getInstrument());

        printer.printMe("MAIN FEED: " + mainFeed.getFeedDescriptor());


        // save the start time for the file name
        Instrument mainInstrument = mainFeed.getFeedDescriptor().getInstrument();
        startDateTime = Instant.ofEpochMilli(history.getTimeOfLastTick(mainInstrument));

        // add all feeds in FeedType to feeds list
        for (FeedType feed : FeedType.values()) {
            // TODO add other symbols here..and subscribe to them as well.

            printer.printMe("Subscibing to " + feed);

            feed.getFeedDescriptor().setInstrument(mainInstrument);
            feed.getFeedDescriptor().setFilter(filter);

//            feeds.add(feed);

            context.setSubscribedInstruments(instruments, true);

            //Subscribe to a feed:
            context.subscribeToFeed(feed.getFeedDescriptor(), this);
        }


    }

    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData) {

        if (!(feedData instanceof IBar)) {
            printer.printMeError("Cannot work with tick feed data");
            return;
        }
//        printer.printMe("Feed1 " + feedDescriptor);

        DataType dataType = feedDescriptor.getDataType();

        if (!feedDescriptor.getDataType().equals(mainFeed.getFeedDescriptor().getDataType())) {
//            printer.printMe("incorrect DataType");
            return; // Skip all feeds other than the mainfeed
        }


        /// TICKBAR
        if (feedData instanceof ITickBar && !feedDescriptor.getTickBarSize().equals(mainFeed.getFeedDescriptor().getTickBarSize())) {
//            printer.printMe("TickBar but wrong size - feedsize: " + feedDescriptor.getTickBarSize() + " - mainfeed size: " + mainFeed.getFeedDescriptor().getTickBarSize());
            return; // Skip all feeds other than the mainfeed
        }
        //// ANY TYPE OF PRICE RANGE TYPE
        else if ((dataType.equals(DataType.PRICE_RANGE_AGGREGATION) ||
                dataType.equals(DataType.RENKO) ||
                dataType.equals(DataType.POINT_AND_FIGURE) ||
                dataType.equals(DataType.KAGI)) &&
                !feedDescriptor.getPriceRange().equals(mainFeed.getFeedDescriptor().getPriceRange())) {

//            printer.printMe("Price aggregation bar but wrong range");
            return; // Skip all feeds other than the mainfeed
        }
        // ANY TYPE OF TIME TYPE
        else if ((dataType.equals(DataType.TIME_PERIOD_AGGREGATION) ||
                dataType.equals(DataType.LINE_BREAK)) &&
                !feedDescriptor.getPeriod().equals(mainFeed.getFeedDescriptor().getPeriod())) {
//            printer.printMe("Time aggregation bar but wrong period");
            return;
        }

//        printer.printMe("Feed2 " + feedDescriptor);
//        if(!feedDescriptor.equals(mainFeed.getFeedDescriptor())) {
//            return; // Skip all feeds other than the mainfeed
//        }
//        printer.printMe("Feed2 ");
        try {

            Instrument instrument = feedDescriptor.getInstrument();

            Instant barOpenedTime = Instant.ofEpochMilli(feedData.getTime()); // this is the start time of the bar not the current time
            Instant lastTickTime = Instant.ofEpochMilli(history.getTimeOfLastTick(instrument));// this is the time the bar closed, so like actualTime

            RowData rowData = new RowData(barOpenedTime, lastTickTime);

            // TODO here would also add all other feeds to the rowData


            for (FeedType feedType : FeedType.values()) {

                IFeedDescriptor usedFeedDescriptor = feedType.getFeedDescriptor();
                OfferSide usedOfferSide = usedFeedDescriptor.getOfferSide();
//                rowData.feedBars.put(mainFeed, feedData);

                ///// FEED BAR DATA
                printer.printMe("Fetching bar data for " + usedFeedDescriptor);
                ITimedData usedFeedData = history.getFeedData(usedFeedDescriptor, 0);
                rowData.feedBars.put(feedType, usedFeedData);

                ///// FEED INDICATOR DATA

                printer.printMe("Fethicng indicator Data");

                // SMA
                CompletableFuture<Double> sma5Future = supplyAsync(() -> indicators.sma(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        5).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                CompletableFuture<Double> sma20Future = supplyAsync(() -> indicators.sma(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        20).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                // EMA
                CompletableFuture<Double> ema5Future = supplyAsync(() -> indicators.ema(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        5).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                CompletableFuture<Double> ema20Future = supplyAsync(() -> indicators.ema(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        20).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                // RSI
                CompletableFuture<Double> rsi14Future = supplyAsync(() -> indicators.rsi(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        14).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                // ADX
                CompletableFuture<Double> adx14Future = supplyAsync(() -> indicators.adx(usedFeedDescriptor, usedOfferSide,
                        14).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                // CCI
                CompletableFuture<Double> cci14Future = supplyAsync(() -> indicators.cci(usedFeedDescriptor, usedOfferSide,
                        14).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                // ATR
                CompletableFuture<Double> atr14Future = supplyAsync(() -> indicators.atr(usedFeedDescriptor, usedOfferSide,
                        14).calculate(0))
                        .exceptionally(e -> {
                            return 0.;
                        });

                Double[] emptyArray = {0., 0., 0., 0., 0.};

                // BOLLINGER BANDS
                CompletableFuture<Double[]> bbands20_2Future = supplyAsync(() -> ArrayUtils.toObject(indicators.bbands(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        20, 2, 2, IIndicators.MaType.SMA).calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });

                CompletableFuture<Double[]> bbands20_1618Future = supplyAsync(() -> ArrayUtils.toObject(indicators.bbands(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        20, 1.618, 1.618, IIndicators.MaType.SMA).calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });

                CompletableFuture<Double[]> bbands20_2618Future = supplyAsync(() -> ArrayUtils.toObject(indicators.bbands(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        20, 2.618, 2.618, IIndicators.MaType.SMA).calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });

                CompletableFuture<Double[]> bbands20_4236Future = supplyAsync(() -> ArrayUtils.toObject(indicators.bbands(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        20, 4.236, 4.236, IIndicators.MaType.SMA).calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });

                // STOCHASTIC OSCILLATOR
                CompletableFuture<Double[]> stoch5_3_3Future = supplyAsync(() -> ArrayUtils.toObject(indicators.stoch(usedFeedDescriptor, usedOfferSide,
                        5, 3, IIndicators.MaType.SMA, 3, IIndicators.MaType.SMA).calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });

                // MACD
                CompletableFuture<Double[]> macd12_26_9Future = supplyAsync(() -> ArrayUtils.toObject(indicators.macd(usedFeedDescriptor, IIndicators.AppliedPrice.CLOSE, usedOfferSide,
                        12, 26, 9).calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });

                // FRACTAL
                CompletableFuture<Double[]> fractal5Future = supplyAsync(() -> ArrayUtils.toObject(indicators.fractal(usedFeedDescriptor, usedOfferSide,
                        5).calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });

                // SUPPORT AND RESISTANCE
                CompletableFuture<Double[]> supResFuture = supplyAsync(() -> ArrayUtils.toObject(indicators.supportResistance(usedFeedDescriptor, usedOfferSide)
                        .calculate(0)))
                        .exceptionally(e -> {
                            return emptyArray;
                        });


//                Double[] emptyArray = {0., 0., 0., 0., 0.};
//
//                double ema5 = 0;
//                double ema20 = 0;
//                double sma5 = 0;
//                double sma20 = 0;
//                double rsi14 = 0;
//                double adx14 = 0;
//                double cci14 = 0;
//                double atr14 = 0;
//
//                Double[] bbands20_2 = emptyArray;
//                Double[] bbands20_1618 = emptyArray;
//                Double[] bbands20_2618 = emptyArray;
//                Double[] bbands20_4236 = emptyArray;
//
//                Double[] stoch5_3_3 = emptyArray;
//                Double[] macd12_26_9 = emptyArray;
//                Double[] fractal5 = emptyArray;
//                Double[] supRes = emptyArray;
//
//
//                /// BLOCKING GET CALLS
//                try {
////                    ema5 = ema5Future.get(1, TimeUnit.SECONDS);
//                    ema20 = ema20Future.get(1, TimeUnit.SECONDS);
//                    sma5 = sma5Future.get(1, TimeUnit.SECONDS);
//                    sma20 = sma20Future.get(1, TimeUnit.SECONDS);
//                    rsi14 = rsi14Future.get(1, TimeUnit.SECONDS);
//                    adx14 = adx14Future.get(1, TimeUnit.SECONDS);
//                    cci14 = cci14Future.get(1, TimeUnit.SECONDS);
//                    atr14 = atr14Future.get(1, TimeUnit.SECONDS);
//
//                    bbands20_2 = bbands20_2Future.get(1, TimeUnit.SECONDS);
//                    bbands20_1618 = bbands20_1618Future.get(1, TimeUnit.SECONDS);
//                    bbands20_2618 = bbands20_2618Future.get(1, TimeUnit.SECONDS);
//                    bbands20_4236 = bbands20_4236Future.get(1, TimeUnit.SECONDS);
//
//                    stoch5_3_3 = stoch5_3_3Future.get(1, TimeUnit.SECONDS);
//                    macd12_26_9 = macd12_26_9Future.get(1, TimeUnit.SECONDS);
//                    fractal5 = fractal5Future.get(1, TimeUnit.SECONDS);
//                    supRes = supResFuture.get(1, TimeUnit.SECONDS);
//                } catch(TimeoutException e) {
//
//                }

//                printer.printMe("EMA5: " + ema5);


                printer.printMe("here4");


                // EMA
                rowData.ema5.put(feedType, ema5Future);
                rowData.ema20.put(feedType, ema20Future);
                // SMA
                rowData.sma5.put(feedType, sma5Future);
                rowData.sma20.put(feedType, sma20Future);
                // RSI
                rowData.rsi14.put(feedType, rsi14Future);
                // Bollinger bands
                rowData.bbands20_2.put(feedType, bbands20_2Future);
                rowData.bbands20_1618.put(feedType, bbands20_1618Future);
                rowData.bbands20_2618.put(feedType, bbands20_2618Future);
                rowData.bbands20_4236.put(feedType, bbands20_4236Future);
                // STOCHASTIC OSCILLATOR
                rowData.stoch5_3_3.put(feedType, stoch5_3_3Future);
                // MACD
                rowData.macd12_26_9.put(feedType, macd12_26_9Future);
                // ADX
                rowData.adx14.put(feedType, adx14Future);
                // CCI
                rowData.cci14.put(feedType, cci14Future);
                // ATR
                rowData.atr14.put(feedType, atr14Future);
                // FRACTAL
                rowData.fractal5.put(feedType, fractal5Future); // will always be N/A for shift 0 since it doesnt show up until 5 candles after
                // SUPPORT AND RESISTANCE
                rowData.supRes.put(feedType, supResFuture); // will always be N/A for shift 0 since it doesnt show up until 2 candles after
            }
            /////////////////// END OF GETTING DATA FOR SAVING

            ////////////////// OPENING ORDERS //////
            double lastTickBid = history.getLastTick(instrument).getBid();
            double lastTickAsk = history.getLastTick(instrument).getAsk();

            int orderNo = 0;

            // OPEN ALL THE ORDER PAIRS BY LOOPING THROUGH THE SL AND TP
            for (int stopLossPips = baseStopLossPips; stopLossPips <= endStopLossPips; stopLossPips += stepStopLossPips) {
                for (int takeProfitPips = baseTakeProfitPips; takeProfitPips <= endTakeProfitPips; takeProfitPips += stepTakeProfitPips) {

//                    printer.printMe("SL: "+ stopLossPips+", TP: "+takeProfitPips);

                    // if we dont allow the outcome to be BOTH we cannot trade with a SL > TP
                    if (!isAllowBoth && stopLossPips > takeProfitPips) continue;

                    double stopLossValue = instrument.getPipValue() * stopLossPips;
                    double takeprofitValue = instrument.getPipValue() * takeProfitPips;

                    double stopLossPriceLong = lastTickBid - stopLossValue;
                    double stopLossPriceShort = lastTickAsk + stopLossValue;

                    double takeProfitPriceLong = lastTickBid + takeprofitValue;
                    double takeProfitPriceShort = lastTickAsk - takeprofitValue;

                    double maxSpreadValue = instrument.getPipValue() * maxSpreadPips;
                    double spreadValue = lastTickAsk - lastTickBid;

                    if (spreadValue <= maxSpreadValue) {

                        String label = String.format("TP%s_SL%s_%s_%s_%d_%d",
                                takeProfitPips, stopLossPips, lastTickTime.getEpochSecond(), lastTickTime.getNano(), globalTradeNo++, orderNo++);

//                        printer.printMe("LABEL: "+("long_".concat(label)));

                        IOrder longOrder = engine.submitOrder("LONG_".concat(label), instrument,
                                IEngine.OrderCommand.BUY, 0.001, 0, 100, stopLossPriceLong, takeProfitPriceLong);

                        IOrder shortOrder = engine.submitOrder("SHORT_".concat(label), instrument,
                                IEngine.OrderCommand.SELL, 0.001, 0, 100, stopLossPriceShort, takeProfitPriceShort);

                        OrderPair orderPair = new OrderPair(longOrder, shortOrder, stopLossPips, takeProfitPips);
                        rowData.orderPairs.put(orderPair, false); // add order pair to rowData, false because no outcome set
                        rowDatas.add(rowData); // add rowData to rowDatas set

                    } else if (isNeitherIfAboveMaxPips) { // we still need to create a OrderPair even without Orders
                        OrderPair orderPair = new OrderPair(stopLossPips, takeProfitPips);
                        rowData.orderPairs.put(orderPair, true); // add order pair to rowData, true because no need to check outcome since no real trades
                        rowDatas.add(rowData); // add rowData to rowDatas set
                    }
                }
            }
        } catch (JFException e) {
            printer.printMeError("Failed with: " + feedDescriptor.toString());
            printer.printMeError(e.getMessage());
//            e.printStackTrace(console.getErr());
        }
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
    }


    public void onStop() throws JFException {

//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            printer.printMeError(e.getMessage());
//            e.printStackTrace(console.getErr());
//        }

        printer.printMe("Stopped - " + rowDatas.size() + " rows to process");


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
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (Exception e) {
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
            for (RowData rowData : rowDatas) {
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


//    public Future<Double> getIndicatorFuture(Method method)
//            throws InterruptedException {
//
//        CompletableFuture<Double> completableFuture = new CompletableFuture<>();
//
//        Executors.newCachedThreadPool().submit(() -> {
//
//            // TODO make it so that instead of giving a string with the type we can just give it the method
//
//            try {
//
//
//            } catch (JFException e) {}
//
////            Thread.sleep(500);
////            completableFuture.cancel(false);
//            return null;
//        });
//
//        return completableFuture;
//    }


//    public static <Double> CompletableFuture<Double> supplyAsync(Callable<Double> c) {
//        CompletableFuture<Double> f=new CompletableFuture<>();
//        CompletableFuture.runAsync(() -> {
//            try { f.complete(c.call()); } catch(Throwable t) { f.completeExceptionally(t); }
//        });
//        return f;
//    }

    private static <T> CompletableFuture<T> supplyAsync(Callable<T> c) {
        CompletableFuture<T> f = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                f.complete(c.call());
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f;
    }
} // end of DATAGEN CLASS


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

    /// CONSTANTS
    public static final int TIME_PERIOD_AGGREGATION_ATTRS = 6;
    public static final int PRICE_RANGE_AGGREGATION_ATTRS = 8;
    public static final int TICK_BAR_ATTRS = 8;
    public static final int RENKO_ATTRS = 9;
    public static final int POINT_AND_FIGURE_ATTRS = 9;
    public static final int LINE_BREAK_ATTRS = 11;
    public static final int KAGI_ATTRS = 14;

    // one RowData object per row on the CSV, holds all data for that row
    Instant mainFeedTimeOpened;
    Instant lastTickTime;

    static IConsole console;

    JForexPrinter printer = new JForexPrinter(console);

    // contains the bar data for each of feed at actualTime, this means most bars will be unfinished,
    Map<FeedType, ITimedData> feedBars = new LinkedHashMap <FeedType, ITimedData>(); // linked to preserve order

    // contains the current indicator data
    // EMA
    Map<FeedType, CompletableFuture> ema5 = new HashMap<>();
    Map<FeedType, CompletableFuture> ema20 = new HashMap<>();

    // SMA
    Map<FeedType, CompletableFuture> sma5 = new HashMap<>();
    Map<FeedType, CompletableFuture> sma20 = new HashMap<>();

    // RSI
    Map<FeedType, CompletableFuture> rsi14 = new HashMap <>();

    // ADX
    Map<FeedType, CompletableFuture> adx14 = new HashMap <>();

    // CCI
    Map<FeedType, CompletableFuture> cci14 = new HashMap <>();

    // ATR
    Map<FeedType, CompletableFuture> atr14 = new HashMap <>();

    // Bollinger bands
    Map<FeedType, CompletableFuture<Double[]>> bbands20_2 = new HashMap <>();
    Map<FeedType, CompletableFuture<Double[]>> bbands20_1618 = new HashMap <>();
    Map<FeedType, CompletableFuture<Double[]>> bbands20_2618 = new HashMap <>();
    Map<FeedType, CompletableFuture<Double[]>> bbands20_4236 = new HashMap <>();

    // STOCHASTIC OSCILLATOR
    Map<FeedType, CompletableFuture<Double[]>> stoch5_3_3 = new HashMap <>();

    // MACD
    Map<FeedType, CompletableFuture<Double[]>> macd12_26_9 = new HashMap <>();

    // FRACTAL
    Map<FeedType, CompletableFuture<Double[]>> fractal5 = new HashMap <>();

    // SUPPORT AND RESISTANCE
    Map<FeedType, CompletableFuture<Double[]>> supRes = new HashMap <>();

    // TODO add indicators

    Map<OrderPair, Boolean> orderPairs = new LinkedHashMap <OrderPair, Boolean>(); // map of orders for this row <OrderPair, isOutcomeSet> // linked to preserve order

    RowData(Instant mainFeedTimeOpened, Instant lastTickTime) {
        this.mainFeedTimeOpened = mainFeedTimeOpened;
        this.lastTickTime = lastTickTime;
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
            # Last Tick Time
            ### FEED INFO (for each feed)
            # Time of FeedBar Opened
            # OHLC
            # Volume
            ### INDICATOR (for each indicator)
            # Indicator values
            ### OUTCOME (for each order pair)
            # Outcome
         */

        String header = "TimeOpened, LastTickTime, ";

        for (FeedType feed : feedBars.keySet()) {

            IFeedDescriptor feedDescriptor = feed.getFeedDescriptor();

            String feedName = String.format("%s %s", feedDescriptor.getInstrument(), feed);

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

            // TODO <--- add indicators here

//            header = header.concat(feedName + " ema5,");

            String indicatorStr =

                    // EMA
                    feedName + " ema5," +
                    feedName + " ema20," +

                    // SMA
                    feedName + " sma5," +
                    feedName + " sma20," +

                    // RSI - @return value for the specified bar
                    feedName + " rsi14," +

                    // Bollinger bands - @return values for the specified bars, outputs are returned in the following order: 'Upper Band', 'Middle Band', 'Lower Band'
                    // SKIPPING MIDDLE BAND
                    feedName + " bbands20_2_Upper," +
                    feedName + " bbands20_2_Lower," +

                    feedName + " bbands20_1618_Upper," +
                    feedName + " bbands20_1618_Lower," +

                    feedName + " bbands20_2618_Upper," +
                    feedName + " bbands20_2618_Lower," +

                    feedName + " bbands20_4236_Upper," +
                    feedName + " bbands20_4236_Lower," +

                    // STOCHASTIC OSCILLATOR - @return values for the specified bars, outputs are returned in the following order: 'Slow %K', 'Slow %D'
                    feedName + " stoch5_3_3_K," +
                    feedName + " stoch5_3_3_D," +

                    // MACD - @return values for the specified bars, outputs are returned ins the following order: 'MACD', 'MACD Signal', 'MACD Hist'
                    feedName + " macd12_26_9_MACD," +
                    feedName + " macd12_26_9_Signal," +
                    feedName + " macd12_26_9_Hist," +

                    // ADX - @return values for the specified bars
                    feedName + " adx14," +

                    // CCI - @return values for the specified bars
                    feedName + " cci14," +

                    // ATR
                    feedName + " atr14," +

                    // FRACTAL - @return values for the specified bars, outputs are returned in the following order: 'Maximums', 'Minimums'
                    feedName + " fractal5_Max," +
                    feedName + " fractal5_Min," +

                    // SUPPORT AND RESISTANCE - @return values for the specified bars, outputs are returned in the following order: 'Maximums', 'Minimums'
                    feedName + " supRes_Max," +
                    feedName + " supRes_Min,";

            header = header.concat(indicatorStr);

        }



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
                lastTickTime + ",";



        //////// FEED BAR DATA ////////

        for (Map.Entry<FeedType, ITimedData> entry : feedBars.entrySet()) {

            try {
                IFeedDescriptor feedDescriptor = entry.getKey().getFeedDescriptor();
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
            catch(NullPointerException e){

                IFeedDescriptor feedDescriptor = entry.getKey().getFeedDescriptor();

                printer.printMe("NullPointExc in: "+feedDescriptor);

                int feedAttrs = 0;

                if (feedDescriptor.getDataType().equals(DataType.TIME_PERIOD_AGGREGATION))      feedAttrs = TIME_PERIOD_AGGREGATION_ATTRS;
                else if (feedDescriptor.getDataType().equals(DataType.PRICE_RANGE_AGGREGATION)) feedAttrs = PRICE_RANGE_AGGREGATION_ATTRS;
                else if (feedDescriptor.getDataType().equals(DataType.TICK_BAR))                feedAttrs = TICK_BAR_ATTRS;
                else if (feedDescriptor.getDataType().equals(DataType.RENKO))                   feedAttrs = RENKO_ATTRS;
                else if (feedDescriptor.getDataType().equals(DataType.POINT_AND_FIGURE))        feedAttrs = POINT_AND_FIGURE_ATTRS;
                else if (feedDescriptor.getDataType().equals(DataType.LINE_BREAK))              feedAttrs = LINE_BREAK_ATTRS;
                else if (feedDescriptor.getDataType().equals(DataType.KAGI))                    feedAttrs = KAGI_ATTRS;

                for (int i = 0; i < feedAttrs; i++) {
                    csvLine = csvLine.concat("null,");
                }

//                printer.printMeError(e.getMessage());
//                e.printStackTrace(console.getErr());
            }
//            printer.printMe("----rowdata-----fractal5  0: "+fractal5.get(entry.getKey())[0]);
//            printer.printMe("-----rowdata----fractal5  1: "+fractal5.get(entry.getKey())[1]);
//            printer.printMe("----rowdata-----supRes  2: "+supRes.get(entry.getKey())[0]);
//            printer.printMe("----rowdata-----supRes  2: "+supRes.get(entry.getKey())[1]);

            int timePeriod = 100;
            TimeUnit unit = TimeUnit.MILLISECONDS;

            String indicatorStr;

            try {
                Double[] bbands20_2Val = bbands20_2.get(entry.getKey()).get(timePeriod, unit);
                Double[] bbands20_1618Val = bbands20_1618.get(entry.getKey()).get(timePeriod, unit);
                Double[] bbands20_2618Val = bbands20_2618.get(entry.getKey()).get(timePeriod, unit);
                Double[] bbands20_4236Val = bbands20_4236.get(entry.getKey()).get(timePeriod, unit);
                Double[] stoch5_3_3Val = stoch5_3_3.get(entry.getKey()).get(timePeriod, unit);
                Double[] macd12_26_9Val = macd12_26_9.get(entry.getKey()).get(timePeriod, unit);
                Double[] fractal5Val = fractal5.get(entry.getKey()).get(timePeriod, unit);
                Double[] supResVal = supRes.get(entry.getKey()).get(timePeriod, unit);

                indicatorStr =

                // EMA
                ema5.get(entry.getKey()).get(timePeriod, unit) + "," +
                ema20.get(entry.getKey()).get(timePeriod, unit) + "," +

                // SMA
                sma5.get(entry.getKey()).get(timePeriod, unit) + "," +
                sma20.get(entry.getKey()).get(timePeriod, unit) + "," +

                // RSI - @return value for the specified bar
                rsi14.get(entry.getKey()).get(timePeriod, unit) + "," +

                // ADX - @return values for the specified bars
                adx14.get(entry.getKey()).get(timePeriod, unit) + "," +

                // CCI - @return values for the specified bars
                cci14.get(entry.getKey()).get(timePeriod, unit) + "," +

                // ATR
                atr14.get(entry.getKey()).get(timePeriod, unit) + "," +

                // Bollinger bands - @return values for the specified bars, outputs are returned in the following order: 'Upper Band', 'Middle Band', 'Lower Band'
                bbands20_2Val[0] + "," +
                bbands20_2Val[2] + "," +

                bbands20_1618Val[0] + "," +
                bbands20_1618Val[2] + "," +

                bbands20_2618Val[0] + "," +
                bbands20_2618Val[2] + "," +

                bbands20_4236Val[0] + "," +
                bbands20_4236Val[2] + "," +

                // STOCHASTIC OSCILLATOR - @return values for the specified bars, outputs are returned in the following order: 'Slow %K', 'Slow %D'
                stoch5_3_3Val[0] + "," +
                stoch5_3_3Val[1] + "," +

                // MACD - @return values for the specified bars, outputs are returned in the following order: 'MACD', 'MACD Signal', 'MACD Hist'
                macd12_26_9Val[0] + "," +
                macd12_26_9Val[1] + "," +
                macd12_26_9Val[2] + "," +

                // FRACTAL - @return values for the specified bars, outputs are returned in the following order: 'Maximums', 'Minimums'
                fractal5Val[0] + "," +
                fractal5Val[1] + "," +

                // SUPPORT AND RESISTANCE - @return values for the specified bars, outputs are returned in the following order: 'Maximums', 'Minimums'
                supResVal[0] + "," +
                supResVal[1] + ",";

                csvLine = csvLine.concat(indicatorStr);

            } catch (Exception e) {}
//
//
//
//
//                    // Bollinger bands - @return values for the specified bars, outputs are returned in the following order: 'Upper Band', 'Middle Band', 'Lower Band'
//                    bbands20_2.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    bbands20_2.get(entry.getKey()).get(timePeriod, unit)[2] + "," +
//
//                    bbands20_1618.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    bbands20_1618.get(entry.getKey()).get(timePeriod, unit)[2] + "," +
//
//                    bbands20_2618.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    bbands20_2618.get(entry.getKey()).get(timePeriod, unit)[2] + "," +
//
//                    bbands20_4236.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    bbands20_4236.get(entry.getKey()).get(timePeriod, unit)[2] + "," +
//
//                    // STOCHASTIC OSCILLATOR - @return values for the specified bars, outputs are returned in the following order: 'Slow %K', 'Slow %D'
//                    stoch5_3_3.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    stoch5_3_3.get(entry.getKey()).get(timePeriod, unit)[1] + "," +
//
//                    // MACD - @return values for the specified bars, outputs are returned in the following order: 'MACD', 'MACD Signal', 'MACD Hist'
//                    macd12_26_9.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    macd12_26_9.get(entry.getKey()).get(timePeriod, unit)[1] + "," +
//                    macd12_26_9.get(entry.getKey()).get(timePeriod, unit)[2] + "," +
//
//                    // FRACTAL - @return values for the specified bars, outputs are returned in the following order: 'Maximums', 'Minimums'
//                    fractal5.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    fractal5.get(entry.getKey()).get(timePeriod, unit)[1] + "," +
//
//                    // SUPPORT AND RESISTANCE - @return values for the specified bars, outputs are returned in the following order: 'Maximums', 'Minimums'
//                    supRes.get(entry.getKey()).get(timePeriod, unit)[0] + "," +
//                    supRes.get(entry.getKey()).get(timePeriod, unit)[1] + ",";
//
//                csvLine = csvLine.concat(indicatorStr);

//        } catch (Exception e) {}
    }



        // TODO <--- add indicators here

        for (OrderPair orderPair : orderPairs.keySet()) {

            csvLine = csvLine.concat(orderPair.outcome.toString() + ",");
        }

        csvLine = csvLine.concat(("\r\n")); // end with new line

        return(csvLine);
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
//
//class getIndicatorValueThread implements Runnable {
//    private volatile double doubleVal;
//    private volatile double[] doubleListVal;
//
//    IFeedDescriptor feedDescriptor;
//    IIndicators indicators;
//    OfferSide offerSide;
//    String type;
//
//
//    getIndicatorValueThread(IFeedDescriptor feedDescriptor, IIndicators indicators, OfferSide offerSide, String type) {
//        this.feedDescriptor = feedDescriptor;
//        this.indicators = indicators;
//        this.offerSide = offerSide;
//        this.type = type;
//    }
//
//    @Override
//    public void run() {
//        try {
//            if (type.equals("ema5")) {
//                doubleVal = indicators.ema(feedDescriptor, IIndicators.AppliedPrice.CLOSE, offerSide,
//                        5).calculate(0);
//            }
//        } catch (JFException e) {}
//    }
//
//    public double getDoubleVal(){ return doubleVal; }
//    public double[] getDoubleListVal() { return doubleListVal; }
//}

