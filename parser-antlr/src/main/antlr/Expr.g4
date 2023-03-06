grammar Expr;

prog: statement+EOF             #Program
;

statement
    : decl                      #VariableDeclaration
    | assign                    #Assignment
    | expr                      #Expression
;

decl: ID ':' type=(FLOAT_TYPE | INT_TYPE | STRING_TYPE) '=' expr;

assign: ID '=' expr;

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

INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';

INT: DIGIT+;
FLOAT: DIGIT '.' DIGIT* | '.' DIGIT+;
STRING: '"' (ESC|.)*? '"';

ID: [a-zA-Z_][a-zA-Z0-9_]*;
COMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: '//'~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment ESC: '\\'[btnr"\\];

