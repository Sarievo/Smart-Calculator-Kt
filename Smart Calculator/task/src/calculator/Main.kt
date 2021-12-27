package calculator

import java.math.BigInteger
import java.util.*
import java.util.regex.Pattern

object Main {
    private var s = Scanner(System.`in`)
    private var errMsg = arrayOf(
        "Invalid expression", "Unknown command", "Invalid assignment", "Unknown variable"
    )
    private var commands = listOf("/help")
    private var precedence = HashMap<Char, Int>()
    private var numberRegex = "[-+]?\\d+"
    private var numberPattern: Pattern = Pattern.compile(numberRegex)
    private var latinRegex = "[a-zA-Z]+"
    private var latinPattern: Pattern = Pattern.compile(latinRegex)

    //    static Pattern variablePattern = Pattern.compile(variableRegex);
    private var operatorRegex = "[*=/()^]|\\++|-+"
    private var operatorPattern: Pattern = Pattern.compile(operatorRegex)
    private var variables = HashMap<String, BigInteger?>()

    //    static HashMap<String, Long> variables = new HashMap<>();
    private var variableRegex = "$numberRegex|$latinRegex"
    private var notationPattern: Pattern = Pattern.compile("$variableRegex|$operatorRegex")
    private fun initPrecedence(precedence: HashMap<Char, Int>) {
        precedence['('] = 0
        precedence[')'] = 0
        precedence['^'] = 1 // Exponents
        precedence['*'] = 2 // Multiplication
        precedence['/'] = 2 // Division
        precedence['+'] = 3 // Addition
        precedence['-'] = 3 // Subtraction
    }

    @JvmStatic
    fun main(args: Array<String>) {
        initPrecedence(precedence)
        while (true) {
            val raw = s.nextLine().trim { it <= ' ' }
            try {
                if ("" == raw.trim { it <= ' ' }) {
                    continue
                } else if (raw[0] == '/') {
                    if ("/exit" == raw) {
                        break
                    } else if (commands.contains(raw)) {
                        if ("/help" == raw) {
                            println("X|   The program calculates the sum of numbers")
                            println("X| Supports binary minus: double-minus means plus")
                            println("X|   Currently no functionality at assigning an expression")
//                            println("X| or negative numbers not separated by a space")
                        }
                        continue
                    }
                    err(1)
                    continue
                }
                if (!checkValidityOrAssign(raw)) continue  // if invalid or assign, just continue
                val infix: Queue<Notation> = scanInfix(raw)
                //                System.out.println("Scanned Infix: " + infix);
                val postfix: Queue<Notation> = convertPostfix(infix)
                //                System.out.println("Converted Postfix: " + postfix);
                val result = calculatePostfix(postfix)
                println(result)
            } catch (e: Exception) {
//                e.printStackTrace();
                err(0)
            }
        }
        println("Bye!")
    }

    private fun calculatePostfix(postfix: Queue<Notation>): BigInteger {
        val result = Stack<BigInteger>()
        //        Stack<Long> result = new Stack<>();
        while (!postfix.isEmpty()) {
            val element: Notation = postfix.remove()
            if (element.isVariable) {
                result.push(element.value)
            } else {
                val operator: Char = element.operator
                val a = result.pop()
                val b = result.pop()
                when (operator) {
                    '^' -> result.push(b.pow(a.toString().toInt()))
                    '*' -> result.push(b.multiply(a))
                    '/' -> result.push(b.divide(a))
                    '+' -> result.push(b.add(a))
                    '-' -> result.push(b.subtract(a))
                }
            }
        }
        if (result.size != 1) {
            println("WHAT")
        }
        return result.pop()
    }

    private fun checkValidityOrAssign(raw: CharSequence): Boolean {
        val names = scanNames(raw)
        val numbers = scanNumbers(raw)
        //        List<Long> numbers = scanNumbers(raw);
        val assignsOrOperators = scanOperators(raw)
        if (names.isEmpty()) { // if no variables
            if (assignsOrOperators.isNotEmpty()) {
                for (operator in assignsOrOperators) {
                    if (operator == '=') {
                        err(2) // err if it has an assign pattern
                        return false
                    }
                }
            }
        } else { // if it has variables
            if (assignsOrOperators.isNotEmpty() && assignsOrOperators[0] == '=') { // if it has an assign pattern
                if (names.size + numbers.size != 2) {
                    err(2) // err if it has not equal to 1 assign pattern
                    return false
                } else if (numbers.isNotEmpty()) {
                    variables[names[0]] = numbers[0] // assign number to variable
                } else {
                    if (!variables.containsKey(names[1])) {
                        err(3) // err if the second variable is unknown
                        return false
                    }
                    variables[names[0]] = variables[names[1]] // assign variable to variable
                }
                return false
            } else {
                for (name in names) {
                    if (!variables.containsKey(name)) {
                        err(3) // err if it has unknown variables
                        return false
                    }
                }
            }
        }
        return true // return true if it is a valid non-assign infix notation
    }

    private fun scanNames(s: CharSequence): List<String> {
        val matchList: MutableList<String> = ArrayList()
        val regexMatcher = latinPattern.matcher(s)
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group())
        }
        return matchList
    }

    private fun scanNumbers(s: CharSequence): List<BigInteger> {
        // only scan numbers
        val matchList: MutableList<BigInteger> = ArrayList()
        //        List<Long> matchList = new ArrayList<>();
        val regexMatcher = numberPattern.matcher(s)
        while (regexMatcher.find()) {
            matchList.add(BigInteger(regexMatcher.group()))
            //            matchList.add(Long.parseLong(regexMatcher.group()));
        }
        return matchList
    }

    private fun scanOperators(s: CharSequence): List<Char> {
        // only scan operator symbols
        val matchList: MutableList<Char> = ArrayList()
        val regexMatcher = operatorPattern.matcher(s)
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group()[0]) // if it's any notation just add at this step
        }
        return matchList
    }

    private fun scanInfix(s: CharSequence): Queue<Notation> {
        // scan all variables and map them to values
        val matchList: Queue<Notation> = ArrayDeque()
        val notationMatcher = notationPattern.matcher(s)
        while (notationMatcher.find()) {
            val notation = notationMatcher.group()
            if (notation.matches(Regex(variableRegex))) { // if it's a variable, append its value
                if (variables.containsKey(notation)) {
                    matchList.add(Notation(variables[notation]))
                } else {
                    matchList.add(Notation(BigInteger(notation)))
                    //                    matchList.add(new Notation(Long.parseLong(notation)));
                }
            } else { // if it's an operator, append its symbol
                if (notation[0] == '-' && notation.length % 2 == 0) {
                    matchList.add(Notation('+')) // if it's some even minus, append plus
                } else {
                    matchList.add(Notation(notation[0]))
                }
            }
        }
        return matchList
    }

    private fun convertPostfix(infix: Queue<Notation>): Queue<Notation> {
        val postfix: Queue<Notation> = ArrayDeque()
        val stack = Stack<Char>()
        while (!infix.isEmpty()) {
            val symbol: Notation = infix.remove()
            if (symbol.isVariable) {
                postfix.add(symbol) // 1. Add operands (numbers and variables) to the result (postfix notation) as they arrive.
            } else {
                val operator: Char = symbol.operator // then it's an operator
                if (stack.isEmpty() || stack.peek() == '(' || operator == '(') { // 5. If the incoming element is a left parenthesis, push it on the stack.
                    stack.push(operator) // 2. If the stack is empty or contains a left parenthesis on top, push the incoming operator on the stack.
                    continue
                }
                if (operator == ')') { // 6. If the incoming element is a right parenthesis, pop the stack and add operators to the result until you see a left parenthesis.
                    while (stack.peek() != '(') {
                        postfix.add(Notation(stack.pop()))
                    }
                    stack.pop() // Discard the pair of parentheses.
                    continue
                }
                if (precedence[operator]!! < precedence[stack.peek()]!!) {
                    stack.push(operator) // 3. If the incoming operator has higher precedence than the top of the stack, push it on the stack.
                } else {
                    do {
                        postfix.add(Notation(stack.pop())) // 4. If the incoming operator has lower or equal precedence than the top of the operator stack, pop the stack and add operators to the result until you see an operator that has a smaller precedence or a left parenthesis on the top of the stack;
                    } while (!stack.isEmpty()
                        && precedence[operator]!! >= precedence[stack.peek()]!! && stack.peek() != '('
                    )
                    stack.push(operator) // then add the incoming operator to the stack.
                }
            }
        }
        while (!stack.isEmpty()) { // 7. At the end of the expression, pop the stack and add all operators to the result.
//            System.out.println(stack);
            postfix.add(Notation(stack.pop()))
        }
        return postfix
    }

    private fun err(i: Int) {
        println(errMsg[i])
    }
}

class Notation {
    var value: BigInteger? = null

    var operator = 0.toChar()
    var isVariable: Boolean

    internal constructor(value: BigInteger?) {
        this.value = value
        isVariable = true
    }

    internal constructor(operator: Char) {
        this.operator = operator
        isVariable = false
    }

    override fun toString(): String {
        return if (isVariable) value.toString() else operator.toString()
    }
}