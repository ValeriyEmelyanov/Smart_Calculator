package calculator

import java.lang.RuntimeException
import java.math.BigInteger

val REGEX_OPERATOR_FIRST = "(?<!^)-+|\\++|\\*|/|\\(|\\)".toRegex();
val REGEX_OPERATOR = "-+|\\++|\\*|/|\\(|\\)".toRegex();

val EQ_DELIMITER = "\\s*=\\s*".toRegex()
val BLANKSPACES = "\\s+".toRegex()

val REGEXP_NUM = "[+-]?\\d+".toRegex()
val REGEX_PLUSES = "\\++".toRegex()
val REGEX_MINUSES = "-+".toRegex()
val REGEXP_VAR = "[a-zA-Z]+".toRegex()

val REGEX_VAR_ASSING_NUM = "[a-zA-Z]+\\s*=\\s*[+-]?\\d+".toRegex()
val REGEX_VAR_ASSING_INVALID_ID = "[a-zA-Z]+\\d+\\w*\\s*=\\s*\\w+".toRegex()
val REGEX_VAR_ASSING_VAR = "[a-zA-Z]+\\s*=\\s*[a-zA-Z]+".toRegex()

val VARIABLES = mutableMapOf<String, BigInteger>()

fun main() {
    while (true) {
        val input = readLine()!!.trim()

        if (input.isEmpty()) continue
        if ("/exit" == input) break

        when {
            "/help" == input -> println("The program calculates the expression")
            input.startsWith("/") -> println("Unknown command")
            input.contains("=") -> varAssing(input.trim())
            else -> calculate(input)
        }
    }

    println("Bye!")
}

fun varAssing(input: String) {
    try {
        when {
            REGEX_VAR_ASSING_NUM.matches(input) -> {
                val split = input.split(EQ_DELIMITER)
                VARIABLES.put(split[0], split[1].toBigInteger())
            }
            REGEX_VAR_ASSING_INVALID_ID.matches(input) -> println("Invalid identifier")
            REGEX_VAR_ASSING_VAR.matches(input) -> {
                val split = input.split(EQ_DELIMITER)
                if (VARIABLES.containsKey(split[1])) {
                    VARIABLES.put(split[0], VARIABLES.get(split[1])!!)
                } else {
                    println("Unknown variable")
                }
            }
            else -> println("Invalid assignment")
        }
    } catch (ignored: Exception) {
        println("Invalid assignment")
    }
}

fun calculate(input: String) {
    try {
        val tokens = convertToPostfix(input)
        calculatePostfix(tokens)
    } catch (e: AppException) {
        println(e.message)
    } catch (e: Exception) {
        println("Invalid expression")
    }
}

fun calculatePostfix(tokens: List<String>) {
    val stack = mutableListOf<BigInteger>()

    for (token in tokens) {
        when {
            REGEXP_NUM.matches(token) -> stack.push(token.toBigInteger())
            REGEXP_VAR.matches(token) && VARIABLES.containsKey(token) -> stack.push(VARIABLES.get(token)!!)
            REGEXP_VAR.matches(token) -> throw UnknownVariableException()
            stack.size < 2 -> throw InvalidExpressionException()
            "+" == token -> stack.push(stack.pop() + stack.pop())
            "-" == token -> {
                val operand2 = stack.pop()
                val operand1 = stack.pop()
                stack.push(operand1 - operand2)
            }
            "*" == token -> stack.push(stack.pop() * stack.pop())
            "/" == token -> {
                val operand2 = stack.pop()
                val operand1 = stack.pop()
                stack.push(operand1 / operand2)
            }
            else -> throw InvalidExpressionException()
        }
    }

    if ((stack.size == 1)) {
        println(stack.pop())
    } else {
        throw InvalidExpressionException()
    }
}

fun convertToPostfix(input: String): List<String> {
    var expr = input.replace(BLANKSPACES, "")
    val tokens = mutableListOf<String>()
    val operators = mutableListOf<String>()
    var isFirst = true

    while (!expr.isEmpty()) {
        val split = expr.split(if (isFirst) REGEX_OPERATOR_FIRST else REGEX_OPERATOR, 2)
        isFirst = false

        if (!split[0].isEmpty()) tokens.add(split[0])

        if (split.size < 2) break

        val operator = normalizeOperator(REGEX_OPERATOR.find(expr)!!.value)

        when {
            operator == "(" -> operators.add(operator)
            operator == ")" -> {
                var solved = false
                for (i in operators.size - 1 downTo 0) {
                    val removing = operators.removeAt(i)
                    if (removing == "(") {
                        solved = true
                        break
                    }
                    tokens.add(removing)
                }
                if (!solved) return emptyList()
            }
            !operators.isEmpty() && isGreaterOrEqualPrecedence(operator, operators.last()) -> {
                tokens.add(operators.removeAt(operators.lastIndex))
                operators.add(operator)
            }
            else -> operators.add(operator)
        }

        expr = split[1]
    }

    for (i in operators.size - 1 downTo 0) {
        tokens.add(operators.removeAt(i))
    }

    return tokens.toList()
}

fun isGreaterOrEqualPrecedence(operator1: String, operator2: String): Boolean {
    return operator1 in "-+" && operator2 in "+-*/"
            || operator1 in "*/" && operator2 in "*/"
}

fun normalizeOperator(operator: String): String {
    return when {
        REGEX_PLUSES.matches(operator) -> "+"
        REGEX_MINUSES.matches(operator) && (operator.length % 2 == 0) -> "+"
        REGEX_MINUSES.matches(operator) && (operator.length % 2 != 0) -> "-"
        else -> operator
    }
}

fun <T> MutableList<T>.push(item: T) = add(item)
fun <T> MutableList<T>.pop(): T = removeAt(lastIndex)

open class AppException(override val message: String): RuntimeException(message)
class InvalidExpressionException: AppException("Invalid expression")
class UnknownVariableException: AppException("Unknown variable")