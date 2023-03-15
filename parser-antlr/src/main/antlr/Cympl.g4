grammar Cympl;

prog: statement+EOF             #Program
;

statement
    : varDecl ';'                   #VariableDeclaration
    | funcDecl                      #FunctionDeclaration
    | assign ';'                    #Assignment
    | indexAssign ';'               #IndexAssignment
    | expr';'                       #Expression
    | returnStat                    #ReturnStatement
    | ifStat                        #IfStatement
    | whileStat                     #WhileStatement
    | forStat                       #ForStatement
    | breakStat                     #BreakStatement
    | continueStat                  #ContinueStatement
    | block                         #BlockStatement
;

type: INT_TYPE | FLOAT_TYPE | STRING_TYPE | BOOL_TYPE | type '[]';

varDecl: ID ':' type '=' expr;

funcDecl: FUNC ID '(' paramDecls? ')' (':' (type | VOID_TYPE))? block;

paramDecls: paramDecl (',' paramDecl)*;

paramDecl: ID ':' type;

returnStat: RETURN expr? ';';

block: '{' statement* '}';

ifStat: IF '(' expr ')' thenBranch=statement (ELSE elseBranch=statement )?;

whileStat: WHILE '(' expr ')' statement;

forInit: varDecl | assign;
forStat: FOR '(' forInit? ';' cond=expr? ';' (updateExpr=expr | updateAssign=assign)? ')' statement;

breakStat: BREAK ';';

continueStat: CONTINUE ';';

assign: ID '=' expr;

indexAssign: arrayExpr=expr '[' indexExpr=expr ']' '=' valueExpr=expr;

expr: ID '(' exprlist? ')'                              # FunctionCall
    | arrayExpr=expr '[' indexExpr=expr ']'             # Index
    | MINUS expr                                        # Negation
    | NOT expr                                          # LogicalNot
    | '(' expr ')'                                      # ParenthesizedExpression
    | op=(INC | DEC) expr                                 # PreIncDec
    | expr op=(INC | DEC)                                 # PostIncDec
    | <assoc=right> expr '^' expr                       # Power
    | expr op=(TIMES | DIV | REM) expr                  # MulDiv
    | expr op=(PLUS | MINUS) expr                       # AddSub
    | expr op=(EQ | NEQ | GT | GTE | LT | LTE) expr     # Comparison
    | expr AND expr                                     # LogicalAnd
    | expr OR expr                                      # LogicalOr
    | ID                                                # Variable
    | bool=(TRUE | FALSE)                               # BOOL
    | INT                                               # INT
    | FLOAT                                             # FLOAT
    | STRING                                            # STRING
    | '[' exprlist? ']'                                 # ArrayExpression
    ;

exprlist: expr (',' expr)*;

INC: '++';
DEC: '--';
PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
REM: '%';
EQ: '==';
NEQ: '!=';
GT: '>';
GTE: '>=';
LT: '<';
LTE: '<=';
NOT: '!';
AND: '&&';
OR: '||';

INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';
BOOL_TYPE: 'BOOL';
VOID_TYPE: 'VOID';

TRUE: 'true';
FALSE: 'false';
INT: DIGIT+;
FLOAT: DIGIT '.' DIGIT* | '.' DIGIT+;
STRING: '"' (ESC|.)*? '"';

FUNC: 'func';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
BREAK: 'break';
CONTINUE: 'continue';
RETURN: 'return';

ID: [a-zA-Z_][a-zA-Z0-9_]*;
COMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: ('//'|'#')~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment ESC: '\\'[btnr"\\];

