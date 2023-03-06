grammar Expr;

prog: statement+EOF             #Program
;

statement
    : varDecl                       #VariableDeclaration
    | funcDecl                      #FunctionDeclaration
    | assign                        #Assignment
    | expr';'                       #Expression
    | returnStat                    #ReturnStatement
    | ifStat                        #IfStatement
    | block                         #BlockStatement
;

varDecl: ID ':' type=(FLOAT_TYPE | INT_TYPE | STRING_TYPE) '=' expr ';';

funcDecl: 'func' ID '(' paramDecls? ')' (':' type=(FLOAT_TYPE | INT_TYPE | STRING_TYPE | VOID_TYPE))? block;
paramDecls: paramDecl (',' paramDecl)*;
paramDecl: ID ':' type=(FLOAT_TYPE | INT_TYPE | STRING_TYPE);

returnStat: 'return' expr? ';';

block: '{' statement* '}';

ifStat: 'if' '(' expr ')' thenBranch=statement ('else' elseBranch=statement )?;

assign: ID '=' expr ';';

expr: ID '(' exprlist? ')'              # FunctionCall
    | '(' expr ')'                      # ParenthesizedExpression
    | <assoc=right> expr '^' expr       # Power
    | expr op=(TIMES | DIV) expr        # MulDiv
    | expr op=(PLUS | MINUS) expr       # AddSub
    | expr op=(EQ | NEQ) expr           # Equality
    | ID                # Variable
    | FLOAT             # FLOAT
    | INT               # INT
    | STRING            # STRING
    ;

exprlist: expr (',' expr)*;

PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
EQ: '==';
NEQ: '!=';


INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';
VOID_TYPE: 'VOID';

INT: DIGIT+;
FLOAT: DIGIT '.' DIGIT* | '.' DIGIT+;
STRING: '"' (ESC|.)*? '"';

ID: [a-zA-Z_][a-zA-Z0-9_]*;
COMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: '//'~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment ESC: '\\'[btnr"\\];

