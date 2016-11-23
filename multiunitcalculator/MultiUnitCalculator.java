package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class MultiUnitCalculator {
    public String evaluate(String expression) {
        Lexer lexer = new Lexer(expression);
        Parser parser = new Parser(lexer);
        return parser.evaluate().toString();
    }

    public static void main(String[] args) throws IOException {
        MultiUnitCalculator calculator;
        String result;

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String expression;
        while (true) {
            // display prompt
            System.out.print("> ");
            // read input
            expression = in.readLine();
            // terminate if input empty
            if (expression.equals(""))
                break;

            // evaluate
            calculator = new MultiUnitCalculator();
            result = calculator.evaluate(expression);
            // display result
            System.out.println(result);
        }
    }
}

class Lexer {

    /**
     * Token in the stream.
     */
    public static class Token {
        final Type type;
        final String text;

        Token(Type type, String text) {
            this.type = type;
            this.text = text;
        }

        Token(Type type) {
            this(type, null);
        }
    }

    @SuppressWarnings("serial")
    static class TokenMismatchException extends Exception {
        TokenMismatchException(){

        }

        TokenMismatchException(String message){
            super(message);
        }
    }

    // create arraylist to store list of tokens
    ArrayList<Token> tokens = new ArrayList<Token>();

    public Lexer(String input) {
        try {
            // remove all white spaces from input
            input = input.replaceAll("\\s","");

            // create initial empty num string
            String num = "";

            // loop through input string and output tokens
            for (int i=0;i<input.length();i++) {
                char c = input.charAt(i);

                if (String.valueOf(c).matches("\\d")) {
                    num += c;
                } else if (String.valueOf(c).matches("\\.")) {
                    if (!num.contains(".")) {
                        num += c;
                    } else {
                        throw new TokenMismatchException("More than one decimal points in a number string");
                    }
                } else { // case if c is not a number or .
                    if (num.length() != 0) {
                        tokens.add(new Token(Type.NUMBER, num));
                        num = "";
                    }

                    // remaining cases are either operators or units
                    switch (c) {
                        case 'i':
                            if (input.charAt(i + 1) == 'n') {
                                tokens.add(new Token(Type.INCH));
                                i += 1;
                            } else {
                                throw new TokenMismatchException("Unknown unit found");
                            }
                            break;
                        case 'p':
                            if (input.charAt(i + 1) == 't') {
                                tokens.add(new Token(Type.POINT));
                                i += 1;
                            } else {
                                throw new TokenMismatchException("Unknown unit found");
                            }
                            break;
                        case '(':
                            tokens.add(new Token(Type.L_PAREN));
                            break;
                        case ')':
                            tokens.add(new Token(Type.R_PAREN));
                            break;
                        case '+':
                            tokens.add(new Token(Type.PLUS));
                            break;
                        case '-':
                            tokens.add(new Token(Type.MINUS));
                            break;
                        case '*':
                            tokens.add(new Token(Type.TIMES));
                            break;
                        case '/':
                            tokens.add(new Token(Type.DIVIDE));
                            break;
                    }
                }
            }
            if (num.length()!=0){
                tokens.add(new Token(Type.NUMBER, num));
                num = "";
            }
        } catch (TokenMismatchException e){
            System.out.println(e.getMessage());
        }
    }
}


class Parser {

    @SuppressWarnings("serial")
	static class ParserException extends RuntimeException {
        ParserException(){

        }

        ParserException(String message){
            super(message);
        }
	}

	/**
	 * Type of values.
	 */
	private enum ValueType {
		POINTS, INCHES, SCALAR
	};

	/**
	 * Internal value is always scalar.
	 */
	public class Value {
		final double value;
		final ValueType type;

		Value(double value, ValueType type) {
			this.value = value;
			this.type = type;
		}

        public double getValue() {
            return this.value;
        }

        public ValueType getType() {
            return this.type;
        }

        Value convert(ValueType newType) {
            if (type == newType) {
                return this;
            } else {
                switch (newType) {
                    case INCHES:
                        if (type == ValueType.POINTS) {
                            return new Value(value/PT_PER_IN, newType);
                        }
                        return new Value(value, newType);
                    case POINTS:
                        if (type == ValueType.INCHES) {
                            return new Value(value*PT_PER_IN, newType);
                        }
                        return new Value(value, newType);
                    default: throw new ParserException();
                }
            }
        }

		@Override
		public String toString() {
            double value2 = Math.round(value*100.0)/100.0;
			switch (type) {
			case INCHES:
				return value2 + " in";
			case POINTS:
				return value2 + " pt";
			default:
				return "" + value2;
			}
		}
	}

	private static final double PT_PER_IN = 72;
	private final Lexer lexer;
    private ArrayList<Lexer.Token> tokens;
    Lexer.Token currentToken;
    Lexer.Token nextToken;
    int position;
    int lParan;
    int rParan;

	Parser(Lexer lexerInput) {
        lexer = lexerInput;
        this.tokens = lexer.tokens;
	}

	public Value evaluate() {
        Value value = rCompute(tokens);
        return value;
	}

    // recursive computation method to handle parans
    private Value rCompute(ArrayList<Lexer.Token> tokensInput){
        //getting the inner most left bracket first (if it exists)
        lParan = -1;
        for (int i=tokensInput.size()-1;i>=0;i--){ //for loop from backwards
            if (tokensInput.get(i).type==Type.L_PAREN){
                lParan = i;
                break;
            }
        }

        //getting complementary inner most right bracket (if it exists)
        rParan = -1;
        if (lParan>-1){
            for (int i=lParan;i<tokensInput.size();i++){
                if (tokensInput.get(i).type==Type.R_PAREN){
                    rParan = i;
                    break;
                }
            }
        }

        //start of actual recursive implementation
        if (lParan==-1 && rParan==-1){ //base case = no parans
            return mathOps(tokensInput);
        } else {
            ArrayList<Lexer.Token> subTokensInput = new ArrayList<Lexer.Token>(tokensInput.subList(lParan+1,rParan));
            Value tempValue = mathOps(subTokensInput);

            ArrayList<Lexer.Token> newTokensInput = new ArrayList<Lexer.Token>();

            for (int i=0;i<tokensInput.size();i++){
                if (i==lParan){
                    newTokensInput.add(new Lexer.Token(Type.NUMBER,String.valueOf(tempValue.getValue())));

                    if (tempValue.getType()==ValueType.INCHES){
                        newTokensInput.add(new Lexer.Token(Type.INCH));
                    } else if (tempValue.getType()==ValueType.POINTS){
                        newTokensInput.add(new Lexer.Token(Type.POINT));
                    }

                    i = rParan;
                } else {
                    newTokensInput.add(tokensInput.get(i));
                }
            }
            return rCompute(newTokensInput);
        }
    }


    // method that can calculate for expressions without parans
    private Value mathOps(ArrayList<Lexer.Token> tokensInput){
        position = 0;
        currentToken = tokensInput.get(position);

        double tempNum = 0.0;
        Value leftValue = new Value(0,ValueType.SCALAR);
        Value rightValue = new Value(0,ValueType.SCALAR);
        ValueType leftValueType = ValueType.SCALAR;

        // first token must be a number
        if (currentToken.type==Type.NUMBER){
            tempNum = Double.valueOf(currentToken.text);
            if (tokens.size()>1){
                nextToken = tokensInput.get(position+1);

                if (nextToken.type==Type.POINT){
                    leftValue = new Value(tempNum,ValueType.POINTS);
                    tempNum = 0.0;
                    position += 2;
                } else if (nextToken.type==Type.INCH){
                    leftValue = new Value(tempNum,ValueType.INCHES);
                    tempNum = 0.0;
                    position += 2;
                } else {
                    leftValue = new Value(tempNum,ValueType.SCALAR);
                    tempNum = 0.0;
                    position += 1;
                }
            } else { //expression is just 1 number
                leftValue = new Value(tempNum,ValueType.SCALAR);
                tempNum = 0.0;
            }
            leftValueType = leftValue.getType();
        } else {
            throw new ParserException("Sub-expression must start with a number.");
        }

        // search for subsequent expression. Must be either an operator or units
        for (int i=position;i<tokensInput.size();i++){
            currentToken = tokensInput.get(i);

            // check for units first
            if (currentToken.type==Type.POINT){
                leftValue = leftValue.convert(ValueType.POINTS);
            }

            if (currentToken.type==Type.INCH){
                leftValue = leftValue.convert(ValueType.INCHES);
            }

            // check for operators
            if (currentToken.type==Type.PLUS || currentToken.type==Type.MINUS || currentToken.type==Type.TIMES || currentToken.type==Type.DIVIDE){
                if (i<tokensInput.size()-2){
                    nextToken = tokensInput.get(i+1);

                    if (nextToken.type==Type.NUMBER){
                        tempNum = Double.valueOf(nextToken.text);
                        if (tokensInput.get(i+2).type==Type.POINT){
                            rightValue = new Value(tempNum,ValueType.POINTS);
                            i+=2;
                        } else if (tokensInput.get(i+2).type==Type.INCH){
                            rightValue = new Value(tempNum,ValueType.INCHES);
                            i+=2;
                        } else {
                            rightValue = new Value(tempNum,ValueType.SCALAR);
                            i+=1;
                        }
                    } else {
                        throw new ParserException("Non-numeral found after an operator");
                    }

                } else if (i==tokensInput.size()-2){ // i is the second last element here and is an operator
                    nextToken = tokensInput.get(i+1);

                    if (nextToken.type==Type.NUMBER){
                        tempNum = Double.valueOf(nextToken.text);
                        rightValue = new Value(tempNum,ValueType.SCALAR);
                    } else {
                        throw new ParserException("Non-numeral found after an operator");
                    }

                } else {
                    throw new ParserException("Cannot end sub-expression with an operator.");
                }

                //convert if necessary
                if (leftValueType!=ValueType.SCALAR){
                    rightValue = rightValue.convert(leftValueType);
                } else { //left value is a scalar
                    if (rightValue.getType()!=ValueType.SCALAR){
//                        System.out.println("Right value type:"+rightValue.getType());
                        leftValue = leftValue.convert(rightValue.getType());
//                        System.out.println("Left value type:"+leftValue.getType());
                        leftValueType = leftValue.getType();

                    }
                }

                // operations
                if (currentToken.type==Type.PLUS){
                    leftValue = new Value(leftValue.getValue()+rightValue.getValue(),leftValueType);
                } else if (currentToken.type==Type.MINUS){
                    leftValue = new Value(leftValue.getValue()-rightValue.getValue(),leftValueType);
                } else if (currentToken.type==Type.TIMES){
                    leftValue = new Value(leftValue.getValue()*rightValue.getValue(),leftValueType);
                } else {
                    leftValue = new Value(leftValue.getValue()/rightValue.getValue(),leftValueType);
                }
            }
        }
        return leftValue;
    }
}
