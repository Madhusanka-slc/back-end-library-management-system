package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import lk.ijse.dep9.dto.MemberDTO;
import lk.ijse.dep9.api.util.HttpServlet2;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@WebServlet(name = "MemberServletTemp", value = "/members/*",loadOnStartup = 0)

public class MemberServletTemp extends HttpServlet2 {
@Resource(lookup = "java:/comp/env/jdbc/dep9-lms")  //glassfish jdbc/lms
    private DataSource pool;

 /*   @Override
    public void init() throws ServletException {
        try {
            InitialContext ctx = new InitialContext();
          pool = (DataSource) ctx.lookup("jdbc/lms");
//            System.out.println(lookup);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

    }*/

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //response.getWriter().println("MemberServletTemp: doGet()");
        //max path segment 1

        if (request.getPathInfo()==null || request.getPathInfo().equals("/")){
            // members. members/
            String query = request.getParameter("q");
            String size = request.getParameter("size");
            String page = request.getParameter("page");
            if(query !=null && size !=null && page !=null){
//                response.getWriter().println("<h1>Search Members by Page(paginated)</h1>");
                if (!size.matches("\\d+") || !page.matches("\\d+")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid Page or Size");
                }else {
                    searchPaginatedMembers(query,Integer.parseInt(size),Integer.parseInt(page),response);

                }

            } else if (query !=null) {
//                response.getWriter().println("<h1>Search Members</h1>");
                searchMembers(query,response);


            } else if (page !=null  & size !=null ) {
//                response.getWriter().println("Load Members by Page");
                if (!size.matches("\\d+") || !page.matches("\\d+")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid Page or Size");
                }else {
                    loadPaginatedAllMembers(Integer.parseInt(size),Integer.parseInt(page),response);

                }
            } else  {
//                response.getWriter().println("Load All Members");
                loadAllMembers(response);
            }

        }else {
            /*  UUID  00000000-0000-0000-0000-000000000000  */
            /* pattern matcher*/
            Pattern pattern = Pattern.compile("^/([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})/?$");
            Matcher matcher = pattern.matcher(request.getPathInfo());

            if(matcher.matches()){
                getMemberDetails(matcher.group(1),response);

            }else {
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,"Expected valid UUID");
            }
        }

    }


    private  void getMemberDetails(String memberId,HttpServletResponse response) throws IOException {
        try(Connection connection = pool.getConnection()) {

            PreparedStatement stm = connection.prepareStatement("SELECT * FROM member WHERE id=?");
            stm.setString(1,memberId);
            ResultSet rst = stm.executeQuery();
            if(rst.next()){
                String id = rst.getString("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");
                MemberDTO member = new MemberDTO(id, name, address, contact);
                response.setContentType("application/json");
                JsonbBuilder.create().toJson(member,response.getWriter());



            }else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,"Invalid member ID");
            }


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to search the members");
        }

    }
    private void loadAllMembers(HttpServletResponse response) throws IOException {
        try(Connection connection = pool.getConnection()) {
            Statement stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SELECT * FROM member");
            ArrayList<MemberDTO> members = new ArrayList<>();
            while (rst.next()){
                String id = rst.getString("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");
                MemberDTO member = new MemberDTO(id, name, address, contact);
                members.add(member);


            }
            response.setContentType("application/json");
            JsonbBuilder.create().toJson(members,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to load all members");
        }

    }
    private void searchMembers(String query, HttpServletResponse response) throws IOException {
        try(Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM member WHERE id LIKE ? name LIKE ? address LIKE ? contact LIKE ?");
            query="%"+query+"%";
            stm.setString(1,query);
            stm.setString(2,query);
            stm.setString(3,query);
            stm.setString(4,query);

            ResultSet rst = stm.executeQuery();

            ArrayList<MemberDTO> members = new ArrayList<>();
            while (rst.next()){
                String id = rst.getString("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");
                MemberDTO member = new MemberDTO(id, name, address, contact);
                members.add(member);

            }
            response.setContentType("application/json");
            JsonbBuilder.create().toJson(members,response.getWriter());



        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to search members");
        }

    }

    private void loadPaginatedAllMembers(int size, int page, HttpServletResponse response) throws IOException {
        try(Connection connection = pool.getConnection()) {
            Statement stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SELECT COUNT(id) AS count FROM member ");
            rst.next();// note
            int totalMembers = rst.getInt("count");
            response.setIntHeader("X-Total-Count",totalMembers);
//            response.addIntHeader("X-Total-Count",totalMembers);

            PreparedStatement stm2 = connection.prepareStatement("SELECT * FROM member WHERE LIMIT=? OFFSET=?");
            //fill parameters
            stm2.setInt(1,size);
            stm2.setInt(2,(page-1)*size);
            rst = stm2.executeQuery();
            ArrayList<MemberDTO> members = new ArrayList<>();
            while (rst.next()){
                String id = rst.getString("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");
                MemberDTO member = new MemberDTO(id, name, address, contact);
                members.add(member);
                /* Alt + shift > multiple cursor*/

            }
            response.setContentType("application/json");
            JsonbBuilder.create().toJson(members,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to load paginated members");
        }

    }
    private void searchPaginatedMembers(String query,int size,int page, HttpServletResponse response) throws IOException {
        try(Connection connection = pool.getConnection()) {
            String sql="SELECT COUNT(id) AS count FROM member WHERE id LIKE ? name LIKE ? address LIKE ? contact LIKE ? ";

            PreparedStatement Countstm = connection.prepareStatement(sql);
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM member WHERE id LIKE ? name LIKE ? address LIKE ? contact LIKE ? LIMIT=? OFFSET=? ");
            query="%" +query+"%";

            int length=sql.split("[?]").length;
            for (int i = 1; i <= length; i++) {
                Countstm.setString(i,query);
                stm.setString(i,query);
            }
            ResultSet rst = Countstm.executeQuery();
            rst.next();
            response.setIntHeader("X-Total-Count",rst.getInt("count"));

            stm.setInt(length+1,size);
            stm.setInt(length+2,(page-1)*size);

            rst = Countstm.executeQuery();
            rst.next();
            response.setIntHeader("X-Total-Count",rst.getInt("count"));

            rst=stm.executeQuery();

            ArrayList<MemberDTO> members = new ArrayList<>();

            while (rst.next()){
                String id = rst.getString("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");

                members.add(new MemberDTO(id,name,address,contact));

            }
            response.setContentType("application/json");
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(members,response.getWriter());



        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to search paginated members");
        }


    }



    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("MemberServletTemp: doPost()");

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println("MemberServletTemp: doDelete()");
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println("MemberServletTemp: doPatch()");
    }
}
