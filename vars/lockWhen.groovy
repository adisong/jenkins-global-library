def call(LinkedHashMap params, boolean expression, Closure body) {
    if (body != null) {
        if (expression) {
            lock(params) {
                body.call()
            }
        } else {
            body.call()
        }
    }
}
