package net.mbonnin.appengine

import com.google.cloud.storage.Storage
import java.util.regex.Pattern
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class MainServlet : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        System.out.println("service=${req.servletPath} pathInfo=${req.pathInfo}")

        resp.status = 200
        resp.writer.write("Hello World")
    }
}
