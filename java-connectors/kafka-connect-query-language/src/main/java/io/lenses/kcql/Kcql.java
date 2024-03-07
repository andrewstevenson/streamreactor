/*
 * Copyright 2017-2024 Lenses.io Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lenses.kcql;

import io.lenses.kcql.antlr4.ConnectorLexer;
import io.lenses.kcql.antlr4.ConnectorParser;
import io.lenses.kcql.antlr4.ConnectorParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Parsing support for Kafka Connect Query Language.
 */
public class Kcql {

    public static final String TIMESTAMP = "sys_time()";
    private static final String MSG_ILLEGAL_FIELD_ALIAS = "Illegal fieldAlias.";
    private String query;
    private boolean autoCreate;
    private boolean autoEvolve;
    private WriteModeEnum writeMode;
    private String source;
    private String target;
    private String docType;
    private String indexSuffix;
    private String incrementalMode;
    private final List<Field> fields = new ArrayList<>();
    private final List<Field> keyFields = new ArrayList<>();
    private final List<Field> headerFields = new ArrayList<>();
    private final List<Field> ignoredFields = new ArrayList<>();
    private final List<Field> primaryKeys = new ArrayList<>();
    private final List<String> partitionBy = new ArrayList<>();
    private int limit = 0;
    private int batchSize;
    private String timestamp;
    private String storedAs;
    private final Map<String, String> storedAsParameters = new HashMap<>();
    private FormatType formatType = null;
    private boolean unwrapping = false;
    private List<Tag> tags;
    private boolean retainStructure = false;
    private String withConverter;
    private long ttl;
    private String withType;
    private String withJmsSelector;
    private String dynamicTarget;
    private List<String> withKeys = null;
    private String keyDelimiter = ".";
    private TimeUnit timestampUnit = TimeUnit.MILLISECONDS;
    private String pipeline;
    private String subscription;
    private String partitioner;
    private String withRegex;
    private long withFlushInterval;
    private long withFlushSize;
    private long withFlushCount;

    private final Map<String, String> properties = new HashMap<>();

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getWithPartitioner() {
        return this.partitioner;
    }

    public void setWithPartitioner(String name) {
        this.partitioner = name;
    }

    public String getWithSubscription() {
        return this.subscription;
    }

    public void SetWithSubscription(String name) {
        this.subscription = name;
    }

    public void setTTL(long ttl) {
        this.ttl = ttl;
    }

    public long getTTL() {
        return this.ttl;
    }

    private void addField(final Field field) {
        if (field == null) {
            throw new IllegalArgumentException(MSG_ILLEGAL_FIELD_ALIAS);
        }
        if (fieldExists(field)) {
            throw new IllegalArgumentException(String.format("Field %s has already been defined", field.getName()));
        }
        fields.add(field);
    }


    private void addKeyField(final Field field) {
        if (field == null) {
            throw new IllegalArgumentException(MSG_ILLEGAL_FIELD_ALIAS);
        }
        if (fieldExists(field)) {
            throw new IllegalArgumentException(String.format("Key field %s has already been defined", field.getName()));
        }
        keyFields.add(field);
    }

    private void addHeaderField(final Field field) {
        if (field == null) {
            throw new IllegalArgumentException(MSG_ILLEGAL_FIELD_ALIAS);
        }
        if (fieldExists(field)) {
            throw new IllegalArgumentException(String.format("Header field %s has already been defined", field.getName()));
        }
        headerFields.add(field);
    }


    private boolean fieldExists(final Field newField) {
        for (Field field : fields) {
            if (!field.getName().equals(newField.getName()) ||
                    !field.getFieldType().equals(newField.getFieldType())) {
                continue;
            }
            if (!field.hasParents() && !newField.hasParents()) {
                return true;
            }
            if (field.hasParents() && newField.hasParents()) {
                if (field.getParentFields().equals(newField.hasParents())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addPartitionByField(final String field) {
        if (field == null || field.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid partition by field");
        }
        for (final String f : partitionBy) {
            if (f.compareToIgnoreCase(field.trim()) == 0) {
                throw new IllegalArgumentException(String.format("The field %s appears twice", field));
            }
        }
        partitionBy.add(field.trim());
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public List<Field> getFields() {
        return fields;
    }

    public List<Field> getKeyFields() {
        return keyFields;
    }

    public List<Field> getHeaderFields() {
        return headerFields;
    }

    public List<Field> getIgnoredFields() {
        return ignoredFields;
    }

    public WriteModeEnum getWriteMode() {
        return writeMode;
    }

    public List<Field> getPrimaryKeys() {
        return primaryKeys;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getStoredAs() {
        return storedAs;
    }

    public Map<String, String> getStoredAsParameters() {
        return storedAsParameters;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public FormatType getFormatType() {
        return formatType;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isAutoEvolve() {
        return autoEvolve;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public Iterator<String> getPartitionBy() {
        return partitionBy.iterator();
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<String> getWithKeys() {
        return withKeys;
    }

    public String getKeyDelimeter() {
        return keyDelimiter;
    }

    public boolean hasRetainStructure() {
        return retainStructure;
    }

    public boolean isUnwrapping() {
        return unwrapping;
    }

    public String getWithType() {
        return this.withType;
    }

    public String getIncrementalMode() {
        return this.incrementalMode;
    }

    public String getDocType() {
        return this.docType;
    }

    public String getIndexSuffix() {
        return this.indexSuffix;
    }

    public String getWithConverter() {
        return withConverter;
    }

    public String getWithJmsSelector() {
        return withJmsSelector;
    }

    public String getPipeline() {
        return pipeline;
    }

    public String getWithRegex() {
        return withRegex;
    }

    private void setWithRegex(String withRegex) {
        this.withRegex = withRegex;
    }

    public long getWithFlushInterval() {
        return withFlushInterval;
    }

    public long getWithFlushSize() {
        return withFlushSize;
    }

    private void setWithFlushCount(long withFlushCount) {
        this.withFlushCount = withFlushCount;
    }

    public long getWithFlushCount() {
        return withFlushCount;
    }

    private void setDynamicTarget(String dynamicTarget) {
        this.dynamicTarget = dynamicTarget;
    }

    public String getDynamicTarget() {
        return dynamicTarget;
    }

    public TimeUnit getTimestampUnit() {
        return timestampUnit;
    }

    private void setTimestampUnit(TimeUnit timestampUnit) {
        this.timestampUnit = timestampUnit;
    }

    private void setWithFlushInterval(long withFlushInterval) {
        this.withFlushInterval = withFlushInterval;
    }

    private void setWithFlushSize(long withFlushSize) {
        this.withFlushSize = withFlushSize;
    }

    public static Kcql parse(final String syntax) {
        final ConnectorLexer lexer = new ConnectorLexer(CharStreams.fromString(syntax));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final ConnectorParser parser = new ConnectorParser(tokens);
        final ArrayList<String> nestedFieldsBuffer = new ArrayList<>();
        final Kcql kcql = new Kcql();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });

        final String[] storedAsParameter = {null};

        final boolean[] isWithinIgnore = {false};

        final String[] tagValue = {null};
        final String[] tagKey = {null};

        parser.addParseListener(new ConnectorParserBaseListener() {

            @Override
            public void exitWith_subscription_value(ConnectorParser.With_subscription_valueContext ctx) {
                kcql.subscription = unescape(ctx.getText());
            }

            @Override
            public void exitWith_partitioner_value(ConnectorParser.With_partitioner_valueContext ctx) {
                kcql.partitioner = unescape(ctx.getText());
            }

            @Override
            public void exitColumn(ConnectorParser.ColumnContext ctx) {
                for (TerminalNode tn : ctx.FIELD()) {
                    nestedFieldsBuffer.add(tn.getText());
                }
                if (ctx.ASTERISK() != null) {
                    nestedFieldsBuffer.add("*");
                }
            }

            @Override
            public void exitWith_unwrap_clause(ConnectorParser.With_unwrap_clauseContext ctx) {
                kcql.unwrapping = true;
            }

            @Override
            public void exitWith_type_value(ConnectorParser.With_type_valueContext ctx) {
                kcql.withType = unescape(ctx.getText());
            }

            @Override
            public void exitWith_structure(ConnectorParser.With_structureContext ctx) {
                kcql.retainStructure = true;
            }

            @Override
            public void exitLimit_value(ConnectorParser.Limit_valueContext ctx) {
                try {
                    int limit = Integer.parseInt(ctx.INT().getText());
                    if (limit < 1)
                        throw new IllegalArgumentException("Invalid limit specified. Needs to be an integer greater than zero");
                    kcql.limit = limit;
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Invalid limit specified(" + ctx.INT().getText() + "). Needs to be an integer greater than zero");
                }
            }

            @Override
            public void enterColumn_name(ConnectorParser.Column_nameContext ctx) {
                nestedFieldsBuffer.clear();
            }

            @Override
            public void exitColumn_name(ConnectorParser.Column_nameContext ctx) {
                super.exitColumn_name(ctx);
                if (ctx.ASTERISK() != null) {
                    Field field = new Field("*", FieldType.VALUE, null);
                    kcql.addField(field);
                    return;
                }

                List<String> parentFields = null;
                String name = nestedFieldsBuffer.get(nestedFieldsBuffer.size() - 1);
                nestedFieldsBuffer.remove(nestedFieldsBuffer.size() - 1);

                if (!nestedFieldsBuffer.isEmpty()) {
                    parentFields = nestedFieldsBuffer;
                }

                Field field;
                if (ctx.column_name_alias() != null) {
                    field = Field.from(name, ctx.column_name_alias().getText(), parentFields);
                } else {
                    field = Field.from(name, parentFields);
                }

                if (isWithinIgnore[0]) {
                    kcql.ignoredFields.add(field);
                } else {
                    List<String> cleanedParent = null;

                    if (field.toString().startsWith("_key.")) {
                        trimParentField(nestedFieldsBuffer);
                        if (!nestedFieldsBuffer.isEmpty()) {
                            cleanedParent = nestedFieldsBuffer;
                        }
                        kcql.addKeyField(Field.from(field.getName(), field.getAlias(), cleanedParent));
                    } else if (field.toString().startsWith("_header.")) {
                        trimParentField(nestedFieldsBuffer);
                        if (!nestedFieldsBuffer.isEmpty()) {
                            cleanedParent = nestedFieldsBuffer;
                        }
                        kcql.addHeaderField(Field.from(field.getName(), field.getAlias(), cleanedParent));
                    } else {
                        kcql.addField(field);
                    }
                }
            }

            private void trimParentField(List<String> parents) {
                if (!parents.isEmpty()) {
                    parents.remove(0);
                }
            }

            @Override
            public void exitDoc_type(ConnectorParser.Doc_typeContext ctx) {
                kcql.docType = unescape(ctx.getText());
            }

            @Override
            public void exitWith_converter_value(ConnectorParser.With_converter_valueContext ctx) {
                kcql.withConverter = unescape(ctx.getText());
            }

            @Override
            public void exitJms_selector_value(ConnectorParser.Jms_selector_valueContext ctx) {
                kcql.withJmsSelector = unescape(ctx.getText());
            }

            @Override
            public void exitIndex_suffix(ConnectorParser.Index_suffixContext ctx) {
                kcql.indexSuffix = unescape(ctx.getText());
            }

            @Override
            public void exitInc_mode(ConnectorParser.Inc_modeContext ctx) {
                kcql.incrementalMode = ctx.getText();
            }

            @Override
            public void exitPartition_name(ConnectorParser.Partition_nameContext ctx) {
                kcql.addPartitionByField(ctx.getText());
            }

            @Override
            public void exitTable_name(ConnectorParser.Table_nameContext ctx) {
                kcql.target = unescape(ctx.getText());
            }

            @Override
            public void enterWith_ignore(ConnectorParser.With_ignoreContext ctx) {
                isWithinIgnore[0] = true;
            }

            @Override
            public void exitWith_ignore(ConnectorParser.With_ignoreContext ctx) {
                isWithinIgnore[0] = false;
            }

            @Override
            public void exitTopic_name(ConnectorParser.Topic_nameContext ctx) {
                kcql.source = unescape(ctx.getText());
            }

            @Override
            public void exitUpsert_into(ConnectorParser.Upsert_intoContext ctx) {
                kcql.writeMode = WriteModeEnum.UPSERT;
            }

            @Override
            public void exitInsert_into(ConnectorParser.Insert_intoContext ctx) {
                kcql.writeMode = WriteModeEnum.INSERT;
            }

            @Override
            public void exitUpdate_into(ConnectorParser.Update_intoContext ctx) {
                kcql.writeMode = WriteModeEnum.UPDATE;
            }

            @Override
            public void exitAutocreate(ConnectorParser.AutocreateContext ctx) {
                kcql.autoCreate = true;
            }

            @Override
            public void enterPk_name(ConnectorParser.Pk_nameContext ctx) {
                nestedFieldsBuffer.clear();
            }

            @Override
            public void exitPk_name(ConnectorParser.Pk_nameContext ctx) {
                List<String> parentFields = null;
                String name = nestedFieldsBuffer.get(nestedFieldsBuffer.size() - 1);
                nestedFieldsBuffer.remove(nestedFieldsBuffer.size() - 1);

                if (!nestedFieldsBuffer.isEmpty()) {
                    parentFields = nestedFieldsBuffer;
                }

                Field field = Field.from(name, parentFields);
                kcql.primaryKeys.add(field);
            }

            @Override
            public void exitAutoevolve(ConnectorParser.AutoevolveContext ctx) {
                kcql.autoEvolve = true;
            }


            @Override
            public void exitStoreas_type(ConnectorParser.Storeas_typeContext ctx) {
                kcql.storedAs = ctx.getText().replace("`", "");
            }


            @Override
            public void exitProperty(ConnectorParser.PropertyContext ctx) {
                String key = unescape(ctx.property_name().getText());
                String value = unescape(ctx.property_value().getText());
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                kcql.getProperties().put(key, value);
            }

            @Override
            public void exitStoreas_parameter(ConnectorParser.Storeas_parameterContext ctx) {
                String value = ctx.getText();
                for (String key : kcql.getStoredAsParameters().keySet()) {
                    if (key.compareToIgnoreCase(value) == 0) {
                        throw new IllegalArgumentException(value + " is a duplicated entry in the storeAs parameters list");
                    }
                }
                storedAsParameter[0] = value;
            }

            @Override
            public void exitStoreas_value(ConnectorParser.Storeas_valueContext ctx) {
                kcql.getStoredAsParameters().put(storedAsParameter[0], ctx.getText());
            }

            @Override
            public void exitBatch_size(ConnectorParser.Batch_sizeContext ctx) {
                final String value = ctx.getText();
                try {
                    int newBatchSize = Integer.parseInt(value);
                    if (newBatchSize <= 0) {
                        throw new IllegalArgumentException(value + " is not a valid number for a batch Size.");
                    }
                    kcql.batchSize = newBatchSize;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(value + " is not a valid number for a batch Size.");
                }
            }

            @Override
            public void exitTtl_type(ConnectorParser.Ttl_typeContext ctx) {
                final String value = ctx.getText();
                try {
                    long newTTL = Long.parseLong(value);
                    if (newTTL <= 0) {
                        throw new IllegalArgumentException(value + " is not a valid number for a TTL.");
                    }
                    kcql.setTTL(newTTL);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(value + " is not a valid number for a TTL.");
                }
            }

            @Override
            public void exitTimestamp_value(ConnectorParser.Timestamp_valueContext ctx) {
                kcql.timestamp = ctx.getText();
            }

            @Override
            public void exitTimestamp_unit_value(ConnectorParser.Timestamp_unit_valueContext ctx) {
                String value = ctx.getText().toUpperCase();
                try {
                    kcql.setTimestampUnit(TimeUnit.valueOf(value));
                } catch (Throwable t) {
                    TimeUnit[] units = TimeUnit.values();
                    StringBuilder sb = new StringBuilder();
                    sb.append(units[0].toString());
                    for (int i = 1; i < units.length; ++i) {
                        sb.append(",");
                        sb.append(units[i].toString());
                    }
                    throw new IllegalArgumentException(("Invalid 'TIMESTAMPUNIT'. Available values are : " + sb));
                }
            }

            @Override
            public void exitWith_format(ConnectorParser.With_formatContext ctx) {
                try {
                    kcql.formatType = FormatType.valueOf(ctx.getText().toUpperCase());
                } catch (Throwable t) {
                    FormatType[] types = FormatType.values();
                    StringBuilder sb = new StringBuilder();
                    sb.append(types[0].toString());
                    for (int i = 1; i < types.length; ++i) {
                        sb.append(",");
                        sb.append(types[i].toString());
                    }
                    throw new IllegalArgumentException(("Invalid 'FORMAT'. Available values are : " + sb));
                }
            }

            @Override
            public void exitWith_target_value(ConnectorParser.With_target_valueContext ctx) {
                kcql.setDynamicTarget(ctx.getText());
            }

            @Override
            public void exitTag_value(ConnectorParser.Tag_valueContext ctx) {
                tagValue[0] = ctx.getText();
            }

            @Override
            public void exitTag_key(ConnectorParser.Tag_keyContext ctx) {
                if (ctx.getText().trim().endsWith(".")) {
                    throw new IllegalArgumentException("Invalid syntax for tags. Field selection can not end with '.'");
                }
                tagKey[0] = ctx.getText();
            }

            @Override
            public void exitTag_definition(ConnectorParser.Tag_definitionContext ctx) {
                String txt = ctx.getText();
                Tag.TagType type = Tag.TagType.DEFAULT;
                if (tagValue[0] != null) {
                    String tmp = txt.replace(tagKey[0], "").trim();
                    if (tmp.startsWith("=")) {
                        type = Tag.TagType.CONSTANT;
                    } else if (tmp.toLowerCase().startsWith("as")) {
                        type = Tag.TagType.ALIAS;
                    } else {
                        throw new IllegalArgumentException("Invalid syntax for tags. Needs to be 'tag1 [as x]' or 'tag1' or 'tag1 = constant'");
                    }
                }

                if (kcql.tags == null) kcql.tags = new ArrayList<>();
                kcql.tags.add(new Tag(tagKey[0], tagValue[0], type));
                tagKey[0] = null;
                tagValue[0] = null;
            }

            @Override
            public void exitWith_key_value(ConnectorParser.With_key_valueContext ctx) {
                String key = ctx.getText();
                if (kcql.withKeys == null) {
                    kcql.withKeys = new ArrayList<>();
                }
                kcql.withKeys.add(unescape(key));
            }

            @Override
            public void exitKey_delimiter_value(ConnectorParser.Key_delimiter_valueContext ctx) {
                kcql.keyDelimiter = ctx.getText().replace("`", "").replace("'", "");
                if (kcql.keyDelimiter.trim().length() == 0) {
                    throw new IllegalArgumentException("Invalid key delimiter. Needs to be a non empty string.");
                }
            }

            @Override
            public void exitPipeline_value(ConnectorParser.Pipeline_valueContext ctx) {
                kcql.pipeline = unescape(ctx.getText());
            }

            @Override
            public void exitWith_regex_value(ConnectorParser.With_regex_valueContext ctx) {
                kcql.withRegex = unescape(ctx.getText());
            }


            @Override
            public void exitWith_flush_bytes_value(ConnectorParser.With_flush_bytes_valueContext ctx) {
                try {
                    long size = Long.parseLong(ctx.getText());
                    if (size <= 0) {
                        throw new IllegalArgumentException("Invalid value specified for WITH_FLUSH_SIZE. Expecting a LONG number greater than 0.");
                    }
                    kcql.setWithFlushSize(size);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid value specified for WITH_FLUSH_SIZE. Expecting a LONG number greater than 0.");
                }
            }

            @Override
            public void exitWith_flush_interval_value(ConnectorParser.With_flush_interval_valueContext ctx) {
                try {
                    long interval = Long.parseLong(ctx.getText());
                    if (interval <= 0) {
                        throw new IllegalArgumentException("Invalid value specified for WITH_FLUSH_INTERVAL. Expecting a LONG number greater than 0.");
                    }
                    kcql.setWithFlushInterval(interval);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid value specified for WITH_FLUSH_INTERVAL. Expecting a LONG number greater than 0.");
                }
            }

            @Override
            public void exitWith_flush_records_value(ConnectorParser.With_flush_records_valueContext ctx) {
                try {
                    long interval = Long.parseLong(ctx.getText());
                    if (interval <= 0) {
                        throw new IllegalArgumentException("Invalid value specified for WITH_FLUSH_COUNT. Expecting a LONG number greater than 0.");
                    }
                    kcql.setWithFlushCount(interval);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid value specified for WITH_FLUSH_COUNT. Expecting a LONG number greater than 0.");
                }
            }

        });


        try {
            parser.stat();
        } catch (Throwable ex) {
            throw new IllegalArgumentException("Invalid syntax." + ex.getMessage(), ex);
        }

        final HashSet<String> cols = new HashSet<>();
        for (Field alias : kcql.fields) {
            cols.add(alias.getAlias());
        }


        String ts = kcql.timestamp;
        if (ts != null) {
            if (TIMESTAMP.compareToIgnoreCase(ts) == 0) {
                kcql.timestamp = ts.toLowerCase();
            } else {
                kcql.timestamp = ts;
            }
        }

        return kcql;
    }

    private static String unescape(String value) {
        if (value.startsWith("`") && value.endsWith("`")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
