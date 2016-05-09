package de.neemann.digital.draw.builder;

import de.neemann.digital.analyse.expression.*;
import de.neemann.digital.core.basic.And;
import de.neemann.digital.core.basic.Or;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.element.Rotation;
import de.neemann.digital.core.io.In;
import de.neemann.digital.core.io.Out;
import de.neemann.digital.draw.elements.Circuit;
import de.neemann.digital.draw.elements.VisualElement;
import de.neemann.digital.draw.elements.Wire;
import de.neemann.digital.draw.graphics.Vector;
import de.neemann.digital.draw.library.ElementLibrary;
import de.neemann.digital.draw.shapes.ShapeFactory;
import de.neemann.digital.gui.Main;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

import static de.neemann.digital.draw.shapes.GenericShape.SIZE;

/**
 * Builder to create a circuit from an expression
 *
 * @author hneemann
 */
public class Builder {

    private final Circuit circuit;
    private final VariableVisitor variableVisitor;
    private final ShapeFactory shapeFactory;
    private int pos;
    private ArrayList<FragmentVariable> fragmentVariables;

    /**
     * Creates a new builder
     *
     * @param shapeFactory ShapeFactory used ti set to the created VisualElements
     */
    public Builder(ShapeFactory shapeFactory) {
        this.shapeFactory = shapeFactory;
        circuit = new Circuit();
        variableVisitor = new VariableVisitor();
        fragmentVariables = new ArrayList<>();
    }

    /**
     * Adds an expression to the circuit
     *
     * @param name       the output name
     * @param expression the expression
     * @return this for chained calls
     */
    public Builder addExpression(String name, Expression expression) {
        Fragment fr = createFragment(expression);

        fr = new FragmentExpression(fr, new FragmentVisualElement(Out.DESCRIPTION, shapeFactory).setAttr(Keys.LABEL, name));

        fr.setPos(new Vector(0, 0));
        Box b = fr.doLayout();

        fr.addToCircuit(new Vector(0, pos), circuit);
        pos += b.getHeight() + SIZE;

        expression.traverse(variableVisitor);

        return this;
    }

    private Fragment createFragment(Expression expression) {
        if (expression instanceof Operation) {
            Operation op = (Operation) expression;
            ArrayList<Fragment> frags = new ArrayList<>();
            for (Expression exp : op.getExpressions())
                frags.add(createFragment(exp));

            if (op instanceof Operation.And)
                return new FragmentExpression(frags, new FragmentVisualElement(And.DESCRIPTION, frags.size(), shapeFactory));
            else if (op instanceof Operation.Or)
                return new FragmentExpression(frags, new FragmentVisualElement(Or.DESCRIPTION, frags.size(), shapeFactory));
            else
                throw new RuntimeException("nyi");
        } else if (expression instanceof Not) {
            Not n = (Not) expression;
            if (n.getExpression() instanceof Variable) {
                FragmentVariable fragmentVariable = new FragmentVariable((Variable) n.getExpression(), true);
                fragmentVariables.add(fragmentVariable);
                return fragmentVariable;
            } else
                return new FragmentExpression(createFragment(n.getExpression()), new FragmentVisualElement(de.neemann.digital.core.basic.Not.DESCRIPTION, shapeFactory));
        } else if (expression instanceof Variable) {
            FragmentVariable fragmentVariable = new FragmentVariable((Variable) expression, false);
            fragmentVariables.add(fragmentVariable);
            return fragmentVariable;
        } else
            throw new RuntimeException("nyi");
    }

    private void createInputBus() {
        HashMap<String, Integer> varPos = new HashMap<>();
        int dx = -variableVisitor.getVariables().size() * SIZE * 2;
        for (Variable v : variableVisitor.getVariables()) {
            VisualElement visualElement = new VisualElement(In.DESCRIPTION.getName()).setShapeFactory(shapeFactory);
            visualElement.getElementAttributes()
                    .set(Keys.ROTATE, new Rotation(3))
                    .set(Keys.LABEL, v.getIdentifier());
            visualElement.setPos(new Vector(dx, -SIZE * 5));
            circuit.add(visualElement);

            visualElement = new VisualElement(de.neemann.digital.core.basic.Not.DESCRIPTION.getName()).setShapeFactory(shapeFactory);
            visualElement.getElementAttributes()
                    .set(Keys.ROTATE, new Rotation(3));
            visualElement.setPos(new Vector(dx + SIZE, -SIZE * 3));
            circuit.add(visualElement);

            circuit.add(new Wire(new Vector(dx, -SIZE * 4), new Vector(dx + SIZE, -SIZE * 4)));
            circuit.add(new Wire(new Vector(dx + SIZE, -SIZE * 3), new Vector(dx + SIZE, -SIZE * 4)));

            circuit.add(new Wire(new Vector(dx, -SIZE * 5), new Vector(dx, pos)));
            circuit.add(new Wire(new Vector(dx + SIZE, -SIZE), new Vector(dx + SIZE, pos)));

            varPos.put(v.getIdentifier(), dx);
            dx += SIZE * 2;
        }

        for (FragmentVariable f : fragmentVariables) {
            Vector p = f.getCircuitPos();
            int in = varPos.get(f.getVariable().getIdentifier());
            if (f.isNeg()) in += SIZE;
            circuit.add(new Wire(p, new Vector(in, p.y)));
        }
    }

    /**
     * Creates the circuit
     *
     * @return the circuit
     */
    public Circuit createCircuit() {
        createInputBus();
        return circuit;
    }


    public static void main(String[] args) {
        Variable a = new Variable("A");
        Variable b = new Variable("B");
        Variable c = new Variable("C");
        Expression y = Operation.or(Not.not(Operation.and(a, Not.not(b), c)), Operation.and(Not.not(a), c), Operation.and(b, Not.not(c)));
        Expression y1 = Operation.or(Not.not(Operation.and(a, Not.not(b), c)), Operation.and(Not.not(a), c), Operation.and(b, Not.not(c)), Operation.and(b, Not.not(c)));

        Expression l = Operation.and(y, y1, a);

        Builder builder = new Builder(new ShapeFactory(new ElementLibrary()));

        Circuit circuit = builder
                .addExpression("L", l)
                .addExpression("Y", y)
                .createCircuit();
        SwingUtilities.invokeLater(() -> new Main(null, circuit).setVisible(true));
    }

}
