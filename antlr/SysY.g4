grammar SysY;

compilationUnit: (declaration | functionDefinition)*;

declaration: constantDeclaration | variableDeclaration;

constantDeclaration:
	CONST typeSpecifier constantDefinition (COMMA constantDefinition)* SEMI;

typeSpecifier: type=(VOID | INT | FLOAT);

constantDefinition:
	Identifier (LBRACKET constantExpression RBRACKET)* ASSIGN constantInitializer;

constantInitializer:
	constantExpression
	| LBRACE (constantInitializer (COMMA constantInitializer)*)? RBRACE;

variableDeclaration:
	typeSpecifier variableDefinition (COMMA variableDefinition)* SEMI;

variableDefinition:
	Identifier (LBRACKET constantExpression RBRACKET)*
	| Identifier (LBRACKET constantExpression RBRACKET)* ASSIGN initializer;

initializer:
	expression # initializerExpression
	| LBRACE (initializer (COMMA initializer)*)? RBRACE # initializerList
	;

functionDefinition:
	typeSpecifier Identifier LPAREN parameterList? RPAREN compoundStatement;

parameterList:
	parameterDeclaration (COMMA parameterDeclaration)*;

parameterDeclaration:
	typeSpecifier Identifier (LBRACKET RBRACKET (LBRACKET expression RBRACKET)*)?;

compoundStatement: LBRACE blockItem* RBRACE;

blockItem: declaration | statement;

statement:
	lValue ASSIGN expression SEMI # assignmentStatement
	| expression? SEMI # expressionStatement
	| compoundStatement # childCompoundStatement
	| IF LPAREN conditionalExpression RPAREN statement (ELSE statement)? # ifStatement
	| WHILE LPAREN conditionalExpression RPAREN statement # whileStatement
	| BREAK SEMI # breakStatement
	| CONTINUE SEMI # continueStatement
	| RETURN expression? SEMI # returnStatement
    ;

expression: additiveExpression;

conditionalExpression: logicalOrExpression;

lValue: Identifier (LBRACKET expression RBRACKET)*;

primaryExpression:
	LPAREN expression RPAREN # parenthesizedExpression
	| lValue # childLValue
	| number # childNumer
	;

number:
	IntegerConstant # integerConstant
	| FloatingConstant # floatingConstant
	;

unaryExpression:
	primaryExpression # childPrimaryExpression
	| Identifier LPAREN argumentExpressionList? RPAREN # functionCallExpression
	| unaryOperator unaryExpression # unaryOperatorExpression
	;

unaryOperator: op=(ADD | SUB | LNOT);

argumentExpressionList: expression (COMMA expression)*;

multiplicativeExpression:
	unaryExpression # childUnaryExpression
	| unaryExpression op=(MUL | DIV | MOD) multiplicativeExpression # binaryMultiplicativeExpression
	;

additiveExpression:
	multiplicativeExpression # childMultiplicativeExpression
	| multiplicativeExpression op=(ADD | SUB) additiveExpression # binaryAdditiveExpression
	;

relationalExpression:
	additiveExpression # childAdditiveExpression
	| additiveExpression op=(LT | GT | LE | GE) relationalExpression # binaryRelationalExpression
	;

equalityExpression:
	relationalExpression # childRelationalExoression
	| relationalExpression op=(EQ | NE) equalityExpression # binaryEqualityExpression
	;

logicalAndExpression:
	equalityExpression # childEqualityExpression
	| equalityExpression op=LAND logicalAndExpression # binaryLogicalAndExpression
	;

logicalOrExpression:
	logicalAndExpression # childLogicalAndExpression
	| logicalAndExpression op=LOR logicalOrExpression # binaryLogicalOrExpression
	;

constantExpression: additiveExpression;

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

Identifier: Nondigit (Nondigit | Digit)*;

fragment Nondigit: [a-zA-Z];

fragment Digit: [0-9];

IntegerConstant:
	DecimalConstant
	| OctalConstant
	| HexadecimalConstant;

fragment DecimalConstant: NonzeroDigit Digit*;

fragment NonzeroDigit: [1-9];

fragment OctalConstant: '0' OctalDigit*;

fragment OctalDigit: [0-7];

fragment HexadecimalConstant:
	HexadecimalPrefix HexadecimalDigit+;

fragment HexadecimalPrefix: '0' [xX];

fragment HexadecimalDigit: [0-9a-fA-F];

FloatingConstant:
	DecimalFloatingConstant
	| HexadecimalFloatingConstant;

fragment DecimalFloatingConstant:
	FractionalConstant ExponentPart?
	| Digit+ ExponentPart;

fragment FractionalConstant: Digit+? '.' Digit+ | Digit+ '.';

fragment ExponentPart: [eE] Sign? Digit+;

fragment Sign: [+-];

fragment HexadecimalFloatingConstant:
	HexadecimalPrefix (HexFractionalConst | HexadecimalDigit+) BinaryExponentPart;

fragment HexFractionalConst:
	HexadecimalDigit+? '.' HexadecimalDigit+
	| HexadecimalDigit+ '.';

fragment BinaryExponentPart: [pP] Sign? Digit+;

Whitespace: [ \t]+ -> skip;

Newline: ('\r' '\n'? | '\n') -> skip;

BlockComment: '/*' .*? '*/' -> skip;

LineComment: '//' ~[\r\n]* -> skip;
