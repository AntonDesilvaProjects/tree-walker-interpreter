package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Scanner for the jLox language. This class will scan some jLox source code and break it down
 * into a list of tokens.
 *
 * Definitions:
 * - lexeme = a "raw" grouping of characters. A token is a lexeme with additional metadata about it
 *
 */
public class Scanner {

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    TokenType.AND);
        keywords.put("class",  TokenType.CLASS);
        keywords.put("else",   TokenType.ELSE);
        keywords.put("false",  TokenType.FALSE);
        keywords.put("for",    TokenType.FOR);
        keywords.put("fun",    TokenType.FUN);
        keywords.put("if",     TokenType.IF);
        keywords.put("nil",    TokenType.NIL);
        keywords.put("or",     TokenType.OR);
        keywords.put("print",  TokenType. PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super",  TokenType.SUPER);
        keywords.put("this",   TokenType.THIS);
        keywords.put("true",   TokenType.TRUE);
        keywords.put("var",    TokenType.VAR);
        keywords.put("while",  TokenType.WHILE);
    }


    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0; // start of the current lexeme being scanned
    private int current = 0; // current character being scanned
    private int line = 1; // line of the current character

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while(!isAtEnd()) {
            start = current;
            scanToken();
        }

        // when we reach the end, add a token to represent the end of source
        tokens.add(new Token(TokenType.EOF, "", null, line));

        return tokens;
    }

    private boolean isAtEnd() {
        // if the current position is greater than length of the source code,
        // then we have reached the end
        return current >= source.length();
    }

    /**
     * Scan one or more tokens
     */
    private void scanToken() {
        char c = advance(); // get the next character

        switch (c) {
            // single character token
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;

            // double character operator tokens
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            // comments or / operator
            case '/':
                if (match('/')) {
                    // comment goes till the end of line character(\n)
                    // we can use match('\n') instead of peek here but it will consume the newline when it finds it
                    // we need to increment line counter so that gets its own case
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                    // don't add any tokens b/c comments have no meaning
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            // whitespace characters
            case  ' ':
            case '\r':
            case '\t':
                break;

            // new line
            case '\n':
                line++;
                break;

            // strings
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) { // we could add cases for all digit characters but this is shorter
                    number();
                } else if (isAlpha(c)) { // handle identifiers
                    identifier();
                } else {
                    // if we find an unexpected character, report the error but don't throw any errors here
                    Lox.error(line, "Unexpected character.");
                    break;
                }
        }
    }

    /**
     * Consume an identifier
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        // reserved words are essentially identifiers that used by the language itself
        // so check if the text matches one first
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        addToken(type);
    }


    /**
     * Consume a number
     */
    private void number() {
        // consume the digits of the number
        while(isDigit(peek())) {
            advance();
        }

        // if we have exited, it can be either b/c of end of source OR we encountered the decimal point
        // in case of latter, check if the character after the decimal is a digit and consume fractional part of the number
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while(isDigit(peek())) {
                advance();
            }
        }

        // create a token for the number
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Consume a string
     */
    private void string() {
        // consume all the character of the string
        // by advancing as long as the next character is not a " or end of source
        while(peek() != '"' && !isAtEnd()) {
            // strings can be multiline so if we encounter a new line,
            // increase line count
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) { // the source ended before terminating the string
            Lox.error(line, "Unterminated string.");
            return;
        }

        // at this point, the next character is the " so to close the string so eat it
        advance();

        String value = source.substring(
                start + 1, // ignore the starting "
                current - 1 // current is at the character after closing ". move it back to the ". substring will exclude it
        );
        addToken(TokenType.STRING, value);
    }

    /**
     * A conditional version of the advance method which would only consume the next character only if
     * it matches the expected character.
     *
     * @param expected character
     * @return true if the expected character was consumed
     */
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        // note that 'current' actually refers to the next character to be consumed because the
        // advance method increments the position after consuming the current position
        if (source.charAt(current) != expected) {
            return false;
        }

        current++;
        return true;
    }

    /**
     * This is a lookahead function that returns the next character without consuming it.
     * @return next character in the source
     */
    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    /**
     * This is a lookahead function that return the character AFTER the next character without consuming anything.
     * @return character after the next one
     */
    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z' ) || (c >= 'A' && c <= 'Z' ) || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Gets the next character of the source code and advances the position
     * of the cursor to the next character.
     *
     * @return current character from the source string
     */
    private char advance() {
     return source.charAt(current++);
    }


    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Creates and add token of the specified token type with literal.
     *
     * @param type type of the token
     * @param literal Java literal type of the token
     */
    private void addToken(TokenType type, Object literal) {
        // extract the lexeme starting with the 'start' position and ending
        // with the 'current' position
        String text = source.substring(start, current);
        // create a Token with specified type
        tokens.add(new Token(type, text, literal, line));
    }
}
