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

ASSIGN: '=';

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

Ident: Nondigit (Nondigit | Digit)*;

fragment Nondigit: [a-zA-Z];

fragment Digit: [0-9];

IntConst: DecConst | OctConst | HexConst;

fragment DecConst: NonzeroDigit Digit*;

fragment NonzeroDigit: [1-9];

fragment OctConst: '0' OctDigit*;

fragment OctDigit: [0-7];

fragment HexConst: HexPrefix HexDigit+;

fragment HexPrefix: '0' [xX];

fragment HexDigit: [0-9a-fA-F];

FloatConst: DecFloatConst | HexFloatConst;

fragment DecFloatConst: FractionalConst ExponentPart?
	                  | Digit+ ExponentPart;

fragment FractionalConst: Digit+? '.' Digit+
	                    | Digit+ '.';

fragment ExponentPart: [eE] Sign? Digit+;

fragment Sign: [+-];

fragment HexFloatConst: HexPrefix (HexFractionalConst | HexDigit+) BinaryExponentPart;

fragment HexFractionalConst: HexDigit+? '.' HexDigit+
	                       | HexDigit+ '.';

fragment BinaryExponentPart: [pP] Sign? Digit+;

Whitespace: [ \t]+ -> skip;

Newline: ('\r' '\n'? | '\n') -> skip;

BlockComment: '/*' .*? '*/' -> skip;

LineComment: '//' ~[\r\n]* -> skip;
