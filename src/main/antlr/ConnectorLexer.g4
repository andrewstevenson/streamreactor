lexer grammar ConnectorLexer;
@ header {
 }

INSERT
   : 'insert' | 'INSERT'
   ;

UPSERT
   : 'upsert' | 'UPSERT'
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

PARTITIONBY
   : 'partitionby' | 'PARTITIONBY'
   ;

DISTRIBUTEBY
   : 'distributeby' | 'DISTRIBUTEBY'
   ;

TIMESTAMP
    : 'withtimestamp' | 'WITHTIMESTAMP'
    ;

STOREDAS
    : 'storedas' | 'STOREDAS'
    ;

SYS_TIME
    : 'sys_time()' | 'SYS_TIME()'
    ;

WITHGROUP
    : 'withgroup' | 'WITHGROUP'
    ;

FROMOFFSET
    :  'fromoffset' | 'FROMOFFSET'
    ;

LATEST
    : 'latest' | 'LATEST'
    ;

EARLIEST
    : 'earliest'|'EARLIEST'
    ;

PK
   : 'pk' | 'PK'
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


ID
   : ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )+
   ;


TOPICNAME
   : ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9'| '.' | '-' | '+' )+
   ;


NEWLINE
   : '\r'? '\n' -> skip
   ;


WS
   : ( ' ' | '\t' | '\n' | '\r' )+ -> skip
   ;


EQUAL
   : '='
   ;
