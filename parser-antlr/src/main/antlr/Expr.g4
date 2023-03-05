grammar Expr;

prog: (decl | expr | assign)+EOF            #Program
;

decl: ID ':' TYPE '=' expr                  #Declaration
;

assign: ID '=' expr     # Assignment
;

expr: '(' expr ')'      # ParenthesizedExpression
    | <assoc=right> expr '^' expr       # Power
    | expr op=(TIMES | DIV) expr        # MulDiv
    | expr op=(PLUS | MINUS) expr       # AddSub
    | ID                # Variable
    | FLOAT             # FLOAT
    | INT               # INT
    | STRING            # STRING
    ;

PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';

TYPE: FLOAT_TYPE | INT_TYPE;
FLOAT_TYPE: 'FLOAT';
INT_TYPE: 'INT';
INT: DIGIT+;
FLOAT: DIGIT '.' DIGIT* | '.' DIGIT+;
STRING: '"' (ESC|.)*? '"';
ID: [a-zA-Z_][a-zA-Z0-9_]*;
COMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: '//'~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment ESC: '\\'[btnr"\\];

