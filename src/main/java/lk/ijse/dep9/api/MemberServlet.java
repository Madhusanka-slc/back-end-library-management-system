package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.JsonException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.dep9.api.util.HttpServlet2;
import lk.ijse.dep9.dto.MemberDTO;

import javax.lang.model.type.NoType;
import javax.sql.DataSource;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "MemberServletTemp", value = "/members/*",loadOnStartup = 0)

public class MemberServlet extends HttpServlet2 {
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
            System.out.println();

            if(matcher.matches()){
                getMemberDetails(matcher.group(1),response);

            }else {
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,"Expected valid UUID");
            }
        }

    }


    private void loadAllMembers(HttpServletResponse response) throws IOException {

        try(   Connection connection = pool.getConnection()) {

//                BasicDataSource pool= (BasicDataSource) getServletContext().getAttribute("pool");


                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM member");
                /* [{"id":"", "name":"",}, {}, {}]*/


                ArrayList<MemberDTO> members = new ArrayList<>();
                while (rst.next()){
                    String id = rst.getString("id");
                    String name = rst.getString("name");
                    String address = rst.getString("address");
                    String contact = rst.getString("contact");
                    MemberDTO dto = new MemberDTO(id, name, address, contact);
                    members.add(dto);
                }

//                pool.releaseConnection(connection);
            connection.close();// This is not going to close the connection,it is going to pool back  , instead it releases the connection

                Jsonb jsonb = JsonbBuilder.create();
//                response.addHeader("Access-Control-Allow-Origin","*");// note
//                response.addHeader("Access-Control-Allow-Origin","http://localhost:5501");
                response.setContentType("application/json");

                jsonb.toJson(members,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to load members");
        }

    }
    private void searchMembers(String query, HttpServletResponse response) throws IOException {
//        System.out.println("searchMembers()");
//        response.getWriter().printf("<h1>WS: Search Members for %s</h1>",query);
        try( Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM member WHERE id LIKE ? OR name LIKE ? OR address LIKE ? OR contact LIKE ?");
            query="%"+query+"%";//note
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
                members.add(new MemberDTO(id,name,address,contact));
            }

            response.setContentType("application/json");
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(members,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to fetch members");
        }

    }

    private void loadPaginatedAllMembers(int size, int page, HttpServletResponse response) throws IOException {
//        System.out.println("loadPaginatedAllMembers()");
//        response.getWriter().printf("<h1>WS: Load All Paginated Members, size: %d, page: %d</h1>",size,page);
        try(Connection connection = pool.getConnection()) {
            Statement stm = connection.createStatement();
            ResultSet rst=stm.executeQuery("SELECT COUNT(id) AS count FROM member");
            rst.next();
            int totalMembers = rst.getInt("count");
            response.setIntHeader("X-Total-Count",totalMembers);

            PreparedStatement stm2 = connection.prepareStatement("     SELECT * FROM member LIMIT ? OFFSET  ?");
            stm2.setInt(1,size);
            stm2.setInt(2,(page-1)*size);
            rst=stm2.executeQuery();

            ArrayList<MemberDTO> members = new ArrayList<>();

            while (rst.next()){
                String id = rst.getString("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");

                members.add(new MemberDTO(id,name,address,contact));

            }
//            response.addHeader("Access-Control-Allow-Origin","*");// note
//            response.addHeader("Access-Control-Allow-Headers","X-Total-Count");// note
//            response.addHeader("Access-Control-Expose-Headers","X-Total-Count");// note
            response.setContentType("application/json");
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(members,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to load paginated members");
        }

    }

    private void searchPaginatedMembers(String query,int size,int page, HttpServletResponse response) throws IOException {
//        System.out.println("searchPaginatedMembers()");
//        response.getWriter().printf("<h1>WS: Search All Paginated Members for %s, size: %d, page: %d</h1>",query,size,page);


        try(Connection connection = pool.getConnection()) {
            String sql="SELECT COUNT(id) AS count FROM member WHERE id LIKE ? OR name LIKE ? OR address LIKE ? OR contact LIKE ?";
            PreparedStatement countStm = connection.prepareStatement(sql);// sanatize , user input use
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM member WHERE id LIKE ? OR name LIKE ? OR address LIKE ? OR contact LIKE ? LIMIT ? OFFSET ?");
            query="%"+query+"%";//note
            int length=sql.split("[?]").length;
            for (int i = 1; i <=length ; i++) {
                countStm.setString(i,query);
                stm.setString(i,query);
            }

            stm.setInt(length + 1,size);
            stm.setInt(length+2,(page-1)*size);
            ResultSet rst = countStm.executeQuery();
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
//            response.addHeader("Access-Control-Allow-Origin","*");// note
//            response.addHeader("Access-Control-Allow-Headers","X-Total-Count");// note
//            response.addHeader("Access-Control-Expose-Headers","X-Total-Count");// note
            response.setContentType("application/json");
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(members,response.getWriter());



        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to fetch paginated members");
        }


    }

//    @Override
//    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//       resp.setHeader("Access-Control-Allow-Origin","*");
//       resp.setHeader("Access-Control-Allow-Methods","POST,GET,PATCH,DELETE,HEAD,OPTIONS,PUT");
//        String headers = req.getHeader("Access-Control-Request-Headers");
//        if(headers !=null){
//            resp.setHeader("Access-Control-Allow-Headers",headers);
//            resp.setHeader("Access-Control-Expose-Headers",headers);
//
//        }
//    }

    private  void getMemberDetails(String memberId, HttpServletResponse response) throws IOException {
//        System.out.println("getMemberDetails()");
//        response.getWriter().printf("<h1>WS: Get Member Details of: %s</h1>",memberId);
        try(Connection connection = pool.getConnection()) {

            PreparedStatement stm = connection.prepareStatement("SELECT * FROM member WHERE id=? ");
            stm.setString(1,memberId);
            ResultSet rst = stm.executeQuery();
            ArrayList<MemberDTO> members = new ArrayList<>();
            if(rst.next()){
                String id = rst.getString("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");
                members.add(new MemberDTO(id,name,address,contact));
//                response.setHeader("Access-Control-Allow-Origin","*");
                response.setContentType("application/json");
                Jsonb jsonb = JsonbBuilder.create();
                jsonb.toJson(members,response.getWriter());

            }else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,"Invalid member id");

            }
/* ctrl + w */


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to fetch the member");
        }

    }



    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        response.getWriter().println("MemberServletTemp: doPost()");
        if(request.getPathInfo()==null || request.getPathInfo().equals("/")){
            try {
                if(request.getContentType()==null || !request.getContentType().startsWith("application/json")){

                    throw new JsonbException("Invalid Json content");
                }
                MemberDTO member=JsonbBuilder.create().fromJson(request.getReader(),MemberDTO.class);

                if (member.getName()==null || !member.getName().matches("[A-Za-z ]+")) {
                    throw new JsonbException("Name is empty or invalid");

                } else if (member.getContact()==null || !member.getContact().matches("\\d{3}-\\d{7}")) {
                    throw new JsonbException("Contact is empty or invalid");

                }else if (member.getAddress()==null || !member.getAddress().matches("[A-Za-z0-9|,.:;#\\/\\\\ -]+")) {
                    throw new JsonbException("Address is empty or invalid");

                }
                try(Connection connection = pool.getConnection()) {
                    member.setId(UUID.randomUUID().toString());
                    PreparedStatement stm = connection.prepareStatement("INSERT INTO  member (id,name,address,contact) VALUES (?,?,?,?)");
                    stm.setString(1,member.getId());
                    stm.setString(2,member.getName());
                    stm.setString(3,member.getAddress());
                    stm.setString(4,member.getContact());

                    int affectedRows = stm.executeUpdate();
                    if(affectedRows==1){
                        response.setStatus(HttpServletResponse.SC_CREATED);
//                        response.setHeader("Access-Control-Allow-Origin","*");
                        response.setContentType("application/json");
                        JsonbBuilder.create().toJson(member,response.getWriter());

                    }else {
                        throw new JsonbException("Something went wrong");

                    }


                } catch (SQLException e) {
                    e.printStackTrace();
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
                }
            }catch (JsonbException e){
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,e.getMessage());

            }

        }else {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }


    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        resp.getWriter().println("MemberServletTemp: doDelete()");

        if(req.getPathInfo()==null || req.getPathInfo().equals("/")){
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;

        }else {
            Pattern pattern = Pattern.compile("^/([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})/?$");
            Matcher matcher = pattern.matcher(req.getPathInfo());

            if(matcher.matches()){
                // Todo delete the member
                deleteMember(matcher.group(1),resp);


            }else {
                resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,"Expected valid UUID");
            }

        }


    }

    private void deleteMember(String memberId,HttpServletResponse response){
         try(Connection connection = pool.getConnection()) {
             PreparedStatement stm = connection.prepareStatement("DELETE FROM member WHERE  id=?");
             stm.setString(1,memberId);
             int affectedRows = stm.executeUpdate();
             if(affectedRows==0){
                 response.sendError(HttpServletResponse.SC_NOT_FOUND,"Invalid member id");

             }else {
//                 response.setHeader("Access-Control-Allow-Origin","*");
                 response.setStatus(HttpServletResponse.SC_NO_CONTENT);
             }
         } catch (SQLException | IOException e) {
             throw new RuntimeException(e);
         }
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }

        Matcher matcher = Pattern.
                compile("^/([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})/?$")
                .matcher(request.getPathInfo());
        if (matcher.matches()) {
            updateMember(matcher.group(1), request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
    }

    private void updateMember(String memberId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
                throw new JsonbException("Invalid JSON");
            }
            MemberDTO member = JsonbBuilder.create().fromJson(request.getReader(), MemberDTO.class);

            if (member.getId() == null || !memberId.equalsIgnoreCase(member.getId())) {
                throw new JsonbException("Id is empty or invalid");
            } else if (member.getName() == null ||
                    !member.getName().matches("[A-Za-z ]+")) {
                throw new JsonbException("Name is empty or invalid");
            } else if (member.getContact() == null ||
                    !member.getContact().matches("\\d{3}-\\d{7}")) {
                throw new JsonbException("Contact is empty or invalid");
            } else if (member.getAddress() == null ||
                    !member.getAddress().matches("[A-Za-z0-9|,.:;#\\/\\\\ -]+")) {
                throw new JsonbException("Address is empty or invalid");
            }

            try (Connection connection = pool.getConnection()) {
                PreparedStatement stm = connection.
                        prepareStatement("UPDATE member SET name=?, address=?, contact=? WHERE id=?");
                stm.setString(1, member.getName());
                stm.setString(2, member.getAddress());
                stm.setString(3, member.getContact());
                stm.setString(4, member.getId());

                if (stm.executeUpdate() == 1) {
//                    response.setHeader("Access-Control-Allow-Origin","*");
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Member does not exist");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update the member");
            }
        } catch (JsonbException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }


}
