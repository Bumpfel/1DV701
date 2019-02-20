package assignment2;

@WebServlet("/upload")  
@MultipartConfig
public class UploadServlet extends HTTPServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String description = request.getParameter("description"); // Retrieves <input type="text" name="description">
        Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
        InputStream fileContent = filePart.getInputStream();
        System.out.print("------------TEST");
        // ... (do your job here)
    }
}