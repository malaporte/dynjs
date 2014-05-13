/**
 *  Copyright 2013 Douglas Campos, and individual contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.dynjs.parser.ast;

import org.dynjs.parser.CodeVisitor;
import org.dynjs.parser.Statement;
import org.dynjs.parser.js.Position;
import org.dynjs.runtime.*;
import org.dynjs.runtime.linker.DynJSBootstrapper;

import java.lang.invoke.CallSite;
import java.util.List;

public class ForExprInStatement extends AbstractForInStatement {

    private final Expression expr;
    private final CallSite rhsGet = DynJSBootstrapper.factory().createGet();

    public ForExprInStatement(Position position, final Expression expr, final Expression rhs, final Statement block) {
        super(position, rhs, block);
        this.expr = expr;
    }

    public Expression getExpr() {
        return this.expr;
    }

    public String toIndentedString(String indent) {
        StringBuilder buf = new StringBuilder();
        buf.append(indent).append("for (").append(expr.toString()).append(" in ").append(getRhs().toString()).append(") {\n");
        buf.append(getBlock().toIndentedString(indent + "  "));
        buf.append(indent).append("}");
        return buf.toString();
    }

    public Object accept(Object context, CodeVisitor visitor, boolean strict) {
        return visitor.visit( context, this, strict );
    }

    public int getSizeMetric() {
        int size = super.getSizeMetric();

        if (this.expr != null) {
            size += this.expr.getSizeMetric();
        }

        return size;
    }

    @Override
    public Completion interpret(ExecutionContext context) {
        Object exprRef = getRhs().interpret(context);
        Object exprValue = getValue(this.rhsGet, context, exprRef);

        if (exprValue == Types.NULL || exprValue == Types.UNDEFINED) {
            return (Completion.createNormal());

        }

        JSObject obj = Types.toObject(context, exprValue);

        Object v = null;

        List<String> names = obj.getAllEnumerablePropertyNames().toList();

        for (String each : names) {

            Object lhsRef = getExpr().interpret(context);

            if (lhsRef instanceof Reference) {
                ((Reference) lhsRef).putValue(context, each);
            }


            Completion completion = (Completion) getBlock().interpret(context);

            if (completion.value != null) {
                v = completion.value;
            }

            if (completion.type == Completion.Type.BREAK) {
                if (completion.target == null || getLabels().contains(completion.target)) {
                    return (Completion.createNormal(v));
                } else {
                    return (completion);
                }

            }

            if (completion.type == Completion.Type.RETURN || completion.type == Completion.Type.BREAK) {
                return (completion);

            }
        }
        return (Completion.createNormal(v));

    }
}
