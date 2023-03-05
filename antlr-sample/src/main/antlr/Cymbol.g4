grammar Cymbol;

prog: (varDecl | funcDecl)+EOF;

varDecl: TYPE ID ('=' expr)? ';';

funcDecl: TYPE ID '(' paramDecls? ')' block;

paramDecls: paramDecl (',' paramDecl)*;

paramDecl: TYPE ID;

stat: block | varDecl | assign | ifStat | whileStat | returnStat | expr ';';

block: '{' stat* '}';

assign: ID '=' expr ';';

ifStat: 'if' '(' expr ')' block ('else' block)?;

whileStat: 'while' '(' expr ')' block;

returnStat: 'return' expr? ';';

expr: ID '(' exprlist? ')'              # FunctionCall
    | '(' expr ')'                      # ParenthesizedExpression
    | <assoc=right> expr '^' expr       # Power
    | expr '*' expr                     # Multiplication
    | expr '/' expr                     # Division
    | expr '+' expr                     # Addition
    | expr '-' expr                     # Substraction
    | ID                                # Variable
    | FLOAT                             # FLOAT
    | INT                               # INT
    | STRING                            # STRING
    ;

exprlist: expr (',' expr)*;

TYPE: INT_TYPE | FLOAT_TYPE | STRING_TYPE | VOID_TYPE;
VOID_TYPE: 'void';
FLOAT_TYPE: 'float';
INT_TYPE: 'int';
STRING_TYPE: 'string';
INT: DIGIT+;
FLOAT: DIGIT '.' DIGIT* | '.' DIGIT+;
STRING: '"' (ESC|.)*? '"';
ID: [a-zA-Z_][a-zA-Z0-9_]*;
COMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: '//'~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment ESC: '\\'[btnr"\\];
