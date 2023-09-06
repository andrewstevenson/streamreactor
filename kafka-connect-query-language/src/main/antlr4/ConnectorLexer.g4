lexer grammar ConnectorLexer;
@ header {
 }

INSERT
   : 'insert' | 'INSERT'
   ;

UPSERT
   : 'upsert' | 'UPSERT'
   ;

UPDATE
   : 'update' | 'UPDATE'
   ;

INTO
   : 'into' | 'INTO'
   ;

SELECT
   : 'select' | 'SELECT'
   ;


FROM
   : 'from' | 'FROM'
   ;


IGNORE
   : 'ignore' | 'IGNORE'
   ;

AS
   : 'as' | 'AS'
   ;

AUTOCREATE
   : 'autocreate' | 'AUTOCREATE'
   ;


AUTOEVOLVE
   : 'autoevolve' | 'AUTOEVOLVE'
   ;

CLUSTERBY
    : 'clusterby' | 'CLUSTERBY'
    ;

BUCKETS
    : 'buckets'|'BUCKETS'
    ;

BATCH
   : 'batch' | 'BATCH'
   ;

CAPITALIZE
   : 'capitalize' | 'CAPITALIZE'
   ;

INITIALIZE
   : 'initialize' | 'INITIALIZE'
   ;

PARTITIONBY
   : 'partitionby' | 'PARTITIONBY'
   ;

DISTRIBUTEBY
   : 'distributeby' | 'DISTRIBUTEBY'
   ;

TIMESTAMP
    : 'withtimestamp' | 'WITHTIMESTAMP'
    ;

SYS_TIME
    : 'sys_time()' | 'SYS_TIME()'
    ;

WITHGROUP
    : 'withgroup' | 'WITHGROUP'
    ;

WITHOFFSET
    :  'withoffset' | 'WITHOFFSET'
    ;

WITHTAG
    :   'withtag' | 'WITHTAG'
    ;

WITHKEY
    :   'withkey' | 'WITHKEY'
    ;

KEYDELIM
    :  'keydelimiter' | 'KEYDELIMITER'
    ;

WITHSTRUCTURE
    : 'withstructure' | 'WITHSTRUCTURE'
    ;

WITHTYPE
    : 'withtype' | 'WITHTYPE'
    ;

PK
   : 'pk' | 'PK'
   ;

SAMPLE
    : 'sample' | 'SAMPLE'
    ;

EVERY
    : 'every'|'EVERY'
    ;

WITHFORMAT
    : 'WITHFORMAT'|'withformat'
    ;

WITHUNWRAP
    : 'WITHUNWRAP'| 'withunwrap'
    ;



PROJECTTO
    : 'projectTo'|'PROJECTTO'|'projectto'
    ;

STOREAS
    : 'STOREAS'|'storeas'
    ;

LIMIT
    : 'LIMIT' | 'limit'
    ;

INCREMENTALMODE
    : 'INCREMENTALMODE'|'incrementalmode'
    ;

WITHDOCTYPE
    :  'WITHDOCTYPE'|'withdoctype'
    ;

WITHINDEXSUFFIX
    : 'WITHINDEXSUFFIX' | 'withindexsuffix'
    ;

WITHCONVERTER
    : 'WITHCONVERTER' | 'withconverter'
    ;

WITHJMSSELECTOR
    : 'WITHJMSSELECTOR' | 'withjmsselector'
    ;

WITHTARGET
    : 'WITHTARGET' | 'withtarget'
    ;

WITHCOMPRESSION
    : 'WITHCOMPRESSION'|'withcompression'
    ;

WITHPARTITIONER
    : 'WITHPARTITIONER' | 'withpartitioner'
    ;

WITHSUBSCRIPTION
    : 'WITHSUBSCRIPTION' | 'withsubscription'
    ;

TIMESTAMPUNIT
    : 'TIMESTAMPUNIT' | 'timestampunit'
    ;

WITHPIPELINE
    : 'WITHPIPELINE' | 'withpipeline'
    ;

WITHDELAY
    : 'WITHDELAY'|'withdelay'
    ;

WITHREGEX
    : 'WITHREGEX'|'withregex'
    ;

WITH_FLUSH_INTERVAL
    : 'WITH_FLUSH_INTERVAL' | 'with_flush_interval'
    ;

WITH_FLUSH_SIZE
    : 'WITH_FLUSH_SIZE' | 'with_flush_size'
    ;

WITH_FLUSH_COUNT
    : 'WITH_FLUSH_COUNT' | 'with_flush_count'
    ;

WITH_SCHEMA_EVOLUTION
    : 'WITH_SCHEMA_EVOLUTION'|'with_schema_evolution'
    ;

WITH_TABLE_LOCATION
    : 'WITH_TABLE_LOCATION' | 'with_table_location'
    ;

WITH_OVERWRITE
    : 'WITH_OVERWRITE' | 'with_overwrite'
    ;

WITH_PARTITIONING
    : 'WITH_PARTITIONING' | 'with_partitioning'
    ;

WITH_SESSION
    : 'WITH_SESSION' | 'with_session'
    ;

TTL
   : 'TTL'|'ttl'
   ;

 WITH_ACK
    : 'with_ack' | 'WITH_ACK'
    ;

 WITH_ENCODE_BASE64
    : 'with_encode_base64' | 'WITH_ENCODE_BASE64'
    ;

WITH_LOCK_TIME
    : 'with_lock_time' | 'WITH_LOCK_TIME'
    ;

 PROPERTIES
    : 'properties' | 'PROPERTIES'
    ;

EQUAL
   : '='
   ;

INT
   : '0' .. '9'+
   ;

ASTERISK
   : '*'
   ;

COMMA
   : ','
   ;

DOT
   : '.'
   ;

LEFT_PARAN
    : '('
    ;

RIGHT_PARAN
    : ')'
    ;


FIELD
   : ( 'a' .. 'z' | 'A' .. 'Z' | '@' |'_' | '0' .. '9' )+
   ;


TOPICNAME
   : ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' | '-' | '+' | '/' |'{'|'}'|':' )+ | ESCAPED_TOPIC
   ;

KEYDELIMVALUE
   : '\'' ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' | '-' | '+' | '/' |'{'|'}'|':'|'|'|'#'|'@'|'`'|'^'|'['|']'|'*'|'?'|'$') '\''
   ;



fragment ESCAPED_TOPIC
    : ( '`' (~'`')+ '`')
    ;

STRING: '\'' ~('\'' | '\r' | '\n')* '\'';

NEWLINE
   : '\r'? '\n' -> skip
   ;


WS
   : ( ' ' | '\t' | '\n' | '\r' )+ -> skip
   ;

