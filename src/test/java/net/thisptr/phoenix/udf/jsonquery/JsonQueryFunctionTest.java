package net.thisptr.phoenix.udf.jsonquery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.phoenix.expression.Expression;
import org.apache.phoenix.expression.LiteralExpression;
import org.apache.phoenix.schema.types.PBoolean;
import org.apache.phoenix.schema.types.PVarchar;
import org.junit.Test;

public class JsonQueryFunctionTest {
	private static final String evaluate(final Expression expr) {
		final ImmutableBytesWritable ptr = new ImmutableBytesWritable();
		assertTrue(expr.evaluate(null, ptr));
		return (String) expr.getDataType().toObject(ptr);
	}

	@Test
	public void testSimpleNumber() throws Exception {
		final LiteralExpression json = LiteralExpression.newConstant("1", PVarchar.INSTANCE);
		final LiteralExpression jq = LiteralExpression.newConstant(". + 1", PVarchar.INSTANCE);
		final LiteralExpression raw = LiteralExpression.newConstant(false, PBoolean.INSTANCE);
		assertEquals("2", evaluate(new JsonQueryFunction(Arrays.asList(json, jq, raw))));
	}

	@Test
	public void testSimpleString() throws Exception {
		final LiteralExpression json = LiteralExpression.newConstant("\"foo\"", PVarchar.INSTANCE);
		final LiteralExpression jq = LiteralExpression.newConstant(".", PVarchar.INSTANCE);
		final LiteralExpression raw = LiteralExpression.newConstant(false, PBoolean.INSTANCE);
		assertEquals("\"foo\"", evaluate(new JsonQueryFunction(Arrays.asList(json, jq, raw))));
	}

	@Test
	public void testRawString() throws Exception {
		final LiteralExpression json = LiteralExpression.newConstant("\"foo\"", PVarchar.INSTANCE);
		final LiteralExpression jq = LiteralExpression.newConstant(".", PVarchar.INSTANCE);
		final LiteralExpression raw = LiteralExpression.newConstant(true, PBoolean.INSTANCE);
		assertEquals("foo", evaluate(new JsonQueryFunction(Arrays.asList(json, jq, raw))));
	}
}
