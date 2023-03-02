grammar Expr;

prog: (decl | expr | assign)+EOF            #Program
;

decl: ID ':' INT_TYPE '=' expr              #Declaration
;

assign: ID '=' expr     # Assignment
;

expr: '(' expr ')'      # ParenthesizedExpression
    | <assoc=right> expr '^' expr     # Power
    | expr '*' expr     # Multiplication
    | expr '/' expr     # Division
    | expr '+' expr     # Addition
    | expr '-' expr     # Substraction
    | ID                # Variable
    | NUM               # Number
    ;

INT_TYPE : 'INT';
ID: [a-z][a-zA-Z0-9_]*;
NUM: '0' | '-'?[1-9][0-9]*;
COMMENT: '//'~[\r\n]* -> skip;
WS: [ \t\r\n] -> skip;
