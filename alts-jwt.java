val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
   
try {
    **// Validate x-kong-authorization JWT**
    if (!validateKongAuthorization(httpRequest, httpResponse)) {
        throw UnauthorizedException("Invalid or missing x-kong-authorization JWT")
    }
    // Proceed with further processing...
} catch (e: UnauthorizedException) {
    logger.warn(e.message)
    httpResponse.status = HttpStatus.UNAUTHORIZED.value()
    httpResponse.writer.println("""{"message": "${e.message}"}""")
    return
}


private fun validateKongAuthorization(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val kongAuthorizationHeader = request.getHeader("x-kong-authorization")
        if (kongAuthorizationHeader.isNullOrBlank()) {
            logger.warn("Missing x-kong-authorization header")
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.writer.println("""{"message": "x-kong-authorization header missing"}""")
            return false
        }
        try {
            val decodedJwt = JwtDecoder.decodeJwt(kongAuthorizationHeader)
            val clientId = decodedJwt.payload["client_id"]?.toString()
            if (clientId != "8797348034790034") {
                logger.warn("Invalid client_id in x-kong-authorization JWT")
                response.status = HttpStatus.UNAUTHORIZED.value()
                response.writer.println("""{"message": "Invalid client_id in JWT"}""")
                return false
            }
        } catch (e: Exception) {
            logger.error("Invalid x-kong-authorization JWT", e)
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.writer.println("""{"message": "Invalid JWT in x-kong-authorization"}""")
            return false
        }
        return true
    }
