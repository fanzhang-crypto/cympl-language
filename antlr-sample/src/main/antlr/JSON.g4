grammar JSON;

json: object | array;

object: '{' pairs? '}' ;
pairs: pair (',' pair)* ;
pair: STRING ':' value ;

array: '[' values? ']';
values: value (',' value)*;

value
    : NUM
    | BOOL
    | STRING
    | object
    | array
    | 'null';

STRING: '"' (~["\\])* '"';

NUM
    : '-'? INT '.' INT EXP?
    | '-'? INT '.' INT
    | '-'? INT
    ;

BOOL: 'true' | 'false';
WS: [ \t\r\n] -> skip;

fragment INT: '0' | [1-9][0-9]*;
fragment EXP: [eE]('+'|'-')?INT;
