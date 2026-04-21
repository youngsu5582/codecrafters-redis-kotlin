object ErrorCode {
    const val SMALLER_OR_EQUAL = "ERR The ID specified in XADD is equal or smaller than the target stream top item"
    const val MUST_BE_GT_ZERO = "ERR The ID specified in XADD must be greater than 0-0"
}

class CustomException(message: String) : Exception(message)