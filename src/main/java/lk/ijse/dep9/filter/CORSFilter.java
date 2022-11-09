package lk.ijse.dep9.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@WebFilter(filterName = "cors-filter",urlPatterns = {"/members/*","/books/*"})
public class CORSFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        res.setHeader("Access-Control-Allow-Origin","*");
        if(req.getMethod().equals("OPTIONS")){
            res.setHeader("Access-Control-Allow-Methods","POST,GET,PATCH,DELETE,HEAD,OPTIONS,PUT");
            String headers = req.getHeader("Access-Control-Request-Headers");
            if(headers !=null){
                res.setHeader("Access-Control-Allow-Headers",headers);
                res.setHeader("Access-Control-Expose-Headers",headers);

            }

        }



        if (req.getPathInfo()==null || req.getPathInfo().equals("/")){
            String query = req.getParameter("q");
            String size = req.getParameter("size");
            String page = req.getParameter("page");
            if((query !=null || query==null) && size !=null && page !=null){
                res.addHeader("Access-Control-Allow-Headers","X-Total-Count");
                res.addHeader("Access-Control-Expose-Headers","X-Total-Count");
                
            }


        }
        chain.doFilter(req,res);

    }
}
