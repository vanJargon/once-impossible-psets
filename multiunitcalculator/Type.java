package com.example;

/*
 * TODO (optional) define your symbols and groups as comment here
 * Math operations: + - * /
 * Numbers: 1 2 3 4 5 6 7 8 9 0 .
 * Units: in pt
 */

/**
 * Token type.
 */
enum Type {
	L_PAREN,	// it means (
	R_PAREN,	// it means )
	NUMBER,		// 1, 2, 3, 4...
	INCH,		
	POINT,
	PLUS,		// +
	MINUS,		// -
	TIMES,		// *
	DIVIDE		// /
}