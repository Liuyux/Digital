package de.neemann.digital.analyse.quinemc;

import de.neemann.digital.analyse.expression.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * If the result does not depend on a certain variable, this variable is removed.
 * <p>
 * Created by hneemann on 12.03.17.
 */
public class TableReducer {

    private List<Variable> vars;
    private BoolTable table;

    /**
     * Creates a new instance
     *
     * @param vars  the variable
     * @param table the bool table
     */
    public TableReducer(List<Variable> vars, BoolTable table) {
        this.vars = new ArrayList<>();
        this.vars.addAll(vars);
        this.table = table;
    }

    /**
     * Returns true if there are reduce variables
     *
     * @return true is reduction was possible
     */
    public boolean canReduce() {
        boolean isReduced = false;
        Iterator<Variable> it = vars.iterator();
        int var = 0;
        while (it.hasNext()) {
            it.next();
            IndependentChecker ic = new IndependentChecker(table);
            if (ic.isIndependentFrom(var)) {
                it.remove();
                table = ic.removeVar(var);
                isReduced = true;
            } else {
                var++;
            }
        }
        return isReduced;
    }

    /**
     * @return the remaining variables
     */
    public List<Variable> getVars() {
        return vars;
    }

    /**
     * @return the reduced table
     */
    public BoolTable getTable() {
        return table;
    }
}