lexer grammar SysYLexer;

BREAK: 'break';
CONST: 'const';
CONTINUE: 'continue';
ELSE: 'else';
FLOAT: 'float';
IF: 'if';
INT: 'int';
RETURN: 'return';
VOID: 'void';
WHILE: 'while';

ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
MOD: '%';

EQ: '==';
NE: '!=';
LT: '<';
LE: '<=';
GT: '>';
GE: '>=';

LNOT: '!';
LAND: '&&';
LOR: '||';

LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
LBRACE: '{';
RBRACE: '}';

COMMA: ',';
SEMI: ';';

Identifier: Nondigit (Nondigit | Digit)*;

fragment Nondigit: [a-zA-Z];

fragment Digit: [0-9];

IntegerConstant: DecimalConstant | OctalConstant | HexadecimalConstant;

fragment DecimalConstant: NonzeroDigit Digit*;

fragment OctalConstant: '0' OctalDigit*;

fragment HexadecimalConstant: HexadecimalPrefix HexadecimalDigit+;

fragment HexadecimalPrefix: '0' [xX];

fragment HexadecimalDigit: [0-9a-fA-F];

fragment NonzeroDigit: [1-9];

fragment OctalDigit: [0-7];

Whitespace: [ \t]+ -> skip;

Newline: ('\r' '\n'? | '\n') -> skip;

BlockComment: '/*' .*? '*/' -> skip;

LineComment: '//' ~[\r\n]* -> skip;
