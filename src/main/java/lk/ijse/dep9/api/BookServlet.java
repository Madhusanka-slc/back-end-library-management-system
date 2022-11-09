package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.api.util.HttpServlet2;
import lk.ijse.dep9.dto.BookDTO;
import lk.ijse.dep9.dto.MemberDTO;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "BookServlet", value = "/books/*",loadOnStartup = 0)
public class BookServlet extends HttpServlet2 {

    @Resource(lookup = "java:/comp/env/jdbc/dep9-lms")  //glassfish jdbc/lms
    private DataSource pool;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getPathInfo()==null || request.getPathInfo().equals("/")){
            System.out.println("Get it");
            // members. members/
            String query = request.getParameter("q");
            String size = request.getParameter("size");
            String page = request.getParameter("page");
            if(query !=null && size !=null && page !=null){
//                response.getWriter().println("<h1>Search Members by Page(paginated)</h1>");
                if (!size.matches("\\d+") || !page.matches("\\d+")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid Page or Size");
                }else {
                    searchPaginatedBooks(query,Integer.parseInt(size),Integer.parseInt(page),response);

                }

            } else if (query !=null) {
//                response.getWriter().println("<h1>Search Members</h1>");
                searchBooks(query,response);


            } else if (page !=null  & size !=null ) {
//                response.getWriter().println("Load Members by Page");
                if (!size.matches("\\d+") || !page.matches("\\d+")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid Page or Size");
                }else {
                    loadPaginatedAllBooks(Integer.parseInt(size),Integer.parseInt(page),response);

                }
            } else  {
//                response.getWriter().println("Load All Members");
                loadAllBooks(response);
            }

        }else {
            /*  978-3-16-148410-0  */
            /* pattern matcher*/
            Pattern pattern = Pattern.compile("^/([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})/?$");
            Matcher matcher = pattern.matcher(request.getPathInfo());

            if(matcher.matches()){
                getBookDetails(matcher.group(1),response);

            }else {
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,"Expected valid UUID");
            }
        }

    }

    private void getBookDetails(String bookId, HttpServletResponse response) throws IOException {

        System.out.println("getBookDetails()");
        try(Connection connection = pool.getConnection()) {

            PreparedStatement stm = connection.prepareStatement("SELECT * FROM book WHERE isbn=? ");
            stm.setString(1,bookId);
            ResultSet rst = stm.executeQuery();
            ArrayList<BookDTO> books = new ArrayList<>();
            if(rst.next()){
                String isbn = rst.getString("isbn");
                String title = rst.getString("title");
                String author = rst.getString("author");
                String copies = rst.getString("copies");
                books.add(new BookDTO(isbn,title,author,Integer.parseInt(copies)));
                response.setHeader("Access-Control-Allow-Origin","*");
                response.setContentType("application/json");
                Jsonb jsonb = JsonbBuilder.create();
                jsonb.toJson(books,response.getWriter());

            }else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,"Invalid book isbn");

            }
            /* ctrl + w */


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to fetch the book");
        }

    }

    private void loadAllBooks(HttpServletResponse response) throws IOException {
        System.out.println("loadAllBooks()");
        try(Connection connection = pool.getConnection()) {

//                BasicDataSource pool= (BasicDataSource) getServletContext().getAttribute("pool");


            Statement stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SELECT * FROM book");
            /* [{"id":"", "name":"",}, {}, {}]*/


            ArrayList<BookDTO> books = new ArrayList<>();
            while (rst.next()){
                String isbn = rst.getString("isbn");
                String title = rst.getString("title");
                String author = rst.getString("author");
                String copies = rst.getString("copies");
                BookDTO dto = new BookDTO(isbn, title, author, Integer.parseInt(copies));
                books.add(dto);
            }

//                pool.releaseConnection(connection);
           // connection.close();// This is not going to close the connection,it is going to pool back  , instead it releases the connection

            Jsonb jsonb = JsonbBuilder.create();
            response.addHeader("Access-Control-Allow-Origin","*");// note
//                response.addHeader("Access-Control-Allow-Origin","http://localhost:5501");
            response.setContentType("application/json");

            jsonb.toJson(books,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to load books");
        }

    }

    private void loadPaginatedAllBooks(int size, int page, HttpServletResponse response) throws IOException {
        System.out.println("loadPaginatedAllBooks()");
        try(Connection connection = pool.getConnection()) {
            Statement stm = connection.createStatement();
            ResultSet rst=stm.executeQuery("SELECT COUNT(isbn) AS count FROM book");
            rst.next();
            int totalBooks = rst.getInt("count");
            response.setIntHeader("X-Total-Count",totalBooks);

            PreparedStatement stm2 = connection.prepareStatement("     SELECT * FROM book LIMIT ? OFFSET  ?");
            stm2.setInt(1,size);
            stm2.setInt(2,(page-1)*size);
            rst=stm2.executeQuery();

            ArrayList<BookDTO> books = new ArrayList<>();

            while (rst.next()){
                String isbn = rst.getString("isbn");
                String title = rst.getString("title");
                String author = rst.getString("author");
                String copies = rst.getString("copies");

                books.add(new BookDTO(isbn,title,author,Integer.parseInt(copies)));

            }
            response.addHeader("Access-Control-Allow-Origin","*");// note
            response.addHeader("Access-Control-Allow-Headers","X-Total-Count");// note
            response.addHeader("Access-Control-Expose-Headers","X-Total-Count");// note
            response.setContentType("application/json");
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(books,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to load paginated books");
        }


    }

    private void searchBooks(String query, HttpServletResponse response) throws IOException {
        System.out.println("searchBooks()");
        try( Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM book WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? OR copies LIKE ?");
            query="%"+query+"%";//note
            stm.setString(1,query);
            stm.setString(2,query);
            stm.setString(3,query);
            stm.setString(4,query);
            ResultSet rst = stm.executeQuery();
            ArrayList<BookDTO> books = new ArrayList<>();
            while (rst.next()){
                String isbn = rst.getString("isbn");
                String title = rst.getString("title");
                String author = rst.getString("author");
                String copies = rst.getString("copies");
                books.add(new BookDTO(isbn,title,author,Integer.parseInt(copies)));
            }

            response.setContentType("application/json");
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(books,response.getWriter());


        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to fetch books");
        }

    }

    private void searchPaginatedBooks(String query, int size, int page, HttpServletResponse response) throws IOException {
        System.out.println("searchPaginatedBooks()");
        try(Connection connection = pool.getConnection()) {
            String sql="SELECT COUNT(isbn) AS count FROM book WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? OR copies LIKE ?";
            PreparedStatement countStm = connection.prepareStatement(sql);// sanatize , user input use
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM book WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? OR copies LIKE ? LIMIT ? OFFSET ?");
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

            ArrayList<BookDTO> books = new ArrayList<>();

            while (rst.next()){
                String isbn = rst.getString("isbn");
                String title = rst.getString("title");
                String author = rst.getString("author");
                String copies = rst.getString("copies");

                books.add(new BookDTO(isbn,title,author,Integer.parseInt(copies)));

            }
            response.addHeader("Access-Control-Allow-Origin","*");// note
            response.addHeader("Access-Control-Allow-Headers","X-Total-Count");// note
            response.addHeader("Access-Control-Expose-Headers","X-Total-Count");// note
            response.setContentType("application/json");
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(books,response.getWriter());



        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to fetch paginated books");
        }
        
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("doPost()");
        if(request.getPathInfo()==null || request.getPathInfo().equals("/")){
            try {
                if(request.getContentType()==null || !request.getContentType().startsWith("application/json")){

                    throw new JsonbException("Invalid Json content");
                }
                BookDTO book=JsonbBuilder.create().fromJson(request.getReader(),BookDTO.class);

                if (book.getTitle()==null || !book.getTitle().matches("[A-Za-z ]+")) {
                    throw new JsonbException("Title is empty or invalid");

                } else if (book.getAuthor()==null || !book.getAuthor().matches("[A-Za-z ]+")) {
                    throw new JsonbException("Author is empty or invalid");

                }else if (book.getCopies()==0 || !String.valueOf(book.getCopies()).matches("\\d+")) {
                    throw new JsonbException("Copies is empty or invalid");

                }
                try(Connection connection = pool.getConnection()) {
                    book.setIsbn(UUID.randomUUID().toString());
                    PreparedStatement stm = connection.prepareStatement("INSERT INTO  book (isbn,title,author,copies) VALUES (?,?,?,?)");
                    stm.setString(1,book.getIsbn());
                    stm.setString(2,book.getTitle());
                    stm.setString(3,book.getAuthor());
                    stm.setString(4, String.valueOf(book.getCopies()));

                    int affectedRows = stm.executeUpdate();
                    if(affectedRows==1){
                        response.setStatus(HttpServletResponse.SC_CREATED);
                        response.setHeader("Access-Control-Allow-Origin","*");
                        response.setContentType("application/json");
                        JsonbBuilder.create().toJson(book,response.getWriter());

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
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("doPatch()");
        if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }

        Matcher matcher = Pattern.
                compile("^/([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})/?$")
                .matcher(request.getPathInfo());
        if (matcher.matches()) {
            updateBook(matcher.group(1), request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }

    }

    private void updateBook(String bookId, HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
                throw new JsonbException("Invalid JSON");
            }
            BookDTO book = JsonbBuilder.create().fromJson(request.getReader(), BookDTO.class);

            if (book.getIsbn() == null || !bookId.equalsIgnoreCase(book.getIsbn())) {
                throw new JsonbException("Isbn is empty or invalid");
            } else if (book.getTitle() == null ||
                    !book.getTitle().matches("[A-Za-z ]+")) {
                throw new JsonbException("Title is empty or invalid");
            } else if (book.getAuthor() == null ||
                    !book.getAuthor().matches("[A-Za-z ]+")) {
                throw new JsonbException("Author is empty or invalid");
            } else if (book.getCopies() == 0 ||
                    !String.valueOf(book.getCopies()).matches("\\d+")) {
                throw new JsonbException("Copies is empty or invalid");
            }

            try (Connection connection = pool.getConnection()) {
                PreparedStatement stm = connection.
                        prepareStatement("UPDATE book SET title=?, author=?, copies=? WHERE isbn=?");
                stm.setString(1, book.getTitle());
                stm.setString(2, book.getAuthor());
                stm.setString(3, String.valueOf(book.getCopies()));
                stm.setString(4, book.getIsbn());

                if (stm.executeUpdate() == 1) {
                    response.setHeader("Access-Control-Allow-Origin","*");
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Book does not exist");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update the Book");
            }
        } catch (JsonbException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getPathInfo()==null || req.getPathInfo().equals("/")){
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;

        }else {
            Pattern pattern = Pattern.compile("^/([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})/?$");
            Matcher matcher = pattern.matcher(req.getPathInfo());

            if(matcher.matches()){
                // Todo delete the member
                deleteBook(matcher.group(1),resp);


            }else {
                resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,"Expected valid UUID");
            }

        }
    }

    private void deleteBook(String bookIsbn, HttpServletResponse response) {
        try(Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("DELETE FROM book WHERE  isbn=?");
            stm.setString(1,bookIsbn);
            int affectedRows = stm.executeUpdate();
            if(affectedRows==0){
                response.sendError(HttpServletResponse.SC_NOT_FOUND,"Invalid book isbn");

            }else {
                response.setHeader("Access-Control-Allow-Origin","*");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            }

        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin","*");
        resp.setHeader("Access-Control-Allow-Methods","POST,GET,PATCH,DELETE,HEAD,OPTIONS,PUT");
        String headers = req.getHeader("Access-Control-Request-Headers");
        if(headers !=null){
            resp.setHeader("Access-Control-Allow-Headers",headers);
            resp.setHeader("Access-Control-Expose-Headers",headers);

        }
    }

}
