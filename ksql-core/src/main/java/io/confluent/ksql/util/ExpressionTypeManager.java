package io.confluent.ksql.util;

import io.confluent.ksql.parser.tree.*;
import io.confluent.ksql.planner.PlanException;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;

public class ExpressionTypeManager extends DefaultASTVisitor<Expression, ExpressionTypeManager.ExpressionTypeContext> {

    final Schema schema;
    SchemaUtil schemaUtil = new SchemaUtil();

    public ExpressionTypeManager(Schema schema) {
        this.schema = schema;
    }

    public Schema.Type getExpressionType(Expression expression) {
        ExpressionTypeContext expressionTypeContext = new ExpressionTypeContext();
        process(expression, expressionTypeContext);
        return expressionTypeContext.getType();
    }

    class ExpressionTypeContext {
        Schema.Type type;

        public Schema.Type getType() {
            return type;
        }

        public void setType(Schema.Type type) {
            this.type = type;
        }
    }

    @Override
    protected Expression visitArithmeticBinary(ArithmeticBinaryExpression node, ExpressionTypeContext expressionTypeContext)
    {
        process(node.getLeft(), expressionTypeContext);
        Schema.Type leftType = expressionTypeContext.getType();
        process(node.getRight(), expressionTypeContext);
        Schema.Type rightType = expressionTypeContext.getType();
        expressionTypeContext.setType(resolveArithmaticType(leftType, rightType));
        return null;
    }

    @Override
    protected Expression visitComparisonExpression(ComparisonExpression node, ExpressionTypeContext expressionTypeContext)
    {
        expressionTypeContext.setType(Schema.Type.BOOLEAN);
        return null;
    }

    @Override
    protected Expression visitQualifiedNameReference(QualifiedNameReference node, ExpressionTypeContext expressionTypeContext)
    {
        Field schemaField = SchemaUtil.getFieldByName(schema, node.getName().getSuffix());
        expressionTypeContext.setType(schemaField.schema().type());
        return null;
    }

    private Schema.Type resolveArithmaticType(Schema.Type leftType, Schema.Type rightType) {
        if(leftType == rightType) {
            return  leftType;
        } else if((leftType == Schema.Type.STRING) || (rightType == Schema.Type.STRING)) {
            throw new PlanException("Incompatible types.");
        } else if((leftType == Schema.Type.BOOLEAN) || (rightType == Schema.Type.BOOLEAN)) {
            throw new PlanException("Incompatible types.");
        } else if((leftType == Schema.Type.FLOAT64) || (rightType == Schema.Type.FLOAT64)) {
            return Schema.Type.FLOAT64;
        } else if((leftType == Schema.Type.INT64) || (rightType == Schema.Type.INT64)) {
            return Schema.Type.INT64;
        } else if((leftType == Schema.Type.INT32) || (rightType == Schema.Type.INT32)) {
            return Schema.Type.INT32;
        }

        throw new PlanException("Unsupported types.");
    }
}
