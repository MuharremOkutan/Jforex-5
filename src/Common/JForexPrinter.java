package Common;

import com.dukascopy.api.IConsole;

public class JForexPrinter {

    IConsole console;

    public JForexPrinter(IConsole console){
        this.console = console;
    }

    public void printMe(Object toPrint) {
        console.getOut().println(toPrint);
    }

    public void printMeError(Object o) {
        console.getErr().println(o);
    }
}
