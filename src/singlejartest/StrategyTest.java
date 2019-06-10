package singlejartest;

import java.util.*;

import com.dukascopy.api.*;

import com.dukascopy.api.IEngine.OrderCommand;

import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.IOhlcChartObject;
import com.dukascopy.api.indicators.OutputParameterInfo.DrawingStyle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class StrategyTest implements IStrategy {
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

    private static final int PREV = 1;
    private static final int SECOND_TO_LAST = 0;

    private IBar previousBar;
    private ITick myLastTick;

    private IOrder order;

    private IChart openedChart;
    private IChartObjectFactory factory;
    private int signals;

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
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
//        console.getOut().println(instrument+" "+period.name());

        if (!instrument.equals(myInstrument) || !period.equals(myPeriod)) {
            return; //quit
        }


        OrderCommand myCommand = null;
        int candlesBefore = 2, candlesAfter = 0;
        //get SMA values of 2nd-to last and last (two last completed) bars
        previousBar = myOfferSide == OfferSide.ASK ? askBar : bidBar;
        long currBarTime = previousBar.getTime();
        double sma[] = indicators.sma(instrument, period, myOfferSide, IIndicators.AppliedPrice.CLOSE,
                smaTimePeriod, Filter.ALL_FLATS, candlesBefore, currBarTime, candlesAfter);

//        for(int i = 0; i < sma.length; i++) {
//            console.getOut().println(i+ "- SMA: "+sma[i]);
//        }


//        previousBar = myOfferSide == OfferSide.ASK ? askBar : bidBar;
//        console.getOut().println(" || PreviousBar- --> " + previousBar + " || Period- --> " + period + " || Instrument- --> " + instrument);

        /*print some message so we can later compare the results with a chart,
         *If SMA is up-trend (green line in sample picture) execute BUY order.
         *If SMA is down-trend (red line in sample picture) execute SELL order.
         */
        printMe(String.format("Bar SMA Values: Second-to-last = %.5f; Last Completed = %.5f", sma[SECOND_TO_LAST], sma[PREV]));
        if(sma[PREV] > sma[SECOND_TO_LAST]){
            printMe("SMA in up-trend"); //indicator goes up
            myCommand = OrderCommand.BUY;
        } else if(sma[PREV] < sma[SECOND_TO_LAST]){
            printMe("SMA in down-trend"); //indicator goes down
            myCommand = OrderCommand.SELL;
        } else {
            return;
        }

        /*check if the order already exists. If exists, then check if the processing order's command is the same as myCommand.
         * If it is the same, then do nothing (let the order stay in open state and continues to processing).
         * If the order command is different (SMA trend changes direction) from the current order's command,
         * then close the opened order and create a new one:
         */
        order = engine.getOrder("MyStrategyOrder");
        if(order != null && engine.getOrders().contains(order) && order.getOrderCommand() != myCommand){
            order.close();
            order.waitForUpdate(IOrder.State.CLOSED); //wait till the order is closed
            console.getOut().println("Order " + order.getLabel() + " is closed");
        }
        //if the order is new or there is no order with the same label, then create a new order:
        if(order == null || !engine.getOrders().contains(order)){
            engine.submitOrder("MyStrategyOrder", instrument, myCommand, 0.1);
        }

        //7.we will draw an arrow when makeing an order.
        //So we need a time of the current bar - to get it, we add one period of a bar to a time of the last completed bar (previousBar).
        long time = previousBar.getTime() + myPeriod.getInterval();
        //8.creating a IChartObject - an up-array or down-array and add a text label to the array.
        //finally, add a IChartObject(array in this case) to the chart.
        if(this.openedChart != null) {
            IChartObject signal = myCommand.isLong() ?
                    factory.createSignalUp("signalUpKey" + signals++, time, previousBar.getLow() - instrument.getPipValue() * 2)
                    : factory.createSignalDown("signalDownKey" + signals++, time, previousBar.getHigh() + instrument.getPipValue() * 2);
            signal.setText(String.format("delta SMA %+.7f", sma[PREV] - sma[SECOND_TO_LAST]), new Font("Monospaced", Font.BOLD, 12));
            openedChart.addToMainChart(signal);
        }
    }

    private boolean addToChart(IChart chart){
        if(chart == null){
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
        if(chart.getFilter() != this.filter){
            printMeError("chart filter is not " + this.filter);
            return false;
        }

        chart.addIndicator(indicators.getIndicator("SMA"), new Object[] {smaTimePeriod},
                new Color[]{Color.GREEN}, new DrawingStyle[]{DrawingStyle.LINE}, new int[]{3});

        //if user has choosed to show the OHLC values, then add them to the chart:
        if(addOHLC){
            //If finds an existing ohlc object, then assign this object to the ohlc ref. variable
            IOhlcChartObject ohlc = null;
            for (IChartObject obj : chart.getAll()) {
                if (obj instanceof IOhlcChartObject) {
                    ohlc = (IOhlcChartObject) obj;
                }
            }
            //if cannot find existing ohlc object, then create new one and assign to ohlc ref. variable
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
}