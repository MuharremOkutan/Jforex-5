package singlejartest;

import com.dukascopy.api.*;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.IOhlcChartObject;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.dukascopy.api.indicators.OutputParameterInfo.DrawingStyle;

import com.dukascopy.api.IIndicators.AppliedPrice;

import com.dukascopy.api.drawings.IChartDependentChartObject;
import com.dukascopy.api.drawings.ITriangleChartObject;
import com.dukascopy.api.drawings.ITextChartObject;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StrategyTestWFeed implements IStrategy, IFeedListener {

    private enum SMATrend {
        UP, DOWN, NOT_SET;
    }

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;


    @Configurable(value="Instrument value")
    public Instrument myInstrument = Instrument.EURGBP;
    @Configurable(value="Offer Side value", obligatory=true)
    public OfferSide myOfferSide;
    @Configurable(value="Period value")
    public Period myPeriod = Period.TEN_MINS;
    @Configurable("SMA time period")
    public int smaTimePeriod = 30;
    @Configurable("Add OHLC Index to chart")
    public boolean addOHLC = true;
    @Configurable("Filter")
    public Filter filter = Filter.WEEKENDS;
    @Configurable("Draw SMA")
    public boolean drawSMA = true;
    @Configurable("Close chart on onStop")
    public boolean closeChart;

    @Configurable("Stop loss in pips")
    public int stopLossPips = 10;
    @Configurable("Take profit in pips")
    public int takeProfitPips = 10;
    @Configurable("Break event pips")
    public double breakEventPips = 5;

    private static final int PREV = 1;
    private static final int SECOND_TO_LAST = 0;

    private IBar previousBar;
    private ITick myLastTick;

    private IOrder order;

    private IChart openedChart;
    private IChartObjectFactory factory;
    private int signals;

    private int uniqueOrderCounter = 1;
    private SMATrend previousSMADirection = SMATrend.NOT_SET;
    private SMATrend currentSMADirection = SMATrend.NOT_SET;
    private Map<IOrder, Boolean> createdOrderMap = new HashMap<IOrder, Boolean>();
    private int shortLineCounter;
    private int textCounterOldSL;
    private int textCounterNewSL;


    private void printMe(String toPrint){
        console.getOut().println(toPrint);
    }

    private void printMeError(Object o){
        console.getErr().println(o);
    }

    public void onStart(IContext context) throws JFException {

        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();

        this.openedChart = context.getChart(myInstrument);
        if(this.openedChart != null) {
            this.factory = openedChart.getChartObjectFactory();
        }
        console.getOut().println("onStart Called");

        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(myInstrument);
        context.setSubscribedInstruments(instruments, true);




//        previousBar = history.getBar(myInstrument, myPeriod, myOfferSide, 1);
//        myLastTick = history.getLastTick(myInstrument);
//
//        console.getOut().println(previousBar);
//        console.getOut().println(myLastTick);
        if(drawSMA) {
            if (!addToChart(openedChart)) {
                printMeError("Indicators did not get plotted on chart. Check the chart values!");
            }
        }
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
        if(closeChart)  context.closeChart(openedChart);
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
//        console.getOut().println(instrument+" "+tick.getAsk()+"/"+tick.getBid());
        if (instrument != myInstrument) {
            return;
        }

//        long prevBarTime = history.getPreviousBarStart(Period.TEN_SECS, tick.getTime());
//        List<IBar> bars = history.getBars(instrument, Period.TEN_SECS, OfferSide.BID, history.getTimeForNBarsBack(Period.TEN_SECS, prevBarTime, 10), prevBarTime);
//
//        printMe("prevBarTime: "+ prevBarTime);


        for (Map.Entry<IOrder, Boolean> entry : createdOrderMap.entrySet()) {
            IOrder currentOrder = entry.getKey();
            boolean currentValue = entry.getValue();
            if (currentValue == false && currentOrder.getProfitLossInPips() >= breakEventPips) {
                printMe("Order has profit of " + currentOrder.getProfitLossInPips() + " pips! Moving the stop loss to the open price.");
                addBreakToChart(currentOrder, tick, currentOrder.getStopLossPrice(), currentOrder.getOpenPrice());
                //add a line to the chart indicating the SL changes
                currentOrder.setStopLossPrice(currentOrder.getOpenPrice());
                entry.setValue(true);
            }
        }


        try {
            IBar prevBar = history.getBar(Instrument.EURUSD, Period.ONE_HOUR, OfferSide.BID, 0);
            console.getOut().println(prevBar);
        } catch (Exception e) {}
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (!instrument.equals(myInstrument) || !period.equals(myPeriod)) {
            return; //quit
        }

        int candlesBefore = 2, candlesAfter = 0;
        long completedBarTimeL = myOfferSide == OfferSide.ASK ? askBar.getTime() : bidBar.getTime();
        double sma[] = indicators.sma(instrument, period, myOfferSide, IIndicators.AppliedPrice.CLOSE,
                smaTimePeriod, Filter.NO_FILTER, candlesBefore, completedBarTimeL, candlesAfter);
        printMe(String.format("Bar SMA Values: Second-to-last = %.5f; Last Completed = %.5f", sma[SECOND_TO_LAST], sma[PREV]));

        IEngine.OrderCommand myCommand = null;
        printMe(String.format("Bar SMA Values: Second-to-last = %.5f; Last Completed = %.5f", sma[SECOND_TO_LAST], sma[PREV]));
        if (sma[PREV] > sma[SECOND_TO_LAST]) {
            printMe("SMA in up-trend"); //indicator goes up
            myCommand = IEngine.OrderCommand.BUY;
            currentSMADirection = SMATrend.UP;
        } else if (sma[PREV] < sma[SECOND_TO_LAST]) {
            printMe("SMA in down-trend"); //indicator goes down
            myCommand = IEngine.OrderCommand.SELL;
            currentSMADirection = SMATrend.DOWN;
        } else {
            return;
        }

        double lastTickBid = history.getLastTick(myInstrument).getBid();
        double lastTickAsk = history.getLastTick(myInstrument).getAsk();
        double stopLossValueForLong = myInstrument.getPipValue() * stopLossPips;
        double stopLossValueForShort = myInstrument.getPipValue() * takeProfitPips;
        double stopLossPrice = myCommand.isLong() ? (lastTickBid - stopLossValueForLong) : (lastTickAsk + stopLossValueForLong);
        double takeProfitPrice = myCommand.isLong() ? (lastTickBid + stopLossValueForShort) : (lastTickAsk - stopLossValueForShort);

        //if SMA trend direction is changed, then create a new order
        if (currentSMADirection != previousSMADirection) {
            previousSMADirection = currentSMADirection;
            IOrder newOrder = engine.submitOrder("MyStrategyOrder" + uniqueOrderCounter++, instrument, myCommand, 0.1, 0, 1, stopLossPrice, takeProfitPrice);
            createdOrderMap.put(newOrder, false);

            if(openedChart == null){
                return;
            }
            long time = bidBar.getTime() + myPeriod.getInterval(); //draw the  ISignalDownChartObject in the current bar
            double space = myInstrument.getPipValue() * 2; //space up or down from bar for ISignalDownChartObject
            IChartDependentChartObject signal = myCommand.isLong()
                    ? factory.createSignalUp("signalUpKey" + signals++, time, bidBar.getLow() - space)
                    : factory.createSignalDown("signalDownKey" + signals++, time, bidBar.getHigh() + space);
            signal.setStickToCandleTimeEnabled(false);
            signal.setText("MyStrategyOrder" + (uniqueOrderCounter - 1));
            openedChart.addToMainChart(signal);
        }

    }//end of onBar method



    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData) {

        Instrument myInstrument = feedDescriptor.getInstrument();
        OfferSide myOfferSide = feedDescriptor.getOfferSide();



        try {
            if(!(feedData instanceof IBar)){
                printMeError("Cannot work with tick feed data");
                return;
            }

            IBar bar = (IBar) feedData;

            int candlesBefore = 2, candlesAfter = 0;
            long completedBarTimeL = bar.getTime();

            Object[] smaObjectsFeed = indicators.calculateIndicator(feedDescriptor, new OfferSide[] { myOfferSide }, "SMA",
                    new AppliedPrice[] { AppliedPrice.CLOSE }, new Object[] { smaTimePeriod }, candlesBefore, feedData.getTime(), candlesAfter);
            double[] sma = (double[]) smaObjectsFeed[0]; // sma has just 1 output

            printMe(String.format("Bar SMA Values: Second-to-last = %.5f; Last Completed = %.5f", sma[SECOND_TO_LAST], sma[PREV]));

            IEngine.OrderCommand myCommand = null;
            printMe(String.format("Bar SMA Values: Second-to-last = %.5f; Last Completed = %.5f", sma[SECOND_TO_LAST], sma[PREV]));
            if (sma[PREV] > sma[SECOND_TO_LAST]) {
                printMe("SMA in up-trend"); //indicator goes up
                myCommand = IEngine.OrderCommand.BUY;
                currentSMADirection = SMATrend.UP;
            } else if (sma[PREV] < sma[SECOND_TO_LAST]) {
                printMe("SMA in down-trend"); //indicator goes down
                myCommand = IEngine.OrderCommand.SELL;
                currentSMADirection = SMATrend.DOWN;
            } else {
                return;
            }

            double lastTickBid = history.getLastTick(myInstrument).getBid();
            double lastTickAsk = history.getLastTick(myInstrument).getAsk();
            double stopLossValueForLong = myInstrument.getPipValue() * stopLossPips;
            double stopLossValueForShort = myInstrument.getPipValue() * takeProfitPips;
            double stopLossPrice = myCommand.isLong() ? (lastTickBid - stopLossValueForLong) : (lastTickAsk + stopLossValueForLong);
            double takeProfitPrice = myCommand.isLong() ? (lastTickBid + stopLossValueForShort) : (lastTickAsk - stopLossValueForShort);

            //if SMA trend direction is changed, then create a new order
            if (currentSMADirection != previousSMADirection) {
                previousSMADirection = currentSMADirection;
                IOrder newOrder = engine.submitOrder("MyStrategyOrder" + uniqueOrderCounter++, myInstrument, myCommand, 0.1, 0, 1, stopLossPrice, takeProfitPrice);
                createdOrderMap.put(newOrder, false);

                if (openedChart == null) {
                    return;
                }
                //get current time of a bar from IFeedDescriptor object
                long time = history.getFeedData(feedDescriptor, 0).getTime(); //draw the  ISignalDownChartObject in the current bar
                double space = myInstrument.getPipValue() * 2; //space up or down from bar for ISignalDownChartObject
                IChartDependentChartObject signal = myCommand.isLong()
                        ? factory.createSignalUp("signalUpKey" + signals++, time, bar.getLow() - space)
                        : factory.createSignalDown("signalDownKey" + signals++, time, bar.getHigh() + space);
                signal.setText("MyStrategyOrder" + (uniqueOrderCounter - 1));
                openedChart.addToMainChart(signal);
            }
        } catch (Exception e) {
        }
    }


    private boolean addToChart(IChart chart) {
        if (!checkChart(chart)) {
            return false;
        }

        chart.addIndicator(indicators.getIndicator("SMA"), new Object[]{smaTimePeriod},
                new Color[]{Color.BLUE}, new DrawingStyle[]{DrawingStyle.LINE}, new int[]{3});

        if (addOHLC) {
            IOhlcChartObject ohlc = null;
            for (IChartObject obj : chart.getAll()) {
                if (obj instanceof IOhlcChartObject) {
                    ohlc = (IOhlcChartObject) obj;
                }
            }
            if (ohlc == null) {
                ohlc = chart.getChartObjectFactory().createOhlcInformer();
                ohlc.setPreferredSize(new Dimension(100, 200));
                chart.addToMainChart(ohlc);
            }
            //show the ohlc index
            ohlc.setShowIndicatorInfo(true);
        }
        return true;
    }//end of addToChart method

    private boolean checkChart(IChart chart) {
        if (chart == null) {
            printMeError("chart for " + myInstrument + " not opened!");
            return false;
        }
        if (chart.getSelectedOfferSide() != this.myOfferSide) {
            printMeError("chart offer side is not " + this.myOfferSide);
            return false;
        }
        if (chart.getSelectedPeriod() != this.myPeriod) {
            printMeError("chart period is not " + this.myPeriod);
            return false;
        }
        if (chart.getFilter() != this.filter) {
            printMeError("chart filter is not " + this.filter);
            return false;
        }
        return true;
    }

    private void addBreakToChart(IOrder changedOrder, ITick tick, double oldSL, double newSL) throws JFException {
        if (openedChart == null) {
            return;
        }

        ITriangleChartObject orderSLTriangle = factory.createTriangle("Triangle " + shortLineCounter++,
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
        double textVerticalPosition = oldSL > newSL ? newSL - myInstrument.getPipValue() : newSL + myInstrument.getPipValue();
        ITextChartObject textOldSL = factory.createText("textKey1" + textCounterOldSL++, tick.getTime(), oldSL);
        ITextChartObject textNewSL = factory.createText("textKey2" + textCounterNewSL++, tick.getTime(), newSL);
        textOldSL.setText(breakTextOldSL);
        textNewSL.setText(breakTextNewSL);
        textOldSL.setStickToCandleTimeEnabled(false);
        textNewSL.setStickToCandleTimeEnabled(false);
        openedChart.addToMainChart(textOldSL);
        openedChart.addToMainChart(textNewSL);
    }
}