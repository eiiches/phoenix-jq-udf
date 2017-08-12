package net.thisptr.phoenix.udf.jsonquery;

import java.io.DataInput;
import java.io.IOException;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.List;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.phoenix.expression.Expression;
import org.apache.phoenix.expression.function.ScalarFunction;
import org.apache.phoenix.parse.FunctionParseNode.Argument;
import org.apache.phoenix.parse.FunctionParseNode.BuiltInFunction;
import org.apache.phoenix.schema.tuple.Tuple;
import org.apache.phoenix.schema.types.PBoolean;
import org.apache.phoenix.schema.types.PDataType;
import org.apache.phoenix.schema.types.PVarchar;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;

@BuiltInFunction(name = JsonQueryFunction.NAME, args = {
		@Argument(allowedTypes = { PVarchar.class }), // json
		@Argument(allowedTypes = { PVarchar.class }, isConstant = true), // jq
		@Argument(allowedTypes = { PBoolean.class }, isConstant = true, defaultValue = "FALSE") // raw
})
public class JsonQueryFunction extends ScalarFunction {
	public static final String NAME = "JQ";
	private final ObjectMapper MAPPER = new ObjectMapper();

	static {
		// Force initializing Scope object when JsonQueryFunction is loaded using the Scope classloader. Otherwise,
		// built-in jq functions are not loaded.
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader((URLClassLoader) Scope.class.getClassLoader());
		try {
			Scope.rootScope();
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	private boolean raw;
	private JsonQuery jq;

	public JsonQueryFunction() {}

	public JsonQueryFunction(final List<Expression> children) throws SQLException {
		super(children);
		init();
	}

	private void init() {
		final ImmutableBytesWritable raw = new ImmutableBytesWritable();
		if (!getChildren().get(2).evaluate(null, raw))
			throw new RuntimeException("jq: the 3rd argument must be a constant boolean.");
		this.raw = (Boolean) PBoolean.INSTANCE.toObject(raw);

		final ImmutableBytesWritable jq = new ImmutableBytesWritable();
		if (!getChildren().get(1).evaluate(null, jq))
			throw new RuntimeException("jq: the 2nd argument must be a constant varchar.");
		try {
			this.jq = JsonQuery.compile((String) PVarchar.INSTANCE.toObject(jq));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean evaluate(final Tuple tuple, final ImmutableBytesWritable ptr) {
		final Expression inArg = getChildren().get(0);
		final ImmutableBytesWritable in = new ImmutableBytesWritable();
		if (!inArg.evaluate(tuple, in))
			return false;

		try {
			final JsonParser p = MAPPER.getFactory().createParser(in.get(), in.getOffset(), in.getLength());
			final JsonNode tree = p.readValueAsTree();

			final List<JsonNode> values = jq.apply(tree);
			if (values.size() != 1)
				throw new RuntimeException("must return 1 value");
			final JsonNode value = values.get(0);

			ptr.set(PVarchar.INSTANCE.toBytes(raw && value.isTextual() ? value.asText() : value.toString()));
			return true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void readFields(final DataInput input) throws IOException {
		super.readFields(input);
		init();
	}

	@Override
	public PDataType<?> getDataType() {
		return PVarchar.INSTANCE;
	}

	@Override
	public String getName() {
		return NAME;
	}
}
