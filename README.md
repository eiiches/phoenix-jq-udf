phoenix-jq-udf
==============

jq for Apache Phoenix

Installation
------------

0. To use UDFs, you have to add the following property to `hbase-site.xml` on both client and server.

   ```xml
   <property>
    <name>phoenix.functions.allowUserDefinedFunctions</name>
    <value>true</value>
   </property>
   ```

1. Build a UDF jar and copy it into your `${hbase.dynamic.jars.dir}`.

   ```sh
   mvn clean package
   # adjust /hbase/lib to your ${hbase.dynamic.jars.dir}
   sudo -u hbase hadoop fs -copyFromLocal target/phoenix-jq-udf-0.0.1.jar /hbase/lib
   ```

2. Run CREATE FUNCTION.

   ```sql
   CREATE FUNCTION jq(VARCHAR, VARCHAR CONSTANT, BOOLEAN CONSTANT DEFAULTVALUE='FALSE') RETURNS VARCHAR AS 'net.thisptr.phoenix.udf.jsonquery.JsonQueryFunction';
   ```

Refer to [User-defined functions (UDFs)](https://phoenix.apache.org/udf.html) official documentation for general information about UDFs.

Usage
-----

```sql
jq(JSON, JQ, RAW=FALSE)
```

### Examples

1. Each UPSERT statement adds an element to set of unique integers represented by a JSON array.

   ```sql
   > CREATE TABLE foo (id INTEGER NOT NULL, val VARCHAR, CONSTRAINT pk PRIMARY KEY (id));
   > UPSERT INTO foo (id, val) VALUES (1, '[1]') ON DUPLICATE KEY UPDATE val = jq(val, '. + [1] | unique_by(.)');
   > UPSERT INTO foo (id, val) VALUES (1, '[1]') ON DUPLICATE KEY UPDATE val = jq(val, '. + [1] | unique_by(.)');
   > UPSERT INTO foo (id, val) VALUES (1, '[2]') ON DUPLICATE KEY UPDATE val = jq(val, '. + [2] | unique_by(.)');
   ```

   ```sql
   > SELECT * FROM foo;
   +-----+--------+
   | ID  |  VAL   |
   +-----+--------+
   | 1   | [1,2]  |
   +-----+--------+
   ```

2. When `RAW` is set to `TRUE`, a raw string value is returned instead of its JSON representation (i.e. no quotes, no escapes).

   ```sql
   > CREATE TABLE foo (id INTEGER NOT NULL, val VARCHAR, CONSTRAINT pk PRIMARY KEY (id));
   > UPSERT INTO foo (id, val) VALUES (1, "\"foo\"");
   ```
   ```sql
   > SELECT id, jq(val, '.', TRUE) FROM foo;
   +-----+----------------------+
   | ID  | JQ(VAL, '.', true)   |
   +-----+----------------------+
   | 1   | foo                  |
   +-----+----------------------+
   ```

   ```sql
   > SELECT id, jq(val, '.') FROM foo;
   +-----+---------------+
   | ID  | JQ(VAL, '.')  |
   +-----+---------------+
   | 1   | "foo"         |
   +-----+---------------+
   ```

License
-------

[The Apache License, Version 2.0](LICENSE)
