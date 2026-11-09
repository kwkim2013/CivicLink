package com.civiclink.complaint;
import com.civiclink.auth.SessionUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class ComplaintRepository {
    private final JdbcTemplate jdbc;
    public ComplaintRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private String filter(SessionUser user){return "ADMIN".equals(user.role)?"":" AND owner_id="+user.id;}
    public List<Complaint> list(SessionUser user){return jdbc.query("SELECT id,title,content,category,status FROM complaints WHERE 1=1"+filter(user)+" ORDER BY id DESC LIMIT 100",(rs,n)->new Complaint(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)));}
    public Complaint find(long id,SessionUser user){List<Complaint> rows=jdbc.query("SELECT id,title,content,category,status FROM complaints WHERE id=?"+filter(user),(rs,n)->new Complaint(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)),id);return rows.isEmpty()?null:rows.get(0);}
    public Complaint create(Complaint input,SessionUser user){
        KeyHolder keys=new GeneratedKeyHolder();
        // Request only id: PostgreSQL otherwise returns every column, which getKey rejects.
        jdbc.update(c->{java.sql.PreparedStatement ps=c.prepareStatement("INSERT INTO complaints(owner_id,title,content,category,status) VALUES(?,?,?,?,?)",new String[]{"id"});ps.setLong(1,user.id);ps.setString(2,input.getTitle());ps.setString(3,input.getContent());ps.setString(4,input.getCategory());ps.setString(5,"RECEIVED");return ps;},keys);
        Number generatedId = keys.getKey();
        if (generatedId == null) throw new org.springframework.dao.DataRetrievalFailureException("Generated complaint id missing");
        Long id = generatedId.longValue();
        return find(id,user);
    }
    public int update(long id,Complaint input,SessionUser user){return jdbc.update("UPDATE complaints SET title=?,content=?,category=? WHERE id=?"+filter(user),input.getTitle(),input.getContent(),input.getCategory(),id);}
    public int delete(long id,SessionUser user){return jdbc.update("DELETE FROM complaints WHERE id=?"+filter(user),id);}
}
