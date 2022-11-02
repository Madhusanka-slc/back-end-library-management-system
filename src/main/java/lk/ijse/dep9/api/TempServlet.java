package lk.ijse.dep9.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.api.util.HttpServlet2;
import lk.ijse.dep9.db.ConnectionPool;

import java.io.IOException;
import java.io.PrintWriter;


@WebServlet(name = "TempServlet", urlPatterns = {"/release"})
public class TempServlet extends HttpServlet2 {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ConnectionPool pool = (ConnectionPool) getServletContext().getAttribute("pool");
        pool.releaseAllConnections();


    }

}

//@WebServlet(name = "TempServlet", urlPatterns = {"/temp/*","*.php"})
//public class TempServlet extends HttpServlet {
//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//
//       response.setContentType("text/html");
//       try(PrintWriter out=response.getWriter()) {
//           out.println("<style>p{font-weight:bold; font-size: 1.2rem;}</style>");
//           out.printf("<p>Request URI: %s</p>",request.getRequestURI());
//           out.printf("<p>Request URL: %s</p>",request.getRequestURL());
//           out.printf("<p>Servlet Path : %s</p>",request.getServletPath());
//           out.printf("<p>Context Path : %s</p>",request.getContextPath());
//           out.printf("<p>Path Info : %s</p>",request.getPathInfo());
//
//       }
//    }
//
//}
